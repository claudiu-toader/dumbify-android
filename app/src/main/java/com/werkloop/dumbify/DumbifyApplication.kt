package com.werkloop.dumbify

import android.app.Application
import com.werkloop.dumbify.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DumbifyApplication : Application() {

    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        // The widget lives outside the Activity, so its refresh has to be
        // driven from something that outlives one.
        widgetUpdater.start()
    }
}
