package com.aritxonly.deadliner

import android.content.Context

class GlobalUtils {
    // TODO: 保证代码的可复用性
    companion object {
        // TODO: 设置项，常用的工具函数
        fun dpToPx(dp: Float, context: Context): Float {
            return dp * context.resources.displayMetrics.density
        }
    }
}