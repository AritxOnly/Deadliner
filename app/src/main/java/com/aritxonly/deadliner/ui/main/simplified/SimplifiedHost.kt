@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.aritxonly.deadliner.ui.main.simplified

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.aritxonly.deadliner.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
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
    var selectedPage by remember { mutableStateOf(vm.currentType) }
    LaunchedEffect(selectedPage) {
        vm.loadData(selectedPage)
    }

    var showOverlay by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (showOverlay) 0.98f else 1f, label = "content-scale")

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
                    // 低版本无 renderEffect，自动忽略
                    if (showOverlay && true) {
                        // 模糊 + 去饱和（近似你的链式 RenderEffect 方案）
                        val blur = android.graphics.RenderEffect.createBlurEffect(24f, 24f, android.graphics.Shader.TileMode.CLAMP)
                        val cm = android.graphics.ColorMatrix().apply { setSaturation(0.5f) }
                        val cf = android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(cm))
                        renderEffect = android.graphics.RenderEffect.createChainEffect(blur, cf).asComposeRenderEffect()
                    } else {
                        renderEffect = null
                    }
                    this.scaleX = scale
                    this.scaleY = scale
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
                onReload = { vm.loadData(vm.currentType) },
                activity = activity,
                modifier = Modifier
                    .fillMaxSize(),
                vm = vm
            )

            HorizontalFloatingToolbar(
                expanded = toolbarExpanded,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = jumpAnim.value.dp, start = 16.dp, end = 16.dp),
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
            ) {
                TextPageIndicator(
                    text = stringResource(R.string.task),
                    onClick = { selectedPage = DeadlineType.TASK },
                    selected = selectedPage.toString(),
                    tag = DeadlineType.TASK.toString()
                )

                Spacer(modifier = Modifier.padding(8.dp))

                FilledIconButton(
                    onClick = {
                        val intent = Intent(context, AddDDLActivity::class.java)
                        launcher.launch(intent)
                    },
                    modifier = Modifier.width(56.dp)
                        .detectSwipeUp {
                            Log.d("SwipeUp", "Triggered")
                            showOverlay = true
                        }
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_add), "")
                }

                Spacer(modifier = Modifier.padding(8.dp))

                TextPageIndicator(
                    text = stringResource(R.string.habit),
                    onClick = { selectedPage = DeadlineType.HABIT },
                    selected = selectedPage.toString(),
                    tag = DeadlineType.HABIT.toString()
                )
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