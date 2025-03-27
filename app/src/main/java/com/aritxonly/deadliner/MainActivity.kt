package com.aritxonly.deadliner

import ApkDownloaderInstaller
import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.work.*
import androidx.work.PeriodicWorkRequestBuilder
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.noties.markwon.Markwon
import kotlinx.coroutines.*
import nl.dionsegijn.konfetti.xml.KonfettiView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.toHexString
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

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

    private var isFireworksAnimEnable = true
    private var pauseRefresh: Boolean = false

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

//        DynamicColors.applyToActivitiesIfAvailable(application)

        DynamicColors.applyToActivityIfAvailable(this)

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

        decideShowEmptyNotice()

        // 设置 RecyclerView
        val itemList = databaseHelper.getAllDDLs()
        adapter = CustomAdapter(itemList, this)
        adapter.setSwipeListener(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 设置 RecyclerView
        adapter = CustomAdapter(itemList, this)
        viewModel.ddlList.observe(this) { items ->
            adapter.itemList = items
            adapter.notifyDataSetChanged()
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
                switchAppBarStatus(selectedCount == 0)
                if (selectedCount != 0) {
                    excitementText.text = "已选中 $selectedCount 项 Deadline"
                } else {
                    updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
                }
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
//                adapter.updateData(databaseHelper.getAllDDLs(), this)
            }
        }
        // 添加新事件按钮
        addEventButton.setOnClickListener {
            val intent = Intent(this, AddDDLActivity::class.java)
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

        // 设置通知定时任务
        updateNotification(GlobalUtils.deadlineNotification)

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
                            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                                // 取消删除，刷新界面
                                adapter.notifyDataSetChanged()
                            }
                            .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                                // 根据选中项进行删除，先复制列表防止修改集合时出错
                                val positionsToDelete = adapter.selectedPositions.toList().sortedDescending()
                                for (position in positionsToDelete) {
                                    val item = adapter.itemList[position]
                                    databaseHelper.deleteDDL(item.id)
                                }
                                viewModel.loadData(currentType)
                                Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()

                                switchAppBarStatus(true)
                                updateTitleAndExcitementText(GlobalUtils.motivationalQuotes)
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
                        triggerVibration(this@MainActivity, 100)
                        val positionsToUpdate = adapter.selectedPositions.toList()
                        for (position in positionsToUpdate) {
                            val item = adapter.itemList[position]
                            item.isCompleted = true
                            item.completeTime = LocalDateTime.now().toString()
                            databaseHelper.updateDDL(item)
                        }
                        viewModel.loadData(currentType)
                        Toast.makeText(this@MainActivity, R.string.toast_finished, Toast.LENGTH_SHORT).show()

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

        setupTabs()

        checkForUpdates()
    }

    override fun onStop() {
        super.onStop()
        // 触发小组件更新
        updateWidget()
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, SingleDeadlineWidget::class.java))
        for (appWidgetId in appWidgetIds) {
            SingleDeadlineWidget.updateWidget(this, appWidgetManager, appWidgetId)
        }
    }

    private fun refreshData() {
        viewModel.loadData(currentType)
        swipeRefreshLayout.isRefreshing = false
        decideShowEmptyNotice()
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

    private fun updateNotification(isEnabled: Boolean) {
        if (!isEnabled) {
            Log.d("Notification", "Here")
            // 如果开关关闭，取消定时任务
            WorkManager.getInstance(this).cancelUniqueWork("DeadlineCheckWork")
            return
        }

        // 检查是否已有相同的任务在队列中
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("DeadlineCheckWork").observe(this) { workInfos ->
            val isWorkScheduled = workInfos.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }

            // 如果任务未调度，则启动新的任务
            if (!isWorkScheduled) {
                val workRequest = PeriodicWorkRequestBuilder<DeadlineWorker>(1, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "DeadlineCheckWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }
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
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                adapter.notifyItemChanged(position) // 取消删除，刷新该项
                pauseRefresh = false
            }
            .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                val item = adapter.itemList[position]
                databaseHelper.deleteDDL(item.id)
                viewModel.loadData(currentType)
                Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                decideShowEmptyNotice()
                pauseRefresh = false
            }
            .show()
    }

    private fun decideShowEmptyNotice() {
        finishNotice.visibility = if (databaseHelper.getAllDDLs().isEmpty()) {
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
        updateNotification(GlobalUtils.deadlineNotification)
        isFireworksAnimEnable = GlobalUtils.fireworksOnFinish
        switchAppBarStatus(true)
        viewModel.loadData(currentType)
        decideShowEmptyNotice()
    }

    companion object {
        private const val REQUEST_CODE_ADD_DDL = 1
        const val ANIMATION_DURATION = 160L
    }

    /* New to v2.0 */
    private var currentAppBarIsPrimary: Boolean? = null

    private fun switchAppBarStatus(isPrimary: Boolean) {
        if (currentAppBarIsPrimary == isPrimary) return

        if (!isPrimary) {
            bottomAppBar.performHide()

            bottomAppBar.postDelayed({
                bottomAppBar.replaceMenu(R.menu.bottom_utility_bar)
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
                val intent = Intent(this, AddDDLActivity::class.java)
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
        // 隐藏软键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

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
                viewModel.loadData(currentType)
            }
            // 其他方法保持不变
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}

fun View.safePerformClick() {
    performClick()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!isAccessibilityFocused) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        }
    }
}