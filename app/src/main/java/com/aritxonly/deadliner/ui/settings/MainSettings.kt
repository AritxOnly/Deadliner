package com.aritxonly.deadliner.ui.settings

import android.graphics.BitmapFactory
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import java.io.File
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    nav: NavController,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val profile by UserProfileRepository.profile.collectAsState(initial = UserProfile())
    val avatarPainter: Painter? by remember(profile.avatarFileName) {
        mutableStateOf(
            profile.avatarFileName?.let { name ->
                val file = File(context.filesDir, "avatars/$name")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapPainter(it.asImageBitmap()) } else null
            }
        )
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_title),
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = navIconPaddingModifier
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
        SettingsLazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier,
        ) {
            item {
                SettingsSection {
                    SettingItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(SettingsRoute.Profile.route) },
                        headlineText = if (profile.nickname.isBlank()) stringResource(R.string.edit_profile) else profile.nickname,
                        supportingText = stringResource(R.string.settings_support_profile),
                        leadingContent = {
                            if (avatarPainter != null) {
                                Image(
                                    painter = avatarPainter!!,
                                    contentDescription = stringResource(R.string.avatar),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_person),
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }

//            item { ShapeShowcase() }

            SettingsRoute.allSubRoutes.forEach { group ->
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
