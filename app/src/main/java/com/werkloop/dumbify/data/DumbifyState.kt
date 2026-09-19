package com.werkloop.dumbify.data

import androidx.compose.runtime.Immutable
import com.werkloop.dumbify.domain.AppSettings
import com.werkloop.dumbify.domain.Grant
import com.werkloop.dumbify.domain.PendingRemoval
import com.werkloop.dumbify.domain.RequestLedger
import com.werkloop.dumbify.domain.Schedule
import java.time.LocalDate

/**
 * Everything Dumbify remembers, in one object.
 *
 * One flat state read by every screen at once beats per-key reads: it is what
 * makes a derived count unable to tear against the list it came from
 * (design decision 12).
 */
@Immutable
data class DumbifyState(
    val setupComplete: Boolean = false,
    /** The day setup finished — the cut-off the pre-Dumbify baseline is measured before. */
    val setupCompletedOn: LocalDate? = null,
    val allowedPackages: Set<String> = emptySet(),
    val schedule: Schedule = Schedule(),
    val settings: AppSettings = AppSettings(),
    val grant: Grant? = null,
    val ledger: RequestLedger? = null,
    val pendingRemoval: PendingRemoval? = null,
)
