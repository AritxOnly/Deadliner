package com.aritxonly.deadliner

import android.app.Application
import androidx.window.embedding.RuleController
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.web.DeepSeekUtils

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalUtils.init(this)
        KeystorePreferenceManager.createKeyIfNeeded()
        DeepSeekUtils.init(this)

        if (GlobalUtils.embeddedActivities) {
            val rules = RuleController.parseRules(this, R.xml.tablet_split_config)
            RuleController.getInstance(this).setRules(rules)
        }
    }
}