package com.aritxonly.deadliner

import ApkDownloaderInstaller
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.work.*
import com.aritxonly.deadliner.notification.NotificationUtil
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.xml.KonfettiView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.toHexString
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import android.Manifest
import com.google.android.material.materialswitch.MaterialSwitch

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

    private lateinit var cloudButton: ImageButton

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

    // 定义权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "通知权限被拒绝，请在设置中手动开启", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 跟随主题色
        Log.d("MainActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
        DeadlineAlarmScheduler.cancelDailyAlarm(applicationContext)

//        DynamicColors.applyToActivitiesIfAvailable(application)

        DynamicColors.applyToActivityIfAvailable(this)

        GlobalUtils.decideHideFromRecent(this, this@MainActivity)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val colorContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)

        Log.d("MainActivity", "colorSurface ${colorSurface.toHexString()}")
        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface), colorContainer)
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
                if (clickedItem.type == DeadlineType.HABIT) return

                pauseRefresh = true

                val myColorScheme = AppColorScheme(
                    primary = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimary),
                    onPrimary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnPrimary),
                    primaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimaryContainer),
                    surface = getMaterialThemeColor(com.google.android.material.R.attr.colorSurface),
                    onSurface = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSurface),
                    surfaceContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)
                )

                val intent = DeadlineDetailActivity.newIntent(this@MainActivity, clickedItem).apply {
                    putExtra("EXTRA_APP_COLOR_SCHEME", myColorScheme)
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
                    excitementText.text = "已选中 $selectedCount 项 Deadline"
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
                triggerVibration(this@MainActivity, 100)

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

                Snackbar.make(snackBarParent, "打卡成功 🎉", Snackbar.LENGTH_LONG)
                    .setAction("撤销") {
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
                        databaseHelper.updateDDL(revertedHabit)
                        viewModel.loadData(currentType)
                    }.setAnchorView(bottomAppBar).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
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
                        triggerVibration(this@MainActivity, 200)
                        adapter.onSwipeLeft(viewHolder.adapterPosition)
                    }
                    ItemTouchHelper.RIGHT -> {
                        triggerVibration(this@MainActivity, 100)
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
                isCurrentlyActive: Boolean
            ) {
                if (adapter.isMultiSelectMode || currentType == DeadlineType.HABIT) {
                    return
                }
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val path = Path()

                    // 左滑：绘制低饱和度红色背景和🗑图标
                    if (dX < 0) {
                        paint.color = Color.parseColor("#FFEBEE") // 低饱和度红色

                        val background = RectF(
                            itemView.right + dX,
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
                        paint.color = Color.parseColor("#E8F5E9") // 低饱和度绿色

                        val background = RectF(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            itemView.left + dX,
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
        bottomAppBar = findViewById(R.id.bottomAppBar)

        // 底部 AppBar 的搜索图标点击事件：显示搜索覆盖层
        bottomAppBar.setNavigationOnClickListener {
            showSearchOverlay()
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
                    val myColorScheme = AppColorScheme(
                        primary = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimary),
                        onPrimary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnPrimary),
                        primaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimaryContainer),
                        surface = getMaterialThemeColor(com.google.android.material.R.attr.colorSurface),
                        onSurface = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSurface),
                        surfaceContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainerLow)
                    )
                    Log.d("MainActivity", "surface=${myColorScheme.surface.toHexString()}")

                    val intent = OverviewActivity.newIntent(this, myColorScheme)

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
                                Toast.makeText(this, "未选择任何项", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    true
                }
                R.id.delete -> {
                    if (adapter.selectedPositions.isNotEmpty()) {
                        triggerVibration(this@MainActivity, 200)
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
                                    databaseHelper.deleteDDL(item.id)
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
                        Toast.makeText(this@MainActivity, "请先选择要删除的项目", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.done -> {
                    if (adapter.selectedPositions.isNotEmpty()) {
                        if (currentType == DeadlineType.HABIT) {
                            triggerVibration(this@MainActivity, 100)
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
                            triggerVibration(this@MainActivity, 100)
                            val positionsToUpdate = adapter.selectedPositions.toList()
                            for (position in positionsToUpdate) {
                                val item = adapter.itemList[position]
                                item.isCompleted = true
                                item.completeTime = LocalDateTime.now().toString()
                                databaseHelper.updateDDL(item)
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
                        Toast.makeText(this@MainActivity, "请先选择要标记为完成的项目", Toast.LENGTH_SHORT).show()
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
                                databaseHelper.updateDDL(item)
                                count++
                            }
                        }
                        viewModel.loadData(currentType)
                        Toast.makeText(
                            this@MainActivity,
                            "$count 项" + resources.getString(R.string.toast_archived),
                            Toast.LENGTH_SHORT
                        ).show()
                        decideShowEmptyNotice()
                        // 清除多选状态

                        switchAppBarStatus(true)
                        updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                        true
                    } else {
                        Toast.makeText(this@MainActivity, "请先选择要标记为完成的项目", Toast.LENGTH_SHORT).show()
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
                            databaseHelper.updateDDL(item)
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
                        Toast.makeText(this@MainActivity, "请先选择要标记为完成的项目", Toast.LENGTH_SHORT).show()
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
        cloudButton = findViewById(R.id.cloudButton)

        swipeRefreshLayout.setColorSchemeColors(
            getMaterialThemeColor(com.google.android.material.R.attr.colorPrimary),
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

        decideCloudStatus()
        cloudButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_cloud_settings, null)
            val switch = dialogView.findViewById<MaterialSwitch>(R.id.cloudSwitch)
            val inputServer = dialogView.findViewById<TextInputEditText>(R.id.inputServer)
            val inputPort = dialogView.findViewById<TextInputEditText>(R.id.inputPort)
            val inputToken = dialogView.findViewById<TextInputEditText>(R.id.inputToken)

            // 初始化值
            switch.isChecked = GlobalUtils.cloudSyncEnable
            inputServer.setText(GlobalUtils.cloudSyncServer ?: "")
            inputPort.setText(GlobalUtils.cloudSyncPort.toString())
            inputToken.setText(GlobalUtils.cloudSyncConstantToken ?: "")

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("云服务设置")
                .setView(dialogView)
                .setPositiveButton(R.string.save) { _, _ ->
                    GlobalUtils.cloudSyncEnable = switch.isChecked
                    GlobalUtils.cloudSyncServer = inputServer.text?.toString()
                    GlobalUtils.cloudSyncPort = inputPort.text?.toString()?.toIntOrNull()?:5000
                    GlobalUtils.cloudSyncConstantToken = inputToken.text?.toString()
                    decideCloudStatus()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        setupTabs()

        checkForUpdates()

        // 初始化通知系统并检查关键权限
        initializeNotificationSystem()
        GlobalUtils.setAlarms(databaseHelper, applicationContext)
        DeadlineAlarmScheduler.scheduleDailyAlarm(applicationContext)
        checkCriticalPermissions()
        // 恢复所有未完成DDL的闹钟
        restoreAllAlarms()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoRefreshRunnable)
        super.onDestroy()
    }

    override fun onStop() {
        // 触发小组件更新
        updateWidget()
        super.onStop()
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

    private fun checkForUpdates() {
        // GitHub Releases API 地址
        val url = "https://api.github.com/repos/AritxOnly/Deadliner/releases/latest"

        // 创建 OkHttp 客户端
        val client = OkHttpClient()

        // 创建请求
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace() // 网络请求失败时的处理
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val json = JSONObject(responseBody)

                        // 获取最新版本号
                        val latestVersion = json.getString("tag_name") // GitHub 上的版本标签
                        val releaseNotesMarkdown = json.getString("body") // 更新说明
                        val assetsArray = json.getJSONArray("assets")

                        val markwon = Markwon.create(this@MainActivity)
                        val releaseNotes = markwon.toMarkdown(releaseNotesMarkdown)

                        val downloadUrl: String?
                        if (assetsArray.length() > 0) {
                            downloadUrl = assetsArray.optJSONObject(0)?.optString("browser_download_url", "")
                            if (!downloadUrl.isNullOrEmpty()) {
                                Log.d("DownloadURL", "$downloadUrl")
                            } else {
                                Log.e("DownloadURL", "downloadUrl null")
                            }
                        } else {
                            Log.e("DownloadURL", "assets null")
                            return@let
                        }

                        // 获取本地版本号
                        val localVersion = packageManager.getPackageInfo(packageName, 0).versionName

                        // 比较版本号
                        if (isNewVersionAvailable(localVersion, latestVersion)) {
                            runOnUiThread {
                                downloadUrl?.let {
                                    showUpdateDialog(latestVersion, releaseNotes,
                                        it
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    // 判断是否有新版本（版本号格式：x.y.z）
    private fun isNewVersionAvailable(localVersion: String, latestVersion: String): Boolean {
        // 去掉前缀 'v'，确保格式为 x.y.z
        val cleanedLocalVersion = localVersion.removePrefix("v")
        val cleanedLatestVersion = latestVersion.removePrefix("v")

        val localParts = cleanedLocalVersion.split(".")
        val latestParts = cleanedLatestVersion.split(".")

        for (i in 0 until minOf(localParts.size, latestParts.size)) {
            val localPart = localParts[i].toIntOrNull() ?: 0
            val latestPart = latestParts[i].toIntOrNull() ?: 0

            if (localPart < latestPart) return true  // 有新版本
            if (localPart > latestPart) return false // 本地版本更新
        }

        // 如果最新版本的部分比本地多，例如 v1.2.3 -> v1.2.3.1，说明有新版本
        return latestParts.size > localParts.size
    }

    // 显示更新提示对话框
    private fun showUpdateDialog(version: String, releaseNotes: Spanned, downloadUrl: String) {
        val customTitleView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_title, null)
        customTitleView.findViewById<TextView>(R.id.dialogTitle).text = "发现新版本：$version"

        val dialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(customTitleView)
            .setMessage(releaseNotes)
            .setPositiveButton("更新") { _, _ ->
                val downloaderInstaller = ApkDownloaderInstaller(this)
                downloaderInstaller.downloadAndInstall(downloadUrl)
            }
            .setNeutralButton("下载") { _, _ ->
                // 打开浏览器下载最新版本
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intent)
            }
            .setNegativeButton("稍后再说", null)
            .show()
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
        databaseHelper.updateDDL(item)
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
                databaseHelper.deleteDDL(item.id)
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

    /*
    * 震动效果📳
    */
    fun triggerVibration(context: Context, duration: Long = 100) {
        if (!GlobalUtils.vibration) {
            return
        }

        val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26 及以上版本使用 VibrationEffect
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // API 25 及以下版本使用过时的 vibrate 方法
            vibrator.vibrate(duration)
        }
    }

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean, colorNavigationBar: Int) {
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

    /**
     * 带渐变动画的系统栏颜色设置
     * @param duration 动画时长（默认300ms）
     * @param interpolator 插值器（默认加速减速）
     */
    private fun setSystemBarColorsWithAnimation(
        targetStatusColor: Int,
        targetNavColor: Int,
        lightIcons: Boolean,
        duration: Long = 200
    ) {
        val window = this.window ?: return

        val interpolator = AccelerateDecelerateInterpolator()

        // 设置图标颜色（动画期间保持不变）
        setSystemBarIcons(lightIcons)

        // 状态栏颜色动画
        val statusAnimator = ValueAnimator.ofArgb(window.statusBarColor, targetStatusColor).apply {
            addUpdateListener { animator ->
                window.statusBarColor = animator.animatedValue as Int
            }
        }

        // 导航栏颜色动画
        val navAnimator = ValueAnimator.ofArgb(window.navigationBarColor, targetNavColor).apply {
            addUpdateListener { animator ->
                window.navigationBarColor = animator.animatedValue as Int
            }
        }

        // 创建动画集合
        AnimatorSet().apply {
            this.duration = duration
            this.interpolator = interpolator
            playTogether(statusAnimator, navAnimator)
            start()
        }
    }

    private fun setSystemBarIcons(lightIcons: Boolean) {
        val window = this.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                // 状态栏图标
                it.setSystemBarsAppearance(
                    if (lightIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                // 导航栏图标
                it.setSystemBarsAppearance(
                    if (lightIcons) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (lightIcons) {
                // 兼容旧版状态栏图标
                window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }.let { visibility ->
                // 兼容旧版导航栏图标（API 26+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    visibility or if (lightIcons)
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    else
                        visibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                } else {
                    visibility
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

//        GlobalUtils.setAlarms(databaseHelper, applicationContext)

        if (searchOverlay.visibility == View.VISIBLE) {
            val s = searchEditText.text
            val filter = SearchFilter.parse(s.toString())

            viewModel.filterData(filter, currentType)
        }
    }

    companion object {
        private const val REQUEST_CODE_ADD_DDL = 1
        const val ANIMATION_DURATION = 160L
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 0x2001
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
                        databaseHelper.updateDDL(updatedDDL)
                        viewModel.loadData(currentType)
                        // 清除多选状态
                        adapter.selectedPositions.clear()
                        adapter.isMultiSelectMode = false
                    }
                    editDialog.show(supportFragmentManager, "EditDDLFragment")
                } else {
                    Toast.makeText(this@MainActivity, "请先选择要修改的项目", Toast.LENGTH_SHORT).show()
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
            bottomAppBar.setNavigationIcon(R.drawable.ic_search)
            bottomAppBar.setNavigationOnClickListener {
                showSearchOverlay()
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
        tabLayout.addTab(tabLayout.newTab().setText("任务"))
        tabLayout.addTab(tabLayout.newTab().setText("习惯"))

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
            setSystemBarColorsWithAnimation(
                colorSurface,
                colorSurface,
                isLightColor(colorSurface),
                duration = 100
            )
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
            setSystemBarColorsWithAnimation(
                colorSurface,
                colorContainer,
                isLightColor(colorContainer),
                duration = 100
            )
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
        grantResults: IntArray
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
            val allDDLs = databaseHelper.getAllDDLs()
            allDDLs.filter { !it.isCompleted }.forEach { ddl ->
                DeadlineAlarmScheduler.scheduleExactAlarm(applicationContext, ddl)
            }
        }
    }

    /**************************************
     * 用户引导系统
     **************************************/
    private fun showPermissionGuidance() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("权限说明")
            setMessage("为保证Deadliner在后台正常运行，需要以下权限：\n\n1. 通知权限\n2. 电池优化例外\n3. 自启动权限")
            setPositiveButton("立即设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            setNegativeButton("稍后再说", null)
        }.show()
    }

    private fun showBatteryOptimizationDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("电池优化设置")
            setMessage("请将Deadliner设为「不受电池优化限制」")
            setPositiveButton("去设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            setCancelable(false)
        }.show()
    }

    private fun showXiaomiAutoStartDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("自启动权限")
            setMessage("请在小米设置中允许Deadliner自启动")
            setPositiveButton("去设置") { _, _ ->
                try {
                    startActivity(Intent("miui.intent.action.OP_AUTO_START").apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                    })
                } catch (e: Exception) {
                    openAppSettings()
                }
            }
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

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun decideCloudStatus() {
        if (!GlobalUtils.cloudSyncEnable ||
            GlobalUtils.cloudSyncServer == null ||
            GlobalUtils.cloudSyncConstantToken == null) {
            cloudButton.setImageResource(R.drawable.ic_cloud_off)
        }
        if (GlobalUtils.cloudSyncEnable &&
            GlobalUtils.cloudSyncServer != null
            && GlobalUtils.cloudSyncConstantToken != null) {
            cloudButton.setImageResource(R.drawable.ic_cloud)
        }
    }
}