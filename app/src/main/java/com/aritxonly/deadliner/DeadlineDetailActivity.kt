package com.aritxonly.deadliner

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.color.DynamicColors
import okhttp3.internal.toHexString
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin

private lateinit var colorScheme: AppColorScheme

class DeadlineDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEADLINE = "com.aritxonly.deadliner.deadline"

        fun newIntent(context: Context, deadline: DDLItem): Intent {
            return Intent(context, DeadlineDetailActivity::class.java).apply {
                putExtra(EXTRA_DEADLINE, deadline)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deadline = intent.getParcelableExtra<DDLItem>(EXTRA_DEADLINE)
            ?: throw IllegalArgumentException("Missing Deadline parameter")

        val appColorScheme = intent.getParcelableExtra<AppColorScheme>("EXTRA_APP_COLOR_SCHEME")
            ?: throw IllegalArgumentException("Missing AppColorScheme")

        colorScheme = appColorScheme

        setSystemBarColors(colorScheme.surface, isLightColor(colorScheme.surface), colorScheme.surface)

        val databaseHelper = DatabaseHelper.getInstance(applicationContext)

        setContent {
            CustomDeadlinerTheme(
                appColorScheme = appColorScheme
            ) {
                var currentDeadline by remember { mutableStateOf(deadline) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeadlineDetailScreen(
                        deadline = currentDeadline,
                        onClose = { finish() },
                        onEdit = {
                            val editDialog = EditDDLFragment(currentDeadline) { updatedDDL ->
                                databaseHelper.updateDDL(updatedDDL)

                                currentDeadline = updatedDDL
                            }
                            editDialog.show(supportFragmentManager, "EditDDLFragment")
                        },
                        onToggleStar = {}
                    )
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
    deadline: DDLItem,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onToggleStar: () -> Unit
) {
    var waterLevel by remember { mutableStateOf(0f) }

    Scaffold(
        modifier = Modifier.background(Color(colorScheme.surface)),
        topBar = {
            TopAppBar(
                title = { Text(text = deadline.name, color = Color(colorScheme.onSurface)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(colorScheme.onSurface)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painterResource(id = R.drawable.ic_edit),
                            contentDescription = "编辑",
                            tint = Color(colorScheme.onSurface)
                        )
                    }
                    IconButton(onClick = onToggleStar) {
                        Icon(
                            painterResource(id = R.drawable.ic_star),
                            contentDescription = "星标",
                            tint = Color(colorScheme.onSurface)
                        )
                    }
                }
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
            )
            DeadlineDetailInfo(deadline, waterLevel)
        }
    }
}

@Composable
fun WaterCupAnimation(deadline: DDLItem,
                      onWaterLevelChange: (Float) -> Unit,
                      modifier: Modifier = Modifier) {
    // 模拟剩余时间对应的水位，取值 [0f, 1f]
    val startTime = GlobalUtils.parseDateTime(deadline.startTime)
    val endTime = GlobalUtils.parseDateTime(deadline.endTime)
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
    val infiniteTransition = rememberInfiniteTransition()
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val color = Color(colorScheme.primary)
    val waveAmplitude = with(LocalDensity.current) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(colorScheme.primaryContainer))
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
    val startTime = GlobalUtils.parseDateTime(deadline.startTime)
    val endTime = GlobalUtils.parseDateTime(deadline.endTime)
    val now = LocalDateTime.now()
    // 计算剩余时间
    val remainingDuration = Duration.between(now, endTime)
    val remainingTimeText = if (remainingDuration.isNegative) "已过期" else formatDuration(remainingDuration)
    // 格式化日期显示
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")

    /**
     * 使用插值法计算背景情况
     */
    val waterColor = Color(colorScheme.primary)
    val containerColor = Color(colorScheme.primaryContainer)
    val currentBackground = lerp(containerColor, waterColor, waterLevel.coerceIn(0f, 1f))

    val textColor = if (isSystemInDarkTheme()) {
        if (currentBackground.luminance() > 0.3f) {
            Color(colorScheme.onPrimary)
        } else {
            Color(colorScheme.onSurface)
        }
    } else {
        if (currentBackground.luminance() > 0.3f) {
            Color(colorScheme.onSurface)
        } else {
            Color(colorScheme.onPrimary)
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = deadline.name, style = MaterialTheme.typography.titleMedium, color = textColor)
        Text(text = "开始时间：${startTime.format(dateFormatter)}", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Text(text = "结束时间：${endTime.format(dateFormatter)}", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Text(text = "剩余时间：$remainingTimeText", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Text(text = deadline.note, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
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
        surface = Color(appColorScheme.surface)
    )
    MaterialTheme(
        colorScheme = customColors,
        typography = com.aritxonly.deadliner.ui.theme.Typography,
        content = content
    )
}