package com.aritxonly.deadliner.model

import android.os.Parcelable
import com.aritxonly.deadliner.model.DeadlineType
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
data class DDLItem(
    val id: Long = -1,
    val name: String,
    val startTime: String,
    val endTime: String,
    var isCompleted: Boolean = false,
    var completeTime: String = "",
    val note: String,
    var isArchived: Boolean = false,
    var isStared: Boolean = false,
    var type: DeadlineType = DeadlineType.TASK,
    var habitCount: Int = 0,
    var habitTotalCount: Int = 0,
    var calendarEventId: Long? = null,
    var timeStamp: String = LocalDateTime.now().toString()
) : Parcelable