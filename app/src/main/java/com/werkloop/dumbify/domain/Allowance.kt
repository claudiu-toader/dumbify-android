package com.werkloop.dumbify.domain

import java.time.LocalDate

/**
 * The daily request ration.
 *
 * The allowance is a domain value, not a user setting: no screen changes
 * [PER_DAY]. That is now a choice rather than a consequence of the settings
 * screen's shape — settings grew a rules section and a numeric control for the
 * app cap, so a control for this could follow if the spec ever asked for one.
 * Every caller reads it from here precisely so that surfacing it would touch no
 * screen (app-requests "Requests are rationed per day").
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
