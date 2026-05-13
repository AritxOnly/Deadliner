package com.aritxonly.deadliner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.OverviewUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.Switch
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.overview.DashboardScreen
import com.aritxonly.deadliner.ui.overview.OverviewStatsScreen
import com.aritxonly.deadliner.ui.overview.TrendAnalysisScreen
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults.blurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

class OverviewActivity : DeadlinerComponentActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, OverviewActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)

        setContent {
            DeadlinerTheme {
                val items = DDLRepository().getDDLsByType(DeadlineType.TASK)
                OverviewScreen(
                    items = items,
                    activity = this,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}

@Composable
fun hashColor(key: String): Color {
    return when (key) {
        stringResource(R.string.today_completed),
        stringResource(R.string.completed),
        stringResource(R.string.cumulative_completed) -> colorResource(R.color.chart_green)

        stringResource(R.string.pending_tasks),
        stringResource(R.string.current_pending),
        stringResource(R.string.incomplete) -> colorResource(R.color.chart_orange)

        stringResource(R.string.today_overdue),
        stringResource(R.string.overdue),
        stringResource(R.string.cumulative_overdue) -> colorResource(R.color.chart_red)

        else -> colorResource(R.color.chart_blue)
    }
}

@Composable
fun OverviewTopBar(
    showNavigationIcon: Boolean = true,
    onClose: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    forceMaterial3: Boolean = false,
    useParentMaterialContainer: Boolean = false,
) {
    TopAppBar(
        title = stringResource(R.string.title_activity_overview),
        mode = mode,
        forceMaterial3 = forceMaterial3,
        useParentMaterialContainer = useParentMaterialContainer,
        navigationIcon = if (showNavigationIcon) {
            {
                IconButton(onClick = onClose) {
                    Icon(
                        painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = expressiveTypeModifier,
                    )
                }
            }
        } else {
            null
        },
        actions = {
            IconButton(onClick = onShowSettings) {
                Icon(
                    painterResource(R.drawable.ic_pref),
                    contentDescription = stringResource(R.string.settings_more),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier,
                )
            }
        }
    )
}

@Composable
fun OverviewSettingsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    var showOverdueSeries by remember {
        mutableStateOf(GlobalUtils.OverviewSettings.showOverdueInDaily)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accept))
            }
        },
        title = {
            Text(stringResource(R.string.settings_more))
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overview_settings_show_overdue_series),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.overview_settings_show_overdue_series_support),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = showOverdueSeries,
                    onCheckedChange = { checked ->
                        showOverdueSeries = checked
                        GlobalUtils.OverviewSettings.showOverdueInDaily = checked
                    }
                )
            }
        }
    )
}

@Composable
fun OverviewContent(
    items: List<DDLItem>,
    activity: Activity,
    modifier: Modifier = Modifier,
    flattenedLayout: Boolean = false,
    selectedTab: Int = 0,
    onSelectedTabChange: (Int) -> Unit = {},
    showTabsInContent: Boolean = true,
    topContentPadding: Dp = 0.dp,
) {
    val snapshot = remember(items) {
        OverviewUtils.buildSnapshot(activity, items)
    }

    if (flattenedLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OverviewStatsScreen(
                activeStats = snapshot.activeStats,
                historyStats = snapshot.historyStats,
                completionTimeStats = snapshot.completionTimeStats,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
                topContentPadding = topContentPadding,
            )
            TrendAnalysisScreen(
                snapshot = snapshot,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
                topContentPadding = topContentPadding,
            )
            DashboardScreen(
                snapshot = snapshot,
                activity = activity,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
                topContentPadding = topContentPadding,
            )
        }
        return
    }

    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    var internalSelectedTab by rememberSaveable { mutableStateOf(0) }
    val currentTab = if (showTabsInContent) internalSelectedTab else selectedTab

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        if (showTabsInContent) {
            TabRow(
                tabs = tabs,
                selectedTabIndex = currentTab,
                onTabSelected = {
                    internalSelectedTab = it
                    onSelectedTabChange(it)
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surface) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        when (currentTab) {
            0 -> OverviewStatsScreen(
                activeStats = snapshot.activeStats,
                historyStats = snapshot.historyStats,
                completionTimeStats = snapshot.completionTimeStats,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                topContentPadding = topContentPadding,
            )

            1 -> TrendAnalysisScreen(
                snapshot = snapshot,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                topContentPadding = topContentPadding,
            )

            else -> DashboardScreen(
                snapshot = snapshot,
                activity = activity,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                topContentPadding = topContentPadding,
            )
        }
    }
}

