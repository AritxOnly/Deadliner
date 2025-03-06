package com.aritxonly.deadliner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.Explode
import androidx.transition.Slide
import androidx.transition.Visibility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchVibration: MaterialSwitch
    private lateinit var switchProgressDir: MaterialSwitch
    private lateinit var switchProgressWidget: MaterialSwitch
    private lateinit var switchDeadlineNotification: MaterialSwitch
    private lateinit var switchDailyStatsNotification: MaterialSwitch
    private lateinit var switchMotivationalQuotes: MaterialSwitch
    private lateinit var switchFireworksOnFinish: MaterialSwitch
    private lateinit var buttonHomePage: MaterialButton
    private lateinit var buttonAuthorPage: MaterialButton
    private lateinit var buttonIssues: MaterialButton
    private lateinit var versionNumber: TextView
    private lateinit var aboutCard: MaterialCardView
    private lateinit var toggleGroupArchiveTime: MaterialButtonToggleGroup

    private var resetTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        DynamicColors.applyToActivityIfAvailable(this)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        GlobalUtils.readConfigInSettings(this)

        // 初始化控件
        switchVibration = findViewById(R.id.switchVibration)
        switchProgressDir = findViewById(R.id.switchProgressDir)
        switchProgressWidget = findViewById(R.id.switchProgressWidget)
        switchDeadlineNotification = findViewById(R.id.switchDeadlineNotification)
        switchDailyStatsNotification = findViewById(R.id.switchDailyStatsNotification)
        switchMotivationalQuotes = findViewById(R.id.switchMotivationalQuotes)
        switchFireworksOnFinish = findViewById(R.id.switchFireworksOnFinish)
        buttonAuthorPage = findViewById(R.id.buttonAuthorPage)
        buttonHomePage = findViewById(R.id.buttonHomePage)
        buttonIssues = findViewById(R.id.buttonIssues)
        versionNumber = findViewById(R.id.versionNumber)
        aboutCard = findViewById(R.id.aboutCard)
        toggleGroupArchiveTime = findViewById(R.id.toggleGroupArchiveTime)

        // 加载保存的设置
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)

        switchVibration.isChecked = sharedPreferences.getBoolean("vibration", true)
        switchProgressDir.isChecked = sharedPreferences.getBoolean("main_progress_dir", false)
        switchProgressWidget.isChecked = sharedPreferences.getBoolean("widget_progress_dir", false)
        switchDeadlineNotification.isChecked = sharedPreferences.getBoolean("deadline_notification", false)
        switchDailyStatsNotification.isChecked = sharedPreferences.getBoolean("daily_stats_notification", false)
        switchMotivationalQuotes.isChecked = sharedPreferences.getBoolean("motivational_quotes", true)
        switchFireworksOnFinish.isChecked = sharedPreferences.getBoolean("fireworks_anim", true)

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

        switchFireworksOnFinish.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("fireworks_anim", isChecked).apply()
        }

        // 设置超链接按钮点击事件
        buttonAuthorPage.setOnClickListener {
            val url = "https://github.com/AritxOnly"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        buttonHomePage.setOnClickListener {
            val url = "https://github.com/AritxOnly/Deadliner"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        buttonIssues.setOnClickListener {
            val url = "https://github.com/AritxOnly/Deadliner/issues"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        aboutCard.setOnClickListener {
            if (sharedPreferences.getBoolean("first_run", true)) {
                Snackbar.make(
                    aboutCard,
                    "你已经设置了下次打开显示欢迎页面",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            resetTimes++
            when (resetTimes) {
                in 2..4 -> {
                    Snackbar.make(
                        aboutCard,
                        "再点击${5 - resetTimes}次即可在下次打开时显示欢迎页面",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                5 -> {
                    Snackbar.make(
                        aboutCard,
                        "下次打开Deadliner将显示欢迎页面",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    sharedPreferences.edit().putBoolean("first_run", true).apply()
                    resetTimes = 0
                }
            }
        }

        toggleGroupArchiveTime.check(hashButton())
        toggleGroupArchiveTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                GlobalUtils.autoArchiveTime = deHashButton(checkedId)
                Log.d("GlobalUtils", "${deHashButton(checkedId)}")
                GlobalUtils.writeConfigInSettings(this)
            }
        }

        val appVersionString = """
            <strong>Dealiner</strong> ${packageManager.getPackageInfo(packageName, 0).versionName}<br>
            By Author <strong>Aritx Zhou</strong>
        """.trimIndent()
        versionNumber.setText(Html.fromHtml(appVersionString))

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finishAfterTransition() // 返回上一个界面
        }
    }

    private fun hashButton() : Int {
        when (GlobalUtils.autoArchiveTime) {
            1 -> return R.id.button1Day
            3 -> return R.id.button3Day
            7 -> return R.id.button7Day
            else -> throw Exception("Hash button Error")
        }
    }

    private fun deHashButton(buttonId: Int): Int {
        when (buttonId) {
            R.id.button1Day -> return 1
            R.id.button3Day -> return 3
            R.id.button7Day -> return 7
            else -> throw Exception("DeHash button Error")
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