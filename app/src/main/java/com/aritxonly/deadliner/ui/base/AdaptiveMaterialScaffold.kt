package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults.blurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val AdaptiveMaterialTopBarShape = RoundedCornerShape(0.dp)

@Composable
fun AdaptiveMaterialScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    legacyTopBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    advancedMaterialTopBarTintColor: Color? = null,
    wrapTopBarInMaterialContainer: Boolean = true,
    topBarMaterialAlpha: Float = 1f,
    useCurrentTopBarHeightForContentPadding: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val designSystem = LocalAppDesignSystem.current
    val backdrop = if (advancedMaterial.enabled) rememberLayerBackdrop() else null
    var currentTopBarHeightPx by remember { mutableIntStateOf(0) }
    var maxTopBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeight = with(density) {
        if (useCurrentTopBarHeightForContentPadding) {
            currentTopBarHeightPx.toDp()
        } else {
            maxTopBarHeightPx.toDp()
        }
    }
    val tintBaseColor = advancedMaterialTopBarTintColor ?: when (designSystem) {
        AppDesignSystem.MATERIAL3 -> MaterialTheme.colorScheme.surfaceContainer
        AppDesignSystem.MIUIX -> MiuixTheme.colorScheme.surface
    }
    val surfaceTint = tintBaseColor.copy(alpha = advancedMaterial.topBarTintAlpha)
    val blurColors = blurColors(blendColors = listOf(BlendColorEntry(surfaceTint)))

    Scaffold(
        modifier = modifier,
        topBar = {
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    legacyTopBar()
                }
            } else {
                legacyTopBar()
            }
        },
        bottomBar = {
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    bottomBar()
                }
            } else {
                bottomBar()
            }
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        val mergedPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding() + topBarHeight,
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { baseModifier ->
                        if (backdrop != null) {
                            baseModifier.layerBackdrop(backdrop)
                        } else {
                            baseModifier
                        }
                    },
            ) {
                content(mergedPadding)
            }

            val topBarModifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    currentTopBarHeightPx = size.height
                    if (size.height > maxTopBarHeightPx) {
                        maxTopBarHeightPx = size.height
                    }
                }

            if (backdrop != null && wrapTopBarInMaterialContainer) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    Box(modifier = topBarModifier) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .textureBlur(
                                    backdrop = backdrop,
                                    shape = AdaptiveMaterialTopBarShape,
                                    blurRadius = advancedMaterial.blurRadius,
                                    noiseCoefficient = advancedMaterial.noiseCoefficient,
                                    colors = blurColors,
                                )
                                .alpha(topBarMaterialAlpha.coerceIn(0f, 1f)),
                        )
                        topBar()
                    }
                }
            } else if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    Box(modifier = topBarModifier) {
                        topBar()
                    }
                }
            } else {
                Box(modifier = topBarModifier) {
                    topBar()
                }
            }
        }
    }
}
