package com.aritxonly.deadliner.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.aritxonly.deadliner.model.ChangeLine
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.SyncState
import com.aritxonly.deadliner.model.Ver
import java.time.LocalDateTime
import androidx.core.database.sqlite.transaction

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also {
                    instance = it
                }
            }
        }

        fun closeInstance() {
            instance?.close()
            instance = null
        }

        const val DATABASE_NAME = "deadliner.db"
        private const val DATABASE_VERSION = 10
        private const val TABLE_NAME = "ddl_items"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_START_TIME = "start_time"
        private const val COLUMN_END_TIME = "end_time"
        private const val COLUMN_IS_COMPLETED = "is_completed"
        private const val COLUMN_COMPLETE_TIME = "complete_time"
        private const val COLUMN_NOTE = "note"
        private const val COLUMN_IS_ARCHIVED = "is_archived"
        private const val COLUMN_IS_STARED = "is_stared"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_HABIT_COUNT = "habit_count"
        private const val COLUMN_HABIT_TOTAL_COUNT = "habit_total_count"
        private const val COLUMN_CALENDAR_EVENT_ID = "calendar_event"
        private const val COLUMN_TIMESTAMP = "timestamp"

        // ===== 同步相关：新表名 =====
        private const val TABLE_SYNC_MAP = "sync_map"           // uid ↔ local_id
        private const val TABLE_SYNC_JOURNAL = "sync_journal"   // 本机增量日志
        private const val TABLE_SYNC_STATE = "sync_state"       // 同步指针

        // ===== sync_state 列 =====
        private const val SS_ID = "id"
        private const val SS_DEVICE_ID = "device_id"
        private const val SS_SNAPSHOT_ETAG = "snapshot_etag"    // 预留（如你也做全量文件）
        private const val SS_LAST_UPLOADED_SEQ = "last_uploaded_seq"
        private const val SS_LAST_SYNC_AT = "last_sync_at"
        private const val SS_CHANGES_ETAG = "changes_etag"      // 当前月份 changes 文件的 ETag
        private const val SS_CHANGES_OFFSET = "changes_offset"  // 已处理到的字节偏移

        // ===== sync_journal 列 =====
        private const val J_SEQ = "seq"
        private const val J_UID = "uid"
        private const val J_OP = "op"               // 'upsert' | 'delete'
        private const val J_SNAPSHOT = "snapshot"   // 行 JSON（upsert 时完整快照）
        private const val J_VER_TS = "ver_ts"       // ISO8601（UTC）
        private const val J_VER_CTR = "ver_ctr"     // 同毫秒计数器
        private const val J_VER_DEV = "ver_dev"     // 设备 ID（短码）

        // ===== sync_map 列 =====
        private const val M_UID = "uid"
        private const val M_LOCAL_ID = "local_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_START_TIME TEXT NOT NULL,
                $COLUMN_END_TIME TEXT NOT NULL,
                $COLUMN_IS_COMPLETED INTEGER,
                $COLUMN_COMPLETE_TIME TEXT NOT NULL,
                $COLUMN_NOTE TEXT NOT NULL,
                $COLUMN_IS_ARCHIVED INTEGER,
                $COLUMN_IS_STARED INTEGER,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_HABIT_COUNT INTEGER,
                $COLUMN_HABIT_TOTAL_COUNT INTEGER,
                $COLUMN_CALENDAR_EVENT_ID INTEGER,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        createSyncTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "version $oldVersion => $newVersion")
        if (oldVersion < 2) {
            Log.d("DatabaseHelper", "Am I here?")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_COMPLETE_TIME TEXT DEFAULT ''")
        }
        if (oldVersion < 3) {
            Log.d("DatabaseHelper", "Update DB to v3")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NOTE TEXT DEFAULT ''")
        }
        if (oldVersion < 4) {
            Log.d("DatabaseHelper", "Update DB to v4")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IS_ARCHIVED INT DEFAULT 0")
        }
        if (oldVersion < 5) {
            Log.d("DatabaseHelper", "Update DB to v5")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IS_STARED INT DEFAULT 0")
        }
        if (oldVersion < 6) {
            Log.d("DatabaseHelper", "Update DB to v6")
            db.beginTransaction()
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TYPE TEXT DEFAULT 'task'")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_HABIT_COUNT INT DEFAULT 0")
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DatabaseHelper", e.toString())
            } finally {
                db.endTransaction()
            }
        }
        if (oldVersion < 7) {
            Log.d("DatabaseHelper", "Update DB to v7")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CALENDAR_EVENT_ID INT DEFAULT -1")
        }
        if (oldVersion < 8) {
            Log.d("DatabaseHelper", "Update DB to v8")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_HABIT_TOTAL_COUNT INT DEFAULT 0")
        }
        if (oldVersion < 9) {
            Log.d("DatabaseHelper", "Update DB to v9")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TIMESTAMP TEXT DEFAULT '${LocalDateTime.now()}'")
        }
        if (oldVersion < 10) {
            Log.d("DatabaseHelper", "Update DB to v10")
            createSyncTables(db)
        }
    }

    // region Deadline数据库
    // 插入 DDL 数据
    fun insertDDL(
        name: String,
        startTime: String,
        endTime: String,
        note: String = "",
        type: DeadlineType = DeadlineType.TASK,
        calendarEventId: Long? = null,
    ): Long {
        Log.d("Database", "Inserting $name, $startTime, $endTime, $note, $type")
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_START_TIME, startTime)
            put(COLUMN_END_TIME, endTime)
            put(COLUMN_IS_COMPLETED, false)
            put(COLUMN_COMPLETE_TIME, "")
            put(COLUMN_NOTE, note)
            put(COLUMN_IS_ARCHIVED, false)
            put(COLUMN_IS_STARED, false)
            put(COLUMN_TYPE, type.toString())
            put(COLUMN_HABIT_COUNT, 0)
            put(COLUMN_HABIT_TOTAL_COUNT, 0)
            put(COLUMN_CALENDAR_EVENT_ID, (calendarEventId?:-1).toInt())
            put(COLUMN_TIMESTAMP, LocalDateTime.now().toString())
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // 获取所有 DDL 数据
    fun getAllDDLs(): List<DDLItem> {
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, null)

        return parseCursor(cursor)
    }

    fun getDDLsByType(type: DeadlineType): List<DDLItem> {
        val db = readableDatabase
        val selection = "$COLUMN_TYPE = ?"
        val selectionArgs = arrayOf(type.toString().lowercase()) // 确保小写匹配

        val cursor = db.query(
            TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_IS_COMPLETED ASC, $COLUMN_END_TIME ASC" // 默认排序规则
        )

        return parseCursor(cursor)
    }

    private fun parseCursor(cursor: Cursor): List<DDLItem> {
        fun parseCalendarEventId(id: Int): Long? {
            return if (id == -1) null else id.toLong()
        }

        val result = mutableListOf<DDLItem>()
        with(cursor) {
            while (moveToNext()) {
                result.add(
                    DDLItem(
                        id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                        name = getString(getColumnIndexOrThrow(COLUMN_NAME)),
                        startTime = getString(getColumnIndexOrThrow(COLUMN_START_TIME)),
                        endTime = getString(getColumnIndexOrThrow(COLUMN_END_TIME)),
                        isCompleted = getInt(getColumnIndexOrThrow(COLUMN_IS_COMPLETED)).toBoolean(),
                        completeTime = getString(getColumnIndexOrThrow(COLUMN_COMPLETE_TIME)),
                        note = getString(getColumnIndexOrThrow(COLUMN_NOTE)),
                        isArchived = getInt(getColumnIndexOrThrow(COLUMN_IS_ARCHIVED)).toBoolean(),
                        isStared = getInt(getColumnIndexOrThrow(COLUMN_IS_STARED)).toBoolean(),
                        type = DeadlineType.Companion.fromString(
                            getString(
                                getColumnIndexOrThrow(
                                    COLUMN_TYPE
                                )
                            )
                        ),
                        habitCount = getInt(getColumnIndexOrThrow(COLUMN_HABIT_COUNT)),
                        habitTotalCount = getInt(getColumnIndexOrThrow(COLUMN_HABIT_TOTAL_COUNT)),
                        calendarEventId = parseCalendarEventId(
                            getInt(getColumnIndexOrThrow(COLUMN_CALENDAR_EVENT_ID))
                        ),
                        timeStamp = getString(getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                )
            }
            close()
        }
        return result
    }

    fun getDDLById(id: Long): DDLItem? {
        val db = readableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())

        val cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null)
        return parseCursor(cursor).firstOrNull()
    }

    fun updateDDL(item: DDLItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, item.name)
            put(COLUMN_START_TIME, item.startTime)
            put(COLUMN_END_TIME, item.endTime)
            put(COLUMN_IS_COMPLETED, item.isCompleted.toInt())
            put(COLUMN_COMPLETE_TIME, item.completeTime)
            put(COLUMN_NOTE, item.note)
            put(COLUMN_IS_ARCHIVED, item.isArchived.toInt())
            put(COLUMN_IS_STARED, item.isStared.toInt())
            put(COLUMN_TYPE, item.type.toString())
            put(COLUMN_HABIT_COUNT, item.habitCount)
            put(COLUMN_HABIT_TOTAL_COUNT, item.habitTotalCount)
            put(COLUMN_CALENDAR_EVENT_ID, item.calendarEventId?:-1)
            put(COLUMN_TIMESTAMP, LocalDateTime.now().toString())
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(item.id.toString()))
    }

    fun deleteDDL(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
    // endregion

    // region 同步表
    private fun createSyncTables(db: SQLiteDatabase) {
        // 映射表：uid ↔ local_id
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS sync_map(
          uid TEXT PRIMARY KEY,
          local_id INTEGER NOT NULL,
          created_at TEXT NOT NULL DEFAULT (datetime('now'))
        )
    """.trimIndent())

        // local_id 唯一索引（索引名是一个整体标识符）
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_map_local ON sync_map(local_id)")

        // 变更日志
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS sync_journal(
          seq INTEGER PRIMARY KEY AUTOINCREMENT,
          uid TEXT NOT NULL,
          op TEXT NOT NULL CHECK(op IN ('upsert','delete')),
          snapshot TEXT,
          ver_ts TEXT NOT NULL,
          ver_ctr INTEGER NOT NULL DEFAULT 0,
          ver_dev TEXT NOT NULL,
          at TEXT NOT NULL DEFAULT (datetime('now'))
        )
    """.trimIndent())

        // 同步状态
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS sync_state(
          id INTEGER PRIMARY KEY CHECK(id=1),
          device_id TEXT NOT NULL,
          snapshot_etag TEXT,
          last_uploaded_seq INTEGER DEFAULT 0,
          last_sync_at TEXT,
          changes_etag TEXT,
          changes_offset INTEGER DEFAULT 0
        )
    """.trimIndent())
        db.execSQL("""
        INSERT OR IGNORE INTO sync_state(id, device_id, last_sync_at)
        VALUES (1, substr(lower(hex(randomblob(8))),1,6), datetime('now'))
    """.trimIndent())
    }

    // 获取设备ID
    fun getDeviceId(): String = readableDatabase
        .rawQuery("SELECT $SS_DEVICE_ID FROM $TABLE_SYNC_STATE WHERE $SS_ID=1", null)
        .use { it.moveToFirst(); it.getString(0) }

    // 确保/创建 uid ↔ local_id 映射（本机新纪录）
    fun ensureUidForLocalId(localId: Long): String {
        val db = writableDatabase
        db.rawQuery("SELECT $M_UID FROM $TABLE_SYNC_MAP WHERE $M_LOCAL_ID=?",
            arrayOf(localId.toString())).use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        val uid = "${getDeviceId()}:$localId"
        db.execSQL("INSERT OR IGNORE INTO $TABLE_SYNC_MAP($M_UID,$M_LOCAL_ID) VALUES(?,?)",
            arrayOf(uid, localId))
        return uid
    }

    // 序列化 ddl_items 指定行为 JSON（作为变更快照）
    private val gson = com.google.gson.Gson()
    private fun snapshotOf(localId: Long): String {
        val db = readableDatabase
        db.rawQuery("""
        SELECT $COLUMN_ID,$COLUMN_NAME,$COLUMN_START_TIME,$COLUMN_END_TIME,$COLUMN_IS_COMPLETED,
               $COLUMN_COMPLETE_TIME,$COLUMN_NOTE,$COLUMN_IS_ARCHIVED,$COLUMN_IS_STARED,$COLUMN_TYPE,
               $COLUMN_HABIT_COUNT,$COLUMN_HABIT_TOTAL_COUNT,$COLUMN_CALENDAR_EVENT_ID,$COLUMN_TIMESTAMP
        FROM $TABLE_NAME WHERE $COLUMN_ID=?
    """.trimIndent(), arrayOf(localId.toString())).use { c ->
            if (!c.moveToFirst()) return "{}"
            val obj = com.google.gson.JsonObject().apply {
                addProperty("id", c.getLong(0))
                addProperty("name", c.getString(1))
                addProperty("start_time", c.getString(2))
                addProperty("end_time", c.getString(3))
                addProperty("is_completed", c.getInt(4))
                addProperty("complete_time", c.getString(5))
                addProperty("note", c.getString(6))
                addProperty("is_archived", c.getInt(7))
                addProperty("is_stared", c.getInt(8))
                addProperty("type", c.getString(9))
                addProperty("habit_count", c.getInt(10))
                addProperty("habit_total_count", c.getInt(11))
                addProperty("calendar_event_id", c.getInt(12))
                addProperty("timestamp", c.getString(13))
            }
            return gson.toJson(obj) // ← 紧凑单行，无换行
        }
    }

    // 记录 upsert（插入/更新后调用）
    fun journalUpsert(localId: Long) {
        val db = writableDatabase
        db.transaction {
            try {
                val uid = ensureUidForLocalId(localId)
                val dev = getDeviceId()
                val nowIso = java.time.Instant.now().toString()
                var lastTs: String? = null;
                var lastCtr = 0
                rawQuery(
                    "SELECT $J_VER_TS,$J_VER_CTR FROM $TABLE_SYNC_JOURNAL WHERE $J_UID=? ORDER BY $J_SEQ DESC LIMIT 1",
                    arrayOf(uid)
                ).use {
                    if (it.moveToFirst()) {
                        lastTs = it.getString(0); lastCtr = it.getInt(1)
                    }
                }
                val ctr = if (nowIso == lastTs) lastCtr + 1 else 0
                val snap = snapshotOf(localId)
                execSQL(
                    """
            INSERT INTO $TABLE_SYNC_JOURNAL($J_UID,$J_OP,$J_SNAPSHOT,$J_VER_TS,$J_VER_CTR,$J_VER_DEV)
            VALUES (?,?,?,?,?,?)
        """.trimIndent(), arrayOf(uid, "upsert", snap, nowIso, ctr, dev)
                )
            } finally {
            } }
    }

    // 记录 delete（物理删除前调用）
    fun journalDelete(localId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery("SELECT $M_UID FROM $TABLE_SYNC_MAP WHERE $M_LOCAL_ID=?",
                arrayOf(localId.toString())).use { c ->
                if (!c.moveToFirst()) { db.setTransactionSuccessful(); return }
                val uid = c.getString(0)
                val dev = getDeviceId()
                val nowIso = java.time.Instant.now().toString()
                var lastTs: String? = null; var lastCtr = 0
                db.rawQuery("SELECT $J_VER_TS,$J_VER_CTR FROM $TABLE_SYNC_JOURNAL WHERE $J_UID=? ORDER BY $J_SEQ DESC LIMIT 1",
                    arrayOf(uid)).use { if (it.moveToFirst()) { lastTs = it.getString(0); lastCtr = it.getInt(1) } }
                val ctr = if (nowIso == lastTs) lastCtr + 1 else 0
                val minimalSnap = """{"id":$localId}"""
                db.execSQL("""
                INSERT INTO $TABLE_SYNC_JOURNAL($J_UID,$J_OP,$J_SNAPSHOT,$J_VER_TS,$J_VER_CTR,$J_VER_DEV)
                VALUES (?,?,?,?,?,?)
            """.trimIndent(), arrayOf(uid, "delete", minimalSnap, nowIso, ctr, dev))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    // 读取尚未上传的本地增量
    fun loadJournalAfterSeq(seq: Long, limit: Int = 1000): List<ChangeLine> {
        val out = mutableListOf<ChangeLine>()
        readableDatabase.rawQuery("""
        SELECT $J_UID,$J_OP,$J_SNAPSHOT,$J_VER_TS,$J_VER_CTR,$J_VER_DEV
        FROM $TABLE_SYNC_JOURNAL WHERE $J_SEQ > ? ORDER BY $J_SEQ ASC LIMIT ?
    """.trimIndent(), arrayOf(seq.toString(), limit.toString())).use { c ->
            while (c.moveToNext()) {
                out.add(ChangeLine(
                    op = c.getString(1),
                    uid = c.getString(0),
                    snapshot = c.getString(2),
                    ver = Ver(c.getString(3), c.getInt(4), c.getString(5))
                ))
            }
        }
        return out
    }

    // 同步状态
    fun getSyncState(): SyncState = readableDatabase
        .rawQuery("""
        SELECT $SS_DEVICE_ID,$SS_LAST_UPLOADED_SEQ,$SS_CHANGES_ETAG,$SS_CHANGES_OFFSET
        FROM $TABLE_SYNC_STATE WHERE $SS_ID=1
    """.trimIndent(), null).use {
            it.moveToFirst()
            SyncState(
                deviceId = it.getString(0),
                lastUploadedSeq = it.getLong(1),
                changesEtag = it.getString(2),
                changesOffset = it.getLong(3)
            )
        }

    fun markUploadedThrough(maxSeq: Long) {
        val cv = ContentValues().apply {
            put(SS_LAST_UPLOADED_SEQ, maxSeq)
            put(SS_LAST_SYNC_AT, LocalDateTime.now().toString())
        }
        writableDatabase.update(TABLE_SYNC_STATE, cv, "$SS_ID=1", null)
    }

    fun updateChangesPointer(etag: String?, offset: Long) {
        val cv = ContentValues().apply {
            put(SS_CHANGES_ETAG, etag)
            put(SS_CHANGES_OFFSET, offset)
            put(SS_LAST_SYNC_AT, LocalDateTime.now().toString())
        }
        writableDatabase.update(TABLE_SYNC_STATE, cv, "$SS_ID=1", null)
    }

    // 应用远端变更到本地（覆盖式，保持原表零改动）
    fun applyRemoteChange(change: ChangeLine) {
        val db = writableDatabase
        db.transaction {
            try {
                when (change.op) {
                    "upsert" -> {
                        val cv = ContentValues()
                        val jo =
                            com.google.gson.JsonParser.parseString(change.snapshot).asJsonObject

                        fun s(n: String) =
                            if (jo.has(n) && !jo.get(n).isJsonNull) jo.get(n).asString else null

                        fun i(n: String) =
                            if (jo.has(n) && !jo.get(n).isJsonNull) jo.get(n).asInt else 0

                        cv.put(COLUMN_NAME, s("name") ?: "")
                        cv.put(COLUMN_START_TIME, s("start_time") ?: "")
                        cv.put(COLUMN_END_TIME, s("end_time") ?: "")
                        cv.put(COLUMN_IS_COMPLETED, i("is_completed"))
                        cv.put(COLUMN_COMPLETE_TIME, s("complete_time") ?: "")
                        cv.put(COLUMN_NOTE, s("note") ?: "")
                        cv.put(COLUMN_IS_ARCHIVED, i("is_archived"))
                        cv.put(COLUMN_IS_STARED, i("is_stared"))
                        cv.put(COLUMN_TYPE, s("type") ?: DeadlineType.TASK.toString())
                        cv.put(COLUMN_HABIT_COUNT, i("habit_count"))
                        cv.put(COLUMN_HABIT_TOTAL_COUNT, i("habit_total_count"))
                        cv.put(COLUMN_CALENDAR_EVENT_ID, i("calendar_event_id"))
                        cv.put(COLUMN_TIMESTAMP, s("timestamp") ?: LocalDateTime.now().toString())

                        var localId: Long? = null
                        rawQuery(
                            "SELECT $M_LOCAL_ID FROM $TABLE_SYNC_MAP WHERE $M_UID=?",
                            arrayOf(change.uid)
                        ).use { c -> if (c.moveToFirst()) localId = c.getLong(0) }

                        if (localId == null) {
                            localId = insert(TABLE_NAME, null, cv)
                            execSQL(
                                "INSERT OR IGNORE INTO $TABLE_SYNC_MAP($M_UID,$M_LOCAL_ID) VALUES(?,?)",
                                arrayOf(change.uid, localId)
                            )
                        } else {
                            update(TABLE_NAME, cv, "$COLUMN_ID = ?", arrayOf(localId.toString()))
                        }
                    }

                    "delete" -> {
                        rawQuery(
                            "SELECT $M_LOCAL_ID FROM $TABLE_SYNC_MAP WHERE $M_UID=?",
                            arrayOf(change.uid)
                        ).use { c ->
                            if (c.moveToFirst()) {
                                val localId = c.getLong(0)
                                delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(localId.toString()))
                                delete(TABLE_SYNC_MAP, "$M_UID = ?", arrayOf(change.uid))
                            }
                        }
                    }

                    else -> {}
                }
            } finally {
            } }
    }
    // endregion
}

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this != 0