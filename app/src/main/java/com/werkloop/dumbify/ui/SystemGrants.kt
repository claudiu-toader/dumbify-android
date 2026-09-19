package com.werkloop.dumbify.ui

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.werkloop.dumbify.ui.screens.PermissionsUiState
import com.werkloop.dumbify.ui.vm.PermissionsViewModel.Companion.KEY_HOME
import com.werkloop.dumbify.ui.vm.PermissionsViewModel.Companion.KEY_NOTIFICATIONS

/**
 * The Grant buttons, routed through the system's own dialog where one exists.
 *
 * The home role and notifications both have one; usage access has none and
 * always goes to Settings. Once a user has declined a dialog twice Android stops
 * showing it and reports a denial without appearing, and nothing tells the two
 * apart — so a declined dialog is not offered again, and the next tap on that
 * row goes to [openSettings], which always works. The launchers live here
 * rather than in a ViewModel because they need the Activity; the outcome is
 * still read back on resume, not from these callbacks (design decision 8).
 */
@Composable
fun rememberGrantRequest(
    state: PermissionsUiState,
    openSettings: (String) -> Unit,
): (String) -> Unit {
    val context = LocalContext.current
    var homeDeclined by rememberSaveable { mutableStateOf(false) }
    var notificationsDeclined by rememberSaveable { mutableStateOf(false) }

    val homeRole = rememberLauncherForActivityResult(StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_OK) homeDeclined = true
    }
    val notifications = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (!granted) notificationsDeclined = true
    }

    return { key ->
        val granted = state.permissions.firstOrNull { it.key == key }?.granted == true
        when {
            // A held grant has no dialog left to show; Settings is where it changes.
            granted -> openSettings(key)
            key == KEY_HOME && !homeDeclined -> {
                val roles = context.getSystemService(RoleManager::class.java)
                if (roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                    try {
                        homeRole.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    } catch (_: ActivityNotFoundException) {
                        openSettings(key)
                    }
                } else {
                    openSettings(key)
                }
            }
            key == KEY_NOTIFICATIONS && !notificationsDeclined &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> openSettings(key)
        }
    }
}
