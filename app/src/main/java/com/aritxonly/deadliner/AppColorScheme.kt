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
    val surfaceContainer: Int,
    val secondary: Int?,
    val onSecondary: Int?,
    val secondaryContainer: Int?,
    val onSecondaryContainer: Int?,
    val tertiary: Int?,
    val onTertiary: Int?,
    val tertiaryContainer: Int?,
    val onTertiaryContainer: Int?
) : Parcelable
