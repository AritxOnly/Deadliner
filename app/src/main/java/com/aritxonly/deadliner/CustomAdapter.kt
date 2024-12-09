package com.aritxonly.deadliner

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CustomAdapter(
    public var itemList: List<DDLItem>,
    private val context: Context // 传递 Context
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val remainingTimeText: TextView = itemView.findViewById(R.id.remainingTimeText)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        val constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)
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
    
    // 尝试解析时间字符串的函数
    fun parseDateTime(dateTimeString: String): LocalDateTime {
        val formatters = listOf(
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        val currentTime = LocalDateTime.now()

        // 将字符串时间转换为 LocalDateTime
        val startTime = parseDateTime(item.startTime)
        val endTime = parseDateTime(item.endTime)

        // 计算剩余时间
        val remainingDuration = Duration.between(currentTime, endTime)
        val remainingMinutes = remainingDuration.toMinutes().toInt()

        // 计算总时长（以分钟为单位）
        val totalDuration = Duration.between(startTime, endTime).toMinutes().toInt()

        // 设置标题
        holder.titleText.text = item.name

        // 设置剩余时间显示
        holder.remainingTimeText.text = if (remainingMinutes >= 0) {
            "${remainingMinutes / 60}小时${remainingMinutes % 60}分钟 到 DDL"
        } else {
            "DDL逾期！！！"
        }

        // 计算并设置进度条进度，确保至少为 1
        val progress = if (totalDuration > 0 && remainingMinutes <= totalDuration) {
            (remainingMinutes * 100 / totalDuration).coerceIn(1, 100)
        } else {
            100
        }
        holder.progressBar.setProgressCompat(progress, true)

        // 绑定单击事件
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        // 使用 getThemeColor 获取主题颜色
        val progressColor = getThemeColor(android.R.attr.colorPrimary)
        val progressNearbyColor = getThemeColor(android.R.attr.colorError)
        val progressPassedColor = getThemeColor(android.R.attr.colorControlHighlight)
        if (remainingMinutes < 0) {
            holder.progressBar.setIndicatorColor(progressPassedColor)
            holder.progressBar.setBackgroundColor(progressPassedColor)
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
    fun updateData(newList: List<DDLItem>) {
        itemList = newList.sortedWith(compareBy<DDLItem> { it.isCompleted }
            .thenBy {
                val endTime = parseDateTime(it.endTime)
                val remainingMinutes = Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                remainingMinutes
            })
        notifyDataSetChanged()
    }
}