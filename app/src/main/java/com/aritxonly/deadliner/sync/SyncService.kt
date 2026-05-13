package com.aritxonly.deadliner.sync

import android.content.ContentValues
import android.util.Log
import com.aritxonly.deadliner.data.DeletedDdlRow
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.data.HabitCarrierSyncRow
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecord
import com.aritxonly.deadliner.model.HabitRecordStatus
import com.aritxonly.deadliner.model.HabitStatus
import com.aritxonly.deadliner.model.Ver
import com.aritxonly.deadliner.web.WebUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class SyncService(
    private val db: DatabaseHelper,
    private val web: WebUtils
) {
    private val gson = Gson()

    fun onLocalInserted(newLocalId: Long) { /* no-op */ }
    fun onLocalUpdated(localId: Long) { /* no-op */ }
    fun onLocalDeleting(localId: Long) { /* no-op */ }

    suspend fun syncOnce(): Boolean {
        Log.d("WebDAV", "snapshot syncOnce")
        return try {
            syncAllSnapshotsOnce()
        } catch (e: Exception) {
            Log.e("Sync", "Failed", e)
            false
        }
    }

    private fun ddlSnapshotV2Path() = "Deadliner/snapshot-v2.json"
    private fun ddlSnapshotV1Path() = "Deadliner/snapshot-v1.json"
    private fun habitSnapshotV2Path() = "Deadliner/habit-snapshot-v2.json"

    private data class RemoteSnapshot(
        val exists: Boolean,
        val etag: String?,
        val snapshot: JsonObject
    )

    private fun emptySnapshot(): JsonObject {
        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts", "1970-01-01T00:00:00Z")
                addProperty("dev", "unknown")
            })
            add("items", JsonArray())
        }
    }

    private fun snapshotVersionObject(): JsonObject {
        return JsonObject().apply {
            addProperty("ts", Instant.now().toString())
            addProperty("dev", db.getDeviceId())
        }
    }

    internal fun buildLocalDdlSnapshotV2(): JsonObject {
        val items = JsonArray()
        val sql = """
            SELECT
                uid,
                COALESCE(deleted,0) AS deleted,
                COALESCE(ver_ts,'1970-01-01T00:00:00Z') AS ver_ts,
                COALESCE(ver_ctr,0) AS ver_ctr,
                COALESCE(ver_dev,'') AS ver_dev,
                COALESCE(is_completed,0) AS is_completed,
                COALESCE(is_archived,0) AS is_archived,
                id,
                name,
                start_time,
                end_time,
                state,
                complete_time,
                note,
                is_stared,
                type,
                habit_count,
                habit_total_count,
                calendar_event,
                timestamp,
                sub_tasks_json
            FROM ddl_items
        """.trimIndent()

        db.readableDatabase.rawQuery(sql, null).use { c ->
            val idxUid = c.getColumnIndexOrThrow("uid")
            val idxDeleted = c.getColumnIndexOrThrow("deleted")
            val idxVerTs = c.getColumnIndexOrThrow("ver_ts")
            val idxVerCtr = c.getColumnIndexOrThrow("ver_ctr")
            val idxVerDev = c.getColumnIndexOrThrow("ver_dev")
            val idxIsCompleted = c.getColumnIndexOrThrow("is_completed")
            val idxIsArchived = c.getColumnIndexOrThrow("is_archived")
            val idxId = c.getColumnIndexOrThrow("id")
            val idxName = c.getColumnIndexOrThrow("name")
            val idxStart = c.getColumnIndexOrThrow("start_time")
            val idxEnd = c.getColumnIndexOrThrow("end_time")
            val idxState = c.getColumnIndexOrThrow("state")
            val idxCompleteTime = c.getColumnIndexOrThrow("complete_time")
            val idxNote = c.getColumnIndexOrThrow("note")
            val idxStar = c.getColumnIndexOrThrow("is_stared")
            val idxType = c.getColumnIndexOrThrow("type")
            val idxHabitCount = c.getColumnIndexOrThrow("habit_count")
            val idxHabitTotalCount = c.getColumnIndexOrThrow("habit_total_count")
            val idxCalendarEvent = c.getColumnIndexOrThrow("calendar_event")
            val idxTimestamp = c.getColumnIndexOrThrow("timestamp")
            val idxSubTasksJson = c.getColumnIndexOrThrow("sub_tasks_json")

            while (c.moveToNext()) {
                val uid = c.getString(idxUid) ?: continue
                val deleted = c.getInt(idxDeleted) != 0
                val item = JsonObject().apply {
                    addProperty("uid", uid)
                    add("ver", versionObject(
                        c.getString(idxVerTs),
                        c.getInt(idxVerCtr),
                        c.getString(idxVerDev)
                    ))
                    addProperty("deleted", deleted)
                }
                if (!deleted) {
                    val state = DDLState.fromStoredValue(
                        rawState = c.getString(idxState),
                        isCompleted = c.getInt(idxIsCompleted) != 0,
                        isArchived = c.getInt(idxIsArchived) != 0
                    )
                    item.add("doc", JsonObject().apply {
                        addProperty("id", c.getLong(idxId))
                        addProperty("name", c.getString(idxName))
                        addProperty("start_time", c.getString(idxStart))
                        addProperty("end_time", c.getString(idxEnd))
                        addProperty("state", state.toWire())
                        addProperty("complete_time", c.getString(idxCompleteTime))
                        addProperty("note", c.getString(idxNote))
                        addProperty("is_stared", c.getInt(idxStar))
                        addProperty("type", c.getString(idxType))
                        addProperty("habit_count", c.getInt(idxHabitCount))
                        addProperty("habit_total_count", c.getInt(idxHabitTotalCount))
                        addProperty("calendar_event", c.getLong(idxCalendarEvent))
                        addProperty("timestamp", c.getString(idxTimestamp))
                        add("sub_tasks", normalizeSubTasksArray(c.getString(idxSubTasksJson)))
                    })
                } else {
                    item.add("doc", JsonNull.INSTANCE)
                }
                items.add(item)
            }
        }

        return pruneExpiredTombstones(
            JsonObject().apply {
                add("version", snapshotVersionObject())
                add("items", items)
            },
            source = "ddl-local-build",
            logTag = "Sync"
        )
    }

    internal fun buildLocalHabitSnapshotV2(): JsonObject {
        val items = JsonArray()
        val carriers = db.getHabitCarrierSyncRows()
        val localDeviceId = db.getDeviceId()
        for (carrier in carriers) {
            val habit = db.getHabitByDdlId(carrier.ddlId)
            val appliedVer = db.getHabitAppliedVersionByDdlId(carrier.ddlId)
            when (
                HabitSyncRules.localBuildDecision(
                    carrierDeleted = carrier.deleted,
                    habitExists = habit != null,
                    carrierVer = carrier.ver,
                    carrierVerDev = carrier.ver.dev,
                    localDeviceId = localDeviceId,
                    appliedVer = appliedVer,
                    habitUpdatedAt = habit?.updatedAt
                )
            ) {
                LocalHabitBuildDecision.SKIP_MISSING_REMOTE_HABIT -> {
                    Log.d("HabitSync", "Skip local habit uid=${carrier.uid}: carrier exists but payload missing and carrier is remote-owned")
                    continue
                }
                LocalHabitBuildDecision.SKIP_STALE_ACTIVE -> {
                    Log.d(
                        "HabitSync",
                        "Skip local habit uid=${carrier.uid}: carrier=${carrier.ver.ts}/${carrier.ver.ctr}/${carrier.ver.dev} applied=${appliedVer?.ts}/${appliedVer?.ctr}/${appliedVer?.dev} updatedAt=${habit?.updatedAt}"
                    )
                    continue
                }
                LocalHabitBuildDecision.EMIT_TOMBSTONE -> {
                    Log.d("HabitSync", "Emit local habit tombstone uid=${carrier.uid}")
                    items.add(JsonObject().apply {
                        addProperty("uid", carrier.uid)
                        add("ver", versionObject(carrier.ver.ts, carrier.ver.ctr, carrier.ver.dev))
                        addProperty("deleted", true)
                        add("doc", JsonNull.INSTANCE)
                    })
                }
                LocalHabitBuildDecision.EMIT_ACTIVE -> {
                    Log.d("HabitSync", "Emit local habit doc uid=${carrier.uid}")
                    val resolvedHabit = habit
                        ?: throw IllegalStateException("Habit missing for active carrier=${carrier.ddlId}")
                    val habitId = db.getHabitIdByDdlId(carrier.ddlId)
                        ?: throw IllegalStateException("Habit id missing for carrier=${carrier.ddlId}")
                    val records = db.getAllHabitRecordsForHabit(habitId)
                    items.add(JsonObject().apply {
                        addProperty("uid", carrier.uid)
                        add("ver", versionObject(carrier.ver.ts, carrier.ver.ctr, carrier.ver.dev))
                        addProperty("deleted", false)
                        add("doc", JsonObject().apply {
                            addProperty("ddl_uid", carrier.uid)
                            add("habit", habitToJson(resolvedHabit))
                            add("records", recordsToJson(records))
                        })
                    })
                }
            }
        }

        return pruneExpiredTombstones(
            JsonObject().apply {
                add("version", snapshotVersionObject())
                add("items", items)
            },
            source = "habit-local-build",
            logTag = "HabitSync"
        )
    }

    private fun habitToJson(habit: Habit): JsonObject {
        validateHabit(habit)
        return JsonObject().apply {
            addProperty("name", habit.name)
            if (habit.description != null) addProperty("description", habit.description) else add("description", JsonNull.INSTANCE)
            if (habit.color != null) addProperty("color", habit.color) else add("color", JsonNull.INSTANCE)
            if (habit.iconKey != null) addProperty("icon_key", habit.iconKey) else add("icon_key", JsonNull.INSTANCE)
            addProperty("period", habit.period.name)
            addProperty("times_per_period", habit.timesPerPeriod)
            addProperty("goal_type", habit.goalType.name)
            if (habit.totalTarget != null) addProperty("total_target", habit.totalTarget) else add("total_target", JsonNull.INSTANCE)
            addProperty("created_at", habit.createdAt.toString())
            addProperty("updated_at", habit.updatedAt.toString())
            addProperty("status", habit.status.name)
            addProperty("sort_order", habit.sortOrder)
            if (habit.alarmTime != null) addProperty("alarm_time", habit.alarmTime) else add("alarm_time", JsonNull.INSTANCE)
        }
    }

    private fun recordsToJson(records: List<HabitRecord>): JsonArray {
        val arr = JsonArray()
        records.sortedWith(compareBy<HabitRecord> { it.date }.thenBy { it.createdAt }).forEach { record ->
            validateHabitRecord(record)
            arr.add(JsonObject().apply {
                addProperty("date", record.date.toString())
                addProperty("count", record.count)
                addProperty("status", record.status.name)
                addProperty("created_at", record.createdAt.toString())
            })
        }
        return arr
    }

    private fun normalizeSubTasksArray(raw: String?): JsonArray {
        if (raw.isNullOrBlank()) return JsonArray()
        val parsed = runCatching {
            JsonParser.parseString(raw).asJsonArray
        }.getOrElse {
            throw IllegalArgumentException("Invalid sub_tasks_json", it)
        }
        val normalized = JsonArray()
        parsed.forEach { element ->
            val item = element.asJsonObject
            normalized.add(JsonObject().apply {
                addProperty("id", item["id"]?.asString ?: "")
                addProperty("content", item["content"]?.asString ?: "")
                addProperty("is_completed", readBooleanishInt(item["is_completed"] ?: item["isCompleted"]))
                addProperty("sort_order", item["sort_order"]?.asInt ?: item["sortOrder"]?.asInt ?: 0)
                if (item.has("created_at")) add("created_at", item["created_at"]) else if (item.has("createdAt")) add("created_at", item["createdAt"]) else add("created_at", JsonNull.INSTANCE)
                if (item.has("updated_at")) add("updated_at", item["updated_at"]) else if (item.has("updatedAt")) add("updated_at", item["updatedAt"]) else add("updated_at", JsonNull.INSTANCE)
            })
        }
        return normalized
    }

    private fun readBooleanishInt(element: JsonElement?): Int {
        return when {
            element == null || element.isJsonNull -> 0
            element.asJsonPrimitive.isBoolean -> if (element.asBoolean) 1 else 0
            else -> element.asInt
        }
    }

    private fun readBooleanish(element: JsonElement?): Boolean {
        if (element == null || element.isJsonNull) return false
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asInt != 0
            primitive.isString -> {
                val raw = primitive.asString.trim().lowercase()
                raw == "1" || raw == "true" || raw == "yes"
            }
            else -> false
        }
    }

    private fun JsonObject.objectMemberOrNull(key: String): JsonObject? {
        val element = this[key] ?: return null
        if (element.isJsonNull || !element.isJsonObject) return null
        return element.asJsonObject
    }

    private fun versionObject(ts: String, ctr: Int, dev: String): JsonObject {
        return JsonObject().apply {
            addProperty("ts", ts)
            addProperty("ctr", ctr)
            addProperty("dev", dev)
        }
    }

    private fun newer(a: JsonObject, b: JsonObject): Boolean {
        val av = a.getAsJsonObject("ver")
        val bv = b.getAsJsonObject("ver")
        val ats = av["ts"].asString
        val bts = bv["ts"].asString
        if (ats != bts) return ats > bts
        val actr = av["ctr"]?.asInt ?: 0
        val bctr = bv["ctr"]?.asInt ?: 0
        if (actr != bctr) return actr > bctr
        val adev = av["dev"]?.asString ?: ""
        val bdev = bv["dev"]?.asString ?: ""
        return adev >= bdev
    }

    internal fun mergeSnapshots(local: JsonObject, remote: JsonObject): JsonObject {
        fun toMap(root: JsonObject): MutableMap<String, JsonObject> {
            val map = mutableMapOf<String, JsonObject>()
            root.getAsJsonArray("items")?.forEach { el ->
                val obj = el.asJsonObject
                map[obj["uid"].asString] = obj
            }
            return map
        }

        val localMap = toMap(pruneExpiredTombstones(local, source = "merge-local", logTag = "Sync"))
        val remoteMap = toMap(pruneExpiredTombstones(remote, source = "merge-remote", logTag = "Sync"))
        val keys = (localMap.keys + remoteMap.keys).toSortedSet()
        val items = JsonArray()

        for (uid in keys) {
            val localItem = localMap[uid]
            val remoteItem = remoteMap[uid]
            val chosen = when {
                localItem == null -> remoteItem!!
                remoteItem == null -> localItem
                else -> if (newer(localItem, remoteItem)) localItem else remoteItem
            }
            items.add(chosen.deepCopy())
        }

        return pruneExpiredTombstones(
            JsonObject().apply {
                add("version", snapshotVersionObject())
                add("items", items)
            },
            source = "merge-result",
            logTag = "Sync"
        )
    }

    private fun tombstoneRetentionDays(): Int = GlobalUtils.tombstoneRetentionDays.coerceAtLeast(0)

    private fun tombstoneCutoff(now: Instant = Instant.now()): Instant? {
        val retentionDays = tombstoneRetentionDays()
        if (retentionDays == 0) return null
        return now.minus(retentionDays.toLong(), ChronoUnit.DAYS)
    }

    private fun parseVersionInstantOrNull(ts: String?): Instant? {
        val raw = ts?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrNull()
    }

    internal fun isExpiredDeletedItem(item: JsonObject, now: Instant = Instant.now()): Boolean {
        if (item["deleted"]?.asBoolean != true) return false
        val cutoff = tombstoneCutoff(now) ?: return false
        val ts = item.getAsJsonObject("ver")?.get("ts")?.asString
        val tombstoneInstant = parseVersionInstantOrNull(ts) ?: return false
        return tombstoneInstant.isBefore(cutoff)
    }

    internal fun pruneExpiredTombstones(
        root: JsonObject,
        source: String,
        logTag: String,
        now: Instant = Instant.now()
    ): JsonObject {
        val items = JsonArray()
        root.getAsJsonArray("items")?.forEach { el ->
            val item = el.asJsonObject
            if (isExpiredDeletedItem(item, now)) {
                val uid = item["uid"]?.asString ?: "(unknown)"
                val verTs = item.getAsJsonObject("ver")?.get("ts")?.asString ?: "(missing)"
                Log.d(logTag, "prune expired tombstone uid=$uid ver=$verTs source=$source")
            } else {
                items.add(item.deepCopy())
            }
        }
        return JsonObject().apply {
            add("version", root.getAsJsonObject("version")?.deepCopy() ?: snapshotVersionObject())
            add("items", items)
        }
    }

    internal fun cleanupExpiredDeletedRows(now: Instant = Instant.now()): Int {
        if (tombstoneRetentionDays() == 0) return 0
        val cutoff = tombstoneCutoff(now) ?: return 0
        val expiredIds = db.getDeletedDdlRows()
            .filter { row -> isExpiredDeletedRow(row, cutoff) }
            .map { it.ddlId }
        return db.hardDeleteDdlRows(expiredIds)
    }

    private fun isExpiredDeletedRow(row: DeletedDdlRow, cutoff: Instant): Boolean {
        val tombstoneInstant = parseVersionInstantOrNull(row.ver.ts) ?: return false
        return tombstoneInstant.isBefore(cutoff)
    }

    private fun canonicalizeDdlV2Snapshot(root: JsonObject): JsonObject {
        val items = JsonArray()
        root.getAsJsonArray("items")?.forEach { el ->
            val obj = el.asJsonObject
            val deleted = obj["deleted"]?.asBoolean == true
            val out = JsonObject().apply {
                addProperty("uid", obj["uid"].asString)
                add("ver", obj.getAsJsonObject("ver").deepCopy())
                addProperty("deleted", deleted)
            }
            if (!deleted) {
                val doc = obj.getAsJsonObject("doc")
                    ?: throw IllegalArgumentException("DDL item missing doc for uid=${obj["uid"].asString}")
                val state = DDLState.fromWire(doc["state"]?.asString ?: "")
                out.add("doc", JsonObject().apply {
                    addProperty("id", doc["id"]?.asLong ?: -1L)
                    addProperty("name", doc["name"]?.asString ?: "")
                    addProperty("start_time", doc["start_time"]?.asString ?: "")
                    addProperty("end_time", doc["end_time"]?.asString ?: "")
                    addProperty("state", state.toWire())
                    addProperty("complete_time", doc["complete_time"]?.asString ?: "")
                    addProperty("note", doc["note"]?.asString ?: "")
                    addProperty("is_stared", doc["is_stared"]?.asInt ?: 0)
                    addProperty("type", doc["type"]?.asString ?: "task")
                    addProperty("habit_count", doc["habit_count"]?.asInt ?: 0)
                    addProperty("habit_total_count", doc["habit_total_count"]?.asInt ?: 0)
                    addProperty("calendar_event", doc["calendar_event"]?.asLong ?: -1L)
                    addProperty("timestamp", doc["timestamp"]?.asString ?: "")
                    add("sub_tasks", normalizeRemoteSubTasks(doc.getAsJsonArray("sub_tasks")))
                })
            } else {
                out.add("doc", JsonNull.INSTANCE)
            }
            items.add(out)
        }
        return JsonObject().apply {
            add("version", root.getAsJsonObject("version")?.deepCopy() ?: emptySnapshot().getAsJsonObject("version"))
            add("items", items)
        }
    }

    private fun normalizeRemoteSubTasks(arr: JsonArray?): JsonArray {
        val normalized = JsonArray()
        arr?.forEach { el ->
            val item = el.asJsonObject
            normalized.add(JsonObject().apply {
                addProperty("id", item["id"]?.asString ?: "")
                addProperty("content", item["content"]?.asString ?: "")
                addProperty("is_completed", readBooleanishInt(item["is_completed"]))
                addProperty("sort_order", item["sort_order"]?.asInt ?: 0)
                if (item.has("created_at")) add("created_at", item["created_at"]) else add("created_at", JsonNull.INSTANCE)
                if (item.has("updated_at")) add("updated_at", item["updated_at"]) else add("updated_at", JsonNull.INSTANCE)
            })
        }
        return normalized
    }

    private fun upgradeV1ToDdlV2(root: JsonObject): JsonObject {
        val items = JsonArray()
        root.getAsJsonArray("items")?.forEach { el ->
            val obj = el.asJsonObject
            val deleted = obj["deleted"]?.asBoolean == true
            val out = JsonObject().apply {
                addProperty("uid", obj["uid"].asString)
                add("ver", obj.getAsJsonObject("ver").deepCopy())
                addProperty("deleted", deleted)
            }
            if (!deleted) {
                val doc = obj.getAsJsonObject("doc")
                    ?: throw IllegalArgumentException("V1 DDL item missing doc for uid=${obj["uid"].asString}")
                val state = when {
                    doc["is_archived"]?.asInt == 1 -> DDLState.ARCHIVED
                    doc["is_completed"]?.asInt == 1 -> DDLState.COMPLETED
                    else -> DDLState.ACTIVE
                }
                out.add("doc", JsonObject().apply {
                    addProperty("id", doc["id"]?.asLong ?: -1L)
                    addProperty("name", doc["name"]?.asString ?: "")
                    addProperty("start_time", doc["start_time"]?.asString ?: "")
                    addProperty("end_time", doc["end_time"]?.asString ?: "")
                    addProperty("state", state.toWire())
                    addProperty("complete_time", doc["complete_time"]?.asString ?: "")
                    addProperty("note", doc["note"]?.asString ?: "")
                    addProperty("is_stared", doc["is_stared"]?.asInt ?: 0)
                    addProperty("type", doc["type"]?.asString ?: "task")
                    addProperty("habit_count", doc["habit_count"]?.asInt ?: 0)
                    addProperty("habit_total_count", doc["habit_total_count"]?.asInt ?: 0)
                    addProperty("calendar_event", doc["calendar_event"]?.asLong ?: -1L)
                    addProperty("timestamp", doc["timestamp"]?.asString ?: "")
                    add("sub_tasks", JsonArray())
                })
            } else {
                out.add("doc", JsonNull.INSTANCE)
            }
            items.add(out)
        }
        return JsonObject().apply {
            add("version", root.getAsJsonObject("version")?.deepCopy() ?: emptySnapshot().getAsJsonObject("version"))
            add("items", items)
        }
    }

    private fun canonicalizeHabitV2Snapshot(root: JsonObject): JsonObject {
        val items = JsonArray()
        root.getAsJsonArray("items")?.forEach { el ->
            val obj = el.asJsonObject
            val uid = obj["uid"].asString
            val doc = obj.objectMemberOrNull("doc")
            val deleted = obj["deleted"]?.asBoolean == true || isHabitDocDeleted(doc)
            val out = JsonObject().apply {
                addProperty("uid", uid)
                add("ver", obj.getAsJsonObject("ver").deepCopy())
                addProperty("deleted", deleted)
            }
            if (!deleted) {
                val resolvedDoc = doc
                    ?: throw IllegalArgumentException("Habit item missing doc for uid=$uid")
                val ddlUid = resolvedDoc["ddl_uid"]?.asString
                    ?: throw IllegalArgumentException("Habit doc missing ddl_uid for uid=$uid")
                if (ddlUid != uid) {
                    throw IllegalArgumentException("Habit ddl_uid mismatch: $ddlUid != $uid")
                }
                val habit = parseHabitPayload(resolvedDoc.objectMemberOrNull("habit"), -1L, 0L)
                val records = parseHabitRecords(resolvedDoc.getAsJsonArray("records"), 0L)
                out.add("doc", JsonObject().apply {
                    addProperty("ddl_uid", uid)
                    add("habit", habitToJson(habit))
                    add("records", recordsToJson(records))
                })
            } else {
                out.add("doc", JsonNull.INSTANCE)
            }
            items.add(out)
        }
        return JsonObject().apply {
            add("version", root.getAsJsonObject("version")?.deepCopy() ?: emptySnapshot().getAsJsonObject("version"))
            add("items", items)
        }
    }

    private fun isHabitDocDeleted(doc: JsonObject?): Boolean {
        if (doc == null || doc.isJsonNull) return false
        val habit = doc.objectMemberOrNull("habit") ?: return false
        if (readBooleanish(habit["deleted"])) return true
        return false
    }

    private suspend fun loadRemoteSnapshot(
        path: String,
        transform: (JsonObject) -> JsonObject
    ): RemoteSnapshot = withContext(Dispatchers.IO) {
        val (code, etag, _) = web.head(path)
        if (code in listOf(404, 409, 410)) {
            return@withContext RemoteSnapshot(false, null, emptySnapshot())
        }
        if (code in 500..599) {
            throw IllegalStateException("HEAD $path -> $code")
        }
        val (bytes, currentEtag) = web.getBytes(path)
        val parsed = runCatching {
            JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject
        }.getOrElse {
            throw IllegalArgumentException("Decode $path failed", it)
        }
        RemoteSnapshot(true, currentEtag ?: etag, transform(parsed))
    }

    private suspend fun putSnapshot(path: String, snapshot: JsonObject, remote: RemoteSnapshot) {
        web.putBytes(
            path = path,
            bytes = gson.toJson(snapshot).toByteArray(StandardCharsets.UTF_8),
            ifMatch = if (remote.exists) remote.etag else null,
            ifNoneMatchStar = !remote.exists
        )
    }

    private fun projectDdlV2ToV1(root: JsonObject): JsonObject {
        val items = JsonArray()
        root.getAsJsonArray("items")?.forEach { el ->
            val obj = el.asJsonObject
            val deleted = obj["deleted"]?.asBoolean == true
            val out = JsonObject().apply {
                addProperty("uid", obj["uid"].asString)
                add("ver", obj.getAsJsonObject("ver").deepCopy())
                addProperty("deleted", deleted)
            }
            if (!deleted) {
                val doc = obj.objectMemberOrNull("doc")
                    ?: throw IllegalArgumentException("DDL V1 projection missing doc for uid=${obj["uid"].asString}")
                val state = DDLState.fromWire(doc["state"]?.asString ?: "")
                val legacy = when (state) {
                    DDLState.ACTIVE -> 0 to 0
                    DDLState.COMPLETED -> 1 to 0
                    DDLState.ARCHIVED -> 1 to 1
                    DDLState.ABANDONED -> 0 to 0
                    DDLState.ABANDONED_ARCHIVED -> 0 to 1
                }
                out.add("doc", JsonObject().apply {
                    addProperty("id", doc["id"]?.asLong ?: -1L)
                    addProperty("name", doc["name"]?.asString ?: "")
                    addProperty("start_time", doc["start_time"]?.asString ?: "")
                    addProperty("end_time", doc["end_time"]?.asString ?: "")
                    addProperty("is_completed", legacy.first)
                    addProperty("complete_time", doc["complete_time"]?.asString ?: "")
                    addProperty("note", doc["note"]?.asString ?: "")
                    addProperty("is_archived", legacy.second)
                    addProperty("is_stared", doc["is_stared"]?.asInt ?: 0)
                    addProperty("type", doc["type"]?.asString ?: "task")
                    addProperty("habit_count", doc["habit_count"]?.asInt ?: 0)
                    addProperty("habit_total_count", doc["habit_total_count"]?.asInt ?: 0)
                    addProperty("calendar_event", doc["calendar_event"]?.asLong ?: -1L)
                    addProperty("timestamp", doc["timestamp"]?.asString ?: "")
                })
            } else {
                out.add("doc", JsonNull.INSTANCE)
            }
            items.add(out)
        }
        return JsonObject().apply {
            add("version", snapshotVersionObject())
            add("items", items)
        }
    }

    private fun applyDdlSnapshotToLocal(merged: JsonObject) {
        val items = merged.getAsJsonArray("items") ?: return
        val wdb = db.writableDatabase
        wdb.beginTransaction()
        try {
            items.forEach { el ->
                val obj = el.asJsonObject
                val uid = obj["uid"].asString
                val ver = parseVer(obj.getAsJsonObject("ver"))
                val deleted = obj["deleted"]?.asBoolean == true
                val existingId = db.getDdlIdByUid(uid)

                if (deleted) {
                    val values = ContentValues().apply {
                        put("uid", uid)
                        put("deleted", 1)
                        put("name", "(deleted)")
                        put("start_time", "")
                        put("end_time", "")
                        put("is_completed", 1)
                        put("complete_time", "")
                        put("note", "")
                        put("is_archived", 1)
                        put("state", DDLState.ARCHIVED.toWire())
                        put("is_stared", 0)
                        put("type", "task")
                        put("habit_count", 0)
                        put("habit_total_count", 0)
                        put("calendar_event", -1)
                        put("timestamp", ver.ts)
                        put("sub_tasks_json", "[]")
                        put("ver_ts", ver.ts)
                        put("ver_ctr", ver.ctr)
                        put("ver_dev", ver.dev)
                    }
                    if (existingId != null) {
                        wdb.update("ddl_items", values, "id=?", arrayOf(existingId.toString()))
                    } else {
                        wdb.insert("ddl_items", null, values)
                    }
                    return@forEach
                }

                val doc = obj.objectMemberOrNull("doc")
                    ?: throw IllegalArgumentException("DDL item missing doc for uid=$uid")
                val state = DDLState.fromWire(doc["state"]?.asString ?: "")
                val legacy = when (state) {
                    DDLState.ACTIVE -> 0 to 0
                    DDLState.COMPLETED -> 1 to 0
                    DDLState.ARCHIVED -> 1 to 1
                    DDLState.ABANDONED -> 0 to 0
                    DDLState.ABANDONED_ARCHIVED -> 0 to 1
                }
                val values = ContentValues().apply {
                    put("name", doc["name"]?.asString ?: "")
                    put("start_time", doc["start_time"]?.asString ?: "")
                    put("end_time", doc["end_time"]?.asString ?: "")
                    put("is_completed", legacy.first)
                    put("complete_time", doc["complete_time"]?.asString ?: "")
                    put("note", doc["note"]?.asString ?: "")
                    put("is_archived", legacy.second)
                    put("state", state.toWire())
                    put("is_stared", doc["is_stared"]?.asInt ?: 0)
                    put("type", doc["type"]?.asString ?: "task")
                    put("habit_count", doc["habit_count"]?.asInt ?: 0)
                    put("habit_total_count", doc["habit_total_count"]?.asInt ?: 0)
                    put("calendar_event", doc["calendar_event"]?.asLong ?: -1L)
                    put("timestamp", doc["timestamp"]?.asString ?: "")
                    put("sub_tasks_json", gson.toJson(normalizeRemoteSubTasks(doc.getAsJsonArray("sub_tasks"))))
                    put("deleted", 0)
                    put("ver_ts", ver.ts)
                    put("ver_ctr", ver.ctr)
                    put("ver_dev", ver.dev)
                }
                if (existingId != null) {
                    wdb.update("ddl_items", values, "id=?", arrayOf(existingId.toString()))
                } else {
                    values.put("uid", uid)
                    wdb.insert("ddl_items", null, values)
                }
            }
            wdb.setTransactionSuccessful()
        } finally {
            wdb.endTransaction()
        }
    }

    internal fun applyHabitSnapshotToLocal(merged: JsonObject) {
        val items = merged.getAsJsonArray("items") ?: return
        val seenUids = mutableSetOf<String>()
        items.forEach { el ->
            val obj = el.asJsonObject
            val uid = obj["uid"].asString
            seenUids.add(uid)
            val ver = parseVer(obj.getAsJsonObject("ver"))
            val deleted = obj["deleted"]?.asBoolean == true || isHabitDocDeleted(obj.objectMemberOrNull("doc"))
            val ddlId = db.getDdlIdByUid(uid)
            var forceApply = false
            val applyDecision = HabitSyncRules.applyDecision(
                carrierExists = ddlId != null,
                deleted = deleted,
                appliedVer = ddlId?.let { db.getHabitAppliedVersionByDdlId(it) },
                incomingVer = ver
            )
            when (applyDecision) {
                HabitApplyDecision.NOOP_TOMBSTONE_WITHOUT_CARRIER -> {
                    Log.d("HabitSync", "Skip tombstone uid=$uid because carrier DDL is absent locally")
                    return@forEach
                }
                HabitApplyDecision.ERROR_MISSING_CARRIER -> {
                    Log.e("HabitSync", "Active habit uid=$uid missing local carrier DDL")
                    throw IllegalArgumentException("Habit carrier DDL missing for uid=$uid")
                }
                HabitApplyDecision.SKIP_ALREADY_APPLIED -> {
                    val resolvedDdlId = ddlId
                        ?: throw IllegalArgumentException("Habit carrier DDL missing for uid=$uid")
                    forceApply = shouldForceApplyDespiteAppliedVersion(obj, resolvedDdlId, ver.dev)
                    if (!forceApply) {
                        Log.d("HabitSync", "Skip apply uid=$uid because habit_applied_ver is newer or equal")
                        return@forEach
                    }
                    Log.w("HabitSync", "Force apply uid=$uid because payload differs while version is equal")
                }
                HabitApplyDecision.APPLY -> Unit
            }
            val resolvedDdlId = ddlId
                ?: throw IllegalArgumentException("Habit carrier DDL missing for uid=$uid")

            if (deleted) {
                Log.d("HabitSync", "Apply habit tombstone uid=$uid")
                val habitId = db.getHabitIdByDdlId(resolvedDdlId)
                if (habitId != null) {
                    db.deleteHabitRecordsForHabit(habitId)
                    db.deleteHabitByDdlId(resolvedDdlId)
                }
                db.writableDatabase.update(
                    "ddl_items",
                    ContentValues().apply {
                        put("deleted", 1)
                        put("timestamp", ver.ts)
                        put("ver_ts", ver.ts)
                        put("ver_ctr", ver.ctr)
                        put("ver_dev", ver.dev)
                    },
                    "id = ?",
                    arrayOf(resolvedDdlId.toString())
                )
                db.setHabitAppliedVersionByDdlId(resolvedDdlId, ver)
                return@forEach
            }

            val doc = obj.objectMemberOrNull("doc")
                ?: throw IllegalArgumentException("Habit item missing doc for uid=$uid")
            val ddlUid = doc["ddl_uid"]?.asString
                ?: throw IllegalArgumentException("Habit doc missing ddl_uid for uid=$uid")
            if (ddlUid != uid) {
                throw IllegalArgumentException("Habit ddl_uid mismatch: $ddlUid != $uid")
            }

            val habit = parseHabitPayload(doc.objectMemberOrNull("habit"), resolvedDdlId, db.getHabitIdByDdlId(resolvedDdlId) ?: 0L)
            val records = parseHabitRecords(doc.getAsJsonArray("records"), 0L)

            val existing = db.getHabitByDdlId(resolvedDdlId)
            val habitId = if (existing != null) {
                db.updateHabit(habit.copy(id = existing.id, ddlId = resolvedDdlId))
                existing.id
            } else {
                db.insertHabit(habit.copy(ddlId = resolvedDdlId))
            }

            db.deleteHabitRecordsForHabit(habitId)
            records.forEach { record ->
                db.insertHabitRecord(record.copy(habitId = habitId))
            }
            Log.d("HabitSync", "Apply habit doc uid=$uid records=${records.size}")
            db.setDdlVersionById(resolvedDdlId, ver)
            if (!forceApply) {
                db.setHabitAppliedVersionByDdlId(resolvedDdlId, ver)
            }
        }
        archiveMissingRemoteOwnedHabits(seenUids)
        val cleaned = db.cleanupOrphanHabits()
        if (cleaned > 0) {
            Log.w("HabitSync", "Cleaned $cleaned orphan habit(s) after applying habit snapshot")
        }
    }

    private fun shouldForceApplyDespiteAppliedVersion(
        item: JsonObject,
        ddlId: Long,
        incomingVerDev: String
    ): Boolean {
        if (incomingVerDev == db.getDeviceId()) return false
        val doc = item.objectMemberOrNull("doc") ?: return false
        val habitDoc = doc.objectMemberOrNull("habit") ?: return false
        val remoteStatus = runCatching {
            HabitStatus.valueOf(habitDoc["status"]?.takeUnless { it.isJsonNull }?.asString ?: return false)
        }.getOrNull() ?: return false
        val localHabit = db.getHabitByDdlId(ddlId) ?: return false
        if (localHabit.status != remoteStatus) return true

        val remoteUpdatedAtRaw = habitDoc["updated_at"]?.takeUnless { it.isJsonNull }?.asString ?: return false
        val remoteUpdatedAt = runCatching { parseFlexibleDateTime(remoteUpdatedAtRaw) }.getOrNull() ?: return false
        return localHabit.updatedAt != remoteUpdatedAt
    }

    private fun archiveMissingRemoteOwnedHabits(seenUids: Set<String>) {
        val localDeviceId = db.getDeviceId()
        db.getHabitCarrierSyncRows().forEach { carrier ->
            if (carrier.uid in seenUids) return@forEach
            if (carrier.deleted) return@forEach
            if (carrier.ver.dev == localDeviceId) return@forEach
            val habit = db.getHabitByDdlId(carrier.ddlId) ?: return@forEach
            if (habit.status == HabitStatus.ARCHIVED) return@forEach
            db.updateHabit(habit.copy(status = HabitStatus.ARCHIVED, updatedAt = parseFlexibleDateTime(carrier.ver.ts)))
            db.setHabitAppliedVersionByDdlId(carrier.ddlId, carrier.ver)
            Log.d("HabitSync", "Archive local habit uid=${carrier.uid} because payload is missing on remote-owned carrier")
        }
    }

    private fun parseVer(obj: JsonObject): Ver {
        return Ver(
            obj["ts"]?.asString ?: "1970-01-01T00:00:00Z",
            obj["ctr"]?.asInt ?: 0,
            obj["dev"]?.asString ?: ""
        )
    }

    private fun parseHabitPayload(doc: JsonObject?, ddlId: Long, habitId: Long): Habit {
        val payload = doc ?: throw IllegalArgumentException("Habit doc.habit missing")
        val period = runCatching { HabitPeriod.valueOf(payload["period"]?.asString ?: "") }
            .getOrElse { throw IllegalArgumentException("Invalid habit period", it) }
        val goalType = runCatching { HabitGoalType.valueOf(payload["goal_type"]?.asString ?: "") }
            .getOrElse { throw IllegalArgumentException("Invalid habit goal_type", it) }
        val status = runCatching { HabitStatus.valueOf(payload["status"]?.asString ?: "") }
            .getOrElse { throw IllegalArgumentException("Invalid habit status", it) }
        val times = payload["times_per_period"]?.asInt
            ?: throw IllegalArgumentException("Habit times_per_period missing")
        if (times <= 0) {
            throw IllegalArgumentException("Habit times_per_period must be positive")
        }

        val habit = Habit(
            id = habitId,
            ddlId = ddlId,
            name = payload["name"]?.asString ?: "",
            description = payload["description"]?.takeUnless { it.isJsonNull }?.asString,
            color = payload["color"]?.takeUnless { it.isJsonNull }?.asInt,
            iconKey = payload["icon_key"]?.takeUnless { it.isJsonNull }?.asString,
            period = period,
            timesPerPeriod = times,
            goalType = goalType,
            totalTarget = payload["total_target"]?.takeUnless { it.isJsonNull }?.asInt,
            createdAt = parseFlexibleDateTime(payload["created_at"]?.asString ?: throw IllegalArgumentException("Habit created_at missing")),
            updatedAt = parseFlexibleDateTime(payload["updated_at"]?.asString ?: throw IllegalArgumentException("Habit updated_at missing")),
            status = status,
            sortOrder = payload["sort_order"]?.asInt ?: 0,
            alarmTime = payload["alarm_time"]?.takeUnless { it.isJsonNull }?.asString
        )
        validateHabit(habit)
        return habit
    }

    private fun parseHabitRecords(records: JsonArray?, habitId: Long): List<HabitRecord> {
        val result = mutableListOf<HabitRecord>()
        records?.forEach { el ->
            val obj = el.asJsonObject
            val status = runCatching { HabitRecordStatus.valueOf(obj["status"]?.asString ?: "") }
                .getOrElse { throw IllegalArgumentException("Invalid habit record status", it) }
            val count = obj["count"]?.asInt
                ?: throw IllegalArgumentException("Habit record count missing")
            if (count <= 0) {
                throw IllegalArgumentException("Habit record count must be positive")
            }
            val record = HabitRecord(
                habitId = habitId,
                date = LocalDate.parse(obj["date"]?.asString ?: throw IllegalArgumentException("Habit record date missing")),
                count = count,
                status = status,
                createdAt = parseFlexibleDateTime(obj["created_at"]?.asString ?: throw IllegalArgumentException("Habit record created_at missing"))
            )
            validateHabitRecord(record)
            result.add(record)
        }
        return result
    }

    private fun validateHabit(habit: Habit) {
        if (habit.timesPerPeriod <= 0) {
            throw IllegalArgumentException("Habit times_per_period must be positive")
        }
    }

    private fun validateHabitRecord(record: HabitRecord) {
        if (record.count <= 0) {
            throw IllegalArgumentException("Habit record count must be positive")
        }
    }

    private fun parseFlexibleDateTime(raw: String): LocalDateTime {
        return runCatching { LocalDateTime.parse(raw) }
            .getOrElse {
                OffsetDateTime.parse(raw).toLocalDateTime()
            }
    }

    private suspend fun syncAllSnapshotsOnce(): Boolean = withContext(Dispatchers.IO) {
        repeat(2) { attempt ->
            val localDdl = buildLocalDdlSnapshotV2()
            val remoteDdlV2 = loadRemoteSnapshot(ddlSnapshotV2Path(), ::canonicalizeDdlV2Snapshot)
            val remoteDdlV1 = loadRemoteSnapshot(ddlSnapshotV1Path(), ::upgradeV1ToDdlV2)
            val mergedDdl = mergeSnapshots(localDdl, mergeSnapshots(remoteDdlV2.snapshot, remoteDdlV1.snapshot))

            try {
                putSnapshot(ddlSnapshotV2Path(), mergedDdl, remoteDdlV2)
                putSnapshot(ddlSnapshotV1Path(), projectDdlV2ToV1(mergedDdl), remoteDdlV1)
                applyDdlSnapshotToLocal(mergedDdl)

                val localHabit = buildLocalHabitSnapshotV2()
                val remoteHabit = loadRemoteSnapshot(habitSnapshotV2Path(), ::canonicalizeHabitV2Snapshot)
                val mergedHabit = mergeSnapshots(localHabit, remoteHabit.snapshot)
                putSnapshot(habitSnapshotV2Path(), mergedHabit, remoteHabit)
                applyHabitSnapshotToLocal(mergedHabit)
                val prunedCount = cleanupExpiredDeletedRows()
                Log.d("Sync", "pruned $prunedCount expired tombstones")
                return@withContext true
            } catch (_: WebUtils.PreconditionFailed) {
                if (attempt == 1) return@withContext false
            }
        }
        false
    }
}
