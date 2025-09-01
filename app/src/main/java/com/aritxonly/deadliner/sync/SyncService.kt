package com.aritxonly.deadliner.sync

import android.content.ContentValues
import android.util.Log
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.web.WebUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.time.Instant

class SyncService(
    private val db: DatabaseHelper,
    private val web: WebUtils
) {
    private val gson = Gson()

    // 业务层钩子（可保留为触发同步的信号；此处不做任何 DB 操作）
    fun onLocalInserted(newLocalId: Long) { /* no-op */ }
    fun onLocalUpdated(localId: Long)     { /* no-op */ }
    fun onLocalDeleting(localId: Long)    { /* no-op */ }

    suspend fun syncOnce(): Boolean {
        Log.d("WebDAV", "snapshot syncOnce")
        return try {
            syncSnapshotOnce()
        } catch (e: Exception) {
            Log.w("Sync", "Failed: $e")
            false
        }
    }

    private fun snapshotPath() = "Deadliner/snapshot-v1.json"

    // ============== 1) 构造“本地快照”（包含墓碑） ==============
    private fun buildLocalSnapshot(): JsonObject {
        val items = JsonArray()

        val sql = """
            SELECT 
                uid,
                COALESCE(deleted,0)              AS deleted,
                COALESCE(ver_ts,'1970-01-01T00:00:00Z') AS ver_ts,
                COALESCE(ver_ctr,0)              AS ver_ctr,
                COALESCE(ver_dev,'')             AS ver_dev,

                id, name, start_time, end_time, is_completed, complete_time, note,
                is_archived, is_stared, type, habit_count, habit_total_count, calendar_event, timestamp
            FROM ddl_items
        """.trimIndent()

        db.readableDatabase.rawQuery(sql, null).use { c ->
            val idxUid  = c.getColumnIndexOrThrow("uid")
            val idxDel  = c.getColumnIndexOrThrow("deleted")
            val idxVts  = c.getColumnIndexOrThrow("ver_ts")
            val idxVctr = c.getColumnIndexOrThrow("ver_ctr")
            val idxVdev = c.getColumnIndexOrThrow("ver_dev")

            val idxId   = c.getColumnIndexOrThrow("id")
            val idxName = c.getColumnIndexOrThrow("name")
            val idxSt   = c.getColumnIndexOrThrow("start_time")
            val idxEt   = c.getColumnIndexOrThrow("end_time")
            val idxCmp  = c.getColumnIndexOrThrow("is_completed")
            val idxCmt  = c.getColumnIndexOrThrow("complete_time")
            val idxNote = c.getColumnIndexOrThrow("note")
            val idxArc  = c.getColumnIndexOrThrow("is_archived")
            val idxStar = c.getColumnIndexOrThrow("is_stared")
            val idxType = c.getColumnIndexOrThrow("type")
            val idxHC   = c.getColumnIndexOrThrow("habit_count")
            val idxHTC  = c.getColumnIndexOrThrow("habit_total_count")
            val idxCal  = c.getColumnIndexOrThrow("calendar_event")
            val idxTs   = c.getColumnIndexOrThrow("timestamp")

            while (c.moveToNext()) {
                val uid = c.getString(idxUid) ?: continue
                val deleted = c.getInt(idxDel) != 0
                val vts = c.getString(idxVts)
                val vctr = c.getInt(idxVctr)
                val vdev = c.getString(idxVdev)

                val ver = JsonObject().apply {
                    addProperty("ts", vts)
                    addProperty("ctr", vctr)
                    addProperty("dev", vdev)
                }

                val jo = JsonObject().apply {
                    addProperty("uid", uid)
                    add("ver", ver)
                    addProperty("deleted", deleted)
                }

                if (!deleted) {
                    val doc = JsonObject().apply {
                        addProperty("id",                 c.getLong(idxId))
                        addProperty("name",               c.getString(idxName))
                        addProperty("start_time",         c.getString(idxSt))
                        addProperty("end_time",           c.getString(idxEt))
                        addProperty("is_completed",       c.getInt(idxCmp))
                        addProperty("complete_time",      c.getString(idxCmt))
                        addProperty("note",               c.getString(idxNote))
                        addProperty("is_archived",        c.getInt(idxArc))
                        addProperty("is_stared",          c.getInt(idxStar))
                        addProperty("type",               c.getString(idxType))
                        addProperty("habit_count",        c.getInt(idxHC))
                        addProperty("habit_total_count",  c.getInt(idxHTC))
                        addProperty("calendar_event",  c.getInt(idxCal))
                        addProperty("timestamp",          c.getString(idxTs))
                    }
                    jo.add("doc", doc)
                } else {
                    jo.add("doc", JsonNull.INSTANCE)
                }

                items.add(jo)
            }
        }

        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts",  Instant.now().toString())
                addProperty("dev", db.getDeviceId())
            })
            add("items", items)
        }
    }

    // ============== 2) 合并（LWW：ts > ctr > dev） ==============
    private fun newer(a: JsonObject, b: JsonObject): Boolean {
        val av = a.getAsJsonObject("ver"); val bv = b.getAsJsonObject("ver")
        val ats = av["ts"].asString; val bts = bv["ts"].asString
        if (ats != bts) return ats > bts
        val actr = av["ctr"]?.asInt ?: 0
        val bctr = bv["ctr"]?.asInt ?: 0
        if (actr != bctr) return actr > bctr
        val adev = av["dev"]?.asString ?: ""
        val bdev = bv["dev"]?.asString ?: ""
        return adev >= bdev
    }

    private fun mergeSnapshots(local: JsonObject, remote: JsonObject): JsonObject {
        fun toMap(root: JsonObject): MutableMap<String, JsonObject> {
            val map = mutableMapOf<String, JsonObject>()
            root.getAsJsonArray("items")?.forEach { el ->
                val obj = el.asJsonObject
                map[obj.get("uid").asString] = obj
            }
            return map
        }
        val lmap = toMap(local)
        val rmap = toMap(remote)
        val keys = (lmap.keys + rmap.keys).toMutableSet()
        val outItems = JsonArray()

        for (uid in keys) {
            val L = lmap[uid]
            val R = rmap[uid]
            val chosen = when {
                L == null -> R!!
                R == null -> L
                else      -> if (newer(L, R)) L else R
            }
            outItems.add(chosen.deepCopy())
        }

        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts",  Instant.now().toString())
                addProperty("dev", db.getDeviceId())
            })
            add("items", outItems)
        }
    }

    // ============== 3) 应用“最终快照”到本地（按 uid upsert/软删） ==============
    private fun applySnapshotToLocal(merged: JsonObject) {
        val arr = merged.getAsJsonArray("items") ?: return
        val wdb = db.writableDatabase

        // 回放期间不触发你上层的“记账/同步钩子”（避免回声）
        // 这里直接批量写 DB
        arr.forEach { el ->
            val obj = el.asJsonObject
            val uid = obj["uid"].asString

            val vjo  = obj.getAsJsonObject("ver")
            val vts  = vjo["ts"].asString
            val vctr = vjo["ctr"]?.asInt ?: 0
            val vdev = vjo["dev"].asString

            val deleted = obj["deleted"]?.asBoolean == true

            // 是否已有此 uid
            val cur = wdb.rawQuery(
                "SELECT id FROM ddl_items WHERE uid=? LIMIT 1",
                arrayOf(uid)
            )
            val exists = cur.moveToFirst()
            val localId = if (exists) cur.getLong(0) else null
            cur.close()

            if (deleted) {
                if (exists) {
                    val cv = ContentValues().apply {
                        put("deleted", 1)
                        put("ver_ts", vts); put("ver_ctr", vctr); put("ver_dev", vdev)
                    }
                    wdb.update("ddl_items", cv, "id=?", arrayOf(localId.toString()))
                } else {
                    val cv = ContentValues().apply {
                        put("uid", uid)
                        put("deleted", 1)
                        // 业务字段占位
                        put("name", "(deleted)")
                        put("start_time",""); put("end_time","")
                        put("is_completed",1); put("complete_time","")
                        put("note",""); put("is_archived",1); put("is_stared",0)
                        put("type","task"); put("habit_count",0); put("habit_total_count",0)
                        put("calendar_event",-1); put("timestamp", vts)
                        // 版本
                        put("ver_ts", vts); put("ver_ctr", vctr); put("ver_dev", vdev)
                    }
                    wdb.insert("ddl_items", null, cv)
                }
            } else {
                val doc = obj.getAsJsonObject("doc")
                val cv = ContentValues().apply {
                    put("name",               doc["name"].asString)
                    put("start_time",         doc["start_time"].asString)
                    put("end_time",           doc["end_time"].asString)
                    put("is_completed",       doc["is_completed"].asInt)
                    put("complete_time",      doc["complete_time"].asString)
                    put("note",               doc["note"].asString)
                    put("is_archived",        doc["is_archived"].asInt)
                    put("is_stared",          doc["is_stared"].asInt)
                    put("type",               doc["type"].asString)
                    put("habit_count",        doc["habit_count"].asInt)
                    put("habit_total_count",  doc["habit_total_count"].asInt)
                    put("calendar_event",     doc["calendar_event"].asInt)
                    put("timestamp",          doc["timestamp"].asString)

                    put("deleted", 0)
                    put("ver_ts", vts); put("ver_ctr", vctr); put("ver_dev", vdev)
                }
                if (exists) {
                    wdb.update("ddl_items", cv, "id=?", arrayOf(localId.toString()))
                } else {
                    cv.put("uid", uid)
                    wdb.insert("ddl_items", null, cv)
                }
            }
        }
    }

    // ============== 4) 一次完整“快照同步” ==============
    private suspend fun syncSnapshotOnce(): Boolean = withContext(Dispatchers.IO) {
        val path = snapshotPath()
        val localSnap = buildLocalSnapshot()

        // 1) 探测远端（某些 WebDAV 把“文件不存在”也回 409）
        val (code, _, _) = runCatching { web.head(path) }
            .getOrElse {
                Log.e("WebDAV", "HEAD failed: ${it.message}"); return@withContext false
            }

        // === 关键改动：把 404/409/410 都当成“文件不存在” ===
        if (code in listOf(404, 409, 410)) {
            // 可选：确保父目录存在（见下文 MKCOL），失败也不致命
            runCatching { web.ensureDir("Deadliner") }

            val bytes = gson.toJson(localSnap).toByteArray(StandardCharsets.UTF_8)
            val created = runCatching {
                // 首次创建用 If-None-Match: *，避免和并发方冲突
                web.putBytes(path, bytes, ifMatch = null, ifNoneMatchStar = true)
            }.isSuccess
            return@withContext created
        }

        if (code in 500..599) {
            Log.w("WebDAV", "HEAD $path -> $code (server unavailable)")
            return@withContext false
        }

        // 2) 拉远端快照（如果 GET 仍回 409，就直接按“空远端”合并）
        val remotePair = runCatching { web.getBytes(path) }.getOrElse { e ->
            val msg = e.message.orEmpty()
            return@withContext if (msg.contains(" 409") || msg.contains("-> 409")) {
                // 把远端当“空快照”
                val remoteEmpty = JsonObject().apply {
                    add("version", JsonObject().apply {
                        addProperty("ts", "1970-01-01T00:00:00Z"); addProperty("dev", "unknown")
                    })
                    add("items", JsonArray())
                }
                val merged = mergeSnapshots(localSnap, remoteEmpty)
                val mergedBytes = gson.toJson(merged).toByteArray(StandardCharsets.UTF_8)
                runCatching {
                    web.putBytes(
                        path,
                        mergedBytes,
                        ifMatch = null,
                        ifNoneMatchStar = true
                    )
                }
                    .isSuccess
            } else {
                Log.e("WebDAV", "GET $path failed: ${e.message}"); false
            }
        }

        val (remoteBytes, currentEtag) = remotePair
        val remoteSnap = try {
            JsonParser.parseString(remoteBytes.toString(StandardCharsets.UTF_8)).asJsonObject
        } catch (_: Exception) {
            JsonObject().apply {
                add("version", JsonObject().apply {
                    addProperty("ts", "1970-01-01T00:00:00Z"); addProperty("dev", "unknown")
                })
                add("items", JsonArray())
            }
        }

        // 3) 合并 + 4) If-Match 写回（保持你现有逻辑）
        val merged = mergeSnapshots(localSnap, remoteSnap)
        val mergedBytes = gson.toJson(merged).toByteArray(StandardCharsets.UTF_8)
        try {
            web.putBytes(path, mergedBytes, ifMatch = currentEtag, ifNoneMatchStar = false)
        } catch (e: WebUtils.PreconditionFailed) {
            val (rb2, et2) = web.getBytes(path)
            val merged2 = mergeSnapshots(
                merged,
                JsonParser.parseString(rb2.toString(StandardCharsets.UTF_8)).asJsonObject
            )
            web.putBytes(
                path,
                gson.toJson(merged2).toByteArray(StandardCharsets.UTF_8),
                ifMatch = et2,
                ifNoneMatchStar = false
            )
        }

        applySnapshotToLocal(merged)
        true
    }
}