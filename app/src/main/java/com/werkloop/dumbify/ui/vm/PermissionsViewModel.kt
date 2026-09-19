package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.system.SystemNavigator
import com.werkloop.dumbify.ui.screens.PermissionRow
import com.werkloop.dumbify.ui.screens.PermissionsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val device: DeviceData,
    private val navigator: SystemNavigator,
) : ViewModel() {

    val state: StateFlow<PermissionsUiState> = combine(
        device.hasUsageAccess, device.isDefaultHome, device.canPostNotifications,
    ) { usage, home, notifications ->
        PermissionsUiState(
            listOf(
                PermissionRow(
                    KEY_USAGE, "Usage access",
                    "Reads the installed list and how long each app was open today. " +
                        "Nothing leaves the phone.",
                    "REQUIRED", usage,
                ),
                PermissionRow(
                    KEY_HOME, "Default home app",
                    "Puts the list where the icons were. Apps you hide are absent from " +
                        "Dumbify — they are not blocked elsewhere on the phone.",
                    "REQUIRED", home,
                ),
                PermissionRow(
                    KEY_NOTIFICATIONS, "Notifications",
                    "One digest at 20:00. Nothing else.",
                    "OPTIONAL", notifications,
                ),
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PermissionsUiState(emptyList()))

    /**
     * The Settings route for each grant — the fallback once a system dialog
     * has been declined, and the only route for usage access
     * (see `rememberGrantRequest`).
     *
     * Each of these is a round-trip to a system surface the user can abandon.
     * Nothing is recorded here — the state comes back from
     * [DeviceData.refresh] on the next resume (design decision 8).
     */
    fun grant(key: String) = when (key) {
        KEY_USAGE -> navigator.openUsageAccessSettings()
        KEY_HOME -> navigator.openDefaultHomeSettings()
        else -> navigator.openNotificationSettings()
    }

    companion object {
        const val KEY_USAGE = "usage"
        const val KEY_HOME = "home"
        const val KEY_NOTIFICATIONS = "notif"
    }
}
