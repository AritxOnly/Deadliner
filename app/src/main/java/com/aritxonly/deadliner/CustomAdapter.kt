package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import android.content.res.Resources
import java.time.format.DateTimeFormatter

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

class CustomAdapter(
    public var itemList: List<DDLItem>,
    private val context: Context,
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    var isMultiSelectMode = false
    val selectedPositions = mutableSetOf<Int>()
    var multiSelectListener: MultiSelectListener? = null

    interface MultiSelectListener {
        fun onSelectionChanged(selectedCount: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val remainingTimeText: TextView = itemView.findViewById(R.id.remainingTimeText)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        val constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        val remainingTimeTextAlt: TextView = itemView.findViewById(R.id.remainingTimeTextAlt)
        val starIcon: ImageView = itemView.findViewById(R.id.starIcon)
        val streakText: TextView = itemView.findViewById(R.id.streakText)
        val frequencyText: TextView = itemView.findViewById(R.id.frequencyText)
        val dailyProgress: LinearLayout = itemView.findViewById(R.id.dailyProgress)
        val checkButton: MaterialButton = itemView.findViewById(R.id.checkButton)
        val monthProgress: LinearProgressIndicator = itemView.findViewById(R.id.monthProgress)
        val progressLabel: TextView = itemView.findViewById(R.id.progressLabel)

        init {
            // 设置长按事件进入多选模式
            itemView.setOnLongClickListener {
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true
                }
                toggleSelection(adapterPosition)
                multiSelectListener?.onSelectionChanged(selectedPositions.size)
                true
            }

            // 普通点击：在多选模式下切换选中状态，否则调用原有点击逻辑
            itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(adapterPosition)
                    multiSelectListener?.onSelectionChanged(selectedPositions.size)
                } else {
                    // 正常的单击事件逻辑
                    itemClickListener?.onItemClick(adapterPosition)
                }
            }
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)
    }

    interface SwipeListener {
        fun onSwipeLeft(position: Int)
        fun onSwipeRight(position: Int)
    }

    private var swipeListener: SwipeListener? = null

    fun setSwipeListener(listener: SwipeListener) {
        swipeListener = listener
    }

    fun onSwipeLeft(position: Int) {
        swipeListener?.onSwipeLeft(position)
    }

    fun onSwipeRight(position: Int) {
        swipeListener?.onSwipeRight(position)
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var itemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    // 根据 currentType 决定加载的布局资源
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewModel.currentType) {
            DeadlineType.HABIT -> R.layout.habit_layout
            else -> R.layout.item_layout
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (viewModel.currentType) {
            DeadlineType.HABIT -> habitBindViewHolder(holder, position)
            else -> taskBindViewHolder(holder, position)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun habitBindViewHolder(holder: ViewHolder, position: Int) {
        val habitItem = itemList[position]
        val context = holder.itemView.context
        val today = LocalDate.now()

        // 解析 note 字段获取 HabitMetaData
        val gson = Gson()
        val type = object : TypeToken<HabitMetaData>() {}.type
        val habitMeta: HabitMetaData = try {
            gson.fromJson(habitItem.note, type)
                ?: HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1)
        } catch (e: Exception) {
            HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1)
        }

        // 从 HabitMetaData 中提取已打卡日期集合（转换为 LocalDate 对象）
        val completedDates: Set<LocalDate> = habitMeta.completedDates.map { LocalDate.parse(it) }.toSet()

        // 1. 绑定标题与连击天数（使用辅助函数计算当前连击）
        holder.titleText.text = habitItem.name
        val currentStreak = calculateCurrentStreak(completedDates)
        holder.streakText.text = "${currentStreak}天连击"

        // 2. 更新星标状态（根据 habitItem.isStared 字段）
        holder.starIcon.visibility = if (habitItem.isStared) View.VISIBLE else View.GONE

        // 3. 设置频率文本（利用 HabitMetaData 中的 frequencyType 和 frequency）
        val freqDesc = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY -> "每天${habitMeta.frequency}次"
            DeadlineFrequency.WEEKLY -> "每周${habitMeta.frequency}次"
            DeadlineFrequency.MONTHLY -> "每月${habitMeta.frequency}次"
            DeadlineFrequency.TOTAL -> "共计${habitMeta.frequency}"
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        holder.frequencyText.text = "$freqDesc · 结束于${GlobalUtils.parseDateTime(habitItem.endTime).format(formatter)}"

        // 4. 更新每日进度点（最近7天）
        holder.dailyProgress.removeAllViews()
        for (i in 0 until 7) {
            val date = today.minusDays((6 - i).toLong())
            val isCompleted = date in completedDates

            val dot = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                    marginEnd = 4.dp
                }
                setImageResource(
                    if (isCompleted) R.drawable.ic_dot_filled else R.drawable.ic_dot_empty
                )
            }
            holder.dailyProgress.addView(dot)
        }

        // 5. 更新月度进度
        val currentMonth = YearMonth.now()
        val completedThisMonth = if (habitMeta.frequencyType == DeadlineFrequency.TOTAL)
            habitItem.habitCount
        else
            completedDates.count { YearMonth.from(it) == currentMonth }
        val monthlyGoal = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY -> currentMonth.lengthOfMonth()
            DeadlineFrequency.WEEKLY -> habitMeta.frequency * 4
            DeadlineFrequency.MONTHLY -> habitMeta.frequency
            DeadlineFrequency.TOTAL -> habitMeta.frequency
        }
        val progress = (completedThisMonth.toFloat() / monthlyGoal * 100).coerceAtMost(100f)
        holder.monthProgress.progress = progress.toInt()
        holder.progressLabel.text = if (habitMeta.frequencyType == DeadlineFrequency.TOTAL) {
            ""
        } else {
            "每月进度 $completedThisMonth/$monthlyGoal"
        }

        // 6. 设置打卡按钮状态
        val canCheckIn = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY -> true
            DeadlineFrequency.WEEKLY -> today.dayOfWeek == DayOfWeek.MONDAY
            DeadlineFrequency.MONTHLY -> today.dayOfMonth == 1
            DeadlineFrequency.TOTAL -> true
        }
        val alreadyChecked = today in completedDates
        holder.checkButton.isEnabled = canCheckIn && !alreadyChecked
        holder.checkButton.text = if (alreadyChecked) "已打卡" else "打卡"
        holder.checkButton.icon = if (alreadyChecked) null
        else ContextCompat.getDrawable(context, R.drawable.ic_check)

        // 7. 设置点击监听（传入 context 给 onCheckInClick）
        holder.checkButton.setOnClickListener { onCheckInClick(context, habitItem) }
    }

    /**
     * 打卡操作：检查当天是否已打卡，若未打卡则更新 note 字段、habitCount，
     * 并调用数据库更新和刷新数据
     */
    private fun onCheckInClick(context: Context, habitItem: DDLItem) {
        val today = LocalDate.now()
        // 如果今天已打卡，则不做处理
        if (today in getCompletedDates(habitItem)) return

        // 更新 note 字段，将今天的日期加入已打卡记录中
        val updatedNote = updateNoteWithDate(habitItem, today)
        // 更新 habitCount 累计打卡次数 +1
        val updatedHabit = habitItem.copy(
            note = updatedNote,
            habitCount = habitItem.habitCount + 1
        )

        // 更新数据库记录
        val databaseHelper = DatabaseHelper.getInstance(context)
        databaseHelper.updateDDL(updatedHabit)

        // 触发 ViewModel 刷新数据（假设 viewModel 已经在 Adapter 或 Activity 中持有）
        viewModel.loadData(viewModel.currentType)
    }

    /**
     * 辅助函数：计算当前连续打卡天数
     */
    private fun calculateCurrentStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var streak = 0
        var currentDate = LocalDate.now()
        while (currentDate in dates) {
            streak++
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }

    private fun taskBindViewHolder(holder: ViewHolder, position: Int) {
        val direction = GlobalUtils.progressDir

        val item = itemList[position]
        val currentTime = LocalDateTime.now()

        // 将字符串时间转换为 LocalDateTime
        val startTime = GlobalUtils.parseDateTime(item.startTime)
        val endTime = GlobalUtils.parseDateTime(item.endTime)

        // 计算剩余时间
        val remainingDuration = Duration.between(currentTime, endTime)
        val remainingMinutes = remainingDuration.toMinutes().toInt()

        // 计算总时长（以分钟为单位）
        val totalDuration = Duration.between(startTime, endTime).toMinutes().toInt()

        // 设置标题
        holder.titleText.text = item.name
        holder.noteText.text = item.note

        val displayFullContent: Boolean
        val remainingTimeTextView: TextView = if (holder.noteText.text.isNotEmpty()) {
            displayFullContent = false
            holder.remainingTimeText.visibility = View.GONE
            holder.remainingTimeTextAlt.visibility = View.VISIBLE
            holder.remainingTimeTextAlt
        } else {
            displayFullContent = true
            holder.remainingTimeTextAlt.visibility = View.GONE
            holder.remainingTimeText.visibility = View.VISIBLE
            holder.remainingTimeText
        }

        // 设置剩余时间显示
        remainingTimeTextView.text = if (remainingMinutes >= 0) {
            if (displayFullContent) "${remainingMinutes / 60}小时${remainingMinutes % 60}分钟 到 DDL"
            else "${remainingMinutes / 60}h${remainingMinutes % 60}m"
        } else {
            if (displayFullContent) "DDL逾期!!!"
            else "已逾期"
        }

        // 计算并设置进度条进度，确保至少为 1
        val progress = if (totalDuration > 0 && remainingMinutes <= totalDuration) {
            (remainingMinutes * 100 / totalDuration).coerceIn(1, 100)
        } else {
            100
        }
        holder.progressBar.setProgressCompat(
            if (direction) {
                100 - progress
            } else {
                progress
            },
            true
        )

        // 使用 getThemeColor 获取主题颜色
        val progressColor = getThemeColor(android.R.attr.colorPrimary)
        val progressNearbyColor = getThemeColor(android.R.attr.colorError)
        val progressPassedColor = getThemeColor(android.R.attr.colorControlHighlight)
        if (remainingMinutes < 0) {
            holder.progressBar.setIndicatorColor(progressPassedColor)
            holder.constraintLayout.setBackgroundResource(R.drawable.item_background_passed)
        } else if (remainingMinutes <= 720) {
            holder.progressBar.setIndicatorColor(progressNearbyColor)
            holder.constraintLayout.setBackgroundResource(R.drawable.item_background_nearby)
        } else {
            holder.progressBar.setIndicatorColor(progressColor)
            holder.constraintLayout.setBackgroundResource(R.drawable.item_background)
        }

        if (item.isCompleted) {
            val finishedColor = getThemeColor(android.R.attr.colorControlActivated)
            holder.progressBar.setIndicatorColor(finishedColor)
            holder.constraintLayout.setBackgroundResource(R.drawable.item_background_finished)
            holder.progressBar.setProgressCompat(100, true)
            holder.remainingTimeText.text = "DDL已完成🎉"
            holder.remainingTimeTextAlt.text = "已完成"
        }

        /* v2.0 added: 只要被多选则更改颜色 */
        if (selectedPositions.contains(position)) {
            holder.constraintLayout.setBackgroundResource(R.drawable.item_background_selected)
        }

        if (item.isStared) {
            holder.starIcon.visibility = View.VISIBLE
        } else {
            holder.starIcon.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    // 获取主题颜色的方法
    private fun getThemeColor(attributeId: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.data
    }
}