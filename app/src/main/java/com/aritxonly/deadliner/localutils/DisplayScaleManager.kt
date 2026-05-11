package com.aritxonly.deadliner.localutils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.DisplayMetrics
import com.aritxonly.deadliner.model.DisplayScalePreset
import kotlin.math.roundToInt

object DisplayScaleManager {
    private const val prefName = "app_settings"
    private const val prefKey = "display_scale_preset"
    private const val customPrefKey = "display_scale_custom_multiplier"

    fun currentPreset(context: Context): DisplayScalePreset {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        return DisplayScalePreset.fromKey(
            prefs.getString(prefKey, DisplayScalePreset.FollowSystem.key)
        )
    }

    fun wrap(base: Context): Context {
        val preset = currentPreset(base)
        if (preset == DisplayScalePreset.FollowSystem) return base

        val configuration = createScaledConfiguration(
            baseConfiguration = base.resources.configuration,
            preset = preset,
            customMultiplier = currentCustomMultiplier(base),
        )
        return base.createConfigurationContext(configuration)
    }

    fun currentCustomMultiplier(context: Context): Float {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        return prefs.getFloat(customPrefKey, 1.00f).coerceIn(0.85f, 1.25f)
    }

    fun createScaledConfiguration(
        baseConfiguration: Configuration,
        preset: DisplayScalePreset,
        customMultiplier: Float = 1.00f,
    ): Configuration {
        val configuration = Configuration(baseConfiguration)
        val multiplier = when (preset) {
            DisplayScalePreset.FollowSystem -> null
            DisplayScalePreset.Custom -> customMultiplier
            else -> preset.multiplier
        } ?: return configuration
        val stableDensity = DisplayMetrics.DENSITY_DEVICE_STABLE
            .takeIf { it > 0 }
            ?: baseConfiguration.densityDpi

        configuration.densityDpi = (stableDensity * multiplier)
            .roundToInt()
            .coerceAtLeast(120)
        return configuration
    }

    fun restartApp(activity: Activity) {
        val launchIntent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
            ?: return
        val restartIntent = Intent.makeRestartActivityTask(launchIntent.component).apply {
            putExtras(launchIntent)
        }
        activity.startActivity(restartIntent)
        activity.finishAffinity()
    }
}
