package com.example.delta3d.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenStore(private val context: Context) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")

        // 首次启动
        private val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    val accessTokenFlow: Flow<String?> =
        context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }

    // 默认为 true
    val isFirstLaunchFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[KEY_IS_FIRST_LAUNCH] ?: true
        }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[KEY_ACCESS_TOKEN] = token }
    }

    // 写入 false
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { it[KEY_IS_FIRST_LAUNCH] = false }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY_ACCESS_TOKEN) }
    }
}