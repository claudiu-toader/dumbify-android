package com.werkloop.dumbify.ui

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every number the user reads, formatted in one place.
 *
 * The launcher, the widget, the statistics screen and the summary all quote the
 * same figures; formatting them separately is how "41m" and "0h 41m" end up on
 * two surfaces at once.
 */
object Fmt {

    /** "41m", "3h 12m", or a neutral dash when there is nothing to report. */
    fun duration(value: Duration?): String {
        if (value == null) return DASH
        val minutes = value.toMinutes()
        if (minutes <= 0) return DASH
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours > 0) "${hours}h ${rest}m" else "${minutes}m"
    }

    /** The same, but zero reads as "0m" — a total of nothing is still a total. */
    fun total(value: Duration?): String {
        if (value == null) return "—"
        val minutes = value.toMinutes()
        if (minutes <= 0) return "0m"
        return duration(value)
    }

    /** "9:41" — no leading zero, matching the design. */
    fun clock(at: LocalDateTime): String = at.format(CLOCK)

    /** "TUE 2 SEP" */
    fun shortDate(at: LocalDateTime): String = at.format(SHORT_DATE).uppercase(Locale.getDefault())

    /** "TUESDAY 2 SEPTEMBER" — the widget's longer header. */
    fun longDate(at: LocalDateTime): String = at.format(LONG_DATE).uppercase(Locale.getDefault())

    /** "21:00" — a window boundary, always 24-hour so the label is unambiguous. */
    fun boundary(at: LocalDateTime): String = at.format(BOUNDARY)

    /** "12:03 LEFT" — a grant's remaining time, counting down by the second. */
    fun countdown(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        return "%d:%02d LEFT".format(safe / 60, safe % 60)
    }

    /** "23h 04m to go" — the removal delay, which is coarse by design. */
    fun coarseRemaining(value: Duration): String {
        val minutes = value.toMinutes().coerceAtLeast(0)
        return "%dh %02dm".format(minutes / 60, minutes % 60)
    }

    const val DASH = "—"

    private val CLOCK = DateTimeFormatter.ofPattern("H:mm")
    private val BOUNDARY = DateTimeFormatter.ofPattern("HH:mm")
    private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM")
    private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM")
}
