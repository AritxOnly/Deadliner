package com.aritxonly.deadliner.model

data class Ver(val ts: String, val ctr: Int, val dev: String)
data class ChangeLine(val op: String, val uid: String, val snapshot: String?, val ver: Ver)
data class SyncState(
    val deviceId: String,
    val lastUploadedSeq: Long,
    val changesEtag: String?,
    val changesOffset: Long
)
