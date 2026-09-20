package com.werkloop.dumbify.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.di.ApplicationScope
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.WindowEvaluator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the widget honest.
 *
 * Two things change what it should say: the state it renders, and the clock
 * crossing a window boundary. The first is a flow; the second is a sleep until
 * the boundary the evaluator reports.
 *
 * Correctness never rests on the wake-up landing on time. `windowStateAt(now)`
 * is re-evaluated whenever the widget is drawn, so an OEM that delays this by
 * twenty minutes produces a late refresh, not a wrong one.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DumbifyRepository,
    private val device: DeviceData,
    private val clock: DumbClock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            repository.state
                // Only the parts the widget actually renders. Toggling the big
                // clock should not repaint someone's home screen.
                .map { Triple(it.allowedPackages, it.schedule, it.grant) }
                .distinctUntilChanged()
                .collect { DumbifyWidget().updateAll(context) }
        }
        // Both permissions are granted and revoked outside this app, and the
        // widget renders differently for each: the home role decides whether it
        // shows the list or the not-enforcing notice, and usage access decides
        // whether that notice carries figures at all. Neither arrives as a
        // callback — `DeviceData.refresh()` re-reads them on the next resume,
        // and these collectors turn that into a repaint. Without them the
        // widget keeps showing a pre-grant render until the half-hour tick.
        //
        // No distinctUntilChanged: a StateFlow already conflates, and the
        // operator is deprecated on one.
        scope.launch {
            combine(device.isDefaultHome, device.hasUsageAccess) { home, usage -> home to usage }
                .distinctUntilChanged()
                .collect { DumbifyWidget().updateAll(context) }
        }
        scope.launch {
            while (true) {
                val state = repository.state.value
                val window = WindowEvaluator.windowStateAt(clock.localNow(), state.schedule)
                val boundary = window.currentWindowEnd ?: window.nextWindowStart
                val wait = boundary
                    ?.let { Duration.between(clock.localNow(), it) }
                    ?.coerceAtLeast(Duration.ofMinutes(1))
                    // Nothing scheduled and nothing to end: check back in an
                    // hour in case the schedule changed under us.
                    ?: Duration.ofHours(1)
                delay(wait.plus(1, ChronoUnit.SECONDS).toMillis())
                DumbifyWidget().updateAll(context)
            }
        }
    }
}
