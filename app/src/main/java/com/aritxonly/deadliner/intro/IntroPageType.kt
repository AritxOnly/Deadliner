package com.aritxonly.deadliner.intro

sealed class IntroPageType(val index: Int) {

    data object Cover : IntroPageType(0)
    data object Permissions : IntroPageType(1)
    data object Theme : IntroPageType(2)
    data object Wizard : IntroPageType(3)
    data object Final : IntroPageType(4)

    companion object {
        val ordered: List<IntroPageType> = listOf(
            Cover,
            Permissions,
            Theme,
            Wizard,
            Final
        )

        fun fromPosition(position: Int): IntroPageType = ordered[position]

        val count: Int get() = ordered.size
    }
}