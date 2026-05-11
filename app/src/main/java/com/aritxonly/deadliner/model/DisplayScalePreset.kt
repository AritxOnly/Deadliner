package com.aritxonly.deadliner.model

enum class DisplayScalePreset(val key: String, val multiplier: Float?) {
    FollowSystem("follow_system", null),
    Compact("compact", 0.90f),
    SlightlyCompact("slightly_compact", 0.95f),
    Standard("standard", 1.00f),
    SlightlyLarge("slightly_large", 1.08f),
    Large("large", 1.16f),
    Custom("custom", null);

    companion object {
        fun fromKey(key: String?): DisplayScalePreset = entries.firstOrNull { it.key == key } ?: FollowSystem
    }
}
