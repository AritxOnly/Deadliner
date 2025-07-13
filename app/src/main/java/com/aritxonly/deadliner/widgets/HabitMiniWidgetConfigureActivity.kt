package com.aritxonly.deadliner.widgets

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import com.aritxonly.deadliner.DatabaseHelper
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.databinding.HabitMiniWidgetConfigureBinding
import com.aritxonly.deadliner.model.DeadlineType
import androidx.core.content.edit
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import okhttp3.internal.toHexString

/**
 * The configuration screen for the [HabitMiniWidget] AppWidget.
 */
class HabitMiniWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var onClickListener = View.OnClickListener {
        val context = this@HabitMiniWidgetConfigureActivity

        saveIdPref(context, appWidgetId, selectedHabitId)

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppMiniHabitWidget(context, appWidgetManager, appWidgetId)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: HabitMiniWidgetConfigureBinding
    private var selectedHabitId = -1L

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        DynamicColors.applyToActivityIfAvailable(this)

        val colorSurface = getMaterialThemeColor(com.google.android.material.R.attr.colorSurface)

        setSystemBarColors(colorSurface, isLightColor(colorSurface), colorSurface)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = HabitMiniWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean, colorNavigationBar: Int) {
        if (GlobalUtils.experimentalEdgeToEdge) return

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

    private fun getMaterialThemeColor(attributeId: Int): Int {
        return MaterialColors.getColor(ContextWrapper(this), attributeId, Color.WHITE)
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

private const val PREFS_NAME = "com.aritxonly.deadliner.widgets.HabitMiniWidget"
private const val PREF_PREFIX_KEY = "appwidget_"

internal fun saveIdPref(context: Context, appWidgetId: Int, id: Long) {
    context.getSharedPreferences(PREFS_NAME, 0).edit {
        putLong(PREF_PREFIX_KEY + appWidgetId, id)
    }
}

internal fun loadIdPref(context: Context, appWidgetId: Int): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1)
}

internal fun deleteIdPref(context: Context, appWidgetId: Int) {
    context.getSharedPreferences(PREFS_NAME, 0).edit {
        remove(PREF_PREFIX_KEY + appWidgetId)
    }
}