package com.werkloop.dumbify.system

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every check reads the system, every time it is asked.
 *
 * Nothing here is memoised and nothing is written to the repository: usage
 * access and the home role can both be taken away in Settings with no callback
 * to us, and a cached boolean is how a permissions screen ends up claiming a
 * grant the app no longer holds (design decision 8).
 */
@Singleton
class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionChecker {

    /**
     * `PACKAGE_USAGE_STATS` is a special access grant, not a runtime
     * permission: there is no dialog to request it and `checkSelfPermission`
     * does not reflect it. The app-op is the only thing that tells the truth.
     */
    // Deprecated in the current SDK, and still the only API that reports this
    // grant at minSdk 29 — the replacements are all above our floor.
    @Suppress("DEPRECATION")
    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Are we the home app the device actually launches? */
    override fun isDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
        return resolved?.activityInfo?.packageName == context.packageName
    }

    override fun canPostNotifications(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
        else ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}
