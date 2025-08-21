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
        private const val DATABASE_VERSION = 11
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
        private const val COLUMN_UID = "uid"               // 跨端稳定ID，例如 "a83f05:24"
        private const val COLUMN_DELETED = "deleted"       // 0/1，软删墓碑
        private const val COLUMN_VER_TS = "ver_ts"         // 版本时间（UTC ISO8601）
        private const val COLUMN_VER_CTR = "ver_ctr"       // 版本计数（HLC counter）
        private const val COLUMN_VER_DEV = "ver_dev"       // 版本设备ID
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
                $COLUMN_TIMESTAMP TEXT,
                $COLUMN_UID TEXT UNIQUE,
                $COLUMN_DELETED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_VER_TS TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z',
                $COLUMN_VER_CTR INTEGER NOT NULL DEFAULT 0,
                $COLUMN_VER_DEV TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        db.execSQL("""
        CREATE TABLE IF NOT EXISTS sync_state(
          id INTEGER PRIMARY KEY CHECK(id=1),
          device_id TEXT NOT NULL,
          last_local_ts TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z',
          last_local_ctr INTEGER NOT NULL DEFAULT 0
        )
    """.trimIndent())
        db.execSQL("INSERT OR IGNORE INTO sync_state(id, device_id) VALUES(1, hex(randomblob(3)))")
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
            db.transaction {
                try {
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TYPE TEXT DEFAULT 'task'")
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_HABIT_COUNT INT DEFAULT 0")
                } catch (e: Exception) {
                    Log.e("DatabaseHelper", e.toString())
                } finally {
                }
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
        if (oldVersion < 11) {
            db.transaction {
                try {
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_UID TEXT")
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DELETED INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_VER_TS TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_VER_CTR INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_VER_DEV TEXT NOT NULL DEFAULT ''")

                    // 确保 sync_state 存在
                    execSQL(
                        """
            CREATE TABLE IF NOT EXISTS sync_state(
              id INTEGER PRIMARY KEY CHECK(id=1),
              device_id TEXT NOT NULL,
              last_local_ts TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z',
              last_local_ctr INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
                    )
                    execSQL("INSERT OR IGNORE INTO sync_state(id, device_id) VALUES(1, hex(randomblob(3)))")

                    // 回填 uid / 版本信息
                    // 取 device_id
                    val c = rawQuery("SELECT device_id FROM sync_state WHERE id=1", null)
                    c.moveToFirst()
                    val deviceId = c.getString(0)
                    c.close()

                    // 用原有 timestamp 作为初始 ver_ts（无则 now）
                    execSQL(
                        """
            UPDATE $TABLE_NAME
               SET $COLUMN_UID = COALESCE($COLUMN_UID, (${'?'} || ':' || $COLUMN_ID)),
                   $COLUMN_VER_TS = CASE 
                        WHEN $COLUMN_TIMESTAMP IS NOT NULL AND $COLUMN_TIMESTAMP <> '' THEN $COLUMN_TIMESTAMP
                        ELSE datetime('now')
                   END,
                   $COLUMN_VER_DEV = CASE WHEN $COLUMN_VER_DEV='' THEN ${'?'} ELSE $COLUMN_VER_DEV END
        """.trimIndent(), arrayOf(deviceId, deviceId)
                    )

                    // 唯一索引
                    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_ddl_uid ON $TABLE_NAME($COLUMN_UID)")
                } finally {
                }
            }
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
            put(COLUMN_DELETED, 0)
            put(COLUMN_VER_TS, java.time.Instant.now().toString())
            put(COLUMN_VER_CTR, 0)
            put(COLUMN_VER_DEV, getDeviceId())
        }

        val id = db.insert(TABLE_NAME, null, values)

        val uid = "${getDeviceId()}:$id"
        val v = nextVersionUTC()
        val cv2 = ContentValues().apply {
            put(COLUMN_UID, uid)
            put(COLUMN_VER_TS, v.ts); put(COLUMN_VER_CTR, v.ctr); put(COLUMN_VER_DEV, v.dev)
        }
        db.update(TABLE_NAME, cv2, "$COLUMN_ID=?", arrayOf(id.toString()))
        return id
    }

    // 获取所有 DDL 数据
    fun getAllDDLs(): List<DDLItem> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_DELETED = 0",
            null, null, null, null
        )
        return parseCursor(cursor)
    }

    fun getDDLsByType(type: DeadlineType): List<DDLItem> {
        val db = readableDatabase
        val selection = "$COLUMN_DELETED = 0 AND $COLUMN_TYPE = ?"
        val selectionArgs = arrayOf(type.toString().lowercase())
        val cursor = db.query(
            TABLE_NAME, null, selection, selectionArgs, null, null,
            "$COLUMN_IS_COMPLETED ASC, $COLUMN_END_TIME ASC"
        )
        return parseCursor(cursor)
    }

    fun getDDLById(id: Long): DDLItem? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null,
            "$COLUMN_DELETED = 0 AND $COLUMN_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return parseCursor(cursor).firstOrNull()
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

    fun updateDDL(item: DDLItem) {
        val db = writableDatabase
        val v = nextVersionUTC()
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
            put(COLUMN_VER_TS, v.ts); put(COLUMN_VER_CTR, v.ctr); put(COLUMN_VER_DEV, v.dev)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(item.id.toString()))
    }

    fun deleteDDL(id: Long) {
        val db = writableDatabase
        val v = nextVersionUTC()
        val values = ContentValues().apply {
            put(COLUMN_DELETED, 1)
            put(COLUMN_VER_TS, v.ts); put(COLUMN_VER_CTR, v.ctr); put(COLUMN_VER_DEV, v.dev)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
    // endregion

    fun getDeviceId(): String = readableDatabase.rawQuery(
        "SELECT device_id FROM sync_state WHERE id=1", null
    ).use { it.moveToFirst(); it.getString(0) }

    private fun getLastLocalVer(): Ver = readableDatabase.rawQuery(
        "SELECT last_local_ts, last_local_ctr, device_id FROM sync_state WHERE id=1", null
    ).use { it.moveToFirst(); Ver(it.getString(0), it.getInt(1), it.getString(2)) }

    // 生成“下一版本”（简单 HLC）：时间未前进则 ctr+1
    fun nextVersionUTC(): Ver {
        val now = java.time.Instant.now().toString()
        val last = getLastLocalVer()
        val dev = getDeviceId()
        val newer = if (now > last.ts) Ver(now, 0, dev) else Ver(last.ts, last.ctr + 1, dev)
        writableDatabase.execSQL(
            "UPDATE sync_state SET last_local_ts=?, last_local_ctr=? WHERE id=1",
            arrayOf(newer.ts, newer.ctr)
        )
        return newer
    }
}

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this != 0