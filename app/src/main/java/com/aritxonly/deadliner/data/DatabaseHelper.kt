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
        private const val DATABASE_VERSION = 12
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

        // —— 新增 habits / habit_records —— //
        private const val TABLE_HABIT = "habits"
        private const val TABLE_HABIT_RECORD = "habit_records"

        // habits 表字段
        private const val HABIT_ID = "id"
        private const val HABIT_DDL_ID = "ddl_id"
        private const val HABIT_NAME = "name"
        private const val HABIT_DESC = "description"
        private const val HABIT_COLOR = "color"
        private const val HABIT_ICON_KEY = "icon_key"
        private const val HABIT_PERIOD = "period"
        private const val HABIT_TIMES_PER_PERIOD = "times_per_period"
        private const val HABIT_GOAL_TYPE = "goal_type"
        private const val HABIT_TOTAL_TARGET = "total_target"
        private const val HABIT_CREATED_AT = "created_at"
        private const val HABIT_UPDATED_AT = "updated_at"
        private const val HABIT_STATUS = "status"
        private const val HABIT_SORT_ORDER = "sort_order"

        // habit_records 表字段
        private const val HR_ID = "id"
        private const val HR_HABIT_ID = "habit_id"
        private const val HR_DATE = "date"
        private const val HR_COUNT = "count"
        private const val HR_STATUS = "status"
        private const val HR_CREATED_AT = "created_at"
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


        // Habit 表
        db.execSQL(
            """
        CREATE TABLE $TABLE_HABIT (
            $HABIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $HABIT_DDL_ID INTEGER NOT NULL UNIQUE,
            $HABIT_NAME TEXT NOT NULL,
            $HABIT_DESC TEXT,
            $HABIT_COLOR INTEGER,
            $HABIT_ICON_KEY TEXT,
            $HABIT_PERIOD TEXT NOT NULL,
            $HABIT_TIMES_PER_PERIOD INTEGER NOT NULL DEFAULT 1,
            $HABIT_GOAL_TYPE TEXT NOT NULL DEFAULT 'PER_PERIOD',
            $HABIT_TOTAL_TARGET INTEGER,
            $HABIT_CREATED_AT TEXT NOT NULL,
            $HABIT_UPDATED_AT TEXT NOT NULL,
            $HABIT_STATUS TEXT NOT NULL DEFAULT 'ACTIVE',
            $HABIT_SORT_ORDER INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY($HABIT_DDL_ID) REFERENCES $TABLE_NAME($COLUMN_ID) ON DELETE CASCADE
        )
        """.trimIndent()
        )
        db.execSQL(
            """
        CREATE TABLE $TABLE_HABIT_RECORD (
            $HR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $HR_HABIT_ID INTEGER NOT NULL,
            $HR_DATE TEXT NOT NULL,
            $HR_COUNT INTEGER NOT NULL DEFAULT 1,
            $HR_STATUS TEXT NOT NULL DEFAULT 'COMPLETED',
            $HR_CREATED_AT TEXT NOT NULL,
            FOREIGN KEY($HR_HABIT_ID) REFERENCES $TABLE_HABIT($HABIT_ID) ON DELETE CASCADE
        )
        """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_hr_habit_date ON $TABLE_HABIT_RECORD($HR_HABIT_ID, $HR_DATE)")
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
        if (oldVersion < 12) {
            db.transaction {
                // habits
                execSQL(
                    """
            CREATE TABLE IF NOT EXISTS $TABLE_HABIT (
                $HABIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $HABIT_DDL_ID INTEGER NOT NULL UNIQUE,
                $HABIT_NAME TEXT NOT NULL,
                $HABIT_DESC TEXT,
                $HABIT_COLOR INTEGER,
                $HABIT_ICON_KEY TEXT,
                $HABIT_PERIOD TEXT NOT NULL,
                $HABIT_TIMES_PER_PERIOD INTEGER NOT NULL DEFAULT 1,
                $HABIT_GOAL_TYPE TEXT NOT NULL DEFAULT 'PER_PERIOD',
                $HABIT_TOTAL_TARGET INTEGER,
                $HABIT_CREATED_AT TEXT NOT NULL,
                $HABIT_UPDATED_AT TEXT NOT NULL,
                $HABIT_STATUS TEXT NOT NULL DEFAULT 'ACTIVE',
                $HABIT_SORT_ORDER INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY($HABIT_DDL_ID) REFERENCES $TABLE_NAME($COLUMN_ID) ON DELETE CASCADE
            )
            """.trimIndent()
                )

                // habit_records
                execSQL(
                    """
            CREATE TABLE IF NOT EXISTS $TABLE_HABIT_RECORD (
                $HR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $HR_HABIT_ID INTEGER NOT NULL,
                $HR_DATE TEXT NOT NULL,
                $HR_COUNT INTEGER NOT NULL DEFAULT 1,
                $HR_STATUS TEXT NOT NULL DEFAULT 'COMPLETED',
                $HR_CREATED_AT TEXT NOT NULL,
                FOREIGN KEY($HR_HABIT_ID) REFERENCES $TABLE_HABIT($HABIT_ID) ON DELETE CASCADE
            )
            """.trimIndent()
                )

                execSQL("CREATE INDEX IF NOT EXISTS idx_hr_habit_date ON $TABLE_HABIT_RECORD($HR_HABIT_ID, $HR_DATE)")

                migrateLegacyHabits(this)
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

    // region Habit数据库
    fun insertHabit(habit: com.aritxonly.deadliner.model.Habit): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(HABIT_DDL_ID, habit.ddlId)
            put(HABIT_NAME, habit.name)
            put(HABIT_DESC, habit.description)
            habit.color?.let { put(HABIT_COLOR, it) } ?: putNull(HABIT_COLOR)
            put(HABIT_ICON_KEY, habit.iconKey)
            put(HABIT_PERIOD, habit.period.name)              // DAILY / WEEKLY / MONTHLY
            put(HABIT_TIMES_PER_PERIOD, habit.timesPerPeriod)
            put(HABIT_GOAL_TYPE, habit.goalType.name)         // PER_PERIOD / TOTAL
            habit.totalTarget?.let { put(HABIT_TOTAL_TARGET, it) } ?: putNull(HABIT_TOTAL_TARGET)
            put(HABIT_CREATED_AT, habit.createdAt.toString()) // ISO-8601
            put(HABIT_UPDATED_AT, habit.updatedAt.toString())
            put(HABIT_STATUS, habit.status.name)              // ACTIVE / ARCHIVED
            put(HABIT_SORT_ORDER, habit.sortOrder)
        }
        return db.insert(TABLE_HABIT, null, values)
    }

    fun updateHabit(habit: com.aritxonly.deadliner.model.Habit) {
        val db = writableDatabase
        val values = ContentValues().apply {
            // ddlId 一般不变，不更新
            put(HABIT_NAME, habit.name)
            put(HABIT_DESC, habit.description)
            habit.color?.let { put(HABIT_COLOR, it) } ?: putNull(HABIT_COLOR)
            put(HABIT_ICON_KEY, habit.iconKey)
            put(HABIT_PERIOD, habit.period.name)
            put(HABIT_TIMES_PER_PERIOD, habit.timesPerPeriod)
            put(HABIT_GOAL_TYPE, habit.goalType.name)
            habit.totalTarget?.let { put(HABIT_TOTAL_TARGET, it) } ?: putNull(HABIT_TOTAL_TARGET)
            put(HABIT_UPDATED_AT, habit.updatedAt.toString())
            put(HABIT_STATUS, habit.status.name)
            put(HABIT_SORT_ORDER, habit.sortOrder)
        }
        db.update(TABLE_HABIT, values, "$HABIT_ID = ?", arrayOf(habit.id.toString()))
    }

    fun getHabitByDdlId(ddlId: Long): com.aritxonly.deadliner.model.Habit? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT,
            null,
            "$HABIT_DDL_ID = ?",
            arrayOf(ddlId.toString()),
            null,
            null,
            null
        )
        val list = parseHabitCursor(cursor)
        return list.firstOrNull()
    }

    fun getHabitById(id: Long): com.aritxonly.deadliner.model.Habit? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT,
            null,
            "$HABIT_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        val list = parseHabitCursor(cursor)
        return list.firstOrNull()
    }

    fun getAllHabits(): List<com.aritxonly.deadliner.model.Habit> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT,
            null,
            null,
            null,
            null,
            null,
            "$HABIT_SORT_ORDER ASC, $HABIT_ID ASC"
        )
        return parseHabitCursor(cursor)
    }

    fun deleteHabitByDdlId(ddlId: Long) {
        val db = writableDatabase
        db.delete(TABLE_HABIT, "$HABIT_DDL_ID = ?", arrayOf(ddlId.toString()))
    }

    /**
     * 内部帮助函数：把 cursor 解析成 Habit 列表
     */
    private fun parseHabitCursor(cursor: Cursor): List<com.aritxonly.deadliner.model.Habit> {
        val result = mutableListOf<com.aritxonly.deadliner.model.Habit>()
        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(HABIT_ID))
                val ddlId = getLong(getColumnIndexOrThrow(HABIT_DDL_ID))
                val name = getString(getColumnIndexOrThrow(HABIT_NAME))
                val desc = getString(getColumnIndexOrThrow(HABIT_DESC))
                val colorIdx = getColumnIndexOrThrow(HABIT_COLOR)
                val color = if (isNull(colorIdx)) null else getInt(colorIdx)
                val iconKey = getString(getColumnIndexOrThrow(HABIT_ICON_KEY))
                val period = com.aritxonly.deadliner.model.HabitPeriod.valueOf(
                    getString(getColumnIndexOrThrow(HABIT_PERIOD))
                )
                val timesPerPeriod = getInt(getColumnIndexOrThrow(HABIT_TIMES_PER_PERIOD))
                val goalType = com.aritxonly.deadliner.model.HabitGoalType.valueOf(
                    getString(getColumnIndexOrThrow(HABIT_GOAL_TYPE))
                )
                val totalTargetIdx = getColumnIndexOrThrow(HABIT_TOTAL_TARGET)
                val totalTarget = if (isNull(totalTargetIdx)) null else getInt(totalTargetIdx)
                val createdAt = java.time.LocalDateTime.parse(
                    getString(getColumnIndexOrThrow(HABIT_CREATED_AT))
                )
                val updatedAt = java.time.LocalDateTime.parse(
                    getString(getColumnIndexOrThrow(HABIT_UPDATED_AT))
                )
                val status = com.aritxonly.deadliner.model.HabitStatus.valueOf(
                    getString(getColumnIndexOrThrow(HABIT_STATUS))
                )
                val sortOrder = getInt(getColumnIndexOrThrow(HABIT_SORT_ORDER))

                result.add(
                    com.aritxonly.deadliner.model.Habit(
                        id = id,
                        ddlId = ddlId,
                        name = name,
                        description = desc,
                        color = color,
                        iconKey = iconKey,
                        period = period,
                        timesPerPeriod = timesPerPeriod,
                        goalType = goalType,
                        totalTarget = totalTarget,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        status = status,
                        sortOrder = sortOrder
                    )
                )
            }
            close()
        }
        return result
    }
    // endregion

    // region Habit记录
    fun insertHabitRecord(record: com.aritxonly.deadliner.model.HabitRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(HR_HABIT_ID, record.habitId)
            put(HR_DATE, record.date.toString())           // LocalDate → "yyyy-MM-dd"
            put(HR_COUNT, record.count)
            put(HR_STATUS, record.status.name)
            put(HR_CREATED_AT, record.createdAt.toString())
        }
        return db.insert(TABLE_HABIT_RECORD, null, values)
    }

    fun getHabitRecordsForHabitOnDate(habitId: Long, date: java.time.LocalDate): List<com.aritxonly.deadliner.model.HabitRecord> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_RECORD,
            null,
            "$HR_HABIT_ID = ? AND $HR_DATE = ?",
            arrayOf(habitId.toString(), date.toString()),
            null,
            null,
            null
        )
        return parseHabitRecordCursor(cursor)
    }

    fun getHabitRecordsForDate(date: java.time.LocalDate): List<com.aritxonly.deadliner.model.HabitRecord> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_RECORD,
            null,
            "$HR_DATE = ?",
            arrayOf(date.toString()),
            null,
            null,
            null
        )
        return parseHabitRecordCursor(cursor)
    }

    fun getHabitRecordsForHabitInRange(
        habitId: Long,
        startDate: java.time.LocalDate,
        endDateInclusive: java.time.LocalDate
    ): List<com.aritxonly.deadliner.model.HabitRecord> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_RECORD,
            null,
            "$HR_HABIT_ID = ? AND $HR_DATE BETWEEN ? AND ?",
            arrayOf(habitId.toString(), startDate.toString(), endDateInclusive.toString()),
            null,
            null,
            "$HR_DATE ASC"
        )
        return parseHabitRecordCursor(cursor)
    }

    fun deleteHabitRecordsForHabitOnDate(habitId: Long, date: java.time.LocalDate) {
        val db = writableDatabase
        db.delete(
            TABLE_HABIT_RECORD,
            "$HR_HABIT_ID = ? AND $HR_DATE = ?",
            arrayOf(habitId.toString(), date.toString())
        )
    }

    /**
     * 内部帮助函数：把 cursor 解析成 HabitRecord 列表
     */
    private fun parseHabitRecordCursor(cursor: Cursor): List<com.aritxonly.deadliner.model.HabitRecord> {
        val result = mutableListOf<com.aritxonly.deadliner.model.HabitRecord>()
        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(HR_ID))
                val habitId = getLong(getColumnIndexOrThrow(HR_HABIT_ID))
                val date = java.time.LocalDate.parse(getString(getColumnIndexOrThrow(HR_DATE)))
                val count = getInt(getColumnIndexOrThrow(HR_COUNT))
                val status = com.aritxonly.deadliner.model.HabitRecordStatus.valueOf(
                    getString(getColumnIndexOrThrow(HR_STATUS))
                )
                val createdAt = java.time.LocalDateTime.parse(
                    getString(getColumnIndexOrThrow(HR_CREATED_AT))
                )

                result.add(
                    com.aritxonly.deadliner.model.HabitRecord(
                        id = id,
                        habitId = habitId,
                        date = date,
                        count = count,
                        status = status,
                        createdAt = createdAt
                    )
                )
            }
            close()
        }
        return result
    }
    // endregion

    // region Habit旧版本迁移
    private fun migrateLegacyHabits(db: SQLiteDatabase) {
        Log.d("DatabaseHelper", "migrateLegacyHabits: start")

        // 只处理 type = 'habit' 且未删除的
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_NOTE, COLUMN_TIMESTAMP, COLUMN_TYPE),
            "$COLUMN_DELETED = 0 AND $COLUMN_TYPE = ?",
            arrayOf(DeadlineType.HABIT.toString()),    // toString() 返回小写 "habit"
            null,
            null,
            null
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val ddlId = c.getLong(c.getColumnIndexOrThrow(COLUMN_ID))
                val name = c.getString(c.getColumnIndexOrThrow(COLUMN_NAME))
                val note = c.getString(c.getColumnIndexOrThrow(COLUMN_NOTE)) ?: ""
                val ts = c.getString(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""

                // 如果该 ddl 已经有 Habit 记录，跳过（避免重复）
                db.rawQuery(
                    "SELECT $HABIT_ID FROM $TABLE_HABIT WHERE $HABIT_DDL_ID = ? LIMIT 1",
                    arrayOf(ddlId.toString())
                ).use { hc ->
                    if (hc.moveToFirst()) {
                        return
                    }
                }

                // note 为空或明显不是 JSON，跳过
                if (note.isBlank() || !note.trimStart().startsWith("{")) {
                    continue
                }

                try {
                    // 尝试按旧 HabitMetaData JSON 解析
                    val habitId = migrateOneHabitFromNote(db, ddlId, name, note, ts)
                    if (habitId != null) {
                        Log.d("DatabaseHelper", "migrateLegacyHabits: migrated ddlId=$ddlId -> habitId=$habitId")
                    }
                } catch (e: Exception) {
                    Log.e("DatabaseHelper", "migrateLegacyHabits: failed for ddlId=$ddlId", e)
                    // 出错就跳过这条，避免整个升级挂掉
                }
            }
        }

        Log.d("DatabaseHelper", "migrateLegacyHabits: done")
    }

    private fun migrateOneHabitFromNote(
        db: SQLiteDatabase,
        ddlId: Long,
        ddlName: String,
        note: String,
        timestamp: String
    ): Long? {
        // 用 org.json 解析，避免依赖 Gson/Moshi
        val obj = org.json.JSONObject(note)

        // 频率类型（枚举序列化后一般就是 "DAILY"/"WEEKLY"/"MONTHLY"/"TOTAL"）
        val freqTypeStr = obj.optString("frequencyType", "DAILY").uppercase()
        val frequency = obj.optInt("frequency", 1)
        val total = obj.optInt("total", 0)

        // 映射到字符串形式（直接写入 DB，不用依赖 HabitPeriod/HabitGoalType 枚举）
        val habitPeriod: String = when (freqTypeStr) {
            "DAILY" -> "DAILY"
            "WEEKLY" -> "WEEKLY"
            "MONTHLY" -> "MONTHLY"
            "TOTAL" -> "DAILY"       // 没有 TOTAL 周期，用 DAILY 兜底
            else -> "DAILY"
        }

        val goalType: String
        val totalTarget: Int?

        if (freqTypeStr == "TOTAL") {
            goalType = "TOTAL"
            totalTarget = if (total > 0) total else null
        } else {
            goalType = "PER_PERIOD"
            totalTarget = null
        }

        val nowStr = java.time.LocalDateTime.now().toString()
        val createdAt = if (timestamp.isNotBlank()) timestamp else nowStr
        val updatedAt = nowStr

        // 1) 插入 habits
        val habitValues = ContentValues().apply {
            put(HABIT_DDL_ID, ddlId)
            put(HABIT_NAME, ddlName)
            put(HABIT_DESC, "")                    // 先不从 note 拆备注，避免误伤
            putNull(HABIT_COLOR)
            put(HABIT_ICON_KEY, null as String?)
            put(HABIT_PERIOD, habitPeriod)
            put(HABIT_TIMES_PER_PERIOD, frequency)
            put(HABIT_GOAL_TYPE, goalType)
            if (totalTarget != null) put(HABIT_TOTAL_TARGET, totalTarget) else putNull(HABIT_TOTAL_TARGET)
            put(HABIT_CREATED_AT, createdAt)
            put(HABIT_UPDATED_AT, updatedAt)
            put(HABIT_STATUS, "ACTIVE")
            put(HABIT_SORT_ORDER, 0)
        }

        val habitId = db.insert(TABLE_HABIT, null, habitValues)
        if (habitId <= 0L) {
            // 插入失败就不继续
            return null
        }

        // 2) 展开 completedDates → habit_records
        val completed = mutableListOf<String>()
        val datesArray = obj.optJSONArray("completedDates")
        if (datesArray != null) {
            for (i in 0 until datesArray.length()) {
                val d = datesArray.optString(i, null)
                if (!d.isNullOrBlank()) {
                    completed.add(d)
                }
            }
        }

        fun parseToLocalDateString(raw: String): String? {
            return try {
                java.time.LocalDate.parse(raw).toString()
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(raw).toLocalDate().toString()
                } catch (_: Exception) {
                    null
                }
            }
        }

        for (raw in completed) {
            val localDateStr = parseToLocalDateString(raw) ?: continue

            val recordValues = ContentValues().apply {
                put(HR_HABIT_ID, habitId)
                put(HR_DATE, localDateStr)               // "yyyy-MM-dd"
                put(HR_COUNT, 1)
                put(HR_STATUS, "COMPLETED")
                put(HR_CREATED_AT, nowStr)
            }

            db.insert(TABLE_HABIT_RECORD, null, recordValues)
        }

        return habitId
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