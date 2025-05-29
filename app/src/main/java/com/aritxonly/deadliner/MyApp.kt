package com.aritxonly.deadliner

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalUtils.init(this)
    }
}