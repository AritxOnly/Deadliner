package com.aritxonly.deadliner.model

enum class AppIconMode(val key: String) {
    Default("default"),
    SeasonalAuto("seasonal_auto"),
    SeasonalSpring("seasonal_spring"),
    SeasonalSummer("seasonal_summer"),
    SeasonalAutumn("seasonal_autumn"),
    SeasonalWinter("seasonal_winter"),
    Custom("custom");

    companion object {
        fun fromKey(key: String?): AppIconMode = entries.firstOrNull { it.key == key } ?: Default
    }
}
