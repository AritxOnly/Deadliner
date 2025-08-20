package com.aritxonly.deadliner.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import com.aritxonly.deadliner.DatabaseHelper
import com.aritxonly.deadliner.databinding.HabitMiniWidgetConfigureBinding
import com.aritxonly.deadliner.model.DeadlineType
import androidx.core.content.edit
import com.google.android.material.color.DynamicColors
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * The configuration screen for the [HabitMiniWidget] AppWidget.
 */
class HabitWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var provider: ComponentName? = null

    private var onClickListener = View.OnClickListener {
        val context = this@HabitWidgetConfigureActivity
        val awm = AppWidgetManager.getInstance(context)

        saveIdPref(context, appWidgetId, selectedHabitId, provider)

        // It is the responsibility of the configuration activity to update the app widget
        when (provider?.className) {
            "com.aritxonly.deadliner.widgets.HabitMiniWidget" -> {
                updateAppMiniHabitWidget(context, awm, appWidgetId)
            }

            "com.aritxonly.deadliner.widgets.HabitMediumWidget" -> {
                updateMediumAppWidget(context, awm, appWidgetId)
            }

            else -> {}
        }

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: HabitMiniWidgetConfigureBinding
    private var selectedHabitId = -1L

    public override fun onCreate(icicle: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(icicle)

        DynamicColors.applyToActivityIfAvailable(this)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = HabitMiniWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top    = sysBarInsets.top + 16,
                bottom = sysBarInsets.bottom + 16
            )

            insets
        }

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // 获取 provider（用于区分 Mini / Medium）
        val awm = AppWidgetManager.getInstance(this)
        val info = awm.getAppWidgetInfo(appWidgetId)
        provider = info.provider

        val habits = DatabaseHelper.getInstance(this).getDDLsByType(DeadlineType.HABIT)
        val names = habits.map { it.name }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, names)
        binding.lvHabits.adapter = adapter
        binding.addButton.isEnabled = false

        binding.lvHabits.choiceMode = ListView.CHOICE_MODE_SINGLE
        binding.lvHabits.setOnItemClickListener { _, _, pos, _ ->
            binding.lvHabits.setItemChecked(pos, true)
            selectedHabitId = habits[pos].id
            binding.addButton.isEnabled = true
        }

        binding.addButton.setOnClickListener(onClickListener)
    }
}

private const val PREFS_NAME_BASE = "com.aritxonly.deadliner.widgets."
private const val PREF_PREFIX_KEY = "appwidget_"

internal fun getProviderName(provider: ComponentName?): String? {
    val className = (provider?:return null).className.substringAfterLast('.')
    return if (className.endsWith("Provider")) {
        className.removeSuffix("Provider")
    } else {
        className
    }
}

internal fun saveIdPref(context: Context, appWidgetId: Int, id: Long, provider: ComponentName?) {
    val providerName = getProviderName(provider)?:""
    val prefsName = PREFS_NAME_BASE + providerName
    context.getSharedPreferences(prefsName, 0).edit {
        putLong(PREF_PREFIX_KEY + appWidgetId, id)
    }
}

internal fun loadIdPref(context: Context, appWidgetId: Int, provider: ComponentName?): Long {
    val providerName = getProviderName(provider)?:""
    val prefsName = PREFS_NAME_BASE + providerName
    val prefs = context.getSharedPreferences(prefsName, 0)
    return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1)
}

internal fun deleteIdPref(context: Context, appWidgetId: Int, provider: ComponentName?) {
    val providerName = getProviderName(provider)?:""
    val prefsName = PREFS_NAME_BASE + providerName
    context.getSharedPreferences(prefsName, 0).edit {
        remove(PREF_PREFIX_KEY + appWidgetId)
    }
}