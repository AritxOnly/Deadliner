package com.aritxonly.deadliner.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Immutable
data class AdvancedMaterialSpec(
    val enabled: Boolean = false,
    val blurRadius: Float = 96f,
    val noiseCoefficient: Float = 0.009f,
    val navigationTintAlpha: Float = 0.72f,
    val topBarTintAlpha: Float = 0.68f,
)

val LocalAdvancedMaterialSpec = staticCompositionLocalOf { AdvancedMaterialSpec() }

val LocalAdvancedMaterialBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
