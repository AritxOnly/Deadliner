package com.aritxonly.deadliner.model

data class DeadlinerCheckResp(
    val deviceId: String,
    val month: String,
    val vip: Boolean,
    val limit: Long,
    val used: Long,
    val remaining: Long,
    val reset_at: String
)