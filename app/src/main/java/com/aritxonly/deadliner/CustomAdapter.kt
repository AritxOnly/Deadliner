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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.microedition.khronos.opengles.GL

class CustomAdapter(
    public var itemList: List<DDLItem>,
    private val context: Context
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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

    // 更新数据的方法，用于动态刷新 RecyclerView
    /*fun updateData(newList: List<DDLItem>, context: Context) {
        val databaseHelper = DatabaseHelper.getInstance(context)
        val filteredList = newList.filter { item ->
            Log.d("updateData", "item ${item.id}, " +
                    "name ${item.name}, " +
                    "completeTime ${item.completeTime}," +
                    "isArchived ${item.isArchived}")
            if (item.completeTime.isNotEmpty()) {
                item.isArchived = (!GlobalUtils.filterArchived(item)) || item.isArchived
                databaseHelper.updateDDL(item)
                !item.isArchived
            } else {
                true // 如果 completeTime 为空，保留该项目
            }
        }.sortedWith(
            compareBy<DDLItem> { it.isCompleted }
                .thenBy { !it.isStared }
                .thenBy {
                    when (GlobalUtils.filterSelection) {
                        1 -> {  // 按名称
                            it.name
                        }
                        2 -> {  // 按开始时间
                            GlobalUtils.parseDateTime(it.startTime)
                        }
                        3 -> {  // 按百分比
                            val startTime = GlobalUtils.parseDateTime(it.startTime)
                            val endTime = GlobalUtils.parseDateTime(it.endTime)
                            val remainingMinutes =
                                Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                            val fullTime =
                                Duration.between(startTime, endTime).toMinutes().toInt()
                            val progress = remainingMinutes.toFloat() / fullTime.toFloat()
                            progress
                        }
                        else -> {
                            val endTime = GlobalUtils.parseDateTime(it.endTime)
                            val remainingMinutes =
                                Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                            remainingMinutes
                        }
                    }
                }
        )

        itemList = filteredList
        notifyDataSetChanged()
    }
    */
}