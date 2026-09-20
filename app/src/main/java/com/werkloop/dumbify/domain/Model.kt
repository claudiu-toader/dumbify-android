package com.werkloop.dumbify.domain

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * The domain model. Pure Kotlin — nothing here imports from Android, so the
 * whole of it is exercised on the JVM (design decision 10).
 */

/** One launchable app on the device. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    /** A coarse label shown in the picker — "SOCIAL", "SYSTEM", "MEDIA". */
    val category: String,
)

enum class FocusMode { AlwaysOn, Scheduled }

enum class Repeat { Daily, PerDay }

/**
 * A window preset, stored as a list of half-open `[startHour, endHour)` pairs
 * **within one calendar day**.
 *
 * The overnight preset is pre-split at midnight — `[[21,24],[0,7]]` — so no
 * evaluator ever has to handle a range that wraps. That is the whole reason
 * midnight-crossing windows are not a special case anywhere in this file
 * (design decision 6).
 */
@Serializable
enum class WindowPreset(val label: String, val ranges: List<HourRange>) {
    Off("OFF", emptyList()),
    WorkingHours("09:00–18:00", listOf(HourRange(9, 18))),
    Evening("18:00–23:00", listOf(HourRange(18, 23))),
    Overnight("21:00–07:00", listOf(HourRange(21, 24), HourRange(0, 7))),
    AllDay("ALL DAY", listOf(HourRange(0, 24)));

    /** The next preset in the fixed order, wrapping from the last back to [Off]. */
    fun next(): WindowPreset = entries[(ordinal + 1) % entries.size]

    fun coversHour(hour: Int): Boolean = ranges.any { it.contains(hour) }
}

/** A half-open range of hours within a single calendar day. */
@Serializable
data class HourRange(val startHour: Int, val endHourExclusive: Int) {
    fun contains(hour: Int): Boolean = hour >= startHour && hour < endHourExclusive
}

/**
 * When the phone is dumb.
 *
 * [perDay] is retained even while [repeat] is [Repeat.Daily], so switching to
 * day-by-day and back does not discard the per-day windows
 * (focus-schedule "Switching back to daily").
 */
@Serializable
data class Schedule(
    val mode: FocusMode = FocusMode.Scheduled,
    val repeat: Repeat = Repeat.Daily,
    val dailyPreset: WindowPreset = WindowPreset.WorkingHours,
    val perDay: Map<@Serializable(with = DayOfWeekSerializer::class) DayOfWeek, WindowPreset> = DEFAULT_PER_DAY,
) {
    /** The preset governing one weekday, ignoring [mode]. */
    fun presetFor(day: DayOfWeek): WindowPreset = when (repeat) {
        Repeat.Daily -> dailyPreset
        Repeat.PerDay -> perDay[day] ?: WindowPreset.Off
    }

    companion object {
        /**
         * The design opens on Scheduled / Every day / 09:00–18:00 — it shows
         * the user what a window is before asking them to choose, while the
         * copy recommends always on (focus-schedule "Initial selection").
         */
        val DEFAULT_PER_DAY: Map<DayOfWeek, WindowPreset> = mapOf(
            DayOfWeek.MONDAY to WindowPreset.WorkingHours,
            DayOfWeek.TUESDAY to WindowPreset.WorkingHours,
            DayOfWeek.WEDNESDAY to WindowPreset.WorkingHours,
            DayOfWeek.THURSDAY to WindowPreset.WorkingHours,
            DayOfWeek.FRIDAY to WindowPreset.WorkingHours,
            DayOfWeek.SATURDAY to WindowPreset.Off,
            DayOfWeek.SUNDAY to WindowPreset.Off,
        )
    }
}

/**
 * A temporary grant into a hidden app.
 *
 * The expiry is absolute, never a countdown: the prototype's per-second
 * `setInterval` is a browser artefact, and storing "seconds left" would extend
 * every grant across process death (design decision 5).
 */
@Serializable
data class Grant(
    val packageName: String,
    @Serializable(with = InstantSerializer::class) val expiresAt: Instant,
) {
    fun isActive(now: Instant): Boolean = now.isBefore(expiresAt)

    /** Seconds left, floored at zero. */
    fun remainingSeconds(now: Instant): Long =
        (expiresAt.epochSecond - now.epochSecond).coerceAtLeast(0)

    companion object {
        /** Every grant is the same length — app-requests "time-boxed". */
        const val DURATION_MINUTES: Long = 15
    }
}

/** Requests spent on one local day. Resets by virtue of the date changing. */
@Serializable
data class RequestLedger(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val used: Int,
)

/** A removal the user has started but not yet confirmed. */
@Serializable
data class PendingRemoval(
    @Serializable(with = InstantSerializer::class) val confirmableAt: Instant,
) {
    fun isConfirmable(now: Instant): Boolean = !now.isBefore(confirmableAt)

    companion object {
        const val DELAY_HOURS: Long = 24
    }
}

/**
 * The five switches, in the order the settings screen shows them.
 *
 * [darkTheme] is deliberately nullable: `null` means "follow the device", which
 * is what the switch reports until the user touches it. Once touched it holds a
 * `Boolean` the device setting no longer overrides (settings "Ground selection").
 */
@Serializable
data class AppSettings(
    val darkTheme: Boolean? = null,
    /**
     * The most apps that may be allowed at once, chosen by the user on the
     * allowlist step and editable from settings. Defaults to the six the old
     * guidance advised (see [AppCap]).
     */
    val maxApps: Int = AppCap.DEFAULT,
    val bigClock: Boolean = true,
    val greyscale: Boolean = false,
    val notificationDigest: Boolean = false,
    val removalDelay: Boolean = true,
)
