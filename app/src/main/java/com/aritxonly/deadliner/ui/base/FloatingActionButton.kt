package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

import androidx.compose.material3.FloatingActionButton as Material3Fab
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFab

private val DeadlinerFabShape = RoundedCornerShape(20.dp)

/**
 * 应用级 FAB：统一 Material3 / MIUIX 两套实现。
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    forceMaterial3: Boolean = false,
    content: @Composable () -> Unit,
) {
    when {
        forceMaterial3 || LocalAppDesignSystem.current == AppDesignSystem.MATERIAL3 -> {
            Material3Fab(
                onClick = onClick,
                modifier = modifier,
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = DeadlinerFabShape,
                content = content,
            )
        }

        else -> {
            MiuixFab(
                onClick = onClick,
                modifier = modifier.clip(DeadlinerFabShape),
                content = content,
            )
        }
    }
}
