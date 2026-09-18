package com.yourname.netforge.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netforge_settings")

class SecurePrefs(private val context: Context) {

    companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("pref_dark_theme")
        val KEY_AUTO_CONNECT = booleanPreferencesKey("pref_auto_connect")
        val KEY_VERBOSE_LOGS = booleanPreferencesKey("pref_verbose_logs")
        val KEY_CUSTOM_DNS = booleanPreferencesKey("pref_custom_dns")
        val KEY_PRIMARY_DNS = stringPreferencesKey("pref_primary_dns")
        val KEY_SECONDARY_DNS = stringPreferencesKey("pref_secondary_dns")
        val KEY_ACTIVE_CONFIG_ID = longPreferencesKey("pref_active_config_id")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_THEME] ?: true }
    val isAutoConnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }
    val isVerboseLogs: Flow<Boolean> = context.dataStore.data.map { it[KEY_VERBOSE_LOGS] ?: true }
    val isCustomDns: Flow<Boolean> = context.dataStore.data.map { it[KEY_CUSTOM_DNS] ?: false }
    val primaryDns: Flow<String> = context.dataStore.data.map { it[KEY_PRIMARY_DNS] ?: "1.1.1.1" }
    val secondaryDns: Flow<String> = context.dataStore.data.map { it[KEY_SECONDARY_DNS] ?: "8.8.8.8" }
    val activeConfigId: Flow<Long?> = context.dataStore.data.map { it[KEY_ACTIVE_CONFIG_ID] }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CONNECT] = enabled }
    }

    suspend fun setVerboseLogs(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VERBOSE_LOGS] = enabled }
    }

    suspend fun setCustomDns(enabled: Boolean, primary: String, secondary: String) {
        context.dataStore.edit {
            it[KEY_CUSTOM_DNS] = enabled
            it[KEY_PRIMARY_DNS] = primary
            it[KEY_SECONDARY_DNS] = secondary
        }
    }

    suspend fun setActiveConfigId(id: Long?) {
        context.dataStore.edit {
            if (id != null) {
                it[KEY_ACTIVE_CONFIG_ID] = id
            } else {
                it.remove(KEY_ACTIVE_CONFIG_ID)
            }
        }
    }
}
