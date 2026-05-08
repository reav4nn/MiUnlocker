package com.reavann.miunlocker.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.reavann.miunlocker.data.AppSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Context.canScheduleExactAlarmsCompat(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
}

class ExactAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return appContext.canScheduleExactAlarmsCompat()
    }

    fun previewNext(
        settings: AppSettings,
        afterMillis: Long = System.currentTimeMillis(),
    ): ScheduledTap? {
        val targetPackage = settings.targetPackage.takeIf { packageName -> packageName.isNotBlank() }
            ?: return null
        val targetTapMillis = calculateNextTargetTapMillis(settings, afterMillis)

        return ScheduledTap(
            targetTapEpochMillis = targetTapMillis,
            alarmTriggerEpochMillis = targetTapMillis - PREPARATION_LEAD_MILLIS,
            targetPackage = targetPackage,
        )
    }

    fun scheduleNext(
        settings: AppSettings,
        afterMillis: Long = System.currentTimeMillis(),
    ): ScheduleResult {
        val scheduledTap = previewNext(settings, afterMillis)
            ?: return ScheduleResult.MissingTargetPackage

        if (!canScheduleExactAlarms()) {
            return ScheduleResult.ExactAlarmPermissionMissing
        }

        val alarmManager = alarmManager
            ?: return ScheduleResult.Failed("AlarmManager is unavailable on this device.")

        return runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                scheduledTap.alarmTriggerEpochMillis,
                createPendingIntent(scheduledTap),
            )
            ScheduleResult.Scheduled(scheduledTap)
        }.getOrElse { throwable ->
            ScheduleResult.Failed(throwable.message ?: "Failed to schedule exact alarm.")
        }
    }

    fun cancel() {
        alarmManager?.cancel(createPendingIntent())
    }

    private fun calculateNextTargetTapMillis(settings: AppSettings, afterMillis: Long): Long {
        val afterDate = Instant.ofEpochMilli(afterMillis)
            .atZone(BAKU_ZONE)
            .toLocalDate()
        var candidate = targetTapMillisForDate(settings, afterDate)

        if (candidate <= afterMillis) {
            candidate = targetTapMillisForDate(settings, afterDate.plusDays(1))
        }

        return candidate
    }

    private fun targetTapMillisForDate(settings: AppSettings, date: LocalDate): Long {
        val baseTargetMillis = date
            .atTime(
                settings.targetHour,
                settings.targetMinute,
                settings.targetSecond,
                settings.targetMillis * NANOS_PER_MILLI,
            )
            .atZone(BAKU_ZONE)
            .toInstant()
            .toEpochMilli()

        return baseTargetMillis + settings.offsetMillis
    }

    private fun createPendingIntent(scheduledTap: ScheduledTap? = null): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = ACTION_START_TAP_WINDOW
            scheduledTap?.let { tap ->
                putExtra(EXTRA_TARGET_TAP_EPOCH_MILLIS, tap.targetTapEpochMillis)
                putExtra(EXTRA_TARGET_PACKAGE, tap.targetPackage)
            }
        }

        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_DAILY_TAP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_START_TAP_WINDOW = "com.reavann.miunlocker.action.START_TAP_WINDOW"
        const val EXTRA_TARGET_TAP_EPOCH_MILLIS = "targetTapEpochMillis"
        const val EXTRA_TARGET_PACKAGE = "targetPackage"
        const val PREPARATION_LEAD_MILLIS = 120_000L

        private const val REQUEST_CODE_DAILY_TAP = 2001
        private const val NANOS_PER_MILLI = 1_000_000
        private val BAKU_ZONE = ZoneId.of("Asia/Baku")
    }
}
