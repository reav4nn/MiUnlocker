package com.reavann.miunlocker.data

data class SetupStatusSnapshot(
    val accessibilityEnabled: Boolean = false,
    val exactAlarmPermissionRequired: Boolean = false,
    val exactAlarmAllowed: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val notificationPermissionGranted: Boolean = true,
)
