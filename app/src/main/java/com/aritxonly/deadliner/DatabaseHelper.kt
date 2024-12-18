package com.aritxonly.deadliner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper



class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "deadliner.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "ddl_items"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_START_TIME = "start_time"
        private const val COLUMN_END_TIME = "end_time"
        private const val COLUMN_IS_COMPLETED = "is_completed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_START_TIME TEXT NOT NULL,
                $COLUMN_END_TIME TEXT NOT NULL,
                $COLUMN_IS_COMPLETED INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 插入 DDL 数据
    fun insertDDL(name: String, startTime: String, endTime: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_START_TIME, startTime)
            put(COLUMN_END_TIME, endTime)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // 获取所有 DDL 数据
    fun getAllDDLs(): List<DDLItem> {
        val db = readableDatabase
        val ddlList = mutableListOf<DDLItem>()
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, null)

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(COLUMN_ID))
                val name = getString(getColumnIndexOrThrow(COLUMN_NAME))
                val startTime = getString(getColumnIndexOrThrow(COLUMN_START_TIME))
                val endTime = getString(getColumnIndexOrThrow(COLUMN_END_TIME))
                val isCompleted = getInt(getColumnIndexOrThrow(COLUMN_IS_COMPLETED))
                ddlList.add(DDLItem(id, name, startTime, endTime, isCompleted.toBoolean()))
            }
            close()
        }
        return ddlList
    }

    fun updateDDL(item: DDLItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, item.name)
            put(COLUMN_START_TIME, item.startTime)
            put(COLUMN_END_TIME, item.endTime)
            put(COLUMN_IS_COMPLETED, item.isCompleted.toInt()) // 假设您在数据库中添加了一个isCompleted列
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