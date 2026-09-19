package com.werkloop.dumbify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class WindowEvaluatorTest {

    /** A Monday, so `plusDays(n)` walks the week in order. */
    private val monday: LocalDateTime = LocalDateTime.of(2026, 9, 7, 0, 0)

    private fun daily(preset: WindowPreset) = Schedule(
        mode = FocusMode.Scheduled,
        repeat = Repeat.Daily,
        dailyPreset = preset,
    )

    private fun perDay(vararg pairs: Pair<DayOfWeek, WindowPreset>) = Schedule(
        mode = FocusMode.Scheduled,
        repeat = Repeat.PerDay,
        perDay = pairs.toMap(),
    )

    // ── the sweep ───────────────────────────────────────────────────────────
    // Every preset, both repeat modes, every hour of every day of one week.
    // The point is not the individual assertion but that `isDumbAt` and the
    // preset's own `coversHour` can never disagree — which is what keeps the
    // 24-hour bar honest against the launcher header.

    @Test
    fun `daily repeat applies the same preset to all seven days`() {
        for (preset in WindowPreset.entries) {
            val schedule = daily(preset)
            for (dayOffset in 0..6) {
                for (hour in 0..23) {
                    val at = monday.plusDays(dayOffset.toLong()).withHour(hour)
                    assertEquals(
                        "preset=$preset day=$dayOffset hour=$hour",
                        preset.coversHour(hour),
                        WindowEvaluator.isDumbAt(at, schedule),
                    )
                }
            }
        }
    }

    @Test
    fun `per-day repeat applies each day its own preset`() {
        val presets = DayOfWeek.entries.associateWith {
            WindowPreset.entries[it.ordinal % WindowPreset.entries.size]
        }
        val schedule = perDay(*presets.toList().toTypedArray())
        for (dayOffset in 0..6) {
            for (hour in 0..23) {
                val at = monday.plusDays(dayOffset.toLong()).withHour(hour)
                assertEquals(
                    "day=${at.dayOfWeek} hour=$hour",
                    presets.getValue(at.dayOfWeek).coversHour(hour),
                    WindowEvaluator.isDumbAt(at, schedule),
                )
            }
        }
    }

    @Test
    fun `a day missing from the per-day map is off, not dumb`() {
        val schedule = perDay(DayOfWeek.MONDAY to WindowPreset.AllDay)
        for (hour in 0..23) {
            assertFalse(WindowEvaluator.isDumbAt(monday.plusDays(1).withHour(hour), schedule))
        }
    }

    // ── boundaries ──────────────────────────────────────────────────────────

    @Test
    fun `working hours is half-open at both ends`() {
        val schedule = daily(WindowPreset.WorkingHours)
        assertFalse("08:59 is not yet dumb", WindowEvaluator.isDumbAt(monday.withHour(8), schedule))
        assertTrue("09:00 is dumb", WindowEvaluator.isDumbAt(monday.withHour(9), schedule))
        assertTrue("17:00 is still dumb", WindowEvaluator.isDumbAt(monday.withHour(17), schedule))
        assertFalse("18:00 is no longer dumb", WindowEvaluator.isDumbAt(monday.withHour(18), schedule))
    }

    @Test
    fun `window end is found at the top of the closing hour`() {
        val state = WindowEvaluator.windowStateAt(monday.withHour(10).withMinute(30), daily(WindowPreset.WorkingHours))
        assertTrue(state.inForce)
        assertEquals(monday.withHour(18), state.currentWindowEnd)
        assertNull(state.nextWindowStart)
    }

    @Test
    fun `next window start is found from outside`() {
        val state = WindowEvaluator.windowStateAt(monday.withHour(7).withMinute(5), daily(WindowPreset.WorkingHours))
        assertFalse(state.inForce)
        assertEquals(monday.withHour(9), state.nextWindowStart)
        assertNull(state.currentWindowEnd)
    }

    // ── midnight ────────────────────────────────────────────────────────────

    @Test
    fun `overnight is pre-split, so both halves of the same day are dumb`() {
        val schedule = daily(WindowPreset.Overnight)
        assertTrue(WindowEvaluator.isDumbAt(monday.withHour(22), schedule))
        assertTrue(WindowEvaluator.isDumbAt(monday.withHour(3), schedule))
        assertFalse(WindowEvaluator.isDumbAt(monday.withHour(7), schedule))
        assertFalse(WindowEvaluator.isDumbAt(monday.withHour(20), schedule))
    }

    @Test
    fun `an overnight window crossing midnight ends on the following morning`() {
        // 22:00 Monday. The range is [21,24) today, but Tuesday opens [0,7):
        // the window does not actually close at midnight, and the launcher must
        // not claim it does.
        val state = WindowEvaluator.windowStateAt(monday.withHour(22), daily(WindowPreset.Overnight))
        assertTrue(state.inForce)
        assertEquals(monday.plusDays(1).withHour(7), state.currentWindowEnd)
    }

    @Test
    fun `a window that opens tomorrow is found across the day boundary`() {
        val state = WindowEvaluator.windowStateAt(monday.withHour(19), daily(WindowPreset.Overnight))
        assertFalse(state.inForce)
        assertEquals(monday.withHour(21), state.nextWindowStart)
    }

    @Test
    fun `an all-day preset never closes, even across days`() {
        val state = WindowEvaluator.windowStateAt(monday.withHour(13), daily(WindowPreset.AllDay))
        assertTrue(state.inForce)
        assertNull("a permanently dumb week has no end to report", state.currentWindowEnd)
    }

    @Test
    fun `a single dumb day reports its end and the next start`() {
        val schedule = perDay(DayOfWeek.WEDNESDAY to WindowPreset.AllDay)
        val wednesday = monday.plusDays(2)
        val inside = WindowEvaluator.windowStateAt(wednesday.withHour(12), schedule)
        assertTrue(inside.inForce)
        assertEquals(wednesday.plusDays(1).withHour(0), inside.currentWindowEnd)

        val outside = WindowEvaluator.windowStateAt(monday.withHour(12), schedule)
        assertFalse(outside.inForce)
        assertEquals(wednesday.withHour(0), outside.nextWindowStart)
    }

    // ── always on / never on ────────────────────────────────────────────────

    @Test
    fun `always on ignores the presets entirely`() {
        val schedule = Schedule(mode = FocusMode.AlwaysOn, dailyPreset = WindowPreset.Off)
        for (dayOffset in 0..6) {
            for (hour in 0..23) {
                assertTrue(WindowEvaluator.isDumbAt(monday.plusDays(dayOffset.toLong()).withHour(hour), schedule))
            }
        }
        val state = WindowEvaluator.windowStateAt(monday.withHour(3), schedule)
        assertTrue(state.inForce)
        assertNull("always on has no end time to state", state.currentWindowEnd)
        assertNull(state.nextWindowStart)
    }

    @Test
    fun `every preset off means no window ever opens`() {
        val state = WindowEvaluator.windowStateAt(monday.withHour(3), daily(WindowPreset.Off))
        assertFalse(state.inForce)
        assertNull("nothing to count down to", state.nextWindowStart)
    }

    // ── the bar ─────────────────────────────────────────────────────────────

    @Test
    fun `the bar always has twenty-four hours`() {
        for (preset in WindowPreset.entries) {
            assertEquals(24, WindowEvaluator.dumbHoursFor(WindowScope.EveryDay, daily(preset)).size)
        }
    }

    @Test
    fun `the bar agrees with the evaluator hour for hour`() {
        for (preset in WindowPreset.entries) {
            val schedule = daily(preset)
            val bar = WindowEvaluator.dumbHoursFor(WindowScope.EveryDay, schedule)
            for (hour in 0..23) {
                assertEquals(
                    "preset=$preset hour=$hour",
                    WindowEvaluator.isDumbAt(monday.withHour(hour), schedule),
                    bar[hour],
                )
            }
        }
    }

    @Test
    fun `always on shades the whole bar`() {
        val bar = WindowEvaluator.dumbHoursFor(WindowScope.Always, daily(WindowPreset.Off))
        assertTrue(bar.all { it })
        assertEquals("ALWAYS", WindowEvaluator.scopeLabel(WindowScope.Always))
    }

    @Test
    fun `the bar reads the selected day in per-day mode`() {
        val schedule = perDay(
            DayOfWeek.MONDAY to WindowPreset.AllDay,
            DayOfWeek.TUESDAY to WindowPreset.Off,
        )
        assertTrue(WindowEvaluator.dumbHoursFor(WindowScope.Day(DayOfWeek.MONDAY), schedule).all { it })
        assertTrue(WindowEvaluator.dumbHoursFor(WindowScope.Day(DayOfWeek.TUESDAY), schedule).none { it })
        assertEquals("MONDAY", WindowEvaluator.scopeLabel(WindowScope.Day(DayOfWeek.MONDAY)))
        assertEquals("EVERY DAY", WindowEvaluator.scopeLabel(WindowScope.EveryDay))
    }

    @Test
    fun `scope follows mode and repeat`() {
        assertEquals(
            WindowScope.Always,
            WindowEvaluator.scopeOf(Schedule(mode = FocusMode.AlwaysOn), DayOfWeek.FRIDAY),
        )
        assertEquals(
            WindowScope.EveryDay,
            WindowEvaluator.scopeOf(daily(WindowPreset.Evening), DayOfWeek.FRIDAY),
        )
        assertEquals(
            WindowScope.Day(DayOfWeek.FRIDAY),
            WindowEvaluator.scopeOf(perDay(), DayOfWeek.FRIDAY),
        )
    }

    // ── preset cycling ──────────────────────────────────────────────────────

    @Test
    fun `cycling wraps from the last preset back to off`() {
        assertEquals(WindowPreset.WorkingHours, WindowPreset.Off.next())
        assertEquals(WindowPreset.Off, WindowPreset.AllDay.next())
        // One full lap returns to where it started, for every entry.
        for (preset in WindowPreset.entries) {
            var p = preset
            repeat(WindowPreset.entries.size) { p = p.next() }
            assertEquals(preset, p)
        }
    }

    @Test
    fun `per-day windows survive a switch to daily and back`() {
        val edited = perDay(DayOfWeek.SATURDAY to WindowPreset.Evening)
        val toDaily = edited.copy(repeat = Repeat.Daily)
        assertEquals(WindowPreset.Evening, toDaily.copy(repeat = Repeat.PerDay).presetFor(DayOfWeek.SATURDAY))
    }
}
