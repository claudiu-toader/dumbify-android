package com.werkloop.dumbify.di

import com.werkloop.dumbify.system.AbsenceOnlyEnforcement
import com.werkloop.dumbify.system.AndroidAppCatalog
import com.werkloop.dumbify.system.AndroidHomeRole
import com.werkloop.dumbify.system.AndroidPermissionChecker
import com.werkloop.dumbify.system.AndroidSystemNavigator
import com.werkloop.dumbify.system.AndroidUsageReader
import com.werkloop.dumbify.system.AppCatalog
import com.werkloop.dumbify.system.EnforcementController
import com.werkloop.dumbify.system.HomeRoleController
import com.werkloop.dumbify.system.PermissionChecker
import com.werkloop.dumbify.system.SystemNavigator
import com.werkloop.dumbify.system.UsageReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The six platform adapters, bound behind their interfaces.
 *
 * Every binding here is what a test replaces with a fake via `@TestInstallIn`,
 * which is the whole point of the seam (design decision 10).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {

    @Binds @Singleton
    abstract fun appCatalog(impl: AndroidAppCatalog): AppCatalog

    @Binds @Singleton
    abstract fun usageReader(impl: AndroidUsageReader): UsageReader

    @Binds @Singleton
    abstract fun permissionChecker(impl: AndroidPermissionChecker): PermissionChecker

    @Binds @Singleton
    abstract fun systemNavigator(impl: AndroidSystemNavigator): SystemNavigator

    @Binds @Singleton
    abstract fun enforcementController(impl: AbsenceOnlyEnforcement): EnforcementController

    @Binds @Singleton
    abstract fun homeRoleController(impl: AndroidHomeRole): HomeRoleController
}
