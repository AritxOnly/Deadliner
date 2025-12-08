package com.aritxonly.deadliner.data

import androidx.datastore.preferences.core.stringPreferencesKey

object UserKeys {
    val NICKNAME = stringPreferencesKey("nickname")
    val AVATAR_FILE_NAME = stringPreferencesKey("avatar_file_name")
}