package com.aritxonly.deadliner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DDLItem(
    val id: Long = -1,
    val name: String,
    val startTime: String,
    val endTime: String,
    var isCompleted: Boolean = false,
    var completeTime: String = "",
    val note: String,
    var isArchived: Boolean = false
) : Parcelable