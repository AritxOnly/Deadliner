package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import androidx.compose.material3.TopAppBar as MaterialTopAppBar

enum class TopAppBarStyle {
    CENTER, LARGE, SMALL
}

private val TopAppBarMaterialShape = RoundedCornerShape(0.dp)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun TopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    isMainTitle: Boolean = false,
    titleTextStyle: TextStyle? = null,
    forceMaterial3: Boolean = false,
    useParentMaterialContainer: Boolean = false,
    material3ScrollBehavior: TopAppBarScrollBehavior? = null,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val advancedMaterialBlurred =
        advancedMaterial.enabled && backdrop != null && !useParentMaterialContainer
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = advancedMaterial.topBarTintAlpha)
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = when {
            useParentMaterialContainer -> Color.Transparent
            advancedMaterialBlurred -> Color.Transparent
            advancedMaterial.enabled -> surfaceTint
            else -> MaterialTheme.colorScheme.surface
        },
        scrolledContainerColor = when {
            useParentMaterialContainer -> Color.Transparent
            advancedMaterialBlurred -> Color.Transparent
            advancedMaterial.enabled -> surfaceTint
            else -> MaterialTheme.colorScheme.surface
        },
    )
    val appDesignSystem = if (forceMaterial3) {
        AppDesignSystem.MATERIAL3
    } else {
        LocalAppDesignSystem.current
    }

    when (appDesignSystem) {
        AppDesignSystem.MATERIAL3 -> AdvancedMaterialTopBarContainer(
            advancedMaterial = advancedMaterial,
            advancedMaterialBlurred = advancedMaterialBlurred,
            backdrop = backdrop,
            surfaceTint = surfaceTint,
        ) {
            when (mode) {
                TopAppBarStyle.CENTER ->
                    CenterAlignedTopAppBar(
                        title = {
                            if (titleTextStyle != null) {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = titleTextStyle,
                                )
                            } else {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = topBarColors,
                    )

                TopAppBarStyle.LARGE ->
                    LargeTopAppBar(
                        title = {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = topBarColors,
                        scrollBehavior = material3ScrollBehavior,
                    )

                else ->
                    if (!isMainTitle) {
                        MaterialTopAppBar(
                            title = {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = titleTextStyle ?: MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            navigationIcon = { navigationIcon?.invoke() },
                            actions = actions,
                            colors = topBarColors,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        MaterialTopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = if (subtitle != null)
                                            MaterialTheme.typography.headlineSmall
                                        else MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Medium
                                    )

                                    if (subtitle != null) {
                                        Text(
                                            text = subtitle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            },
                            actions = {
                                navigationIcon?.invoke()
                                actions()
                            },
                            colors = topBarColors,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
            }
        }

        AppDesignSystem.MIUIX -> AdvancedMaterialTopBarContainer(
            advancedMaterial = advancedMaterial,
            advancedMaterialBlurred = advancedMaterialBlurred,
            backdrop = backdrop,
            surfaceTint = surfaceTint,
        ) {
            val miuixContainerColor = when {
                useParentMaterialContainer -> Color.Transparent
                advancedMaterialBlurred -> Color.Transparent
                advancedMaterial.enabled -> surfaceTint
                else -> MaterialTheme.colorScheme.surface
            }
            when (mode) {
                TopAppBarStyle.LARGE ->
                    MiuixTopAppBar(
                        title = title,
                        largeTitle = title,
                        color = miuixContainerColor,
                        navigationIcon = navigationIcon ?: {},
                        actions = actions,
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        largeTitleColor = MaterialTheme.colorScheme.onSurface,
                        scrollBehavior = miuixScrollBehavior,
                    )

                else ->
                    SmallTopAppBar(
                        title = title,
                        color = miuixContainerColor,
                        navigationIcon = navigationIcon ?: {},
                        actions = actions,
                        titleColor = MaterialTheme.colorScheme.onSurface,
                    )
            }
        }
    }
}

@Composable
private fun AdvancedMaterialTopBarContainer(
    advancedMaterial: AdvancedMaterialSpec,
    advancedMaterialBlurred: Boolean,
    backdrop: LayerBackdrop? = null,
    surfaceTint: Color,
    content: @Composable () -> Unit,
) {
    if (advancedMaterialBlurred && backdrop != null) {
        val blurColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(surfaceTint)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .advancedTextureBlur(
                    advancedMaterial = advancedMaterial,
                    backdrop = backdrop,
                    shape = TopAppBarMaterialShape,
                    colors = blurColors,
                ),
        ) {
            content()
        }
    } else {
        content()
    }
}
