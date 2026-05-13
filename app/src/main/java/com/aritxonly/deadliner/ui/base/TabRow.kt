package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.basic.TabRowDefaults

// 别名防止冲突
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow

/**
 * Deadliner 基础 TabRow 组件
 * 采用数据驱动的 API 设计，抹平 M3 插槽和 MIUIX 列表的差异
 */
@Composable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // M3 专属：传入对应的图标列表。如果不传或切到 MIUIX，就会自动忽略
    tabIcons: List<Painter>? = null,
    // M3 专属：底部分割线
    divider: @Composable () -> Unit = {}
) {
    when (LocalAppDesignSystem.current) {

        AppDesignSystem.MATERIAL3 -> {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                divider = divider,
                containerColor = Color.Transparent
            ) {
                // 在 M3 分支内部，我们帮你把 forEach 循环写掉
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = index == selectedTabIndex,
                        onClick = { onTabSelected(index) },
                        icon = tabIcons?.getOrNull(index)?.let { painter ->
                            { Icon(painter, contentDescription = title) }
                        },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        AppDesignSystem.MIUIX -> {
            MiuixTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                modifier = modifier.padding(horizontal = 8.dp),
                colors = TabRowDefaults.tabRowColors(backgroundColor = Color.Transparent)
            )
        }
    }
}