package com.reavann.miunlocker.scheduling

data class ScheduledTap(
    val targetTapEpochMillis: Long,
    val alarmTriggerEpochMillis: Long,
    val preWarningEpochMillis: Long?,
    val targetPackage: String,
)
