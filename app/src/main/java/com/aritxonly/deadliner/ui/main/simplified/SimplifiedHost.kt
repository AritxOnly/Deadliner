@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.aritxonly.deadliner.ui.main.simplified

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.aritxonly.deadliner.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.EditDDLFragment
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.data.ViewModelFactory
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.model.updateNoteWithDate
import com.aritxonly.deadliner.ui.agent.AIOverlayHost
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.main.TextPageIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.core.net.toUri
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme

@Composable
fun SimplifiedHost(
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val vm: MainViewModel = viewModel(factory = ViewModelFactory(context))
    val clipboardManager = remember {
        context.getSystemService(android.content.ClipboardManager::class.java)
    }
    val view = LocalView.current

    val snackbarHostState = remember { SnackbarHostState() }

    var pendingUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var lastHandledUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        consumeDeadlinerUrl(activity)?.let { pendingUrl = it }
    }

    LaunchedEffect(pendingUrl) {
        val url = pendingUrl ?: return@LaunchedEffect
        if (url == lastHandledUrl) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.detect_share_link),
            actionLabel = context.getString(R.string.add),
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )

        if (result == SnackbarResult.ActionPerformed) {
            val item = DeadlinerURLScheme.decodeWithPassphrase(url, "deadliner-2025".toCharArray())
            val intent = Intent(context, AddDDLActivity::class.java).apply {
                putExtra("EXTRA_FULL_DDL", item)
            }
            activity.startActivity(intent)
        }
    }

    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) return@OnWindowFocusChangeListener

            // 只有窗口真正拿到焦点时才读剪贴板，避免 Android 14 的 Denying log
            val clipText = clipboardManager.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.trim()
                .orEmpty()

            if (clipText.isNotEmpty() && clipText.startsWith("deadliner://share")) {
                pendingUrl = clipText
            }
        }

        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
        }
    }

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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }
    var childRequestsBlur by remember { mutableStateOf(false) }
    val shouldBlur = showOverlay || childRequestsBlur || moreExpanded || showDeleteDialog

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

    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var fireKey by remember { mutableIntStateOf(0) }
    fun celebrate() {
        parties = PartyPresets.festive()
        fireKey++
    }
    LaunchedEffect(parties) {
        if (parties.isNotEmpty()) {
            delay(3500)
            parties = emptyList()
        }
    }

    val textFieldState = rememberTextFieldState()
    var suggestions by rememberSaveable { mutableStateOf(emptyList<DDLItem>()) }
    var base by remember { mutableStateOf<List<DDLItem>>(emptyList()) }
    LaunchedEffect(selectedPage) {
        base = vm.getBaseList(selectedPage)
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .debounce(250)
            .collect { q ->
                val f = SearchFilter.parse(q)
                suggestions = if (q.isBlank()) emptyList()
                else base.asSequence()
                    .filter { f.matches(it) }
                    .toList()
            }
    }
    
    val profile by UserProfileRepository.profile.collectAsState(initial = UserProfile())
    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname) }

    val avatarPainter: Painter? by remember(profile.avatarFileName) {
        mutableStateOf<Painter?>(
            if (profile.avatarFileName != null) {
                val file = File(context.filesDir, "avatars/${profile.avatarFileName}")
                if (file.exists()) {
                    // 读取文件并转为 Painter
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) BitmapPainter(bitmap.asImageBitmap()) else null
                } else {
                    null
                }
            } else {
                null
            }
        )
    }
    val useAvatar = avatarPainter != null
    var moreAnchorRect by remember { mutableStateOf<Rect?>(null) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    fun enterSelection(id: Long) {
        selectionMode = true
        if (!selectedIds.contains(id)) selectedIds.add(id)
    }
    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        if (selectedIds.isEmpty()) selectionMode = false
    }
    fun clearSelection() {
        selectedIds.clear()
        selectionMode = false
    }

    BackHandler(enabled = selectionMode || searchActive) {
        when {
            searchActive -> {
                onSearchActiveChange(false)
            }
            selectionMode -> clearSelection()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val effects = mutableListOf<RenderEffect>()

                if (blurRadius >= EPS) {
                    effects += RenderEffect.createBlurEffect(
                        blurRadius, blurRadius,
                        Shader.TileMode.CLAMP
                    )
                }
                if (saturation < 1f - 1e-3f) {
                    val cm = ColorMatrix().apply { setSaturation(saturation) }
                    effects += RenderEffect.createColorFilterEffect(
                        ColorMatrixColorFilter(cm)
                    )
                }

                renderEffect = when (effects.size) {
                    0 -> null
                    1 -> effects[0].asComposeRenderEffect()
                    else -> RenderEffect.createChainEffect(
                        effects[0],
                        effects[1]
                    ).asComposeRenderEffect()
                }

                scaleX = scale
                scaleY = scale
            },
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                    actionContentColor = MaterialTheme.colorScheme.inversePrimary,
                    dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        },
        topBar = {
            AnimatedContent(
                targetState = selectionMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180, delayMillis = 60)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(180)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(120)) +
                                    scaleOut(targetScale = 0.98f, animationSpec = tween(120))
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "topappbar-switch"
            ) { isSelection ->
                if (isSelection) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { clearSelection() },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_close), null,
                                    modifier = expressiveTypeModifier)
                            }
                        },
                        title = { Text(stringResource(R.string.selected_items, selectedIds.size)) }
                    )
                } else {
                    MainSearchBar(
                        textFieldState = textFieldState,
                        searchResults = suggestions,
                        onQueryChanged = { q ->
                            val f = SearchFilter.parse(q)
                            suggestions = if (q.isBlank()) emptyList()
                            else base.filter { f.matches(it) }.toList()
                        },
                        onMoreClick = { moreExpanded = true },
                        onMoreAnchorChange = { rect -> moreAnchorRect = rect },
                        useAvatar = useAvatar,
                        avatarPainter = avatarPainter,
                        activity = activity,
                        expanded = searchActive,
                        onExpandedChangeExternal = onSearchActiveChange,
                        selectedPage = selectedPage
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MainDisplay(
                ddlList = ddlList,
                refreshState = refreshState,
                selectedPage = selectedPage,
                activity = activity,
                modifier = Modifier
                    .fillMaxSize(),
                vm = vm,
                listState = listState,
                onRequestBackdropBlur = { enable -> childRequestsBlur = enable },
                onShowUndoSnackbar = { updatedHabit ->
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.habit_success),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Long,
                            withDismissAction = true
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val todayStr = LocalDate.now().toString()
                            val json = JSONObject(updatedHabit.note)
                            val datesArray = json.optJSONArray("completedDates") ?: JSONArray()
                            for (i in datesArray.length() - 1 downTo 0) {
                                if (datesArray.optString(i) == todayStr) {
                                    datesArray.remove(i)
                                }
                            }
                            json.put("completedDates", datesArray)
                            val revertedNoteJson = json.toString()
                            val revertedHabit = updatedHabit.copy(
                                note = revertedNoteJson,
                                habitCount = updatedHabit.habitCount - 1
                            )
                            DDLRepository().updateDDL(revertedHabit)
                            vm.loadData(selectedPage)
                        }
                    }
                },
                onCelebrate = { celebrate() },
                moreExpanded = moreExpanded,
                moreAnchorRect = moreAnchorRect,
                useAvatar = useAvatar,
                nickname = nickname,
                avatarPainter = avatarPainter,
                onCloseMorePanel = { moreExpanded = false },
                selectionMode = selectionMode,
                isSelected = { id -> selectedIds.contains(id) },
                onItemLongPress = { id -> enterSelection(id) },
                onItemClickInSelection = { id -> toggleSelection(id) }
            )

            key(fireKey) {
                Box(Modifier.fillMaxSize()) {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = parties
                    )
                }
            }

            AnimatedVisibility(
                visible = !searchActive,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = jumpAnim.value.dp, start = 16.dp, end = 16.dp),
                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
                exit  = slideOutVertically(targetOffsetY  = { fullHeight -> fullHeight }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    AnimatedContent(
                        targetState = selectionMode,
                        transitionSpec = {
                            (slideInVertically(
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                initialOffsetY = { full -> full / 6 }
                            ) + fadeIn(tween(150)) + scaleIn(initialScale = 0.995f, animationSpec = tween(180)))
                                .togetherWith(
                                    slideOutVertically(
                                        animationSpec = tween(140, easing = FastOutLinearInEasing),
                                        targetOffsetY = { full -> full / 6 }
                                    ) + fadeOut(tween(120)) + scaleOut(targetScale = 0.995f, animationSpec = tween(140))
                                )
                                .using(SizeTransform(clip = false))
                        },
                        label = "toolbar-switch",
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) { isSelection ->
                        if (!isSelection) {
                            HorizontalFloatingToolbar(
                                expanded = toolbarExpanded,
                                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                                leadingContent = {
                                    Box(modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                                        TextPageIndicator(
                                            text = stringResource(R.string.task),
                                            onClick = { selectedPage = DeadlineType.TASK },
                                            selected = selectedPage.toString(),
                                            tag = DeadlineType.TASK.toString(),
                                            badgeConfig = Triple(
                                                GlobalUtils.nearbyTasksBadge,
                                                dueSoonCounts[DeadlineType.TASK] ?: 0,
                                                GlobalUtils.nearbyDetailedBadge
                                            )
                                        )
                                    }
                                },
                                trailingContent = {
                                    Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                                        TextPageIndicator(
                                            text = stringResource(R.string.habit),
                                            onClick = { selectedPage = DeadlineType.HABIT },
                                            selected = selectedPage.toString(),
                                            tag = DeadlineType.HABIT.toString(),
                                            badgeConfig = Triple(
                                                GlobalUtils.nearbyTasksBadge,
                                                dueSoonCounts[DeadlineType.HABIT] ?: 0,
                                                GlobalUtils.nearbyDetailedBadge
                                            )
                                        )
                                    }
                                }
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        val intent = Intent(context, AddDDLActivity::class.java).apply {
                                            putExtra("EXTRA_CURRENT_TYPE", if (selectedPage == DeadlineType.TASK) 0 else 1)
                                        }
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
                        } else {
                            HorizontalFloatingToolbar(
                                expanded = toolbarExpanded,
                                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
                            ) {
                                IconButton(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            GlobalUtils.triggerVibration(activity, 100)

                                            if (selectedPage == DeadlineType.HABIT) {
                                                val idsToUpdate = selectedIds.toList()
                                                idsToUpdate.forEach { id ->
                                                    val item =
                                                        ddlList.find { it.id == id }
                                                            ?: return@forEach
                                                    val habitMeta =
                                                        GlobalUtils.parseHabitMetaData(item.note)
                                                    val today = LocalDate.now()

                                                    val completedDates: Set<LocalDate> =
                                                        habitMeta.completedDates.map {
                                                            LocalDate.parse(
                                                                it
                                                            )
                                                        }.toSet()

                                                    val canCheckIn = (habitMeta.total != 0 && (
                                                            if (habitMeta.frequencyType != DeadlineFrequency.TOTAL) {
                                                                (item.habitCount < habitMeta.frequency) && (completedDates.size < habitMeta.total)
                                                            } else true
                                                            ) && (item.habitTotalCount < habitMeta.total)) || (habitMeta.total == 0)

                                                    val alreadyChecked =
                                                        when (habitMeta.frequencyType) {
                                                            DeadlineFrequency.TOTAL -> false
                                                            else -> habitMeta.frequency <= item.habitCount
                                                        }
                                                    val canPerformClick =
                                                        canCheckIn && !alreadyChecked

                                                    if (!canPerformClick) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.snackbar_already_checkin),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    val updatedNote =
                                                        updateNoteWithDate(item, today)

                                                    val updatedHabit = item.copy(
                                                        note = updatedNote,
                                                        habitCount = item.habitCount + 1,
                                                        habitTotalCount = item.habitTotalCount + 1
                                                    )

                                                    DDLRepository().updateDDL(updatedHabit)
                                                }

                                                vm.loadData(selectedPage)
                                                selectedIds.clear()
                                                if (GlobalUtils.fireworksOnFinish) celebrate()
                                                Toast.makeText(
                                                    context,
                                                    R.string.toast_finished,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectionMode = false
                                            } else {
                                                val idsToUpdate = selectedIds.toList()
                                                idsToUpdate.forEach { id ->
                                                    val item =
                                                        ddlList.find { it.id == id }
                                                            ?: return@forEach
                                                    val updatedTask = item.copy(
                                                        isCompleted = true,
                                                        completeTime = LocalDateTime.now()
                                                            .toString()
                                                    )
                                                    DDLRepository().updateDDL(updatedTask)
                                                }

                                                vm.loadData(selectedPage)
                                                Toast.makeText(
                                                    context,
                                                    R.string.toast_finished,
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                selectedIds.clear()
                                                selectionMode = false
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                R.string.please_select_done_first,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                ) {
                                    Icon(
                                        iconResource(R.drawable.ic_done),
                                        contentDescription = null
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            val idsToUpdate = selectedIds.toList()
                                            var count = 0

                                            idsToUpdate.forEach { id ->
                                                val item = ddlList.firstOrNull { it.id == id }
                                                    ?: return@forEach
                                                if (item.isCompleted) {
                                                    val updated = item.copy(isArchived = true)
                                                    DDLRepository().updateDDL(updated)
                                                    count++
                                                }
                                            }

                                            // 刷新 & 提示
                                            vm.loadData(selectedPage)
                                            Toast.makeText(
                                                activity,
                                                activity.getString(R.string.toast_archived, count),
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            // 清除多选状态 & 恢复 AppBar
                                            selectedIds.clear()
                                            selectionMode = false
                                        } else {
                                            Toast.makeText(
                                                activity,
                                                activity.getString(R.string.please_select_done_first),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                ) {
                                    Icon(iconResource(R.drawable.ic_archiving), null)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            GlobalUtils.triggerVibration(activity, 200)
                                            showDeleteDialog = true
                                        } else {
                                            Toast.makeText(
                                                activity,
                                                activity.getString(R.string.please_select_delete_first),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                ) {
                                    Icon(iconResource(R.drawable.ic_delete), null)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            // 取第一个选中项
                                            val firstId = selectedIds.first()
                                            val clickedItem =
                                                ddlList.firstOrNull { it.id == firstId }

                                            if (clickedItem != null) {
                                                val editDialog =
                                                    EditDDLFragment(clickedItem) { updatedDDL ->
                                                        // 回调：保存并刷新
                                                        DDLRepository().updateDDL(updatedDDL)
                                                        vm.loadData(selectedPage)

                                                        // 清除多选状态
                                                        selectedIds.clear()
                                                        selectionMode = false
                                                    }
                                                editDialog.show(
                                                    activity.supportFragmentManager,
                                                    "EditDDLFragment"
                                                )
                                            } else {
                                                Toast.makeText(
                                                    activity,
                                                    activity.getString(R.string.please_select_edit_first),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } else {
                                            Toast.makeText(
                                                activity,
                                                activity.getString(R.string.please_select_edit_first),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                ) {
                                    Icon(iconResource(R.drawable.ic_edit), null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOverlay) {
        AIOverlayHost(
            initialText = "",
            onAddDDL = { intent ->
                launcher.launch(intent)
            },
            onRemoveFromWindow = {
                showOverlay = false
            },
            respondIme = true
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text  = { Text(stringResource(R.string.alert_delete_message)) },
            dismissButton = {
                TextButton(onClick = {
                    // 取消删除
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 确认删除
                    val idsToDelete = selectedIds.toList() // 拷贝一份避免并发修改
                    idsToDelete.forEach { id ->
                        DDLRepository().deleteDDL(id)
                        DeadlineAlarmScheduler.cancelAlarm(activity.applicationContext, id)
                    }

                    vm.loadData(selectedPage)

                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_deletion),
                        Toast.LENGTH_SHORT
                    ).show()

                    // 清空多选 & 恢复 AppBar
                    selectedIds.clear()
                    selectionMode = false

                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.accept))
                }
            }
        )
    }
}

fun Modifier.detectSwipeUp(
    threshold: Dp = 64.dp,
    onSwipeUp: () -> Unit,
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

private fun consumeDeadlinerUrl(activity: Activity): String? {
    val data = activity.intent?.dataString ?: return null
    if (data.startsWith("deadliner://share")) {
        // 消费一次，避免重组或回到前台时重复触发
        activity.intent?.data = null
        return data
    }
    return null
}