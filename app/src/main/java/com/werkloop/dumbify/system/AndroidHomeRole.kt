package com.werkloop.dumbify.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Switches the manifest's home alias on and off.
 *
 * The enabled setting survives process death and reboots — it is stored by
 * `PackageManager`, not by us — so this is deliberately not mirrored into the
 * repository. One fact, one owner: asking the package manager is always right,
 * and a cached copy is how the settings screen would start lying after the user
 * changed launchers from outside the app.
 */
@Singleton
class AndroidHomeRole @Inject constructor(
    @ApplicationContext private val context: Context,
) : HomeRoleController {

    private val alias = ComponentName(context.packageName, HOME_ALIAS)

    override fun isOfferedAsHome(): Boolean =
        // DEFAULT means "whatever the manifest said", and the manifest says
        // enabled — so only an explicit DISABLED counts as off.
        context.packageManager.getComponentEnabledSetting(alias) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    override fun setOfferedAsHome(enabled: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            alias,
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            // Without DONT_KILL_APP the system restarts the process here, which
            // would take the settings screen down mid-confirmation and look
            // exactly like a crash.
            PackageManager.DONT_KILL_APP,
        )
    }

    private companion object {
        /** Must track the `<activity-alias android:name>` in the manifest. */
        const val HOME_ALIAS = "com.werkloop.dumbify.HomeSurface"
    }
}
