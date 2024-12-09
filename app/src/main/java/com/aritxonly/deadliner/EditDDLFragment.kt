package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.material3.DatePickerDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditDDLFragment(private val ddlItem: DDLItem, private val onUpdate: (DDLItem) -> Unit) : DialogFragment() {

    private lateinit var ddlNameEditText: TextInputEditText
    private lateinit var startTimeCard: View
    private lateinit var endTimeCard: View
    private lateinit var startTimeContent: TextView
    private lateinit var endTimeContent: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var backButton: ImageButton

    private var startTime: LocalDateTime = LocalDateTime.parse(ddlItem.startTime)
    private var endTime: LocalDateTime = LocalDateTime.parse(ddlItem.endTime)

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
        saveButton = view.findViewById(R.id.saveButton)
        backButton = view.findViewById(R.id.backButton)

        ddlNameEditText.setText(ddlItem.name)
        startTimeContent.text = formatLocalDateTime(startTime)
        endTimeContent.text = formatLocalDateTime(endTime)

        // 设置沉浸式状态栏和导航栏
        val colorSurface = getThemeColor(android.R.attr.colorBackground)
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        // 选择开始时间
        startTimeCard.setOnClickListener {
            showDateTimePicker { selectedTime ->
                startTime = selectedTime
                startTimeContent.text = formatLocalDateTime(startTime)
            }
        }

        // 选择结束时间
        endTimeCard.setOnClickListener {
            showDateTimePicker { selectedTime ->
                endTime = selectedTime
                endTimeContent.text = formatLocalDateTime(endTime)
            }
        }

        // 保存按钮点击事件
        saveButton.setOnClickListener {
            val updatedDDL = ddlItem.copy(
                name = ddlNameEditText.text.toString(),
                startTime = startTime.toString(),
                endTime = endTime.toString()
            )
            onUpdate(updatedDDL)
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

    /**
     * 显示日期和时间选择器
     */
    private fun showDateTimePicker(onDateTimeSelected: (LocalDateTime) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val selectedDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                onDateTimeSelected(selectedDateTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun getTheme(): Int = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
}