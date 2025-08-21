package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import okhttp3.internal.toHexString
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

private lateinit var colorScheme: AppColorScheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class DeadlineDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEADLINE = "com.aritxonly.deadliner.deadline"

        fun newIntent(context: Context, deadline: DDLItem): Intent {
            return Intent(context, DeadlineDetailActivity::class.java).apply {
                putExtra(EXTRA_DEADLINE, deadline)
            }
        }
    }

    private class ColorSchemeHelper(val context: Context) {
        val defaultColorScheme = AppColorScheme(
            primary = getThemeColor(androidx.appcompat.R.attr.colorPrimary),
            onPrimary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnPrimary),
            primaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimaryContainer),
            surface = getMaterialThemeColor(com.google.android.material.R.attr.colorSurface),
            onSurface = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSurface),
            surfaceContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer),
            secondary = getMaterialThemeColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSecondary),
            secondaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSecondaryContainer),
            onSecondaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer),
            tertiary = getMaterialThemeColor(com.google.android.material.R.attr.colorTertiary),
            onTertiary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnTertiary),
            tertiaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorTertiaryContainer),
            onTertiaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorOnTertiaryContainer),
        )

        /**
         * 获取主题颜色
         * @param attributeId 主题属性 ID
         * @return 颜色值
         */
        private fun getThemeColor(attributeId: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attributeId, typedValue, true)
            Log.d("ThemeColor", "getColor $attributeId: ${typedValue.data.toHexString()}")
            return typedValue.data
        }

        private fun getMaterialThemeColor(attributeId: Int): Int {
            return MaterialColors.getColor(
                ContextWrapper(context),
                attributeId,
                android.graphics.Color.WHITE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        val initialDeadline = intent.getParcelableExtra<DDLItem>(EXTRA_DEADLINE)
        ?: throw IllegalArgumentException("Missing Deadline parameter")

        val appColorScheme = intent.getParcelableExtra<AppColorScheme>("EXTRA_APP_COLOR_SCHEME")
            ?: ColorSchemeHelper(this).defaultColorScheme

        colorScheme = appColorScheme

        setSystemBarColors(colorScheme.surface, isLightColor(colorScheme.surface), colorScheme.surface)

        val latestDeadline = DDLRepository().getDDLById(initialDeadline.id) ?: initialDeadline

        setContent {
            CustomDeadlinerTheme(
                appColorScheme = appColorScheme
            ) {
                var currentDeadline by remember { mutableStateOf(latestDeadline) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(colorScheme.surface))
                ) { innerPadding ->
                    DeadlineDetailScreen(
                        applicationContext = applicationContext,
                        context = this,
                        deadline = currentDeadline,
                        onClose = { finish() },
                        onEdit = {
                            val editDialog = EditDDLFragment(currentDeadline) { updatedDDL ->
                                DDLRepository().updateDDL(updatedDDL)

                                currentDeadline = updatedDDL
                            }
                            editDialog.show(supportFragmentManager, "EditDDLFragment")
                        },
                        onToggleStar = { isStared ->
                            currentDeadline.isStared = isStared
                            DDLRepository().updateDDL(currentDeadline)
                            currentDeadline = DDLRepository().getDDLById(currentDeadline.id) ?: currentDeadline
                        }
                    ) {
                        finishAfterTransition()
                    }
                }
            }
        }
    }

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean, colorNavigationBar: Int) {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = color
            navigationBarColor = colorNavigationBar

            // 设置状态栏图标颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.setSystemBarsAppearance(
                    if (lightIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                insetsController?.setSystemBarsAppearance(
                    if (lightIcons) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = if (lightIcons) {
                    decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }

    /**
     * 判断颜色是否为浅色
     */
    private fun isLightColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * ((color shr 16 and 0xFF) / 255.0) +
                0.587 * ((color shr 8 and 0xFF) / 255.0) +
                0.114 * ((color and 0xFF) / 255.0))
        return darkness < 0.5
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class)
@Composable
fun DeadlineDetailScreen(
    applicationContext: Context,
    context: Context,
    deadline: DDLItem,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onToggleStar: (Boolean) -> Unit,
    finishActivity: () -> Unit
) {
    var waterLevel by remember { mutableFloatStateOf(0f) }
    var isStared by remember { mutableStateOf(deadline.isStared) }
    var isCompleted by remember { mutableStateOf(deadline.isCompleted) }

    Log.d("DetailPage", "DeadlineDetailScreen: $isStared")

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(Color(colorScheme.surfaceContainer), CircleShape)
        .padding(8.dp)

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.background(Color(colorScheme.surface)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = deadline.name,
                        color = Color(colorScheme.onSurface),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = Color(colorScheme.onSurface),
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isStared = !isStared
                        onToggleStar(isStared)
                    }) {
                        val iconStar = painterResource(
                            if (isStared) R.drawable.ic_star_filled
                            else R.drawable.ic_star
                        )
                        val tintColor = if (isStared)
                            Color("ffffe819".hexToInt())
                        else Color(colorScheme.onSurface)
                        Icon(
                            iconStar,
                            contentDescription = "星标",
                            tint = tintColor,
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                modifier = Modifier
                    .background(Color(colorScheme.surface))
                    .padding(horizontal = 8.dp)
            )
        }
    ) { innerPadding ->
        // 内容区域
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(colorScheme.surface)),
            contentAlignment = Alignment.Center
        ) {
            // 模拟一个水杯容器
            WaterCupAnimation(
                deadline,
                onWaterLevelChange = { level -> waterLevel = level },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(0.6f)
                    .padding(bottom = 32.dp)
            )
            DeadlineDetailInfo(deadline, waterLevel)

            DeadlineEditFABMenu(
                isCompleted = isCompleted,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) { desc ->
                Log.d("Desc", desc)
                when (desc) {
                    "编辑" -> onEdit()
                    "完成", "标记完成", "撤销完成" -> {
                        deadline.isCompleted = !deadline.isCompleted
                        if (deadline.isCompleted) {
                            Toast.makeText(context, R.string.toast_finished, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, R.string.toast_definished, Toast.LENGTH_SHORT).show()
                        }
                        DDLRepository().updateDDL(deadline)
                    }
                    "归档" -> {
                        deadline.isArchived = true
                        Toast.makeText(
                            context,
                            R.string.toast_archived,
                            Toast.LENGTH_SHORT
                        ).show()
                        finishActivity()
                    }
                    "删除" -> {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.alert_delete_title)
                            .setMessage(R.string.alert_delete_message)
                            .setNegativeButton(R.string.cancel) { _, _ ->

                            }.setPositiveButton(R.string.accept) { dialog, _ ->
                                DDLRepository().deleteDDL(deadline.id)
                                DeadlineAlarmScheduler.cancelAlarm(applicationContext, deadline.id)
                                Toast.makeText(context, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                                finishActivity()
                            }
                            .show()

                    }
                    "保存至日历" -> {
                        val calendarHelper = CalendarHelper(applicationContext)

                        coroutineScope.launch {
                            try {
                                val eventId = calendarHelper.insertEvent(deadline)
                                deadline.calendarEventId = eventId
                                DDLRepository().updateDDL(deadline)
                                Toast.makeText(context, "已添加至日历", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("Calendar", e.toString())
                                Toast.makeText(context, "添加至日历失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlineEditFABMenu(
    isCompleted: Boolean,
    modifier: Modifier,
    callback: (desc: String) -> Unit
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    var items = listOf(
        painterResource(R.drawable.ic_edit) to "编辑",
        painterResource(R.drawable.ic_done) to "标记完成",
        painterResource(R.drawable.ic_archiving) to "归档",
        painterResource(R.drawable.ic_delete) to "删除",
        painterResource(R.drawable.ic_event) to "保存至日历"
    )

    FloatingActionButtonMenu(
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier
                    .semantics {
                        traversalIndex = -1f
                        stateDescription = if (fabMenuExpanded) "展开" else "收起"
                        contentDescription = "编辑菜单"
                    }
                    .animateFloatingActionButton(
                        visible = true,
                        alignment = Alignment.BottomEnd
                    ),
                containerColor = { x: Float ->
                    if (x > 0.5f)
                        Color(colorScheme.tertiary!!)
                    else
                        Color(colorScheme.tertiaryContainer!!)
                },
                checked = fabMenuExpanded,
                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
            ) {
                val closeVector = ImageVector.vectorResource(R.drawable.ic_close)
                val editVector = ImageVector.vectorResource(R.drawable.ic_edit)
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) closeVector
                        else editVector
                    }
                }

                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }, color = { progress ->
                        if (progress > 0.5f)
                            Color(colorScheme.onTertiary!!)
                        else
                            Color(colorScheme.onTertiaryContainer!!)
                    }),
                )
            }
        },
        modifier = modifier
    ) {
        items.forEachIndexed { i, item ->
            FloatingActionButtonMenuItem(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    if (i == items.size - 1) {
                        customActions = listOf(
                            CustomAccessibilityAction(
                                label = "关闭菜单",
                                action = {
                                    fabMenuExpanded = false
                                    true
                                }
                            )
                        )
                    }
                },
                onClick =  {
                    callback(item.second)
                    fabMenuExpanded = false
                },
                icon = { Icon(item.first, contentDescription = null) },
                text = { Text(text = item.second) },
                containerColor = Color(colorScheme.tertiaryContainer!!),
                contentColor = Color(colorScheme.onTertiaryContainer!!)
            )
        }
    }
}

