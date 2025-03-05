import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.aritxonly.deadliner.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ApkDownloaderInstaller(private val context: AppCompatActivity) {

    private var downloadId: Long = -1
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private var progressDialog: androidx.appcompat.app.AlertDialog? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private var isDownloading = true

    /**
     * 下载并安装 APK
     */
    fun downloadAndInstall(apkUrl: String, apkName: String = "update.apk") {
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName)

        // 删除旧的 APK
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("正在下载更新")
            setDescription("请稍候...")
            setDestinationUri(Uri.fromFile(apkFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType("application/vnd.android.package-archive")
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        }

        downloadId = downloadManager.enqueue(request)
        showProgressDialog()
        monitorDownloadProgress()
        registerDownloadReceiver(apkFile)
    }

    /**
     * 显示下载进度条对话框
     */
    private fun showProgressDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        progressBar = dialogView.findViewById(R.id.downloadProgressBar)
        progressText = dialogView.findViewById(R.id.downloadProgressText)

        progressDialog = MaterialAlertDialogBuilder(context)
            .setTitle("下载更新")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("取消") { _, _ ->
                downloadManager.remove(downloadId)
                isDownloading = false
            }
            .show()
    }

    /**
     * 监听下载进度
     */
    private fun monitorDownloadProgress() {
        Thread {
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (totalBytes > 0) {
                        val progress = (bytesDownloaded * 100L / totalBytes).toInt()
                        Handler(Looper.getMainLooper()).post {
                            progressBar.progress = progress
                            progressText.text = "下载进度: $progress%"
                        }
                    }
                }
                cursor.close()
                Thread.sleep(500) // 每 500ms 更新一次进度
            }
        }.start()
    }

    /**
     * 监听 APK 下载完成事件
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerDownloadReceiver(apkFile: File) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    isDownloading = false
                    progressDialog?.dismiss()
                    context.unregisterReceiver(this)
                    installApk(apkFile)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    /**
     * 安装 APK
     */
    private fun installApk(apkFile: File) {
        Log.d("ApkDownloaderInstaller", "开始安装 APK: ${apkFile.absolutePath}")

        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
        } else {
            context.startActivity(intent)
        }
    }
}