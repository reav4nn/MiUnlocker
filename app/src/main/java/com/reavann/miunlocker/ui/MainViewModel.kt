package com.reavann.miunlocker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reavann.miunlocker.data.InstalledAppInfo
import com.reavann.miunlocker.data.InstalledAppsRepository
import com.reavann.miunlocker.data.SettingsRepository
import com.reavann.miunlocker.data.SetupStatusRepository
import com.reavann.miunlocker.data.SetupStatusSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val setupStatusRepository: SetupStatusRepository,
) : ViewModel() {
    private val appPickerState = MutableStateFlow(AppPickerState())
    private val setupStatus = MutableStateFlow(SetupStatusSnapshot())

    val uiState = combine(
        settingsRepository.settings,
        appPickerState,
        setupStatus,
    ) { settings, appPicker, setupStatus ->
        MainUiState(
            settings = settings,
            installedApps = appPicker.installedApps,
            isAppListLoading = appPicker.isAppListLoading,
            manualPackageInput = appPicker.manualPackageInput,
            targetPackageError = appPicker.targetPackageError,
            setupStatus = setupStatus,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MainUiState(),
        )

    init {
        refreshSetupStatus()
    }

    fun onOffsetSelected(offsetMillis: Int) {
        viewModelScope.launch {
            settingsRepository.setOffsetMillis(offsetMillis)
        }
    }

    fun onDailyEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDailyEnabled(enabled)
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            appPickerState.update { state -> state.copy(isAppListLoading = true) }
            val apps = withContext(Dispatchers.Default) {
                installedAppsRepository.getLaunchableApps()
            }
            appPickerState.update { state ->
                state.copy(
                    installedApps = apps,
                    isAppListLoading = false,
                )
            }
        }
    }

    fun refreshSetupStatus() {
        setupStatus.value = setupStatusRepository.getSnapshot()
    }

    fun onManualPackageInputChange(packageName: String) {
        appPickerState.update { state ->
            state.copy(
                manualPackageInput = packageName,
                targetPackageError = null,
            )
        }
    }

    fun onManualPackageSubmitted() {
        val packageName = appPickerState.value.manualPackageInput
        viewModelScope.launch {
            persistTargetPackage(packageName)
        }
    }

    fun onTargetPackageSelected(packageName: String) {
        viewModelScope.launch {
            persistTargetPackage(packageName)
        }
    }

    fun onTargetPackageCleared() {
        viewModelScope.launch {
            settingsRepository.setTargetPackage("")
            appPickerState.update { state ->
                state.copy(
                    manualPackageInput = "",
                    targetPackageError = null,
                )
            }
        }
    }

    private suspend fun persistTargetPackage(packageName: String) {
        val safePackageName = packageName.trim()
        val saved = settingsRepository.setTargetPackage(safePackageName)

        appPickerState.update { state ->
            if (saved) {
                state.copy(
                    manualPackageInput = safePackageName,
                    targetPackageError = null,
                )
            } else {
                state.copy(
                    targetPackageError = "Enter an installed, launchable package name such as com.example.app.",
                )
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext

            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        return MainViewModel(
                            settingsRepository = SettingsRepository(appContext),
                            installedAppsRepository = InstalledAppsRepository(appContext),
                            setupStatusRepository = SetupStatusRepository(appContext),
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    private data class AppPickerState(
        val installedApps: List<InstalledAppInfo> = emptyList(),
        val isAppListLoading: Boolean = false,
        val manualPackageInput: String = "",
        val targetPackageError: String? = null,
    )
}
