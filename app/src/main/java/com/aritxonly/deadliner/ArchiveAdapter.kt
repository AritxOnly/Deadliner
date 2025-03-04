package com.aritxonly.deadliner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aritxonly.deadliner.CustomAdapter.ViewHolder
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ArchiveAdapter(
    public var itemList: List<DDLItem>,
    private val context: Context,
    private val databaseHelper: DatabaseHelper
) : RecyclerView.Adapter<ArchiveAdapter.ViewHolder>() {

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
        val item = itemList[position]
        val endTime = parseDateTime(item.endTime)

        holder.archiveTitleText.text = item.name


        holder.archiveDeleteButton.setOnClickListener {
            databaseHelper.deleteDDL(item.id)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

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
}