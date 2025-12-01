package com.aritxonly.deadliner.ui.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.intro.IntroWizardViewModel
import com.aritxonly.deadliner.intro.WizardStep
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.main.DDLItemCard
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.deadliner.localutils.GlobalUtils
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.ui.util.lerp
import com.aritxonly.deadliner.ui.main.SelectionOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ClassicWizardWithTransition(
    vm: IntroWizardViewModel,
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsState()

    AnimatedContent(
        modifier = modifier,
        targetState = state.currentStep,
        label = "classic-wizard",
        transitionSpec = {
            val enter = slideInHorizontally(
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) { fullWidth -> fullWidth } + fadeIn(tween(180))

            val exit = slideOutHorizontally(
                animationSpec = tween(220, easing = FastOutLinearInEasing)
            ) { fullWidth -> -fullWidth / 3 } + fadeOut(tween(160))

            enter togetherWith exit
        }
    ) { step ->
        ClassicWizard(step = step, vm = vm)
    }
}

@Composable
fun ClassicWizard(
    step: WizardStep,
    vm: IntroWizardViewModel
) {
    val config: WizardScreenConfig? = when (step) {
        WizardStep.AddEntry -> WizardScreenConfig(
            title = stringResource(R.string.wizard_add_a_task),
            description = stringResource(R.string.wizard_add_a_task_desc),
            highlight = HighlightTarget.Fab,
            interaction = InteractionTarget.ClickAddFab,
            onInteractionSatisfied = { vm.onAddEntryClicked() }
        )

        WizardStep.SwipeRightComplete -> WizardScreenConfig(
            title = stringResource(R.string.wizard_mark_as_complete),
            description = stringResource(R.string.wizard_mark_as_complete_desc),
            highlight = HighlightTarget.MainCard,
            interaction = InteractionTarget.SwipeTaskRight,
            onInteractionSatisfied = { vm.onSwipeRightComplete() }
        )

        WizardStep.SwipeLeftDelete -> WizardScreenConfig(
            title = stringResource(R.string.wizard_delete),
            description = stringResource(R.string.wizard_delete_desc),
            highlight = HighlightTarget.MainCard,
            interaction = InteractionTarget.SwipeTaskLeft,
            onInteractionSatisfied = { vm.onSwipeLeftDelete() }
        )

        WizardStep.AiEntry -> WizardScreenConfig(
            title = stringResource(R.string.wizard_deadliner_ai),
            description = stringResource(R.string.wizard_deadliner_ai_classic_desc),
            highlight = HighlightTarget.AiIcon,
            interaction = InteractionTarget.ClickAiIcon,
            onInteractionSatisfied = { vm.onAiEntryTriggered() }
        )

        // 纯信息页 / Done 不在 ClassicMainWizardScreen 里画
        else -> null
    }

    if (config != null) {
        ClassicMainWizardScreen(config)
    } else {
        when (step) {
            WizardStep.AddEntryInfo ->
                InfoStepScreen(
                    title = stringResource(R.string.wizard_add_a_task_info),
                    description = stringResource(R.string.wizard_add_a_task_info_desc),
                    primaryButtonText = stringResource(R.string.i_know_and_continue),
                    onPrimaryClick = { vm.onAddEntryInfoNext() },
                    screenshot = R.drawable.demo_add
                )
            WizardStep.AiInfo ->
                InfoStepScreen(
                    title = stringResource(R.string.wizard_deadliner_ai_info),
                    description = stringResource(R.string.wizard_deadliner_ai_info_desc),
                    primaryButtonText = stringResource(R.string.finish_wizard),
                    onPrimaryClick = { vm.onAiInfoNext() },
                    screenshot = R.drawable.demo_ai
                )
            else -> null
        }
    }
}

@Composable
fun ClassicMainWizardScreen(
    config: WizardScreenConfig
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ClassicWizardTopBar()
        },
        bottomBar = {
            ClassicWizardBottomBar(
                highlightFab = config.highlight == HighlightTarget.Fab,
                highlightAi = config.highlight == HighlightTarget.AiIcon,
                onFabClick = {
                    if (config.interaction == InteractionTarget.ClickAddFab) {
                        config.onInteractionSatisfied()
                    }
                },
                onAiClick = {
                    if (config.interaction == InteractionTarget.ClickAiIcon) {
                        config.onInteractionSatisfied()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ClassicTabsRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                )

                // 列表区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wizard_classic_demo_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    key(config.interaction) {
                        // 主演示卡片（真正可滑动）
                        DDLItemCardSwipeableClassic(
                            title = when (config.interaction) {
                                InteractionTarget.SwipeTaskRight ->
                                    stringResource(R.string.wizard_swipe_right_title)

                                InteractionTarget.SwipeTaskLeft ->
                                    stringResource(R.string.wizard_swipe_left_title)

                                else ->
                                    stringResource(R.string.wizard_sample_task_title)
                            },
                            remainingTimeAlt = stringResource(R.string.wizard_main_time_today),
                            remainingTime = stringResource(R.string.wizard_main_remaining_time),
                            note = stringResource(R.string.wizard_main_demo_note),
                            progressPercent = 40,
                            isStarred = true,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = null,
                            onComplete = {
                                // 只有“右滑完成”步骤需要在完成时过关
                                if (config.interaction == InteractionTarget.SwipeTaskRight) {
                                    config.onInteractionSatisfied()
                                }
                            },
                            onDelete = {
                                // 只有“左滑删除”步骤需要在删除时过关
                                if (config.interaction == InteractionTarget.SwipeTaskLeft) {
                                    config.onInteractionSatisfied()
                                }
                            }
                        )
                    }

                    DDLItemCard(
                        title = stringResource(R.string.wizard_secondary_task_title),
                        remainingTimeAlt = stringResource(R.string.wizard_secondary_time_tomorrow),
                        remainingTime = stringResource(R.string.wizard_secondary_remaining_time),
                        note = stringResource(R.string.wizard_secondary_demo_note),
                        progressPercent = 60,
                        isStarred = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = null
                    )
                }

                // 底部提示面板：使用 config.title / description
                WizardHintPanel(
                    title = config.title,
                    description = config.description,
                    extra = null
                )
            }

            // Spotlight 覆盖层：只给 FAB / 主卡片做大范围高亮
            when (config.highlight) {
                HighlightTarget.Fab -> {
                    SpotlightFabOverlay()
                }

                HighlightTarget.MainCard -> {
                    SpotlightMainCardOverlay()
                }

                HighlightTarget.AiIcon,
                HighlightTarget.None -> Unit
            }
        }
    }
}

