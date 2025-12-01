package com.aritxonly.deadliner.ui.intro

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aritxonly.deadliner.intro.IntroWizardViewModel
import com.aritxonly.deadliner.intro.WizardStep
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.iconResource


enum class HighlightTarget {
    None,
    Fab,
    MainCard,
    AiIcon
}

enum class InteractionTarget {
    None,
    ClickAddFab,
    SwipeTaskRight,
    SwipeTaskLeft,
    SwipeUpBottomNav,
    ClickAiIcon
}

data class WizardScreenConfig(
    val title: String,
    val description: String,
    val highlight: HighlightTarget,
    val interaction: InteractionTarget,
    val onInteractionSatisfied: () -> Unit
)

// -------- 顶层 Root：只管流程、样式切换 --------

@Composable
fun IntroWizardRoot(
    onWizardFinished: () -> Unit,
    onWizardSkipped: () -> Unit,
    vm: IntroWizardViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val step = state.currentStep
    val isSimplified = remember { GlobalUtils.style == "simplified" }

    LaunchedEffect(step) {
        if (step is WizardStep.Done) {
            onWizardFinished()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            WizardHeader(
                step = step,
                onSkip = onWizardSkipped,
                onNext = { vm.next() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isSimplified) {
                    SimplifiedWizardWithTransition(vm = vm)
                } else {
                    ClassicWizardWithTransition(vm = vm)
                }
            }
        }
    }
}


@Composable
internal fun InfoStepScreen(
    title: String,
    description: String,
    primaryButtonText: String,
    @DrawableRes screenshot: Int?,   // ← 新增
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (screenshot != null) {
                    Image(
                        painter = painterResource(id = screenshot),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "在这里放截图（TODO）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
        ) {
            Text(primaryButtonText)
        }
    }
}

@Composable
internal fun WizardHeader(
    step: WizardStep,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stepIndex = when (step) {
            WizardStep.AddEntry,
            WizardStep.AddEntryInfo -> 1

            WizardStep.SwipeRightComplete,
            WizardStep.SwipeLeftDelete -> 2

            WizardStep.AiEntry,
            WizardStep.AiInfo,
            WizardStep.Done -> 3
        }

        Text(
            text = stringResource(
                id = com.aritxonly.deadliner.R.string.wizard_header_title,
                stepIndex
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        TextButton(onClick = onSkip) {
            Text(text = stringResource(com.aritxonly.deadliner.R.string.wizard_header_skip))
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = iconResource(com.aritxonly.deadliner.R.drawable.ic_next),
                contentDescription = null
            )
        }
    }
}

@Composable
fun WizardHintPanel(
    title: String,
    description: String,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (extra != null) extra()
        }
    }
}

/**
 * 圆形高亮圈，作为「这里可以操作」的视觉提示。
 * 用法：包在一个 Box 外面，配合 matchParentSize() / 指定 size。
 */
@Composable
fun SpotlightCircle(
    modifier: Modifier = Modifier,
    align: Alignment = Alignment.Center
) {
    Box(
        modifier = modifier,
        contentAlignment = align
    ) {
        Box(
            modifier = Modifier
                .size(84.dp) // 需要更紧可以改小，比如 64.dp
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
        )
    }
}