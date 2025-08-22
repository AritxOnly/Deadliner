package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Filter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.GeneratedDDL
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.toJson
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@SuppressLint("SimpleDateFormat")
class AddDDLActivity : AppCompatActivity() {

    private lateinit var repo: DDLRepository
    private lateinit var ddlNameEditText: EditText
    private lateinit var startTimeCard: MaterialCardView
    private lateinit var endTimeCard: MaterialCardView
    private lateinit var ddlNoteEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var saveToCalendarButton: Button
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
    private lateinit var habitNoteHint: TextView

    private lateinit var importFromCalendarButton: ImageButton

    private var calendarEventId: Long? = null

    private var selectedPage = 0

    private var frequency: Int? = null
    private var total: Int? = null
    private var frequencyType = DeadlineFrequency.TOTAL


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("AddDDLActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ddl)

        val generatedDDL = intent.getParcelableExtra<GeneratedDDL>("EXTRA_GENERATE_DDL")

        DynamicColors.applyToActivitiesIfAvailable(this.application)
        DynamicColors.applyToActivityIfAvailable(this)

        GlobalUtils.decideHideFromRecent(this, this@AddDDLActivity)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(android.R.attr.colorBackground)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        selectedPage = intent.getIntExtra("EXTRA_CURRENT_TYPE", 0)

        repo = DDLRepository()
        ddlNameEditText = findViewById(R.id.ddlNameEditText)
        startTimeCard = findViewById(R.id.startTimeCard) // MaterialCardView
        endTimeCard = findViewById(R.id.endTimeCard) // MaterialCardView
        ddlNoteEditText = findViewById(R.id.ddlNoteEditText)
        saveButton = findViewById(R.id.saveButton)
        saveToCalendarButton = findViewById(R.id.saveToCalendarButton)
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
        habitNoteHint = findViewById(R.id.habitNoteHint)

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
            save(toCalendar = false)
        }

        saveToCalendarButton.setOnClickListener {
            save(toCalendar = true)
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
                        habitNoteHint.visibility = View.GONE
                    }
                    1 -> {
                        ddlNoteLayout.visibility = View.GONE
                        ddlNoteEditText.visibility = View.GONE
                        freqTypeToggleGroup.visibility = View.VISIBLE
                        freqTypeHint.visibility = View.VISIBLE
                        freqEditLayout.visibility = View.VISIBLE
                        habitNoteHint.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        freqEditText.addTextChangedListener(
            afterTextChanged = { editable ->
                frequency = editable?.toString()?.toIntOrNull()
                val formattedNote = GlobalUtils.generateHabitNote(this@AddDDLActivity, frequency, total, frequencyType)
                habitNoteHint.text = formattedNote
            }
        )

        totalEditText.addTextChangedListener(
            afterTextChanged = { editable ->
                total = editable?.toString()?.toIntOrNull()
                val formattedNote = GlobalUtils.generateHabitNote(this@AddDDLActivity, frequency, total, frequencyType)
                habitNoteHint.text = formattedNote
            }
        )

        freqTypeToggleGroup.addOnButtonCheckedListener { _, _, _ ->
            frequencyType = when (freqTypeToggleGroup.checkedButtonId) {
                R.id.btnTotal -> DeadlineFrequency.TOTAL
                R.id.btnDaily -> DeadlineFrequency.DAILY
                R.id.btnWeekly -> DeadlineFrequency.WEEKLY
                R.id.btnYearly -> DeadlineFrequency.MONTHLY
                else -> DeadlineFrequency.TOTAL
            }
            val formattedNote = GlobalUtils.generateHabitNote(this@AddDDLActivity, frequency, total, frequencyType)
            habitNoteHint.text = formattedNote
        }

        importFromCalendarButton.setOnClickListener {
            try {
                loadCalendarEventsAndShowDialog()
            } catch (e: Exception) {
                Log.e("Calendar", e.toString())
                Toast.makeText(this, getString(R.string.permission_calendar_error, e.toString()), Toast.LENGTH_SHORT).show()
            }
        }

        val formattedNote = GlobalUtils.generateHabitNote(this, frequency, total, frequencyType)
        habitNoteHint.text = formattedNote

        ddlNameEditText.setText(generatedDDL?.name)
        endTime = generatedDDL?.dueTime
        if (generatedDDL != null)
            endTimeContent.text = formatLocalDateTime(generatedDDL.dueTime)
        ddlNoteEditText.setText(generatedDDL?.note)
    }

