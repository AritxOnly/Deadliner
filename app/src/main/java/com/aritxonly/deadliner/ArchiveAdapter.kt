package com.aritxonly.deadliner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.format.DateTimeFormatter

class ArchiveAdapter(
    public var itemList: List<DDLItem>,
    private val context: Context
) : RecyclerView.Adapter<ArchiveAdapter.ViewHolder>() {

    private var filteredItemList: List<DDLItem> = filterItems(itemList, context) // ✅ 初始化时就筛选

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val archiveTitleText: TextView = itemView.findViewById(R.id.archiveTitleText)
        val endingTimeText: TextView = itemView.findViewById(R.id.endingTimeText)
        val archiveDeleteButton: MaterialButton = itemView.findViewById(R.id.archiveDeleteButton)
        val archiveNoteText: TextView = itemView.findViewById(R.id.archiveNoteText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.archived_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItemList[position]
        val endTime = GlobalUtils.safeParseDateTime(item.endTime)

        holder.archiveTitleText.text = item.name
        holder.archiveNoteText.text = item.note

        val displayFullContent = holder.archiveNoteText.text.isEmpty()

        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
        val formatterAlt = DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
        holder.endingTimeText.text = if (displayFullContent) {
            endTime.format(formatter)
        } else {
            endTime.format(formatterAlt)
        }

        val databaseHelper = DatabaseHelper.getInstance(context)

        holder.archiveDeleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(holder.itemView.context)
                .setTitle("删除已完成的 Deadline？")
                .setPositiveButton("确定") { _, _ ->
                    databaseHelper.deleteDDL(item.id)

                    // 重新获取数据 & 过滤
                    updateData(databaseHelper.getAllDDLs(), context)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    override fun getItemCount(): Int = filteredItemList.size

    // 过滤列表，仅保留符合条件的项目
    private fun filterItems(itemList: List<DDLItem>, context: Context): List<DDLItem> {
        val databaseHelper = DatabaseHelper.getInstance(context)

        return itemList.filterNot { item ->
            if (!item.isCompleted) return@filterNot true
            item.isArchived = (!GlobalUtils.filterArchived(item)) || item.isArchived
            databaseHelper.updateDDL(item)
            !item.isArchived
        }
    }

    // 更新数据并重新筛选
    fun updateData(newItemList: List<DDLItem>, context: Context) {
        filteredItemList = filterItems(newItemList, context)
        notifyDataSetChanged()
    }
}