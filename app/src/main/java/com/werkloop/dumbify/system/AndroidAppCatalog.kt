package com.werkloop.dumbify.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.werkloop.dumbify.domain.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Apps enumerated through the launcher intent rather than through
 * `QUERY_ALL_PACKAGES`.
 *
 * A launcher is one of the use cases Play explicitly permits for that
 * declaration, but the launcher-intent query answers the question Dumbify
 * actually asks — "what could a home screen show?" — so the sensitive
 * declaration stays a fallback we have not needed.
 */
@Singleton
class AndroidAppCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppCatalog {

    override suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            // Dumbify is not offered as a togglable entry — allowing or hiding
            // the launcher itself is meaningless (app-allowlist "excludes itself").
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    category = categoryOf(it),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /**
     * The picker's right-hand column. Play's own category is used where the app
     * declares one; otherwise the only honest distinction left is whether it
     * shipped with the device.
     */
    private fun categoryOf(info: ApplicationInfo): String = when (info.category) {
        ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
        ApplicationInfo.CATEGORY_NEWS -> "NEWS"
        ApplicationInfo.CATEGORY_GAME -> "GAME"
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "MEDIA"
        ApplicationInfo.CATEGORY_IMAGE -> "PHOTO"
        ApplicationInfo.CATEGORY_MAPS -> "MAPS"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "WORK"
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "ACCESS"
        else -> if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) "SYSTEM" else "APP"
    }
}
