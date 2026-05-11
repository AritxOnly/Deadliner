package com.aritxonly.deadliner.localutils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.AppIconMode
import com.aritxonly.deadliner.model.Season
import java.time.LocalDate

object AppIconManager {
    enum class AppIconVariant(
        val aliasClassName: String,
        @DrawableRes val previewRes: Int,
        val manifestEnabledByDefault: Boolean = false,
    ) {
        DEFAULT(
            aliasClassName = "com.aritxonly.deadliner.LauncherAliasDefault",
            previewRes = R.mipmap.ic_launcher,
            manifestEnabledByDefault = true,
        ),
        SPRING(
            aliasClassName = "com.aritxonly.deadliner.LauncherAliasSpring",
            previewRes = R.mipmap.ic_launcher_spring,
        ),
        SUMMER(
            aliasClassName = "com.aritxonly.deadliner.LauncherAliasSummer",
            previewRes = R.mipmap.ic_launcher_summer,
        ),
        AUTUMN(
            aliasClassName = "com.aritxonly.deadliner.LauncherAliasAutumn",
            previewRes = R.mipmap.ic_launcher_autumn,
        ),
        WINTER(
            aliasClassName = "com.aritxonly.deadliner.LauncherAliasWinter",
            previewRes = R.mipmap.ic_launcher_winter,
        ),
    }

    val selectableModes = listOf(
        AppIconMode.Default,
        AppIconMode.SeasonalAuto,
        AppIconMode.SeasonalSpring,
        AppIconMode.SeasonalSummer,
        AppIconMode.SeasonalAutumn,
        AppIconMode.SeasonalWinter,
    )

    fun applyCurrentMode(context: Context) {
        applyMode(context, GlobalUtils.appIconMode, persist = false)
    }

    fun applyMode(
        context: Context,
        mode: AppIconMode,
        persist: Boolean = true,
    ) {
        val targetVariant = resolveVariant(mode)
        if (persist) {
            GlobalUtils.appIconMode = mode
        }
        if (currentEnabledVariant(context) == targetVariant) return

        val packageManager = context.packageManager
        AppIconVariant.entries.forEach { variant ->
            val component = ComponentName(context, variant.aliasClassName)
            val newState = if (variant == targetVariant) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    fun currentEnabledVariant(context: Context): AppIconVariant? {
        val packageManager = context.packageManager
        return AppIconVariant.entries.firstOrNull { variant ->
            val state = packageManager.getComponentEnabledSetting(
                ComponentName(context, variant.aliasClassName)
            )
            when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> variant.manifestEnabledByDefault
                else -> false
            }
        }
    }

    fun resolveVariant(
        mode: AppIconMode,
        date: LocalDate = LocalDate.now(),
    ): AppIconVariant {
        return when (mode) {
            AppIconMode.Default -> AppIconVariant.DEFAULT
            AppIconMode.SeasonalAuto -> when (SeasonResolver.resolve(date)) {
                Season.SPRING -> AppIconVariant.SPRING
                Season.SUMMER -> AppIconVariant.SUMMER
                Season.AUTUMN -> AppIconVariant.AUTUMN
                Season.WINTER -> AppIconVariant.WINTER
            }
            AppIconMode.SeasonalSpring -> AppIconVariant.SPRING
            AppIconMode.SeasonalSummer -> AppIconVariant.SUMMER
            AppIconMode.SeasonalAutumn -> AppIconVariant.AUTUMN
            AppIconMode.SeasonalWinter -> AppIconVariant.WINTER
            AppIconMode.Custom -> AppIconVariant.DEFAULT
        }
    }

    @DrawableRes
    fun previewResFor(mode: AppIconMode): Int {
        return resolveVariant(mode).previewRes
    }

    @StringRes
    fun titleResFor(mode: AppIconMode): Int {
        return when (mode) {
            AppIconMode.Default -> R.string.settings_app_icon_default
            AppIconMode.SeasonalAuto -> R.string.settings_app_icon_auto
            AppIconMode.SeasonalSpring -> R.string.settings_app_icon_spring
            AppIconMode.SeasonalSummer -> R.string.settings_app_icon_summer
            AppIconMode.SeasonalAutumn -> R.string.settings_app_icon_autumn
            AppIconMode.SeasonalWinter -> R.string.settings_app_icon_winter
            AppIconMode.Custom -> R.string.settings_app_icon_default
        }
    }

    @StringRes
    fun summaryResFor(mode: AppIconMode): Int {
        return when (mode) {
            AppIconMode.Default -> R.string.settings_app_icon_default_summary
            AppIconMode.SeasonalAuto -> R.string.settings_app_icon_auto_summary
            AppIconMode.SeasonalSpring -> R.string.settings_app_icon_spring_summary
            AppIconMode.SeasonalSummer -> R.string.settings_app_icon_summer_summary
            AppIconMode.SeasonalAutumn -> R.string.settings_app_icon_autumn_summary
            AppIconMode.SeasonalWinter -> R.string.settings_app_icon_winter_summary
            AppIconMode.Custom -> R.string.settings_app_icon_default_summary
        }
    }
}
