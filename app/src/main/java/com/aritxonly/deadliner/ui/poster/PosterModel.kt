package com.aritxonly.deadliner.ui.poster

import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.ui.overview.Metric

data class ExportDashboardData(
    val monthText: String,
    val metrics: List<Metric>,
    val brand: String = "Deadliner – 1st Anniversary",
    val generatedAt: String
)

object PosterTheme {
    val gradientBg = listOf(
        Color(0xFF6A5AE0), // 主色
        Color(0xFF00C2FF), // 辅色
        Color(0xFFFF6EA7)  // 点缀
    )
    val cardContainer = Color(0x66FFFFFF) // 半透明白
    val onCard = Color(0xFF0F172A)        // 深色文字
    val up = Color(0xFF22C55E)
    val down = Color(0xFFEF4444)
    val neutral = Color(0xFF60A5FA)
}