package com.aritxonly.deadliner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.OverviewUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.Switch
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.overview.DashboardScreen
import com.aritxonly.deadliner.ui.overview.OverviewStatsScreen
import com.aritxonly.deadliner.ui.overview.TrendAnalysisScreen
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

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
) {
    TopAppBar(
        title = stringResource(R.string.title_activity_overview),
        mode = mode,
        forceMaterial3 = forceMaterial3,
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
            )
            TrendAnalysisScreen(
                snapshot = snapshot,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
            )
            DashboardScreen(
                snapshot = snapshot,
                activity = activity,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        return
    }

    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surface) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when (selectedTab) {
            0 -> OverviewStatsScreen(
                activeStats = snapshot.activeStats,
                historyStats = snapshot.historyStats,
                completionTimeStats = snapshot.completionTimeStats,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            )

            1 -> TrendAnalysisScreen(
                snapshot = snapshot,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            )

            else -> DashboardScreen(
                snapshot = snapshot,
                activity = activity,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}

@Composable
fun OverviewScreen(
    items: List<DDLItem>,
    activity: Activity,
    showTopBar: Boolean = true,
    showNavigationIcon: Boolean = true,
    onClose: () -> Unit = {},
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = if (showTopBar) {
            {
                OverviewTopBar(
                    showNavigationIcon = showNavigationIcon,
                    onClose = onClose,
                    onShowSettings = { showSettings = true },
                )
            }
        } else {
            {
            }
        }
    ) { paddingValues ->
        OverviewContent(
            items = items,
            activity = activity,
            modifier = Modifier.padding(paddingValues),
        )
        OverviewSettingsDialog(
            visible = showSettings,
            onDismiss = { showSettings = false },
        )
    }
}
