package com.reavann.miunlocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toAppSettings() }

    suspend fun getCurrentSettings(): AppSettings = settings.first()

    suspend fun setOffsetMillis(offsetMillis: Int) {
        val safeOffset = if (offsetMillis in AppSettings.ALLOWED_OFFSETS_MILLIS) {
            offsetMillis
        } else {
            AppSettings.DEFAULT_OFFSET_MILLIS
        }

        dataStore.edit { preferences ->
            preferences[Keys.OFFSET_MILLIS] = safeOffset
        }
    }

    suspend fun setDailyEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DAILY_ENABLED] = enabled
        }
    }

    suspend fun setTargetPackage(packageName: String): Boolean {
        val safePackageName = packageName.trim()

        if (safePackageName.isEmpty()) {
            dataStore.edit { preferences ->
                preferences.remove(Keys.TARGET_PACKAGE)
                preferences[Keys.TARGET_EXPLICITLY_CLEARED] = true
            }
            return true
        }

        if (!isValidPackageName(safePackageName) || !isInstalledLaunchablePackage(safePackageName)) {
            return false
        }

        dataStore.edit { preferences ->
            preferences[Keys.TARGET_PACKAGE] = safePackageName
            preferences[Keys.TARGET_EXPLICITLY_CLEARED] = false
        }
        return true
    }

    suspend fun setTargetTime(hour: Int, minute: Int, second: Int, millis: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.TARGET_HOUR] = hour.coerceIn(0, 23)
            preferences[Keys.TARGET_MINUTE] = minute.coerceIn(0, 59)
            preferences[Keys.TARGET_SECOND] = second.coerceIn(0, 59)
            preferences[Keys.TARGET_MILLIS] = millis.coerceIn(0, 999)
        }
    }

    suspend fun setTapRatios(xRatio: Float, yRatio: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.TAP_X_RATIO] = xRatio.safeRatio(AppSettings.DEFAULT_TAP_X_RATIO)
            preferences[Keys.TAP_Y_RATIO] = yRatio.safeRatio(AppSettings.DEFAULT_TAP_Y_RATIO)
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val rawOffset = this[Keys.OFFSET_MILLIS] ?: AppSettings.DEFAULT_OFFSET_MILLIS
        val safeOffset = if (rawOffset in AppSettings.ALLOWED_OFFSETS_MILLIS) {
            rawOffset
        } else {
            AppSettings.DEFAULT_OFFSET_MILLIS
        }

        val rawTargetPackage = this[Keys.TARGET_PACKAGE]
        val explicitlyCleared = this[Keys.TARGET_EXPLICITLY_CLEARED] == true

        val resolvedTargetPackage = when {
            rawTargetPackage != null -> rawTargetPackage
            explicitlyCleared -> ""
            else -> AppSettings.DEFAULT_TARGET_PACKAGE
        }

        return AppSettings(
            targetPackage = resolvedTargetPackage
                .trim()
                .takeIf(::isValidPackageName)
                ?.takeIf(::isInstalledLaunchablePackage)
                .orEmpty(),
            targetHour = (this[Keys.TARGET_HOUR] ?: AppSettings.DEFAULT_TARGET_HOUR).coerceIn(0, 23),
            targetMinute = (this[Keys.TARGET_MINUTE] ?: AppSettings.DEFAULT_TARGET_MINUTE).coerceIn(0, 59),
            targetSecond = (this[Keys.TARGET_SECOND] ?: AppSettings.DEFAULT_TARGET_SECOND).coerceIn(0, 59),
            targetMillis = (this[Keys.TARGET_MILLIS] ?: AppSettings.DEFAULT_TARGET_MILLIS).coerceIn(0, 999),
            offsetMillis = safeOffset,
            tapXRatio = (this[Keys.TAP_X_RATIO] ?: AppSettings.DEFAULT_TAP_X_RATIO)
                .safeRatio(AppSettings.DEFAULT_TAP_X_RATIO),
            tapYRatio = (this[Keys.TAP_Y_RATIO] ?: AppSettings.DEFAULT_TAP_Y_RATIO)
                .safeRatio(AppSettings.DEFAULT_TAP_Y_RATIO),
            dailyEnabled = this[Keys.DAILY_ENABLED] ?: AppSettings.DEFAULT_DAILY_ENABLED,
        )
    }

    private fun Float.safeRatio(defaultValue: Float): Float {
        return if (isNaN() || isInfinite()) {
            defaultValue
        } else {
            coerceIn(0f, 1f)
        }
    }

    private object Keys {
        val TARGET_PACKAGE = stringPreferencesKey("targetPackage")
        val TARGET_EXPLICITLY_CLEARED = booleanPreferencesKey("targetExplicitlyCleared")
        val TARGET_HOUR = intPreferencesKey("targetHour")
        val TARGET_MINUTE = intPreferencesKey("targetMinute")
        val TARGET_SECOND = intPreferencesKey("targetSecond")
        val TARGET_MILLIS = intPreferencesKey("targetMillis")
        val OFFSET_MILLIS = intPreferencesKey("offsetMillis")
        val TAP_X_RATIO = floatPreferencesKey("tapXRatio")
        val TAP_Y_RATIO = floatPreferencesKey("tapYRatio")
        val DAILY_ENABLED = booleanPreferencesKey("dailyEnabled")
    }

    private fun isValidPackageName(packageName: String): Boolean {
        return PACKAGE_NAME_PATTERN.matches(packageName)
    }

    private fun isInstalledLaunchablePackage(packageName: String): Boolean {
        return appContext.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private companion object {
        val PACKAGE_NAME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }
}
