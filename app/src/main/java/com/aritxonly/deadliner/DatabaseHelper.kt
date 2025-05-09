package com.aritxonly.deadliner

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


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
        private const val DATABASE_VERSION = 6
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
                $COLUMN_HABIT_COUNT INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
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
    }

    // 插入 DDL 数据
    fun insertDDL(
        name: String,
        startTime: String,
        endTime: String,
        note: String = "",
        type: DeadlineType = DeadlineType.TASK
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
                        type = DeadlineType.fromString(getString(getColumnIndexOrThrow(COLUMN_TYPE))),
                        habitCount = getInt(getColumnIndexOrThrow(COLUMN_HABIT_COUNT))
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
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(item.id.toString()))
    }

    fun deleteDDL(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
}

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this != 0