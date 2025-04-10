package com.aritxonly.deadliner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppColorScheme(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceContainer: Int
) : Parcelable
