package com.reavann.miunlocker.data

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.reavann.miunlocker.automation.MiUnlockAccessibilityService
import com.reavann.miunlocker.scheduling.canScheduleExactAlarmsCompat

class SetupStatusRepository(context: Context) {
    private val appContext = context.applicationContext

    fun getSnapshot(): SetupStatusSnapshot {
        val exactAlarmRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val notificationRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        return SetupStatusSnapshot(
            accessibilityEnabled = isAccessibilityServiceEnabled(),
            exactAlarmPermissionRequired = exactAlarmRequired,
            exactAlarmAllowed = appContext.canScheduleExactAlarmsCompat(),
            batteryOptimizationIgnored = isIgnoringBatteryOptimizations(),
            notificationPermissionRequired = notificationRequired,
            notificationPermissionGranted = !notificationRequired || isNotificationPermissionGranted(),
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return MiUnlockAccessibilityService.isServiceActive()
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
}