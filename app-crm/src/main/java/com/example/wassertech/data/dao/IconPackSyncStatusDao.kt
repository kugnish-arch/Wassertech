package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.IconPackSyncStatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы со статусом синхронизации икон-паков.
 */
@Dao
interface IconPackSyncStatusDao {
    
    /**
     * Получить статус по ID пака.
     */
    @Query("SELECT * FROM icon_pack_sync_status WHERE packId = :packId LIMIT 1")
    suspend fun getByPackId(packId: String): IconPackSyncStatusEntity?
    
    /**
     * Получить статус по ID пака (Flow).
     */
    @Query("SELECT * FROM icon_pack_sync_status WHERE packId = :packId LIMIT 1")
    fun observeByPackId(packId: String): Flow<IconPackSyncStatusEntity?>
    
    /**
     * Получить все статусы.
     */
    @Query("SELECT * FROM icon_pack_sync_status")
    suspend fun getAll(): List<IconPackSyncStatusEntity>
    
    /**
     * Получить все статусы (Flow).
     */
    @Query("SELECT * FROM icon_pack_sync_status")
    fun observeAll(): Flow<List<IconPackSyncStatusEntity>>
    
    /**
     * Получить все загруженные паки.
     */
    @Query("SELECT * FROM icon_pack_sync_status WHERE isDownloaded = 1")
    suspend fun getDownloadedPacks(): List<IconPackSyncStatusEntity>
    
    /**
     * Вставить или обновить статус.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: IconPackSyncStatusEntity)
    
    /**
     * Вставить или обновить несколько статусов.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(statuses: List<IconPackSyncStatusEntity>)
    
    /**
     * Обновить статус загрузки пака.
     */
    @Query("UPDATE icon_pack_sync_status SET isDownloaded = :isDownloaded, downloadedIcons = :downloadedIcons, totalIcons = :totalIcons, lastSyncEpoch = :lastSyncEpoch WHERE packId = :packId")
    suspend fun updateDownloadStatus(
        packId: String,
        isDownloaded: Boolean,
        downloadedIcons: Int,
        totalIcons: Int,
        lastSyncEpoch: Long
    )
    
    /**
     * Удалить статус по ID пака.
     */
    @Query("DELETE FROM icon_pack_sync_status WHERE packId = :packId")
    suspend fun deleteByPackId(packId: String)
}




