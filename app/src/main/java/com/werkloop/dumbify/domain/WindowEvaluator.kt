package com.werkloop.dumbify.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Whether the phone is dumb right now, and when that next changes.
 *
 * [currentWindowEnd] is null when a window is in force but never ends — which
 * is exactly the always-on case, and is why the launcher's header reads "DUMB"
 * with no time rather than inventing one (dumb-launcher "Always on has no end
 * to state"). [nextWindowStart] is null when no window will open at all, which
 * is every preset set to OFF.
 */
data class WindowState(
    val inForce: Boolean,
    val currentWindowEnd: LocalDateTime?,
    val nextWindowStart: LocalDateTime?,
)

/** Which slice of the week the resulting-window bar is drawing. */
sealed interface WindowScope {
    data object Always : WindowScope
    data object EveryDay : WindowScope
    data class Day(val day: DayOfWeek) : WindowScope
}

/**
 * Every question about windows is answered here, by one pure function over
 * pre-split hour ranges, so the 24-hour bar, the launcher header, the widget
 * countdown and the boundary alarm cannot drift apart (design decision 6).
 */
object WindowEvaluator {

    /** How far ahead [windowStateAt] looks for a boundary before giving up. */
    private const val LOOKAHEAD_DAYS = 8L

    /** Is the phone dumb at this exact local time? */
    fun isDumbAt(at: LocalDateTime, schedule: Schedule): Boolean = when (schedule.mode) {
        FocusMode.AlwaysOn -> true
        FocusMode.Scheduled -> schedule.presetFor(at.dayOfWeek).coversHour(at.hour)
    }

    /**
     * Walks forward an hour at a time to the next change of state.
     *
     * Every boundary in the system falls on the hour, so hour-stepping finds
     * them exactly; it also means a window that continues across midnight —
     * overnight's `[21,24)` running into the next day's `[0,7)` — is found by
     * walking rather than by a merge rule that could disagree with the bar.
     */
    fun windowStateAt(now: LocalDateTime, schedule: Schedule): WindowState {
        val dumbNow = isDumbAt(now, schedule)
        val limit = now.plusDays(LOOKAHEAD_DAYS)
        var probe = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        while (probe.isBefore(limit) && isDumbAt(probe, schedule) == dumbNow) {
            probe = probe.plusHours(1)
        }
        val boundary = if (probe.isBefore(limit)) probe else null
        return WindowState(
            inForce = dumbNow,
            currentWindowEnd = if (dumbNow) boundary else null,
            nextWindowStart = if (dumbNow) null else boundary,
        )
    }

    /**
     * The 24 booleans behind the resulting-window bar, one per hour of the day.
     */
    fun dumbHoursFor(scope: WindowScope, schedule: Schedule): List<Boolean> = when (scope) {
        WindowScope.Always -> List(24) { true }
        WindowScope.EveryDay -> List(24) { schedule.dailyPreset.coversHour(it) }
        is WindowScope.Day -> List(24) { schedule.presetFor(scope.day).coversHour(it) }
    }

    /** The scope the bar is labelled with — "ALWAYS", "EVERY DAY", "MONDAY". */
    fun scopeLabel(scope: WindowScope): String = when (scope) {
        WindowScope.Always -> "ALWAYS"
        WindowScope.EveryDay -> "EVERY DAY"
        is WindowScope.Day -> scope.day.name
    }

    /** The scope implied by the current schedule and the day being edited. */
    fun scopeOf(schedule: Schedule, selectedDay: DayOfWeek): WindowScope = when {
        schedule.mode == FocusMode.AlwaysOn -> WindowScope.Always
        schedule.repeat == Repeat.Daily -> WindowScope.EveryDay
        else -> WindowScope.Day(selectedDay)
    }
}
