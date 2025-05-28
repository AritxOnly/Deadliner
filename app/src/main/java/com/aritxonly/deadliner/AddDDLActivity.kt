package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aritxonly.deadliner.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.toJson
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("SimpleDateFormat")
class AddDDLActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var ddlNameEditText: EditText
    private lateinit var startTimeCard: MaterialCardView
    private lateinit var endTimeCard: MaterialCardView
    private lateinit var ddlNoteEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var ddlNoteLayout: TextInputLayout

    private var startTime: LocalDateTime? = null
    private var endTime: LocalDateTime? = null

    private lateinit var freqEditLayout: LinearLayout
    private lateinit var typeTabLayout: TabLayout
    private lateinit var freqTypeToggleGroup: MaterialButtonToggleGroup
    private lateinit var freqTextInput: TextInputLayout
    private lateinit var freqEditText: EditText
    private lateinit var totalTextInput: TextInputLayout
    private lateinit var totalEditText: EditText
    private lateinit var freqTypeHint: TextView

    private lateinit var importFromCalendarButton: ImageButton

    private var calendarEventId: Long? = null

    private var selectedPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("AddDDLActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ddl)

        DynamicColors.applyToActivitiesIfAvailable(this.application)
        DynamicColors.applyToActivityIfAvailable(this)

        GlobalUtils.decideHideFromRecent(this, this@AddDDLActivity)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(android.R.attr.colorBackground)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        selectedPage = intent.getIntExtra("EXTRA_CURRENT_TYPE", 0)

        databaseHelper = DatabaseHelper.getInstance(applicationContext)
        ddlNameEditText = findViewById(R.id.ddlNameEditText)
        startTimeCard = findViewById(R.id.startTimeCard) // MaterialCardView
        endTimeCard = findViewById(R.id.endTimeCard) // MaterialCardView
        ddlNoteEditText = findViewById(R.id.ddlNoteEditText)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)
        importFromCalendarButton = findViewById(R.id.importFromCalendarButton)
        ddlNoteLayout = findViewById(R.id.ddlNoteLayout)

        freqEditLayout = findViewById(R.id.freqEditLayout)
        typeTabLayout = findViewById(R.id.typeTabLayout)
        freqTypeToggleGroup = findViewById(R.id.freqTypeToggleGroup)
        freqTextInput = findViewById(R.id.freqTextInput)
        freqEditText = findViewById(R.id.freqEditText)
        totalTextInput = findViewById(R.id.totalTextInput)
        totalEditText = findViewById(R.id.totalEditText)
        freqTypeHint = findViewById(R.id.freqTypeHint)

        val startTimeContent: TextView = findViewById(R.id.startTimeContent)
        val endTimeContent: TextView = findViewById(R.id.endTimeContent)

        // 默认时间
        startTime = LocalDateTime.now()
        startTimeContent.text = formatLocalDateTime(startTime!!)

        // 设置开始时间选择
        startTimeCard.setOnClickListener {
            GlobalUtils.showDateTimePicker(supportFragmentManager) { selectedTime ->
                startTime = selectedTime
                startTimeContent.text = formatLocalDateTime(startTime!!)
            }
        }

        // 设置结束时间选择
        endTimeCard.setOnClickListener {
            GlobalUtils.showDateTimePicker(supportFragmentManager) { selectedTime ->
                endTime = selectedTime
                endTimeContent.text = formatLocalDateTime(endTime!!)
            }
        }

        // 保存按钮点击事件
        saveButton.setOnClickListener {
            val ddlName = ddlNameEditText.text.toString()
            val ddlNote = ddlNoteEditText.text.toString()
            val frequency = freqEditText.text.toString().ifBlank { "1" }.toInt()
            val total = totalEditText.text.toString().ifBlank { "0" }.toIntOrNull()

            val frequencyType = when (freqTypeToggleGroup.checkedButtonId) {
                R.id.btnTotal -> DeadlineFrequency.TOTAL
                R.id.btnDaily -> DeadlineFrequency.DAILY
                R.id.btnWeekly -> DeadlineFrequency.WEEKLY
                R.id.btnYearly -> DeadlineFrequency.MONTHLY
                else -> DeadlineFrequency.TOTAL
            }

            if (ddlName.isNotBlank() && startTime != null) {
                if (selectedPage != 1) {
                    if (endTime == null) return@setOnClickListener
                    // 保存到数据库
                    val ddlId = databaseHelper.insertDDL(
                        ddlName,
                        startTime.toString(),
                        endTime.toString(),
                        ddlNote,
                        calendarEventId = calendarEventId
                    )

                    databaseHelper.getDDLById(ddlId)?.let { item ->
                        Log.d("AlarmDebug", "Reached here")
                        if (GlobalUtils.deadlineNotification)
                            DeadlineAlarmScheduler.scheduleExactAlarm(applicationContext, item)
                    }

                    setResult(RESULT_OK)
                    finishAfterTransition() // 返回 MainActivity
                } else {
                    databaseHelper.insertDDL(
                        ddlName,
                        startTime.toString(),
                        endTime.toString(),
                        note = HabitMetaData(
                            completedDates = setOf(),
                            frequencyType = frequencyType,
                            frequency = frequency,
                            total = total ?: 0,
                            refreshDate = LocalDate.now().toString()
                        ).toJson(),
                        type = DeadlineType.HABIT
                    )
                    Log.d("endTime", endTime.toString())
                    setResult(RESULT_OK)
                    finishAfterTransition() // 返回 MainActivity
                }
            }
        }

        backButton.setOnClickListener {
            finishAfterTransition()
        }

        typeTabLayout.getTabAt(selectedPage)?.select()
        initTab()

        typeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedPage = tab?.position!!
                when (selectedPage) {
                    0 -> {
                        ddlNoteLayout.visibility = View.VISIBLE
                        ddlNoteEditText.visibility = View.VISIBLE
                        freqTypeToggleGroup.visibility = View.GONE
                        freqTypeHint.visibility = View.GONE
                        freqEditLayout.visibility = View.GONE
                    }
                    1 -> {
                        ddlNoteLayout.visibility = View.GONE
                        ddlNoteEditText.visibility = View.GONE
                        freqTypeToggleGroup.visibility = View.VISIBLE
                        freqTypeHint.visibility = View.VISIBLE
                        freqEditLayout.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        importFromCalendarButton.setOnClickListener {
            try {
                loadCalendarEventsAndShowDialog()
            } catch (e: Exception) {
                Toast.makeText(this, "请检查日历权限：${e.toString()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCalendarEventsAndShowDialog() {
        val calendarHelper = CalendarHelper(applicationContext)

        // 使用 CalendarHelper 查询日历事件
        val calendarEvents = calendarHelper.queryCalendarEvents()

        if (calendarEvents.isEmpty()) {
            Toast.makeText(this, "没有可导入的日历事件", Toast.LENGTH_SHORT).show()
            return
        }

        // 准备显示的事件标题列表
        val eventTitles = calendarEvents.map { event ->
            val time = GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())
            "${event.title} - ${formatLocalDateTime(time!!)}"
        }.toTypedArray()

        var selectedPosition = 0

        MaterialAlertDialogBuilder(this)
            .setTitle("选择要导入的事件")
            .setSingleChoiceItems(eventTitles, -1) { dialog, which ->
                selectedPosition = which
            }
            .setPositiveButton("导入") { dialog, _ ->
                // 获取选中的事件
                val event = calendarEvents[selectedPosition]
                ddlNameEditText.setText(event.title)
                ddlNoteEditText.setText(event.description)
                endTime = GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())
                val endTimeContent: TextView = findViewById(R.id.endTimeContent)
                endTimeContent.text = formatLocalDateTime(endTime!!)
                calendarEventId = event.id
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun initTab() {
        when (selectedPage) {
            0 -> {
                ddlNoteLayout.visibility = View.VISIBLE
                ddlNoteEditText.visibility = View.VISIBLE
                freqTypeToggleGroup.visibility = View.GONE
                freqTypeHint.visibility = View.GONE
                freqEditLayout.visibility = View.GONE
            }
            1 -> {
                ddlNoteLayout.visibility = View.GONE
                ddlNoteEditText.visibility = View.GONE
                freqTypeToggleGroup.visibility = View.VISIBLE
                freqTypeHint.visibility = View.VISIBLE
                freqEditLayout.visibility = View.VISIBLE
            }
            else -> {}
        }
    }

    fun formatLocalDateTime(dateTime: LocalDateTime): String {
        // 定义格式化器
        val formatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm", Locale.CHINA)
        // 格式化 LocalDateTime
        return dateTime.format(formatter)
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
}