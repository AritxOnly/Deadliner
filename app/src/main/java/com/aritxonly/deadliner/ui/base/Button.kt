package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.TextButton as Material3TextButton
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/**
 * Deadliner 基础 Button 组件 (实心按钮)
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    forceMaterial3: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val designSystem = if (forceMaterial3) AppDesignSystem.MATERIAL3 else LocalAppDesignSystem.current
    when (designSystem) {
        AppDesignSystem.MATERIAL3 -> {
            Material3Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        AppDesignSystem.MIUIX -> {
            val contentColor = if (enabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            MiuixButton(
                onClick = onClick,
                enabled = enabled,
                insideMargin = top.yukonga.miuix.kmp.basic.ButtonDefaults.InsideMargin,
                colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                    color = colors.containerColor,
                ),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides contentColor
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Deadliner 基础 TextButton 组件 (文本按钮)
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    forceMaterial3: Boolean = false,

    // MIUIX 专属辅助参数
    miuixText: String = "",

    content: @Composable RowScope.() -> Unit
) {
    val designSystem = if (forceMaterial3) AppDesignSystem.MATERIAL3 else LocalAppDesignSystem.current
    when (designSystem) {
        AppDesignSystem.MATERIAL3 -> {
            Material3TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        AppDesignSystem.MIUIX -> {
            MiuixTextButton(
                text = miuixText,
                onClick = onClick,
                enabled = enabled,
                insideMargin = top.yukonga.miuix.kmp.basic.ButtonDefaults.InsideMargin,
            )
        }
    }
}
