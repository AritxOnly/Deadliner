package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aritxonly.deadliner.composable.settings.AboutSettingsScreen
import com.aritxonly.deadliner.composable.settings.ArchiveSettingsScreen
import com.aritxonly.deadliner.composable.settings.BackupSettingsScreen
import com.aritxonly.deadliner.composable.settings.BadgeSettingsScreen
import com.aritxonly.deadliner.composable.settings.AISettingsScreen
import com.aritxonly.deadliner.composable.settings.DeveloperSettingsScreen
import com.aritxonly.deadliner.composable.settings.DonateScreen
import com.aritxonly.deadliner.composable.settings.FeedbackScreen
import com.aritxonly.deadliner.composable.settings.GeneralSettingsScreen
import com.aritxonly.deadliner.composable.settings.InterfaceSettingsScreen
import com.aritxonly.deadliner.composable.settings.LicenseScreen
import com.aritxonly.deadliner.composable.settings.MainSettingsScreen
import com.aritxonly.deadliner.composable.settings.NotificationSettingsScreen
import com.aritxonly.deadliner.composable.settings.PolicyScreen
import com.aritxonly.deadliner.composable.settings.PromptSettingsScreen
import com.aritxonly.deadliner.composable.settings.UpdateScreen
import com.aritxonly.deadliner.composable.settings.VibrationSettingsScreen
import com.aritxonly.deadliner.composable.settings.WebSettingsScreen
import com.aritxonly.deadliner.composable.settings.WidgetSettingsScreen
import com.aritxonly.deadliner.composable.settings.WikiScreen
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

