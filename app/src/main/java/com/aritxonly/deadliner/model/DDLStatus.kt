package com.aritxonly.deadliner.model

import java.time.Duration
import java.time.LocalDateTime

enum class DDLStatus {
    COMPLETED,
    UNDERGO,
    NEAR,
    PASSED;

    companion object {
        fun calculateStatus(
            startTime: LocalDateTime?,
            endTime: LocalDateTime?,
            now: LocalDateTime = LocalDateTime.now(),
            isCompleted: Boolean
        ): DDLStatus {
            // 优先判断完成
            if (isCompleted) return COMPLETED

            // 如果没有 endTime，默认算进行中
            if (endTime == null) return UNDERGO

            // 逾期
            if (now.isAfter(endTime)) {
                return PASSED
            }

            // NEAR：距离结束时间 ≤ 12 小时
            val remainHours = Duration.between(now, endTime).toHours()
            if (remainHours <= 12) {
                return NEAR
            }

            // 否则就是进行中
            return UNDERGO
        }
    }
}