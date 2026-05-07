package com.reavann.miunlocker.data

import java.util.Locale

data class AppSettings(
    val targetPackage: String = "",
    val targetHour: Int = DEFAULT_TARGET_HOUR,
    val targetMinute: Int = DEFAULT_TARGET_MINUTE,
    val targetSecond: Int = DEFAULT_TARGET_SECOND,
    val targetMillis: Int = DEFAULT_TARGET_MILLIS,
    val offsetMillis: Int = DEFAULT_OFFSET_MILLIS,
    val tapXRatio: Float = DEFAULT_TAP_X_RATIO,
    val tapYRatio: Float = DEFAULT_TAP_Y_RATIO,
    val dailyEnabled: Boolean = DEFAULT_DAILY_ENABLED,
) {
    val targetTimeText: String
        get() = String.format(
            Locale.US,
            "%02d:%02d:%02d.%03d",
            targetHour,
            targetMinute,
            targetSecond,
            targetMillis,
        )

    companion object {
        const val DEFAULT_TARGET_HOUR = 19
        const val DEFAULT_TARGET_MINUTE = 59
        const val DEFAULT_TARGET_SECOND = 59
        const val DEFAULT_TARGET_MILLIS = 800
        const val DEFAULT_OFFSET_MILLIS = 0
        const val DEFAULT_TAP_X_RATIO = 0.5f
        const val DEFAULT_TAP_Y_RATIO = 0.89f
        const val DEFAULT_DAILY_ENABLED = false

        val ALLOWED_OFFSETS_MILLIS = listOf(-500, -300, -200, 0, 50)
    }
}

fun formatSignedMillis(value: Int): String = when {
    value > 0 -> "+${value}ms"
    value < 0 -> "${value}ms"
    else -> "0ms"
}
