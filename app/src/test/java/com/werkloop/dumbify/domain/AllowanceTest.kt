package com.werkloop.dumbify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AllowanceTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 13)
    private val yesterday: LocalDate = today.minusDays(1)

    @Test
    fun `a fresh day starts at the full allowance`() {
        assertEquals(3, Allowance.remaining(null, today))
        assertTrue(Allowance.canGrant(null, today))
    }

    @Test
    fun `spending decrements`() {
        var ledger = Allowance.spend(null, today)
        assertEquals(2, Allowance.remaining(ledger, today))
        ledger = Allowance.spend(ledger, today)
        assertEquals(1, Allowance.remaining(ledger, today))
        ledger = Allowance.spend(ledger, today)
        assertEquals(0, Allowance.remaining(ledger, today))
        assertFalse("exhausted means exhausted", Allowance.canGrant(ledger, today))
    }

    @Test
    fun `the reset is the date changing, not a scheduled job`() {
        val exhausted = RequestLedger(yesterday, 3)
        assertEquals(0, Allowance.remaining(exhausted, yesterday))
        assertEquals(3, Allowance.remaining(exhausted, today))
        assertTrue(Allowance.canGrant(exhausted, today))
    }

    @Test
    fun `spending on a new day starts a new ledger rather than adding to the old`() {
        val spent = Allowance.spend(RequestLedger(yesterday, 3), today)
        assertEquals(today, spent.date)
        assertEquals(1, spent.used)
    }

    @Test
    fun `remaining never goes negative or above the allowance`() {
        assertEquals(0, Allowance.remaining(RequestLedger(today, 99), today))
        assertEquals(3, Allowance.remaining(RequestLedger(today, -5), today))
    }

    @Test
    fun `an allowance of zero can never grant`() {
        assertEquals(0, Allowance.remaining(null, today, perDay = 0))
        assertFalse(Allowance.canGrant(null, today, perDay = 0))
    }

    @Test
    fun `used is what the statistics counter reports`() {
        assertEquals(0, Allowance.used(null, today))
        assertEquals(0, Allowance.used(RequestLedger(yesterday, 3), today))
        assertEquals(2, Allowance.used(RequestLedger(today, 2), today))
    }
}
