package com.werkloop.dumbify.system

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.werkloop.dumbify.domain.DumbClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground time, from one pass over `queryEvents`.
 *
 * Per-app time and the day totals are built in the **same** walk, so the
 * launcher's rows sum to the statistics screen's "today" by construction. The
 * underlying estimate is approximate — brief resumes double-count, sessions
 * split at midnight — but the two figures cannot disagree with each other,
 * which is what dumb-launcher "Totals reconcile" actually asks for.
 */
@Singleton
class AndroidUsageReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: DumbClock,
    private val permissions: PermissionChecker,
) : UsageReader {

    private val manager: UsageStatsManager?
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override suspend fun today(): UsageToday? = withContext(Dispatchers.IO) {
        if (!permissions.hasUsageAccess()) return@withContext null
        val zone = clock.zone()
        val now = clock.now()
        val dayStart = clock.today().atStartOfDay(zone).toInstant()
        val walk = walk(dayStart, now) ?: return@withContext null

        val perPackage = walk.sessions
            .groupBy { it.packageName }
            .mapValues { (_, s) -> s.fold(Duration.ZERO) { acc, it -> acc + it.duration } }
            // A session shorter than a second reads as "0m" and adds nothing but
            // a row the user did not cause.
            .filterValues { !it.isZero && !it.isNegative }

        UsageToday(
            perPackage = perPackage,
            sessions = walk.sessions,
            appsOpened = walk.appsOpened,
            unlocks = walk.unlocks,
        )
    }

    override suspend fun lastSevenDays(): List<DayUsage>? = withContext(Dispatchers.IO) {
        if (!permissions.hasUsageAccess()) return@withContext null
        val zone = clock.zone()
        val today = clock.today()
        val from = today.minusDays(6).atStartOfDay(zone).toInstant()
        val walk = walk(from, clock.now()) ?: return@withContext null

        val byDay = walk.sessions.groupBy {
            // A session is attributed to the day it started. Sessions spanning
            // midnight are rare and short; splitting them would buy precision
            // the underlying data does not have.
            it.start.atZone(zone).toLocalDate()
        }
        (0..6).map { offset ->
            val date = today.minusDays((6 - offset).toLong())
            DayUsage(
                date = date,
                total = byDay[date].orEmpty().fold(Duration.ZERO) { acc, s -> acc + s.duration },
            )
        }
    }

    override suspend fun baselineDailyAverage(setupCompletedOn: LocalDate?): Duration? =
        withContext(Dispatchers.IO) {
            if (!permissions.hasUsageAccess() || setupCompletedOn == null) return@withContext null
            val zone = clock.zone()
            // Only whole days strictly before setup count: the day setup
            // happened is half dumb and would drag the baseline down.
            val end = setupCompletedOn.atStartOfDay(zone).toInstant()
            val start = setupCompletedOn.minusDays(BASELINE_DAYS).atStartOfDay(zone).toInstant()
            val walk = walk(start, end) ?: return@withContext null

            val byDay = walk.sessions.groupBy { it.start.atZone(zone).toLocalDate() }
            // Averaging two or three days and calling it "before Dumbify" is
            // not a baseline, it is a coincidence. Below the floor, say
            // unavailable (usage-stats "No baseline available").
            if (byDay.size < MIN_BASELINE_DAYS) return@withContext null
            val total = walk.sessions.fold(Duration.ZERO) { acc, s -> acc + s.duration }
            total.dividedBy(byDay.size.toLong())
        }

    override suspend fun baselinePerApp(setupCompletedOn: LocalDate?): Map<String, Duration> =
        withContext(Dispatchers.IO) {
            if (!permissions.hasUsageAccess() || setupCompletedOn == null) return@withContext emptyMap()
            val zone = clock.zone()
            val end = setupCompletedOn.atStartOfDay(zone).toInstant()
            val start = setupCompletedOn.minusDays(BASELINE_DAYS).atStartOfDay(zone).toInstant()
            val walk = walk(start, end) ?: return@withContext emptyMap()
            val days = walk.sessions.map { it.start.atZone(zone).toLocalDate() }.distinct().size
            if (days == 0) return@withContext emptyMap()
            walk.sessions
                .groupBy { it.packageName }
                .mapValues { (_, s) ->
                    s.fold(Duration.ZERO) { acc, it -> acc + it.duration }.dividedBy(days.toLong())
                }
        }

    private data class Walk(
        val sessions: List<Session>,
        val appsOpened: Int,
        val unlocks: Int,
    )

    /**
     * Pairs RESUMED with the next PAUSED/STOPPED for the same package. An app
     * still in the foreground when the walk ends is closed off at [end] rather
     * than dropped, so the current session counts.
     */
    private fun walk(begin: Instant, end: Instant): Walk? {
        val events = manager?.queryEvents(begin.toEpochMilli(), end.toEpochMilli()) ?: return null
        val open = HashMap<String, Long>()
        val sessions = ArrayList<Session>()
        var appsOpened = 0
        var unlocks = 0
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (open.put(event.packageName, event.timeStamp) == null) appsOpened++
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val startedAt = open.remove(event.packageName) ?: continue
                    if (event.timeStamp > startedAt) {
                        sessions += Session(
                            event.packageName,
                            Instant.ofEpochMilli(startedAt),
                            Instant.ofEpochMilli(event.timeStamp),
                        )
                    }
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlocks++
            }
        }
        // Whatever is still open was open when we looked.
        open.forEach { (pkg, startedAt) ->
            if (end.toEpochMilli() > startedAt) {
                sessions += Session(pkg, Instant.ofEpochMilli(startedAt), end)
            }
        }
        return Walk(sessions, appsOpened, unlocks)
    }

    private companion object {
        /** How far back to look for a pre-Dumbify baseline. */
        const val BASELINE_DAYS = 7L

        /**
         * Fewer distinct days than this and there is no baseline worth stating.
         * `UsageStatsManager` keeps roughly a week, so on a fresh device this
         * is the common case, not the edge case.
         */
        const val MIN_BASELINE_DAYS = 5
    }
}
