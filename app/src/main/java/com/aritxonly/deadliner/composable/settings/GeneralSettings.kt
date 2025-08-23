package com.aritxonly.deadliner.composable.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.SplitController
import androidx.window.layout.WindowMetricsCalculator
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsScreen(
    nav: NavHostController,
    handleRestart: () -> Unit,
    navigateUp: () -> Unit
) {
    var hideFromRecent by remember { mutableStateOf(GlobalUtils.hideFromRecent) }
    val onHideFromRecentChange: (Boolean) -> Unit = {
        hideFromRecent = it
        GlobalUtils.hideFromRecent = it
    }

    val isTablet = rememberIsLargeDeviceByMaxMetrics()
    val supportSplit = rememberSupportSplit(LocalContext.current)
    val supportDynamicSplit = rememberSupportDynamicSplit()

    var embeddedActivities by remember { mutableStateOf(GlobalUtils.embeddedActivities) }
    val onEmbeddedActivitiesChange: (Boolean) -> Unit = {
        embeddedActivities = it
        GlobalUtils.embeddedActivities = it
        handleRestart()
    }

    var splitPlaceholder by remember { mutableStateOf(GlobalUtils.splitPlaceholderEnable) }
    val onPlaceholderChange: (Boolean) -> Unit = {
        splitPlaceholder = it
        GlobalUtils.splitPlaceholderEnable = it
        handleRestart()
    }

    var dynamicSplit by remember { mutableStateOf(GlobalUtils.dynamicSplit) }
    val onDynamicSplitChange: (Boolean) -> Unit = {
        dynamicSplit = it
        GlobalUtils.dynamicSplit = it
        handleRestart()
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_general),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SvgCard(R.drawable.svg_general, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_general_main)) {
                SettingsRoute.generalThirdRoutes.forEachIndexed { index, route ->
                    SettingItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .clickable { nav.navigate(route.route) },
                        headlineText = stringResource(route.titleRes),
                        supportingText = stringResource(route.supportRes!!),
                        trailingContent = null
                    )

                    if (index != SettingsRoute.generalThirdRoutes.lastIndex) {
                        SettingsSectionDivider()
                    }
                }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_general_others)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_hide_from_recent,
                    supportingText = R.string.settings_support_hide_from_recent,
                    checked = hideFromRecent,
                    onCheckedChange = onHideFromRecentChange
                )
            }

            if (isTablet && supportSplit) {
                SettingsSection(topLabel = stringResource(R.string.settings_tablets_only)) {
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_embedded_activities,
                        supportingText = R.string.settings_support_embedded_activities,
                        checked = embeddedActivities,
                        onCheckedChange = onEmbeddedActivitiesChange
                    )

                    if (embeddedActivities) {
                        SettingsSectionDivider()

                        SettingsDetailSwitchItem(
                            headline = R.string.settings_split_placeholder,
                            supportingText = R.string.settings_support_split_placeholder,
                            checked = splitPlaceholder,
                            onCheckedChange = onPlaceholderChange
                        )
                    }

                    if (!splitPlaceholder) {
                        SettingsSectionDivider()

                        SettingsDetailSwitchItem(
                            headline = R.string.settings_dynamic_split,
                            supportingRawText = stringResource(R.string.settings_support_dynamic_split) +
                                if (!supportDynamicSplit)
                                    stringResource(R.string.settings_support_dynamic_split_not_avail)
                                else ""
                            ,
                            checked = dynamicSplit,
                            onCheckedChange = onDynamicSplitChange
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun rememberIsLargeDeviceByMaxMetrics(): Boolean {
    val context = LocalContext.current
    val density = LocalDensity.current

    return remember(context, density) {
        val wm = WindowMetricsCalculator.getOrCreate()
        val max = wm.computeMaximumWindowMetrics(context as Activity)  // “未被分屏限制”的最大窗口
        val widthPx = max.bounds.width()
        val widthDp = with(density) { widthPx / density.density }     // px -> dp
        widthDp >= 840
    }
}

@Composable
fun rememberSupportSplit(context: Context): Boolean {
    return remember { SplitController.getInstance(context).splitSupportStatus ==
            SplitController.SplitSupportStatus.SPLIT_AVAILABLE }
}

@Composable
fun rememberSupportDynamicSplit(): Boolean {
    return remember { WindowSdkExtensions.getInstance().extensionVersion >= 6 }
}