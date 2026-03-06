package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.ui.iconResource

val M3BaselineColors = listOf(
    // ✨ M3 官方基准色 (经典、沉稳)
    "#6750A4", // Baseline Purple (基准紫)
    "#0061A4", // Baseline Blue (基准蓝)
    "#006874", // Baseline Cyan (深海青)
    "#386A20", // Baseline Green (护眼绿)

    // 🌿 自然与平静 (适合缓解 DDL 焦虑)
    "#2E7D32", // Forest Green (森林绿)
    "#556B2F", // Olive Drab (橄榄绿)
    "#00796B", // Teal (水鸭青)
    "#5D4037", // Brown (大地棕)

    // 🔥 活力与紧迫感 (适合专注冲刺)
    "#C62828", // Deep Red (警示红)
    "#E65100", // Deep Orange (落日橙)
    "#F57F17", // Amber (琥珀黄)
    "#880E4F"  // Deep Pink (晚樱粉)
)

@Composable
fun ThemeColorPicker(
    currentSeed: String?, // 传入当前的选中的种子色
    onColorSelected: (String?) -> Unit // 选中后的回调
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. 动态壁纸取色按钮 (传 null 代表跟随系统)
        item {
            ColorCircle(
                colorHex = null,
                isSelected = currentSeed == null,
                onClick = { onColorSelected(null) }
            )
        }

        // 2. 官方预设色卡
        items(M3BaselineColors) { hex ->
            ColorCircle(
                colorHex = hex,
                isSelected = currentSeed == hex,
                onClick = { onColorSelected(hex) }
            )
        }
    }
}

@Composable
private fun ColorCircle(
    colorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 如果是 null，显示为系统的 SurfaceVariant 颜色，并加个壁纸小图标
    val backgroundColor = if (colorHex != null) {
        Color(colorHex.toColorInt())
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() }
            .then(
                // 选中时加一个边框反馈
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // 选中时打个勾
            Icon(
                imageVector = iconResource(com.aritxonly.deadliner.R.drawable.ic_on),
                contentDescription = "Selected",
                tint = if (colorHex == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
            )
        } else if (colorHex == null) {
            // 动态颜色的图标提示
            Icon(
                imageVector = iconResource(com.aritxonly.deadliner.R.drawable.ic_wallpaper),
                contentDescription = "Dynamic Color",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}