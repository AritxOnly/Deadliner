package com.aritxonly.deadliner
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

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
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var pauseRefresh: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 跟随主题色
        Log.d("MainActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DynamicColors.applyToActivitiesIfAvailable(application)

        DynamicColors.applyToActivityIfAvailable(this)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))
        val mainPage: ConstraintLayout = findViewById(R.id.main)
        mainPage.setBackgroundColor(colorSurface)

        databaseHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.recyclerView)
        addEventButton = findViewById(R.id.addEvent)
        settingsButton = findViewById(R.id.settingsButton)

        // 设置 RecyclerView
        val itemList = databaseHelper.getAllDDLs()
        adapter = CustomAdapter(itemList, this)
        adapter.setSwipeListener(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 设置 RecyclerView
        adapter = CustomAdapter(itemList, this)
        adapter.updateData(itemList)
        adapter.setSwipeListener(this)
        // 设置单击监听器
        adapter.setOnItemClickListener(object : CustomAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val clickedItem = adapter.itemList[position]
                pauseRefresh = true

                val finishedString = if (clickedItem.isCompleted) {
                    resources.getString(R.string.alert_edit_definish)
                } else {
                    resources.getString(R.string.alert_edit_finished)
                }

                // 选项数组
                val options = arrayOf(
                    resources.getString(R.string.alert_edit_modify),
                    resources.getString(R.string.alert_edit_delete),
                    finishedString
                )

                // 显示竖排按钮的 MaterialAlertDialog
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.alert_edit_title)
                    .setItems(options) { dialog, which ->
                        when (which) {
                            0 -> {
                                // 修改操作
                                val editDialog = EditDDLFragment(clickedItem) { updatedDDL ->
                                    databaseHelper.updateDDL(updatedDDL)
                                    adapter.updateData(databaseHelper.getAllDDLs())
                                }
                                editDialog.show(supportFragmentManager, "EditDDLFragment")
                                pauseRefresh = false
                            }
                            1 -> {
                                // 删除操作
                                triggerVibration(this@MainActivity, 200)
                                MaterialAlertDialogBuilder(this@MainActivity)
                                    .setTitle(R.string.alert_delete_title)
                                    .setMessage(R.string.alert_delete_message)
                                    .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                                        adapter.notifyItemChanged(position) // 取消删除，刷新该项
                                        pauseRefresh = false
                                    }
                                    .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                                        val item = adapter.itemList[position]
                                        databaseHelper.deleteDDL(item.id)
                                        adapter.updateData(databaseHelper.getAllDDLs())
                                        Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                                        pauseRefresh = false
                                    }
                                    .show()
                            }
                            2 -> {
                                // 标记为完成操作
                                triggerVibration(this@MainActivity, 100)
                                val item = adapter.itemList[position]
                                item.isCompleted = !item.isCompleted
                                databaseHelper.updateDDL(item)
                                adapter.updateData(databaseHelper.getAllDDLs())
                                if (item.isCompleted) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        R.string.toast_finished,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        R.string.toast_definished,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .show()
            }
        })

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
                adapter.updateData(databaseHelper.getAllDDLs())
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

        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)

        // 检查鼓励语句开关状态
        val isMotivationalQuotesEnabled = sharedPreferences.getBoolean("motivational_quotes", true)
        updateTitleAndExcitementText(isMotivationalQuotesEnabled)
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
        // 在后台线程获取数据，并在主线程更新 UI
        CoroutineScope(Dispatchers.Main).launch {
            val newData = withContext(Dispatchers.IO) {
                databaseHelper.getAllDDLs()
            }
            adapter.updateData(newData)
            swipeRefreshLayout.isRefreshing = false // 停止刷新动画
        }
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
            adapter.updateData(databaseHelper.getAllDDLs())
        }
    }

    override fun onSwipeRight(position: Int) {
        val item = adapter.itemList[position]
        item.isCompleted = !item.isCompleted
        databaseHelper.updateDDL(item)
        adapter.updateData(databaseHelper.getAllDDLs())
        if (item.isCompleted) {
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
                adapter.updateData(databaseHelper.getAllDDLs())
                Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                pauseRefresh = false
            }
            .show()
    }

    /*
    * 震动效果📳
    */
    fun triggerVibration(context: Context, duration: Long = 100) {
        val isVibrationOn = sharedPreferences.getBoolean("vibration", true)
        if (!isVibrationOn) {
            return
        }

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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

    override fun onResume() {
        super.onResume()
        val isMotivationalQuotesEnabled = sharedPreferences.getBoolean("motivational_quotes", true)
        updateTitleAndExcitementText(isMotivationalQuotesEnabled)
    }

    companion object {
        private const val REQUEST_CODE_ADD_DDL = 1
    }
}