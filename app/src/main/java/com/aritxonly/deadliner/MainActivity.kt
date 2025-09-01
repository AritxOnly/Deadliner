package com.aritxonly.deadliner

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.window.layout.WindowInfoTracker
import com.aritxonly.deadliner.SettingsActivity.Companion.EXTRA_INITIAL_ROUTE
import com.aritxonly.deadliner.composable.agent.DeepseekOverlayHost
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.ViewModelFactory
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineFrequency.*
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.model.updateNoteWithDate
import com.aritxonly.deadliner.notification.NotificationUtil
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.web.UpdateInfo
import com.aritxonly.deadliner.web.UpdateManager
import com.aritxonly.deadliner.widgets.HabitMiniWidget
import com.aritxonly.deadliner.widgets.LargeDeadlineWidget
import com.aritxonly.deadliner.widgets.MultiDeadlineWidget
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.loadingindicator.LoadingIndicator
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.dionsegijn.konfetti.xml.KonfettiView
import okhttp3.internal.toHexString
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId

class MainActivity : AppCompatActivity(), CustomAdapter.SwipeListener {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var addEventButton: FloatingActionButton
    private lateinit var settingsButton: ImageButton
    private val itemList = mutableListOf<DDLItem>()
    private lateinit var adapter: CustomAdapter
    private lateinit var addDDLLauncher: ActivityResultLauncher<Intent>
    private lateinit var titleBar: TextView
    private lateinit var excitementText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var konfettiViewMain: KonfettiView
    private lateinit var finishNotice: LinearLayout

    /**
     * Note in v2.0 build:
     *  archivedButton is now decrypted
     *  all the button is now implemented in bottomAppBar
     */
    /* v2.0 added */
    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var bottomBarContainer: CoordinatorLayout

    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchOverlay: ConstraintLayout
    private lateinit var dataOverlay: View
    private lateinit var refreshIndicator: CircularProgressIndicator
    private lateinit var bottomBlur: View
    private lateinit var bottomBarBackground: View

    private lateinit var viewHolderWithAppBar: View
    private lateinit var viewHolderWithNoAppBar: View

