package com.aritxonly.deadliner.web

import android.util.Log
import android.widget.Toast
import com.aritxonly.deadliner.GlobalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

object WebUtils {
    private const val API_URL_DIR = "api"
    private const val API_VERSION = "v1"

    private var apiUrl: String? = null
        get() = field
        set(value) { field = value }

    fun init() {
        val host = GlobalUtils.cloudSyncServer ?: return
        val port = GlobalUtils.cloudSyncPort
        val formattedHost = if (host.startsWith("http://") || host.startsWith("https://")) host else "http://$host"
        val base = if (port == 80 || port == 443) {
            // 不显式添加默认端口
            formattedHost
        } else {
            "$formattedHost:$port"
        }

        apiUrl = "$base/$API_URL_DIR/$API_VERSION"
    }

    suspend fun isWebAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (apiUrl == null) return@withContext false

        Log.d("WebUtils", "Ping $apiUrl")
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(apiUrl!!)
                .build()

            val response = client.newCall(request).execute()

            Log.d("WebUtils", "$response")

            response.use {
                it.isSuccessful
            }
        } catch (e: IOException) {
            Log.e("WebUtils", e.toString());
            false
        }
    }
}