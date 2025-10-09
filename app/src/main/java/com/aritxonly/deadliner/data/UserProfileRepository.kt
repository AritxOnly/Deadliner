package com.aritxonly.deadliner.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.aritxonly.deadliner.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.min

object UserProfileRepository  {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun ctx(): Context {
        check(::appContext.isInitialized) { "UserProfileRepository.init(context) must be called before use." }
        return appContext
    }

    val profile: Flow<UserProfile>
        get() =
            ctx().userProfileDataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs ->
                    UserProfile(
                        nickname = prefs[UserKeys.NICKNAME].orEmpty(),
                        avatarFileName = prefs[UserKeys.AVATAR_FILE_NAME]
                    )
                }

    suspend fun setNickname(nickname: String) {
        ctx().userProfileDataStore.edit { prefs ->
            prefs[UserKeys.NICKNAME] = nickname
        }
    }

    suspend fun removeAvatar(context: Context) {
        val dir = File(ctx().filesDir, "avatars")
        val oldName = currentAvatarFileName()
        ctx().userProfileDataStore.edit { prefs ->
            prefs.remove(UserKeys.AVATAR_FILE_NAME)
        }
        if (!oldName.isNullOrBlank()) {
            File(dir, oldName).takeIf { it.exists() }?.delete()
        }
    }

    private suspend fun currentAvatarFileName(): String? {
        return ctx().userProfileDataStore.data
            .map { it[UserKeys.AVATAR_FILE_NAME] }
            .firstOrNull()
    }

    suspend fun setAvatarFromBitmap(context: Context, bmp: Bitmap) {
        val dir = File(ctx().filesDir, "avatars").apply { mkdirs() }
        val newName = "${UUID.randomUUID()}.jpg"
        val dst = File(dir, newName)

        FileOutputStream(dst).use { out ->
            // 这里你希望头像是 512x512 的正方形（不失真）
            val outBmp = if (bmp.width != bmp.height) {
                val size = min(bmp.width, bmp.height)
                val x = (bmp.width - size) / 2
                val y = (bmp.height - size) / 2
                Bitmap.createBitmap(bmp, x, y, size, size)
            } else bmp
            val scaled = if (outBmp.width != 512) {
                Bitmap.createScaledBitmap(outBmp, 512, 512, true)
            } else outBmp
            scaled.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // 更新 DataStore
        val oldName = ctx().userProfileDataStore.data
            .map { it[UserKeys.AVATAR_FILE_NAME] }
            .firstOrNull()

        ctx().userProfileDataStore.edit { prefs ->
            prefs[UserKeys.AVATAR_FILE_NAME] = newName
        }

        if (!oldName.isNullOrBlank() && oldName != newName) {
            File(dir, oldName).takeIf { it.exists() }?.delete()
        }
    }
}