package com.aritxonly.deadliner.composable.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlappingWaves(
    modifier: Modifier = Modifier,
    waveRadius: Dp = 60.dp,
    offset1: Offset = Offset(150f, 150f),
    offset2: Offset = Offset(180f, 180f),
    rotationDegrees: Float = -45f
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    // Convert Dp radius to pixels
    val radiusPx = with(LocalDensity.current) { waveRadius.toPx() }

    Canvas(modifier = modifier) {
        // Apply a global rotation about the center
        rotate(rotationDegrees, pivot = center) {
            // 1) Draw both waves in primaryContainer
            drawCircle(
                color = primaryContainer,
                radius = radiusPx,
                center = offset1
            )
            drawCircle(
                color = primaryContainer,
                radius = radiusPx,
                center = offset2
            )

            // 2) Compute the wave2 path (the clipping shape)
            val wave2Path = Path().apply {
                addOval(
                    Rect(
                        offset = offset2 - Offset(radiusPx, radiusPx),
                        size = Size(radiusPx * 2f, radiusPx * 2f)
                    )
                )
            }

            // 3) Clip to wave2 and draw wave1 again in primary
            clipPath(wave2Path) {
                drawCircle(
                    color = primary,
                    radius = radiusPx,
                    center = offset1
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOverlappingWaves() {
    // Give it a background so we can see it
    Box(
        Modifier
            .size(200.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        OverlappingWaves(modifier = Modifier.fillMaxSize())
    }
}