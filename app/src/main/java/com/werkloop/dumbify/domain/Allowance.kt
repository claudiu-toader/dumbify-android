package com.werkloop.dumbify.domain

import java.time.LocalDate

/**
 * The daily request ration.
 *
 * The allowance is a domain value, not a user setting: the settings screen is
 * five switches and no numeric control, so nothing in the app changes
 * [PER_DAY]. It is read from here by every caller precisely so that surfacing
 * it later touches no screen (app-requests "Requests are rationed per day").
 */
object Allowance {

    const val PER_DAY: Int = 3

    /**
     * Requests left today. A ledger from any earlier date counts as unused —
     * that *is* the midnight reset, and it needs no scheduled work to happen.
     */
    fun remaining(ledger: RequestLedger?, today: LocalDate, perDay: Int = PER_DAY): Int {
        val used = if (ledger?.date == today) ledger.used else 0
        return (perDay - used).coerceIn(0, perDay)
    }

    fun canGrant(ledger: RequestLedger?, today: LocalDate, perDay: Int = PER_DAY): Boolean =
        remaining(ledger, today, perDay) > 0

    /** The ledger after spending one request. Callers must check [canGrant] first. */
    fun spend(ledger: RequestLedger?, today: LocalDate): RequestLedger =
        if (ledger?.date == today) ledger.copy(used = ledger.used + 1)
        else RequestLedger(today, 1)

    /** Requests used today, for the statistics counter's "N of M". */
    fun used(ledger: RequestLedger?, today: LocalDate): Int =
        if (ledger?.date == today) ledger.used else 0
}
