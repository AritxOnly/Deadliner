package com.aritxonly.deadliner.composable.agent

import android.content.Intent
import com.aritxonly.deadliner.R

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.model.GeneratedDDL
import com.aritxonly.deadliner.web.DeepSeekUtils.generateDeadline
import com.aritxonly.deadliner.web.DeepSeekUtils.parseGeneratedDDL
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DeepseekOverlayHost(
    initialText: String,
    onAddDDL: (Intent) -> Unit,
    onRemoveFromWindow: () -> Unit   // 退场动画播放完后回调
) {
    // 初始不可见 -> 目标可见：触发入场
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 8 },
        exit  = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 6 }
    ) {
        DeepseekOverlay(
            initialText = initialText,
            onDismiss = { visibleState.targetState = false },  // 先触发退场
            onAddDDL = onAddDDL
        )
    }

    // 退场动画结束后，通知外层移除 ComposeView
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            onRemoveFromWindow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DeepseekOverlay(
    initialText: String = "",
    onDismiss: () -> Unit,
    onAddDDL: (Intent) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    borderThickness: Dp = 4.dp,
    glowColors: List<Color> = listOf(Color(0xFF6AA9FF), Color(0xFFFFC36A), Color(0xFFFF6AE6)),
    hintText: String = stringResource(R.string.deepseek_overlay_enter_questions)
) {
    // UI 状态
    var textState by remember { mutableStateOf(TextFieldValue(initialText)) }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GeneratedDDL>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val panelAlpha = remember { Animatable(0f) }
    val panelTranslate = remember { Animatable(40f) }    // px：初始稍微在下方
    val glowAlpha = remember { Animatable(0f) }
    val hintAlpha = remember { Animatable(0f) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        panelAlpha.animateTo(1f, tween(320, delayMillis = 60, easing = FastOutSlowInEasing))
        panelTranslate.animateTo(0f, tween(420, delayMillis = 60, easing = FastOutSlowInEasing))
        hintAlpha.animateTo(1f, tween(240, delayMillis = 120, easing = LinearOutSlowInEasing))
        focusRequester.requestFocus()
    }

    BackHandler(enabled = true) { onDismiss() }

    Box(modifier = modifier
        .fillMaxSize()
        .imePadding()
        .clickable {
            focusManager.clearFocus()
            onDismiss()
        }
    ) {
        val wobblePx = rememberScreenScaledWobbleDp(fractionOfMinSide = 0.2f)

        GlowScrim(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            height = 260.dp,
            blur = 60.dp,
            opacity = glowAlpha.value,
            jitterEnabled = true,
            jitterRadius = wobblePx,
            freqBlue = 0.20f,   // 5s 周期
            freqPink = 0.18f,   // ~5.5s 周期
            freqAmber = 0.15f,  // ~6.7s 周期
            freqBreathe = 0.125f // 8s 周期
        )

        // 顶部提示气泡
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .graphicsLayer { alpha = hintAlpha.value }
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.deepseek_overlay_hint_top),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val itemCorner = dimensionResource(R.dimen.item_corner_radius)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .graphicsLayer {
//                    alpha = panelAlpha.value
                    translationY = panelTranslate.value
                }
                .glowingWobbleBorder(
                    colors = glowColors,
                    corner = itemCorner,
                    stroke = borderThickness,
                    wobblePx = wobblePx,
                    breatheAmp = 0.10f
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
                )
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            ) {
                val lineHeightDp = 24.dp
                val maxHeight = lineHeightDp * 2

                val scrollState = rememberScrollState()
                val parseFailedText = stringResource(R.string.parse_failed)
                val unknownErrorText = stringResource(R.string.unknown_error)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = maxHeight)
                        .verticalScroll(scrollState)
                        .bringIntoViewRequester(bringIntoViewRequester)
                ) {

                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                focusManager.clearFocus()
                                if (textState.text.isNotBlank()) {
                                    results = emptyList()
                                    failed = false
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val raw = generateDeadline(context, textState.text)
                                            val ddl = parseGeneratedDDL(raw)
                                            results = listOf(ddl)
                                        } catch (e: Exception) {
                                            results = listOf(
                                                GeneratedDDL(
                                                    name = parseFailedText,
                                                    dueTime = LocalDateTime.now(),
                                                    note = e.message ?: unknownErrorText
                                                )
                                            )
                                            failed = true
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            if (textState.text.isEmpty()) {
                                Text(
                                    text = hintText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                IconButton(onClick = {
                    focusManager.clearFocus()
                    if (textState.text.isNotBlank()) {
                        results = emptyList()
                        failed = false
                        scope.launch {
                            isLoading = true
                            try {
                                val raw = generateDeadline(context, textState.text)
                                val ddl = parseGeneratedDDL(raw)
                                results = listOf(ddl)
                            } catch (e: Exception) {
                                results = listOf(
                                    GeneratedDDL(
                                        name = parseFailedText,
                                        dueTime = LocalDateTime.now(),
                                        note = e.message ?: unknownErrorText
                                    )
                                )
                                failed = true
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_send),
                        contentDescription = stringResource(R.string.send),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 加载指示条
        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 结果展示
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            results.forEach { ddl ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val view = LayoutInflater.from(ctx)
                                .inflate(R.layout.item_layout, null, false)
                            view.findViewById<TextView>(R.id.titleText)
                                .text = ddl.name
                            view.findViewById<TextView>(R.id.remainingTimeTextAlt)
                                .text =
                                ddl.dueTime.format(DateTimeFormatter
                                        .ofLocalizedDateTime(FormatStyle.MEDIUM)
                                        .withLocale(Locale.getDefault()))
                            view.findViewById<TextView>(R.id.noteText)
                                .text = ddl.note
                            view.findViewById<ImageView>(R.id.starIcon)
                                .visibility = View.GONE
                            view.findViewById<LinearProgressIndicator>(R.id.progressBar)
                                .visibility = View.GONE
                            view.findViewById<TextView>(R.id.remainingTimeText)
                                .visibility = View.GONE
                            view
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    if (!failed) {
                        Button(
                            onClick = {
                                val intent = Intent(context, AddDDLActivity::class.java).apply {
                                    putExtra("EXTRA_CURRENT_TYPE", 0)
                                    putExtra("EXTRA_GENERATE_DDL", ddl)
                                }
                                onAddDDL(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)  // 圆角按钮
                        ) {
                            Text(text = stringResource(R.string.add_event), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun rememberScreenScaledWobbleDp(
    fractionOfMinSide: Float = 0.012f, // 1.2% 的最短边
    minDp: Dp = 16.dp,                  // 下限，避免太小看不见
    maxDp: Dp = 48.dp                  // 上限，避免太大夸张
): Dp {
    val cfg = LocalConfiguration.current
    val base = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val raw = (base * fractionOfMinSide).dp
    return raw.coerceIn(minDp, maxDp)
}

@Composable
fun GlowScrim(
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    blur: Dp = 60.dp,
    opacity: Float = 1f,
    jitterEnabled: Boolean = true,
    jitterRadius: Dp = 6.dp,    // 可放到 64dp
    freqBlue: Float = 1.00f,    // Hz
    freqPink: Float = 0.95f,
    freqAmber: Float = 1.10f,
    freqBreathe: Float = 0.35f
) {
    val a = opacity.coerceIn(0f, 1f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val jPx = with(density) { jitterRadius.toPx() }

    // —— 连续时间（秒） ——
    val timeSec by rememberTimeSeconds()

    // 工具：连续正弦
    fun s(freqHz: Float, phase: Float = 0f): Float {
        val angle = (2f * Math.PI.toFloat()) * (timeSec * freqHz) + phase
        return kotlin.math.sin(angle)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .blur(blur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .drawWithCache {
                val w = size.width
                val h = size.height

                val refW = with(density) { 840.dp.toPx() }

                // 0..1，屏幕越宽 -> 越接近 1；越窄 -> 越接近 0
                val widthNorm = (w / refW).coerceIn(0f, 1f)

                // 根据宽度计算自适应系数（可微调这些数）
                val separationBoost = lerp(0.0f, 0.8f, 1f - widthNorm)  // 窄屏把中心再往左右推 ~9%
                val radiusScale     = lerp(0.82f, 1.00f, widthNorm)      // 窄屏把半径降到 82%
                val alphaScale      = lerp(0.85f, 1.00f, widthNorm)      // 窄屏整体稍微降亮度
                val jitterScale     = lerp(0.70f, 1.00f, widthNorm)      // 窄屏减小抖动幅度

                // 把你的 jPx 做个缩放，避免窄屏晃动导致重叠更严重
                val j = jPx * jitterScale

                // —— 计算动态中心：窄屏时增加左右分离度 ——
                val blueCenter = Offset(
                    w * (0.25f - separationBoost) + if (jitterEnabled) j * 0.9f * s(freqBlue, 0.13f) else 0f,
                    h * 0.80f + if (jitterEnabled) j * 0.5f * s(freqBlue * 1.3f, 0.37f) else 0f
                )
                val pinkCenter = Offset(
                    w * (0.78f + separationBoost) + if (jitterEnabled) j * 0.7f * s(freqPink, 0.51f) else 0f,
                    h * 0.72f + if (jitterEnabled) j * 0.6f * s(freqPink * 1.4f, 0.11f) else 0f
                )
                val amberCenter = Offset(
                    w * (0.55f) + if (jitterEnabled) j * 0.8f * s(freqAmber, 0.29f) else 0f,
                    h * 0.95f + if (jitterEnabled) j * 0.4f * s(freqAmber * 0.8f, 0.73f) else 0f
                )

                // —— 半径按窄屏缩小：半径仍以高度为基准，但乘以 radiusScale ——
                val blueRadius  = h * 1.10f * radiusScale * (1f + if (jitterEnabled) 0.015f * s(freqBlue * 1.1f, 0.2f) else 0f)
                val pinkRadius  = h * 1.00f * radiusScale * (1f + if (jitterEnabled) 0.018f * s(freqPink * 0.95f, 0.4f) else 0f)
                val amberRadius = h * 1.30f * radiusScale * (1f + if (jitterEnabled) 0.012f * s(freqAmber * 1.05f, 0.6f) else 0f)

                val breathe = 0.90f + 0.10f * (if (jitterEnabled) (s(freqBreathe, 0.18f) * 0.5f + 0.5f) else 1f)

                // —— 颜色强度按窄屏轻降，避免 Plus 混得太狠 ——
                val blue = Brush.radialGradient(
                    colors = listOf(Color(0xFF6AA9FF).copy(alpha = 0.85f * alphaScale * a * breathe), Color.Transparent),
                    center = blueCenter,
                    radius = blueRadius
                )
                val pink = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF6AE6).copy(alpha = 0.80f * alphaScale * a * breathe), Color.Transparent),
                    center = pinkCenter,
                    radius = pinkRadius
                )
                val amber = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFC36A).copy(alpha = 0.80f * alphaScale * a * breathe), Color.Transparent),
                    center = amberCenter,
                    radius = amberRadius
                )

                // 白雾与底部压暗保持不变（也可以按需加一点点 scale）
                val whiteFog = Brush.radialGradient(
                    colors = listOf(surfaceColor.copy(alpha = 0.55f * a), Color.Transparent),
                    center = Offset(w / 2f, h * 1.12f),
                    radius = h * 1.25f
                )
                val vertical = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.60f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.35f * a)
                )

                onDrawBehind {
                    drawRect(amber, blendMode = BlendMode.Plus)
                    drawRect(blue,  blendMode = BlendMode.Plus)
                    drawRect(pink,  blendMode = BlendMode.Plus)
                    drawRect(whiteFog)
                    drawRect(vertical)
                }
            }
    )
}

fun Modifier.glowingWobbleBorder(
    colors: List<Color>,
    corner: Dp,
    stroke: Dp,
    wobblePx: Dp = 4.dp,
    freqHz: Float = 0.20f,      // 左右轻微晃动的频率（Hz）
    breatheAmp: Float = 0.12f,  // alpha 呼吸幅度
    breatheHz: Float = 0.10f
): Modifier = composed {
    val density = LocalDensity.current
    val timeSec by rememberTimeSeconds()

    val wobble = with(density) { wobblePx.toPx() } *
            kotlin.math.sin(2f * Math.PI.toFloat() * (timeSec * freqHz))

    val breathe = 1f + breatheAmp *
            kotlin.math.sin(2f * Math.PI.toFloat() * (timeSec * breatheHz + 0.17f))

    this.then(
        Modifier.drawWithCache {
            val strokePx = stroke.toPx()
            val r = corner.toPx()

            val brush = Brush.linearGradient(
                colors = colors.map { it.copy(alpha = (it.alpha * breathe).coerceIn(0f, 1f)) },
                start = Offset(-wobble, 0f),
                end   = Offset(size.width + wobble, 0f)
            )

            onDrawBehind {
                drawRoundRect(
                    brush = brush,
                    size = size,
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = strokePx)
                )
            }
        }
    )
}

@Composable
fun rememberTimeSeconds(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    time.floatValue += dt
                }
                last = now
            }
        }
    }
    return time
}