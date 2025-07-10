package com.aritxonly.deadliner

import android.app.Application
import com.aritxonly.deadliner.localutils.GlobalUtils

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalUtils.init(this)
    }
}