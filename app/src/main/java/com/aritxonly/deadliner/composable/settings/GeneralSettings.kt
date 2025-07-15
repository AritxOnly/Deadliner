package com.aritxonly.deadliner.composable.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit
) {
    var hideFromRecent by remember { mutableStateOf(GlobalUtils.hideFromRecent) }
    val onHideFromRecentChange: (Boolean) -> Unit = {
        hideFromRecent = it
        GlobalUtils.hideFromRecent = it
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
                    contentDescription = "返回",
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

            SettingsSection(topLabel = "主要设置") {
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

            SettingsSection(topLabel = "杂项设置") {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_hide_from_recent,
                    supportingText = R.string.settings_support_hide_from_recent,
                    checked = hideFromRecent,
                    onCheckedChange = onHideFromRecentChange
                )
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}