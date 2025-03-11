package com.aritxonly.deadliner

import java.time.LocalDateTime

data class DDLItem(
    val id: Long = -1,
    val name: String,
    val startTime: String,
    val endTime: String,
    var isCompleted: Boolean = false,
    var completeTime: String = "",
    val note: String
)