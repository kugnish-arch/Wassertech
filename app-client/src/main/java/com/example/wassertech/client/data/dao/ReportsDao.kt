package ru.wassertech.client.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.wassertech.client.data.entities.ReportEntity

/**
 * DAO для работы с отчётами.
 */
@Dao
interface ReportsDao {
    
    /**
     * Вставка или обновление отчётов (upsert).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReports(reports: List<ReportEntity>)
    
    /**
     * Вставка или обновление одного отчёта.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReport(report: ReportEntity)
    
    /**
     * Получить все неархивные отчёты для указанного клиента.
     */
    @Query("""
        SELECT * FROM reports 
        WHERE clientId = :clientId AND isArchived = 0
        ORDER BY createdAtEpoch DESC
    """)
    fun observeReportsForClient(clientId: String): Flow<List<ReportEntity>>
    
    /**
     * Получить все неархивные отчёты для указанного клиента (suspend).
     */
    @Query("""
        SELECT * FROM reports 
        WHERE clientId = :clientId AND isArchived = 0
        ORDER BY createdAtEpoch DESC
    """)
    suspend fun getReportsForClient(clientId: String): List<ReportEntity>
    
    /**
     * Получить максимальный updatedAtEpoch для указанного клиента.
     * Используется для инкрементальной синхронизации.
     */
    @Query("""
        SELECT MAX(updatedAtEpoch) FROM reports 
        WHERE clientId = :clientId AND isArchived = 0
    """)
    suspend fun getMaxUpdatedAtEpoch(clientId: String): Long?
    
    /**
     * Получить отчёты, которые нужно скачать (не скачаны и не архивированы).
     */
    @Query("""
        SELECT * FROM reports 
        WHERE clientId = :clientId 
        AND isArchived = 0 
        AND isDownloaded = 0
        AND fileUrl IS NOT NULL
        ORDER BY createdAtEpoch ASC
    """)
    suspend fun getReportsToDownload(clientId: String): List<ReportEntity>
    
    /**
     * Получить отчёт по ID.
     */
    @Query("SELECT * FROM reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): ReportEntity?
    
    /**
     * Обновить локальный путь к файлу и флаг скачивания.
     */
    @Query("""
        UPDATE reports 
        SET localFilePath = :localFilePath, isDownloaded = 1 
        WHERE id = :reportId
    """)
    suspend fun updateReportDownloadStatus(reportId: String, localFilePath: String)
}


