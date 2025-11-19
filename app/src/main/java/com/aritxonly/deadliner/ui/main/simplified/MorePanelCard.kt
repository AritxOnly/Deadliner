package com.aritxonly.deadliner.ui.main.simplified

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aritxonly.deadliner.ArchiveActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import java.time.LocalTime

@Composable
fun MorePanelCard(
    onDismiss: () -> Unit,
    avatarPainter: Painter?,
    nickname: String,
    modifier: Modifier = Modifier,
    activity: MainActivity
) {
    val context = LocalContext.current
    var showProfileEditor by remember { mutableStateOf(false) }

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarPainter != null) {
                Image(
                    painter = avatarPainter,
                    contentDescription = stringResource(R.string.avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                RotatingGreeting(nickname)
                Text(stringResource(R.string.panel_greeting_deadliner), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) {
                Icon(ImageVector.vectorResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close), modifier = expressiveTypeModifier)
            }
        }

        Divider()

        // 功能列表
        Column(Modifier.padding(vertical = 4.dp)) {
            MoreRow(R.drawable.ic_settings, stringResource(R.string.settings_title)) {
                activity.startActivity(Intent(context, SettingsActivity::class.java))
            }
            MoreRow(R.drawable.ic_archive, stringResource(R.string.archive)) {
                val intent = Intent(activity, ArchiveActivity::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val options = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
                    activity.startActivity(intent, options)
                } else {
                    activity.startActivity(intent)
                }
            }
            MoreRow(R.drawable.ic_chart, stringResource(R.string.overview)) {
                activity.startActivity(Intent(context, OverviewActivity::class.java))
            }
            MoreRow(R.drawable.ic_edit, stringResource(R.string.edit_profile)) {
                showProfileEditor = true
            }
        }
    }

    if (showProfileEditor) {
        Dialog(
            onDismissRequest = { showProfileEditor = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))

                    ProfileEditor(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreRow(@DrawableRes icon: Int, title: String, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        leadingContent = {
            Icon(ImageVector.vectorResource(icon), contentDescription = null)
        },
        headlineContent = { Text(title) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    )
}