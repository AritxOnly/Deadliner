// PosterExport.kt
@file:Suppress("SetTextI18n")

package com.aritxonly.deadliner.ui.poster

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.ComponentDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import com.aritxonly.deadliner.ui.overview.TextTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("SetTextI18n")
suspend fun renderPosterToCacheUri(
    activity: Activity,
    data: ExportDashboardData,
    widthPx: Int = 1080,
    backgroundBitmap: Bitmap? = null,
    textTone: TextTone = TextTone.Light,
    fileNamePrefix: String = "deadliner_dashboard_"
): Uri = withContext(Dispatchers.Main) {
    val dialog = androidx.activity.ComponentDialog(
        activity,
        android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
    ).apply {
        // 关键：禁止返回键 & 外部点击关闭
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.setDimAmount(0f) // 无需背景变暗
    }

    val root = FrameLayout(activity).apply { alpha = 0f }

    val composeView = androidx.compose.ui.platform.ComposeView(activity).apply {
        tag = "poster"
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = -10000
            topMargin = -10000
        }
        setContent {
            MaterialTheme {
                ShareDashboardPoster(
                    data = data,
                    widthPx = widthPx,
                    backgroundBitmap = backgroundBitmap,
                    textTone = textTone
                )
            }
        }
    }

    root.addView(composeView)
    dialog.setContentView(root)

    // 标记是否已经成功产出，避免重复回调/早退
    var finished = false

    suspendCancellableCoroutine { cont ->
        // 防御：就算有系统/无障碍导致的 dismiss，也别崩
        dialog.setOnDismissListener {
            if (!finished && cont.isActive) {
                // 用户强行关闭或系统重建，安全地结束协程（不抛异常也行）
                cont.cancel(CancellationException("Dialog dismissed before capture"))
            }
        }

        // 显示后再监听预绘制，确保已 attach 到 Window
        dialog.show()

        // 使用 doOnPreDraw 更简洁（需要 core-ktx 1.7+）
        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                // 移除监听，避免多次回调
                if (root.viewTreeObserver.isAlive) {
                    root.viewTreeObserver.removeOnPreDrawListener(this)
                }

                try {
                    // 到这里已完成测量布局，尺寸非 0
                    val bmp = composeView.drawToBitmap(Bitmap.Config.ARGB_8888)

                    val file = File(
                        activity.cacheDir,
                        "${fileNamePrefix}${System.currentTimeMillis()}.png"
                    )
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.fileprovider",
                        file
                    )

                    finished = true
                    if (cont.isActive) cont.resume(uri) {}
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                } finally {
                    dialog.dismiss()
                }
                return true
            }
        })
    }
}

/**
 * 把缓存中的图片（content:// 或 file://）保存到系统相册（Pictures/Deadliner）。
 * 返回写入后的相册 Uri。
 */
suspend fun saveImageToGallery(
    context: Context,
    srcUri: Uri,
    displayNameWithoutExt: String = "Deadliner_${System.currentTimeMillis()}",
    subDir: String = "Deadliner"
): Uri {
    val resolver = context.contentResolver
    val fileName = "$displayNameWithoutExt.png"
    val mime = "image/png"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+：直接写 MediaStore
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            // 会出现在 “相册/图片/Deadliner”
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/$subDir")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val dst = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Insert MediaStore failed")
        resolver.openOutputStream(dst)?.use { out ->
            resolver.openInputStream(srcUri)?.use { `in` ->
                `in`.copyTo(out)
            } ?: error("Open srcUri failed")
        } ?: error("Open dstUri failed")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(dst, values, null, null)
        return dst
    } else {
        // Android 9 及以下：写到公共图片目录并触发媒体扫描
        @Suppress("DEPRECATION")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .resolve(subDir).apply { if (!exists()) mkdirs() }
        val outFile = File(dir, fileName)
        context.contentResolver.openInputStream(srcUri)?.use { `in` ->
            FileOutputStream(outFile).use { out -> `in`.copyTo(out) }
        } ?: error("Open srcUri failed")
        MediaScannerConnection.scanFile(
            context, arrayOf(outFile.absolutePath), arrayOf(mime), null
        )
        return Uri.fromFile(outFile)
    }
}

// 分享
fun shareImage(context: Activity, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享月度看板"))
}