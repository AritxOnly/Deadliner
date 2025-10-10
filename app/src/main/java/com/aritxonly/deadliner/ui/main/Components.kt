package com.aritxonly.deadliner.ui.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
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
    val containerColor = if (selected == tag) MaterialTheme.colorScheme.primaryContainer else Color.Companion.Transparent
    val (enabled, num, detail) = badgeConfig

    Button(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(containerColor = containerColor)
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
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Companion.Center
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
    status: DDLStatus = DDLStatus.UNDERGO
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val progressClamped = progress.coerceIn(0f, 1f)
    val indicatorColor: Color
    val bgColor: Color
    when (status) {
        DDLStatus.UNDERGO -> {
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        }
        DDLStatus.NEAR -> {
            indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        }
        DDLStatus.PASSED -> {
            indicatorColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        }
        DDLStatus.COMPLETED -> {
            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(bgColor)
                .fillMaxWidth()
                .height(76.dp) // 👈 固定高度
        ) {
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 第二行：备注（可空），如果没有就占位 Spacer 保持底部留白
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
                    Spacer(modifier = Modifier.height(0.dp)) // 👈 不显示文字，但保持布局干净
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
    onDelete: () -> Unit
) {
    var hasTriggered by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
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
            // 当前方向（可能为 null，表示未开始滑）
            val dir = dismissState.dismissDirection

            // 动作色（红=删除；绿=完成）与对齐位置
            val actionColor: Color
            val icon: ImageVector
            val alignment: Alignment

            when (dir) {
                SwipeToDismissBoxValue.EndToStart -> {
                    actionColor = colorResource(R.color.chart_red)
                    icon = ImageVector.vectorResource(R.drawable.ic_delete)
                    alignment = Alignment.CenterEnd
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    actionColor = colorResource(R.color.chart_green)
                    icon = ImageVector.vectorResource(R.drawable.ic_check)
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
            DDLItemCardSimplified(
                title = title,
                remainingTimeAlt = remainingTimeAlt,
                note = note,
                progress = progress,
                isStarred = isStarred,
                modifier = modifier
                    .clip(shape)
                    .onSizeChanged { widthPx = it.width },
                onClick = onClick,
                status = status
            )
        }
    )
}


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
        Column {
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
        }
    }
}