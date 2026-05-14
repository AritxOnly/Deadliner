package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.Switch as Material3Switch
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

/**
 * Deadliner 基础 Switch 组件
 * 签名完全对齐 Material 3，实现双框架无缝路由
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            // M3 分支：参数 1:1 完美透传
            Material3Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                thumbContent = thumbContent,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource
            )
        }
        AppDesignSystem.MIUIX -> {
            val scheme = if (isSystemInDarkTheme()) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier.padding(vertical = 2.dp),
                enabled = enabled,

                colors = top.yukonga.miuix.kmp.basic.SwitchDefaults.switchColors(
                    uncheckedThumbColor = scheme.onSecondary,
                    uncheckedTrackColor = scheme.secondary
                )
            )
        }
    }
}
