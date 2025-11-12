package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    fun getValue(key: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getValueSync(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(settings: SettingsEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun delete(key: String)
}


