package com.aritxonly.deadliner.model

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val description: String,
    val rrule: String?
)