package com.aritxonly.deadliner.ui.intro

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.intro.IntroWizardViewModel
import com.aritxonly.deadliner.intro.WizardStep
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import com.aritxonly.deadliner.ui.main.simplified.MainSearchBar

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SimplifiedWizardWithTransition(
    vm: IntroWizardViewModel,
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsState()

    AnimatedContent(
        modifier = modifier,
        targetState = state.currentStep,
        label = "simplified-wizard",
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
        SimplifiedWizard(step = step, vm = vm)
    }
}

@Composable
fun SimplifiedWizard(
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
            description = stringResource(R.string.wizard_deadliner_ai_simplified_desc),
            highlight = HighlightTarget.AiIcon,            // 之后在简洁模式界面里用它高亮底部导航
            interaction = InteractionTarget.SwipeUpBottomNav,
            onInteractionSatisfied = { vm.onAiEntryTriggered() }
        )

        // 纯信息页 / Done 不在 SimplifiedMainWizardScreen 里画
        else -> null
    }

    if (config != null) {
        SimplifiedMainWizardScreen(config)
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
fun SimplifiedMainWizardScreen(
    config: WizardScreenConfig
) {
    val completeHandler: (() -> Unit)? =
        if (config.interaction == InteractionTarget.SwipeTaskRight)
            config.onInteractionSatisfied
        else
            null

    val deleteHandler: (() -> Unit)? =
        if (config.interaction == InteractionTarget.SwipeTaskLeft)
            config.onInteractionSatisfied
        else
            null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SimplifiedWizardTopBar()
        },
        bottomBar = {
            SimplifiedWizardBottomBar(
                highlightFab = config.highlight == HighlightTarget.Fab,
                highlightAi = config.highlight == HighlightTarget.AiIcon,
                interaction = config.interaction,
                onFabClick = {
                    if (config.interaction == InteractionTarget.ClickAddFab) {
                        config.onInteractionSatisfied()
                    }
                },
                onSwipeUp = {
                    if (config.interaction == InteractionTarget.SwipeUpBottomNav) {
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
                // 顶部说明 / 占位
                Text(
                    text = stringResource(R.string.wizard_simplified_title_sample),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 列表区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    key(config.interaction) {
                        // 主演示卡片（真正可滑动）
                        DDLItemCardSwipeable(
                            title = when (config.interaction) {
                                InteractionTarget.SwipeTaskRight ->
                                    stringResource(R.string.wizard_swipe_right_title)

                                InteractionTarget.SwipeTaskLeft ->
                                    stringResource(R.string.wizard_swipe_left_title)

                                else ->
                                    stringResource(R.string.wizard_sample_task_title)
                            },
                            remainingTimeAlt = stringResource(R.string.wizard_main_time_today),
                            note = stringResource(R.string.wizard_main_demo_note),
                            progress = 0.4f,
                            isStarred = true,
                            status = DDLStatus.UNDERGO,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = null,
                            onComplete = {
                                completeHandler?.invoke()
                            },
                            onDelete = {
                                deleteHandler?.invoke()
                            }
                        )
                    }

                    // 次要演示卡片：同样用 swipeable，但回调为空
                    DDLItemCardSwipeable(
                        title = stringResource(R.string.wizard_secondary_task_title),
                        remainingTimeAlt = stringResource(R.string.wizard_secondary_remaining_time),
                        note = stringResource(R.string.wizard_secondary_demo_note),
                        progress = 0.6f,
                        isStarred = false,
                        status = DDLStatus.NEAR,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = null,
                        onComplete = {},
                        onDelete = {}
                    )
                }

                // 底部提示面板：统一用 WizardScreenConfig 的文案
                WizardHintPanel(
                    title = config.title,
                    description = config.description,
                    extra = null
                )
            }

            // Spotlight 覆盖层：粗略对齐简洁模式下的热点区域
            when (config.highlight) {
                HighlightTarget.MainCard -> {
                    SimplifiedSpotlightMainCardOverlay()
                }

                HighlightTarget.AiIcon -> {
                    SimplifiedSpotlightBottomBarOverlay()
                }

                HighlightTarget.Fab,
                HighlightTarget.None -> Unit
            }
        }
    }
}

@Composable
private fun SimplifiedWizardTopBar() {
    val textFieldState = rememberTextFieldState()

    MainSearchBar(
        textFieldState = textFieldState,
        onQueryChanged = {},
        searchResults = listOf(),
        expanded = false,
        selectedPage = DeadlineType.TASK,
    )
}

@Composable
private fun SimplifiedWizardBottomBar(
    highlightFab: Boolean,
    highlightAi: Boolean,
    interaction: InteractionTarget,
    onFabClick: () -> Unit,
    onSwipeUp: () -> Unit
) {
    // 用一个“伪 FloatingToolbar”，兼容点击 + 和上滑手势
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .pointerInput(interaction) {
                    if (interaction == InteractionTarget.SwipeUpBottomNav) {
                        detectVerticalDragGestures { _, dragAmount ->
                            // 向上拖动一段距离视为唤醒 AI
                            if (-dragAmount > 30f) {
                                onSwipeUp()
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.task),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FilledIconButton(
                        onClick = onFabClick
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

                Text(
                    text = stringResource(R.string.habit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // AI 入口高亮：在整个 Toolbar 上加一圈更大的 Spotlight
            if (highlightAi) {
                SpotlightCircle(
                    modifier = Modifier
                        .matchParentSize(),
                    align = Alignment.Center
                )
            }
        }
    }
}


@Composable
private fun SimplifiedSpotlightMainCardOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .height(120.dp)
        ) {
            SpotlightCircle(
                modifier = Modifier.matchParentSize(),
                align = Alignment.Center
            )
        }
    }
}

@Composable
private fun SimplifiedSpotlightBottomBarOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 48.dp)
                .fillMaxWidth(0.9f)
                .height(72.dp)
        ) {
            SpotlightCircle(
                modifier = Modifier.matchParentSize(),
                align = Alignment.Center
            )
        }
    }
}