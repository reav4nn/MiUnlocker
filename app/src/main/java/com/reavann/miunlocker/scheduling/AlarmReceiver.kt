package com.reavann.miunlocker.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reavann.miunlocker.automation.TapForegroundService
import com.reavann.miunlocker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ExactAlarmScheduler.ACTION_START_TAP_WINDOW) return

        val targetTapEpochMillis = intent.getLongExtra(
            ExactAlarmScheduler.EXTRA_TARGET_TAP_EPOCH_MILLIS,
            0L,
        )
        val targetPackage = intent.getStringExtra(ExactAlarmScheduler.EXTRA_TARGET_PACKAGE).orEmpty()

        if (targetTapEpochMillis <= 0L || targetPackage.isBlank()) return

        TapForegroundService.start(
            context = context,
            targetTapEpochMillis = targetTapEpochMillis,
            targetPackage = targetPackage,
        )

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching {
                    val settings = SettingsRepository(context).getCurrentSettings()
                    if (settings.dailyEnabled && settings.targetPackage.isNotBlank()) {
                        ExactAlarmScheduler(context).scheduleNext(
                            settings = settings,
                            afterMillis = targetTapEpochMillis + 1L,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
