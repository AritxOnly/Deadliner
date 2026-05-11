package com.aritxonly.deadliner.model

enum class AppThemeStyle(val key: String) {
    Material3("material3"),
    Miuix("miuix");

    companion object {
        fun fromKey(value: String?): AppThemeStyle = when (value) {
            Miuix.key -> Miuix
            else -> Material3
        }
    }
}