// ---------- 顶部栏：只保留搜索 + 设置 ----------

@Composable
private fun ClassicWizardTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Deadliner",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { /* 搜索占位，无交互 */ }) {
                Icon(
                    imageVector = iconResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.search_hint),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = { /* 设置占位，无交互 */ }) {
                Icon(
                    imageVector = iconResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ---------- Tab 区（假 TabLayout） ----------

@Composable
private fun ClassicTabsRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClassicTabChip(
            text = stringResource(R.string.task),
            selected = true
        )
        ClassicTabChip(
            text = stringResource(R.string.habit),
            selected = false
        )
    }
}

@Composable
private fun ClassicTabChip(
    text: String,
    selected: Boolean
) {
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- 底栏 + FAB + AI（DeepSeek 图标） ----------

@Composable
private fun ClassicWizardBottomBar(
    highlightFab: Boolean,
    highlightAi: Boolean,
    onFabClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val aiDrawable = GlobalUtils.getDeadlinerAIConfig().getCurrentLogo()

    BottomAppBar(
        actions = {
            // 左 1：AI（DeepSeek 图标）
            Box(
                modifier = Modifier.height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onAiClick) {
                    Icon(
                        iconResource(aiDrawable),
                        contentDescription = "Deadliner AI"
                    )
                }

                if (highlightAi) {
                    SpotlightCircle(
                        modifier = Modifier.matchParentSize(),
                        align = Alignment.Center
                    )
                }
            }
            IconButton(onClick = { /* noop */ }) {
                Icon(
                    imageVector = iconResource(R.drawable.ic_chart),
                    contentDescription = null
                )
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier.height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .align(Alignment.Center)
                ) {
                    FloatingActionButton(
                        onClick = onFabClick,
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(
                            imageVector = iconResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add_edit_deadline)
                        )
                    }

                    if (highlightFab) {
                        SpotlightCircle(
                            modifier = Modifier.matchParentSize(),
                            align = Alignment.Center
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DDLItemCardSwipeableClassic(
    title: String,
    remainingTimeAlt: String,
    remainingTime: String,
    note: String,
    progressPercent: Int,
    isStarred: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null
) {
    val swipeEnabled = !selectionMode

    var hasTriggered by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (!swipeEnabled) return@rememberSwipeToDismissBoxState false

            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!hasTriggered) {
                        hasTriggered = true
                        onComplete()
                    }
                    false   // 不真正 dismiss，回到原位
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!hasTriggered) {
                        hasTriggered = true
                        onDelete()
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

    // 回到静止位置后，允许再次触发 onComplete / onDelete
    LaunchedEffect(dismissState) {
        snapshotFlow {
            val off = runCatching { dismissState.requireOffset() }.getOrElse { 0f }
            val atRest = abs(off) < 0.5f &&
                    dismissState.currentValue == SwipeToDismissBoxValue.Settled &&
                    dismissState.targetValue  == SwipeToDismissBoxValue.Settled
            atRest
        }
            .distinctUntilChanged()
            .collectLatest { atRest ->
                if (atRest) {
                    hasTriggered = false
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

            val direction = dismissState.dismissDirection

            val actionColor: Color
            val icon: ImageVector
            val alignment: Alignment

            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // 右滑：完成（绿）
                    actionColor = colorResource(R.color.chart_green)
                    icon = ImageVector.vectorResource(R.drawable.ic_check)
                    alignment = Alignment.CenterStart
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // 左滑：删除（红）
                    actionColor = colorResource(R.color.chart_red)
                    icon = ImageVector.vectorResource(R.drawable.ic_delete)
                    alignment = Alignment.CenterEnd
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
                DDLItemCard(
                    title = title,
                    remainingTimeAlt = remainingTimeAlt,
                    remainingTime = remainingTime,
                    note = note,
                    progressPercent = progressPercent,
                    isStarred = isStarred,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .onSizeChanged { widthPx = it.width },
                    onClick = null   // 点击交互在外层 combinedClickable 已处理
                )

                if (selectionMode && selected) {
                    SelectionOverlay(
                        shape = shape,
                        modifier = Modifier.height(76.dp)
                    )
                }
            }
        }
    )
}

// ---------- Spotlight 覆盖层（大范围辅助高亮） ----------

@Composable
private fun SpotlightFabOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 40.dp)
                .height(96.dp)
        ) {
            SpotlightCircle(
                modifier = Modifier.matchParentSize(),
                align = Alignment.Center
            )
        }
    }
}

@Composable
private fun SpotlightMainCardOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .height(140.dp)
        ) {
            SpotlightCircle(
                modifier = Modifier.matchParentSize(),
                align = Alignment.Center
            )
        }
    }
}