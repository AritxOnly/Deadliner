package com.aritxonly.deadliner.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.base.RadioButton

@Composable
fun UiSettingsScreen(
    navigateUp: () -> Unit
) {
    val currentStyle by GlobalUtils.styleFlow.collectAsState()

    val onStyleChange: (String) -> Unit = {
        GlobalUtils.setStyle(com.aritxonly.deadliner.model.UiStyle.fromKey(it))
    }

    val darkTheme = isSystemInDarkTheme()

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

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_ui_mode_title),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = navIconPaddingModifier
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        SettingsScrollColumn(
            contentPadding = padding,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            UiModeSelectionRow(
                currentStyle = currentStyle.key,
                onStyleChange = onStyleChange,
                invertColorFilter = invertColorFilter,
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun UiModeSelectionRow(
    currentStyle: String,
    onStyleChange: (String) -> Unit,
    invertColorFilter: ColorFilter?,
    inIntroPage: Boolean = false
) {
    val listState = rememberLazyListState()

    // 2. 动画滚动逻辑更新：根据不同的 Style 滚动到对应的卡片
    LaunchedEffect(currentStyle) {
        val index = when (currentStyle) {
            "simplified" -> 0
            "miuix" -> 1
            else -> 2 // classic
        }
        listState.animateScrollToItem(index)
    }

    val edgePadding = if (!inIntroPage) 16.dp else 2.dp

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .fadingHorizontalEdge(edgePadding, false)
            .fadingHorizontalEdge(edgePadding, true),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 卡片 1：极简模式 (Simplified)
        item {
            UiModeOptionCard(
                label = stringResource(R.string.ui_style_simplified),
                supporting = stringResource(R.string.ui_style_simplified_support),
                imageRes = R.drawable.preview_simplified,
                selected = currentStyle == "simplified",
                enabled = true,
                colorFilter = invertColorFilter,
                onClick = { onStyleChange("simplified") },
                modifier = Modifier.fillParentMaxWidth(0.66f).padding(start = edgePadding)
            )
        }

        // 🌟 卡片 2：澎湃模式 (MIUIX)
        item {
            UiModeOptionCard(
                label = stringResource(R.string.ui_style_miuix),
                supporting = stringResource(R.string.ui_style_miuix_support),
                imageRes = R.drawable.preview_classic,
                selected = currentStyle == "miuix",
                enabled = true,
                colorFilter = invertColorFilter,
                onClick = { onStyleChange("miuix") },
                modifier = Modifier.fillParentMaxWidth(0.66f)
            )
        }

        // 卡片 3：经典模式 (Classic)
        item {
            UiModeOptionCard(
                label = stringResource(R.string.ui_style_classic),
                supporting = stringResource(R.string.ui_style_classic_support),
                imageRes = R.drawable.preview_classic,
                selected = currentStyle == "classic",
                enabled = true,
                colorFilter = invertColorFilter,
                onClick = { onStyleChange("classic") },
                modifier = Modifier.fillParentMaxWidth(0.66f).padding(end = edgePadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UiModeOptionCard(
    label: String,
    supporting: String,
    @DrawableRes imageRes: Int,
    selected: Boolean,
    enabled: Boolean, // 新增 enabled 参数
    colorFilter: ColorFilter?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))

    // 3. 视觉反馈：不可选时降低整体透明度
    val cardAlpha = if (enabled) 1f else 0.4f

    Card(
        modifier = modifier
            .wrapContentHeight()
            .alpha(cardAlpha) // 改变透明度实现置灰效果
            .clip(shape) // 确保涟漪效果不会超出圆角
            .clickable(enabled = enabled, onClick = onClick), // 彻底禁用点击事件
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(shape.copy(all = CornerSize(20.dp))),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                     enabled = enabled
                )
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = label,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = supporting,
                        minLines = 3,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// FadingEdge 的扩展函数保持不变
fun Modifier.fadingHorizontalEdge(
    width: Dp = 32.dp,
    inverted: Boolean = false
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val w = width.toPx().coerceAtLeast(1f)

        if (!inverted) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = w
                ),
                size = size.copy(width = w),
                blendMode = BlendMode.DstIn
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(w, 0f),
                size = size.copy(width = size.width - w),
                blendMode = BlendMode.DstIn
            )
        } else {
            val startX = size.width - w
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = startX,
                    endX = size.width
                ),
                topLeft = Offset(startX, 0f),
                size = size.copy(width = w),
                blendMode = BlendMode.DstIn
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, 0f),
                size = size.copy(width = size.width - w),
                blendMode = BlendMode.DstIn
            )
        }
    }
