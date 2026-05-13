package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults as Material3CheckboxDefaults
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

// 为官方和 MIUIX 的组件起别名
import androidx.compose.material3.Checkbox as Material3Checkbox
import androidx.compose.material3.RadioButton as Material3RadioButton
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox

/**
 * Deadliner 基础 RadioButton 组件
 * 在 M3 下表现为标准单选圆点，在 MIUIX 下无缝转译为打勾的 Checkbox
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            Material3RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource
            )
        }
        AppDesignSystem.MIUIX -> {
            // 核心转译：将 Boolean 的选中状态映射为 MIUIX 需要的三态类型
            val toggleState = if (selected) ToggleableState.On else ToggleableState.Off
            val scheme = if (isSystemInDarkTheme()) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            MiuixCheckbox(
                state = toggleState,
                onClick = onClick,
                modifier = modifier.padding(vertical = 4.dp),
                enabled = enabled,
                colors = CheckboxDefaults.checkboxColors(
                    uncheckedForegroundColor = scheme.secondary,
                    uncheckedBackgroundColor = scheme.secondary
                )
            )
        }
    }
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = Material3CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            Material3Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
            )
        }

        AppDesignSystem.MIUIX -> {
            val toggleState = if (checked) ToggleableState.On else ToggleableState.Off
            val scheme = if (isSystemInDarkTheme()) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            MiuixCheckbox(
                state = toggleState,
                onClick = onCheckedChange?.let { callback -> { callback(!checked) } },
                modifier = modifier.padding(4.dp),
                enabled = enabled,
                colors = CheckboxDefaults.checkboxColors(
                    uncheckedForegroundColor = scheme.secondary,
                    uncheckedBackgroundColor = scheme.secondary
                )
            )
        }
    }
}
