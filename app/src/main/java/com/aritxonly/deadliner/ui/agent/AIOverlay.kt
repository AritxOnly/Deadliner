package com.aritxonly.deadliner.ui.agent

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.ai.AIUtils.generateDeadline
import com.aritxonly.deadliner.ai.IntentType
import com.aritxonly.deadliner.ai.UserProfile
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.iconResource
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.sin

@Composable
fun AIOverlayHost(
    initialText: String,
    onAddDDL: (Intent) -> Unit,
    onRemoveFromWindow: () -> Unit,
    respondIme: Boolean = true
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 8 },
        exit  = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 6 }
    ) {
        AIOverlay(
            initialText = initialText,
            onDismiss = { visibleState.targetState = false },  // 先触发退场
            onAddDDL = onAddDDL,
            respondIme = respondIme
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
fun AIOverlay(
    initialText: String = "",
    onDismiss: () -> Unit,
    onAddDDL: (Intent) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    borderThickness: Dp = 2.dp,
    glowColors: List<Color> = listOf(Color(0xFF6AA9FF), Color(0xFFFFC36A), Color(0xFFFF6AE6)),
    hintText: String = stringResource(R.string.ai_overlay_enter_questions),
    respondIme: Boolean = true
) {
    // UI 状态
    var textState by remember { mutableStateOf(TextFieldValue(initialText)) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var state by remember { mutableStateOf(ResultState()) }
    val clipboard = LocalClipboardManager.current

    val panelAlpha = remember { Animatable(0f) }
    val panelTranslate = remember { Animatable(40f) }    // px：初始稍微在下方
    val glowAlpha = remember { Animatable(0f) }
    val hintAlpha = remember { Animatable(0f) }

    val context = LocalContext.current

    fun launchAI() {
        val text = textState.text
        if (text.isBlank()) return
        focusManager.clearFocus()
        state = state.copy(isLoading = true, error = null, cards = emptyList())
        scope.launch {
            try {
                val (guess, json) = AIUtils.generateAuto(
                    context = context,
                    rawText = text,
                    profile = UserProfile(preferredLang = null, defaultEveningHour = 20, defaultReminderMinutes = listOf(30)),
                    preferLLM = true
                )
                val mixed = AIUtils.parseMixedResult(json)
                val (primary, cards) = mapMixedToUiCards(mixed)
                val defaultFilter = ResultFilter.All
                state = state.copy(intent = guess.intent, cards = cards, filter = defaultFilter, isLoading = false, error = null)
            } catch (t: Throwable) {
                state = state.copy(isLoading = false, error = t.message ?: "Unknown error")
            }
        }
    }

    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        panelAlpha.animateTo(1f, tween(320, delayMillis = 60, easing = FastOutSlowInEasing))
        panelTranslate.animateTo(0f, tween(420, delayMillis = 60, easing = FastOutSlowInEasing))
        hintAlpha.animateTo(1f, tween(240, delayMillis = 120, easing = LinearOutSlowInEasing))
        focusRequester.requestFocus()
    }

    BackHandler(enabled = true) { onDismiss() }

    val density = LocalDensity.current
    var parentHeightPx by remember { mutableIntStateOf(0) }
    var hintBottomPx by remember { mutableFloatStateOf(0f) }
    var toolbarTopPx by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }

    // 计算安全内边距（顶部/底部）
    val topSafePadding = with(density) {
        (hintBottomPx + 16.dp.toPx()).toDp()
    }
    val bottomSafePadding = with(density) {
        val padPx = (parentHeightPx.toFloat() - toolbarTopPx) + 16.dp.toPx()
        max(0f, padPx).toDp()
    }

    Box(modifier = modifier
        .fillMaxSize()
        .then(
            if (respondIme)
                Modifier.imePadding()
            else
                Modifier
        )
        .onGloballyPositioned { parentHeightPx = it.size.height }
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
                .onGloballyPositioned { hintBottomPx = it.boundsInParent().bottom }
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_overlay_hint_top),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val toolbarShape = RoundedCornerShape(percent = 50)

        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .graphicsLayer {
                    translationY = panelTranslate.value
                }
                .onGloballyPositioned { toolbarTopPx = it.boundsInParent().top }
                .glowingWobbleBorder(
                    shape = toolbarShape,
                    colors = glowColors,
                    stroke = borderThickness,
                    wobblePx = wobblePx,
                    breatheAmp = 0.10f
                ),
            trailingContent = {
                IconButton(onClick = {
                    focusManager.clearFocus()
                    if (textState.text.isNotBlank()) {
                        launchAI()
                    }
                }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_send),
                        contentDescription = stringResource(R.string.send),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.dp

            val buttonWidth = 48.dp
            val horizontalPadding = 32.dp

            val textFieldWidth = screenWidthDp - buttonWidth - horizontalPadding * 2

            BasicTextField(
                value = textState,
                onValueChange = { textState = it },
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                modifier = Modifier
                    .width(textFieldWidth)
                    .padding(start = 8.dp)
                    .heightIn(max = 48.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        focusManager.clearFocus()
                        if (textState.text.isNotBlank()) {
                            launchAI()
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

        // 加载指示条
        if (state.isLoading) {
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

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = topSafePadding,
                    bottom = bottomSafePadding
                )
        ) {
            // 顶部过滤（仅在有内容/加载/错误时显示）
            if (state.cards.isNotEmpty() || state.error != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MyFilterChip("全部", state.filter == ResultFilter.All) { state = state.copy(filter = ResultFilter.All) }
                    MyFilterChip("任务", state.filter == ResultFilter.Tasks) { state = state.copy(filter = ResultFilter.Tasks) }
                    MyFilterChip("日程", state.filter == ResultFilter.Plan) { state = state.copy(filter = ResultFilter.Plan) }
                    MyFilterChip("步骤", state.filter == ResultFilter.Steps) { state = state.copy(filter = ResultFilter.Steps) }
                }
                Spacer(Modifier.height(8.dp))
            }

            when {
                state.isLoading -> null
                state.error != null -> ErrorBlock(state.error!!)
                state.cards.isEmpty() -> EmptyHint()
                else -> {
                    val filtered = when (state.filter) {
                        ResultFilter.All   -> state.cards
                        ResultFilter.Tasks -> state.cards.filterIsInstance<UiCard.TaskCard>()
                        ResultFilter.Plan  -> state.cards.filterIsInstance<UiCard.PlanBlockCard>()
                        ResultFilter.Steps -> state.cards.filterIsInstance<UiCard.StepsCard>()
                    }

                    Column {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .edgeFade(top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            items(filtered) { card ->
                                when (card) {
                                    is UiCard.TaskCard -> TaskCardView(
                                        card = card,
                                        onAdd = {
                                            card.toGeneratedDDLOrNull()?.let { ddl ->
                                                val intent = Intent(
                                                    context,
                                                    AddDDLActivity::class.java
                                                ).apply {
                                                    putExtra("EXTRA_CURRENT_TYPE", 0)
                                                    putExtra("EXTRA_GENERATE_DDL", ddl)
                                                }
                                                onAddDDL(intent)
                                            }
                                        },
                                        onCopy = {}
                                    )

                                    is UiCard.PlanBlockCard -> PlanBlockCardView(card) {
                                        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                        val start = runCatching {
                                            LocalDateTime.parse(
                                                card.start,
                                                fmt
                                            )
                                        }.getOrNull()
                                        val end = runCatching {
                                            LocalDateTime.parse(
                                                card.end,
                                                fmt
                                            )
                                        }.getOrNull()
                                        val startMillis =
                                            start?.atZone(ZoneId.systemDefault())?.toInstant()
                                                ?.toEpochMilli()
                                        val endMillis =
                                            end?.atZone(ZoneId.systemDefault())?.toInstant()
                                                ?.toEpochMilli()

                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, card.title)
                                            if (!card.location.isNullOrBlank())
                                                putExtra(
                                                    CalendarContract.Events.EVENT_LOCATION,
                                                    card.location
                                                )
                                            if (!card.linkTask.isNullOrBlank())
                                                putExtra(
                                                    CalendarContract.Events.DESCRIPTION,
                                                    "关联任务: ${card.linkTask}"
                                                )
                                            if (startMillis != null) putExtra(
                                                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                                startMillis
                                            )
                                            if (endMillis != null) putExtra(
                                                CalendarContract.EXTRA_EVENT_END_TIME,
                                                endMillis
                                            )
                                        }
                                        context.startActivity(intent)
                                    }

                                    is UiCard.StepsCard -> StepsCardView(
                                        card = card,
                                        onCreateSubtasks = { title, checklist ->
                                            clipboard.setText(
                                                AnnotatedString(
                                                    (listOf(title) + checklist.mapIndexed { i, s -> "${i + 1}. $s" }).joinToString(
                                                        "\n"
                                                    )
                                                )
                                            )
                                        }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // 批量/应用 更改按钮
                        val planBlocks = filtered.filterIsInstance<UiCard.PlanBlockCard>()
                        val showFooter = when (state.filter) {
                            ResultFilter.All  -> true
                            ResultFilter.Plan -> planBlocks.isNotEmpty()
                            else              -> false
                        }

                        if (showFooter) {
                            Spacer(Modifier.height(8.dp))
                            val isPlan = state.filter == ResultFilter.Plan
                            Button(
                                onClick = {
                                    if (isPlan) {
                                        val first = planBlocks.firstOrNull() ?: return@Button
                                        val fmt =
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                        val start = runCatching {
                                            LocalDateTime.parse(
                                                first.start,
                                                fmt
                                            )
                                        }.getOrNull()
                                        val end = runCatching {
                                            LocalDateTime.parse(
                                                first.end,
                                                fmt
                                            )
                                        }.getOrNull()
                                        val startMillis =
                                            start?.atZone(ZoneId.systemDefault())?.toInstant()
                                                ?.toEpochMilli()
                                        val endMillis =
                                            end?.atZone(ZoneId.systemDefault())?.toInstant()
                                                ?.toEpochMilli()

                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, first.title)
                                            if (!first.location.isNullOrBlank())
                                                putExtra(
                                                    CalendarContract.Events.EVENT_LOCATION,
                                                    first.location
                                                )
                                            if (!first.linkTask.isNullOrBlank())
                                                putExtra(
                                                    CalendarContract.Events.DESCRIPTION,
                                                    "关联任务: ${first.linkTask}"
                                                )
                                            if (startMillis != null) putExtra(
                                                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                                startMillis
                                            )
                                            if (endMillis != null) putExtra(
                                                CalendarContract.EXTRA_EVENT_END_TIME,
                                                endMillis
                                            )
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        scope.launch {
                                            val repo = DDLRepository()
                                            val taskCards =
                                                filtered.filterIsInstance<UiCard.TaskCard>()

                                            taskCards.forEach { t ->
                                                val fmt =
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                                val end = runCatching {
                                                    LocalDateTime.parse(
                                                        t.due,
                                                        fmt
                                                    )
                                                }.getOrElse { LocalDateTime.now() }

                                                repo.insertDDL(
                                                    name = t.title,
                                                    startTime = LocalDateTime.now().toString(),
                                                    endTime = end.toString(),
                                                    note = t.note.orEmpty(),
                                                    type = DeadlineType.TASK,
                                                    calendarEventId = null
                                                )
                                            }

                                            // 2) 汇总 Steps 为 Markdown 并复制到剪贴板
                                            val stepsCards =
                                                filtered.filterIsInstance<UiCard.StepsCard>()
                                            val md = buildString {
                                                stepsCards.forEach { sc ->
                                                    append("## ").append(sc.title).append('\n')
                                                    sc.checklist.forEach { item ->
                                                        append("- ").append(item).append('\n')
                                                    }
                                                    append('\n')
                                                }
                                            }.trim()
                                            clipboard.setText(AnnotatedString(md))

                                            val first = planBlocks.firstOrNull()
                                            if (first != null) {
                                                val fmt =
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                                val start = runCatching {
                                                    LocalDateTime.parse(
                                                        first.start,
                                                        fmt
                                                    )
                                                }.getOrNull()
                                                val end = runCatching {
                                                    LocalDateTime.parse(
                                                        first.end,
                                                        fmt
                                                    )
                                                }.getOrNull()
                                                val startMillis =
                                                    start?.atZone(ZoneId.systemDefault())
                                                        ?.toInstant()?.toEpochMilli()
                                                val endMillis =
                                                    end?.atZone(ZoneId.systemDefault())
                                                        ?.toInstant()?.toEpochMilli()

                                                val intent =
                                                    Intent(Intent.ACTION_INSERT).apply {
                                                        data =
                                                            CalendarContract.Events.CONTENT_URI
                                                        putExtra(
                                                            CalendarContract.Events.TITLE,
                                                            first.title
                                                        )
                                                        if (!first.location.isNullOrBlank())
                                                            putExtra(
                                                                CalendarContract.Events.EVENT_LOCATION,
                                                                first.location
                                                            )
                                                        if (!first.linkTask.isNullOrBlank())
                                                            putExtra(
                                                                CalendarContract.Events.DESCRIPTION,
                                                                "关联任务: ${first.linkTask}"
                                                            )
                                                        if (startMillis != null) putExtra(
                                                            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                                            startMillis
                                                        )
                                                        if (endMillis != null) putExtra(
                                                            CalendarContract.EXTRA_EVENT_END_TIME,
                                                            endMillis
                                                        )
                                                    }
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(if (isPlan) "导入日程块" else "应用全部更改")
                            }
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
        return sin(angle)
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
                val radiusScale = lerp(0.82f, 1.00f, widthNorm)      // 窄屏把半径降到 82%
                val alphaScale = lerp(0.85f, 1.00f, widthNorm)      // 窄屏整体稍微降亮度
                val jitterScale = lerp(0.70f, 1.00f, widthNorm)      // 窄屏减小抖动幅度

                // 把你的 jPx 做个缩放，避免窄屏晃动导致重叠更严重
                val j = jPx * jitterScale

                // —— 计算动态中心：窄屏时增加左右分离度 ——
                val blueCenter = Offset(
                    w * (0.25f - separationBoost) + if (jitterEnabled) j * 0.9f * s(
                        freqBlue,
                        0.13f
                    ) else 0f,
                    h * 0.80f + if (jitterEnabled) j * 0.5f * s(freqBlue * 1.3f, 0.37f) else 0f
                )
                val pinkCenter = Offset(
                    w * (0.78f + separationBoost) + if (jitterEnabled) j * 0.7f * s(
                        freqPink,
                        0.51f
                    ) else 0f,
                    h * 0.72f + if (jitterEnabled) j * 0.6f * s(freqPink * 1.4f, 0.11f) else 0f
                )
                val amberCenter = Offset(
                    w * (0.55f) + if (jitterEnabled) j * 0.8f * s(freqAmber, 0.29f) else 0f,
                    h * 0.95f + if (jitterEnabled) j * 0.4f * s(freqAmber * 0.8f, 0.73f) else 0f
                )

                // —— 半径按窄屏缩小：半径仍以高度为基准，但乘以 radiusScale ——
                val blueRadius = h * 1.10f * radiusScale * (1f + if (jitterEnabled) 0.015f * s(
                    freqBlue * 1.1f,
                    0.2f
                ) else 0f)
                val pinkRadius = h * 1.00f * radiusScale * (1f + if (jitterEnabled) 0.018f * s(
                    freqPink * 0.95f,
                    0.4f
                ) else 0f)
                val amberRadius = h * 1.30f * radiusScale * (1f + if (jitterEnabled) 0.012f * s(
                    freqAmber * 1.05f,
                    0.6f
                ) else 0f)

                val breathe = 0.90f + 0.10f * (if (jitterEnabled) (s(
                    freqBreathe,
                    0.18f
                ) * 0.5f + 0.5f) else 1f)

                // —— 颜色强度按窄屏轻降，避免 Plus 混得太狠 ——
                val blue = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6AA9FF).copy(alpha = 0.85f * alphaScale * a * breathe),
                        Color.Transparent
                    ),
                    center = blueCenter,
                    radius = blueRadius
                )
                val pink = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF6AE6).copy(alpha = 0.80f * alphaScale * a * breathe),
                        Color.Transparent
                    ),
                    center = pinkCenter,
                    radius = pinkRadius
                )
                val amber = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFC36A).copy(alpha = 0.80f * alphaScale * a * breathe),
                        Color.Transparent
                    ),
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
                    drawRect(blue, blendMode = BlendMode.Plus)
                    drawRect(pink, blendMode = BlendMode.Plus)
                    drawRect(whiteFog)
                    drawRect(vertical)
                }
            }
    )
}

fun Modifier.glowingWobbleBorder(
    shape: Shape,
    colors: List<Color>,
    stroke: Dp,
    wobblePx: Dp = 4.dp,
    freqHz: Float = 0.20f,
    breatheAmp: Float = 0.12f,
    breatheHz: Float = 0.10f
): Modifier = composed {
    val density = LocalDensity.current
    val timeSec by rememberTimeSeconds()

    val wobble = with(density) { wobblePx.toPx() } *
            sin(2f * Math.PI.toFloat() * (timeSec * freqHz))
    val breathe = 1f + breatheAmp *
            sin(2f * Math.PI.toFloat() * (timeSec * breatheHz + 0.17f))

    this.then(
        Modifier.drawWithCache {
            val strokePx = stroke.toPx()

            val brush = Brush.linearGradient(
                colors = colors.map { it.copy(alpha = (it.alpha * breathe).coerceIn(0f, 1f)) },
                start = Offset(-wobble, 0f),
                end = Offset(size.width + wobble, 0f)
            )

            onDrawWithContent {
                drawContent()

                inset(strokePx / 2f) {
                    val outline = shape.createOutline(
                        size = size,
                        layoutDirection = this.layoutDirection,
                        density = this
                    )
                    when (outline) {
                        is Outline.Rounded -> {
                            val rr = outline.roundRect
                            val path = Path().apply { addRoundRect(rr) }
                            drawPath(path = path, brush = brush, style = Stroke(width = strokePx))
                        }
                        is Outline.Rectangle -> {
                            drawRect(brush = brush, style = Stroke(width = strokePx))
                        }
                        is Outline.Generic -> {
                            drawPath(path = outline.path, brush = brush, style = Stroke(width = strokePx))
                        }
                    }
                }
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

@Composable
private fun MyFilterChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) { { Icon(iconResource(R.drawable.ic_finish), contentDescription = null) } } else null,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors()
    )
}

@Stable
fun Modifier.edgeFade(top: Dp = 16.dp, bottom: Dp = 16.dp) = this.then(
    drawWithContent {
        drawContent()

        val h = size.height.coerceAtLeast(1f)
        val topPx = top.toPx().coerceAtMost(h / 2f)
        val bottomPx = bottom.toPx().coerceAtMost(h / 2f)

        val c0 = Color.Black.copy(alpha = 0f) // 完全裁掉
        val c1 = Color.Black.copy(alpha = 1f) // 完全保留

        val sTop = (topPx / h).coerceIn(0f, 0.33f)
        val sBot = (1f - bottomPx / h).coerceIn(0.67f, 1f)

        val brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f   to c0,
                sTop to c1,
                sBot to c1,
                1f   to c0
            ),
            startY = 0f,
            endY = h
        )

        // 用目标(alpha) ∧ 源(alpha) 的交集作为显示区域
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
)