package com.pro.aifitnesstracker

import android.app.Application

class FitnessApplication : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = AppOpenAdManager(this)
    }
}
