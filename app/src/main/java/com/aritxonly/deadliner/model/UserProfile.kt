package com.aritxonly.deadliner.model

data class UserProfile(
    val nickname: String = "用户",
    val avatarFileName: String? = null,
) {
    fun hasAvatar() = !avatarFileName.isNullOrBlank()
}