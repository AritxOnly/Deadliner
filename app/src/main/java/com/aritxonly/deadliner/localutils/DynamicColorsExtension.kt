package com.aritxonly.deadliner.localutils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.R
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

object DynamicColorsExtension {

    fun apply(
        activity: Activity,
        seed: String? = null,
        isMiuixMode: Boolean = GlobalUtils.appearancePreferences.usesMiuixTheme,
        isMiuixColor: Boolean = GlobalUtils.appearancePreferences.usePureMiuixAccent
    ) {
        val builder = DynamicColorsOptions.Builder()

        // 🌟 魔法 1：如果开启了纯正澎湃色，强行把种子色替换为澎湃蓝，用来生成协调的容器辅色
        val effectiveSeed = if (isMiuixMode && isMiuixColor) "#3481FF" else seed

        if (!effectiveSeed.isNullOrBlank()) {
            try {
                builder.setContentBasedSource(effectiveSeed.toColorInt())
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        DynamicColors.applyToActivityIfAvailable(activity, builder.build())

        if (isMiuixMode) {
            activity.theme.applyStyle(R.style.ThemeOverlay_Deadliner_MiuixBackground, true)

            if (isMiuixColor) {
                activity.theme.applyStyle(R.style.ThemeOverlay_Deadliner_MiuixDefaults, true)
            }
        }
    }

    fun applyApp(
        app: Application,
        seed: String? = null,
        isMiuixMode: Boolean = GlobalUtils.appearancePreferences.usesMiuixTheme,
        isMiuixColor: Boolean = GlobalUtils.appearancePreferences.usePureMiuixAccent
    ) {
        val builder = DynamicColorsOptions.Builder()

        // 同理，替换全局 Application 级别的种子
        val effectiveSeed = if (isMiuixMode && isMiuixColor) "#3481FF" else seed

        if (!effectiveSeed.isNullOrBlank()) {
            try {
                builder.setContentBasedSource(effectiveSeed.toColorInt())
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        DynamicColors.applyToActivitiesIfAvailable(app, builder.build())

        if (isMiuixMode) {
            app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                    super.onActivityPreCreated(activity, savedInstanceState)

                    // 在每一个 Activity 创建前，打上双重补丁
                    activity.theme.applyStyle(R.style.ThemeOverlay_Deadliner_MiuixBackground, true)

                    if (isMiuixColor) {
                        activity.theme.applyStyle(R.style.ThemeOverlay_Deadliner_MiuixDefaults, true)
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        }
    }
}