@Composable
fun WaterCupAnimation(deadline: DDLItem,
                      onWaterLevelChange: (Float) -> Unit,
                      modifier: Modifier = Modifier) {
    // 模拟剩余时间对应的水位，取值 [0f, 1f]
    val startTime = GlobalUtils.safeParseDateTime(deadline.startTime)
    val endTime = GlobalUtils.safeParseDateTime(deadline.endTime)
    val now = LocalDateTime.now()
    val totalDuration = Duration.between(startTime, endTime)
    val remainingDuration = Duration.between(now, endTime)
    val targetWaterLevel = remainingDuration.toMillis().toFloat() / totalDuration.toMillis().toFloat()
    val animatedWaterLevel by animateFloatAsState(
        targetValue = targetWaterLevel,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
    )

    onWaterLevelChange(animatedWaterLevel)

    // 用于波浪动画，创建一个无限循环的动画变量，用于控制波浪水平位移（相位）
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    val color = if (deadline.isCompleted) Color(colorScheme.secondary!!)
        else Color(colorScheme.primary)
    val waveAmplitude = with(LocalDensity.current) { 8.dp.toPx() }

    val containerColor = if (deadline.isCompleted) Color(colorScheme.secondaryContainer!!)
        else Color(colorScheme.primaryContainer)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(containerColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            // 根据动画进度计算水位高度
            val waterHeight = height * animatedWaterLevel

            // 创建波浪路径
            val path = Path().apply {
                // 从左下角开始
                moveTo(0f, height)
                // 到达水面起始位置
                lineTo(0f, height - waterHeight)

                // 设置波长：这里我们使用 width / 1.5f 作为一个示例
                val waveLength = width / 1.5f
                // 绘制波浪，步长为5像素
                var x = 0f
                while (x <= width) {
                    // 使用正弦函数计算 y 坐标
                    val angle = (2 * PI * (x / waveLength)).toFloat() + wavePhase
                    val y = (sin(angle) * waveAmplitude) + (height - waterHeight)
                    lineTo(x, y)
                    x += 5f
                }
                // 画到右下角
                lineTo(width, height)
                close()
            }

            // 绘制波浪路径
            drawPath(path, color = color)
        }
    }
}

