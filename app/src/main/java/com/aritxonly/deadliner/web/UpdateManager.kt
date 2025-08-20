package com.aritxonly.deadliner.web

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object UpdateManager {
    private const val GITHUB_LATEST_RELEASE = "https://api.github.com/repos/AritxOnly/Deadliner/releases/latest"

    suspend fun fetchUpdateInfo(context: Context): UpdateInfo {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        val current = pkg.versionName ?: "0.0.0"

        val client = OkHttpClient()
        val request = Request.Builder().url(GITHUB_LATEST_RELEASE).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val json = JSONObject(resp.body!!.string())
            val latest = json.getString("tag_name")
            val notes = json.getString("body")
            val assets = json.getJSONArray("assets")
            val url = assets.takeIf { it.length() > 0 }
                ?.getJSONObject(0)
                ?.getString("browser_download_url")
                .orEmpty()

            return UpdateInfo(current, latest, notes, url)
        }
    }

    /** SemVer 比较 */
    fun isNewer(current: String, latest: String): Boolean =
        compareSemVer(current, latest) < 0

    private fun compareSemVer(v1: String, v2: String): Int {
        val p1 = v1.removePrefix("v").split(".")
        val p2 = v2.removePrefix("v").split(".")
        val max = maxOf(p1.size, p2.size)
        for (i in 0 until max) {
            val s1 = p1.getOrNull(i).orEmpty()
            val s2 = p2.getOrNull(i).orEmpty()
            val n1 = s1.substringBefore('-').toIntOrNull() ?: 0
            val n2 = s2.substringBefore('-').toIntOrNull() ?: 0
            if (n1 != n2) return n1 - n2
            val suf1 = s1.substringAfter('-', "")
            val suf2 = s2.substringAfter('-', "")
            if (suf1.isEmpty() != suf2.isEmpty()) return if (suf1.isEmpty()) 1 else -1
            if (suf1 != suf2) return suf1.compareTo(suf2)
        }
        return 0
    }
}