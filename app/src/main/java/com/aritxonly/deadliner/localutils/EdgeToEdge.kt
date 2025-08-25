package com.aritxonly.deadliner.localutils

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

/**
 * 用于开启所有设备的edgeToEdge模式：
 *      小米HyperOS由于历史原因，需要加入`window.isNavigationBarContrastEnforced`保持导航栏沉浸
 *      并使用enableEdgeToEdge()开启edgeToEdge
 * @param: null
 */
fun ComponentActivity.enableEdgeToEdgeForAllDevices() {
    enableEdgeToEdge()

    // For Xiaomi HyperOS: god knows why they need this to keep edgeToEdge in light mode.
    window.isNavigationBarContrastEnforced = false
}