@file:OptIn(ExperimentalMaterial3Api::class)
package com.aritxonly.deadliner.ui.main.simplified

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.toString

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainDisplay(
    ddlList: List<DDLItem>,
    dueSoonCounts: Map<DeadlineType, Int>,
    refreshState: MainViewModel.RefreshState,
    selectedPage: DeadlineType,
    onSearch: (String) -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
    vm: MainViewModel,
    listState: LazyListState,
    onRequestBackdropBlur: (Boolean) -> Unit = {},
    onCelebrate: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<DDLItem?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = refreshState is MainViewModel.RefreshState.Loading && !refreshState.silent

    var moreExpanded by remember { mutableStateOf(false) }
    var moreAnchorRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val profile by UserProfileRepository.profile.collectAsState(initial = UserProfile())
    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname) }

    val avatarPainter: Painter? by remember(profile.avatarFileName) {
        mutableStateOf<Painter?>(
            if (profile.avatarFileName != null) {
                val file = File(context.filesDir, "avatars/${profile.avatarFileName}")
                if (file.exists()) {
                    // 读取文件并转为 Painter
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) BitmapPainter(bitmap.asImageBitmap()) else null
                } else {
                    null
                }
            } else {
                null
            }
        )
    }
    val useAvatar = avatarPainter != null

    val needsBlur by remember {
        derivedStateOf { pendingDelete != null || moreExpanded }
    }
    LaunchedEffect(needsBlur) {
        onRequestBackdropBlur(needsBlur)
    }
    DisposableEffect(Unit) {
        onDispose { onRequestBackdropBlur(false) }
    }

    Column(modifier) {
        val textFieldState = rememberTextFieldState()

        MainSearchBar(
            textFieldState = textFieldState,
            onSearch = onSearch,
            searchResults = emptyList(),
            onMoreClick = { moreExpanded = true },
            onMoreAnchorChange = { rect -> moreAnchorRect = rect },
            useAvatar = useAvatar,
            avatarPainter = avatarPainter
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = {
                vm.refreshFromPull(selectedPage)
            },
            modifier = modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 96.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(
                    items = ddlList,
                    key = { it.id }
                ) { item ->
                    when (selectedPage) {
                        DeadlineType.TASK -> {
                            val startTime = GlobalUtils.parseDateTime(item.startTime)
                            val endTime = GlobalUtils.parseDateTime(item.endTime)
                            val now = LocalDateTime.now()

                            val remainingTimeText =
                                if (!item.isCompleted)
                                    GlobalUtils.buildRemainingTime(
                                        context,
                                        startTime,
                                        endTime,
                                        true,
                                        now
                                    )
                                else stringResource(R.string.completed)

                            val progress = computeProgress(startTime, endTime, now)
                            val status =
                                DDLStatus.calculateStatus(startTime, endTime, now, item.isCompleted)

                            DDLItemCardSwipeable(
                                title = item.name,
                                remainingTimeAlt = remainingTimeText,
                                note = item.note,
                                progress = progress,
                                isStarred = item.isStared,
                                status = status,
                                onClick = {
                                    val intent = DeadlineDetailActivity.newIntent(context, item)
                                    activity.startActivity(intent)
                                },
                                onComplete = {
                                    GlobalUtils.triggerVibration(activity, 100)

                                    val realItem = DDLRepository().getDDLById(item.id)
                                        ?: return@DDLItemCardSwipeable
                                    val newItem = realItem.copy(
                                        isCompleted = !realItem.isCompleted,
                                        completeTime = if (!realItem.isCompleted) LocalDateTime.now()
                                            .toString() else ""
                                    )
                                    DDLRepository().updateDDL(newItem)
                                    vm.loadData(selectedPage)
                                    if (newItem.isCompleted) {
                                        if (GlobalUtils.fireworksOnFinish) onCelebrate?.invoke()
                                        Toast.makeText(
                                            activity,
                                            R.string.toast_finished,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            activity,
                                            R.string.toast_definished,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onDelete = {
                                    GlobalUtils.triggerVibration(activity, 200)
                                    pendingDelete = item
                                }
                            )
                        }

                        DeadlineType.HABIT -> {
                            Text(item.name)
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    DDLRepository().deleteDDL(target.id)
                    DeadlineAlarmScheduler.cancelAlarm(activity.applicationContext, target.id)
                    pendingDelete = null
                    vm.loadData(selectedPage)
                    Toast.makeText(context, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (moreAnchorRect != null) {
        MorePanelFromAnchor(
            moreExpanded = moreExpanded,
            anchorRect = moreAnchorRect,
            useAvatar = useAvatar,
            avatarPainter = avatarPainter,
            nickname = nickname,
            activity = activity,
        ) {
            moreExpanded = false
        }
    }
}

fun computeProgress(
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    now: LocalDateTime = LocalDateTime.now(),
    progressDir: Boolean = GlobalUtils.progressDir
): Float {
    if (startTime == null || endTime == null) return 0f
    val total = Duration.between(startTime, endTime).toMinutes().toFloat().coerceAtLeast(1f)
    val elapsed = Duration.between(startTime, now).toMinutes().toFloat().coerceIn(0f, total)
    val remaining = total - elapsed

    return if (progressDir) {
        // 已经过的时间占比
        elapsed / total
    } else {
        // 剩余的时间占比
        remaining / total
    }
}

@Composable
fun MainSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onMoreAnchorChange: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    useAvatar: Boolean = false,
    avatarPainter: Painter? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isEnabled = GlobalUtils.motivationalQuotes
    val excitementArray = stringArrayResource(id = R.array.excitement_array).toList()

    var idx by rememberSaveable {
        mutableIntStateOf(
            if (excitementArray.isNotEmpty()) (0 until excitementArray.size).random() else 0
        )
    }

    LaunchedEffect(isEnabled, excitementArray) {
        if (!isEnabled || excitementArray.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(30_000)
            idx = (idx + 1) % excitementArray.size
        }
    }

    val searchBarPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else 16.dp,
        label = "Search bar padding"
    )

    Box(
        modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = searchBarPadding)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = { textFieldState.edit { replace(0, length, it) } },
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        expanded = false
                        focusManager.clearFocus()
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },

                    placeholder = {
                        val hint = if (expanded) {
                            stringResource(R.string.search_hint)
                        } else {
                            if (isEnabled && excitementArray.isNotEmpty())
                                excitementArray[idx]
                            else stringResource(R.string.search_hint)
                        }
                        val style = if (expanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium

                        Text(hint, style = style, maxLines = 1)
                    },

                    leadingIcon = {
                        if (expanded) {
                            IconButton(
                                onClick = {
                                    expanded = false
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                                    contentDescription = "返回"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                contentDescription = "搜索"
                            )
                        }
                    },

                    trailingIcon = {
                        if (!expanded) {
                            val iconModifier = Modifier
                                .clip(CircleShape)
                                .onGloballyPositioned { coords ->
                                    onMoreAnchorChange(coords.boundsInWindow())
                                }
                            if (useAvatar && avatarPainter != null) {
                                IconButton(onClick = onMoreClick, modifier = iconModifier.size(32.dp)) {
                                    Image(
                                        painter = avatarPainter,
                                        contentDescription = "用户",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                IconButton(onClick = onMoreClick, modifier = iconModifier) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_more),
                                        contentDescription = stringResource(R.string.settings_more)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                searchResults.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                textFieldState.edit { replace(0, length, result) }
                                expanded = false
                                focusManager.clearFocus()
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun MorePanelFromAnchor(
    moreExpanded: Boolean,
    anchorRect: androidx.compose.ui.geometry.Rect?,
    useAvatar: Boolean,
    avatarPainter: Painter?,
    nickname: String,
    activity: MainActivity,
    onDismiss: () -> Unit,
) {
    if (anchorRect == null) return

    // 可见性过渡：允许退场动画期间继续挂载
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(moreExpanded) { visibleState.targetState = moreExpanded }
    if (!(visibleState.currentState || visibleState.targetState)) return

    Popup(
        alignment = Alignment.TopStart,
        offset = with(LocalDensity.current) {
            IntOffset(anchorRect.left.toInt(), anchorRect.bottom.toInt())
        },
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        onDismissRequest = onDismiss
    ) {
        val density = LocalDensity.current

        // 起点（像素）
        val startXpx = anchorRect.left
        val startYpx = anchorRect.top

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val parentWpx = with(density) { maxWidth.toPx() }
            val parentHpx = with(density) { maxHeight.toPx() }
            val marginPx  = with(density) { 16.dp.toPx() }

            var cardSize by remember { mutableStateOf(IntSize.Zero) }
            val cardWpx = cardSize.width.toFloat()
            val cardHpx = cardSize.height.toFloat()

            // 目标点：贴近锚点且不越界（安全处理避免空区间）
            val safeRight  = max(marginPx, parentWpx - cardWpx - marginPx)
            val safeBottom = max(marginPx, parentHpx - cardHpx - marginPx)
            val targetXpx  = (startXpx - with(density) { 12.dp.toPx() }).coerceIn(marginPx, safeRight)
            val targetYpx  = (startYpx + with(density) { 8.dp.toPx()  }).coerceIn(marginPx, safeBottom)

            // 统一过渡：progress 0→1（入场），1→0（退场）
            val transition = updateTransition(visibleState, label = "more-panel-popup")
            val progress by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 280, easing = FastOutSlowInEasing) },
                label = "progress"
            ) { if (it) 1f else 0f }

            // 遮罩（半透明即可；真正的背景模糊放在 SimplifiedHost 统一做）
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            )

            // 位移/缩放/透明度/圆角随 progress 插值（像素插值更稳定）
            val curX   = lerp(startXpx, targetXpx, progress)
            val curY   = lerp(startYpx, targetYpx, progress)
            val scale  = lerp(0.6f, 1f, progress)
            val alpha  = progress
            val radius = lerpDp(16.dp, 24.dp, progress)

            Box(
                Modifier
                    .graphicsLayer {
                        translationX = curX
                        translationY = curY
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                MorePanelCard(
                    onDismiss = onDismiss,
                    avatarPainter = if (useAvatar) avatarPainter else null,
                    nickname = nickname,
                    activity = activity,
                    modifier = Modifier
                        .onSizeChanged { cardSize = it }
                        .widthIn(max = 360.dp)
                        .shadow(16.dp, RoundedCornerShape(radius))
                        .clip(RoundedCornerShape(radius))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

// -------- 小工具 --------
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp =
    start + (stop - start) * fraction