// 辅助函数：格式化 Duration（例如：2天 3小时 15分）
fun formatDuration(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    return if (days > 0) "$days 天 $hours 小时 $minutes 分" else "$hours 小时 $minutes 分"
}

@Composable
fun DeadlineDetailInfo(deadline: DDLItem, waterLevel: Float) {
    // 将字符串时间解析为 LocalDateTime
    val startTime = GlobalUtils.safeParseDateTime(deadline.startTime)
    val endTime = GlobalUtils.safeParseDateTime(deadline.endTime)
    val now = LocalDateTime.now()
    // 计算剩余时间
    val remainingDuration = Duration.between(now, endTime)
    val remainingTimeText = if (remainingDuration.isNegative) "已过期" else formatDuration(remainingDuration)
    // 格式化日期显示
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")

    /**
     * 使用插值法计算背景情况
     */
    val waterColor = if (deadline.isCompleted) Color(colorScheme.secondary!!)
        else Color(colorScheme.primary)
    val containerColor = if (deadline.isCompleted) Color(colorScheme.secondaryContainer!!)
        else Color(colorScheme.primaryContainer)
    val currentBackground = lerp(containerColor, waterColor, waterLevel.coerceIn(0f, 1f))

    val textColor = remember(currentBackground) {
        selectOptimalTextColor(
            backgroundColor = currentBackground,
            lightColor = Color(colorScheme.onPrimary),
            darkColor = Color(colorScheme.onSurface)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = deadline.name, style = MaterialTheme.typography.headlineLarge, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "开始时间：${startTime.format(dateFormatter)}", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Text(text = "结束时间：${endTime.format(dateFormatter)}", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Text(text = "剩余时间：$remainingTimeText", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        SelectionContainer {
            Text(text = deadline.note, style = MaterialTheme.typography.bodyMedium, color = textColor)
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

/**
 * WCAG 2.1对比度优化算法
 */
fun selectOptimalTextColor(
    backgroundColor: Color,
    lightColor: Color,
    darkColor: Color
): Color {
    // 计算背景亮度（标准化到0-1）
    val bgLuminance = backgroundColor.luminance()

    // 计算两种候选颜色的对比度
    val lightContrast = calculateContrastRatio(bgLuminance, lightColor.luminance())
    val darkContrast = calculateContrastRatio(bgLuminance, darkColor.luminance())

    // 选择对比度更高的颜色（至少满足AA标准4.5:1）
    return when {
        lightContrast >= 4.5f && lightContrast > darkContrast -> lightColor
        darkContrast >= 4.5f -> darkColor
        else -> if (bgLuminance > 0.5f) darkColor else lightColor // 降级方案
    }
}

/**
 * 根据WCAG公式计算对比度比率
 */
fun calculateContrastRatio(backgroundLuminance: Float, textLuminance: Float): Float {
    val l1 = maxOf(backgroundLuminance, textLuminance) + 0.05f
    val l2 = minOf(backgroundLuminance, textLuminance) + 0.05f
    return l1 / l2
}

@Composable
fun CustomDeadlinerTheme(
    appColorScheme: AppColorScheme,
    content: @Composable () -> Unit
) {
    val customColors = lightColorScheme(
        primary = Color(appColorScheme.primary),
        onPrimary = Color(appColorScheme.onPrimary),
        primaryContainer = Color(appColorScheme.primaryContainer),
        surface = Color(appColorScheme.surface),
        onSurface = Color(appColorScheme.onSurface),
        surfaceContainer = Color(appColorScheme.surfaceContainer)
    )
    MaterialTheme(
        colorScheme = customColors,
        typography = com.aritxonly.deadliner.ui.theme.Typography,
        content = content
    )
}