package com.reavann.miunlocker.data

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

class SetupStatusRepository(context: Context) {
    private val appContext = context.applicationContext

    fun getSnapshot(): SetupStatusSnapshot {
        val exactAlarmRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val notificationRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        return SetupStatusSnapshot(
            accessibilityEnabled = isAccessibilityServiceEnabled(),
            exactAlarmPermissionRequired = exactAlarmRequired,
            exactAlarmAllowed = !exactAlarmRequired || canScheduleExactAlarms(),
            batteryOptimizationIgnored = isIgnoringBatteryOptimizations(),
            notificationPermissionRequired = notificationRequired,
            notificationPermissionGranted = !notificationRequired || isNotificationPermissionGranted(),
        )
    }

    private fun canScheduleExactAlarms(): Boolean {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        return alarmManager?.canScheduleExactAlarms() == true
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == true
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val resolver = appContext.contentResolver
        val accessibilityEnabled = Settings.Secure.getInt(
            resolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1

        if (!accessibilityEnabled) return false

        val enabledServices = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        return enabledServices
            .split(':')
            .asSequence()
            .mapNotNull(ComponentName::unflattenFromString)
            .any { componentName ->
                componentName.packageName == appContext.packageName &&
                    componentName.className == ACCESSIBILITY_SERVICE_CLASS
            }
    }

    private companion object {
        const val ACCESSIBILITY_SERVICE_CLASS =
            "com.reavann.miunlocker.automation.MiUnlockAccessibilityService"
    }
}
