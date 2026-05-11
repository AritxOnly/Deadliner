package com.aritxonly.deadliner.ui.main.modern

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.ArchiveActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.OverviewContent
import com.aritxonly.deadliner.OverviewSettingsDialog
import com.aritxonly.deadliner.OverviewTopBar
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.capture.ui.CaptureContent
import com.aritxonly.deadliner.capture.ui.CaptureTopBar
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.MainViewModelFactory
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.data.HabitViewModelFactory
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.localutils.GlobalUtils.showHabitReminderDialog
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.ui.agent.AIOverlayHost
import com.aritxonly.deadliner.ui.base.AdaptiveNavItem
import com.aritxonly.deadliner.ui.base.AdaptiveNavigationSuiteScaffold
import com.aritxonly.deadliner.ui.base.FloatingActionButton
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.main.shared.MainSection
import com.aritxonly.deadliner.ui.main.modern.components.ModernMainHeader
import com.aritxonly.deadliner.ui.main.shared.DeadlinerUrlIntake
import com.aritxonly.deadliner.ui.main.shared.MainDisplay
import com.aritxonly.deadliner.ui.main.shared.MainHostLifecycleCoordinator
import com.aritxonly.deadliner.ui.main.shared.rememberMainHostUiState
import com.aritxonly.deadliner.ui.main.shared.rememberMainSelectionActionController
import com.aritxonly.deadliner.ui.main.shared.rememberMainHostState
import com.aritxonly.deadliner.ui.main.simplified.MainSearchBar
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import java.io.File
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

private fun LazyListState.hasScrolledPastHeader(threshold: Int = 12): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > threshold
}

private enum class ModernTopBarState {
    HIDDEN,
    LIST_DEFAULT,
    LIST_SELECTION,
    OVERVIEW,
    CAPTURE,
}

