package com.werkloop.dumbify.widget

import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.system.AppCatalog
import com.werkloop.dumbify.system.UsageReader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * A widget is not a Hilt-injectable class, so it reaches the same singletons the
 * app uses through an entry point rather than building its own — the widget and
 * the launcher must never disagree about what is allowed or what time it is.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): DumbifyRepository
    fun appCatalog(): AppCatalog
    fun usageReader(): UsageReader
    fun clock(): DumbClock
}
