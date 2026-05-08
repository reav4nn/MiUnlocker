package com.reavann.miunlocker.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val targetPackage: String = "",
    val scheduledTime: Long? = null,
    val actualTime: Long? = null,
    val deltaMillis: Long? = null,
    val nodeFound: Boolean? = null,
    val fallbackUsed: Boolean? = null,
    val resultTitle: String = "",
    val resultText: String = "",
) {
    val formattedTimestamp: String
        get() = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.of("Asia/Baku"))
            .format(LOG_FORMATTER)

    val eventTypeLabel: String
        get() = when (eventType) {
            EVENT_MANUAL_TEST -> "Manual test"
            EVENT_APP_LAUNCH -> "App launch"
            EVENT_PREPARATION -> "Preparation"
            EVENT_TAP_EXECUTION -> "Tap execution"
            else -> eventType
        }

    companion object {
        const val EVENT_MANUAL_TEST = "manual_test"
        const val EVENT_APP_LAUNCH = "app_launch"
        const val EVENT_PREPARATION = "preparation"
        const val EVENT_TAP_EXECUTION = "tap_execution"

        private val LOG_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US,
        )
    }
}