private val OverviewHeaderShape = RoundedCornerShape(0.dp)

@Composable
fun OverviewTopBarWithTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigationIcon: Boolean = true,
    onClose: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    forceMaterial3: Boolean = false,
) {
    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val advancedMaterialBlurred = advancedMaterial.enabled && backdrop != null
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = advancedMaterial.topBarTintAlpha)

    OverviewHeaderContainer(
        advancedMaterialBlurred = advancedMaterialBlurred,
        backdrop = backdrop,
        blurRadius = advancedMaterial.blurRadius,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        surfaceTint = surfaceTint,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (advancedMaterialBlurred) Color.Transparent else if (advancedMaterial.enabled) surfaceTint else MaterialTheme.colorScheme.surface),
        ) {
            OverviewTopBar(
                showNavigationIcon = showNavigationIcon,
                onClose = onClose,
                onShowSettings = onShowSettings,
                mode = mode,
                forceMaterial3 = forceMaterial3,
                useParentMaterialContainer = true,
            )
            TabRow(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = onTabSelected,
                divider = { HorizontalDivider(color = Color.Transparent) },
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun OverviewHeaderContainer(
    advancedMaterialBlurred: Boolean,
    backdrop: LayerBackdrop? = null,
    blurRadius: Float,
    noiseCoefficient: Float,
    surfaceTint: Color,
    content: @Composable () -> Unit,
) {
    if (advancedMaterialBlurred && backdrop != null) {
        val blurColors = blurColors(blendColors = listOf(BlendColorEntry(surfaceTint)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .textureBlur(
                    backdrop = backdrop,
                    shape = OverviewHeaderShape,
                    blurRadius = blurRadius,
                    noiseCoefficient = noiseCoefficient,
                    colors = blurColors,
                ),
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun OverviewScreen(
    items: List<DDLItem>,
    activity: Activity,
    showTopBar: Boolean = true,
    showNavigationIcon: Boolean = true,
    flattenedLayout: Boolean = false,
    onClose: () -> Unit = {},
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val layoutDirection = LocalLayoutDirection.current

    AdaptiveMaterialScaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        wrapTopBarInMaterialContainer = false,
        topBar = {
            if (!showTopBar) return@AdaptiveMaterialScaffold
            if (flattenedLayout) {
                OverviewTopBar(
                    showNavigationIcon = showNavigationIcon,
                    onClose = onClose,
                    onShowSettings = { showSettings = true },
                )
            } else {
                OverviewTopBarWithTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    showNavigationIcon = showNavigationIcon,
                    onClose = onClose,
                    onShowSettings = { showSettings = true },
                )
            }
        },
    ) { paddingValues ->
        val topContentPadding = if (showTopBar && !flattenedLayout) {
            paddingValues.calculateTopPadding()
        } else {
            0.dp
        }
        val contentModifier = if (showTopBar && !flattenedLayout) {
            Modifier.padding(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding(),
            )
        } else {
            Modifier.padding(paddingValues)
        }
        OverviewContent(
            items = items,
            activity = activity,
            modifier = contentModifier,
            flattenedLayout = flattenedLayout,
            selectedTab = selectedTab,
            onSelectedTabChange = { selectedTab = it },
            showTabsInContent = flattenedLayout,
            topContentPadding = topContentPadding,
        )
        OverviewSettingsDialog(
            visible = showSettings,
            onDismiss = { showSettings = false },
        )
    }
}
