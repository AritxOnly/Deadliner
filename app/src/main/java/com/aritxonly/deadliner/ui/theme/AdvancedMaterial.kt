package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.BlurDefaults.blurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

@Immutable
data class AdvancedMaterialSpec(
    val enabled: Boolean = false,
    val blurEnabled: Boolean = true,
    val blurRadius: Float = 256f,
    val blurRadiusX: Float? = null,
    val blurRadiusY: Float? = null,
    val noiseCoefficient: Float = 0.09f,
    val blurBlendColors: List<BlendColorEntry> = emptyList(),
    val blurBrightness: Float = 0f,
    val blurContrast: Float = 1f,
    val blurSaturation: Float = 0.95f,
    val blurContentBlendMode: BlendMode = BlendMode.SrcOver,
    val navigationTintAlpha: Float = 0.72f,
    val topBarTintAlpha: Float = 0.68f,
) {
    val resolvedBlurRadius: Float
        get() = blurRadius.coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val resolvedBlurRadiusX: Float
        get() = (blurRadiusX ?: blurRadius).coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val resolvedBlurRadiusY: Float
        get() = (blurRadiusY ?: blurRadius).coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val usesIndependentBlurRadii: Boolean
        get() = blurRadiusX != null || blurRadiusY != null

    val textureBlurEnabled: Boolean
        get() = enabled && blurEnabled
}

@Composable
fun AdvancedMaterialSpec.rememberBlurColors(
    additionalBlendColors: List<BlendColorEntry> = emptyList(),
): BlurColors = blurColors(
    blendColors = additionalBlendColors + blurBlendColors,
    brightness = blurBrightness,
    contrast = blurContrast,
    saturation = blurSaturation,
)

fun Modifier.advancedTextureBlur(
    advancedMaterial: AdvancedMaterialSpec,
    backdrop: Backdrop,
    shape: Shape,
    colors: BlurColors,
): Modifier = if (advancedMaterial.usesIndependentBlurRadii) {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadiusX = advancedMaterial.resolvedBlurRadiusX,
        blurRadiusY = advancedMaterial.resolvedBlurRadiusY,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        colors = colors,
        contentBlendMode = advancedMaterial.blurContentBlendMode,
        enabled = advancedMaterial.textureBlurEnabled,
    )
} else {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = advancedMaterial.resolvedBlurRadius,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        colors = colors,
        contentBlendMode = advancedMaterial.blurContentBlendMode,
        enabled = advancedMaterial.textureBlurEnabled,
    )
}

val LocalAdvancedMaterialSpec = staticCompositionLocalOf { AdvancedMaterialSpec() }

val LocalAdvancedMaterialBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
