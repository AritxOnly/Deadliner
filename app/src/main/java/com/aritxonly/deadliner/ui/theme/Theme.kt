package com.aritxonly.deadliner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class AppDesignSystem {
    MATERIAL3,
    MIUIX,
}

val LocalAppDesignSystem = staticCompositionLocalOf { AppDesignSystem.MATERIAL3 }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val designSystem = if (appearance.usesMiuixTheme) AppDesignSystem.MIUIX else AppDesignSystem.MATERIAL3
    val tokens = DeadlinerColorTokenFactory.rememberTokens(
        seedColorHex = appearance.seedColorHex,
        colorSource = appearance.colorSource,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        usePureMiuixAccent = appearance.usesMiuixTheme && appearance.usePureMiuixAccent,
        useMiuixNeutralSurfaces = appearance.usesMiuixTheme && appearance.useMiuixNeutralSurfaces,
    )
    val materialColorScheme = DeadlinerMaterial3ColorSchemeFactory.create(
        tokens = tokens,
        darkTheme = darkTheme,
    )
    val miuixColorScheme = DeadlinerMiuixColorSchemeFactory.create(
        tokens = tokens,
        darkTheme = darkTheme,
    )
    val advancedMaterialSpec = AdvancedMaterialSpec(
        enabled = appearance.useAdvancedMaterial,
    )

    CompositionLocalProvider(
        LocalAppDesignSystem provides designSystem,
        LocalAdvancedMaterialSpec provides advancedMaterialSpec,
    ) {
        when (designSystem) {
            AppDesignSystem.MATERIAL3 -> {
                MaterialExpressiveTheme(
                    colorScheme = materialColorScheme,
                    typography = Typography,
                ) {
                    content()
                }
            }

            AppDesignSystem.MIUIX -> {
                MiuixTheme(colors = miuixColorScheme) {
                    MaterialExpressiveTheme(
                        colorScheme = materialColorScheme,
                        typography = Typography,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
