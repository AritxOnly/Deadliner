package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

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

    private lateinit var switchDetailDisplayMode: MaterialSwitch

    private lateinit var buttonImport: MaterialButton
    private lateinit var buttonExport: MaterialButton

    private var resetTimes = 0

    companion object {
        private const val EXPORT_REQUEST_CODE = 1001
        private const val IMPORT_REQUEST_CODE = 1002
    }

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
        switchFireworksOnFinish = findViewById(R.id.switchFireworksOnFinish)
        switchDetailDisplayMode = findViewById(R.id.switchDetailDisplayMode)
        buttonAuthorPage = findViewById(R.id.buttonAuthorPage)
        buttonHomePage = findViewById(R.id.buttonHomePage)
        buttonIssues = findViewById(R.id.buttonIssues)
        versionNumber = findViewById(R.id.versionNumber)
        aboutCard = findViewById(R.id.aboutCard)
        toggleGroupArchiveTime = findViewById(R.id.toggleGroupArchiveTime)

        buttonImport = findViewById(R.id.buttonImport)
        buttonExport = findViewById(R.id.buttonExport)

        switchVibration.isChecked = GlobalUtils.vibration
        switchProgressDir.isChecked = GlobalUtils.progressDir
        switchProgressWidget.isChecked = GlobalUtils.progressWidget
        switchDeadlineNotification.isChecked = GlobalUtils.deadlineNotification
        switchDailyStatsNotification.isChecked = GlobalUtils.dailyStatsNotification
        switchMotivationalQuotes.isChecked = GlobalUtils.motivationalQuotes
        switchFireworksOnFinish.isChecked = GlobalUtils.fireworksOnFinish
        switchDetailDisplayMode.isChecked = GlobalUtils.detailDisplayMode

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
        }

        switchDailyStatsNotification.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.dailyStatsNotification = isChecked
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
            if (GlobalUtils.showIntroPage) {
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
                    GlobalUtils.showIntroPage = true
                    resetTimes = 0
                }
            }
        }

        toggleGroupArchiveTime.check(hashButton())
        toggleGroupArchiveTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                GlobalUtils.autoArchiveTime = deHashButton(checkedId)
                Log.d("GlobalUtils", "${deHashButton(checkedId)}")
            }
        }

        val appVersionString = """
            <strong>Deadliner</strong> ${packageManager.getPackageInfo(packageName, 0).versionName}<br>
            By Author <strong>Aritx Zhou</strong>
        """.trimIndent()
        versionNumber.setText(Html.fromHtml(appVersionString))

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finishAfterTransition() // 返回上一个界面
        }

        buttonImport.setOnClickListener {
            openBackup()
        }

        buttonExport.setOnClickListener {
            createBackup()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EXPORT_REQUEST_CODE -> handleExportResult(resultCode, data)
            IMPORT_REQUEST_CODE -> handleImportResult(resultCode, data)
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