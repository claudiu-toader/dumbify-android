package com.werkloop.dumbify.system

import com.werkloop.dumbify.domain.InstalledApp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * The whole of Android, behind six interfaces.
 *
 * Nothing above this file imports an Android API, which is what lets the
 * domain and every screen be exercised on the JVM with a fake in place of the
 * device (design decision 10).
 */

interface AppCatalog {
    /** Launchable apps, Dumbify excluded, sorted by label. */
    suspend fun installedApps(): List<InstalledApp>
}

/** One foreground stretch in one app. */
data class Session(val packageName: String, val start: Instant, val end: Instant) {
    val duration: Duration get() = Duration.between(start, end)
}

/**
 * Today's usage.
 *
 * [total] is the sum of [perPackage] rather than a second query, so the
 * launcher's rows and the statistics screen's "today" reconcile by
 * construction even where the underlying estimate is imperfect
 * (dumb-launcher "Totals reconcile").
 */
data class UsageToday(
    val perPackage: Map<String, Duration>,
    val sessions: List<Session>,
    val appsOpened: Int,
    val unlocks: Int,
) {
    val total: Duration = perPackage.values.fold(Duration.ZERO, Duration::plus)
}

data class DayUsage(val date: LocalDate, val total: Duration)

interface UsageReader {
    /** Null when usage access is not granted — never zero. */
    suspend fun today(): UsageToday?

    /** Seven days ending today, oldest first. Null when access is not granted. */
    suspend fun lastSevenDays(): List<DayUsage>?

    /**
     * The daily average before Dumbify was set up.
     *
     * Null means "unavailable", which is the honest answer on a fresh device:
     * `UsageStatsManager` keeps daily buckets for about a week, so there may
     * simply be no history to average (usage-stats "No baseline available").
     */
    suspend fun baselineDailyAverage(setupCompletedOn: LocalDate?): Duration?

    /**
     * Pre-Dumbify daily average per app, for the request screen's "1.2h/day
     * before" line. Empty rather than null for an app with no history — the
     * distinction that matters is whether there is a baseline *at all*, which
     * [baselineDailyAverage] already answers.
     */
    suspend fun baselinePerApp(setupCompletedOn: LocalDate?): Map<String, Duration>
}

/**
 * Permission state, read live every time.
 *
 * Both grants are revocable from outside the app with no callback to us, so
 * none of these is ever cached as a boolean — a cached flag is how these
 * screens end up lying (design decision 8).
 */
interface PermissionChecker {
    fun hasUsageAccess(): Boolean
    fun isDefaultHome(): Boolean
    fun canPostNotifications(): Boolean
}

/** The round-trips out to system surfaces. */
interface SystemNavigator {
    fun openUsageAccessSettings()
    fun openDefaultHomeSettings()
    /** Device-wide greyscale lives behind accessibility colour correction. */
    fun openColorCorrectionSettings()
    fun openNotificationSettings()
    /** False when the app has gone away since the catalogue was read. */
    fun launchApp(packageName: String): Boolean
}

/**
 * What this build can actually enforce.
 *
 * Absence-only: a disallowed app is absent from every Dumbify surface, and
 * Dumbify does not intercept, overlay or kill an app reached by another route.
 * The interface exists so a stricter implementation can be added later without
 * touching a screen (design decision 1).
 */
enum class EnforcementModel { AbsenceOnly }

interface EnforcementController {
    val model: EnforcementModel

    /** True when Dumbify's own surfaces should be hiding disallowed apps. */
    fun isEnforcing(windowInForce: Boolean, setupComplete: Boolean): Boolean
}

/**
 * Whether Dumbify offers itself as a home app at all.
 *
 * Android has no API for giving a role back: `RoleManager` can request
 * `ROLE_HOME` and there is no matching release. What an app *can* do is stop
 * being a candidate — the `CATEGORY_HOME` filter lives on an `<activity-alias>`
 * rather than on the activity, and disabling that component takes Dumbify out
 * of the home-app list. The system then falls back to another launcher, or asks
 * the user to pick one if more than one remains.
 *
 * This is what makes "stop enforcing" mean something. Without it the button
 * cleared a grant and left Dumbify sitting on the Home button regardless
 * (settings "Stopping releases the home role").
 *
 * Reversible: re-enabling puts Dumbify back among the candidates, though the
 * user still has to choose it — being a candidate is not the same as holding
 * the role, which is why [PermissionChecker.isDefaultHome] stays the authority
 * on whether it is actually in place.
 */
interface HomeRoleController {
    /** False once the alias has been disabled. */
    fun isOfferedAsHome(): Boolean

    fun setOfferedAsHome(enabled: Boolean)
}
