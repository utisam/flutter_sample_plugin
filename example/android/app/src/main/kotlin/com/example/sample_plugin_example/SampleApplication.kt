package com.example.sample_plugin_example

import android.app.Application
import com.example.sample_plugin.BuildConfig
import timber.log.Timber

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}