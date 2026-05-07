package com.reavann.miunlocker.ui

import com.reavann.miunlocker.data.AppSettings

data class MainUiState(
    val settings: AppSettings = AppSettings(),
) {
    val selectedTargetAppText: String
        get() = settings.targetPackage.ifBlank { "Not selected" }

    val dailyAutomationText: String
        get() = if (settings.dailyEnabled) {
            "Enabled in settings only"
        } else {
            "Disabled"
        }
}
