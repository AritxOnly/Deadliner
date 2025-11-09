package com.aritxonly.deadliner

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.main.classic.ClassicController
import com.aritxonly.deadliner.ui.main.classic.ClassicHost
import com.aritxonly.deadliner.ui.main.classic.CustomAdapter
import com.aritxonly.deadliner.ui.main.simplified.SimplifiedHost
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.widgets.HabitMiniWidget
import com.aritxonly.deadliner.widgets.LargeDeadlineWidget
import com.aritxonly.deadliner.widgets.MultiDeadlineWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : AppCompatActivity(), CustomAdapter.SwipeListener {
    private var classicController: ClassicController? = null

    lateinit var addDDLLauncher: ActivityResultLauncher<Intent>

    private val notifyLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        classicController?.dialogFlipperNext()
    }

    private val calendarLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        if (!results.values.all { it }) {
            classicController?.onCalendarPermissionDenied()
        }
        classicController?.dialogFlipperNext()
    }


    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addDDLLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                classicController?.reloadAfterAdd()
            }
        }

        enableEdgeToEdgeForAllDevices()

        _showSearch.value = intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false)

        setContent {
            val searchActive by showSearch.collectAsState()

            DeadlinerTheme {
                val style = remember { UiStyle.fromKey(GlobalUtils.style) }
                when (style) {
                    UiStyle.Classic     -> ClassicHost(
                        activity = this,
                        addDDLLauncher = addDDLLauncher,
                        notifyPermissionLauncher = notifyLauncher,
                        calendarPermissionLauncher = calendarLauncher
                    )
                    UiStyle.Simplified  -> SimplifiedHost(
                        searchActive = searchActive,
                        onSearchActiveChange = { _showSearch.value = it },
                        activity = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateWidget()
    }

    override fun onStop() {
        super.onStop()
        updateWidget()
    }

    // —— 某些系统回调需要从 Activity 转发到 classicController —— //
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val style = UiStyle.fromKey(GlobalUtils.style)

        when (style) {
            UiStyle.Classic ->
                classicController?.onNewIntent(intent)
            UiStyle.Simplified ->
                _showSearch.value = intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false)
        }

        Log.d("Search", _showSearch.value.toString())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        classicController?.onConfigurationChanged(newConfig)
    }
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        classicController?.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        classicController?.onWindowFocusChanged(hasFocus)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        classicController?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        classicController?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onSwipeLeft(position: Int)  { classicController?.onSwipeLeft(position) }
    override fun onSwipeRight(position: Int) { classicController?.onSwipeRight(position) }

    internal fun setClassicControllerRef(c: ClassicController?) {
        classicController = c
        c?.let {
            c.normalizeRootInsets()
        }
    }

    fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, MultiDeadlineWidget::class.java))
        for (appWidgetId in appWidgetIds) {
            MultiDeadlineWidget.updateWidget(this, appWidgetManager, appWidgetId)
        }

        val largeWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, LargeDeadlineWidget::class.java))
        for (largeWidgetId in largeWidgetIds) {
            LargeDeadlineWidget.updateWidget(this, appWidgetManager, largeWidgetId)
        }

        val habitMiniWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this,
            HabitMiniWidget::class.java))
        for (habitMiniWidgetId in habitMiniWidgetIds) {
            HabitMiniWidget.updateWidget(this, appWidgetManager, habitMiniWidgetId)
        }
    }
}

