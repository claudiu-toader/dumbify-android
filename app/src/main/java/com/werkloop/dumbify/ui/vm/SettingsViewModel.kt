package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.PendingRemoval
import com.werkloop.dumbify.system.HomeRoleController
import com.werkloop.dumbify.system.SystemNavigator
import com.werkloop.dumbify.ui.Fmt
import com.werkloop.dumbify.ui.seconds
import com.werkloop.dumbify.ui.screens.SettingKey
import com.werkloop.dumbify.ui.screens.SettingToggle
import com.werkloop.dumbify.ui.screens.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    private val navigator: SystemNavigator,
    private val homeRole: HomeRoleController,
    private val clock: DumbClock,
) : ViewModel() {

    private val greyscaleNotice = MutableStateFlow(false)
    private val confirming = MutableStateFlow(false)

    // Owned by PackageManager, not by us — read once here and re-read after
    // every change rather than mirrored into the repository.
    private val offeredAsHome = MutableStateFlow(homeRole.isOfferedAsHome())

    val state: StateFlow<SettingsUiState> = combine(
        repository.state, greyscaleNotice, confirming, offeredAsHome, seconds(),
    ) { saved, notice, confirmDialog, isHome, _ ->
        val settings = saved.settings
        val pending = saved.pendingRemoval
        val remaining = pending?.let { Duration.between(clock.now(), it.confirmableAt) }
        SettingsUiState(
            toggles = listOf(
                SettingToggle(
                    SettingKey.DarkTheme, "Dark theme",
                    "Paper ground inverts to steel-on-black.",
                    settings.darkTheme ?: false,
                ),
                SettingToggle(
                    SettingKey.BigClock, "Big clock",
                    "The time is the largest thing on the launcher.",
                    settings.bigClock,
                ),
                SettingToggle(
                    SettingKey.Greyscale, "Force greyscale",
                    "Colour drains from Dumbify while it is on.",
                    settings.greyscale,
                ),
                SettingToggle(
                    SettingKey.NotificationDigest, "Notification digest",
                    "Held and delivered once at 20:00.",
                    settings.notificationDigest,
                ),
                SettingToggle(
                    SettingKey.RemovalDelay, "Removal delay",
                    "Turning Dumbify off takes 24 hours to take effect.",
                    settings.removalDelay,
                ),
            ),
            removalCountdown = remaining?.let { Fmt.coarseRemaining(it) },
            removalConfirmable = pending?.isConfirmable(clock.now()) == true,
            confirmingRemoval = confirmDialog,
            greyscaleNotice = notice && settings.greyscale,
            offeredAsHome = isHome,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(emptyList()))

    fun toggle(key: SettingKey, on: Boolean) = viewModelScope.launch {
        val current = repository.state.value.settings
        repository.setSettings(
            when (key) {
                // Writing a non-null value here is what stops the device's own
                // setting overriding the user's choice from now on.
                SettingKey.DarkTheme -> current.copy(darkTheme = on)
                SettingKey.BigClock -> current.copy(bigClock = on)
                SettingKey.Greyscale -> current.copy(greyscale = on)
                SettingKey.NotificationDigest -> current.copy(notificationDigest = on)
                SettingKey.RemovalDelay -> current.copy(removalDelay = on)
            }
        )
        if (key == SettingKey.Greyscale) greyscaleNotice.value = on
    }

    fun openColorCorrection() = navigator.openColorCorrectionSettings()

    /**
     * Removal is a two-step with a day in between — unless the user has turned
     * the delay off, in which case one confirmation is enough
     * (settings "Removal delay disabled").
     */
    fun startRemoval() = viewModelScope.launch {
        val saved = repository.state.value
        when {
            !saved.settings.removalDelay -> confirming.value = true
            saved.pendingRemoval == null ->
                repository.setPendingRemoval(
                    PendingRemoval(clock.now().plus(PendingRemoval.DELAY_HOURS, ChronoUnit.HOURS))
                )
            saved.pendingRemoval.isConfirmable(clock.now()) -> confirming.value = true
        }
    }

    fun cancelRemoval() = viewModelScope.launch {
        repository.setPendingRemoval(null)
        confirming.value = false
    }

    /**
     * Enforcement actually stops here.
     *
     * Disabling the home alias is the part that matters: until this call
     * Dumbify stayed on the Home button no matter what the button said, because
     * there is no API to hand `ROLE_HOME` back (settings "Stopping releases the
     * home role"). The allowlist, schedule and setup flag are all kept, so
     * turning it back on is not a second setup.
     */
    fun confirmRemoval() = viewModelScope.launch {
        homeRole.setOfferedAsHome(false)
        offeredAsHome.value = homeRole.isOfferedAsHome()
        repository.setPendingRemoval(null)
        repository.setGrant(null)
        confirming.value = false
    }

    fun dismissRemovalDialog() { confirming.value = false }

    /**
     * Back into the home-app list.
     *
     * Being a candidate is not the same as holding the role — the user still
     * has to choose Dumbify — so this opens the picker straight after.
     */
    fun resumeAsHome() {
        homeRole.setOfferedAsHome(true)
        offeredAsHome.value = homeRole.isOfferedAsHome()
        navigator.openDefaultHomeSettings()
    }
}
