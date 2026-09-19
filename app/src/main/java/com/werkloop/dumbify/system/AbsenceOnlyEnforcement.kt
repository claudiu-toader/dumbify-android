package com.werkloop.dumbify.system

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only enforcement this build ships.
 *
 * It holds no state and touches nothing: enforcement *is* the launcher not
 * drawing a row. The three Android mechanisms that could block an app outright
 * are all wrong here — an `AccessibilityService` is a sensitive permission with
 * real store-review risk, device-owner mode needs a factory reset to provision,
 * and `UsageStatsManager` is read-only. This is a stated shortfall, not an
 * oversight (design decision 1).
 */
@Singleton
class AbsenceOnlyEnforcement @Inject constructor() : EnforcementController {
    override val model = EnforcementModel.AbsenceOnly

    override fun isEnforcing(windowInForce: Boolean, setupComplete: Boolean): Boolean =
        setupComplete && windowInForce
}
