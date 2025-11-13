package ru.wassertech.client.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Менеджер для управления оффлайн режимом приложения
 */
class OfflineModeManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_mode_prefs")
        private val OFFLINE_MODE_KEY = booleanPreferencesKey("offline_mode")
        
        @Volatile
        private var INSTANCE: OfflineModeManager? = null
        
        fun getInstance(context: Context): OfflineModeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineModeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val dataStore = context.dataStore
    
    /**
     * Проверить, включен ли оффлайн режим
     */
    suspend fun isOfflineMode(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[OFFLINE_MODE_KEY] ?: false
        }.first()
    }
    
    /**
     * Получить Flow оффлайн режима
     */
    fun observeOfflineMode(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[OFFLINE_MODE_KEY] ?: false
        }
    }
    
    /**
     * Включить оффлайн режим
     */
    suspend fun setOfflineMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[OFFLINE_MODE_KEY] = enabled
        }
    }
    
    /**
     * Синхронная проверка оффлайн режима (для совместимости)
     */
    fun isOfflineModeSync(): Boolean {
        return runBlocking {
            dataStore.data.map { preferences ->
                preferences[OFFLINE_MODE_KEY] ?: false
            }.first()
        }
    }
}

