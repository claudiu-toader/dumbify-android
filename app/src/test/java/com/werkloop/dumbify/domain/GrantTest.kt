package com.werkloop.dumbify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class GrantTest {

    private val start: Instant = Instant.parse("2026-09-13T09:41:00Z")
    private val grant = Grant("com.example.social", start.plus(15, ChronoUnit.MINUTES))

    @Test
    fun `a grant is active up to but not including its expiry`() {
        assertTrue(grant.isActive(start))
        assertTrue(grant.isActive(start.plus(14, ChronoUnit.MINUTES).plusSeconds(59)))
        assertFalse("expiry is exclusive", grant.isActive(grant.expiresAt))
        assertFalse(grant.isActive(grant.expiresAt.plusSeconds(1)))
    }

    @Test
    fun `remaining counts down and floors at zero`() {
        assertEquals(900, grant.remainingSeconds(start))
        assertEquals(60, grant.remainingSeconds(start.plus(14, ChronoUnit.MINUTES)))
        assertEquals(0, grant.remainingSeconds(grant.expiresAt))
        assertEquals("never negative", 0, grant.remainingSeconds(grant.expiresAt.plusSeconds(600)))
    }

    @Test
    fun `expiry is absolute, so a restart neither extends nor loses it`() {
        // The prototype counts down in a setInterval. If that were persisted as
        // "seconds left", killing the process would hand back the full 15
        // minutes on the next launch. Storing the instant is what makes this
        // test possible at all (design decision 5).
        val sixMinutesIn = start.plus(9, ChronoUnit.MINUTES)
        val roundTripped = Grant(grant.packageName, Instant.ofEpochMilli(grant.expiresAt.toEpochMilli()))
        assertEquals(360, roundTripped.remainingSeconds(sixMinutesIn))
    }
}
