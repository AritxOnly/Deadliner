package com.aritxonly.deadliner

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.aritxonly.deadliner.localutils.DisplayScaleManager

abstract class DeadlinerAppCompatActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(DisplayScaleManager.wrap(newBase))
    }
}

abstract class DeadlinerComponentActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(DisplayScaleManager.wrap(newBase))
    }
}
