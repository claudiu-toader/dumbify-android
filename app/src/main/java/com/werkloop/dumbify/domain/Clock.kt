package com.werkloop.dumbify.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Time, injectable.
 *
 * This wraps `java.time.Clock` rather than replacing it — the platform type
 * already carries both the instant and the zone, and `Clock.fixed` is a
 * ready-made test double. What it adds is the two projections every caller
 * actually wants, so that no screen or repository reaches for
 * `LocalDateTime.now()` and quietly becomes untestable.
 */
interface DumbClock {
    fun now(): Instant
    fun zone(): ZoneId

    fun localNow(): LocalDateTime = LocalDateTime.ofInstant(now(), zone())
    fun today(): LocalDate = localNow().toLocalDate()
}

class SystemDumbClock(
    private val delegate: java.time.Clock = java.time.Clock.systemDefaultZone(),
) : DumbClock {
    override fun now(): Instant = delegate.instant()
    override fun zone(): ZoneId = delegate.zone
}
