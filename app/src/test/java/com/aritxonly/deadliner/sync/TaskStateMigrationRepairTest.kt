package com.aritxonly.deadliner.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.web.WebUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TaskStateMigrationRepairTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        GlobalUtils.init(context)
    }

    @After
    fun tearDown() {
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
    }

    @Test
    fun reopeningRepairsLegacyArchivedRowStuckAtActive() {
        val db = DatabaseHelper.getInstance(context)
        val ddlId = db.insertDDL(
            name = "legacy archived",
            startTime = "2026-03-01T08:00:00",
            endTime = "2026-03-01T18:00:00",
            type = DeadlineType.TASK
        )
        db.writableDatabase.execSQL(
            """
            UPDATE ddl_items
               SET state='active',
                   is_completed=1,
                   is_archived=1,
                   complete_time='2026-03-02T09:00:00'
             WHERE id=?
            """.trimIndent(),
            arrayOf(ddlId.toString())
        )

        DatabaseHelper.closeInstance()

        val repairedDb = DatabaseHelper.getInstance(context)
        val repaired = repairedDb.getDDLById(ddlId)!!
        val rawState = repairedDb.readableDatabase.rawQuery(
            "SELECT state FROM ddl_items WHERE id=?",
            arrayOf(ddlId.toString())
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

        assertEquals(DDLState.ARCHIVED, repaired.state)
        assertEquals("archived", rawState)
    }

    @Test
    fun snapshotBuildDoesNotResurfaceLegacyCompletedRowStuckAtActive() {
        val db = DatabaseHelper.getInstance(context)
        val ddlId = db.insertDDL(
            name = "legacy completed",
            startTime = "2026-03-01T08:00:00",
            endTime = "2026-03-01T18:00:00",
            type = DeadlineType.TASK
        )
        db.writableDatabase.execSQL(
            """
            UPDATE ddl_items
               SET state='active',
                   is_completed=1,
                   is_archived=0,
                   complete_time='2026-03-02T09:00:00'
             WHERE id=?
            """.trimIndent(),
            arrayOf(ddlId.toString())
        )

        val sync = SyncService(db, WebUtils("http://127.0.0.1"))
        val uid = db.getDdlUidById(ddlId)!!
        val item = sync.buildLocalDdlSnapshotV2()
            .getAsJsonArray("items")
            .map { it.asJsonObject }
            .first { it["uid"].asString == uid }

        assertEquals("completed", item.getAsJsonObject("doc")["state"].asString)
    }
}
