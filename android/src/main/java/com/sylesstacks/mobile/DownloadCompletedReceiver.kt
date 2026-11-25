package com.sylesstacks.mobile

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.widget.Toast

/**
 * Opens a completed APK using Android's official package installer.
 * The installer always requires the device owner's confirmation.
 */
class DownloadCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId < 0L) return

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        var mimeType: String? = null
        var status = -1
        manager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
            }
        }

        if (status != DownloadManager.STATUS_SUCCESSFUL) return

        val uri: Uri = manager.getUriForDownloadedFile(downloadId) ?: return
        val apkFlag = context.getSharedPreferences(
            "sylesstacks_downloads",
            Context.MODE_PRIVATE
        )
        val isApk = mimeType.equals("application/vnd.android.package-archive", true) ||
            apkFlag.getBoolean(downloadId.toString(), false)

        if (!isApk) return
        apkFlag.edit().remove(downloadId.toString()).apply()

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(installIntent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "APK скачан. Откройте его из уведомления или папки Download.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
