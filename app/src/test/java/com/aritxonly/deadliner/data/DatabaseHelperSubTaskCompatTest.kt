package com.aritxonly.deadliner.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.model.DeadlineType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DatabaseHelperSubTaskCompatTest {
    private lateinit var context: Context
    private lateinit var db: DatabaseHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        db = DatabaseHelper.getInstance(context)
    }

    @After
    fun tearDown() {
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
    }

    @Test
    fun getDDLsByType_parsesLegacyNumericSubTaskIsCompleted() {
        val id = db.insertDDL(
            name = "compat-test",
            startTime = "2026-04-19T08:00:00",
            endTime = "2026-04-19T20:00:00",
            type = DeadlineType.TASK
        )
        val legacyJson = """
            [
              {"id":"s1","content":"a","is_completed":1,"sort_order":0},
              {"id":"s2","content":"b","is_completed":0,"sort_order":1}
            ]
        """.trimIndent()
        db.writableDatabase.execSQL(
            "UPDATE ddl_items SET sub_tasks_json = ? WHERE id = ?",
            arrayOf(legacyJson, id)
        )

        val task = db.getDDLsByType(DeadlineType.TASK).first { it.id == id }
        assertEquals(2, task.subTasks.size)
        assertEquals(1, task.subTasks[0].isCompleted)
        assertEquals(0, task.subTasks[1].isCompleted)
    }
}
