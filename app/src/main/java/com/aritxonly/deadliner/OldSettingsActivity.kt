package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.Activity
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updatePadding
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class OldSettingsActivity : AppCompatActivity() {

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

    private lateinit var switchDetailDisplayMode: MaterialSwitch
    private lateinit var switchNearbyTasksBadge: MaterialSwitch

    private lateinit var buttonImport: MaterialButton
    private lateinit var buttonExport: MaterialButton

    private lateinit var buttonCancelAll: MaterialButton
    private lateinit var settingsAdvanced: TextView
    private lateinit var settingsAdvancedCard: MaterialCardView

    private lateinit var buttonShowIntroPage: MaterialButton
    private lateinit var buttonCloudSyncServer: MaterialButton
    private lateinit var buttonCustomFilterList: MaterialButton

    private lateinit var switchHideFromRecent: MaterialSwitch
    private lateinit var switchExperimentalEdgeToEdge: MaterialSwitch

    private lateinit var sliderArchiveTime: Slider

    private var resetTimes = 0

    companion object {
        private const val EXPORT_REQUEST_CODE = 1001
        private const val IMPORT_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        DynamicColors.applyToActivityIfAvailable(this)

        if (GlobalUtils.experimentalEdgeToEdge) {
            enableEdgeToEdge()
            window.isNavigationBarContrastEnforced = false

            val rootView = findViewById<LinearLayout>(R.id.mainSettings)
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

                view.updatePadding(top = statusBarInset)

                WindowInsetsCompat.CONSUMED
            }
        }

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        GlobalUtils.decideHideFromRecent(this, this@OldSettingsActivity)

        // 初始化控件
        switchVibration = findViewById(R.id.switchVibration)
        switchProgressDir = findViewById(R.id.switchProgressDir)
        switchProgressWidget = findViewById(R.id.switchProgressWidget)
        switchDeadlineNotification = findViewById(R.id.switchDeadlineNotification)
        switchDailyStatsNotification = findViewById(R.id.switchDailyStatsNotification)
        switchMotivationalQuotes = findViewById(R.id.switchMotivationalQuotes)
        switchFireworksOnFinish = findViewById(R.id.switchFireworksOnFinish)
        switchDetailDisplayMode = findViewById(R.id.switchDetailDisplayMode)
        switchNearbyTasksBadge = findViewById(R.id.switchNearbyTasksBadge)
        switchHideFromRecent = findViewById(R.id.switchHideFromRecent)
        switchExperimentalEdgeToEdge = findViewById(R.id.switchExperimentalEdgeToEdge)
        buttonAuthorPage = findViewById(R.id.buttonAuthorPage)
        buttonHomePage = findViewById(R.id.buttonHomePage)
        buttonIssues = findViewById(R.id.buttonIssues)
        versionNumber = findViewById(R.id.versionNumber)
        aboutCard = findViewById(R.id.aboutCard)

        buttonImport = findViewById(R.id.buttonImport)
        buttonExport = findViewById(R.id.buttonExport)

        buttonCancelAll = findViewById(R.id.buttonCancelAll)
        settingsAdvanced = findViewById(R.id.settingsAdvanced)
        settingsAdvancedCard = findViewById(R.id.settingsAdvancedCard)

        buttonShowIntroPage = findViewById(R.id.buttonShowIntroPage)
        buttonCloudSyncServer = findViewById(R.id.buttonCloudSyncServer)
        buttonCustomFilterList = findViewById(R.id.buttonCustomFilterList)

        sliderArchiveTime = findViewById(R.id.sliderArchiveTime)

        decideToShowAdvancedMode()

        switchVibration.isChecked = GlobalUtils.vibration
        switchProgressDir.isChecked = GlobalUtils.progressDir
        switchProgressWidget.isChecked = GlobalUtils.progressWidget
        switchDeadlineNotification.isChecked = GlobalUtils.deadlineNotification
        switchDailyStatsNotification.isChecked = GlobalUtils.dailyStatsNotification
        switchMotivationalQuotes.isChecked = GlobalUtils.motivationalQuotes
        switchFireworksOnFinish.isChecked = GlobalUtils.fireworksOnFinish
        switchDetailDisplayMode.isChecked = GlobalUtils.detailDisplayMode
        switchNearbyTasksBadge.isChecked = GlobalUtils.nearbyTasksBadge
        switchHideFromRecent.isChecked = GlobalUtils.hideFromRecent
        switchExperimentalEdgeToEdge.isChecked = GlobalUtils.experimentalEdgeToEdge

        // 监听开关状态变化并保存设置
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.vibration = isChecked
        }

        switchProgressDir.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.progressDir = isChecked
        }

        switchProgressWidget.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.progressWidget = isChecked
        }

        switchDeadlineNotification.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.deadlineNotification = isChecked
            if (!isChecked) {
                DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
                GlobalUtils.NotificationStatusManager.clearAllNotified()
            } else {
                Log.d("AlarmDebug", "Reached Here")
                GlobalUtils.setAlarms(DatabaseHelper.getInstance(applicationContext), applicationContext)
            }
        }

        switchDailyStatsNotification.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.dailyStatsNotification = isChecked
            if (isChecked) {
                showDailyTimePicker()
            } else {
                DeadlineAlarmScheduler.cancelDailyAlarm(applicationContext)
            }
        }
        switchDailyStatsNotification.setOnLongClickListener {
            if (GlobalUtils.dailyStatsNotification) {
                showDailyTimePicker()
            }
            true
        }

        switchMotivationalQuotes.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.motivationalQuotes = isChecked
        }

        switchFireworksOnFinish.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.fireworksOnFinish = isChecked
        }

        switchDetailDisplayMode.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.detailDisplayMode = isChecked
        }

        switchNearbyTasksBadge.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.nearbyTasksBadge = isChecked
            if (isChecked) {
                val options = arrayOf("数字角标", "圆点角标")
                var selectedItem = 0
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.alert_select_badge_type)
                    .setSingleChoiceItems(options, selectedItem) { dialog, which ->
                        selectedItem = which
                    }
                    .setPositiveButton(R.string.accept) { dialog, which ->
                        if (selectedItem != -1) {
                            when (selectedItem) {
                                0 -> GlobalUtils.nearbyDetailedBadge = true
                                else -> GlobalUtils.nearbyDetailedBadge = false
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, which ->
                        GlobalUtils.nearbyTasksBadge = false
                        switchNearbyTasksBadge.isChecked = false
                    }
                    .show()
            }
        }
        switchNearbyTasksBadge.setOnLongClickListener {
            if (GlobalUtils.nearbyTasksBadge) {
                val options = arrayOf("数字角标", "圆点角标")
                var selectedItem = 0
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.alert_select_badge_type)
                    .setSingleChoiceItems(options, selectedItem) { dialog, which ->
                        selectedItem = which
                    }
                    .setPositiveButton(R.string.accept) { dialog, which ->
                        if (selectedItem != -1) {
                            when (selectedItem) {
                                0 -> GlobalUtils.nearbyDetailedBadge = true
                                else -> GlobalUtils.nearbyDetailedBadge = false
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, which ->

                    }
                    .show()
            }
            true
        }

        switchHideFromRecent.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.hideFromRecent = isChecked
            GlobalUtils.decideHideFromRecent(this, this@OldSettingsActivity)
        }

        switchExperimentalEdgeToEdge.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.experimentalEdgeToEdge = isChecked
            MaterialAlertDialogBuilder(this)
                .setMessage("导入成功，需要重启应用生效")
                .setPositiveButton("立即重启") { _, _ ->
                    restartApp()
                }
                .setNegativeButton("稍后", null)
                .show()
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
            val hint = if (GlobalUtils.developerMode) "关闭" else "打开"
            resetTimes++
            when (resetTimes) {
                in 2..4 -> {
                    Toast.makeText(
                        this,
                        "再点击${5 - resetTimes}次即可${hint}高级模式",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                5 -> {
                    Toast.makeText(
                        this,
                        "高级模式已${hint}",
                        Toast.LENGTH_SHORT
                    ).show()
                    GlobalUtils.developerMode = !GlobalUtils.developerMode
                    resetTimes = 0

                    decideToShowAdvancedMode()
                }
            }
        }

        sliderArchiveTime.value = GlobalUtils.autoArchiveTime.toFloat()
        sliderArchiveTime.addOnChangeListener { _, value, _ ->
            GlobalUtils.autoArchiveTime = value.toInt()
        }

        val appVersionString = """
            <strong>Deadliner</strong> ${packageManager.getPackageInfo(packageName, 0).versionName}<br>
            By Author <strong>Aritx Zhou</strong>
        """.trimIndent()
        versionNumber.text = Html.fromHtml(appVersionString)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finishAfterTransition() // 返回上一个界面
        }
        // Material You Expressive Style BUTTON
        toolbar.post {
            val navDrawable = toolbar.navigationIcon ?: return@post
            val navButton = toolbar.children
                .firstOrNull { (it as? ImageView)?.drawable == navDrawable }
                ?: return@post

            val sizeDp = 40
            val density = navButton.resources.displayMetrics.density
            val sizePx = (sizeDp * density).toInt()

            val lp = navButton.layoutParams
                .also {
                    it.width = sizePx
                    it.height = sizePx
                }
            navButton.layoutParams = lp

            val paddingDp = 8
            val padPx = (paddingDp * density).toInt()
            navButton.setPadding(padPx, padPx, padPx, padPx)

            navButton.background = ContextCompat.getDrawable(this, R.drawable.circle_background_main)

            navButton.requestLayout()
        }

        buttonImport.setOnClickListener {
            DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
            GlobalUtils.NotificationStatusManager.clearAllNotified()
            Toast.makeText(this, "已成功销毁所有闹钟时间", Toast.LENGTH_SHORT).show()
            openBackup()
        }

        buttonExport.setOnClickListener {
            createBackup()
        }

        buttonCancelAll.setOnClickListener {
            DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
            GlobalUtils.NotificationStatusManager.clearAllNotified()
            Toast.makeText(this, "已成功销毁所有闹钟时间", Toast.LENGTH_SHORT).show()
        }

        buttonShowIntroPage.setOnClickListener {
            GlobalUtils.showIntroPage = true
        }

        buttonCloudSyncServer.setOnClickListener {
        }

        buttonCustomFilterList.setOnClickListener {
            // 原始数据源
            val allItems = GlobalUtils.customCalendarFilterList?.filterNotNull()?.toMutableList() ?: mutableListOf()
            // 用户当前选中的
            val selectedItems = GlobalUtils.customCalendarFilterListSelected?.filterNotNull()?.toMutableSet() ?: allItems.toMutableSet()

            fun showFilterDialog() {
                val itemsArray = allItems.toTypedArray()
                val checkedArray = BooleanArray(itemsArray.size) { index ->
                    // 默认已选中 current selection（如果第一次打开且 selectedItems 为空，则默认全选）
                    if (selectedItems.isEmpty()) true
                    else selectedItems.contains(itemsArray[index])
                }

                MaterialAlertDialogBuilder(this)
                    .setTitle("请选择要显示的日历过滤器")
                    .setMultiChoiceItems(itemsArray, checkedArray) { _, which, isChecked ->
                        val item = itemsArray[which]
                        if (isChecked) selectedItems.add(item)
                        else selectedItems.remove(item)
                    }
                    .setNeutralButton("添加项") { dialog, _ ->
                        dialog.dismiss()
                        // 弹出输入框，添加新选项
                        val inputLayout = TextInputLayout(this).apply {
                            hint = "新过滤器名称"
                            setPadding(32, 0, 32, 0)
                        }
                        val editText = TextInputEditText(inputLayout.context)
                        inputLayout.addView(editText)

                        MaterialAlertDialogBuilder(this)
                            .setTitle("新增过滤器")
                            .setView(inputLayout)
                            .setPositiveButton("确定") { subDialog, _ ->
                                val newItem = editText.text?.toString()?.trim()
                                if (!newItem.isNullOrEmpty() && !allItems.contains(newItem)) {
                                    // 更新数据源和选中集
                                    allItems.add(newItem)
                                    selectedItems.add(newItem)
                                    GlobalUtils.customCalendarFilterList = allItems.toSet()
                                    GlobalUtils.customCalendarFilterListSelected = selectedItems.toSet()
                                }
                                subDialog.dismiss()
                                // 重新打开主多选框
                                showFilterDialog()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    .setPositiveButton("确认") { dialog, _ ->
                        GlobalUtils.customCalendarFilterListSelected = selectedItems.toSet()
                        dialog.dismiss()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }

            // 首次打开
            showFilterDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EXPORT_REQUEST_CODE -> handleExportResult(resultCode, data)
            IMPORT_REQUEST_CODE -> handleImportResult(resultCode, data)
        }
    }

    private fun decideToShowAdvancedMode() {
        if (GlobalUtils.developerMode) {
            buttonCancelAll.visibility = View.VISIBLE
            settingsAdvanced.visibility = View.VISIBLE
            settingsAdvancedCard.visibility = View.VISIBLE
        } else {
            buttonCancelAll.visibility = View.GONE
            settingsAdvanced.visibility = View.GONE
            settingsAdvancedCard.visibility = View.GONE
        }
    }

    // 导出数据库
    @SuppressLint("SimpleDateFormat")
    private fun createBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/x-sqlite3"
            putExtra(Intent.EXTRA_TITLE, "deadliner_backup_${SimpleDateFormat("yyyyMMdd_HHmmss").format(
                Date()
            )}.db")
        }

        startActivityForResult(intent, EXPORT_REQUEST_CODE)
    }

    // 导入数据库
    private fun openBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/x-sqlite3",
                "application/vnd.sqlite3",
                "application/octet-stream",
                "application/sqlite3",
                "application/x-sql"
            ))
            // 可选：限制文件扩展名
            putExtra(Intent.EXTRA_TITLE, "*.db")
        }

        // 兼容旧版本
        val chooser = Intent.createChooser(intent, "选择数据库备份文件")
        startActivityForResult(chooser, IMPORT_REQUEST_CODE)
    }

    private fun handleExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME)
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    showToast("✅ 导出成功")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("❌ 导出失败: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun handleImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("确认导入")
                    .setMessage("将覆盖当前所有数据，请确认操作！")
                    .setPositiveButton("确定") { _, _ ->
                        performImport(uri)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    private fun performImport(uri: Uri) {
        try {
            // 关闭当前数据库连接
            DatabaseHelper.closeInstance()

            // 替换数据库文件
            val dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 重新初始化数据库
            DatabaseHelper.getInstance(this)

            // 提示需要重启应用
            MaterialAlertDialogBuilder(this)
                .setMessage("导入成功，需要重启应用生效")
                .setPositiveButton("立即重启") { _, _ ->
                    restartApp()
                }
                .setNegativeButton("稍后", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("❌ 导入失败: ${e.localizedMessage}")
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showDailyTimePicker() {
        val currentHour = GlobalUtils.dailyNotificationHour
        val currentMinute = GlobalUtils.dailyNotificationMinute
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("选择每日通知时间")
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute
            GlobalUtils.dailyNotificationHour = selectedHour
            GlobalUtils.dailyNotificationMinute = selectedMinute
            DeadlineAlarmScheduler.scheduleDailyAlarm(applicationContext)
        }

        picker.show(supportFragmentManager, "daily_time_picker")
    }

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean) {
        if (GlobalUtils.experimentalEdgeToEdge) return

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