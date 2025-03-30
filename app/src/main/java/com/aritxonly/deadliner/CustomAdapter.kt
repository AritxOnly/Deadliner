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
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import android.content.res.Resources

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
    var onCheckInClickGlobalListener: OnCheckInClickGlobalListener? = null

    interface OnCheckInClickGlobalListener {
        fun onCheckInClickGlobal(context: Context, habitItem: DDLItem, canPerformClick: Boolean)
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

        // 1. 绑定标题与连击天数（使用辅助函数计算当前连击）
        holder.titleText.text = habitItem.name
        val currentStreak = calculateCurrentStreak(completedDates)
        streakText.text = "${currentStreak}天连击"

        // 2. 更新星标状态（根据 habitItem.isStared 字段）
        holder.starIcon.visibility = if (habitItem.isStared) View.VISIBLE else View.GONE

        // 3. 设置频率文本（利用 HabitMetaData 中的 frequencyType 和 frequency）
        Log.d("Database", "${habitMeta.frequencyType}")
        val freqDesc = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY ->
                "每天${habitMeta.frequency}" + if (habitMeta.total == 0) "次" else "次/共${habitMeta.total}天"
            DeadlineFrequency.WEEKLY ->
                "每周${habitMeta.frequency}" + if (habitMeta.total == 0) "次" else "次/共${habitMeta.total}天"
            DeadlineFrequency.MONTHLY ->
                "每月${habitMeta.frequency}" + if (habitMeta.total == 0) "次" else "次/共${habitMeta.total}天"
            DeadlineFrequency.TOTAL -> {
                if (habitMeta.total == 0) "持续坚持"
                else "共计${habitMeta.total}次"
            }
        }

        frequencyText.text = freqDesc + if (habitItem.endTime != "null") {
            val now = LocalDateTime.now()
            val endTime = GlobalUtils.safeParseDateTime(habitItem.endTime)
            val duration = Duration.between(now, endTime)
            " · 剩余${duration.toDays()}天"
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

        // 5. 更新月度进度
        val currentMonth = YearMonth.now()

        val progress: Float
        if (habitMeta.total == 0) {
            // 若未制定总次数，则以月进度计
            val completedThisMonth = if (habitMeta.frequencyType == DeadlineFrequency.TOTAL)
                habitItem.habitCount
            else
                completedDates.count { YearMonth.from(it) == currentMonth }
            val monthlyGoal = when (habitMeta.frequencyType) {
                DeadlineFrequency.DAILY -> currentMonth.lengthOfMonth()
                DeadlineFrequency.WEEKLY -> habitMeta.frequency * 4
                DeadlineFrequency.MONTHLY -> habitMeta.frequency
                DeadlineFrequency.TOTAL -> habitMeta.total
            }
            progress = (completedThisMonth.toFloat() / monthlyGoal * 100).coerceAtMost(100f)
        } else {
            progress = (completedDates.size.toFloat() / habitMeta.total.toFloat() * 100).coerceAtMost(100f)
        }

        monthProgress.progress = progress.toInt()

        progressLabel.text = when (habitMeta.frequencyType) {
            DeadlineFrequency.TOTAL -> ""
            DeadlineFrequency.DAILY -> "每日进度 ${habitItem.habitCount}/${habitMeta.frequency}"
            DeadlineFrequency.WEEKLY -> "每周进度 ${habitItem.habitCount}/${habitMeta.frequency}"
            DeadlineFrequency.MONTHLY -> "每月进度 ${habitItem.habitCount}/${habitMeta.frequency}"
        }

        // 6. 设置打卡按钮状态
        val canCheckIn = (habitMeta.total != 0 && if (habitMeta.frequencyType == DeadlineFrequency.TOTAL) {
            (habitItem.habitCount < habitMeta.total)
        } else {
            (habitItem.habitCount < habitMeta.frequency) && (completedDates.size < habitMeta.total)
        }) || (habitMeta.total == 0)

        Log.d("Check", "habitItem: ${habitItem.name} | type: ${habitMeta.frequencyType} | count: ${habitItem.habitCount} | canPerformClick: $canCheckIn")
        val alreadyChecked = if (habitMeta.frequencyType == DeadlineFrequency.DAILY) {
            habitItem.habitCount >= habitMeta.frequency
        } else today in completedDates
        val canPerformClick = canCheckIn && !alreadyChecked
        checkButton.text = if (alreadyChecked) "今日已打卡" else "打卡"
        checkButton.icon = if (alreadyChecked) null
        else ContextCompat.getDrawable(context, R.drawable.ic_check)

        // 7. 设置点击监听（传入 context 给 onCheckInClick）
        checkButton.setOnClickListener { onCheckInClick(context, habitItem, canPerformClick) }

        if (completedDates.size >= habitMeta.total) {
            constraintLayout.setBackgroundResource(R.drawable.item_background_finished)
        }

        if (selectedPositions.contains(position)) {
            constraintLayout.setBackgroundResource(R.drawable.item_background_selected)
        } else {
            constraintLayout.setBackgroundResource(R.drawable.item_background)
        }
    }

    /**
     * 打卡操作：检查当天是否已打卡，若未打卡则更新 note 字段、habitCount，
     * 并调用数据库更新和刷新数据
     */
    private fun onCheckInClick(context: Context, habitItem: DDLItem, canPerformClick: Boolean) {
        if (!canPerformClick) {
            onCheckInClickGlobalListener?.onCheckInClickGlobal(context, habitItem, false)
            return
        }

        val today = LocalDate.now()

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

        viewModel.loadData(viewModel.currentType)
    }

    /**
     * 辅助函数：自动清零
     */
    private fun refreshCount() {

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
            remainingTimeText.text = "DDL已完成🎉"
            remainingTimeTextAlt.text = "已完成"
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