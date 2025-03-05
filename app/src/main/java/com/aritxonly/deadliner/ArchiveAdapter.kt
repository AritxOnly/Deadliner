package com.aritxonly.deadliner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ArchiveAdapter(
    public var itemList: List<DDLItem>,
    private val context: Context
) : RecyclerView.Adapter<ArchiveAdapter.ViewHolder>() {

    private var filteredItemList: List<DDLItem> = filterItems(itemList) // ✅ 初始化时就筛选

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val archiveTitleText: TextView = itemView.findViewById(R.id.archiveTitleText)
        val endingTimeText: TextView = itemView.findViewById(R.id.endingTimeText)
        val archiveDeleteButton: MaterialButton = itemView.findViewById(R.id.archiveDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.archived_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItemList[position]
        val endTime = parseDateTime(item.endTime)

        holder.archiveTitleText.text = item.name

        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
        holder.endingTimeText.text = endTime.format(formatter)

        val databaseHelper = DatabaseHelper.getInstance(context)

        holder.archiveDeleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(holder.itemView.context)
                .setTitle("删除已完成的 Deadline？")
                .setPositiveButton("确定") { _, _ ->
                    databaseHelper.deleteDDL(item.id)

                    // 重新获取数据 & 过滤
                    updateData(databaseHelper.getAllDDLs())
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    override fun getItemCount(): Int = filteredItemList.size

    // 解析日期时间
    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        )

        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(dateTimeString, formatter)
            } catch (e: Exception) {
                // 忽略错误，尝试下一个格式
            }
        }
        throw IllegalArgumentException("Invalid date format: $dateTimeString")
    }

    // 过滤列表，仅保留符合条件的项目
    private fun filterItems(itemList: List<DDLItem>): List<DDLItem> {
        return itemList.filterNot { item ->
            if (!item.isCompleted) return@filterNot true

            try {
                val completeTime = parseDateTime(item.completeTime)
                val daysSinceCompletion = Duration.between(completeTime, LocalDateTime.now()).toDays()
                daysSinceCompletion <= 7
            } catch (e: Exception) {
                true
            }
        }
    }

    // 更新数据并重新筛选
    fun updateData(newItemList: List<DDLItem>) {
        filteredItemList = filterItems(newItemList)
        notifyDataSetChanged()
    }
}