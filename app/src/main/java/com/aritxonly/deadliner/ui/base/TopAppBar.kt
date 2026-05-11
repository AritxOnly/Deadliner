package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import androidx.compose.material3.TopAppBar as MaterialTopAppBar

enum class TopAppBarStyle {
    CENTER, LARGE, SMALL
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun TopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    isMainTitle: Boolean = false,
    titleTextStyle: TextStyle? = null,
) {
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
    )

    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            when (mode) {
                TopAppBarStyle.CENTER ->
                    CenterAlignedTopAppBar(
                        title = {
                            if (titleTextStyle != null) {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = titleTextStyle,
                                )
                            } else {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = topBarColors,
                    )

                TopAppBarStyle.LARGE ->
                    LargeTopAppBar(
                        title = {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = topBarColors,
                    )

                else ->
                    if (!isMainTitle) {
                        MaterialTopAppBar(
                            title = {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = titleTextStyle ?: MaterialTheme.typography.headlineMedium
                                )
                            },
                            navigationIcon = { navigationIcon?.invoke() },
                            actions = actions,
                            colors = topBarColors,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        MaterialTopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = if (subtitle != null)
                                            MaterialTheme.typography.headlineSmallEmphasized
                                        else MaterialTheme.typography.headlineMediumEmphasized
                                    )

                                    if (subtitle != null) {
                                        Text(
                                            text = subtitle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            },
                            actions = {
                                navigationIcon?.invoke()
                                actions()
                            },
                            colors = topBarColors,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
            }
        }

        AppDesignSystem.MIUIX -> {
            when (mode) {
                TopAppBarStyle.LARGE ->
                    MiuixTopAppBar(
                        title = title,
                        color = MaterialTheme.colorScheme.surface,
                        navigationIcon = navigationIcon ?: {},
                        actions = actions,
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        largeTitleColor = MaterialTheme.colorScheme.onSurface,
                    )

                else ->
                    SmallTopAppBar(
                        title = title,
                        color = MaterialTheme.colorScheme.surface,
                        navigationIcon = navigationIcon ?: {},
                        actions = actions,
                        titleColor = MaterialTheme.colorScheme.onSurface,
                    )
            }
        }
    }
}
