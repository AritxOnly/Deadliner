package com.aritxonly.deadliner.composable.settings

import ShapeShowcase
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.localutils.GlobalUtils


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    nav: NavController,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_title),
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding),
        ) {
//            item { ShapeShowcase() }

            SettingsRoute.allSubRoutes.forEach { group ->
                if (!(group.contains(SettingsRoute.Lab) && !GlobalUtils.developerMode)) {
                    item {
                        SettingsSection {
                            group.forEachIndexed { index, route ->
                                val supportText = (if (route.route == "about") "v${context.getAppVersion()} " else "") +
                                        stringResource(route.supportRes!!)
                                SettingItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { nav.navigate(route.route) },
                                    headlineText = stringResource(route.titleRes),
                                    supportingText = supportText,
                                    leadingContent = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                route.iconRes ?: R.drawable.ic_package
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                )

                                if (index != group.lastIndex) {
                                    SettingsSectionDivider()
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

fun Context.getAppVersion(): String {
    return try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        pInfo.versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}