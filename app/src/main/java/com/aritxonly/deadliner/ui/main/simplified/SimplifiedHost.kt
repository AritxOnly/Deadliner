@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.aritxonly.deadliner.ui.main.simplified

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.aritxonly.deadliner.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.ViewModelFactory
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.agent.AIOverlayHost
import com.aritxonly.deadliner.ui.main.TextPageIndicator

@Composable
fun SimplifiedHost(
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val vm: MainViewModel = viewModel(factory = ViewModelFactory(context))

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadData(vm.currentType)  // Activity 恢复时强制刷新
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            vm.loadData(vm.currentType)
        }
    }

    LaunchedEffect(Unit) {
        GlobalUtils.decideHideFromRecent(context, activity)
        vm.loadData(vm.currentType)
    }

    val ddlList by vm.ddlListFlow.collectAsStateWithLifecycle()
    val dueSoonCounts by vm.dueSoonCounts.observeAsState(emptyMap())
    val refreshState by vm.refreshState.collectAsStateWithLifecycle()

    var toolbarExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        val closeThresholdPx = with(density) { 32.dp.toPx() }   // 收起需要更大距离
        val openThresholdPx  = with(density) { 16.dp.toPx() }   // 展开阈值更小(滞回)

        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        var accumDown = 0f   // 向下滚（内容上推）累计
        var accumUp   = 0f   // 向上滚（内容下拉）累计

        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val dyPx = (offset - lastOffset) + (index - lastIndex) * 10_000f
            lastIndex = index; lastOffset = offset

            // 顶部自动展开，且清零累计
            val atTop = index == 0 && offset == 0
            if (atTop) {
                if (!toolbarExpanded) toolbarExpanded = true
                accumDown = 0f; accumUp = 0f
                return@collect
            }

            if (dyPx > 0f) {
                // 向下滚：准备收起
                accumDown += dyPx
                accumUp = 0f
                if (toolbarExpanded && accumDown >= closeThresholdPx) {
                    toolbarExpanded = false
                    accumDown = 0f
                }
            } else if (dyPx < 0f) {
                // 向上滚：准备展开
                accumUp += -dyPx
                accumDown = 0f
                if (!toolbarExpanded && accumUp >= openThresholdPx) {
                    toolbarExpanded = true
                    accumUp = 0f
                }
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val nearTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset < with(density) { 24.dp.toPx() }
            if (nearTop) toolbarExpanded = true
        }
    }

    var selectedPage by remember { mutableStateOf(vm.currentType) }
    LaunchedEffect(selectedPage) {
        vm.loadData(selectedPage)
    }

    var showOverlay by remember { mutableStateOf(false) }
    var childRequestsBlur by remember { mutableStateOf(false) }
    val shouldBlur = showOverlay || childRequestsBlur

    val scale by animateFloatAsState(targetValue = if (showOverlay) 0.98f else 1f, label = "content-scale")
    val maxBlur = 24f
    val blurProgress by animateFloatAsState(
        targetValue = if (shouldBlur) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "blur-progress"
    )
    val blurRadius = (maxBlur * blurProgress)
        .coerceIn(0f, maxBlur)
    val saturation = lerp(1f, 0.5f, blurProgress)
    val EPS = 0.5f

    val jumpAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        jumpAnim.snapTo(40f)
        jumpAnim.animateTo(
            targetValue = 32f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessLow
            )
        )
        jumpAnim.animateTo(
            targetValue = 40f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .graphicsLayer {
                    val effects = mutableListOf<android.graphics.RenderEffect>()

                    if (blurRadius >= EPS) {
                        effects += android.graphics.RenderEffect.createBlurEffect(
                            blurRadius, blurRadius,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    // 如果你希望在无模糊时也保持去饱和，就保留下面这段；
                    // 若不需要，改成：if (blurRadius >= EPS) { ...createColorFilter... }
                    if (saturation < 1f - 1e-3f) {
                        val cm = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
                        effects += android.graphics.RenderEffect.createColorFilterEffect(
                            android.graphics.ColorMatrixColorFilter(cm)
                        )
                    }

                    renderEffect = when (effects.size) {
                        0 -> null
                        1 -> effects[0].asComposeRenderEffect()
                        else -> android.graphics.RenderEffect.createChainEffect(effects[0], effects[1]).asComposeRenderEffect()
                    }

                    // 你已有的缩放动画
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            MainDisplay(
                ddlList = ddlList,
                dueSoonCounts = dueSoonCounts,
                refreshState = refreshState,
                selectedPage = selectedPage,
                onSearch = { query ->
                    vm.filterData(SearchFilter(query = query), vm.currentType)
                },
                activity = activity,
                modifier = Modifier
                    .fillMaxSize(),
                vm = vm,
                listState = listState,
                onRequestBackdropBlur = { enable -> childRequestsBlur = enable }
            )

            HorizontalFloatingToolbar(
                expanded = toolbarExpanded,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = jumpAnim.value.dp, start = 16.dp, end = 16.dp),
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                leadingContent = {
                    Box(modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                        TextPageIndicator(
                            text = stringResource(R.string.task),
                            onClick = { selectedPage = DeadlineType.TASK },
                            selected = selectedPage.toString(),
                            tag = DeadlineType.TASK.toString()
                        )
                    }
                },
                trailingContent = {
                    Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                        TextPageIndicator(
                            text = stringResource(R.string.habit),
                            onClick = { selectedPage = DeadlineType.HABIT },
                            selected = selectedPage.toString(),
                            tag = DeadlineType.HABIT.toString()
                        )
                    }
                }
            ) {

                FilledIconButton(
                    onClick = {
                        val intent = Intent(context, AddDDLActivity::class.java)
                        launcher.launch(intent)
                    },
                    modifier = Modifier
                        .width(56.dp)
                        .detectSwipeUp {
                            Log.d("SwipeUp", "Triggered")
                            showOverlay = true
                        }
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_add), "")
                }
            }
        }

        if (showOverlay) {
            AIOverlayHost(
                initialText = "",
                onAddDDL = {},
                onRemoveFromWindow = {
                    showOverlay = false
                },
                respondIme = true
            )
        }
    }
}

fun Modifier.detectSwipeUp(
    threshold: Dp = 64.dp,
    onSwipeUp: () -> Unit
): Modifier = composed {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    var totalDy by remember { mutableStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    pointerInput(thresholdPx) {
        detectVerticalDragGestures(
            onDragStart = {
                totalDy = 0f
                triggered = false
            },
            onVerticalDrag = { change, dragAmount ->
                totalDy += dragAmount            // 累计
                if (!triggered && totalDy <= -thresholdPx) {
                    triggered = true
                    onSwipeUp()
                    change.consume()
                }
            },
            onDragEnd = {
                totalDy = 0f
                triggered = false
            },
            onDragCancel = {
                totalDy = 0f
                triggered = false
            }
        )
    }
}