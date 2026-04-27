package com.dusk.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DuskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is auto-initialized via google-services.json
    }
}
