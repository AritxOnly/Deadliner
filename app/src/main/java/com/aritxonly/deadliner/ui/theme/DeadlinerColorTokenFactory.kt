package com.aritxonly.deadliner.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.model.AppearanceColorSource
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.scheme.DynamicScheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as darkMiuixColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as lightMiuixColorScheme

object DeadlinerColorTokenFactory {
    private val fallbackDarkScheme = darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80
    )

    private val fallbackLightScheme = lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40
    )

    private val miuixAccentSeedLight = Color(0xFF3482FF)
    private val miuixAccentSeedDark = Color(0xFF277AF7)

    @Composable
    fun rememberBaseMaterialColorScheme(
        seedColorHex: String?,
        colorSource: AppearanceColorSource,
        darkTheme: Boolean,
        dynamicColor: Boolean,
    ): ColorScheme {
        val context = LocalContext.current
        val parsedSeedColor = seedColorHex
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }

        return when {
            parsedSeedColor != null -> createDynamicScheme(parsedSeedColor, darkTheme)
            colorSource == AppearanceColorSource.SystemDynamic &&
                dynamicColor &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            darkTheme -> fallbackDarkScheme
            else -> fallbackLightScheme
        }
    }

    @Composable
    fun rememberTokens(
        seedColorHex: String?,
        colorSource: AppearanceColorSource,
        darkTheme: Boolean,
        dynamicColor: Boolean,
        usePureMiuixAccent: Boolean,
        useMiuixNeutralSurfaces: Boolean,
    ): DeadlinerColorTokens {
        val baseScheme = rememberBaseMaterialColorScheme(
            seedColorHex = seedColorHex,
            colorSource = colorSource,
            darkTheme = darkTheme,
            dynamicColor = dynamicColor,
        )

        val accentScheme = if (usePureMiuixAccent) {
            createDynamicScheme(
                seedColor = if (darkTheme) miuixAccentSeedDark else miuixAccentSeedLight,
                darkTheme = darkTheme,
            )
        } else {
            null
        }
        val miuixNeutralPalette = if (useMiuixNeutralSurfaces) {
            createMiuixNeutralPalette(darkTheme)
        } else {
            null
        }

        return createFromMaterialScheme(
            baseScheme = baseScheme,
            accentScheme = accentScheme,
            miuixNeutralPalette = miuixNeutralPalette,
        )
    }

    @Composable
    private fun createDynamicScheme(
        seedColor: Color,
        darkTheme: Boolean,
    ): ColorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        platform = DynamicScheme.Platform.PHONE
    )

    fun createFromMaterialScheme(
        baseScheme: ColorScheme,
        accentScheme: ColorScheme? = null,
        miuixNeutralPalette: MiuixNeutralPalette? = null,
    ): DeadlinerColorTokens {
        val primaryScheme = accentScheme ?: baseScheme

        return DeadlinerColorTokens(
            primary = primaryScheme.primary,
            onPrimary = primaryScheme.onPrimary,
            primaryContainer = primaryScheme.primaryContainer,
            onPrimaryContainer = primaryScheme.onPrimaryContainer,
            inversePrimary = primaryScheme.inversePrimary,
            secondary = baseScheme.secondary,
            onSecondary = baseScheme.onSecondary,
            secondaryContainer = baseScheme.secondaryContainer,
            onSecondaryContainer = baseScheme.onSecondaryContainer,
            tertiary = baseScheme.tertiary,
            onTertiary = baseScheme.onTertiary,
            tertiaryContainer = baseScheme.tertiaryContainer,
            onTertiaryContainer = baseScheme.onTertiaryContainer,
            background = miuixNeutralPalette?.background ?: baseScheme.background,
            onBackground = miuixNeutralPalette?.onBackground ?: baseScheme.onBackground,
            onBackgroundVariant = miuixNeutralPalette?.onBackgroundVariant ?: baseScheme.onSurfaceVariant,
            surface = miuixNeutralPalette?.surface ?: baseScheme.surface,
            onSurface = miuixNeutralPalette?.onSurface ?: baseScheme.onSurface,
            surfaceVariant = miuixNeutralPalette?.surfaceVariant ?: baseScheme.surfaceVariant,
            onSurfaceVariant = miuixNeutralPalette?.onSurfaceVariant ?: baseScheme.onSurfaceVariant,
            onSurfaceVariantSummary = miuixNeutralPalette?.onSurfaceVariantSummary ?: baseScheme.onSurfaceVariant,
            onSurfaceVariantActions = miuixNeutralPalette?.onSurfaceVariantActions ?: baseScheme.onSurface,
            surfaceContainer = miuixNeutralPalette?.surfaceContainer ?: baseScheme.surfaceContainer,
            surfaceContainerLow = miuixNeutralPalette?.surfaceContainerLow ?: baseScheme.surfaceContainerLow,
            surfaceContainerHigh = miuixNeutralPalette?.surfaceContainerHigh ?: baseScheme.surfaceContainerHigh,
            surfaceContainerHighest = miuixNeutralPalette?.surfaceContainerHighest ?: baseScheme.surfaceContainerHighest,
            surfaceContainerLowest = miuixNeutralPalette?.surfaceContainerLowest ?: baseScheme.surfaceContainerLowest,
            surfaceBright = miuixNeutralPalette?.surfaceBright ?: baseScheme.surfaceBright,
            surfaceDim = miuixNeutralPalette?.surfaceDim ?: baseScheme.surfaceDim,
            outline = miuixNeutralPalette?.outline ?: baseScheme.outline,
            outlineVariant = miuixNeutralPalette?.outlineVariant ?: baseScheme.outlineVariant,
            error = baseScheme.error,
            onError = baseScheme.onError,
            errorContainer = baseScheme.errorContainer,
            onErrorContainer = baseScheme.onErrorContainer,
            scrim = miuixNeutralPalette?.scrim ?: baseScheme.scrim,
        )
    }

    private fun createMiuixNeutralPalette(darkTheme: Boolean): MiuixNeutralPalette {
        val defaults = if (darkTheme) darkMiuixColorScheme() else lightMiuixColorScheme()
        val supportingTextColor = lerp(
            defaults.onSurfaceVariantSummary,
            defaults.onSurface,
            if (darkTheme) 0.10f else 0.18f,
        )
        val backgroundVariantColor = lerp(
            defaults.onBackgroundVariant,
            defaults.onBackground,
            if (darkTheme) 0.08f else 0.12f,
        )
        return MiuixNeutralPalette(
            background = defaults.background,
            onBackground = defaults.onBackground,
            onBackgroundVariant = backgroundVariantColor,
            surface = defaults.surface,
            onSurface = defaults.onSurface,
            surfaceVariant = defaults.surfaceVariant,
            onSurfaceVariant = supportingTextColor,
            onSurfaceVariantSummary = supportingTextColor,
            onSurfaceVariantActions = defaults.onSurfaceVariantActions,
            surfaceContainer = defaults.surfaceContainer,
            surfaceContainerLow = defaults.surface,
            surfaceContainerHigh = defaults.surfaceContainerHigh,
            surfaceContainerHighest = defaults.surfaceContainerHighest,
            surfaceContainerLowest = defaults.background,
            surfaceBright = defaults.surfaceVariant,
            surfaceDim = defaults.surfaceContainerHigh,
            outline = defaults.outline,
            outlineVariant = defaults.dividerLine,
            scrim = defaults.windowDimming,
        )
    }

    data class MiuixNeutralPalette(
        val background: Color,
        val onBackground: Color,
        val onBackgroundVariant: Color,
        val surface: Color,
        val onSurface: Color,
        val surfaceVariant: Color,
        val onSurfaceVariant: Color,
        val onSurfaceVariantSummary: Color,
        val onSurfaceVariantActions: Color,
        val surfaceContainer: Color,
        val surfaceContainerLow: Color,
        val surfaceContainerHigh: Color,
        val surfaceContainerHighest: Color,
        val surfaceContainerLowest: Color,
        val surfaceBright: Color,
        val surfaceDim: Color,
        val outline: Color,
        val outlineVariant: Color,
        val scrim: Color,
    )
}
