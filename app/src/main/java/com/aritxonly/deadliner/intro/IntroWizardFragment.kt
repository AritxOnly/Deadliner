package com.aritxonly.deadliner.intro

import android.animation.ValueAnimator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.airbnb.lottie.LottieAnimationView
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import kotlinx.coroutines.launch

class IntroWizardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DeadlinerTheme {
                    IntroWizardScreen(
                        onWizardFinished = {
                            setFragmentResult("wizardFinished", bundleOf())
                        },
                        onWizardSkipped = {
                            setFragmentResult("wizardSkipped", bundleOf())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroWizardScreen(
    onWizardFinished: () -> Unit,
    onWizardSkipped: () -> Unit
) {
    val style = remember { UiStyle.fromKey(GlobalUtils.style) }
    val scenes = remember(style) { IntroGuideScenes.forStyle(style) }
    val pagerState = rememberPagerState(pageCount = { scenes.size })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(scenes.isEmpty()) {
        if (scenes.isEmpty()) {
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.intro_guide_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onWizardSkipped) {
                    Text(text = stringResource(R.string.skip))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                IntroGuidePage(
                    scene = scenes[page],
                    page = page + 1,
                    pageCount = scenes.size
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GuidePagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = scenes.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (pagerState.currentPage >= scenes.lastIndex) {
                        onWizardFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (pagerState.currentPage >= scenes.lastIndex) {
                        stringResource(R.string.finish_wizard)
                    } else {
                        stringResource(R.string.intro_guide_next)
                    }
                )
            }
        }
    }
}

@Composable
private fun IntroGuidePage(
    scene: IntroGuideSceneConfig,
    page: Int,
    pageCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                IntroGuideLottie(scene = scene)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.intro_guide_page_indicator, page, pageCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = scene.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = scene.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = scene.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IntroGuideLottie(scene: IntroGuideSceneConfig) {
    val animationRes = remember(scene.fileName) {
        IntroGuideScenes.resolveRawRes(scene.fileName)
    }

    AndroidView(
        factory = { context ->
            LottieAnimationView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                repeatCount = ValueAnimator.INFINITE
                setAnimation(animationRes)
                tag = animationRes
                playAnimation()
            }
        },
        update = { view ->
            if (view.tag != animationRes) {
                view.setAnimation(animationRes)
                view.tag = animationRes
            }
            if (!view.isAnimating) {
                view.playAnimation()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GuidePagerIndicator(
    currentPage: Int,
    pageCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .height(8.dp)
                    .width(if (selected) 24.dp else 8.dp)
            )
        }
    }
}

// -------- Legacy State & VM --------

data class IntroWizardState(
    val currentStep: WizardStep = WizardStep.AddEntry
)

sealed class WizardStep {
    data object AddEntry : WizardStep()
    data object AddEntryInfo : WizardStep()
    data object SwipeRightComplete : WizardStep()
    data object SwipeLeftDelete : WizardStep()
    data object AiEntry : WizardStep()
    data object AiInfo : WizardStep()
    data object Done : WizardStep()
}