sealed class SettingsRoute(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val supportRes: Int?,
    @DrawableRes val iconRes: Int?
) {
    object Main : SettingsRoute("main", R.string.settings_title, null, null)

    // region: 二级界面
    object General : SettingsRoute("general", R.string.settings_general, R.string.settings_support_general, R.drawable.ic_general_settings)
    object Interface : SettingsRoute("interface", R.string.settings_interface_display, R.string.settings_support_interface_display, R.drawable.ic_palette)
    object Notification : SettingsRoute("notification", R.string.settings_notification, R.string.settings_support_notification, R.drawable.ic_notification_settings)
    object Backup : SettingsRoute("backup", R.string.settings_backup, R.string.settings_support_backup, R.drawable.ic_backup_settings)
    object AI : SettingsRoute("ai", R.string.settings_deadliner_ai, R.string.settings_support_deadliner_ai, R.drawable.ic_deepseek)
    object WebDAV : SettingsRoute("webdav", R.string.settings_webdav, R.string.settings_support_webdav, R.drawable.ic_cloud)
    object Developer : SettingsRoute("developer", R.string.settings_developer, R.string.settings_support_developer, R.drawable.ic_developer_settings)
    object Widget : SettingsRoute("widget", R.string.settings_widget, R.string.settings_support_widget, R.drawable.ic_widgets_settings)
    object Wiki : SettingsRoute("wiki", R.string.settings_wiki, R.string.settings_support_wiki, R.drawable.ic_manual)
    object Feedback : SettingsRoute("feedback", R.string.settings_feedback, R.string.settings_support_feedback, R.drawable.ic_support)
    object About : SettingsRoute("about", R.string.settings_about, R.string.settings_support_about, R.drawable.ic_package)
    // endregion

    // region:三级界面
    object Vibration : SettingsRoute("vibration", R.string.settings_vibration_title, R.string.settings_support_vibration, null)
    object Badge : SettingsRoute("badge", R.string.settings_tasks_badge_title, R.string.settings_support_tasks_badge, null)
    object Archive : SettingsRoute("archive", R.string.settings_auto_archive_title, R.string.settings_support_auto_archive, null)

    object Prompt : SettingsRoute("prompt", R.string.settings_ai_custom_prompt, R.string.settings_support_ai_custom_prompt, null)

    object Update : SettingsRoute("update", R.string.settings_check_for_updates, R.string.settings_check_for_updates, R.drawable.ic_update)
    object License : SettingsRoute("license", R.string.settings_license, R.string.settings_license_summary, R.drawable.ic_license)
    object Policy : SettingsRoute("policy", R.string.settings_privacy_policy, R.string.settings_privacy_summary, R.drawable.ic_privacy)
    object Donate : SettingsRoute("donate", R.string.settings_donate, R.string.settings_support_donate, null)
    // endregion

    companion object {
        val allSubRoutes = listOf(
            listOf(
                General,
                Interface,
                Notification,
                Backup,
            ),
            listOf(Widget),
            listOf(
                AI,
                WebDAV,
            ),
            listOf(Developer),
            listOf(
                Wiki,
                Feedback,
                About
            )
        )

        val generalThirdRoutes = listOf(
            Vibration, Badge, Archive
        )

        val aboutThirdRoutes = listOf(
            Update, License, Policy, Donate
        )
    }
}

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val EXPORT_REQUEST_CODE = 1001
        private const val IMPORT_REQUEST_CODE = 1002

        const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()

        super.onCreate(savedInstanceState)

        setContent {
            DeadlinerTheme {
                val navController = rememberNavController()

                val defaultEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))
                }
                val defaultExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                    fadeOut(tween(300))
                }
                val defaultPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                    fadeIn(tween(300))
                }
                val defaultPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(300))
                }

                val initialRoute = intent.getStringExtra(EXTRA_INITIAL_ROUTE)
                LaunchedEffect(initialRoute) {
                    initialRoute?.let { route ->
                        navController.navigate(route) {
                            popUpTo(SettingsRoute.Main.route) { inclusive = false }
                        }
                    }
                }

                NavHost(
                    navController,
                    startDestination = SettingsRoute.Main.route,
                    enterTransition = defaultEnter,
                    exitTransition = defaultExit,
                    popEnterTransition = defaultPopEnter,
                    popExitTransition = defaultPopExit
                ) {
                    composable(SettingsRoute.Main.route) {
                        MainSettingsScreen(navController, onClose = { finishAfterTransition() })
                    }

                    composable(SettingsRoute.General.route) { GeneralSettingsScreen(
                        navController, handleRestart = { showDialogRestartAppTablet() }
                    ) { navController.navigateUp() } }
                    composable(SettingsRoute.Interface.route) { InterfaceSettingsScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Notification.route) { NotificationSettingsScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Backup.route) {
                        BackupSettingsScreen(
                            handleExport = { createBackup() },
                            handleImport = {
                                DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
                                GlobalUtils.NotificationStatusManager.clearAllNotified()
                                Toast.makeText(this@SettingsActivity, getString(R.string.destroy_alarms), Toast.LENGTH_SHORT).show()
                                openBackup()
                            },
                            handleWebSettings = { navController.navigate(SettingsRoute.WebDAV.route) }
                        ) { navController.navigateUp() }
                    }

                    composable(SettingsRoute.Widget.route) { WidgetSettingsScreen { navController.navigateUp() } }

                    composable(SettingsRoute.AI.route) { AISettingsScreen(navController) { navController.navigateUp() } }
                    composable(SettingsRoute.WebDAV.route) { WebSettingsScreen { navController.navigateUp() } }

                    composable(SettingsRoute.Developer.route) {
                        DeveloperSettingsScreen(
                            onClickCustomFilter = {
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

                                    MaterialAlertDialogBuilder(this@SettingsActivity)
                                        .setTitle(R.string.select_filter_calendar)
                                        .setMultiChoiceItems(itemsArray, checkedArray) { _, which, isChecked ->
                                            val item = itemsArray[which]
                                            if (isChecked) selectedItems.add(item)
                                            else selectedItems.remove(item)
                                        }
                                        .setNeutralButton(R.string.filter_add_one) { dialog, _ ->
                                            dialog.dismiss()
                                            // 弹出输入框，添加新选项
                                            val inputLayout = TextInputLayout(this@SettingsActivity).apply {
                                                hint = getString(R.string.new_filter_name)
                                                setPadding(32, 0, 32, 0)
                                            }
                                            val editText = TextInputEditText(inputLayout.context)
                                            inputLayout.addView(editText)

                                            MaterialAlertDialogBuilder(this@SettingsActivity)
                                                .setTitle(R.string.new_filter_title)
                                                .setView(inputLayout)
                                                .setPositiveButton(R.string.accept) { subDialog, _ ->
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
                                                .setNegativeButton(R.string.cancel, null)
                                                .show()
                                        }
                                        .setPositiveButton(R.string.accept) { dialog, _ ->
                                            GlobalUtils.customCalendarFilterListSelected = selectedItems.toSet()
                                            dialog.dismiss()
                                        }
                                        .setNegativeButton(R.string.cancel, null)
                                        .show()
                                }

                                // 首次打开
                                showFilterDialog()
                            },
                            onClickCancelAll = {
                                DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
                                GlobalUtils.NotificationStatusManager.clearAllNotified()
                                Toast.makeText(this@SettingsActivity, getString(R.string.destroy_alarms), Toast.LENGTH_SHORT).show()
                            },
                            onClickShowIntro = {
                                GlobalUtils.showIntroPage = true
                                Toast.makeText(this@SettingsActivity, getString(R.string.show_intro_next_time), Toast.LENGTH_SHORT).show()
                            },
                        ) { navController.navigateUp() }
                    }

                    composable(SettingsRoute.Wiki.route) { WikiScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Feedback.route) { FeedbackScreen { navController.navigateUp() } }
                    composable(SettingsRoute.About.route) { AboutSettingsScreen(navController) { navController.navigateUp() } }

                    composable(SettingsRoute.Vibration.route) { VibrationSettingsScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Badge.route) { BadgeSettingsScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Archive.route) { ArchiveSettingsScreen { navController.navigateUp() } }

                    composable(SettingsRoute.Prompt.route) { PromptSettingsScreen { navController.navigateUp() } }

                    composable(SettingsRoute.Update.route) { UpdateScreen { navController.navigateUp() } }
                    composable(SettingsRoute.License.route) { LicenseScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Policy.route) { PolicyScreen { navController.navigateUp() } }
                    composable(SettingsRoute.Donate.route) { DonateScreen { navController.navigateUp() } }
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EXPORT_REQUEST_CODE -> handleExportResult(resultCode, data)
            IMPORT_REQUEST_CODE -> handleImportResult(resultCode, data)
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
        val chooser = Intent.createChooser(intent, getString(R.string.choose_backup))
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
                    showToast(getString(R.string.export_success))
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast(getString(R.string.export_failed, e.localizedMessage))
                }
            }
        }
    }

    private fun handleImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_import)
                    .setMessage(R.string.confirm_import_message)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        performImport(uri)
                    }
                    .setNegativeButton(R.string.cancel, null)
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
                .setMessage(R.string.import_success)
                .setPositiveButton(R.string.restart_now) { _, _ ->
                    restartApp()
                }
                .setNegativeButton(R.string.later, null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.import_failed, e.localizedMessage))
        }
    }

    private fun showDialogRestartAppTablet() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.embedded_activities_success)
            .setPositiveButton(R.string.restart_now) { _, _ ->
                restartApp()
            }
            .setNegativeButton(R.string.later, null)
            .show()
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}
