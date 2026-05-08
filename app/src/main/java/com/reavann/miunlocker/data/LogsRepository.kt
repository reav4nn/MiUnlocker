package com.reavann.miunlocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.logsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "logs",
)

class LogsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.logsDataStore

    val logs: Flow<List<LogEntry>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            parseLogsJson(preferences[Keys.LOGS] ?: "[]")
        }

    suspend fun getLogEntries(): List<LogEntry> = logs.first()

    suspend fun addLogEntry(entry: LogEntry) {
        dataStore.edit { preferences ->
            val current = parseLogsJson(preferences[Keys.LOGS] ?: "[]")
            val updated = (current + entry).takeLast(MAX_LOG_ENTRIES)
            preferences[Keys.LOGS] = serializeLogsJson(updated)
        }
    }

    suspend fun clearLogs() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.LOGS)
        }
    }

    private fun parseLogsJson(json: String): List<LogEntry> {
        return runCatching {
            val array = JSONArray(json)
            val entries = mutableListOf<LogEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entries.add(
                    LogEntry(
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        eventType = obj.optString("eventType", ""),
                        targetPackage = obj.optString("targetPackage", ""),
                        scheduledTime = obj.optLong("scheduledTime", -1L).takeIf { it >= 0 },
                        actualTime = obj.optLong("actualTime", -1L).takeIf { it >= 0 },
                        deltaMillis = obj.optLong("deltaMillis", Long.MIN_VALUE)
                            .takeIf { it != Long.MIN_VALUE },
                        nodeFound = if (obj.has("nodeFound")) obj.optBoolean("nodeFound") else null,
                        fallbackUsed = if (obj.has("fallbackUsed")) obj.optBoolean("fallbackUsed") else null,
                        resultTitle = obj.optString("resultTitle", ""),
                        resultText = obj.optString("resultText", ""),
                    ),
                )
            }
            entries
        }.getOrDefault(emptyList())
    }

    private fun serializeLogsJson(entries: List<LogEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("timestamp", entry.timestamp)
            obj.put("eventType", entry.eventType)
            obj.put("targetPackage", entry.targetPackage)
            entry.scheduledTime?.let { obj.put("scheduledTime", it) }
            entry.actualTime?.let { obj.put("actualTime", it) }
            entry.deltaMillis?.let { obj.put("deltaMillis", it) }
            entry.nodeFound?.let { obj.put("nodeFound", it) }
            entry.fallbackUsed?.let { obj.put("fallbackUsed", it) }
            obj.put("resultTitle", entry.resultTitle)
            obj.put("resultText", entry.resultText)
            array.put(obj)
        }
        return array.toString()
    }

    private object Keys {
        val LOGS = stringPreferencesKey("logs")
    }

    private companion object {
        const val MAX_LOG_ENTRIES = 100
    }
}
