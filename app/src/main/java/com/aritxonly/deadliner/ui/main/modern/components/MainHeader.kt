package com.aritxonly.deadliner.ui.main.modern.components

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ArchiveActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.main.TextPageIndicator
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

@Composable
fun ModernMainHeader(
    activity: MainActivity,
    selectedPage: DeadlineType,
    onSelectedPageChange: (DeadlineType) -> Unit,
    avatarPainter: Painter?,
    onShowAiOverlay: () -> Unit,
    showPageTabs: Boolean = true,
    showAccessoryRow: Boolean = true,
    showAiActionInTopBar: Boolean = false,
    forceMaterialTopAppBar: Boolean = false,
) {
    val pageLabel = when (selectedPage) {
        DeadlineType.TASK -> stringResource(R.string.task)
        DeadlineType.HABIT -> stringResource(R.string.habit)
    }
    val tabsTrackColor = if (LocalAppDesignSystem.current == AppDesignSystem.MIUIX) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Column {
        TopAppBar(
            title = "Deadliner",
            navigationIcon = {
                if (showAiActionInTopBar) {
                    FilledTonalButton(
                        onClick = onShowAiOverlay,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_lifi),
                                contentDescription = stringResource(R.string.ai_quick_add),
                                modifier = Modifier.size(18.dp),
                            )
                            Text("问 AI")
                        }
                    }
                }

                IconButton(onClick = {
                    val intent = Intent(activity, ArchiveActivity::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val options =
                            ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
                        activity.startActivity(intent, options)
                    } else {
                        activity.startActivity(intent)
                    }
                }) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_archive),
                        contentDescription = stringResource(R.string.archive),
                        modifier = expressiveTypeModifier
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                }) {
                    if (avatarPainter != null) {
                        Image(
                            painter = avatarPainter,
                            contentDescription = stringResource(R.string.user),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                            modifier = expressiveTypeModifier
                        )
                    }
                }
            },
            mode = TopAppBarStyle.SMALL,
            isMainTitle = true,
            forceMaterial3 = forceMaterialTopAppBar,
        )

        AnimatedVisibility(
            visible = showAccessoryRow,
            enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight / 3 },
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ),
            exit = fadeOut(animationSpec = tween(120)) +
                shrinkVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    targetOffsetY = { fullHeight -> -fullHeight / 3 },
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                ),
            label = "modern-main-header-accessories",
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showPageTabs) {
                    Surface(
                        color = tabsTrackColor,
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            TextPageIndicator(
                                text = stringResource(R.string.task),
                                onClick = { onSelectedPageChange(DeadlineType.TASK) },
                                selected = selectedPage.toString(),
                                tag = DeadlineType.TASK.toString(),
                                badgeConfig = Triple(false, 0, false),
                            )
                            TextPageIndicator(
                                text = stringResource(R.string.habit),
                                onClick = { onSelectedPageChange(DeadlineType.HABIT) },
                                selected = selectedPage.toString(),
                                tag = DeadlineType.HABIT.toString(),
                                badgeConfig = Triple(false, 0, false),
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = pageLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!showAiActionInTopBar) {
                    FilledTonalButton(
                        onClick = onShowAiOverlay,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_lifi),
                                contentDescription = stringResource(R.string.ai_quick_add),
                                modifier = Modifier.size(18.dp),
                            )
                            Text("问 AI")
                        }
                    }
                }
            }
        }
    }
}
