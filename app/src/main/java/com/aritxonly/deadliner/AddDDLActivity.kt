package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import java.sql.Time
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("SimpleDateFormat")
class AddDDLActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var ddlNameEditText: EditText
    private lateinit var startTimeCard: MaterialCardView
    private lateinit var endTimeCard: MaterialCardView
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton

    private var startTime: LocalDateTime? = null
    private var endTime: LocalDateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("AddDDLActivity", "available: ${DynamicColors.isDynamicColorAvailable()}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ddl)

        DynamicColors.applyToActivitiesIfAvailable(this.application)
        DynamicColors.applyToActivityIfAvailable(this)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(android.R.attr.colorBackground)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        databaseHelper = DatabaseHelper(this)
        ddlNameEditText = findViewById(R.id.ddlNameEditText)
        startTimeCard = findViewById(R.id.startTimeCard) // MaterialCardView
        endTimeCard = findViewById(R.id.endTimeCard) // MaterialCardView
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        val startTimeContent: TextView = findViewById(R.id.startTimeContent)
        val endTimeContent: TextView = findViewById(R.id.endTimeContent)

        // 默认时间
        startTime = LocalDateTime.now()
        startTimeContent.text = formatLocalDateTime(startTime!!)

        // 设置开始时间选择
        startTimeCard.setOnClickListener {
            showDateTimePicker { selectedTime ->
                startTime = selectedTime
                startTimeContent.text = formatLocalDateTime(startTime!!)
            }
        }

        // 设置结束时间选择
        endTimeCard.setOnClickListener {
            showDateTimePicker { selectedTime ->
                endTime = selectedTime
                endTimeContent.text = formatLocalDateTime(endTime!!)
            }
        }

        // 保存按钮点击事件
        saveButton.setOnClickListener {
            val ddlName = ddlNameEditText.text.toString()
            if (ddlName.isNotBlank() && startTime != null && endTime != null) {
                // 保存到数据库
                databaseHelper.insertDDL(ddlName, startTime.toString(), endTime.toString())
                setResult(RESULT_OK)
                finishAfterTransition() // 返回 MainActivity
            }
        }

        backButton.setOnClickListener {
            finishAfterTransition()
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

    /**
     * 显示日期和时间选择器
     */
    private fun showDateTimePicker(onDateTimeSelected: (LocalDateTime) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val selectedDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                onDateTimeSelected(selectedDateTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
}