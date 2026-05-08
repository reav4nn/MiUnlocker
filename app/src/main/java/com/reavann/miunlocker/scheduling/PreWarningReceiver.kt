package com.reavann.miunlocker.scheduling

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.reavann.miunlocker.MainActivity
import com.reavann.miunlocker.R

class PreWarningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PREWARNING) return

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        if (targetPackage.isBlank()) return

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        createNotificationChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val largeIcon = BitmapFactory.decodeResource(
            context.resources, R.drawable.miunlocker_logo
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle("Unlock reminder")
            .setContentText("Unlock your phone and keep the screen on. Automation starts in about 2 minutes.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Unlock your phone and keep the screen on. Automation starts in about 2 minutes."))
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Unlock reminder",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Audible reminder to unlock the phone before scheduled automation."
        }

        notificationManager(context).createNotificationChannel(channel)
    }

    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }

    companion object {
        const val ACTION_PREWARNING = "com.reavann.miunlocker.action.PREWARNING"
        const val EXTRA_TARGET_PACKAGE = "targetPackage"
        private const val CHANNEL_ID = "pre_warning"
        private const val NOTIFICATION_ID = 3003
        private const val REQUEST_CODE_OPEN_APP = 3004
    }
}
