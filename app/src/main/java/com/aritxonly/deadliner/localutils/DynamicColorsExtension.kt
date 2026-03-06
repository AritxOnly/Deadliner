package com.aritxonly.deadliner.localutils

import android.app.Activity
import android.app.Application
import androidx.core.graphics.toColorInt
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

object DynamicColorsExtension {
    fun apply(activity: Activity, seed: String? = null) {
        if (seed.isNullOrBlank()) {
            DynamicColors.applyToActivityIfAvailable(activity)
        } else {
            try {
                val seedColorInt = seed.toColorInt()
                val options = DynamicColorsOptions.Builder()
                    .setContentBasedSource(seedColorInt)
                    .build()

                DynamicColors.applyToActivityIfAvailable(activity, options)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                DynamicColors.applyToActivityIfAvailable(activity)
            }
        }
    }

    fun applyApp(app: Application, seed: String? = null) {
        if (seed.isNullOrBlank()) {
            DynamicColors.applyToActivitiesIfAvailable(app)
        } else {
            try {
                val seedColorInt = seed.toColorInt()
                val options = DynamicColorsOptions.Builder()
                    .setContentBasedSource(seedColorInt)
                    .build()

                DynamicColors.applyToActivitiesIfAvailable(app, options)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                DynamicColors.applyToActivitiesIfAvailable(app)
            }
        }
    }
}
