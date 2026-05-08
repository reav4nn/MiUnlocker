package com.reavann.miunlocker.ui

import com.reavann.miunlocker.data.AppSettings
import com.reavann.miunlocker.data.InstalledAppInfo
import com.reavann.miunlocker.data.LogEntry
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
    val manualTestState: ManualTestUiState = ManualTestUiState(),
    val logsState: LogsUiState = LogsUiState(),
) {
    val selectedTargetAppText: String
        get() {
            val packageName = settings.targetPackage
            if (packageName.isBlank()) return "Not selected"

            val label = installedApps.firstOrNull { app -> app.packageName == packageName }?.label
            if (label.isNullOrBlank() && packageName == AppSettings.DEFAULT_TARGET_PACKAGE) {
                return "Xiaomi Community ($packageName)"
            }

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
            settings.dailyEnabled -> "The exact alarm starts the foreground service, prepares the unlock page, and sends the Accessibility tap command at tap time."
            else -> "Arming schedules the next exact alarm using the selected target app, offset, and saved tap ratios."
        }

    val canChangeDailyAutomation: Boolean
        get() = settings.dailyEnabled ||
            (settings.targetPackage.isNotBlank() && setupStatus.exactAlarmAllowed)

    val canRunManualTest: Boolean
        get() = settings.targetPackage.isNotBlank() &&
            setupStatus.accessibilityEnabled &&
            !manualTestState.isRunning

    val manualTestButtonText: String
        get() = if (manualTestState.isRunning) "Testing..." else "Test now"

    val manualTestSupportingText: String
        get() = when {
            settings.targetPackage.isBlank() -> "Select a target app before running a manual test."
            !setupStatus.accessibilityEnabled -> "Enable the MiUnlocker tap service before running a manual test."
            manualTestState.isRunning -> "Opening the target app, dismissing the prompt, opening ME, and waiting for Unlock bootloader/Apply."
            manualTestState.resultTitle != null -> "${manualTestState.resultTitle}: ${manualTestState.resultText.orEmpty()}"
            else -> "Launches the selected app, prepares the unlock page, then sends the saved Accessibility tap command."
        }

    val nextTapTimeText: String
        get() = scheduleState.nextTapEpochMillis?.formatBakuTime() ?: "Not scheduled"

    val foregroundStartTimeText: String
        get() = scheduleState.alarmTriggerEpochMillis?.formatBakuTime() ?: "Not scheduled"

    val preWarningTimeText: String
        get() = scheduleState.preWarningEpochMillis?.formatBakuTime() ?: "Not scheduled"

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

data class ManualTestUiState(
    val isRunning: Boolean = false,
    val resultTitle: String? = null,
    val resultText: String? = null,
)

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
)
