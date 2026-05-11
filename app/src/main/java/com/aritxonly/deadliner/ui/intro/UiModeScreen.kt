package com.aritxonly.deadliner.ui.intro

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.settings.ThemeColorPicker
import com.aritxonly.deadliner.ui.settings.UiModeSelectionRow

@Composable
fun UiModeScreen() {
    val darkTheme = isSystemInDarkTheme()

    val currentStyle by GlobalUtils.styleFlow.collectAsState()
    val selectedColorState by GlobalUtils.seedColorFlow.collectAsState()

    val onStyleChange: (String) -> Unit = {
        GlobalUtils.setStyle(com.aritxonly.deadliner.model.UiStyle.fromKey(it))
    }

    val onThemeChange: (String?) -> Unit = {
        GlobalUtils.seedColor = it
    }

    val invertColorFilter = remember(darkTheme) {
        if (!darkTheme) null
        else ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f,   0f
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp) // 加大模块之间的间距
    ) {

        // ==========================================
        // 模块 1：底层渲染引擎开关 (MIUIX Mode)
        // ==========================================
        // ==========================================
        // 模块 2：布局风格选择
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.intro_theme_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.intro_theme_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            UiModeSelectionRow(
                currentStyle = currentStyle.key,
                onStyleChange = onStyleChange,
                invertColorFilter = invertColorFilter,
                inIntroPage = true,
            )
        }
        // ==========================================
        // 模块 3：强调色选择 (Theme Color Picker)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            ThemeColorPicker(
                currentSeed = selectedColorState,
                onColorSelected = onThemeChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp)) // 底部留白
    }
}
