package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivitiesIfAvailable(application)

        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("first_run", true)

        if (isFirstRun) {
            startActivity(Intent(this, IntroActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
    }
}