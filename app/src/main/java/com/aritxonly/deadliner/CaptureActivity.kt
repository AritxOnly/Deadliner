package com.aritxonly.deadliner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.capture.ui.CaptureScreen
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

class CaptureActivity : DeadlinerComponentActivity() {
    private val vm by viewModels<CaptureViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        DynamicColorsExtension.apply(this, null)

        setContent {
            DeadlinerTheme {
                CaptureScreen(vm = vm, onClose = { finishAfterTransition() })
            }
        }
    }
}
