package com.werkloop.dumbify.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The round-trips out of the app.
 *
 * Each one is a system surface the user can abandon silently, which is why
 * nothing here returns a promise about the outcome — the permission screen
 * re-reads the live state on resume instead (design decision 8).
 */
@Singleton
class AndroidSystemNavigator @Inject constructor(
    @ApplicationContext private val context: Context,
) : SystemNavigator {

    /**
     * Usage access has no request dialog. The package URI opens Dumbify's own
     * toggle rather than the list of every app on Android 10+; OEM builds that
     * ignore it still show the list, and one that rejects it gets the plain
     * intent.
     */
    override fun openUsageAccessSettings() {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            start(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun openDefaultHomeSettings() =
        start(Intent(Settings.ACTION_HOME_SETTINGS))

    /**
     * Device-wide greyscale is `Settings.Secure`, behind `WRITE_SECURE_SETTINGS`
     * — a signature permission a normal app cannot hold. So the app deep-links
     * to the setting and says what is needed rather than silently failing
     * (design decision 3).
     */
    override fun openColorCorrectionSettings() =
        start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    override fun openNotificationSettings() = start(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    override fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    private fun start(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds ship without one of these screens. Falling back to
            // the app's own settings page is better than a crash on the surface
            // that is meant to be helping the user grant something.
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
