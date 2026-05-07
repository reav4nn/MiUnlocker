package com.reavann.miunlocker.scheduling

sealed interface ScheduleResult {
    data class Scheduled(val scheduledTap: ScheduledTap) : ScheduleResult
    data object MissingTargetPackage : ScheduleResult
    data object ExactAlarmPermissionMissing : ScheduleResult
    data class Failed(val message: String) : ScheduleResult
}
