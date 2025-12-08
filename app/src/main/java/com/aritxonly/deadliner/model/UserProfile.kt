package com.aritxonly.deadliner.model

data class UserProfile(
    val nickname: String = "",
    val avatarFileName: String? = null,
) {
    fun hasAvatar() = !avatarFileName.isNullOrBlank()
}