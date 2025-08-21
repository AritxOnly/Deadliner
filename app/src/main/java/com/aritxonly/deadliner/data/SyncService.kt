package com.aritxonly.deadliner.data

import android.util.Log
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.model.ChangeLine
import com.aritxonly.deadliner.model.Ver
import com.aritxonly.deadliner.model.SyncState
import com.aritxonly.deadliner.web.WebUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SyncService(
    private val db: DatabaseHelper,
    private val web: WebUtils
) {
    private val gson = Gson()

    // —— 供业务层调用的“钩子” —— //
    fun onLocalInserted(newLocalId: Long) = db.journalUpsert(newLocalId)
    fun onLocalUpdated(localId: Long) = db.journalUpsert(localId)
    fun onLocalDeleting(localId: Long) = db.journalDelete(localId)

    // —— 一次完整同步：先上传再拉取 —— //
    suspend fun syncOnce(): Boolean {
        Log.d("WebDAV", "syncOnce")
        val up = uploadLocalJournal()
        val down = pullRemoteTail()
        return up && down
    }

    // 远端 changes 文件名（按月滚动）
    private fun changesFileName(today: LocalDate = LocalDate.now()): String {
        val ym = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return "Deadliner/changes-$ym.ndjson"
    }

    // 序列化 NDJSON（逐行）
    private fun serializeNdjson(changes: List<ChangeLine>): ByteArray {
        if (changes.isEmpty()) return ByteArray(0)
        val sb = StringBuilder()
        for (ch in changes) {
            val verJson = gson.toJson(ch.ver)
            val line = if (ch.op == "upsert") {
                """{"op":"upsert","uid":"${ch.uid}","doc":${ch.snapshot ?: "{}"},"ver":$verJson}"""
            } else {
                """{"op":"delete","uid":"${ch.uid}","doc":null,"ver":$verJson}"""
            }
            sb.append(line).append('\n')
        }
        return sb.toString().toByteArray(StandardCharsets.UTF_8)
    }

    // 上传：把本机 journal（未上传段）追加到远端 changes 文件
    private suspend fun uploadLocalJournal(): Boolean = withContext(Dispatchers.IO) {
        val state: SyncState = db.getSyncState()
        val notUploaded = db.loadJournalAfterSeq(state.lastUploadedSeq, limit = 1000)
        if (notUploaded.isEmpty()) return@withContext true

        val file = changesFileName()
        val payload = serializeNdjson(notUploaded)

        // 读取现有内容（简化：全量 GET；数据大时可优化为缓存+Range）
        val (code, etag, _) = web.head(file)
        val existing = when (code) {
            200, 204 -> web.getBytes(file).first
            404 -> ByteArray(0)
            else -> ByteArray(0)
        }

        val merged = if (existing.isEmpty()) payload else {
            val out = ByteArray(existing.size + payload.size)
            System.arraycopy(existing, 0, out, 0, existing.size)
            System.arraycopy(payload, 0, out, existing.size, payload.size)
            out
        }

        try {
            if (etag == null || code == 404) {
                web.putBytes(file, merged, ifMatch = null, ifNoneMatchStar = true)
            } else {
                web.putBytes(file, merged, ifMatch = etag, ifNoneMatchStar = false)
            }
        } catch (e: WebUtils.PreconditionFailed) {
            // 并发写冲突：交给外层重试
            return@withContext false
        }

        // 标记这些 seq 已上传
        val maxSeq = state.lastUploadedSeq + notUploaded.size
        db.markUploadedThrough(maxSeq)
        true
    }

    private suspend fun pullRemoteTail(): Boolean = withContext(Dispatchers.IO) {
        val state = db.getSyncState()
        val file = changesFileName()

        val (code, etag, _) = web.head(file)
        if (code == 404) {
//            Log.d("Sync","$file not found; nothing to pull")
            return@withContext true
        }

        // ETag 没变 -> 远端文件未变化，直接返回
        if (etag != null && state.changesEtag != null && etag == state.changesEtag) {
//            Log.d("Sync","etag unchanged ($etag), skip pulling")
            return@withContext true
        }

        // 全量 GET，再回放
        val (bytes, etag2) = web.getBytes(file)
        if (bytes.isEmpty()) return@withContext true

        val text = bytes.toString(StandardCharsets.UTF_8)
        applyNdjsonTextAll(text)

        // 把偏移直接置为当前长度（方便后续切回增量时使用）
        db.updateChangesPointer(etag2 ?: etag, bytes.size.toLong())
//        Log.d("Sync","full pull done: new etag=${etag2 ?: etag}, new offset=${bytes.size}")
        true
    }

    private fun applyNdjsonTextAll(text: String) {
        val parser = com.google.gson.JsonStreamParser(text)
        var applied = 0
        while (parser.hasNext()) {
            try {
                val jo = parser.next().asJsonObject
                val op = jo.get("op").asString
                val uid = jo.get("uid").asString
                val verObj = jo.getAsJsonObject("ver")
                val ver = Ver(
                    ts = verObj.get("ts").asString,
                    ctr = verObj.get("ctr").asInt,
                    dev = verObj.get("dev").asString
                )
                val snap = if (jo.has("doc") && !jo.get("doc").isJsonNull) jo.get("doc").toString() else null

                // 关闭本地记账，避免形成回环
                db.applyRemoteChange(ChangeLine(op = op, uid = uid, snapshot = snap, ver = ver))
                applied++
            } catch (e: Exception) {
                Log.w("Sync", "parse one json failed: ${e.message}")
            }
        }
//        Log.d("Sync", "applied=$applied")
    }

    suspend fun debugDumpCurrentMonth(): Unit = withContext(Dispatchers.IO) {
        val file = changesFileName()
        val (bytes, etag) = web.getBytes(file) // 全量
        val text = bytes.toString(StandardCharsets.UTF_8)
        val lines = text.lines().filter { it.isNotBlank() }
//        android.util.Log.d("SyncDbg","dump $file etag=$etag lines=${lines.size}")
        lines.take(5).forEachIndexed { i, ln -> android.util.Log.d("SyncDbg","head[$i] $ln") }
        if (lines.size > 5) android.util.Log.d("SyncDbg","...")
        lines.takeLast(5).forEachIndexed { i, ln -> android.util.Log.d("SyncDbg","tail[${lines.size-5+i}] $ln") }
    }

    fun debugResetOffsetToZero() {
        db.updateChangesPointer(etag = null, offset = 0L)
        android.util.Log.d("SyncDbg","offset reset to 0")
    }

    fun debugLogLocalState() {
        val st = db.getSyncState()
        android.util.Log.d("SyncDbg","state device=${st.deviceId} lastSeq=${st.lastUploadedSeq} etag=${st.changesEtag} offset=${st.changesOffset}")
    }
}