private const val WideLayoutMinWidthDp = 840
private const val FlattenedOverviewMinWidthDp = 1100

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ModernHost(
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWideLayout = configuration.screenWidthDp >= WideLayoutMinWidthDp
    val useFlattenedOverview = configuration.screenWidthDp >= FlattenedOverviewMinWidthDp
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(context))
    val habitVm: HabitViewModel = viewModel(factory = HabitViewModelFactory(context))
    val captureVm: CaptureViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    val hostState = rememberMainHostState(vm.currentType)
    val uiState = rememberMainHostUiState()
    var overviewItems by remember { mutableStateOf<List<DDLItem>>(emptyList()) }
    var showOverviewSettings by rememberSaveable { mutableStateOf(false) }
    var showCaptureMergeSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchPage by rememberSaveable { mutableStateOf(false) }
    var activeSelectionPane by rememberSaveable { mutableStateOf<DeadlineType?>(null) }
    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var fireKey by remember { mutableIntStateOf(0) }

    val taskListState = rememberLazyListState()
    val habitListState = rememberLazyListState()
    val textFieldState = rememberTextFieldState()
    var suggestions by rememberSaveable { mutableStateOf(emptyList<DDLItem>()) }
    var base by remember { mutableStateOf<List<DDLItem>>(emptyList()) }

    LaunchedEffect(isWideLayout) {
        if (isWideLayout && hostState.selectedPage != DeadlineType.TASK) {
            hostState.selectedPage = DeadlineType.TASK
            vm.selectType(DeadlineType.TASK)
        }
    }

    LaunchedEffect(hostState.selectionMode) {
        if (!hostState.selectionMode) {
            activeSelectionPane = null
            if (isWideLayout) {
                hostState.selectedPage = DeadlineType.TASK
                vm.selectType(DeadlineType.TASK)
            }
        }
    }
    fun celebrate() {
        parties = PartyPresets.festive()
        fireKey++
    }
    LaunchedEffect(parties) {
        if (parties.isNotEmpty()) {
            kotlinx.coroutines.delay(3500)
            parties = emptyList()
        }
    }

    val profile by UserProfileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val avatarPainter: Painter? by remember(profile.avatarFileName) {
        mutableStateOf(
            profile.avatarFileName?.let { name ->
                val file = File(context.filesDir, "avatars/$name")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapPainter(it.asImageBitmap()) } else null
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            if (isWideLayout) vm.loadAllData() else vm.loadData(hostState.selectedPage)
            habitVm.refresh()
        }
    }

    MainHostLifecycleCoordinator(
        activity = activity,
        selectedPage = hostState.selectedPage,
        onPageChanged = { page ->
            vm.selectType(page)
            if (isWideLayout) vm.loadAllData(silent = true) else vm.loadData(page)
            habitVm.refresh()
        },
        onResumed = { page ->
            vm.selectType(page)
            if (isWideLayout) vm.loadAllData(silent = true) else vm.loadData(page)
            habitVm.refresh()
            overviewItems = DDLRepository().getDDLsByType(DeadlineType.TASK)
        },
        onInitialExtra = {
            overviewItems = DDLRepository().getDDLsByType(DeadlineType.TASK)
        },
    )

    LaunchedEffect(hostState.selectedPage) {
        base = vm.getBaseList(DeadlineType.TASK) + vm.getBaseList(DeadlineType.HABIT)
    }

    LaunchedEffect(hostState.selectedSection) {
        if (hostState.selectedSection == MainSection.OVERVIEW) {
            overviewItems = DDLRepository().getDDLsByType(DeadlineType.TASK)
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .debounce(250)
            .collect { q ->
                val f = SearchFilter.parse(q)
                suggestions = if (q.isBlank()) emptyList() else base.filter { f.matches(it) }
            }
    }

    DeadlinerUrlIntake(
        activity = activity,
        snackbarHostState = snackbarHostState,
        pendingUrl = hostState.pendingUrl,
        onPendingUrlChange = { hostState.pendingUrl = it },
        onDecodedItemReady = { item ->
            val intent = Intent(context, AddDDLActivity::class.java).apply {
                putExtra("EXTRA_FULL_DDL", item)
            }
            activity.startActivity(intent)
        },
    )

    BackHandler(enabled = hostState.selectionMode || showSearchPage || hostState.showOverlay) {
        when {
            hostState.showOverlay -> hostState.showOverlay = false
            showSearchPage -> {
                showSearchPage = false
                onSearchActiveChange(false)
            }
            hostState.selectionMode -> {
                hostState.clearSelection()
                activeSelectionPane = null
            }
        }
    }

    val navItems = listOf(
        AdaptiveNavItem("list", context.getString(R.string.main_section_list), ImageVector.vectorResource(R.drawable.ic_done)),
        AdaptiveNavItem("overview", context.getString(R.string.overview), ImageVector.vectorResource(R.drawable.ic_chart)),
        AdaptiveNavItem("capture", context.getString(R.string.capture_title), ImageVector.vectorResource(R.drawable.ic_draw)),
        AdaptiveNavItem("search", context.getString(R.string.search), ImageVector.vectorResource(R.drawable.ic_search)),
    )

    val selectedKey = if (showSearchPage) {
        "search"
    } else when (hostState.selectedSection) {
        MainSection.LIST -> "list"
        MainSection.OVERVIEW -> "overview"
        MainSection.CAPTURE -> "capture"
    }
    val topBarState = when {
        showSearchPage -> ModernTopBarState.HIDDEN
        hostState.selectedSection == MainSection.LIST && hostState.selectionMode ->
            ModernTopBarState.LIST_SELECTION
        hostState.selectedSection == MainSection.LIST -> ModernTopBarState.LIST_DEFAULT
        hostState.selectedSection == MainSection.OVERVIEW -> ModernTopBarState.OVERVIEW
        else -> ModernTopBarState.CAPTURE
    }
    val shouldCollapseHeaderAccessories by remember(
        isWideLayout,
        hostState.selectedSection,
        hostState.selectedPage,
        hostState.selectionMode,
        showSearchPage,
        taskListState,
        habitListState,
    ) {
        androidx.compose.runtime.derivedStateOf {
            if (
                hostState.selectedSection != MainSection.LIST ||
                hostState.selectionMode ||
                showSearchPage
            ) {
                false
            } else if (isWideLayout) {
                taskListState.hasScrolledPastHeader() || habitListState.hasScrolledPastHeader()
            } else {
                when (hostState.selectedPage) {
                    DeadlineType.TASK -> taskListState.hasScrolledPastHeader()
                    DeadlineType.HABIT -> habitListState.hasScrolledPastHeader()
                }
            }
        }
    }

    val ddlList by vm.ddlListFlow.collectAsStateWithLifecycle()
    val taskList by vm.taskListFlow.collectAsStateWithLifecycle()
    val habitList by vm.habitListFlow.collectAsStateWithLifecycle()
    val refreshState by vm.refreshState.collectAsStateWithLifecycle()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val isShortBottomNavigation = navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium
    val selectionActions = rememberMainSelectionActionController(
        activity = activity,
        vm = vm,
        habitVm = habitVm,
        hostState = hostState,
    )

    LaunchedEffect(selectionActions.habitReminderTargetId) {
        val targetId = selectionActions.habitReminderTargetId ?: return@LaunchedEffect
        showHabitReminderDialog(context, targetId) {
            selectionActions.clearHabitReminderTarget()
        }
    }

    val shouldBlur = hostState.showOverlay ||
        uiState.childRequestsBlur ||
        selectionActions.showDeleteDialog ||
        (selectionActions.habitReminderTargetId != null) ||
        (hostState.selectedSection == MainSection.OVERVIEW && showOverviewSettings) ||
        (hostState.selectedSection == MainSection.CAPTURE && showCaptureMergeSheet)
    val blurProgress by animateFloatAsState(
        targetValue = if (shouldBlur) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "modern-blur-progress",
    )
    val blurRadius = (24f * blurProgress).coerceIn(0f, 24f)
    val saturation = 1f - (0.5f * blurProgress)
    val scale by animateFloatAsState(
        targetValue = if (hostState.showOverlay) 0.98f else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "modern-content-scale",
    )
    val blurEps = 0.5f

    AdaptiveNavigationSuiteScaffold(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val effects = mutableListOf<RenderEffect>()

                if (blurRadius >= blurEps) {
                    effects += RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
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
        items = navItems,
        selectedKey = selectedKey,
        onItemSelected = {
            when (it.key) {
                "search" -> {
                    hostState.selectedSection = MainSection.LIST
                    showSearchPage = true
                    onSearchActiveChange(true)
                    textFieldState.edit { replace(0, length, "") }
                    suggestions = emptyList()
                }
                "overview" -> {
                    showSearchPage = false
                    hostState.selectedSection = MainSection.OVERVIEW
                }
                "capture" -> {
                    showSearchPage = false
                    hostState.selectedSection = MainSection.CAPTURE
                }
                else -> {
                    showSearchPage = false
                    hostState.selectedSection = MainSection.LIST
                }
            }
            hostState.clearSelection()
            activeSelectionPane = null
        },
        topBar = {
            AnimatedContent(
                targetState = topBarState,
                transitionSpec = {
                    val enter =
                        fadeIn(animationSpec = tween(180, delayMillis = 20)) +
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight / 6 },
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                            )
                    val exit =
                        fadeOut(animationSpec = tween(120)) +
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> -fullHeight / 10 },
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                            )

                    enter.togetherWith(exit).using(SizeTransform(clip = false))
                },
                label = "modern-topbar-transition",
            ) { currentTopBarState ->
                when (currentTopBarState) {
                    ModernTopBarState.HIDDEN -> Unit
                    ModernTopBarState.LIST_DEFAULT -> {
                        ModernMainHeader(
                            activity = activity,
                            selectedPage = hostState.selectedPage,
                            onSelectedPageChange = { hostState.selectedPage = it },
                            avatarPainter = avatarPainter,
                            onShowAiOverlay = { hostState.showOverlay = true },
                            showPageTabs = !isWideLayout,
                            showAccessoryRow = !isWideLayout && !shouldCollapseHeaderAccessories,
                            showAiActionInTopBar = isWideLayout,
                        )
                    }
                    ModernTopBarState.LIST_SELECTION -> {
                        TopAppBar(
                            title = context.getString(R.string.selected_items, hostState.selectedIds.size),
                            mode = TopAppBarStyle.SMALL,
                            titleTextStyle = MaterialTheme.typography.titleLarge,
                            navigationIcon = {
                                IconButton(onClick = {
                                    hostState.clearSelection()
                                    activeSelectionPane = null
                                }) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.close),
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    selectionActions.onDoneClick(
                                        ddlList = ddlList,
                                        onCelebrate = {
                                            if (com.aritxonly.deadliner.localutils.GlobalUtils.fireworksOnFinish) {
                                                celebrate()
                                            }
                                        },
                                    )
                                }) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_done),
                                        contentDescription = stringResource(R.string.accept)
                                    )
                                }
                                if (hostState.selectedPage == DeadlineType.TASK) {
                                    IconButton(onClick = { selectionActions.onArchiveClick(ddlList = ddlList) }) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.ic_archiving),
                                            contentDescription = stringResource(R.string.archive)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { selectionActions.onReminderClick(ddlList = ddlList) }) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.ic_notification_add),
                                            contentDescription = stringResource(R.string.settings_more)
                                        )
                                    }
                                }
                                IconButton(onClick = { selectionActions.onDeleteClick() }) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.delete)
                                    )
                                }
                                IconButton(onClick = { selectionActions.onEditClick(ddlList = ddlList) }) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_edit),
                                        contentDescription = stringResource(R.string.edit)
                                    )
                                }
                            },
                        )
                    }
                    ModernTopBarState.OVERVIEW -> {
                        OverviewTopBar(
                            showNavigationIcon = false,
                            onShowSettings = { showOverviewSettings = true },
                            mode = TopAppBarStyle.SMALL,
                        )
                    }
                    ModernTopBarState.CAPTURE -> {
                        CaptureTopBar(
                            vm = captureVm,
                            onClose = { hostState.selectedSection = MainSection.LIST },
                            showNavigationIcon = false,
                            onRequestMerge = { showCaptureMergeSheet = true },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val shouldShowFab =
                hostState.selectedSection == MainSection.LIST && !hostState.selectionMode && !showSearchPage

                if (shouldShowFab) {
                    val onFabClick = {
                        val intent = Intent(context, AddDDLActivity::class.java).apply {
                            putExtra("EXTRA_CURRENT_TYPE", if (hostState.selectedPage == DeadlineType.TASK) 0 else 1)
                        }
                        launcher.launch(intent)
                    }
                    FloatingActionButton(
                        onClick = onFabClick,
                        forceMaterial3 = true,
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add_task)
                        )
                    }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {

            when (hostState.selectedSection) {
                MainSection.LIST -> {
                    if (!showSearchPage) {
                        val onUndoSnackbar: (DDLItem) -> Unit = { updatedHabit ->
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.habit_success),
                                    actionLabel = context.getString(R.string.undo),
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    val todayStr = LocalDate.now().toString()
                                    val json = JSONObject(updatedHabit.note)
                                    val datesArray =
                                        json.optJSONArray("completedDates") ?: JSONArray()
                                    for (i in datesArray.length() - 1 downTo 0) {
                                        if (datesArray.optString(i) == todayStr) {
                                            datesArray.remove(i)
                                        }
                                    }
                                    json.put("completedDates", datesArray)
                                    val revertedHabit = updatedHabit.copy(
                                        note = json.toString(),
                                        habitCount = updatedHabit.habitCount - 1,
                                    )
                                    com.aritxonly.deadliner.data.DDLRepository()
                                        .updateDDL(revertedHabit)
                                    if (isWideLayout) vm.loadAllData(silent = true) else vm.loadData(hostState.selectedPage)
                                    habitVm.refresh()
                                }
                            }
                        }

                        if (isWideLayout) {
                            val taskPaneBlocked = hostState.selectionMode && activeSelectionPane == DeadlineType.HABIT
                            val habitPaneBlocked = hostState.selectionMode && activeSelectionPane == DeadlineType.TASK
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MainDisplay(
                                        ddlList = taskList,
                                        habitViewModel = habitVm,
                                        refreshState = refreshState,
                                        selectedPage = DeadlineType.TASK,
                                        activity = activity,
                                        vm = vm,
                                        listState = taskListState,
                                        modifier = Modifier.fillMaxSize(),
                                        moreExpanded = false,
                                        moreAnchorRect = null,
                                        useAvatar = false,
                                        nickname = profile.nickname,
                                        avatarPainter = avatarPainter,
                                        onCloseMorePanel = {},
                                        selectionMode = hostState.selectionMode,
                                        isSelected = { id -> hostState.selectedIds.contains(id) },
                                        onItemLongPress = { id ->
                                            if (!taskPaneBlocked && (activeSelectionPane == null || activeSelectionPane == DeadlineType.TASK)) {
                                                activeSelectionPane = DeadlineType.TASK
                                                hostState.selectedPage = DeadlineType.TASK
                                                vm.selectType(DeadlineType.TASK)
                                                hostState.enterSelection(id)
                                            }
                                        },
                                        onItemClickInSelection = { id ->
                                            if (!taskPaneBlocked && activeSelectionPane == DeadlineType.TASK) {
                                                hostState.toggleSelection(id)
                                            }
                                        },
                                        onRequestBackdropBlur = { enable ->
                                            uiState.childRequestsBlur = enable
                                        },
                                        onShowUndoSnackbar = onUndoSnackbar,
                                        onCelebrate = { celebrate() },
                                    )
                                    if (taskPaneBlocked) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    MainDisplay(
                                        ddlList = habitList,
                                        habitViewModel = habitVm,
                                        refreshState = refreshState,
                                        selectedPage = DeadlineType.HABIT,
                                        activity = activity,
                                        vm = vm,
                                        listState = habitListState,
                                        modifier = Modifier.fillMaxSize(),
                                        moreExpanded = false,
                                        moreAnchorRect = null,
                                        useAvatar = false,
                                        nickname = profile.nickname,
                                        avatarPainter = avatarPainter,
                                        onCloseMorePanel = {},
                                        selectionMode = hostState.selectionMode,
                                        isSelected = { id -> hostState.selectedIds.contains(id) },
                                        onItemLongPress = { id ->
                                            if (!habitPaneBlocked && (activeSelectionPane == null || activeSelectionPane == DeadlineType.HABIT)) {
                                                activeSelectionPane = DeadlineType.HABIT
                                                hostState.selectedPage = DeadlineType.HABIT
                                                vm.selectType(DeadlineType.HABIT)
                                                hostState.enterSelection(id)
                                            }
                                        },
                                        onItemClickInSelection = { id ->
                                            if (!habitPaneBlocked && activeSelectionPane == DeadlineType.HABIT) {
                                                hostState.toggleSelection(id)
                                            }
                                        },
                                        onRequestBackdropBlur = { enable ->
                                            uiState.childRequestsBlur = enable
                                        },
                                        onShowUndoSnackbar = onUndoSnackbar,
                                        onCelebrate = { celebrate() },
                                    )
                                    if (habitPaneBlocked) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        )
                                    }
                                }
                            }
                        } else {
                            MainDisplay(
                                ddlList = ddlList,
                                habitViewModel = habitVm,
                                refreshState = refreshState,
                                selectedPage = hostState.selectedPage,
                                activity = activity,
                                vm = vm,
                                listState = if (hostState.selectedPage == DeadlineType.TASK) {
                                    taskListState
                                } else {
                                    habitListState
                                },
                                moreExpanded = false,
                                moreAnchorRect = null,
                                useAvatar = avatarPainter != null,
                                nickname = profile.nickname,
                                avatarPainter = avatarPainter,
                                onCloseMorePanel = {},
                                selectionMode = hostState.selectionMode,
                                isSelected = { id -> hostState.selectedIds.contains(id) },
                                onItemLongPress = { id -> hostState.enterSelection(id) },
                                onItemClickInSelection = { id -> hostState.toggleSelection(id) },
                                onRequestBackdropBlur = { enable ->
                                    uiState.childRequestsBlur = enable
                                },
                                onShowUndoSnackbar = onUndoSnackbar,
                                onCelebrate = { celebrate() },
                            )
                        }
                    }
                }
                MainSection.OVERVIEW -> {
                    OverviewContent(
                        items = overviewItems,
                        activity = activity,
                        flattenedLayout = useFlattenedOverview,
                    )
                }
                MainSection.CAPTURE -> {
                    CaptureContent(
                        vm = captureVm,
                        twoColumnLayout = isWideLayout,
                        showMergeSheet = showCaptureMergeSheet,
                        onShowMergeSheetChange = { showCaptureMergeSheet = it },
                    )
                }
            }
            }

        AnimatedVisibility(
            visible = showSearchPage,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                expandVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                shrinkVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            label = "modern-searchbar-visibility",
        ) {
            MainSearchBar(
                textFieldState = textFieldState,
                onQueryChanged = { q ->
                    val f = SearchFilter.parse(q)
                    suggestions = if (q.isBlank()) emptyList() else base.filter { f.matches(it) }
                },
                searchResults = suggestions,
                activity = activity,
                habitViewModel = habitVm,
                expanded = true,
                onExpandedChangeExternal = { expanded ->
                    if (!expanded) {
                        showSearchPage = false
                        onSearchActiveChange(false)
                    }
                },
                selectedPage = hostState.selectedPage,
                miuixMode = false,
                resultsHorizontalPadding = 0.dp,
                mixedResultTypes = true,
            )
        }

        key(fireKey) {
            Box(Modifier.fillMaxSize()) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = parties
                )
            }
        }
        }
    }

    OverviewSettingsDialog(
        visible = hostState.selectedSection == MainSection.OVERVIEW && showOverviewSettings,
        onDismiss = { showOverviewSettings = false },
    )

    if (selectionActions.showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectionActions.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            dismissButton = {
                TextButton(onClick = { selectionActions.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { selectionActions.confirmDeleteSelected() }) {
                    Text(stringResource(R.string.accept))
                }
            },
        )
    }

    if (hostState.showOverlay) {
        AIOverlayHost(
            initialText = "",
            onAddDDL = { launcher.launch(it) },
            onRemoveFromWindow = { hostState.showOverlay = false },
            respondIme = true,
        )
    }
}
