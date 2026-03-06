data class SubTask(
    val id: Long = 0,
    val ddlId: Long,
    val content: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    // Sync 字段
    val uid: String? = null,
    val deleted: Boolean = false,
    val verTs: String = "1970-01-01T00:00:00Z",
    val verCtr: Int = 0,
    val verDev: String = ""
)