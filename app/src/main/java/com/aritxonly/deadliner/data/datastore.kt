package com.aritxonly.deadliner.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")