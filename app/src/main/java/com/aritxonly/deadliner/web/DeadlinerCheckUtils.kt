package com.aritxonly.deadliner.web

import android.util.Log
import com.aritxonly.deadliner.model.DeadlinerCheckResp
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private val gson by lazy { Gson() }
private val http by lazy { OkHttpClient() }

suspend fun fetchQuotaCheck(
    endpoint: String,
    appSecret: String,
    deviceId: String
): Result<DeadlinerCheckResp> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val base = if (endpoint.endsWith("/")) endpoint else "$endpoint/"
    val url = base + "check"

    val req = Request.Builder()
        .url(url)
        .header("X-Deadliner-Key", appSecret)
        .header("X-Deadliner-Device", deviceId)
        .get()
        .build()

    Log.d("DeadlinerCheck", req.toString())

    try {
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${resp.code}"))
            }
            val body = resp.body?.string().orEmpty()
            Log.d("DeadlinerCheck", body)
            val data = gson.fromJson(body, DeadlinerCheckResp::class.java)
            Result.success(data)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}