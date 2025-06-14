package com.aritxonly.deadliner.web

import android.util.Log
import android.widget.Toast
import com.aritxonly.deadliner.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

object WebUtils {
    private const val API_URL_DIR = "api"
    private const val API_VERSION = "v1"

    private var apiUrl: String? = null

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

    suspend fun getFromApiAndUpdate(
        databaseHelper: DatabaseHelper
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiUrl == null) return@withContext false

        Log.d("WebUtils", "Get Database from api")
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$apiUrl/db/items")
                .header("AndroidKey", GlobalUtils.cloudSyncConstantToken?:"")
                .build()

            val response = client.newCall(request).execute()

            Log.d("WebUtils", "$response")

            response.use {
                if (!it.isSuccessful) {
                    throw Exception("Server returned error code ${it.code}")
                }

                val bodyString = it.body!!.string()

                val gson = Gson()

                val listType = object : TypeToken<List<DDLItem>>() {}.type
                val ddlList: List<DDLItem> = gson.fromJson(bodyString, listType)

                val actualList = databaseHelper.getAllDDLs()

                for (item in ddlList) {
                    val matchedItem: DDLItem? = actualList.find { it.name == item.name }
                    if (matchedItem == null) {
                        databaseHelper.insertDDL(
                            item.name,
                            item.startTime,
                            item.endTime,
                            item.note,
                            item.type,
                            item.calendarEventId
                        )
                    } else {
                        val webTimestamp = GlobalUtils.safeParseDateTime(item.timeStamp)
                        val actualTimestamp = GlobalUtils.safeParseDateTime(matchedItem.timeStamp)
                        if (webTimestamp.isAfter(actualTimestamp)) {
                            databaseHelper.updateDDL(item)
                        }
                    }
                }

                it.isSuccessful
            }
        } catch (e: IOException) {
            Log.e("WebUtils", e.toString())
            false
        }
    }
}