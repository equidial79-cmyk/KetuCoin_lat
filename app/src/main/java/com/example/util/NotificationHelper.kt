package com.example.util

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.AppUpdateInfo

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "ketucoin_updates_channel"
    private const val CHANNEL_NAME = "App Updates & Releases"
    private const val CHANNEL_DESC = "Notifications for new KetuCoin application versions and security updates"
    private const val NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showUpdateNotification(context: Context, update: AppUpdateInfo) {
        try {
            createNotificationChannel(context)

            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted; notification skipped.")
                    return
                }
            }

            // Target intent: either to app or download URL
            val intent = if (update.downloadUrl.isNotBlank() && update.downloadUrl.startsWith("http")) {
                Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_OPEN_UPDATE", true)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notesSnippet = if (update.releaseNotes.isNotBlank()) {
                update.releaseNotes.take(160)
            } else {
                "New performance optimizations and OTC updates."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("🚀 KetuCoin Update Available: ${update.versionName}")
                .setContentText("Version ${update.versionName} is now ready to download and install.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Version ${update.versionName} (Build ${update.versionCode}) is available!\n\nWhat's New:\n$notesSnippet\n\nTap to download and install the latest release.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Update notification displayed for version ${update.versionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show update notification", e)
        }
    }
}
