package com.aritxonly.deadliner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchVibration: MaterialSwitch
    private lateinit var switchProgressDir: MaterialSwitch
    private lateinit var switchProgressWidget: MaterialSwitch
    private lateinit var switchDeadlineNotification: MaterialSwitch
    private lateinit var switchDailyStatsNotification: MaterialSwitch
    private lateinit var switchMotivationalQuotes: MaterialSwitch
    private lateinit var buttonAuthorPage: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        DynamicColors.applyToActivityIfAvailable(this)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        // 初始化控件
        switchVibration = findViewById(R.id.switchVibration)
        switchProgressDir = findViewById(R.id.switchProgressDir)
        switchProgressWidget = findViewById(R.id.switchProgressWidget)
        switchDeadlineNotification = findViewById(R.id.switchDeadlineNotification)
        switchDailyStatsNotification = findViewById(R.id.switchDailyStatsNotification)
        switchMotivationalQuotes = findViewById(R.id.switchMotivationalQuotes)
        buttonAuthorPage = findViewById(R.id.buttonAuthorPage)

        // 加载保存的设置
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)

        switchVibration.isChecked = sharedPreferences.getBoolean("vibration", true)
        switchProgressDir.isChecked = sharedPreferences.getBoolean("main_progress_dir", false)
        switchProgressWidget.isChecked = sharedPreferences.getBoolean("widget_progress_dir", false)
        switchDeadlineNotification.isChecked = sharedPreferences.getBoolean("deadline_notification", false)
        switchDailyStatsNotification.isChecked = sharedPreferences.getBoolean("daily_stats_notification", false)
        switchMotivationalQuotes.isChecked = sharedPreferences.getBoolean("motivational_quotes", true)

        // 监听开关状态变化并保存设置
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("vibration", isChecked).apply()
        }

        switchProgressDir.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("main_progress_dir", isChecked).apply()
        }

        switchProgressWidget.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("widget_progress_dir", isChecked).apply()
        }

        switchDeadlineNotification.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("deadline_notification", isChecked).apply()
        }

        switchDailyStatsNotification.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("daily_stats_notification", isChecked).apply()
        }

        switchMotivationalQuotes.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("motivational_quotes", isChecked).apply()
        }

        // 设置超链接按钮点击事件
        buttonAuthorPage.setOnClickListener {
            val url = "https://github.com/AritxOnly"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finishAfterTransition() // 返回上一个界面
        }
    }

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean) {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = color
            navigationBarColor = color

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
     * 获取主题颜色
     * @param attributeId 主题属性 ID
     * @return 颜色值
     */
    private fun getThemeColor(attributeId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.data
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