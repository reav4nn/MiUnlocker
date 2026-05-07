package com.reavann.miunlocker.ui

import com.reavann.miunlocker.data.AppSettings
import com.reavann.miunlocker.data.InstalledAppInfo
import com.reavann.miunlocker.data.SetupStatusSnapshot

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isAppListLoading: Boolean = false,
    val manualPackageInput: String = "",
    val targetPackageError: String? = null,
    val setupStatus: SetupStatusSnapshot = SetupStatusSnapshot(),
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
        get() = if (settings.dailyEnabled) {
            "Enabled in settings only"
        } else {
            "Disabled"
        }

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
}
