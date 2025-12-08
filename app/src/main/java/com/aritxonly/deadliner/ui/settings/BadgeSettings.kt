package com.aritxonly.deadliner.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.PreviewCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@Composable
fun BadgeSettingsScreen(
    navigateUp: () -> Unit
) {
    var nearbyTasksBadgeEnabled by remember { mutableStateOf(GlobalUtils.nearbyTasksBadge) }

    val onNearbyTasksBadgeChange: (Boolean) -> Unit = {
        GlobalUtils.nearbyTasksBadge = it
        nearbyTasksBadgeEnabled = it
    }

    val badgeOpts = listOf(
        RadioOption("number", R.string.settings_tasks_badge_number),
        RadioOption("dot", R.string.settings_tasks_badge_dot)
    )
    val badgeRightNow = if (GlobalUtils.nearbyDetailedBadge) "number" else "dot"
    var badgeModeSelected by remember { mutableStateOf(badgeRightNow) }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)
    
    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_tasks_badge_title),
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
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(
                mainContent = true,
                enabled = nearbyTasksBadgeEnabled
            ) {
                SettingsSwitchItem(
                    label = R.string.settings_tasks_badge,
                    checked = nearbyTasksBadgeEnabled,
                    onCheckedChange = onNearbyTasksBadgeChange,
                    mainSwitch = true
                )
            }

            PreviewCard(modifier = Modifier.padding(16.dp)) {
                Box(Modifier.padding(top = 36.dp)) {
                    if (nearbyTasksBadgeEnabled) {
                        if (badgeModeSelected == "number") {
                            CombinedBadgePreview(
                                badgeRes = R.drawable.badge_number,
                                phoneRes = R.drawable.badge_phone,
                                titleRes = R.drawable.badge_title,
                                containerRes = R.drawable.badge_container,
                                bgRes = R.drawable.badge_bg
                            )
                        } else {
                            CombinedBadgePreview(
                                badgeRes = R.drawable.badge_dot,
                                phoneRes = R.drawable.badge_phone,
                                titleRes = R.drawable.badge_title,
                                containerRes = R.drawable.badge_container,
                                bgRes = R.drawable.badge_bg
                            )
                        }
                    } else {
                        CombinedBadgePreview(
                            badgeRes = null,
                            phoneRes = R.drawable.badge_phone,
                            titleRes = R.drawable.badge_title,
                            containerRes = R.drawable.badge_container,
                            bgRes = R.drawable.badge_bg
                        )
                    }
                }
            }

            if (nearbyTasksBadgeEnabled) {
                SettingsSection {
                    SettingsRadioGroupItem(
                        options = badgeOpts,
                        selectedKey = badgeModeSelected,
                        onOptionSelected = {
                            badgeModeSelected = it
                            GlobalUtils.nearbyDetailedBadge = badgeModeSelected == "number"
                        },
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedBadgePreview(
    @DrawableRes badgeRes: Int?,
    @DrawableRes phoneRes: Int,
    @DrawableRes titleRes: Int,
    @DrawableRes containerRes: Int,
    @DrawableRes bgRes: Int,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val invertMatrix = ColorMatrix(
        floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
            0f, -1f,  0f, 0f, 255f,
            0f,  0f, -1f, 0f, 255f,
            0f,  0f,  0f, 1f,   0f
        )
    )

    Box(modifier = modifier) {
        // 背景：铺满且适配，不裁切
        Image(
            painter = painterResource(bgRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = if (isDark) ColorFilter.colorMatrix(invertMatrix) else null
        )

        // container：铺满且适配，不裁切
        Image(
            painter = painterResource(containerRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.errorContainer)
        )

        // phone：铺满且适配
        Image(
            painter = painterResource(phoneRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // title：铺满且适配
        Image(
            painter = painterResource(titleRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )

        badgeRes?.let {
            Image(
                painter = painterResource(badgeRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
            )
        }
    }
}