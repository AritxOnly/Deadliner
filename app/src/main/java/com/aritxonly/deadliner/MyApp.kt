package com.aritxonly.deadliner

import android.app.Application
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.web.DeepSeekUtils

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalUtils.init(this)
        KeystorePreferenceManager.createKeyIfNeeded()
        DeepSeekUtils.init(this)
    }
}