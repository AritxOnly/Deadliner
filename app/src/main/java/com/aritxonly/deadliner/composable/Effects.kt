package com.aritxonly.deadliner.composable

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TintedGradientImage(
    @DrawableRes drawableId: Int,
    tintColor: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(drawableId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.tint(
            tintColor,
            blendMode = BlendMode.Multiply
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvgCard(
    @DrawableRes svgRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = svgRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp)
    ) {
        content()
    }
}

@Composable
fun AnimatedItem(
    delayMillis: Long = 0,
    content: @Composable () -> Unit
) {
    // 位移动画，从 50px 高度慢慢弹到 0
    val offsetY = remember { Animatable(50f) }
    // 透明度动画，从 0f 到 1f
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 延迟让每个 item 阶梯式入场
        delay(delayMillis)
        // 并发执行位移 + 淡入
        launch { offsetY.animateTo(0f, tween(500, easing = EaseOutCubic)) }
        launch { alpha.animateTo(1f, tween(400)) }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            translationY = offsetY.value
            this.alpha = alpha.value
        }
    ) {
        content()
    }
}