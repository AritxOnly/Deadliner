package com.aritxonly.deadliner.ui.main.classic

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.databinding.ActivityMainBinding

@Composable
fun ClassicHost(
    activity: MainActivity,
    addDDLLauncher: ActivityResultLauncher<Intent>,
    notifyPermissionLauncher: ActivityResultLauncher<String>,
    calendarPermissionLauncher: ActivityResultLauncher<Array<String>>
) {
    val controller = remember(activity) { ClassicController(activity) }

    // 传入 launchers（非 @Composable，普通方法即可）
    LaunchedEffect(Unit) {
        controller.setLaunchers(
            addDDL = addDDLLauncher,
            notifyPermission = notifyPermissionLauncher,
            calendarPermission = calendarPermissionLauncher
        )
    }

    var bindingRef by remember { mutableStateOf<ActivityMainBinding?>(null) }
    AndroidViewBinding(ActivityMainBinding::inflate) { bindingRef = this }

    var attached by remember { mutableStateOf(false) }
    LaunchedEffect(bindingRef) { bindingRef?.let {
        controller.attach(it)
        attached = true
    } }
    DisposableEffect(Unit) { onDispose { controller.detach() } }

    // 回灌 controller 引用、生命周期桥接（保持你之前写法）
    DisposableEffect(controller) {
        activity.setClassicControllerRef(controller)
        onDispose { activity.setClassicControllerRef(null) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller) {
        if (!attached) return@DisposableEffect onDispose {  }
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME  -> controller.onResume()
                Lifecycle.Event.ON_STOP    -> controller.onStop()
                Lifecycle.Event.ON_DESTROY -> controller.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
}