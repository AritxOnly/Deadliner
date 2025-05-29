package com.aritxonly.deadliner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aritxonly.deadliner.web.WebUtils
import com.google.android.material.color.DynamicColors
import java.time.LocalDateTime

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivitiesIfAvailable(application)

        GlobalUtils.init(this)
        WebUtils.init()

        if (GlobalUtils.showIntroPage) {
            if (GlobalUtils.firstRun) GlobalUtils.timeNull = LocalDateTime.now()
            startActivity(Intent(this, IntroActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
    }
}