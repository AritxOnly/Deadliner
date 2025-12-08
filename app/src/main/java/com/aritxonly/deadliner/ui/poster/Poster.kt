package com.aritxonly.deadliner.ui.poster

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.overview.Metric
import com.aritxonly.deadliner.ui.overview.TextTone

@Composable
fun ShareDashboardPoster(
    data: ExportDashboardData,
    modifier: Modifier = Modifier,
    backgroundBitmap: Bitmap? = null,
    textTone: TextTone = TextTone.Light,
    widthPx: Int = 1080
) {
    val textColor = if (textTone == TextTone.Light) Color.White else Color.Black
    val posterWidthDp = with(LocalDensity.current) { widthPx.toDp() }

    Box(
        modifier = modifier
            .width(posterWidthDp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // 默认渐变
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(PosterTheme.gradientBg))
            )
        }

        Column (
            modifier = Modifier.padding(24.dp)
        ) {

            Text(
                text = data.brand,
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = data.monthText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            // 中部网格
            Spacer(Modifier.height(64.dp))
            MetricsGridWrap(data.metrics)

            Spacer(Modifier.height(24.dp))
            // 底部水印
            Text(
                text = data.generatedAt,
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.75f),
//                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricsGridWrap(
    metrics: List<Metric>,
    spacing: Dp = 12.dp
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = 2
        val cell = (maxWidth - spacing * (columns - 1)) / columns

        FlowRow(
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            metrics.forEach { m ->
                PosterMetricCard(
                    m = m,
                    modifier = Modifier
                        .requiredWidth(cell)     // 关键：强约束列宽
                        .fillMaxWidth()          // 卡片内部可以填满这列
                        .heightIn(min = 120.dp)
                )
            }
        }
    }
}

@Composable
private fun PosterMetricCard(
    m: Metric,
    modifier: Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            // 背景半透明，让模糊层透出来
            containerColor = PosterTheme.cardContainer.copy(alpha = 0.55f)
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
            .graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(
                    /* radiusX = */ 25f,
                    /* radiusY = */ 25f,
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
                clip = true
                shape = RoundedCornerShape(18.dp)
            }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                m.label,
                style = MaterialTheme.typography.labelLarge,
                color = PosterTheme.onCard.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                m.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PosterTheme.onCard
            )

            m.change?.takeIf { it.isNotEmpty() }?.let { ch ->
                Spacer(Modifier.height(8.dp))
                val color = when (m.isDown) {
                    true -> PosterTheme.down
                    false -> PosterTheme.up
                    null -> PosterTheme.neutral
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (m.isDown) {
                        true -> R.drawable.ic_arrow_down
                        false -> R.drawable.ic_arrow_up
                        null -> R.drawable.ic_next
                    }
                    Icon(iconResource(icon), contentDescription = null, tint = color)
                    Spacer(Modifier.width(6.dp))
                    Text(ch, style = MaterialTheme.typography.bodyMedium, color = color)
                }
            }
        }
    }
}