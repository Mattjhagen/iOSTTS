package com.metroreader.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MetroReaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Disable epublib's verbose logging
        try {
            val logFactory = Class.forName("org.slf4j.impl.StaticLoggerBinder")
            // slf4j-android handles this automatically
        } catch (_: Exception) {}
    }
}
