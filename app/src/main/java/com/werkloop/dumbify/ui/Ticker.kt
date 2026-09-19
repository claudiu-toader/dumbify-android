package com.werkloop.dumbify.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A one-second heartbeat.
 *
 * Every screen that shows it collects with `collectAsStateWithLifecycle`, so
 * this stops the moment the app is backgrounded — which is the whole reason
 * design decision 12 forbids bare `collectAsState()` here. A focus app that
 * ticked behind the lock screen would be its own worst advertisement.
 *
 * The `while (true)` is bounded by its collector, not by a condition: `delay`
 * suspends rather than spins and is cancellable, and `flow { }` is cold, so the
 * loop does not exist until something subscribes and unwinds when that
 * subscription is cancelled.
 *
 * Shared by `LauncherViewModel` — for the clock and the grant countdown — and
 * by `SettingsViewModel`, for the removal countdown. It sits beside [Fmt]
 * rather than in `domain`: that package is pure functions with no coroutine
 * dependency, and a `Flow` would be the first.
 */
internal fun seconds(): Flow<Long> = flow {
    var n = 0L
    while (true) {
        emit(n++)
        delay(1_000)
    }
}
