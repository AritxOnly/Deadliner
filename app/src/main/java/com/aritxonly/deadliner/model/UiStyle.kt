package com.aritxonly.deadliner.model

enum class UiStyle(val key: String) {
    Classic("classic"),
    Simplified("simplified");

    companion object {
        fun fromKey(k: String?): UiStyle = when (k) {
            Classic.key -> Classic
            Simplified.key -> Simplified
            else -> Classic
        }
    }
}