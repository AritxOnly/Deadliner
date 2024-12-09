package com.aritxonly.deadliner
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDateTime

class MainActivity : AppCompatActivity(), CustomAdapter.SwipeListener {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var addEventButton: FloatingActionButton
    private lateinit var settingsButton: ImageButton
    private val itemList = mutableListOf<DDLItem>()
    private lateinit var adapter: CustomAdapter
    private lateinit var addDDLLauncher: ActivityResultLauncher<Intent>

    private var pauseRefresh: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 跟随主题色
        Log.d("MainActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DynamicColors.applyToActivitiesIfAvailable(application)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(android.R.attr.colorBackground)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        databaseHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.recyclerView)
        addEventButton = findViewById(R.id.addEvent)
        settingsButton = findViewById(R.id.settingsButton)

        // 加载数据库中的数据
        val itemList = databaseHelper.getAllDDLs()

        // 设置 RecyclerView
        adapter = CustomAdapter(itemList, this)
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
                                val item = adapter.itemList[position]
                                databaseHelper.deleteDDL(item.id)
                                adapter.updateData(databaseHelper.getAllDDLs())
                                Toast.makeText(this@MainActivity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                                pauseRefresh = false
                            }
                            2 -> {
                                // 标记为完成操作
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

        // 设置ItemTouchHelper
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when (direction) {
                    ItemTouchHelper.LEFT -> adapter.onSwipeLeft(viewHolder.adapterPosition)
                    ItemTouchHelper.RIGHT -> adapter.onSwipeRight(viewHolder.adapterPosition)
                }
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
            Log.d("MainActivity", "Settings triggered")
        }
    }


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

    private val refreshInterval = 5000L
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            // 刷新数据
            if (!pauseRefresh) {
                adapter.updateData(databaseHelper.getAllDDLs())
                handler.postDelayed(this, refreshInterval)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 开始周期性刷新
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        // 停止周期性刷新
        handler.removeCallbacks(refreshRunnable)
    }

    companion object {
        private const val REQUEST_CODE_ADD_DDL = 1
    }
}