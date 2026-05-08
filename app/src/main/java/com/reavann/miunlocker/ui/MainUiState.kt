package com.reavann.miunlocker.ui

import com.reavann.miunlocker.data.AppSettings
import com.reavann.miunlocker.data.InstalledAppInfo
import com.reavann.miunlocker.data.SetupStatusSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isAppListLoading: Boolean = false,
    val manualPackageInput: String = "",
    val targetPackageError: String? = null,
    val setupStatus: SetupStatusSnapshot = SetupStatusSnapshot(),
    val scheduleState: ScheduleUiState = ScheduleUiState(),
) {
    val selectedTargetAppText: String
        get() {
            val packageName = settings.targetPackage
            if (packageName.isBlank()) return "Not selected"

            val label = installedApps.firstOrNull { app -> app.packageName == packageName }?.label
            return if (label.isNullOrBlank() || label == packageName) {
                packageName
            } else {
                "$label ($packageName)"
            }
        }

    val dailyAutomationText: String
        get() = when {
            scheduleState.schedulingError != null -> "Scheduling issue"
            settings.dailyEnabled && scheduleState.nextTapEpochMillis != null -> "Scheduled"
            settings.dailyEnabled -> "Enabled, waiting for schedule"
            else -> "Disabled"
        }

    val dailyAutomationSupportingText: String
        get() = when {
            settings.targetPackage.isBlank() -> "Select a target app before arming daily automation."
            !setupStatus.exactAlarmAllowed -> "Grant exact alarm permission before arming daily automation."
            scheduleState.schedulingError != null -> scheduleState.schedulingError
            settings.dailyEnabled -> "The exact alarm starts the foreground service, launches the target app, and sends the Accessibility tap command."
            else -> "Arming schedules the next exact alarm using the selected target app, offset, and saved tap ratios."
        }

    val canChangeDailyAutomation: Boolean
        get() = settings.dailyEnabled ||
            (settings.targetPackage.isNotBlank() && setupStatus.exactAlarmAllowed)

    val nextTapTimeText: String
        get() = scheduleState.nextTapEpochMillis?.formatBakuTime() ?: "Not scheduled"

    val foregroundStartTimeText: String
        get() = scheduleState.alarmTriggerEpochMillis?.formatBakuTime() ?: "Not scheduled"

    val accessibilityStatusText: String
        get() = if (setupStatus.accessibilityEnabled) "Enabled" else "Disabled"

    val exactAlarmStatusText: String
        get() = when {
            !setupStatus.exactAlarmPermissionRequired -> "Not required on this Android version"
            setupStatus.exactAlarmAllowed -> "Allowed"
            else -> "Needs permission"
        }

    val batteryOptimizationStatusText: String
        get() = if (setupStatus.batteryOptimizationIgnored) {
            "Unrestricted"
        } else {
            "May be optimized"
        }

    val notificationStatusText: String
        get() = when {
            !setupStatus.notificationPermissionRequired -> "Not required on this Android version"
            setupStatus.notificationPermissionGranted -> "Granted"
            else -> "Needs permission"
        }

    private fun Long.formatBakuTime(): String {
        return Instant.ofEpochMilli(this)
            .atZone(BAKU_ZONE)
            .format(SCHEDULE_FORMATTER)
    }

    private companion object {
        val BAKU_ZONE: ZoneId = ZoneId.of("Asia/Baku")
        val SCHEDULE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss.SSS z",
            Locale.US,
        )
    }
}
