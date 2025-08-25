package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.view.MotionEvent
import android.view.Window
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ArchiveActivity : AppCompatActivity() {

    private lateinit var dropPageButton: ImageButton
    private lateinit var archiveRecyclerView: RecyclerView
    private lateinit var clearAllButton: MaterialButton

    private lateinit var adapter: ArchiveAdapter
    private lateinit var repo: DDLRepository

    private lateinit var rootView: ConstraintLayout
    private var initialY = 0f
    private var isDragging = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        setupWindowTransitions()

        setContentView(R.layout.activity_archive)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dropPageButton = findViewById(R.id.dropPageButton)
        archiveRecyclerView = findViewById(R.id.archiveRecyclerView)
        clearAllButton = findViewById(R.id.clearAllButton)

        dropPageButton.setOnClickListener {
            finishAfterTransition()
        }

        repo = DDLRepository()

        adapter = ArchiveAdapter(repo.getAllDDLs(), this)
        archiveRecyclerView.adapter = adapter
        archiveRecyclerView.layoutManager = LinearLayoutManager(this)

        clearAllButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_all_archive)
                .setPositiveButton(resources.getString(R.string.accept)) { _, _ ->
                    val itemList = repo.getAllDDLs()
                    val filteredList = itemList.filterNot { item ->
                        if (!item.isCompleted) return@filterNot true
                        item.isArchived = (!GlobalUtils.filterArchived(item)) || item.isArchived
                        repo.updateDDL(item)
                        !item.isArchived
                    }

                    for (item in filteredList) {
                        repo.deleteDDL(item.id)
                    }

                    adapter.itemList = repo.getAllDDLs()
                    adapter.notifyDataSetChanged()

                    finishAfterTransition()
                }.setNegativeButton(resources.getString(R.string.cancel), null)
                .show()
        }

        rootView = findViewById(R.id.main)

        // 给 RecyclerView 设置触摸监听
        archiveRecyclerView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    isDragging = false
                    // 不拦截 ACTION_DOWN，让 RecyclerView 也能响应
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    // 判断 RecyclerView 是否在顶部
                    val atTop = !archiveRecyclerView.canScrollVertically(-1)
                    val deltaY = event.rawY - initialY
                    if (!atTop) {
                        // RecyclerView 没有滚动到顶部，允许正常滚动
                        isDragging = false
                        false
                    } else {
                        // RecyclerView 在顶部
                        if (!isDragging && deltaY > GlobalUtils.dpToPx(15f, this)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            // 拦截事件，更新根布局的 translationY 和透明度
                            rootView.translationY = deltaY
                            rootView.alpha = 1 - (deltaY / rootView.height).coerceAtMost(0.5f)
                            true  // 消费事件，RecyclerView 不再处理
                        } else {
                            // 没达到拦截阈值，不拦截，让 RecyclerView 处理可能的 overscroll
                            false
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val deltaY = event.rawY - initialY
                        if (deltaY > rootView.height * 0.4f) {
                            // 超过 40% 的拖动距离，执行退出动画并 finish
                            rootView.animate()
                                .translationY(rootView.height.toFloat())
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    finishAfterTransition()
                                }
                                .start()
                        } else {
                            // 恢复原状
                            rootView.animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        }
                        true  // 拦截 ACTION_UP
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    /**
     * 设置窗口动画
     */
    private fun setupWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val fade = Fade()
            fade.duration = 300  // 过渡动画时长 300ms

            val slide = Slide()
            slide.slideEdge = android.view.Gravity.BOTTOM // 从底部滑入
            slide.duration = 300

            window.enterTransition = slide // 进入动画
            window.exitTransition = fade   // 退出动画
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdge()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdge()
    }
}