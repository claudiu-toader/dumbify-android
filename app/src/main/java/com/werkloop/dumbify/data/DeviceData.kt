package com.werkloop.dumbify.data

import com.werkloop.dumbify.di.ApplicationScope
import com.werkloop.dumbify.domain.InstalledApp
import com.werkloop.dumbify.system.AppCatalog
import com.werkloop.dumbify.system.PermissionChecker
import com.werkloop.dumbify.system.UsageReader
import com.werkloop.dumbify.system.UsageToday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the device currently says, cached for the duration of a resume.
 *
 * Three screens need the app catalogue and four need today's usage; querying
 * `PackageManager` and `UsageStatsManager` once per screen would be both slow
 * and a way for two screens to disagree about the same second. [refresh] is
 * called on `ON_RESUME`, which is also when permission state is re-read — a
 * grant revoked while the app was away shows up as an empty read here rather
 * than as a stale cached list (design decision 8).
 */
@Singleton
class DeviceData @Inject constructor(
    private val catalog: AppCatalog,
    private val usage: UsageReader,
    private val permissions: PermissionChecker,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    /** Null while unknown *and* when usage access is not granted. */
    private val _today = MutableStateFlow<UsageToday?>(null)
    val today: StateFlow<UsageToday?> = _today.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _isDefaultHome = MutableStateFlow(false)
    val isDefaultHome: StateFlow<Boolean> = _isDefaultHome.asStateFlow()

    private val _canPostNotifications = MutableStateFlow(false)
    val canPostNotifications: StateFlow<Boolean> = _canPostNotifications.asStateFlow()

    fun refresh() {
        // Permissions first and synchronously-ish: everything below depends on
        // whether we are still allowed to read any of it.
        _hasUsageAccess.value = permissions.hasUsageAccess()
        _isDefaultHome.value = permissions.isDefaultHome()
        _canPostNotifications.value = permissions.canPostNotifications()
        scope.launch {
            _apps.value = catalog.installedApps()
            _today.value = usage.today()
        }
    }
}
