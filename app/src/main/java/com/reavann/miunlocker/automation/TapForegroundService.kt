package com.reavann.miunlocker.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.reavann.miunlocker.MainActivity
import com.reavann.miunlocker.R
import com.reavann.miunlocker.data.SettingsRepository
import com.reavann.miunlocker.scheduling.ExactAlarmScheduler
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TapForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var targetTapEpochMillis: Long = 0L
    private var targetPackage: String = ""
    private var launchAttempted = false
    private var launchStatusText = "Opening selected target app."
    private var preparationStatusText = "Waiting to prepare unlock page."
    private var tapCommandSent = false
    private val preparationLock = Any()
    private var preparationJob: Job? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateCountdownNotification()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START_COUNTDOWN) {
            startFallbackForegroundAndStop()
            return START_NOT_STICKY
        }

        targetTapEpochMillis = intent.getLongExtra(EXTRA_TARGET_TAP_EPOCH_MILLIS, 0L)
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        launchAttempted = false
        launchStatusText = "Opening selected target app."
        preparationStatusText = "Waiting to prepare unlock page."
        tapCommandSent = false
        synchronized(preparationLock) {
            preparationJob?.cancel()
            preparationJob = null
        }

        if (targetTapEpochMillis <= 0L || targetPackage.isBlank()) {
            startFallbackForegroundAndStop()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title = "Auto tap armed",
                text = "Preparing selected target app.",
                ongoing = true,
            ),
        )

        launchTargetAppOnce()
        prepareUnlockPageOnce()
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        synchronized(preparationLock) { preparationJob?.cancel() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startFallbackForegroundAndStop() {
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title = "Auto tap cancelled",
                text = "Invalid scheduling parameters.",
                ongoing = false,
            ),
        )
        stopSelf()
    }

    private fun updateCountdownNotification() {
        val remainingMillis = targetTapEpochMillis - System.currentTimeMillis()

        if (remainingMillis <= 0L) {
            sendTapCommandOnce()
            return
        }

        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = "Auto tap armed",
                text = "$launchStatusText $preparationStatusText ${formatRemaining(remainingMillis)} until target time.",
                ongoing = true,
            ),
        )

        handler.postDelayed(
            tickRunnable,
            max(MIN_TICK_MILLIS, min(MAX_TICK_MILLIS, remainingMillis)),
        )
    }

    private fun launchTargetAppOnce() {
        if (launchAttempted) return
        launchAttempted = true

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launchIntent == null) {
            launchStatusText = "Target app could not be opened."
            return
        }

        runCatching {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launchIntent)
        }.onSuccess {
            launchStatusText = "Selected target app opened."
        }.onFailure {
            launchStatusText = "Target app launch failed."
        }
    }

    private fun prepareUnlockPageOnce() {
        synchronized(preparationLock) {
            if (preparationJob != null) return

            preparationJob = serviceScope.launch {
                preparationStatusText = "Preparing Xiaomi Community unlock page."
                val timeoutMillis = (targetTapEpochMillis - System.currentTimeMillis() - FINAL_TAP_GUARD_MILLIS)
                    .coerceAtLeast(0L)
                val result = MiUnlockAccessibilityService.prepareUnlockPageCommand(
                    targetPackage = targetPackage,
                    timeoutMillis = timeoutMillis,
                )
                preparationStatusText = when {
                    result.readyForFinalTap -> "Unlock page ready."
                    else -> "Preparation result: ${result.title}."
                }
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        title = "Auto tap armed",
                        text = preparationStatusText,
                        ongoing = true,
                    ),
                )
            }
        }
    }

    private fun sendTapCommandOnce() {
        if (tapCommandSent) return
        tapCommandSent = true
        synchronized(preparationLock) { preparationJob?.cancel() }
        handler.removeCallbacks(tickRunnable)

        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = "Tap time reached",
                text = "Sending Accessibility tap command.",
                ongoing = true,
            ),
        )

        serviceScope.launch {
            val result = buildTapCommandResult()
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(
                    title = result.title,
                    text = result.text,
                    ongoing = false,
                ),
            )
            handler.postDelayed({ stopSelf() }, STOP_AFTER_TARGET_MILLIS)
        }
    }

    private suspend fun buildTapCommandResult(): TapExecutionResult {
        val settings = withContext(Dispatchers.IO) {
            SettingsRepository(this@TapForegroundService).getCurrentSettings()
        }

        if (!settings.dailyEnabled) {
            return TapExecutionResult(
                title = "Tap skipped",
                text = "Daily automation was disabled before tap time.",
            )
        }

        if (settings.targetPackage != targetPackage) {
            return TapExecutionResult(
                title = "Tap skipped",
                text = "Selected target changed before tap time.",
            )
        }

        val currentScheduledTap = ExactAlarmScheduler(this).previewNext(
            settings = settings,
            afterMillis = targetTapEpochMillis - 1L,
        )
        if (currentScheduledTap?.targetTapEpochMillis != targetTapEpochMillis) {
            return TapExecutionResult(
                title = "Tap skipped",
                text = "Scheduled tap time changed before execution.",
            )
        }

        return MiUnlockAccessibilityService.executeTapCommand(
            targetPackage = settings.targetPackage,
            xRatio = settings.tapXRatio,
            yRatio = settings.tapYRatio,
        )
    }

    private fun buildNotification(
        title: String,
        text: String,
        ongoing: Boolean,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto tap countdown",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Foreground countdown before the configured tap time."
        }

        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    companion object {
        private const val ACTION_START_COUNTDOWN = "com.reavann.miunlocker.action.START_COUNTDOWN"
        private const val EXTRA_TARGET_TAP_EPOCH_MILLIS = "targetTapEpochMillis"
        private const val EXTRA_TARGET_PACKAGE = "targetPackage"
        private const val CHANNEL_ID = "auto_tap_countdown"
        private const val NOTIFICATION_ID = 3001
        private const val REQUEST_CODE_OPEN_APP = 3002
        private const val MAX_TICK_MILLIS = 1_000L
        private const val MIN_TICK_MILLIS = 50L
        private const val STOP_AFTER_TARGET_MILLIS = 5_000L
        private const val FINAL_TAP_GUARD_MILLIS = 800L

        fun start(
            context: Context,
            targetTapEpochMillis: Long,
            targetPackage: String,
        ) {
            val intent = Intent(context, TapForegroundService::class.java).apply {
                action = ACTION_START_COUNTDOWN
                putExtra(EXTRA_TARGET_TAP_EPOCH_MILLIS, targetTapEpochMillis)
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        private fun formatRemaining(remainingMillis: Long): String {
            val seconds = remainingMillis / 1_000L
            val millis = remainingMillis % 1_000L

            return String.format(Locale.US, "%d.%03ds", seconds, millis)
        }
    }
}
