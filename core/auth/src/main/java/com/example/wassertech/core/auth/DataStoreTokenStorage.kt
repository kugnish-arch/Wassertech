package ru.wassertech.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.wassertech.core.network.TokenStorage

/**
 * Реализация TokenStorage с использованием DataStore
 */
class DataStoreTokenStorage(private val context: Context) : TokenStorage {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
    
    private val dataStore = context.dataStore
    
    override fun getAccessToken(): String? {
        return try {
            // Используем runBlocking для синхронного доступа (для совместимости с интерфейсом)
            kotlinx.coroutines.runBlocking {
                dataStore.data.first()[ACCESS_TOKEN_KEY]
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun saveAccessToken(token: String) {
        kotlinx.coroutines.runBlocking {
            dataStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = token
            }
        }
    }
    
    override fun getRefreshToken(): String? {
        return try {
            kotlinx.coroutines.runBlocking {
                dataStore.data.first()[REFRESH_TOKEN_KEY]
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun saveRefreshToken(token: String) {
        kotlinx.coroutines.runBlocking {
            dataStore.edit { preferences ->
                preferences[REFRESH_TOKEN_KEY] = token
            }
        }
    }
    
    override fun clearTokens() {
        kotlinx.coroutines.runBlocking {
            dataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN_KEY)
                preferences.remove(REFRESH_TOKEN_KEY)
            }
        }
    }
    
    /**
     * Получить токен как Flow (для реактивного доступа)
     */
    fun getAccessTokenFlow(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }
    
    /**
     * Сохранить токен асинхронно
     */
    suspend fun saveAccessTokenAsync(token: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }
    
    /**
     * Очистить токены асинхронно
     */
    suspend fun clearTokensAsync() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}

