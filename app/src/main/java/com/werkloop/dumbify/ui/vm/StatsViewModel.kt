package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.data.DumbifyState
import com.werkloop.dumbify.domain.Allowance
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.WindowEvaluator
import com.werkloop.dumbify.system.DayUsage
import com.werkloop.dumbify.system.SystemNavigator
import com.werkloop.dumbify.system.UsageReader
import com.werkloop.dumbify.system.UsageToday
import com.werkloop.dumbify.ui.Fmt
import com.werkloop.dumbify.ui.screens.DayBar
import com.werkloop.dumbify.ui.screens.StatRow
import com.werkloop.dumbify.ui.screens.StatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    private val device: DeviceData,
    private val navigator: SystemNavigator,
    private val usage: UsageReader,
    private val clock: DumbClock,
) : ViewModel() {

    private val week = MutableStateFlow<List<DayUsage>?>(null)
    private val baseline = MutableStateFlow<Duration?>(null)

    init { reload() }

    private fun reload() = viewModelScope.launch {
        week.value = usage.lastSevenDays()
        baseline.value = usage.baselineDailyAverage(repository.state.value.setupCompletedOn)
    }

    val state: StateFlow<StatsUiState> = combine(
        device.today, device.hasUsageAccess, repository.state, week, baseline,
    ) { today, hasAccess, saved, days, before ->
        if (!hasAccess) return@combine StatsUiState("", null, emptyList(), emptyList(), true)
        val tallest = days.orEmpty().maxOfOrNull { it.total.toMinutes() }?.coerceAtLeast(1) ?: 1
        StatsUiState(
            todayTotal = Fmt.total(today?.total),
            // Null reads "UNAVAILABLE" on the screen — never as a zero the user
            // would read as an achievement (usage-stats "No baseline available").
            baseline = before?.let { Fmt.total(it) },
            week = days.orEmpty().map {
                DayBar(
                    initial = it.date.dayOfWeek.name.take(1),
                    fraction = it.total.toMinutes().toFloat() / tallest,
                    isToday = it.date == clock.today(),
                )
            },
            rows = listOf(
                StatRow("Apps opened", today?.appsOpened?.toString() ?: Fmt.DASH),
                StatRow("Unlocks", today?.unlocks?.toString() ?: Fmt.DASH),
                StatRow(
                    "Requests used",
                    "${Allowance.used(saved.ledger, clock.today())} of ${Allowance.PER_DAY}",
                ),
                StatRow("Longest dumb stretch", Fmt.total(longestDumbStretch(today, saved))),
            ),
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        StatsUiState(Fmt.DASH, null, emptyList(), emptyList()),
    )

    /**
     * The longest run of today during which the phone was dumb and no app was
     * opened.
     *
     * Both halves matter: time outside a window is not a dumb stretch, and time
     * inside one spent in an allowed app is not either. Computing it from the
     * same sessions the row values come from keeps it consistent with them.
     */
    private fun longestDumbStretch(today: UsageToday?, saved: DumbifyState): Duration? {
        if (today == null) return null
        val zone = clock.zone()
        val dayStart = clock.today().atStartOfDay(zone).toInstant()
        val now = clock.now()
        val busy = today.sessions.sortedBy { it.start }

        var longest = Duration.ZERO
        var cursor = dayStart
        fun consider(from: Instant, to: Instant) {
            var start = from
            while (start.isBefore(to)) {
                val local = start.atZone(zone).toLocalDateTime()
                val hourEnd = minOf(
                    start.atZone(zone).toLocalDateTime().truncatedTo(ChronoUnit.HOURS)
                        .plusHours(1).atZone(zone).toInstant(),
                    to,
                )
                if (WindowEvaluator.isDumbAt(local, saved.schedule)) {
                    val run = Duration.between(start, hourEnd)
                    if (run > longest) longest = run
                }
                start = hourEnd
            }
        }
        for (session in busy) {
            if (session.start.isAfter(cursor)) consider(cursor, session.start)
            if (session.end.isAfter(cursor)) cursor = session.end
        }
        if (cursor.isBefore(now)) consider(cursor, now)
        return longest.takeIf { !it.isZero }
    }

    fun fixUsageAccess() {
        navigator.openUsageAccessSettings()
    }
}