    private lateinit var searchButton: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            viewModel.loadData(currentType, silent = true)
            handler.postDelayed(this, 30000)
        }
    }

    private var isFireworksAnimEnable = true
    private var pauseRefresh: Boolean = false
    private var isBottomBarVisible = true

    private var currentType = DeadlineType.TASK

    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory(
            context = this
        )
    }

    private lateinit var materialColorScheme: AppColorScheme
    private var dialogFlipper: ViewFlipper? = null

    private lateinit var clipboardManager: android.content.ClipboardManager
    private val clipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        if (!GlobalUtils.clipboardEnable) return@OnPrimaryClipChangedListener
        handleClipboardChange()
    }
    private var hasCheckedInitialClipboard = false

    private fun handleClipboardChange() {
        val clip: ClipData? = clipboardManager.primaryClip
        val newText = clip?.getItemAt(0)?.coerceToText(this).toString()
        if (newText.isNotBlank() && newText != viewModel.lastClipboardText) {
            viewModel.lastClipboardText = newText
            triggerFeatureBasedOnClipboard(newText)
        }
    }

    private fun triggerFeatureBasedOnClipboard(text: String) {
        if (!GlobalUtils.clipboardEnable) return

        val snackBarParent = if (isBottomBarVisible)
            viewHolderWithAppBar
        else viewHolderWithNoAppBar

        val snackbar = Snackbar.make(
            snackBarParent,
            getString(R.string.show_clipboard_deepseek_snackbar),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.add)) {
            showAgentOverlay(text)
        }.setAnchorView(bottomAppBar)

        val bg = snackbar.view.background
        if (bg is MaterialShapeDrawable) {
            snackbar.view.background = bg.apply {
                shapeAppearanceModel = shapeAppearanceModel
                    .toBuilder()
                    .setAllCornerSizes(16f.dpToPx())
                    .build()
            }
        }
        snackbar.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 跟随主题色
        Log.d("MainActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 开启边到边沉浸
        enableEdgeToEdgeForAllDevices()
        normalizeRootInsets()

        DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
        DeadlineAlarmScheduler.cancelDailyAlarm(applicationContext)

        materialColorScheme = AppColorScheme(
            primary = getThemeColor(androidx.appcompat.R.attr.colorPrimary),
            onPrimary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnPrimary),
            primaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimaryContainer),
            surface = getMaterialThemeColor(com.google.android.material.R.attr.colorSurface),
            onSurface = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSurface),
            surfaceContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer),
            secondary = getMaterialThemeColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSecondary),
            secondaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSecondaryContainer),
            onSecondaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer),
            tertiary = getMaterialThemeColor(com.google.android.material.R.attr.colorTertiary),
            onTertiary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnTertiary),
            tertiaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorTertiaryContainer),
            onTertiaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorOnTertiaryContainer),
        )

        DynamicColors.applyToActivityIfAvailable(this)

        GlobalUtils.decideHideFromRecent(this, this@MainActivity)

        // 获取主题中的 colorSurface 值
        val colorSurface = materialColorScheme.surface
        val colorContainer = materialColorScheme.surfaceContainer

        Log.d("MainActivity", "colorSurface ${colorSurface.toHexString()}")

        val mainPage: ConstraintLayout = findViewById(R.id.main)
        mainPage.setBackgroundColor(colorSurface)

        databaseHelper = DatabaseHelper.getInstance(applicationContext)

        finishNotice = findViewById(R.id.finishNotice)
        konfettiViewMain = findViewById(R.id.konfettiViewMain)
        recyclerView = findViewById(R.id.recyclerView)
        addEventButton = findViewById(R.id.addEvent)
        settingsButton = findViewById(R.id.settingsButton)

        viewHolderWithAppBar = findViewById(R.id.viewHolderWithAppBar)
        viewHolderWithNoAppBar = findViewById(R.id.viewHolderWithNoAppBar)

        decideShowEmptyNotice()

        // 设置 RecyclerView
        adapter = CustomAdapter(itemList, this, viewModel)
        viewModel.ddlList.observe(this) { items ->
            adapter.itemList = items
            adapter.notifyDataSetChanged()
            decideShowEmptyNotice()
        }
        adapter.setSwipeListener(this)
        // 设置单击监听器
        adapter.setOnItemClickListener(object : CustomAdapter.OnItemClickListener {
            /**
             * v2.0 update note:
             * 这里是一个我解决不了的问题：Compose页面 (DeadlineDetailActivity) 无法应用动态取色。
             * 因此我只能退而求其次，将我需要用到的颜色封装到一个数据类中，并在Compose Activity中调用数据类
             * 获取MainActivity的颜色
             * Dirty but 唯一的做法
             */
            override fun onItemClick(position: Int) {
                val clickedItem = adapter.itemList[position]

                if (clickedItem.type == DeadlineType.HABIT) {
                    GlobalUtils.showRetroactiveDatePicker(supportFragmentManager) { pickedDateMillis ->
                        // pickedDateMillis：UTC 毫秒时间戳
                        // 在这里把 pickedDateMillis 转成 LocalDate 或者你数据类里的格式，然后执行“补签”操作
                        val pickedDate = Instant.ofEpochMilli(pickedDateMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val nowDate = LocalDate.now()
                        val period: Period = Period.between(pickedDate, nowDate)

                        val habitMeta = GlobalUtils.parseHabitMetaData(clickedItem.note)

                        val shouldPlusOne = when (habitMeta.frequencyType) {
                            DAILY -> period.days < 1
                            WEEKLY -> period.days < 7
                            MONTHLY -> period.months < 1
                            TOTAL -> true
                        }

                        val updatedNote = updateNoteWithDate(clickedItem, pickedDate)

                        val updatedHabit = clickedItem.copy(
                            note = updatedNote,
                            habitCount = clickedItem.habitCount + if (shouldPlusOne) 1 else 0,
                            habitTotalCount = clickedItem.habitTotalCount + 1
                        )

                        onRetroCheckSuccess(clickedItem, habitMeta, pickedDate)

//                        databaseHelper.updateDDL(updatedHabit)
                        DDLRepository().updateDDL(updatedHabit)

                        viewModel.loadData(viewModel.currentType)
                    }
                    return
                }

                pauseRefresh = true

                val intent = DeadlineDetailActivity.newIntent(this@MainActivity, clickedItem).apply {
                    putExtra("EXTRA_APP_COLOR_SCHEME", materialColorScheme)
                }
                startActivity(intent)
                pauseRefresh = false
            }
        })

        adapter.multiSelectListener = object : CustomAdapter.MultiSelectListener {
            override fun onSelectionChanged(selectedCount: Int) {
                showBottomBar()
                switchAppBarStatus(selectedCount == 0)
                if (selectedCount != 0) {
                    excitementText.text = getString(R.string.selected_items, selectedCount)
                } else {
                    updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                }
            }
        }

        adapter.onCheckInGlobalListener = object : CustomAdapter.OnCheckInGlobalListener {
            override fun onCheckInFailedGlobal(context: Context, habitItem: DDLItem) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.snackbar_already_checkin),
                    Toast.LENGTH_SHORT).show()
            }

            override fun onCheckInSuccessGlobal(context: Context, habitItem: DDLItem, habitMeta: HabitMetaData) {
                GlobalUtils.triggerVibration(this@MainActivity, 100)

                val count = habitItem.habitCount
                val frequency = habitMeta.frequency

                if (habitMeta.frequencyType == DeadlineFrequency.DAILY) {
                    Log.d("Count", count.toString())
                    if (count >= frequency) {
                        if (GlobalUtils.fireworksOnFinish) { konfettiViewMain.start(PartyPresets.festive()) }
                    }
                } else {
                    if (GlobalUtils.fireworksOnFinish) { konfettiViewMain.start(PartyPresets.festive()) }
                }

                val snackBarParent = if (isBottomBarVisible)
                        viewHolderWithAppBar
                    else viewHolderWithNoAppBar

                val snackbar = Snackbar.make(snackBarParent, getString(R.string.habit_success), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo)) {
                        val todayStr = LocalDate.now().toString()
                        // 解析 note JSON
                        val json = JSONObject(habitItem.note ?: "{}")
                        val datesArray = json.optJSONArray("completedDates") ?: JSONArray()
                        // 从末尾遍历并移除今日日期
                        for (i in datesArray.length() - 1 downTo 0) {
                            if (datesArray.optString(i) == todayStr) {
                                datesArray.remove(i)
                            }
                        }
                        json.put("completedDates", datesArray)
                        val revertedNoteJson = json.toString()
                        val revertedHabit = habitItem.copy(
                            note = revertedNoteJson,
                            habitCount = habitItem.habitCount - 1
                        )
                        DDLRepository().updateDDL(revertedHabit)
                        viewModel.loadData(currentType)
                    }.setAnchorView(bottomAppBar)

                val bg = snackbar.view.background
                if (bg is MaterialShapeDrawable) {
                    snackbar.view.background = bg.apply {
                        shapeAppearanceModel = shapeAppearanceModel
                            .toBuilder()
                            .setAllCornerSizes(16f.dpToPx())
                            .build()
                    }
                }
                snackbar.show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.IO) {
                DDLRepository().syncNow()
                refreshData()
            }
        }

        // 添加滑动特效
        // 设置ItemTouchHelper
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            // 定义画笔和图标
            private val paint = Paint()
            private val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete) // 🗑图标资源
            private val checkIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_check)   // ✅图标资源
            private val iconMargin = resources.getDimension(R.dimen.icon_margin).toInt()
            private val cornerRadius = resources.getDimension(R.dimen.item_corner_radius) // 24dp圆角

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (adapter.isMultiSelectMode) {
                    adapter.notifyItemChanged(viewHolder.adapterPosition)
                    return
                }

                if (currentType == DeadlineType.HABIT) {
                    return
                }

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        GlobalUtils.triggerVibration(this@MainActivity, 200)
                        adapter.onSwipeLeft(viewHolder.adapterPosition)
                    }
                    ItemTouchHelper.RIGHT -> {
                        GlobalUtils.triggerVibration(this@MainActivity, 100)
                        adapter.onSwipeRight(viewHolder.adapterPosition)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                if (adapter.isMultiSelectMode || currentType == DeadlineType.HABIT) {
                    return
                }
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                val horizontalPadding = 4f.dpToPx()

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val path = Path()

                    // 左滑：绘制低饱和度红色背景和🗑图标
                    if (dX < 0) {
                        paint.color = "#FFEBEE".toColorInt() // 低饱和度红色

                        val background = RectF(
                            itemView.right + dX + horizontalPadding,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat()
                        )

                        path.addRoundRect(background, cornerRadius, cornerRadius, Path.Direction.CW)
                        c.drawPath(path, paint)

                        deleteIcon?.let {
                            val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                            val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                            val iconRight = itemView.right - iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight

                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    }

                    // 右滑：绘制低饱和度绿色背景和✅图标
                    if (dX > 0) {
                        paint.color = "#E8F5E9".toColorInt() // 低饱和度绿色

                        val background = RectF(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            itemView.left + dX - horizontalPadding,
                            itemView.bottom.toFloat()
                        )

                        path.addRoundRect(background, cornerRadius, cornerRadius, Path.Direction.CW)
                        c.drawPath(path, paint)

                        checkIcon?.let {
                            val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                            val iconBottom = iconTop + it.intrinsicHeight

                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        addDDLLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // 更新数据
                viewModel.loadData(currentType)
            }
        }
        // 添加新事件按钮
        addEventButton.setOnClickListener {
            val intent = Intent(this, AddDDLActivity::class.java).apply {
                putExtra("EXTRA_CURRENT_TYPE", if (currentType == DeadlineType.TASK) 0 else 1)
            }
            addDDLLauncher.launch(intent)
        }

        settingsButton.setOnClickListener {
//            Log.d("MainActivity", "Settings triggered")
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        titleBar = findViewById(R.id.titleBar)
        excitementText = findViewById(R.id.excitementText)

        // 获取变量
        isFireworksAnimEnable = GlobalUtils.fireworksOnFinish

        // 检查鼓励语句开关状态
        updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)

        /* v2.0 added */
        bottomAppBar = findViewById(R.id.bottomAppBar)
        bottomBarContainer = findViewById(R.id.bottomBarContainer)

        // 初始化新搜索控件（覆盖层）
        searchOverlay = findViewById(R.id.searchOverlay)
        searchInputLayout = findViewById(R.id.searchInputLayout)
        searchEditText = findViewById(R.id.searchEditText)

        bottomAppBar.navigationIcon = if (GlobalUtils.deepSeekEnable)
            ContextCompat.getDrawable(this, R.drawable.ic_deepseek)
        else null
        bottomAppBar.setNavigationOnClickListener {
            showAgentOverlay()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val filter = SearchFilter.parse(s.toString())

                viewModel.filterData(filter, currentType)
            }
        })

        // 返回图标点击事件：隐藏搜索覆盖层
        searchInputLayout.setStartIconOnClickListener {
            searchEditText.text?.clear()
            viewModel.loadData(currentType)
            hideSearchOverlay()
        }

        bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.chart -> {

                    val intent = OverviewActivity.newIntent(this, materialColorScheme)

                    startActivity(intent)
                    true
                }
                R.id.archive -> {
                    val intent = Intent(this, ArchiveActivity::class.java)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                        startActivity(intent, options)
                    } else {
                        startActivity(intent)
                    }
                    true
                }
                R.id.filter -> {
                    val options = arrayOf(
                        resources.getString(R.string.filter_dialog_default),
                        resources.getString(R.string.filter_dialog_name),
                        resources.getString(R.string.filter_dialog_start_time),
                        resources.getString(R.string.filter_dialog_elapse_time)
                    )
                    var selectedItem = GlobalUtils.filterSelection

                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.filter_dialog_title)
                        .setSingleChoiceItems(options, selectedItem) { dialog, which ->
                            // 保存选中的项索引
                            selectedItem = which
                        }
                        .setPositiveButton(R.string.accept) { dialog, which ->
                            if (selectedItem != -1) {
                                GlobalUtils.filterSelection = selectedItem
                                viewModel.loadData(currentType)
                            } else {
                                Toast.makeText(this, getString(R.string.none_selected), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    true
                }
                R.id.delete -> {
                    if (adapter.selectedPositions.isNotEmpty()) {
                        GlobalUtils.triggerVibration(this@MainActivity, 200)
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle(R.string.alert_delete_title)
                            .setMessage(R.string.alert_delete_message)
                            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                                // 取消删除，刷新界面
                                adapter.notifyDataSetChanged()
                            }
                            .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                                // 根据选中项进行删除，先复制列表防止修改集合时出错
                                val positionsToDelete = adapter.selectedPositions.toList().sortedDescending()
                                for (position in positionsToDelete) {
                                    val item = adapter.itemList[position]
                                    DDLRepository().deleteDDL(item.id)
                                    DeadlineAlarmScheduler.cancelAlarm(applicationContext, item.id)
                                }
                                viewModel.loadData(currentType)
                                Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()

                                switchAppBarStatus(true)
                                updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                            }
                            .setOnCancelListener {
                                adapter.notifyDataSetChanged()
                            }
                            .show()

                        decideShowEmptyNotice()
                        true
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.please_select_delete_first), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.done -> {
                    if (adapter.selectedPositions.isNotEmpty()) {
                        if (currentType == DeadlineType.HABIT) {
                            GlobalUtils.triggerVibration(this@MainActivity, 100)
                            val positionsToUpdate = adapter.selectedPositions.toList()
                            for (position in positionsToUpdate) {
                                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                                viewHolder?.let {
                                    // 获取按钮并执行点击操作
                                    val button = it.itemView.findViewById<MaterialButton>(R.id.checkButton)
                                    button.performClick()
                                }
                            }

                            decideShowEmptyNotice()
                            // 清除多选状态

                            switchAppBarStatus(true)
                            updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                            true
                        } else {
                            GlobalUtils.triggerVibration(this@MainActivity, 100)
                            val positionsToUpdate = adapter.selectedPositions.toList()
                            for (position in positionsToUpdate) {
                                val item = adapter.itemList[position]
                                item.isCompleted = true
                                item.completeTime = LocalDateTime.now().toString()
                                DDLRepository().updateDDL(item)
                            }
                            viewModel.loadData(currentType)
                            Toast.makeText(
                                this@MainActivity,
                                R.string.toast_finished,
                                Toast.LENGTH_SHORT
                            ).show()

                            decideShowEmptyNotice()
                            // 清除多选状态

                            switchAppBarStatus(true)
                            updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                            true
                        }
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.please_select_done_first), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.archiving -> {
                    if (adapter.selectedPositions.isNotEmpty()) {
                        val positionsToUpdate = adapter.selectedPositions.toList()
                        var count = 0
                        for (position in positionsToUpdate) {
                            val item = adapter.itemList[position]
                            if (item.isCompleted) {
                                item.isArchived = true
                                DDLRepository().updateDDL(item)
                                count++
                            }
                        }
                        viewModel.loadData(currentType)
                        Toast.makeText(
                            this@MainActivity,
                            resources.getString(R.string.toast_archived, count),
                            Toast.LENGTH_SHORT
                        ).show()
                        decideShowEmptyNotice()
                        // 清除多选状态

                        switchAppBarStatus(true)
                        updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                        true
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.please_select_done_first), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.star -> {
                    if (adapter.selectedPositions.isNotEmpty()) {
                        val positionsToUpdate = adapter.selectedPositions.toList()
                        var count = 0
                        for (position in positionsToUpdate) {
                            val item = adapter.itemList[position]
                            item.isStared = !item.isStared
                            DDLRepository().updateDDL(item)
                            count++
                        }
                        viewModel.loadData(currentType)
                        Toast.makeText(
                            this@MainActivity,
                            resources.getString(R.string.toast_stared),
                            Toast.LENGTH_SHORT
                        ).show()
                        decideShowEmptyNotice()
                        // 清除多选状态

                        switchAppBarStatus(true)
                        updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                        true
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.please_select_done_first), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }

        onBackPressedDispatcher.addCallback {
            if (searchOverlay.visibility == View.VISIBLE) {
                searchEditText.text?.clear()
                viewModel.loadData(currentType)
                hideSearchOverlay()
            }
            else if (adapter.isMultiSelectMode) {
                switchAppBarStatus(true)
                updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
            }
            else {
                return@addCallback
            }
        }

        // 滑动隐藏bottomAppBar
        bottomBlur = findViewById(R.id.bottomBlur)
        bottomBarBackground = findViewById(R.id.bottomBarBackground)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var accumulatedDy = 0
            private var lastDirection = 0 // 0: 初始, 1: 向下, -1: 向上
            private val scrollThreshold = 20 // 触发阈值

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy == 0) return

                val currentDirection = if (dy > 0) 1 else -1

                // 方向变化时重置累计距离
                if (currentDirection != lastDirection) {
                    accumulatedDy = 0
                    lastDirection = currentDirection
                }

                // 累计滚动距离（取绝对值）
                accumulatedDy += Math.abs(dy)

                // 达到阈值时执行操作
                if (accumulatedDy >= scrollThreshold) {
                    if (currentDirection == 1) { // 向下滚动
                        if (isBottomBarVisible && !adapter.isMultiSelectMode) {
                            hideBottomBar()
                            isBottomBarVisible = false
                        }
                    } else { // 向上滚动
                        if (!isBottomBarVisible && !adapter.isMultiSelectMode) {
                            showBottomBar()
                            isBottomBarVisible = true
                        }
                    }
                    accumulatedDy = 0 // 重置累计距离防止重复触发
                }
            }
        })

        dataOverlay = findViewById(R.id.dataOverlay)
        refreshIndicator = findViewById(R.id.refreshIndicator)
        searchButton = findViewById(R.id.searchButton)

        swipeRefreshLayout.setColorSchemeColors(
            getThemeColor(androidx.appcompat.R.attr.colorPrimary),
            getMaterialThemeColor(com.google.android.material.R.attr.colorSecondary),
            getMaterialThemeColor(com.google.android.material.R.attr.colorTertiary)
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refreshState.collect { state ->
                    when (state) {
                        is MainViewModel.RefreshState.Loading -> {
                            if (!state.silent) {
                                swipeRefreshLayout.isRefreshing = true
                            }
                        }
                        is MainViewModel.RefreshState.Success -> {
                            swipeRefreshLayout.isRefreshing = false
                            decideShowEmptyNotice()
                            hideOverlay()
                        }
                        else -> {}
                    }
                }
            }
        }

        handler.postDelayed(autoRefreshRunnable, 30000)

        titleBar.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
        excitementText.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        searchButton.setOnClickListener {
            showSearchOverlay()
        }

        addEventButton.stateListAnimator = null

        setupTabs()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val info = UpdateManager.fetchUpdateInfo(this@MainActivity)
                Log.d("UpdateInfo", info.toString())
                if (UpdateManager.isNewer(info.currentVersion, info.latestVersion)) {
                    withContext(Dispatchers.Main) {
                        showUpdatePrompt(info)
                    }
                }
            } catch (e: Exception) {
                // 忽略错误或者 log
                Log.w("UpdateInfo", e)
            }
        }

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)

        if (!GlobalUtils.permissionSetupDone) {
            showFirstTimeSetupDialog()
        } else {
            runPostSetupInitialization()
        }

        if (intent?.getBooleanExtra("EXTRA_SHOW_SEARCH", false) == true) {
            showSearchOverlay()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false) == true) {
            showSearchOverlay()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoRefreshRunnable)
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    override fun onStop() {
        // 触发小组件更新
        updateWidget()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasCheckedInitialClipboard) {
            hasCheckedInitialClipboard = true
            checkClipboardAndPrompt()
        }
    }

    private fun checkClipboardAndPrompt() {
        if (!GlobalUtils.clipboardEnable) return

        clipboardManager.primaryClip?.let { clip ->
            if (clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString()
                if (text.isNotBlank()) {
                    triggerFeatureBasedOnClipboard(text)
                }
            }
        }
    }

    private fun updateWidget() {
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

    private fun refreshData() {
        viewModel.loadData(currentType)
    }

    private fun updateTitleAndExcitementText(isEnabled: Boolean) {
        if (isEnabled) {
            Log.d("MainActivity", "Enabled here")
            titleBar.textSize = 24f // 调整 Deadliner 尺寸
            excitementText.visibility = TextView.VISIBLE

            // 随机选择数组中的一条语句
            val excitementArray = resources.getStringArray(R.array.excitement_array)
            val randomIndex = (excitementArray.indices).random()
            excitementText.text = excitementArray[randomIndex]
        } else {
            titleBar.textSize = 32f // 设置为默认大小
            excitementText.visibility = TextView.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ADD_DDL && resultCode == RESULT_OK) {
            // 刷新数据
            viewModel.loadData(currentType)
        }

        decideShowEmptyNotice()
    }

    override fun onSwipeRight(position: Int) {
        val item = adapter.itemList[position]
        item.isCompleted = !item.isCompleted
        item.completeTime = if (item.isCompleted) {
            LocalDateTime.now().toString()
        } else {
            ""
        }
        DDLRepository().updateDDL(item)
        viewModel.loadData(currentType)
        if (item.isCompleted) {
            if (isFireworksAnimEnable) { konfettiViewMain.start(PartyPresets.festive()) }
            Toast.makeText(this, R.string.toast_finished, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.toast_definished, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSwipeLeft(position: Int) {
        pauseRefresh = true
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.alert_delete_title)
            .setMessage(R.string.alert_delete_message)
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                adapter.notifyItemChanged(position) // 取消删除，刷新该项
                pauseRefresh = false
            }
            .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                val item = adapter.itemList[position]
                DDLRepository().deleteDDL(item.id)
                DeadlineAlarmScheduler.cancelAlarm(applicationContext, item.id)
                viewModel.loadData(currentType)
                Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                decideShowEmptyNotice()
                pauseRefresh = false
            }
            .setOnCancelListener {
                adapter.notifyItemChanged(position) // 取消删除，刷新该项
                pauseRefresh = false
            }
            .show()
    }

    private fun decideShowEmptyNotice() {
        finishNotice.visibility = if (viewModel.isEmpty() == true) {
            View.VISIBLE
        } else {
            View.GONE
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
        Log.d("ThemeColor", "getColor $attributeId: ${typedValue.data.toHexString()}")
        return typedValue.data
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

    override fun onResume() {
        super.onResume()
        updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
        isFireworksAnimEnable = GlobalUtils.fireworksOnFinish
        switchAppBarStatus(true)
        viewModel.loadData(currentType)
        decideShowEmptyNotice()

        lifecycleScope.launch(Dispatchers.IO) {
            DDLRepository().syncNow()
            refreshData()
        }

        if (searchOverlay.visibility == View.VISIBLE) {
            val s = searchEditText.text
            val filter = SearchFilter.parse(s.toString())

            viewModel.filterData(filter, currentType)
        }

        addEventButton.apply {
            isPressed = false
            clearFocus()
            refreshDrawableState()
        }
    }

    companion object {
        private const val REQUEST_CODE_ADD_DDL = 1
        const val ANIMATION_DURATION = 160L
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 0x2001
        private const val REQUEST_CODE_CALENDAR_PERMISSION = 0x2002
    }

    /* New to v2.0 */
    private var currentAppBarIsPrimary: Boolean? = null

    private fun switchAppBarStatus(isPrimary: Boolean) {
        if (currentAppBarIsPrimary == isPrimary) return

        if (!isPrimary) {
            bottomAppBar.performHide()

            bottomAppBar.postDelayed({
                bottomAppBar.replaceMenu(
                    if (currentType == DeadlineType.TASK) R.menu.bottom_utility_bar
                    else R.menu.bottom_utility_bar_alt
                )
                switchAppBarMenuStatus(false)
                bottomAppBar.performShow()
            }, ANIMATION_DURATION)

            addEventButton.animate().alpha(0f).setDuration(150).withEndAction {
                // 切换图标
                addEventButton.setImageResource(R.drawable.ic_edit)
                addEventButton.animate().alpha(1f).setDuration(150).start()
            }.start()

            addEventButton.setOnClickListener {
                // 修改操作
                if (adapter.selectedPositions.isNotEmpty()) {
                    // 获取第一个选中的位置
                    val firstPosition = adapter.selectedPositions.first()
                    val clickedItem = adapter.itemList[firstPosition]
                    val editDialog = EditDDLFragment(clickedItem) { updatedDDL ->
                        DDLRepository().updateDDL(updatedDDL)
                        viewModel.loadData(currentType)
                        // 清除多选状态
                        adapter.selectedPositions.clear()
                        adapter.isMultiSelectMode = false
                    }
                    editDialog.show(supportFragmentManager, "EditDDLFragment")
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.please_select_edit_first), Toast.LENGTH_SHORT).show()
                }
                pauseRefresh = false
            }
        } else {
            bottomAppBar.performHide()

            bottomAppBar.postDelayed({
                bottomAppBar.replaceMenu(R.menu.bottom_app_bar)
                switchAppBarMenuStatus(true)
                bottomAppBar.performShow()
            }, ANIMATION_DURATION)

            adapter.selectedPositions.clear()
            adapter.isMultiSelectMode = false
            viewModel.loadData(currentType)

            addEventButton.animate().alpha(0f).setDuration(150).withEndAction {
                // 切换图标
                addEventButton.setImageResource(R.drawable.ic_add)
                addEventButton.animate().alpha(1f).setDuration(150).start()
            }.start()
            addEventButton.setOnClickListener {
                val intent = Intent(this, AddDDLActivity::class.java).apply {
                    putExtra("EXTRA_CURRENT_TYPE", if (currentType == DeadlineType.TASK) 0 else 1)
                }
                addDDLLauncher.launch(intent)
            }
        }
        currentAppBarIsPrimary = isPrimary
    }

    private fun switchAppBarMenuStatus(isPrimary: Boolean) {
        if (!isPrimary) {
            bottomAppBar.setNavigationIcon(R.drawable.ic_back)
            bottomAppBar.setNavigationOnClickListener {
                // 撤销多选
                adapter.isMultiSelectMode = false
                adapter.selectedPositions.clear()
                viewModel.loadData(currentType)
                switchAppBarStatus(true)
                updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
            }
        } else {
            bottomAppBar.navigationIcon = if (GlobalUtils.deepSeekEnable)
                ContextCompat.getDrawable(this, R.drawable.ic_deepseek)
            else null
            bottomAppBar.setNavigationOnClickListener {
                showAgentOverlay()
            }
        }
    }

    /**
     * 显示搜索覆盖层并打开软键盘
     */
    private fun showSearchOverlay() {
        TransitionManager.beginDelayedTransition(searchOverlay, AutoTransition())
        searchOverlay.visibility = View.VISIBLE
        searchEditText.requestFocus()

        bottomBarContainer.visibility = View.GONE
        hideBottomBar()
        // 打开软键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * 隐藏搜索覆盖层并关闭软键盘
     */
    private fun hideSearchOverlay() {
        TransitionManager.beginDelayedTransition(searchOverlay, AutoTransition())
        searchOverlay.visibility = View.GONE

        showBottomBar()
        bottomBarContainer.visibility = View.VISIBLE
        // 隐藏软键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.task)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.habit)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = when (tab?.position) {
                    0 -> DeadlineType.TASK
                    1 -> DeadlineType.HABIT
                    else -> DeadlineType.TASK
                }
                showOverlay()

                adapter.selectedPositions.clear()
                adapter.isMultiSelectMode = false
                updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                switchAppBarStatus(true)

                adapter.updateType(currentType)
                viewModel.loadData(currentType)

                if (searchOverlay.visibility == View.VISIBLE) {
                    val s = searchEditText.text
                    val filter = SearchFilter.parse(s.toString())

                    viewModel.filterData(filter, currentType)
                }

                handler.postDelayed({
                    showBottomBar()
                    hideOverlay()
                }, 300)
            }
            // 其他方法保持不变
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // **1.** 先更新一次（防止初始时没有 Badge）
        updateTabBadges(mapOf(
            DeadlineType.TASK to viewModel.dueSoonCount(DeadlineType.TASK),
            DeadlineType.HABIT to viewModel.dueSoonCount(DeadlineType.HABIT)
        ))

        // **2.** 订阅 ViewModel 中即将到期数量的 LiveData／Flow
        viewModel.dueSoonCounts.observe(this) { counts ->
            // counts: Map<DeadlineType, Int>
            updateTabBadges(counts)
        }
    }

    /**
     * @param counts 一个类型到“即将到期 DDL 数量”的映射
     */
    private fun updateTabBadges(counts: Map<DeadlineType, Int>) {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        fun dp2px(dp: Int): Int =
            (dp * resources.displayMetrics.density).toInt()


        // TASK 对应 position 0，HABIT 对应 position 1
        counts.forEach { (type, num) ->
            val index = when (type) {
                DeadlineType.TASK  -> 0
                DeadlineType.HABIT -> 1
            }
            tabLayout.getTabAt(index)?.let { tab ->
                if (num > 0 && GlobalUtils.nearbyTasksBadge) {
                    tab.orCreateBadge.apply {
                        badgeGravity = BadgeDrawable.TOP_END

                        if (GlobalUtils.nearbyDetailedBadge) {
                            // 显示数字
                            number = num
                            horizontalOffset = dp2px(0)
                            verticalOffset = dp2px(12)
                            isVisible = true
                        } else {
                            // 清除数字，变成一个纯圆点
                            clearNumber()
                            horizontalOffset = dp2px(4)
                            verticalOffset = dp2px(6)
                            isVisible = true
                        }
                    }
                } else {
                    tab.removeBadge()
                }
            }
        }
    }

    private fun showOverlay() {
        dataOverlay.alpha = 1f
        handler.postDelayed({
            refreshIndicator.alpha = 1f
        }, 100)
    }

    private fun hideOverlay() {
        refreshIndicator.alpha = 0f
        dataOverlay.animate()
            .alpha(0f)
            .setDuration(500)
    }


    private fun hideBottomBar() {
        if (!isBottomBarVisible) return
        isBottomBarVisible = false

        // 动画1：隐藏 BottomAppBar
        bottomAppBar.animate()
            .translationY(bottomAppBar.height.toFloat())
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .start()

        // 动画2：渐隐背景层
        bottomBarBackground.animate()
            .alpha(0f)
            .setDuration(100)
            .start()

        bottomBlur.animate()
            .alpha(0f)
            .setDuration(100)
            .start()

        handler.postDelayed({
            val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)
            val colorContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)
        }, 200)
    }

    private fun showBottomBar() {
        if (isBottomBarVisible) return
        isBottomBarVisible = true

        // 动画1：显示 BottomAppBar
        bottomAppBar.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 动画2：恢复背景层
        bottomBarBackground.animate()
            .alpha(1f)
            .setDuration(100)
            .start()

        bottomBlur.animate()
            .alpha(1f)
            .setDuration(100)
            .start()

        handler.postDelayed({
            val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)
            val colorContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)
        }, 0)
    }

    private fun initializeNotificationSystem() {
        NotificationUtil.createNotificationChannels(this)
    }

    /**************************************
     * 权限管理系统
     **************************************/
    private fun checkCriticalPermissions() {
        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }

        // 电池优化白名单
        if (!isIgnoringBatteryOptimizations()) {
            showBatteryOptimizationDialog()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d("Error", "error")
                }
            }
        }
    }

    /**************************************
     * 数据恢复系统
     **************************************/
    private fun restoreAllAlarms() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allDDLs = DDLRepository().getAllDDLs()
            allDDLs.filter { !it.isCompleted }.forEach { ddl ->
                DeadlineAlarmScheduler.scheduleExactAlarm(applicationContext, ddl)
            }
        }
    }

    private fun showBatteryOptimizationDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle(getString(R.string.battery_optimization_title))
            setMessage(getString(R.string.battery_optimization_message))
            setPositiveButton(getString(R.string.goto_setting)) { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                })
            }
            setCancelable(false)
        }.show()
    }

    /**************************************
     * 工具方法
     **************************************/
    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun onCalendarPermissionDenied() {
        // 提示用户权限被拒绝
        Toast.makeText(this, getString(R.string.permission_calendar_denied), Toast.LENGTH_LONG).show()

        // 可选：引导用户前往应用设置手动授权
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun Float.dpToPx(): Float =
        this * Resources.getSystem().displayMetrics.density + 0.5f

    private val notifyLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        dialogFlipper?.showNext()  // 或者其他逻辑
    }

    private val calendarLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        if (!results.values.all { it }) {
            // 权限被拒绝
            onCalendarPermissionDenied()
        }
        dialogFlipper?.showNext()
    }

    private fun showFirstTimeSetupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_first_time_setup, null)
        dialogFlipper = dialogView.findViewById<ViewFlipper>(R.id.vf_steps)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        // Step1 按钮
        dialogView.findViewById<Button>(R.id.btn_next1).setOnClickListener {
            // 请求通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                dialogFlipper?.showNext()
            }
        }
        dialogView.findViewById<Button>(R.id.btn_skip1).setOnClickListener {
            dialogFlipper?.showNext()
        }

        // Step2 按钮
        dialogView.findViewById<Button>(R.id.btn_next2).setOnClickListener {
            calendarLauncher.launch(arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            ))
        }
        dialogView.findViewById<Button>(R.id.btn_skip2).setOnClickListener {
            dialogFlipper?.showNext()
        }

        dialogView.findViewById<Button>(R.id.btn_next3).setOnClickListener {
            Toast.makeText(this, getString(R.string.loading_wiki), Toast.LENGTH_LONG).show()

            val webViewView = layoutInflater.inflate(R.layout.dialog_webview, null)
            val webDialog = MaterialAlertDialogBuilder(this)
                .setView(webViewView)
                .setCancelable(true)
                .create()

            val webView = webViewView.findViewById<WebView>(R.id.setup_webview)
            val li = webViewView.findViewById<LoadingIndicator>(R.id.loading_indicator)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    li.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    li.visibility = View.GONE
                }
            }

            webView.settings.javaScriptEnabled = true

            webView.loadUrl(GlobalUtils.generateWikiForSpecificDevice())

            webDialog.setOnDismissListener {
                if (!isIgnoringBatteryOptimizations()) {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:$packageName".toUri()
                    })
                }

                dialogFlipper?.showNext()
            }

            webDialog.show()
        }
        dialogView.findViewById<Button>(R.id.btn_skip3).setOnClickListener {
            dialogFlipper?.showNext()
        }

        // Step4 完成
        dialogView.findViewById<Button>(R.id.btn_done).setOnClickListener {
            GlobalUtils.permissionSetupDone = true
            dialog.dismiss()
            runPostSetupInitialization()
        }
    }

    private fun runPostSetupInitialization() {
        initializeNotificationSystem()
        GlobalUtils.setAlarms(databaseHelper, applicationContext)
        DeadlineAlarmScheduler.scheduleDailyAlarm(applicationContext)
        checkCriticalPermissions()
        restoreAllAlarms()
    }

    private fun onRetroCheckSuccess(habitItem: DDLItem, habitMeta: HabitMetaData, retroDate: LocalDate) {
        GlobalUtils.triggerVibration(this@MainActivity, 100)

        val count = habitItem.habitCount
        val frequency = habitMeta.frequency

        if (habitMeta.frequencyType == DeadlineFrequency.DAILY) {
            Log.d("Count", count.toString())
            if (count >= frequency) {
                if (GlobalUtils.fireworksOnFinish) { konfettiViewMain.start(PartyPresets.festive()) }
            }
        } else {
            if (GlobalUtils.fireworksOnFinish) { konfettiViewMain.start(PartyPresets.festive()) }
        }

        val snackBarParent = if (isBottomBarVisible)
            viewHolderWithAppBar
        else viewHolderWithNoAppBar

        val snackbar = Snackbar.make(snackBarParent, getString(R.string.habit_success_retro), Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                val retroDateStr = retroDate.toString()
                // 解析 note JSON
                val json = JSONObject(habitItem.note ?: "{}")
                val datesArray = json.optJSONArray("completedDates") ?: JSONArray()
                // 从末尾遍历并移除今日日期
                for (i in datesArray.length() - 1 downTo 0) {
                    if (datesArray.optString(i) == retroDateStr) {
                        datesArray.remove(i)
                    }
                }
                json.put("completedDates", datesArray)
                val nowDate = LocalDate.now()
                val period: Period = Period.between(retroDate, nowDate)

                val shouldPlusOne = when (habitMeta.frequencyType) {
                    DAILY -> period.days < 1
                    WEEKLY -> period.days < 7
                    MONTHLY -> period.months < 1
                    TOTAL -> true
                }
                val revertedNoteJson = json.toString()
                val revertedHabit = habitItem.copy(
                    note = revertedNoteJson,
                    habitCount = habitItem.habitCount - if (shouldPlusOne) 1 else 0
                )
                DDLRepository().updateDDL(revertedHabit)
                viewModel.loadData(currentType)
            }.setAnchorView(bottomAppBar)

        val bg = snackbar.view.background
        if (bg is MaterialShapeDrawable) {
            snackbar.view.background = bg.apply {
                shapeAppearanceModel = shapeAppearanceModel
                    .toBuilder()
                    .setAllCornerSizes(16f.dpToPx())
                    .build()
            }
        }
        snackbar.show()
    }

    private fun showAgentOverlay(initialText: String = "") {
        val composeOverlay = findViewById<ComposeView>(R.id.agentCompose)

        applyBackgroundSeparation(true)

        composeOverlay.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        )
        composeOverlay.visibility = View.VISIBLE

        composeOverlay.setContent {
            DeadlinerTheme {
                DeepseekOverlayHost(
                    initialText = initialText,
                    onAddDDL = { intent -> addDDLLauncher.launch(intent) },
                    onRemoveFromWindow = {
                        // 只有当退场动画播完才真正移除
                        applyBackgroundSeparation(false)
                        composeOverlay.disposeComposition()
                        composeOverlay.visibility = View.GONE
                    }
                )
            }
        }
    }

    private fun applyBackgroundSeparation(on: Boolean) {
        val bg = findViewById<View>(R.id.backgroundHost) ?: return

        if (on) {
            hideBottomBar()
            val blur = RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
            val cm = ColorMatrix().apply { setSaturation(0.5f) }
            val cf = ColorMatrixColorFilter(cm)
            val chained = RenderEffect.createChainEffect(blur, RenderEffect.createColorFilterEffect(cf))
            bg.setRenderEffect(chained)
            handler.postDelayed({ bg.animate().scaleX(0.98f).scaleY(0.98f).setDuration(280).start() }, 200)
        } else {
            showBottomBar()
            bg.setRenderEffect(null)
            bg.animate().scaleX(1f).scaleY(1f).setDuration(320).start()
        }
    }

    private fun showUpdatePrompt(info: UpdateInfo) {
        val markwon = Markwon.create(this@MainActivity)
        val releaseNotes = markwon.toMarkdown(info.releaseNotes)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.find_updates, info.latestVersion))
            .setMessage(releaseNotes)
            .setPositiveButton(R.string.goto_update) { _, _ ->
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra(EXTRA_INITIAL_ROUTE, SettingsRoute.Update.route)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.later, null)
            .show()
    }

    private fun normalizeRootInsets() {
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root, null)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            // 只应用 top inset，忽略 bottom
            v.setPadding(v.paddingLeft, status.top, v.paddingRight, 0)
            insets // 不消费，让子控件能继续收到
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
        normalizeRootInsets()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
        normalizeRootInsets()
    }
}

