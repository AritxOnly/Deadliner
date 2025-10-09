@file:OptIn(ExperimentalMaterial3Api::class)
package com.aritxonly.deadliner.ui.main.simplified

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class, FlowPreview::class)
@Composable
fun MainDisplay(
    ddlList: List<DDLItem>,
    dueSoonCounts: Map<DeadlineType, Int>,
    refreshState: MainViewModel.RefreshState,
    selectedPage: DeadlineType,
    activity: MainActivity,
    onSearchBarExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    vm: MainViewModel,
    listState: LazyListState,
    onRequestBackdropBlur: (Boolean) -> Unit = {},
    onCelebrate: (() -> Unit)? = null
) {
    val context = LocalContext.current

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
        var suggestions by rememberSaveable { mutableStateOf(emptyList<DDLItem>()) }
        var base by remember { mutableStateOf<List<DDLItem>>(emptyList()) }
        LaunchedEffect(selectedPage) {
            base = vm.getBaseList(selectedPage)
        }
        LaunchedEffect(textFieldState) {
            snapshotFlow { textFieldState.text.toString() }
                .distinctUntilChanged()
                .debounce(250)
                .collect { q ->
                    val f = SearchFilter.parse(q)
                    suggestions = if (q.isBlank()) emptyList()
                    else base.asSequence()
                        .filter { f.matches(it) }
                        .toList()
                }
        }

        MainSearchBar(
            textFieldState = textFieldState,
            searchResults = suggestions,
            onQueryChanged = { q ->
                val f = SearchFilter.parse(q)
                suggestions = if (q.isBlank()) emptyList()
                else base.filter { f.matches(it) }.toList()
            },
            onMoreClick = { moreExpanded = true },
            onMoreAnchorChange = { rect -> moreAnchorRect = rect },
            useAvatar = useAvatar,
            avatarPainter = avatarPainter,
            activity = activity,
            onExpandedChangeExternal = onSearchBarExpandedChange,
            onItemDelete = { pendingDelete = it }
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
            when (selectedPage) {
                DeadlineType.TASK -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            bottom = 96.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                            .fadingTopEdge(height = 16.dp),
                        state = listState
                    ) {
                        itemsIndexed(
                            items = ddlList,
                            key = { _, it -> it.id }
                        ) { index, item ->
                            AnimatedItem(
                                item = item,
                                index = index
                            ) {
                                TaskItem(
                                    item = item,
                                    activity = activity,
                                    updateDDL = { newItem ->
                                        DDLRepository().updateDDL(newItem)
                                        vm.loadData(selectedPage)
                                    },
                                    celebrate = {
                                        if (GlobalUtils.fireworksOnFinish) onCelebrate?.invoke()
                                    },
                                    onDelete = {
                                        pendingDelete = item
                                    },
                                )
                            }
                        }
                    }
                }
                DeadlineType.HABIT -> {

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

    moreAnchorRect?.let {
        MorePanelFromAnchor(
            moreExpanded = moreExpanded,
            anchorRect = it.toAndroidRect(),
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
    onQueryChanged: (String) -> Unit,
    searchResults: List<DDLItem>,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onMoreAnchorChange: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    useAvatar: Boolean = false,
    avatarPainter: Painter? = null,
    activity: MainActivity,
    onExpandedChangeExternal: (Boolean) -> Unit = {},
    onItemDelete: (DDLItem) -> Unit = {}
) {
    val context = LocalContext.current

    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isEnabled = GlobalUtils.motivationalQuotes
    val excitementArray = stringArrayResource(id = R.array.excitement_array).toList()

    var idx by rememberSaveable {
        mutableIntStateOf(
            if (excitementArray.isNotEmpty()) (0 until excitementArray.size).random() else 0
        )
    }

    LaunchedEffect(expanded) { onExpandedChangeExternal(expanded) }

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
                    onQueryChange = {
                        onQueryChanged(it)
                        textFieldState.edit { replace(0, length, it) }
                    },
                    onSearch = {
                        textFieldState.clearText()
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
                                contentDescription = stringResource(R.string.search_events)
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
                                        contentDescription = stringResource(R.string.user),
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
                        } else {
                            IconButton(onClick = { textFieldState.clearText() }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.close)
                                )
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 96.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .fadingTopEdge(height = 16.dp),
            ) {
                items(
                    items = searchResults,
                    key = { it.id }
                ) { item ->
                    when (item.type) {
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

                            DDLItemCardSimplified(
                                title = item.name,
                                remainingTimeAlt = remainingTimeText,
                                note = item.note,
                                progress = progress,
                                isStarred = item.isStared,
                                status = status,
                                onClick = {
                                    val intent = DeadlineDetailActivity.newIntent(context, item)
                                    activity.startActivity(intent)
                                }
                            )
                        }
                        DeadlineType.HABIT -> {

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MorePanelFromAnchor(
    moreExpanded: Boolean,
    anchorRect: Rect?,
    useAvatar: Boolean,
    avatarPainter: Painter?,
    nickname: String,
    activity: MainActivity,
    onDismiss: () -> Unit,
) {
    if (anchorRect == null) return

    val density = LocalDensity.current
    val visibleState = remember { MutableTransitionState(false) }

    // 控制动画进入 / 退出
    LaunchedEffect(moreExpanded) { visibleState.targetState = moreExpanded }
    if (!(visibleState.currentState || visibleState.targetState)) return

    // Popup 以 window 坐标为原点
    Popup(
        alignment = Alignment.TopStart,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        onDismissRequest = onDismiss
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val parentWpx = with(density) { maxWidth.toPx() }
            val parentHpx = with(density) { maxHeight.toPx() }

            var cardSize by remember { mutableStateOf(IntSize.Zero) }
            val cardW = cardSize.width.toFloat()
            val cardH = cardSize.height.toFloat()

            // 锚点（右上角）
            val anchorX = anchorRect.right.toFloat()
            val anchorY = anchorRect.top.toFloat()

            Log.d("Anchor", "$anchorX, $anchorY")

            // 目标位置（居中）
            val bias = 0.8f
            val centerX = parentWpx / 2f - cardW / 2f
            val centerY = lerp(parentHpx / 2f - cardH / 2f, anchorY, bias)

            // 动画进度
            val transition = updateTransition(visibleState, label = "more-panel-popup")
            val progress by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 360, easing = FastOutSlowInEasing) },
                label = "progress"
            ) { if (it) 1f else 0f }

            // 动画插值：右上角 → 中央
            val curX = lerp(anchorX, centerX, progress)
            val curY = lerp(anchorY, centerY, progress)
            val scale = lerp(0.6f, 1f, progress)
            val alpha = progress

            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            )

            // 卡片
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = curX
                        translationY = curY
                        transformOrigin = TransformOrigin(1f, 0f)
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .onSizeChanged { cardSize = it }
            ) {
                MorePanelCard(
                    onDismiss = onDismiss,
                    avatarPainter = if (useAvatar) avatarPainter else null,
                    nickname = nickname,
                    activity = activity,
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(max = 360.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

// -------- 小工具 --------
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/**
 * 给可滚动容器的「可视区域顶部」添加原生式渐隐（fading edge）。
 * 无覆盖层、无 RenderEffect，仅对自身内容做 Alpha 遮罩。
 *
 * @param height 渐隐高度（像素越大，过渡越长）
 * @param inverted 当需要做“底部渐隐”时可设 true；默认做顶部
 */
fun Modifier.fadingTopEdge(
    height: Dp = 32.dp,
    inverted: Boolean = false
): Modifier = this
    // 关键：开启离屏合成，才能让后续的 DstIn 作为整块内容的 Alpha 遮罩生效
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        // 先正常画内容
        drawContent()

        val h = height.toPx().coerceAtLeast(1f)

        // 目标：用 DstIn 画一层“遮罩”：
        // - 遮罩在渐隐带内：从 0→1 的 Alpha 梯度（顶部透明、向下变实）
        // - 渐隐带下方：全 1（不影响内容）
        // 这样内容在顶部就会被“吃掉”一段，越靠近顶边越透明，形成原生式 fading edge。
        // 注意：DstIn 保留 destination（内容）与 source（遮罩）交集，且按 source 的 Alpha 调整内容不透明度。

        // 1) 渐隐带
        drawRect(
            brush = Brush.verticalGradient(
                colors = if (!inverted) listOf(Color.Transparent, Color.Black)
                else listOf(Color.Black, Color.Transparent),
                startY = 0f,
                endY = h
            ),
            size = size.copy(height = h),
            blendMode = BlendMode.DstIn
        )

        // 2) 渐隐带之外全部填充为不透明遮罩，确保其余区域不被影响
        if (!inverted) {
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, h),
                size = size.copy(height = size.height - h),
                blendMode = BlendMode.DstIn
            )
        } else {
            // 做底部渐隐时，上方整块要保持不透明
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, 0f),
                size = size.copy(height = size.height - h),
                blendMode = BlendMode.DstIn
            )
        }
    }