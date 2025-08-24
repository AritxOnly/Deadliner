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
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        GlowScrim(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            height = 260.dp,
            blur = 60.dp,
            opacity = glowAlpha.value
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
                    alpha = panelAlpha.value
                    translationY = panelTranslate.value
                }
                .drawBehind {
                    val stroke = borderThickness.toPx()
                    // Load your dimen as Dp
                    val cornerDp = itemCorner
                    // Convert to pixels
                    val cornerPx = cornerDp.toPx()
                    drawRoundRect(
                        brush = Brush.horizontalGradient(glowColors),
                        size = size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = stroke)
                    )
                }
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
fun GlowScrim(
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    blur: Dp = 60.dp,
    opacity: Float = 1f
) {
    val a = opacity.coerceIn(0f, 1f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .blur(blur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .drawWithCache {
                // 把 a 乘到每一层的颜色透明度上（注意别把 Transparent 乘出来发灰）
                val blue = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6AA9FF).copy(alpha = 0.85f * a),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.80f),
                    radius = size.height * 1.10f
                )
                val pink = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF6AE6).copy(alpha = 0.80f * a),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.72f),
                    radius = size.height * 1.00f
                )
                val amber = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFC36A).copy(alpha = 0.80f * a),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.55f, size.height * 0.95f),
                    radius = size.height * 1.30f
                )
                val whiteFog = Brush.radialGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.55f * a),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 1.12f),
                    radius = size.height * 1.25f
                )
                val vertical = Brush.verticalGradient(
                    0f    to Color.Transparent,                         // 顶部仍完全透明
                    0.60f to Color.Transparent,
                    1f    to Color.Black.copy(alpha = 0.35f * a)       // 底部压暗也跟随 a
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