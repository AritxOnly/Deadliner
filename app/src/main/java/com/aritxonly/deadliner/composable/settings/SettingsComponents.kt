package com.aritxonly.deadliner.composable.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Label
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.theme.Typography

// region: These codes are referenced from https://github.com/YangDai2003/OpenNote-Compose/
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingItem(
    headlineText: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    expressiveTypeModifier: Modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
            modifier = expressiveTypeModifier
                .padding(start = 2.dp),
            contentDescription = null
        )
    },
    colors: ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation
) = ListItem(
    modifier = modifier,
    headlineContent = {
        Text(
            text = headlineText,
            maxLines = 1,
            style = MaterialTheme.typography.titleMediumEmphasized
        )
    },
    supportingContent = {
        Text(
            text = supportingText,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    colors = colors,
    tonalElevation = tonalElevation,
    shadowElevation = shadowElevation
)

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    topLabel: String? = null,
    mainContent: Boolean = false,
    enabled: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val radiusDimen = if (mainContent) 48.dp else dimensionResource(R.dimen.item_corner_radius)
    val containerColor = if (enabled && mainContent)
            MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        topLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large.copy(CornerSize(radiusDimen)),
            color = containerColor.copy(alpha = 0.6f)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsSectionDivider() = HorizontalDivider(
    thickness = if (!GlobalUtils.hideDividerUi) 2.dp else 0.dp,
    color = MaterialTheme.colorScheme.surface
)
// endregion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollapsingTopBarScaffold(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    // 1. 统一管理滚动状态
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = titleColor,
                ),
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = snackbarHost,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSwitchItem(
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    mainSwitch: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (!mainSwitch || !checked)
                    MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_on),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsDetailSwitchItem(
    @StringRes headline: Int,
    @StringRes supportingText: Int,
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: (Boolean) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onCheckedChange(!checked) },
                    onLongClick = { onLongPress(!checked) }
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(headline),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(supportingText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = if (checked) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_on),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsDetailSwitchItem(
    @StringRes headline: Int,
    supportingRawText: String,
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: (Boolean) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onCheckedChange(!checked) },
                    onLongClick = { onLongPress(checked) }
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(headline),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = supportingRawText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = if (checked) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_on),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSliderItem(
    @StringRes label: Int,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            // 整个 Slider 区块放到一个 Box 里
            Box(Modifier.fillMaxWidth()) {
                Slider(
                    value = value,
                    onValueChange = {
                        dragging = true
                        onValueChange(it)
                    },
                    onValueChangeFinished = {
                        dragging = false
                    },
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSliderItemWithLabel(
    @StringRes label: Int,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    // 复用一个 interactionSource 给 Slider 和 Label 共享
    val interactionSource = remember { MutableInteractionSource() }
    // 自定义 thumb + track 颜色
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
    )
    var sliderPosition by remember { mutableStateOf(value) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    onValueChange(it)
                },
                valueRange = valueRange,
                steps = steps,
                interactionSource = interactionSource,
                thumb = {
                    Label(
                        label = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sliderPosition.toInt().toString(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
                                )
                            }
                        },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .padding(bottom = 4.dp)    // label 与 thumb 之间留点间隙
                    ) {
                        // 底层实际显示的 thumb
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            colors = sliderColors
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTextButtonItem(
    modifier: Modifier = Modifier,
    @StringRes text: Int,
    @DrawableRes iconRes: Int? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailTextButtonItem(
    modifier: Modifier = Modifier,
    @StringRes headline: Int,
    @StringRes supporting: Int,
    @DrawableRes iconRes: Int? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(headline),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class RadioOption<T>(
    val key: T,
    @StringRes val labelRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsRadioGroupItem(
    options: List<RadioOption<T>>,
    selectedKey: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    divider: @Composable () -> Unit = { SettingsSectionDivider() }
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option.key) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    RadioButton(
                        selected = option.key == selectedKey,
                        onClick = { onOptionSelected(option.key) },
                        colors = RadioButtonDefaults.colors(
                            // 透明背景 already on Card
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                if (showDivider && index < options.lastIndex) {
                    divider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsRadioGroupItem() {
    val opts = listOf(
        RadioOption("system", R.string.settings_vibration_system),
        RadioOption("custom", R.string.settings_vibration_custom)
    )
    var selected by remember { mutableStateOf("system") }

    SettingsRadioGroupItem(
        options = opts,
        selectedKey = selected,
        onOptionSelected = { selected = it }
    )
}