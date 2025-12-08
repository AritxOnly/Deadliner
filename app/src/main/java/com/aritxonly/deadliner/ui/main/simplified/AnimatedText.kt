@file:OptIn(ExperimentalAnimationApi::class)

package com.aritxonly.deadliner.ui.main.simplified

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import com.aritxonly.deadliner.R

private enum class GreetingSegment { Morning, Noon, Afternoon, Evening, Midnight, Dawn, Default }

@Composable
fun RotatingGreeting(name: String, fadeMillis: Int = 250) {
    val context = LocalContext.current

    fun segmentForHour(hour: Int) = when (hour) {
        in 5..10 -> GreetingSegment.Morning
        in 11..13 -> GreetingSegment.Noon
        in 14..17 -> GreetingSegment.Afternoon
        in 18..22 -> GreetingSegment.Evening
        23, 0, 1 -> GreetingSegment.Midnight
        in 2..4 -> GreetingSegment.Dawn
        else -> GreetingSegment.Default
    }

    fun resIdFor(seg: GreetingSegment) = when (seg) {
        GreetingSegment.Morning   -> R.array.panel_greeting_morning
        GreetingSegment.Noon      -> R.array.panel_greeting_noon
        GreetingSegment.Afternoon -> R.array.panel_greeting_afternoon
        GreetingSegment.Evening   -> R.array.panel_greeting_evening
        GreetingSegment.Midnight  -> R.array.panel_greeting_midnight
        GreetingSegment.Dawn      -> R.array.panel_greeting_dawn
        GreetingSegment.Default   -> R.array.panel_greeting_default
    }

    @SuppressLint("LocalContextResourcesRead")
    fun pickRandom(seg: GreetingSegment): String {
        val arr = context.resources.getStringArray(resIdFor(seg))
        return if (arr.isNotEmpty()) arr.random() else "%s"
    }

    var segment by remember { mutableStateOf(segmentForHour(LocalTime.now().hour)) }
    var currentText by remember { mutableStateOf(pickRandom(segment)) } // 首次展开就随机

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val msToNextMinute = 60_000 - (now % 60_000)
            delay(msToNextMinute)

            val newHour = LocalTime.now().hour
            val newSeg = segmentForHour(newHour)
            segment = newSeg
            currentText = pickRandom(newSeg)
        }
    }

    AnimatedContent(
        targetState = currentText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(fadeMillis))) with
                    (fadeOut(animationSpec = tween(fadeMillis)))
        },
        label = "greeting-anim"
    ) { text ->
        Text(text = String.format(text, name))
    }
}

@Composable
fun AnimatedHintPlaceholder(
    expanded: Boolean,
    isEnabled: Boolean,
    excitement: List<String>,
    idx: Int,
    fadeMillis: Int = 250
) {
    val defaultHint = stringResource(R.string.search_hint)

    // 目标 hint
    val targetHint = when {
        expanded -> defaultHint
        isEnabled && excitement.isNotEmpty() -> excitement[idx % excitement.size]
        else -> defaultHint
    }

    AnimatedContent(
        targetState = targetHint,
        transitionSpec = {
            (fadeIn(animationSpec = tween(fadeMillis))) with
                    (fadeOut(animationSpec = tween(fadeMillis))) using
                    SizeTransform(clip = false)
        },
        label = "search-placeholder-anim"
    ) { hint ->
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedProgressText(
    progress: Float,
    fadeMillis: Int = 250
) {
    // 用整型百分比做 targetState，可以避免浮点抖动导致频繁重组
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()

    AnimatedContent(
        targetState = percent,
        transitionSpec = {
            fadeIn(animationSpec = tween(fadeMillis)) with
                    fadeOut(animationSpec = tween(fadeMillis))
        },
        label = "progress-text-anim"
    ) { p ->
        Text(
            text = "$p%",
            style = MaterialTheme.typography.titleMediumEmphasized
        )
    }
}