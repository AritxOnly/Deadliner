package com.aritxonly.deadliner

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.microedition.khronos.opengles.GL

class EditDDLFragment(private val ddlItem: DDLItem, private val onUpdate: (DDLItem) -> Unit) : DialogFragment() {

    private lateinit var ddlNameEditText: TextInputEditText
    private lateinit var startTimeCard: View
    private lateinit var endTimeCard: View
    private lateinit var startTimeContent: TextView
    private lateinit var endTimeContent: TextView
    private lateinit var ddlNoteLayout: TextInputLayout
    private lateinit var ddlNoteEditText: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var backButton: ImageButton

    private var startTime: LocalDateTime = GlobalUtils.safeParseDateTime(ddlItem.startTime)
    private var endTime: LocalDateTime? = GlobalUtils.parseDateTime(ddlItem.endTime)

    private lateinit var freqEditLayout: LinearLayout
    private lateinit var freqTypeToggleGroup: MaterialButtonToggleGroup
    private lateinit var freqTextInput: TextInputLayout
    private lateinit var freqEditText: EditText
    private lateinit var totalTextInput: TextInputLayout
    private lateinit var totalEditText: EditText
    private lateinit var freqTypeHint: TextView

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setWindowAnimations(R.style.DialogAnimation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_ddl, container, false)

        // 获取主题中的 colorBackground 并设置为背景
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        view.setBackgroundColor(typedValue.data)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ddlNameEditText = view.findViewById(R.id.ddlNameEditText)
        startTimeCard = view.findViewById(R.id.startTimeCard)
        endTimeCard = view.findViewById(R.id.endTimeCard)
        startTimeContent = view.findViewById(R.id.startTimeContent)
        endTimeContent = view.findViewById(R.id.endTimeContent)
        ddlNoteLayout = view.findViewById(R.id.ddlNoteLayout)
        ddlNoteEditText = view.findViewById(R.id.ddlNoteEditText)
        saveButton = view.findViewById(R.id.saveButton)
        backButton = view.findViewById(R.id.backButton)

        freqEditLayout = view.findViewById(R.id.freqEditLayout)
        freqTypeToggleGroup = view.findViewById(R.id.freqTypeToggleGroup)
        freqTextInput = view.findViewById(R.id.freqTextInput)
        freqEditText = view.findViewById(R.id.freqEditText)
        totalTextInput = view.findViewById(R.id.totalTextInput)
        totalEditText = view.findViewById(R.id.totalEditText)
        freqTypeHint = view.findViewById(R.id.freqTypeHint)

        ddlNameEditText.setText(ddlItem.name)
        startTimeContent.text = formatLocalDateTime(startTime)
//        Log.d("endTime", "endTime: $endTime | realValue: ${ddlItem.endTime} | parseReturnValue: ${GlobalUtils.parseDateTime(ddlItem.endTime)} | safeParse: ${GlobalUtils.safeParseDateTime(ddlItem.endTime)}")
        if (endTime != null && endTime != GlobalUtils.timeNull) endTimeContent.text = formatLocalDateTime(endTime!!)

        when (ddlItem.type) {
            DeadlineType.TASK -> {
                ddlNoteLayout.visibility = View.VISIBLE
                ddlNoteEditText.visibility = View.VISIBLE
                freqTypeToggleGroup.visibility = View.GONE
                freqTypeHint.visibility = View.GONE
                freqEditLayout.visibility = View.GONE

                ddlNoteEditText.setText(ddlItem.note)
            }
            DeadlineType.HABIT -> {
                ddlNoteLayout.visibility = View.GONE
                ddlNoteEditText.visibility = View.GONE
                freqTypeToggleGroup.visibility = View.VISIBLE
                freqTypeHint.visibility = View.VISIBLE
                freqEditLayout.visibility = View.VISIBLE

                val habitMeta = GlobalUtils.parseHabitMetaData(ddlItem.note)
                freqTypeToggleGroup.check(
                    when (habitMeta.frequencyType) {
                        DeadlineFrequency.TOTAL -> R.id.btnTotal
                        DeadlineFrequency.DAILY -> R.id.btnDaily
                        DeadlineFrequency.WEEKLY -> R.id.btnWeekly
                        DeadlineFrequency.MONTHLY -> R.id.btnYearly
                    }
                )
                freqEditText.setText(habitMeta.frequency.toString())
                totalEditText.setText(habitMeta.total.toString())
            }
        }

        // 设置沉浸式状态栏和导航栏
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        // 选择开始时间
        startTimeCard.setOnClickListener {
            GlobalUtils.showDateTimePicker(parentFragmentManager) { selectedTime ->
                startTime = selectedTime
                startTimeContent.text = formatLocalDateTime(startTime)
            }
        }

        // 选择结束时间
        endTimeCard.setOnClickListener {
            GlobalUtils.showDateTimePicker(parentFragmentManager) { selectedTime ->
                endTime = selectedTime
                endTimeContent.text = formatLocalDateTime(endTime!!)
            }
        }

        // 保存按钮点击事件
        saveButton.setOnClickListener {
            when (ddlItem.type) {
                DeadlineType.TASK -> {
                    val updatedDDL = ddlItem.copy(
                        name = ddlNameEditText.text.toString(),
                        startTime = startTime.toString(),
                        endTime = endTime.toString(),
                        note = ddlNoteEditText.text.toString(),
                        type = DeadlineType.TASK
                    )
                    onUpdate(updatedDDL)
                }
                DeadlineType.HABIT -> {
                    val frequency = freqEditText.text.toString().ifBlank { "1" }.toInt()
                    val total = totalEditText.text.toString().ifBlank { "0" }.toIntOrNull()

                    val frequencyType = when (freqTypeToggleGroup.checkedButtonId) {
                        R.id.btnTotal -> DeadlineFrequency.TOTAL
                        R.id.btnDaily -> DeadlineFrequency.DAILY
                        R.id.btnWeekly -> DeadlineFrequency.WEEKLY
                        R.id.btnYearly -> DeadlineFrequency.MONTHLY
                        else -> DeadlineFrequency.TOTAL
                    }

                    val updatedDDL = ddlItem.copy(
                        name = ddlNameEditText.text.toString(),
                        startTime = startTime.toString(),
                        endTime = endTime.toString(),
                        note = HabitMetaData(
                            completedDates = setOf(),
                            frequencyType = frequencyType,
                            frequency = frequency,
                            total = total?:0,
                            refreshDate = LocalDate.now().toString()
                        ).toJson(),
                        type = DeadlineType.HABIT
                    )
                    onUpdate(updatedDDL)
                }
            }
            dismiss()
        }

        backButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean) {
        dialog?.window?.apply {
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
        requireContext().theme.resolveAttribute(attributeId, typedValue, true)
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

    /**
     * 格式化 LocalDateTime 为字符串
     */
    private fun formatLocalDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm", Locale.CHINA)
        return dateTime.format(formatter)
    }

    override fun getTheme(): Int = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
}