package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.transition.Fade
import android.transition.Slide
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.Window
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ArchiveActivity : AppCompatActivity() {

    private lateinit var dropPageButton: ImageButton
    private lateinit var archiveRecyclerView: RecyclerView
    private lateinit var clearAllButton: MaterialButton

    private lateinit var adapter: ArchiveAdapter
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        databaseHelper = DatabaseHelper.getInstance(applicationContext)

        adapter = ArchiveAdapter(databaseHelper.getAllDDLs(), this)
        archiveRecyclerView.adapter = adapter
        archiveRecyclerView.layoutManager = LinearLayoutManager(this)

        clearAllButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("清除所有已完成的Deadlines？")
                .setPositiveButton(resources.getString(R.string.accept)) { _, _ ->
                    val itemList = databaseHelper.getAllDDLs()
                    val filteredList = itemList.filterNot { item ->
                        if (!item.isCompleted) return@filterNot true

                        try {
                            val completeTime = parseDateTime(item.completeTime)
                            val daysSinceCompletion = Duration.between(completeTime, LocalDateTime.now()).toDays()
                            Log.d("updateData", "remains $daysSinceCompletion days")
                            daysSinceCompletion <= 7
                        } catch (e: Exception) {
                            Log.e("updateData", "Error parsing date: ${item.completeTime}", e)
                            true
                        }
                    }

                    for (item in filteredList) {
                        databaseHelper.deleteDDL(item.id)
                    }

                    adapter.itemList = databaseHelper.getAllDDLs()
                    adapter.notifyDataSetChanged()

                    finishAfterTransition()
                }.setNegativeButton(resources.getString(R.string.cancel), null)
                .show()
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

    fun parseDateTime(dateTimeString: String): LocalDateTime {
        val formatters = listOf(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        )

        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(dateTimeString, formatter)
            } catch (e: Exception) {
                // 尝试下一个格式
            }
        }
        throw IllegalArgumentException("Invalid date format: $dateTimeString")
    }
}