package com.reavann.miunlocker.ui

data class ScheduleUiState(
    val nextTapEpochMillis: Long? = null,
    val alarmTriggerEpochMillis: Long? = null,
    val schedulingError: String? = null,
)
