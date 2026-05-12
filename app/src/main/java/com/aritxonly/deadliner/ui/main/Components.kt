package com.aritxonly.deadliner.ui.main

import android.content.Intent
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.main.simplified.detectSwipeUp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import nl.dionsegijn.konfetti.xml.KonfettiView
import kotlin.math.abs

@Composable
fun TextPageIndicator(
    text: String,
    onClick: () -> Unit,
    selected: String,
    tag: String,
    badgeConfig: Triple<Boolean, Int, Boolean>
) {
    val isSelected = selected == tag
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "text-page-indicator-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "text-page-indicator-content",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.985f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "text-page-indicator-scale",
    )
    val (enabled, num, detail) = badgeConfig

    Button(
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )
    ) {
        BadgedBox(
            badge = {
                if (num != 0 && enabled) {
                    if (detail) {
                        Badge(
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        ) {
                            Text(text = num.toString())
                        }
                    } else {
                        Badge(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    }
                }
            }
        ) {
            Text(
                text,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DDLItemCard(
    title: String,
    remainingTimeAlt: String,     // 右上角那行（例：1:00 截止）
    remainingTime: String,        // 标题下的时间说明
    note: String,                 // 备注（可为空）
    progressPercent: Int,         // 0..100
    isStarred: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // 第一行：标题 + 右侧alt时间 + 星标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge, // ~24sp 粗体
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                // 右侧“1:00 截止”
                Text(
                    text = remainingTimeAlt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )

                if (isStarred) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_star),
                        contentDescription = null
                    )
                }
            }

            // 第二行：标题下的剩余时间
            if (remainingTime.isNotEmpty()) {
                Text(
                    text = remainingTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                )
            }

            // 第三行：备注（可空）
            if (note.isNotEmpty()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp, end = 12.dp)
                        .fillMaxWidth()
                )
            }

            // 进度条
            val progress = (progressPercent.coerceIn(0, 100)) / 100f
            LinearProgressIndicator(
                progress = { progress },
                color = indicatorColor,
                trackColor = trackColor,
                modifier = Modifier
                    .padding(top = 8.dp, start = 0.dp, end = 0.dp, bottom = 0.dp)
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@Composable
fun DDLItemCardSimplified(
    title: String,
    remainingTimeAlt: String,
    note: String,
    progress: Float,
    isStarred: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    status: DDLStatus = DDLStatus.UNDERGO,
    useDisabledCompletedStyle: Boolean = false
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val progressClamped = progress.coerceIn(0f, 1f)

    // 🌟 获取全局鸿蒙莫兰迪色开关
    val usePreset = GlobalUtils.presetIndicatorColor

    val indicatorColor: Color
    val bgColor: Color

    when (status) {
        DDLStatus.UNDERGO -> {
            indicatorColor = if (usePreset) colorResource(R.color.indicator_morandi_undergo).copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            bgColor = if (usePreset) colorResource(R.color.bg_morandi_undergo).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        }
        DDLStatus.NEAR -> {
            indicatorColor = if (usePreset) colorResource(R.color.indicator_morandi_near).copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
            bgColor = if (usePreset) colorResource(R.color.bg_morandi_near).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        }
        DDLStatus.PASSED -> {
            indicatorColor = if (usePreset) colorResource(R.color.indicator_morandi_passed).copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            // 列表中没有 bg_morandi_passed，用 indicator 降低透明度代替
            bgColor = if (usePreset) colorResource(R.color.indicator_morandi_passed).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        }
        DDLStatus.COMPLETED -> {
            if (useDisabledCompletedStyle) {
                indicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                indicatorColor = if (usePreset) colorResource(R.color.indicator_morandi_completed).copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                bgColor = if (usePreset) colorResource(R.color.bg_morandi_completed).copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(bgColor)
                .fillMaxWidth()
                .height(76.dp)
        ) {
            // 进度条渲染保持不变，它会自动使用全新的 indicatorColor 进行渐变
            if (progressClamped > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressClamped)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(indicatorColor.copy(alpha = 0.4f), indicatorColor)
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 第一行：标题 + 截止时间 + 星标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    )

                    Text(
                        text = remainingTimeAlt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    if (isStarred) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_star_filled),
                            contentDescription = null,
                            tint = indicatorColor.copy(alpha = 1f) // 星标颜色自动同步莫兰迪色
                        )
                    }
                }

                // 第二行：备注（可空）
                if (note.isNotEmpty()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DDLItemCardSwipeable(
    title: String,
    remainingTimeAlt: String,
    note: String,
    progress: Float,
    isStarred: Boolean,
    status: DDLStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    useDisabledCompletedStyle: Boolean = false,
    primaryActionIcon: ImageVector? = null,
    primaryActionColor: Color? = null,
    secondaryActionIcon: ImageVector? = null,
    secondaryActionColor: Color? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null
) {
    val swipeEnabled = !selectionMode
    val resolvedPrimaryActionIcon = primaryActionIcon ?: ImageVector.vectorResource(R.drawable.ic_check)
    val resolvedPrimaryActionColor = primaryActionColor ?: colorResource(R.color.chart_green)
    val resolvedSecondaryActionIcon = secondaryActionIcon ?: ImageVector.vectorResource(R.drawable.ic_delete)
    val resolvedSecondaryActionColor = secondaryActionColor ?: colorResource(R.color.chart_red)

    key(
        swipeEnabled,
        resolvedPrimaryActionIcon,
        resolvedPrimaryActionColor,
        resolvedSecondaryActionIcon,
        resolvedSecondaryActionColor
    ) {
        var hasTriggered by remember { mutableStateOf(false) }
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { v ->
                if (!swipeEnabled) return@rememberSwipeToDismissBoxState false
                when (v) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (!hasTriggered) {
                            hasTriggered = true
                            onDelete()
                        }
                        false
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (!hasTriggered) {
                            hasTriggered = true
                            onComplete()
                        }
                        false
                    }
                    else -> false
                }
            }
        )
        val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))

        var widthPx by remember { mutableIntStateOf(1) }
        val rawOffset = runCatching { dismissState.requireOffset() }.getOrElse { 0f }
        val fraction = (abs(rawOffset) / widthPx.toFloat()).coerceIn(0f, 1f)

        LaunchedEffect(dismissState) {
            snapshotFlow {
                // 读取偏移和状态
                val off = runCatching { dismissState.requireOffset() }.getOrElse { 0f }
                val atRest = abs(off) < 0.5f &&
                        dismissState.currentValue == SwipeToDismissBoxValue.Settled &&
                        dismissState.targetValue  == SwipeToDismissBoxValue.Settled
                atRest
            }
                .distinctUntilChanged()
                .collectLatest { atRest ->
                    if (atRest) {
                        hasTriggered = false   // 只有真正回到“静止原位”才解锁
                    }
                }
        }

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                if (!swipeEnabled) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { widthPx = it.width }
                            .clip(shape)
                    )
                    return@SwipeToDismissBox
                }

                val dir = dismissState.dismissDirection

                // 动作色（红=删除；绿=完成）与对齐位置
                val actionColor: Color
                val icon: ImageVector
                val alignment: Alignment

                when (dir) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        actionColor = resolvedSecondaryActionColor
                        icon = resolvedSecondaryActionIcon
                        alignment = Alignment.CenterEnd
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        actionColor = resolvedPrimaryActionColor
                        icon = resolvedPrimaryActionIcon
                        alignment = Alignment.CenterStart
                    }
                    else -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .onSizeChanged { widthPx = it.width }
                                .clip(shape)
                        )
                        return@SwipeToDismissBox
                    }
                }

                val base = MaterialTheme.colorScheme.surfaceVariant
                val bg = lerp(base, actionColor.copy(alpha = 0.80f), fraction)

                val iconTint = lerp(
                    actionColor.copy(alpha = 0.65f),
                    actionColor,
                    fraction.coerceIn(0f, 1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { widthPx = it.width }
                        .clip(shape)
                        .background(bg)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint
                    )
                }
            },
            content = {
                Box(
                    modifier = modifier
                        .clip(shape)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    onToggleSelect?.invoke()
                                } else {
                                    onClick?.invoke()
                                }
                            },
                            onLongClick = {
                                onLongPressSelect?.invoke()
                            }
                        )
                ) {
                    DDLItemCardSimplified(
                        title = title,
                        remainingTimeAlt = remainingTimeAlt,
                        note = note,
                        progress = progress,
                        isStarred = isStarred,
                        modifier = modifier
                            .clip(shape)
                            .onSizeChanged { widthPx = it.width },
                        onClick = null,
                        status = status,
                        useDisabledCompletedStyle = useDisabledCompletedStyle
                    )

                    if (selectionMode && selected) {
                        SelectionOverlay(
                            shape, Modifier
                            .height(76.dp)
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitItemCardSimplified(
    title: String,
    habitCount: Int,
    habitTotalCount: Int,
    freqAndTotalText: String,
    remainingText: String,
    isStarred: Boolean,
    progressTime: Float?,
    modifier: Modifier = Modifier,
    onCheckIn: (() -> Unit)? = null,
    status: DDLStatus = DDLStatus.UNDERGO,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))

    val indicatorColor: Color
    val bgColor: Color
    when (status) {
        DDLStatus.UNDERGO -> {
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        }
        DDLStatus.NEAR -> {
            indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
        }
        DDLStatus.PASSED -> {
            indicatorColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f)
        }
        DDLStatus.COMPLETED -> {
            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f)
        }
    }

    // 进度计算
    val safeTotal = habitTotalCount.coerceAtLeast(0)
    val safeCount = habitCount.coerceIn(0, maxOf(0, safeTotal))
    val progress = if (safeTotal == 0) 0f else safeCount.toFloat() / safeTotal.toFloat()
    val progressClamped = progress.coerceIn(0f, 1f)
    val progressTime = progressTime?.coerceIn(0f, 1f)?:1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelect?.invoke()
                    } else {
                        onCheckIn?.invoke()
                    }
                },
                onLongClick = { onLongPressSelect?.invoke() }
            )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = shape
        ) {
            Box(
                modifier = Modifier
                    .background(bgColor)
                    .fillMaxSize()
            ) {


                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                        .align(Alignment.CenterEnd)
                        .background(bgColor)
                ) {
                    if (progressClamped > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progressClamped)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(indicatorColor.copy(alpha = 0.40f), indicatorColor)
                                    )
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .align(Alignment.CenterEnd)
                        .background(indicatorColor.copy(alpha = 0.18f))
                ) {
                    if (progressTime > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progressTime)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(indicatorColor, indicatorColor.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // 顶部：标题 + 文字进度 + 星标
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .basicMarquee()
                        )
                        if (isStarred) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_star_filled),
                                contentDescription = null,
                                tint = indicatorColor.copy(alpha = 1f)
                            )
                        }
                    }

                    // 中间留白，保证底部信息贴靠左下
                    Spacer(modifier = Modifier.weight(1f))

                    // 左下角两行小字：每周/总计、剩余时间
                    if (remainingText.isNotBlank()) {
                        Text(
                            text = remainingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp)
                        )
                    }

                    if (freqAndTotalText.isNotBlank()) {
                        Text(
                            text = freqAndTotalText,
                            style = MaterialTheme.typography.bodySmallEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp)
                        )
                    }
                }
            }
        }

        if (selectionMode && selected) {
            SelectionOverlay(shape)
        }
    }
}

