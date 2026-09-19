package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.WindowEvaluator
import com.werkloop.dumbify.domain.allowedApps
import com.werkloop.dumbify.system.HomeRoleController
import com.werkloop.dumbify.system.SystemNavigator
import com.werkloop.dumbify.ui.Fmt
import com.werkloop.dumbify.ui.seconds
import com.werkloop.dumbify.ui.screens.LauncherRow
import com.werkloop.dumbify.ui.screens.LauncherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    private val device: DeviceData,
    private val navigator: SystemNavigator,
    private val homeRole: HomeRoleController,
    private val clock: DumbClock,
) : ViewModel() {

    val state: StateFlow<LauncherUiState> = combine(
        device.apps, device.today, repository.state, device.isDefaultHome, seconds(),
    ) { apps, usage, saved, isHome, _ ->
        val now = clock.now()
        val localNow = clock.localNow()
        val window = WindowEvaluator.windowStateAt(localNow, saved.schedule)
        val grant = saved.grant?.takeIf { it.isActive(now) }

        val rows = allowedApps(apps, saved.allowedPackages).map {
            LauncherRow(
                packageName = it.packageName,
                name = it.label,
                meta = Fmt.duration(usage?.perPackage?.get(it.packageName)),
            )
        } + listOfNotNull(
            // The grant is appended, never sorted in: it is visibly a temporary
            // exception rather than a member of the list.
            grant?.let { active ->
                LauncherRow(
                    packageName = active.packageName,
                    name = apps.firstOrNull { it.packageName == active.packageName }?.label
                        ?: active.packageName,
                    meta = Fmt.countdown(active.remainingSeconds(now)),
                    granted = true,
                )
            }
        )

        LauncherUiState(
            dateLine = buildString {
                append(Fmt.shortDate(localNow))
                append(" · ")
                append(
                    when {
                        !window.inForce ->
                            window.nextWindowStart
                                ?.let { "NEXT WINDOW ${Fmt.boundary(it)}" }
                                ?: "NOT IN FORCE"
                        // Always on has no end to state, so it states none.
                        window.currentWindowEnd == null -> "DUMB"
                        else -> "DUMB UNTIL ${Fmt.boundary(window.currentWindowEnd)}"
                    }
                )
            },
            clock = Fmt.clock(localNow),
            apps = rows,
            todayTotal = Fmt.total(usage?.total),
            bigClock = saved.settings.bigClock,
            isDefaultHome = isHome,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        LauncherUiState("", Fmt.clock(clock.localNow()), emptyList(), Fmt.DASH),
    )

    init {
        // An expired grant is cleared rather than merely hidden, so the app
        // cannot come back by itself after a restart.
        viewModelScope.launch {
            seconds().collect {
                val grant = repository.state.value.grant ?: return@collect
                if (!grant.isActive(clock.now())) repository.setGrant(null)
            }
        }
    }

    fun open(packageName: String) {
        if (!navigator.launchApp(packageName)) device.refresh()
    }

    /**
     * Put Dumbify back on the Home button.
     *
     * Re-enables the alias first — it may have been switched off by a previous
     * "stop enforcing", and a picker that does not list Dumbify would be a dead
     * end. Only the user can actually assign the role, so this ends at the
     * system picker rather than claiming success; the result is read back by
     * `DeviceData.refresh()` on the next resume (design decision 8).
     */
    fun makeHome() {
        if (!homeRole.isOfferedAsHome()) homeRole.setOfferedAsHome(true)
        navigator.openDefaultHomeSettings()
    }
}
