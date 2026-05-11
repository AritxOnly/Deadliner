package com.aritxonly.deadliner.model

enum class AppearanceColorSource {
    SystemDynamic,
    SeedColor,
}

data class AppearancePreferences(
    val uiStyle: UiStyle = UiStyle.Simplified,
    val themeStyle: AppThemeStyle = AppThemeStyle.Material3,
    val colorSource: AppearanceColorSource = AppearanceColorSource.SystemDynamic,
    val seedColorHex: String? = null,
    val displayScalePreset: DisplayScalePreset = DisplayScalePreset.FollowSystem,
    val customDisplayScaleMultiplier: Float = 1.00f,
    val usePureMiuixAccent: Boolean = false,
    val useMiuixNeutralSurfaces: Boolean = true,
    val useAdvancedMaterial: Boolean = false,
    val appIconMode: AppIconMode = AppIconMode.Default,
) {
    val supportsMiuixTheme: Boolean
        get() = uiStyle != UiStyle.Classic

    val usesMiuixThemePreference: Boolean
        get() = themeStyle == AppThemeStyle.Miuix

    val effectiveThemeStyle: AppThemeStyle
        get() = if (supportsMiuixTheme) themeStyle else AppThemeStyle.Material3

    val usesMiuixTheme: Boolean
        get() = effectiveThemeStyle == AppThemeStyle.Miuix
}
