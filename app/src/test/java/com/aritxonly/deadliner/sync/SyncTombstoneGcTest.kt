package com.aritxonly.deadliner.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.Ver
import com.aritxonly.deadliner.web.WebUtils
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SyncTombstoneGcTest {
    private lateinit var context: Context
    private lateinit var db: DatabaseHelper
    private lateinit var sync: SyncService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        db = DatabaseHelper.getInstance(context)
        GlobalUtils.init(context)
        sync = SyncService(db, WebUtils("http://127.0.0.1"))
    }

    @After
    fun tearDown() {
        GlobalUtils.tombstoneRetentionDays = 30
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
    }

    @Test
    fun buildSnapshotKeepsUnexpiredTombstone() {
        GlobalUtils.tombstoneRetentionDays = 30
        val ddlId = createDeletedDdl(Instant.now().minus(10, ChronoUnit.DAYS).toString())
        val uid = db.getDdlUidById(ddlId)!!

        val snapshot = sync.buildLocalDdlSnapshotV2()

        assertTrue(snapshotUids(snapshot).contains(uid))
    }

    @Test
    fun buildSnapshotDropsExpiredTombstone() {
        GlobalUtils.tombstoneRetentionDays = 30
        val ddlId = createDeletedDdl(Instant.now().minus(60, ChronoUnit.DAYS).toString())
        val uid = db.getDdlUidById(ddlId)!!

        val snapshot = sync.buildLocalDdlSnapshotV2()

        assertFalse(snapshotUids(snapshot).contains(uid))
    }

    @Test
    fun mergeDropsExpiredTombstoneFromEitherSide() {
        GlobalUtils.tombstoneRetentionDays = 30
        val local = snapshotOf(
            tombstoneItem("LOCAL:1", Ver(Instant.now().minus(80, ChronoUnit.DAYS).toString(), 0, "LOCAL"))
        )
        val remote = snapshotOf(
            tombstoneItem("REMOTE:1", Ver(Instant.now().minus(70, ChronoUnit.DAYS).toString(), 0, "REMOTE"))
        )

        val merged = sync.mergeSnapshots(local, remote)

        assertEquals(emptyList<String>(), snapshotUids(merged))
    }

    @Test
    fun syncCleanupPhysicallyDeletesExpiredTombstone() {
        GlobalUtils.tombstoneRetentionDays = 30
        val ddlId = createDeletedDdl(Instant.now().minus(60, ChronoUnit.DAYS).toString())
        val uid = db.getDdlUidById(ddlId)!!

        val deletedCount = sync.cleanupExpiredDeletedRows()

        assertEquals(1, deletedCount)
        assertNull(db.getDdlIdByUid(uid))
    }

    @Test
    fun retentionZeroDisablesFilteringAndCleanup() {
        GlobalUtils.tombstoneRetentionDays = 0
        val ddlId = createDeletedDdl(Instant.now().minus(60, ChronoUnit.DAYS).toString())
        val uid = db.getDdlUidById(ddlId)!!

        val snapshot = sync.buildLocalDdlSnapshotV2()
        val merged = sync.mergeSnapshots(
            snapshotOf(tombstoneItem(uid, Ver(Instant.now().minus(60, ChronoUnit.DAYS).toString(), 0, "A"))),
            snapshotOf()
        )
        val deletedCount = sync.cleanupExpiredDeletedRows()

        assertTrue(snapshotUids(snapshot).contains(uid))
        assertTrue(snapshotUids(merged).contains(uid))
        assertEquals(0, deletedCount)
        assertNotNull(db.getDdlIdByUid(uid))
    }

    @Test
    fun invalidVerTsKeepsTombstone() {
        GlobalUtils.tombstoneRetentionDays = 30
        val ddlId = createDeletedDdl("bad-ts")
        val uid = db.getDdlUidById(ddlId)!!

        val snapshot = sync.buildLocalDdlSnapshotV2()
        val deletedCount = sync.cleanupExpiredDeletedRows()

        assertTrue(snapshotUids(snapshot).contains(uid))
        assertEquals(0, deletedCount)
        assertNotNull(db.getDdlIdByUid(uid))
    }

    private fun createDeletedDdl(verTs: String): Long {
        val ddlId = db.insertDDL(
            name = "to delete",
            startTime = "2026-03-31T08:00:00",
            endTime = "2026-03-31T18:00:00",
            type = DeadlineType.TASK
        )
        db.deleteDDL(ddlId)
        db.setDdlVersionById(ddlId, Ver(verTs, 0, "TEST"))
        return ddlId
    }

    private fun snapshotUids(snapshot: JsonObject): List<String> {
        return snapshot.getAsJsonArray("items").map { it.asJsonObject["uid"].asString }
    }

    private fun snapshotOf(vararg items: JsonObject): JsonObject {
        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts", Instant.now().toString())
                addProperty("dev", "TEST")
            })
            add("items", JsonArray().apply {
                items.forEach { add(it) }
            })
        }
    }

    private fun tombstoneItem(uid: String, ver: Ver): JsonObject {
        return JsonObject().apply {
            addProperty("uid", uid)
            add("ver", JsonObject().apply {
                addProperty("ts", ver.ts)
                addProperty("ctr", ver.ctr)
                addProperty("dev", ver.dev)
            })
            addProperty("deleted", true)
            add("doc", JsonNull.INSTANCE)
        }
    }
}
