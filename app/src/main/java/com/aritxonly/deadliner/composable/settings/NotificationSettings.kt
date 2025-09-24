package com.aritxonly.deadliner.composable.settings

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.composable.expressiveTypeModifier
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

@Composable
fun NotificationSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    fun formatDailyTime(hour: Int, minute: Int): String {
        val hh = hour.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')
        return context.getString(R.string.settings_notification_formatter_set_time, hh, mm)
    }

    val fragmentManager = remember(context) {
        (context as? FragmentActivity)?.supportFragmentManager
    }

    var deadlineNotification by remember { mutableStateOf(GlobalUtils.deadlineNotification) }
    var dailyNotification by remember { mutableStateOf(GlobalUtils.dailyStatsNotification) }

    var dailyMinute by remember { mutableStateOf(GlobalUtils.dailyNotificationMinute) }
    var dailyHour by remember { mutableStateOf(GlobalUtils.dailyNotificationHour) }

    var notifyBefore by remember { mutableStateOf(GlobalUtils.deadlineNotificationBefore) }
    val onNotifyBeforeChange: (Float) -> Unit = {
        notifyBefore = it.toLong()
        GlobalUtils.deadlineNotificationBefore = notifyBefore
        DeadlineAlarmScheduler.cancelAllAlarms(context)
        GlobalUtils.setAlarms(DatabaseHelper.getInstance(context), context)
    }

    val defaultText = stringResource(R.string.settings_support_daily_notification)
    val initialTextDailyNotification = if (!dailyNotification)
        defaultText
    else formatDailyTime(dailyHour, dailyMinute)

    var supportingTextDailyNotification by remember { mutableStateOf(initialTextDailyNotification) }

    val onDeadlineNotificationChange: (Boolean) -> Unit = {
        GlobalUtils.deadlineNotification = it
        deadlineNotification = it
    }
    val onDailyNotificationChange: (Boolean) -> Unit = {
        GlobalUtils.dailyStatsNotification = it
        dailyNotification = it
        supportingTextDailyNotification = if (!dailyNotification)
            defaultText
        else formatDailyTime(dailyHour, dailyMinute)
    }
    val onDailyNotificationLongPress: (Boolean) -> Unit = {
        if (!it) {
            fragmentManager?.let { supportFragmentManager ->
                showDailyTimePicker(context, supportFragmentManager) { hour, minute ->
                    dailyMinute = minute
                    dailyHour = hour
                }
            }
        }
        supportingTextDailyNotification = if (!dailyNotification)
            defaultText
        else formatDailyTime(dailyHour, dailyMinute)
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_notification),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())) {
            SvgCard(R.drawable.svg_notifications, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_nearby_notification_push)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_nearby_notification,
                    supportingText = R.string.settings_support_nearby_notification,
                    checked = deadlineNotification,
                    onCheckedChange = onDeadlineNotificationChange,
                )

                if (deadlineNotification) {
                    SettingsSectionDivider()

                    SettingsSliderItemWithLabel(
                        label = R.string.settings_nearby_notification_before,
                        value = notifyBefore.toFloat(),
                        valueRange = 6f..72f,
                        onValueChange = onNotifyBeforeChange,
                        steps = 10
                    )
                }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_daily_notification_push)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_everyday_notification,
                    supportingRawText = supportingTextDailyNotification,
                    checked = dailyNotification,
                    onCheckedChange = onDailyNotificationChange,
                    onLongPress = onDailyNotificationLongPress
                )
            }
        }
    }
}

private fun showDailyTimePicker(
    context: Context,
    supportFragmentManager: FragmentManager,
    onChange:(Int, Int) -> Unit
) {
    val currentHour = GlobalUtils.dailyNotificationHour
    val currentMinute = GlobalUtils.dailyNotificationMinute
    val picker = MaterialTimePicker.Builder()
        .setTimeFormat(TimeFormat.CLOCK_24H)
        .setHour(currentHour)
        .setMinute(currentMinute)
        .setTitleText(R.string.choose_daily_notifictaion_time)
        .build()

    picker.addOnPositiveButtonClickListener {
        val selectedHour = picker.hour
        val selectedMinute = picker.minute
        GlobalUtils.dailyNotificationHour = selectedHour
        GlobalUtils.dailyNotificationMinute = selectedMinute
        onChange(selectedHour, selectedMinute)
        DeadlineAlarmScheduler.scheduleDailyAlarm(context)
    }

    picker.show(supportFragmentManager, "daily_time_picker")
}