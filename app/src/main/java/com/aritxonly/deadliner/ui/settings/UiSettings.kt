package com.aritxonly.deadliner.ui.settings

import android.widget.Space
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aritxonly.deadliner.ui.PreviewCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier

@Composable
fun UiSettingsScreen(
    navigateUp: () -> Unit
) {
    var simplifiedEnabled by remember { mutableStateOf(GlobalUtils.style == "simplified") }

    val onSimplifiedChange: (Boolean) -> Unit = {
        GlobalUtils.style = if (it) "simplified" else "classic"
        simplifiedEnabled = it
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
                modifier = Modifier.padding(start = 8.dp)
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
        Column(
            Modifier
                .padding(padding)
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            UiModeSelectionRow(simplifiedEnabled, onSimplifiedChange, invertColorFilter)

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun UiModeSelectionRow(
    simplifiedEnabled: Boolean,
    onSimplifiedChange: (Boolean) -> Unit,
    invertColorFilter: ColorFilter?,
    inIntroPage: Boolean = false
) {
    val listState = rememberLazyListState()

    LaunchedEffect(simplifiedEnabled) {
        val index = if (simplifiedEnabled) 0 else 1
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
        item {
            UiModeOptionCard(
                label = stringResource(R.string.ui_style_simplified),
                supporting = stringResource(R.string.ui_style_simplified_support),
                imageRes = R.drawable.preview_simplified,
                selected = simplifiedEnabled,
                colorFilter = invertColorFilter,
                onClick = { onSimplifiedChange(true) },
                modifier = Modifier.fillParentMaxWidth(0.66f).padding(start = edgePadding)
            )
        }

        item {
            UiModeOptionCard(
                label = stringResource(R.string.ui_style_classic),
                supporting = stringResource(R.string.ui_style_classic_support),
                imageRes = R.drawable.preview_classic,
                selected = !simplifiedEnabled,
                colorFilter = invertColorFilter,
                onClick = { onSimplifiedChange(false) },
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

    Card(
        modifier = modifier
            .wrapContentHeight()
            .clickable(onClick = onClick),
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors()
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
                    onClick = onClick
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

/**
 * 给可滚动容器的「可视区域水平方向」添加原生式渐隐（fading edge）。
 * 无覆盖层、无 RenderEffect，仅对自身内容做 Alpha 遮罩。
 *
 * @param width 渐隐宽度（像素越大，过渡越长）
 * @param inverted 当需要做「右侧渐隐」时设为 true；默认做「左侧渐隐」
 */
fun Modifier.fadingHorizontalEdge(
    width: Dp = 32.dp,
    inverted: Boolean = false
): Modifier = this
    // 关键：开启离屏合成，才能让后续的 DstIn 作为整块内容的 Alpha 遮罩生效
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        // 先正常画内容
        drawContent()

        val w = width.toPx().coerceAtLeast(1f)

        if (!inverted) {
            // 左侧渐隐：左边 w 区域做渐变，其余保持不透明
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
            // 右侧渐隐：右边 w 区域做渐变，其余保持不透明
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