@Composable
fun SelectionOverlay(
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        )

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = iconResource(R.drawable.ic_ok),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}

// 小工具：把任意子 Box 放到父布局左上角
private fun Modifier.align(alignment: Alignment) = this.then(
    Modifier.layout { measurable, constraints ->
        val p = measurable.measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = when (alignment) {
                Alignment.TopStart -> 0
                Alignment.TopEnd -> constraints.maxWidth - p.width
                Alignment.BottomStart -> 0
                Alignment.BottomEnd -> constraints.maxWidth - p.width
                else -> 0
            }
            val y = when (alignment) {
                Alignment.TopStart, Alignment.TopEnd -> 0
                Alignment.BottomStart, Alignment.BottomEnd -> constraints.maxHeight - p.height
                else -> 0
            }
            p.place(x, y)
        }
    }
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview(showBackground = true)
private fun PreviewPageIndicator() {
    var selectedPage = DeadlineType.TASK
    MaterialTheme {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            leadingContent = {
                Box(modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                    TextPageIndicator(
                        text = stringResource(R.string.task),
                        onClick = { selectedPage = DeadlineType.TASK },
                        selected = selectedPage.toString(),
                        tag = DeadlineType.TASK.toString(),
                        badgeConfig = Triple(true, 2, true)
                    )
                }
            },
            trailingContent = {
                Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                    TextPageIndicator(
                        text = stringResource(R.string.habit),
                        onClick = { selectedPage = DeadlineType.HABIT },
                        selected = selectedPage.toString(),
                        tag = DeadlineType.HABIT.toString(),
                        badgeConfig = Triple(true, 1, false)
                    )
                }
            }
        ) {
            FilledIconButton(
                modifier = Modifier
                    .width(56.dp),
                onClick = {},
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_add), "")
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewDDLItemCard() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DDLItemCard(
                title = "DDL Sample",
                remainingTimeAlt = "1:00 截止",
                remainingTime = "1:00 截止",
                note = "备注：这里是一段可选的补充说明……",
                progressPercent = 50,
                isStarred = true,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            DDLItemCardSimplified(
                title = "DDL Sample",
                remainingTimeAlt = "1:00 截止",
                note = "备注：这里是一段可选的补充说明……",
                progress = 0.9f,
                isStarred = true,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            DDLItemCardSwipeable(
                title = "DDL Sample",
                remainingTimeAlt = "1:00 截止",
                note = "备注：这里是一段可选的补充说明……",
                progress = 0.9f,
                isStarred = true,
                onClick = {},
                status = DDLStatus.NEAR,
                onComplete = {},
                onDelete = {},
                selectionMode = true,
                selected = true
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(
    name = "Habit Waterfall 2-Columns (Phone)",
    showBackground = true
)
@Composable
fun PreviewHabitWaterfallTwoColumns() {
    MaterialTheme() {
        val samples = listOf(
            Triple("每日阅读 30 分钟", "每周 5 次 / 共计 30 次", "剩余 7 天"),
            Triple("晨跑 3km", "每周 3 次 / 共计 20 次", "剩余 2 天"),
            Triple("多喝水", "每天 8 杯 / 共计 240 杯", "已过期"),
            Triple("学习 Kotlin", "每周 7 次 / 共计 10 次", "已完成"),
            Triple("晚间拉伸", "每周 4 次 / 共计 14 次", "剩余 14 天"),
            Triple("英文单词 50 个", "每周 6 次 / 共计 30 次", "剩余 5 天"),
            Triple("冥想 10 分钟", "每周 5 次 / 共计 21 次", "剩余 20 天"),
            Triple("背肌弹力带", "每周 5 次 / 共计 10 次", "剩余 1 天"),
        )

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2), // 👉 手机上固定两列
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(samples.size) { i ->
                val (title, freqTotal, remain) = samples[i]
                val count = (5..25).random()
                val total = (count + 5..count + 20).random()
                val starred = (0..1).random() == 0
                val status = listOf(
                    DDLStatus.UNDERGO, DDLStatus.NEAR, DDLStatus.PASSED, DDLStatus.COMPLETED
                ).random()

                HabitItemCardSimplified(
                    title = title,
                    habitCount = count,
                    habitTotalCount = total,
                    freqAndTotalText = freqTotal,
                    remainingText = remain,
                    isStarred = starred,
                    status = status,
                    progressTime = null,
                    onCheckIn = {} // 预览中留空
                )
            }
        }
    }
}
