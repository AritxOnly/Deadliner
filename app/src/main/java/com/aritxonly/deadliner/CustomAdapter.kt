package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import android.content.res.Resources
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.toJson
import com.aritxonly.deadliner.model.updateNoteWithDate
import java.time.temporal.ChronoUnit

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
    var onCheckInGlobalListener: OnCheckInGlobalListener? = null

    interface OnCheckInGlobalListener {
        fun onCheckInFailedGlobal(context: Context, habitItem: DDLItem)
        fun onCheckInSuccessGlobal(context: Context, habitItem: DDLItem, habitMeta: HabitMetaData)
    }

    interface MultiSelectListener {
        fun onSelectionChanged(selectedCount: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val starIcon: ImageView = itemView.findViewById(R.id.starIcon)

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

    companion object {
        const val VIEW_TYPE_TASK = 0
        const val VIEW_TYPE_HABIT = 1
    }

    private var currentType: DeadlineType = DeadlineType.TASK

    // 根据当前类型返回视图类型
    override fun getItemViewType(position: Int): Int {
        return when (currentType) {
            DeadlineType.TASK -> VIEW_TYPE_TASK
            DeadlineType.HABIT -> VIEW_TYPE_HABIT
        }
    }

    // 根据视图类型创建对应ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            VIEW_TYPE_HABIT -> R.layout.habit_layout
            else -> R.layout.item_layout
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    // 更新类型的方法
    fun updateType(newType: DeadlineType) {
        if (currentType != newType) {
            currentType = newType
            notifyDataSetChanged() // 关键刷新操作
        }
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
        val streakText: TextView = holder.itemView.findViewById(R.id.streakText)
        val frequencyText: TextView = holder.itemView.findViewById(R.id.frequencyText)
        val dailyProgress: LinearLayout = holder.itemView.findViewById(R.id.dailyProgress)
        val checkButton: MaterialButton = holder.itemView.findViewById(R.id.checkButton)
        val monthProgress: LinearProgressIndicator = holder.itemView.findViewById(R.id.monthProgress)
        val progressLabel: TextView = holder.itemView.findViewById(R.id.progressLabel)
        val constraintLayout: ConstraintLayout = holder.itemView.findViewById(R.id.constraintLayout)

        val habitItem = itemList[position]
        val context = holder.itemView.context
        val today = LocalDate.now()

        // 解析 note 字段获取 HabitMetaData
        val habitMeta = GlobalUtils.parseHabitMetaData(habitItem.note)

        // 从 HabitMetaData 中提取已打卡日期集合（转换为 LocalDate 对象）
        val completedDates: Set<LocalDate> = habitMeta.completedDates.map { LocalDate.parse(it) }.toSet()

        // 0. 判断是否需要清零
        refreshCount(context, habitItem, habitMeta)

        // 1. 绑定标题与连击天数（使用辅助函数计算当前连击）
        holder.titleText.text = habitItem.name
        val currentStreak = calculateCurrentStreak(completedDates)
        val canBeDone = GlobalUtils.canHabitBeDone(habitItem, habitMeta)
        streakText.text = if (canBeDone)
            context.getString(R.string.days_streak, currentStreak)
        else context.getString(R.string.no_time_left)

        // 2. 更新星标状态（根据 habitItem.isStared 字段）
        holder.starIcon.visibility = if (habitItem.isStared) View.VISIBLE else View.GONE

        // 3. 设置频率文本（利用 HabitMetaData 中的 frequencyType 和 frequency）
        Log.d("Database", "${habitMeta.frequencyType}")
        val freqDesc = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY ->
                if (habitMeta.total == 0)
                    context.getString(R.string.daily_frequency, habitMeta.frequency)
                else
                    context.getString(R.string.daily_frequency_with_total, habitMeta.frequency, habitMeta.total)

            DeadlineFrequency.WEEKLY ->
                if (habitMeta.total == 0)
                    context.getString(R.string.weekly_frequency, habitMeta.frequency)
                else
                    context.getString(R.string.weekly_frequency_with_total, habitMeta.frequency, habitMeta.total)

            DeadlineFrequency.MONTHLY ->
                if (habitMeta.total == 0)
                    context.getString(R.string.monthly_frequency, habitMeta.frequency)
                else
                    context.getString(R.string.monthly_frequency_with_total, habitMeta.frequency, habitMeta.total)

            DeadlineFrequency.TOTAL ->
                if (habitMeta.total == 0)
                    context.getString(R.string.total_frequency_persistent)
                else
                    context.getString(R.string.total_frequency_count, habitMeta.total)
        }

        val endTime = GlobalUtils.safeParseDateTime(habitItem.endTime)
        frequencyText.text = freqDesc + if (endTime != GlobalUtils.timeNull) {
            val now = LocalDateTime.now()
            val duration = Duration.between(now, endTime)
            val days = duration.toDays()
            if (days < 0) " · " + context.getString(R.string.ddl_overdue_short)
            else " · " + context.getString(R.string.remaining_days_arg, days)
        } else ""

        // 4. 更新每日进度点（最近7天）
        dailyProgress.removeAllViews()
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
            dailyProgress.addView(dot)
        }

        // 5. 更新进度条
        val now = LocalDateTime.now()

        val progress: Float

        if (endTime == GlobalUtils.timeNull) {
            // 若为空，不显示
            progress = 1f
            monthProgress.setIndicatorColor(getThemeColor(android.R.attr.colorControlActivated))
        } else {
            val startTime = GlobalUtils.safeParseDateTime(habitItem.startTime)

            val duration = Duration.between(now, endTime)
            val totalDuration = Duration.between(startTime, endTime)

            progress = duration.toMinutes().toFloat() / totalDuration.toMinutes().toFloat()
        }

        monthProgress.progress = (progress * 100).toInt()

        val parts = mutableListOf<String>()

        when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY -> parts += context.getString(
                R.string.progress_daily, habitItem.habitCount, habitMeta.frequency
            )
            DeadlineFrequency.WEEKLY -> parts += context.getString(
                R.string.progress_weekly, habitItem.habitCount, habitMeta.frequency
            )
            DeadlineFrequency.MONTHLY -> parts += context.getString(
                R.string.progress_monthly, habitItem.habitCount, habitMeta.frequency
            )
            DeadlineFrequency.TOTAL -> { /* 周期型进度不显示 */ }
        }

        if (habitMeta.total != 0) {
            parts += context.getString(
                R.string.progress_total, habitItem.habitTotalCount, habitMeta.total
            )
        }

        progressLabel.text = parts.joinToString(separator = " · ")

        // 6. 设置打卡按钮状态
        val canCheckIn = (habitMeta.total != 0 && (if (habitMeta.frequencyType != DeadlineFrequency.TOTAL) {
            (habitItem.habitCount < habitMeta.frequency) && (completedDates.size < habitMeta.total)
        } else true) && (habitItem.habitTotalCount < habitMeta.total)) || (habitMeta.total == 0)

        val alreadyChecked = when (habitMeta.frequencyType) {
            DeadlineFrequency.TOTAL -> false
            else -> habitMeta.frequency <= habitItem.habitCount
        }
        val canPerformClick = canCheckIn && !alreadyChecked
        checkButton.text = if (habitMeta.total != 0 && habitItem.habitTotalCount >= habitMeta.total)
                context.getString(R.string.completed) else if (alreadyChecked)
                    context.getString(R.string.already_check_habit) else context.getString(R.string.check_habit)
        checkButton.icon = if (alreadyChecked) null
        else ContextCompat.getDrawable(context, R.drawable.ic_check)

        // 7. 设置点击监听（传入 context 给 onCheckInClick）
        checkButton.setOnClickListener { onCheckInClick(context, habitItem, habitMeta, canPerformClick) }

        if (selectedPositions.contains(position)) {
            constraintLayout.setBackgroundResource(R.drawable.item_background_selected)
        } else {
            if (habitMeta.total != 0 && habitItem.habitTotalCount >= habitMeta.total) {
                constraintLayout.setBackgroundResource(R.drawable.item_background_finished)

                streakText.text = ""

                // 设置为isCompleted
                if (!habitItem.isCompleted) {
                    val updatedHabit = habitItem.copy(
                        isCompleted = true,
                        completeTime = LocalDateTime.now().toString()
                    )

                    DDLRepository().updateDDL(updatedHabit)

                    viewModel.loadData(viewModel.currentType)
                }
                return
            }

            val now = LocalDateTime.now()
            val endTime = GlobalUtils.safeParseDateTime(habitItem.endTime)

            if (endTime != GlobalUtils.timeNull && now.isAfter(endTime)) {
                constraintLayout.setBackgroundResource(R.drawable.item_background_passed)
                streakText.text = ""

                return
            }

            constraintLayout.setBackgroundResource(R.drawable.item_background)
        }
    }

    /**
     * 打卡操作：检查当天是否已打卡，若未打卡则更新 note 字段、habitCount，
     * 并调用数据库更新和刷新数据
     */
    private fun onCheckInClick(context: Context, habitItem: DDLItem, habitMeta: HabitMetaData, canPerformClick: Boolean) {
        if (!canPerformClick) {
            onCheckInGlobalListener?.onCheckInFailedGlobal(context, habitItem)
            return
        }

        val today = LocalDate.now()

        // 更新 note 字段，将今天的日期加入已打卡记录中
        val updatedNote = updateNoteWithDate(habitItem, today)
        // 更新 habitCount 累计打卡次数 +1
        val updatedHabit = habitItem.copy(
            note = updatedNote,
            habitCount = habitItem.habitCount + 1,
            habitTotalCount = habitItem.habitTotalCount + 1
        )

        onCheckInGlobalListener?.onCheckInSuccessGlobal(context, updatedHabit, habitMeta)

        // 更新数据库记录
        DDLRepository().updateDDL(updatedHabit)

        viewModel.loadData(viewModel.currentType)
    }

    /**
     * 辅助函数：自动清零
     */
    private fun refreshCount(context: Context, habitItem: DDLItem, habitMeta: HabitMetaData) {
        val month = YearMonth.now()
        val presetDuration = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY -> 1    // 1天清空一次
            DeadlineFrequency.WEEKLY -> 7
            DeadlineFrequency.MONTHLY -> month.lengthOfMonth()
            DeadlineFrequency.TOTAL -> return
        }

        val duration = ChronoUnit.DAYS.between(LocalDate.parse(habitMeta.refreshDate), LocalDate.now())
        if (duration >= presetDuration) {
            // refresh
            val updatedNote = habitMeta.copy(
                refreshDate = LocalDate.now().toString()
            ).toJson()

            val updatedHabit = habitItem.copy(
                note = updatedNote,
                habitCount = 0
            )

            DDLRepository().updateDDL(updatedHabit)

            viewModel.loadData(viewModel.currentType)
        }
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
        val remainingTimeText: TextView = holder.itemView.findViewById(R.id.remainingTimeText)
        val progressBar: LinearProgressIndicator = holder.itemView.findViewById(R.id.progressBar)
        val constraintLayout: ConstraintLayout = holder.itemView.findViewById(R.id.constraintLayout)
        val noteText: TextView = holder.itemView.findViewById(R.id.noteText)
        val remainingTimeTextAlt: TextView = holder.itemView.findViewById(R.id.remainingTimeTextAlt)

        val direction = GlobalUtils.progressDir

        val item = itemList[position]
        val currentTime = LocalDateTime.now()

        // 将字符串时间转换为 LocalDateTime
        val startTime = GlobalUtils.safeParseDateTime(item.startTime)
        val endTime = GlobalUtils.safeParseDateTime(item.endTime)

        // 计算剩余时间
        val remainingDuration = Duration.between(currentTime, endTime)
        val remainingMinutes = remainingDuration.toMinutes().toInt()

        // 计算总时长（以分钟为单位）
        val totalDuration = Duration.between(startTime, endTime).toMinutes().toInt()

        // 设置标题
        holder.titleText.text = item.name
        noteText.text = item.note

        val displayFullContent: Boolean
        val remainingTimeTextView: TextView = if (noteText.text.isNotEmpty()) {
            displayFullContent = false
            remainingTimeText.visibility = View.GONE
            remainingTimeTextAlt.visibility = View.VISIBLE
            remainingTimeTextAlt
        } else {
            displayFullContent = true
            remainingTimeTextAlt.visibility = View.GONE
            remainingTimeText.visibility = View.VISIBLE
            remainingTimeText
        }

        // 计算天、小时、分钟
        val days = remainingMinutes / (24 * 60)
        val hours = (remainingMinutes % (24 * 60)) / 60
        val minutesPart = remainingMinutes % 60

        val compactDays = remainingMinutes.toFloat() / (24 * 60).toFloat()

        remainingTimeTextView.text = if (remainingMinutes >= 0) {
            if (displayFullContent) {
                if (GlobalUtils.detailDisplayMode) {
                    buildString {
                        append(context.getString(R.string.remaining_prefix))
                        if (days != 0) append(context.getString(R.string.remaining_days, days))
                        if (hours != 0) append(context.getString(R.string.remaining_hours, hours))
                        append(context.getString(R.string.remaining_minutes, minutesPart))
                    }
                } else {
                    context.getString(R.string.remaining_compact_days, compactDays)
                }
            } else {
                if (GlobalUtils.detailDisplayMode) {
                    buildString {
                        if (days != 0) append(context.getString(R.string.remaining_days_short, days))
                        if (hours != 0) append(context.getString(R.string.remaining_hours_short, hours))
                        append(context.getString(R.string.remaining_minutes_short, minutesPart))
                    }
                } else {
                    context.getString(R.string.remaining_compact_days_short, compactDays)
                }
            }
        } else {
            if (displayFullContent)
                context.getString(R.string.ddl_overdue_full)
            else
                context.getString(R.string.ddl_overdue_short)
        }

        // 计算并设置进度条进度，确保至少为 1
        val progress = if (totalDuration > 0 && remainingMinutes <= totalDuration) {
            (remainingMinutes * 100 / totalDuration).coerceIn(1, 100)
        } else {
            100
        }
        progressBar.setProgressCompat(
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
            progressBar.setIndicatorColor(progressPassedColor)
            constraintLayout.setBackgroundResource(R.drawable.item_background_passed)
        } else if (remainingMinutes <= 720) {
            progressBar.setIndicatorColor(progressNearbyColor)
            constraintLayout.setBackgroundResource(R.drawable.item_background_nearby)
        } else {
            progressBar.setIndicatorColor(progressColor)
            constraintLayout.setBackgroundResource(R.drawable.item_background)
        }

        if (item.isCompleted) {
            val finishedColor = getThemeColor(android.R.attr.colorControlActivated)
            progressBar.setIndicatorColor(finishedColor)
            constraintLayout.setBackgroundResource(R.drawable.item_background_finished)
            progressBar.setProgressCompat(100, true)
            remainingTimeText.text = context.getString(R.string.completed_long)
            remainingTimeTextAlt.text = context.getString(R.string.completed)
        }

        /* v2.0 added: 只要被多选则更改颜色 */
        if (selectedPositions.contains(position)) {
            constraintLayout.setBackgroundResource(R.drawable.item_background_selected)
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