    private fun save(toCalendar: Boolean) {
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
                if (endTime == null) return

                // 保存到数据库
                val ddlId = repo.insertDDL(
                    ddlName,
                    startTime.toString(),
                    endTime.toString(),
                    ddlNote,
                    calendarEventId = calendarEventId
                )

                repo.getDDLById(ddlId)?.let { item ->
                    if (GlobalUtils.deadlineNotification)
                        DeadlineAlarmScheduler.scheduleExactAlarm(applicationContext, item)
                    if (toCalendar) {
                        val calendarHelper = CalendarHelper(this)
                        lifecycleScope.launch {
                            try {
                                val eventId = calendarHelper.insertEvent(item)
                                item.calendarEventId = eventId
                                DDLRepository().updateDDL(item)
                                Toast.makeText(this@AddDDLActivity, getString(R.string.add_calendar_success), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("Calendar", e.toString())
                                Toast.makeText(this@AddDDLActivity, getString(R.string.add_calendar_failed, e.toString()), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                setResult(RESULT_OK)
                finishAfterTransition() // 返回 MainActivity
            } else {
                repo.insertDDL(
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

    private fun loadCalendarEventsAndShowDialog() {
        val calendarHelper = CalendarHelper(applicationContext)
        val calendarEvents = calendarHelper.queryAllCalendarEvents()
        if (calendarEvents.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_task_add_calendar), Toast.LENGTH_SHORT).show()
            return
        }

        data class EventItem(val event: CalendarEvent) {
            val display: String get() {
                val time = GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())
                return "${event.title} – ${time?.let(::formatLocalDateTime) ?: getString(R.string.parse_failed)}"
            }
            override fun toString() = display
        }

        // 原始条目文本
        val items = calendarEvents.map(::EventItem)

        // Inflate 布局
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_calendar_events, null, false)
        val etSearch = dialogView.findViewById<TextInputEditText>(R.id.searchEditText)
        val lvEvents = dialogView.findViewById<ListView>(R.id.eventListView)

        // 自定义 ArrayAdapter + Filterable
        open class EventAdapter(
            ctx: Context,
            @LayoutRes layoutResId: Int,
            @IdRes textViewResId: Int,
            items: List<EventItem>
        ) : ArrayAdapter<EventItem>(ctx, layoutResId, textViewResId, items) {
            private val original = items.toList()
            private val filtered = items.toMutableList()

            override fun getFilter(): Filter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                    filtered.clear()
                    if (constraint.isNullOrBlank()) {
                        filtered.addAll(original)
                    } else {
                        val kw = constraint.toString().lowercase()
                        filtered.addAll(
                            original.filter { it.display.lowercase().contains(kw) }
                        )
                    }
                    values = filtered.toList()
                    count = filtered.size
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(c: CharSequence?, results: FilterResults) {
                    clear()
                    addAll(results.values as List<EventItem>)
                    notifyDataSetChanged()
                }
            }

            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val view = super.getView(pos, cv, parent)
                val rb = view.findViewById<RadioButton>(R.id.radio)
                rb.isChecked = (parent as ListView).isItemChecked(pos)
                return view
            }
        }

        // 1. 用自定义适配器
        val adapter = EventAdapter(this, R.layout.dialog_single_choice_layout, android.R.id.text1, items)
        lvEvents.adapter = adapter
        lvEvents.choiceMode = ListView.CHOICE_MODE_SINGLE

        // 2. 搜索框监听：每次文本变化都触发 filter
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 3. 记录用户的点击
        var selectedPosition = -1
        lvEvents.setOnItemClickListener { _, _, pos, _ ->
            selectedPosition = pos
            lvEvents.setItemChecked(pos, true)
            adapter.notifyDataSetChanged()
        }

        // 4. 弹窗
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_calendar_to_import)
            .setView(dialogView)
            .setNeutralButton(R.string.filter_calendar_account) { _, _ -> showCalendarFilterDialog() }
            .setPositiveButton(R.string.settings_import) { dialog, _ ->
                if (selectedPosition >= 0) {
                    val item = adapter.getItem(selectedPosition)!!
                    val event = item.event
                    ddlNameEditText.setText(event.title)
                    ddlNoteEditText.setText(event.description)
                    endTime = GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())
                    findViewById<TextView>(R.id.endTimeContent)
                        .text = formatLocalDateTime(endTime!!)
                    calendarEventId = event.id
                } else {
                    Toast.makeText(this, getString(R.string.no_event_select), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCalendarFilterDialog() {
        // 先拿到所有账号
        val helper = CalendarHelper(applicationContext)
        val accounts = helper.getAllCalendarAccounts()

        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.no_valid_calendar_account, Toast.LENGTH_SHORT).show()
            return
        }

        // 准备对话框要显示的条目和选中状态
        val names = accounts.map { it.accountName.ifEmpty { it.accountName } }.toTypedArray()
        val savedSet = GlobalUtils.filteredCalendars?:setOf()
        val checked = names.map { savedSet.contains(it) }.toBooleanArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_calendar_account_to_hide)
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.accept) { _, _ ->
                // 把勾选了的那些账户名存到 prefs 里
                val newFiltered = names
                    .zip(checked.toList())
                    .filter { it.second }    // true 表示要“过滤掉”
                    .map   { it.first }
                    .toSet()

                GlobalUtils.filteredCalendars = newFiltered

                Toast.makeText(this, R.string.calendar_filter_saved, Toast.LENGTH_SHORT).show()
                loadCalendarEventsAndShowDialog()
            }
            .setNegativeButton(R.string.cancel, null)
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
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)  // SHORT / MEDIUM / LONG / FULL
            .withLocale(Locale.getDefault())
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