package ru.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.wassertech.data.entities.ReportEntity

/**
 * DAO для работы с отчётами в app-crm.
 */
@Dao
interface ReportDao {
    
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
     * Получить все неархивные отчёты для указанной установки.
     */
    @Query("""
        SELECT * FROM reports 
        WHERE installationId = :installationId AND isArchived = 0
        ORDER BY createdAtEpoch DESC
    """)
    fun observeReportsForInstallation(installationId: String): Flow<List<ReportEntity>>
    
    /**
     * Получить все неархивные отчёты для указанной установки (suspend).
     */
    @Query("""
        SELECT * FROM reports 
        WHERE installationId = :installationId AND isArchived = 0
        ORDER BY createdAtEpoch DESC
    """)
    suspend fun getReportsForInstallation(installationId: String): List<ReportEntity>
    
    /**
     * Получить все неархивные отчёты для указанной сессии ТО.
     */
    @Query("""
        SELECT * FROM reports 
        WHERE sessionId = :sessionId AND isArchived = 0
        ORDER BY createdAtEpoch DESC
    """)
    suspend fun getReportsForSession(sessionId: String): List<ReportEntity>
    
    /**
     * Получить все неархивные отчёты для указанного клиента.
     */
    @Query("""
        SELECT * FROM reports 
        WHERE clientId = :clientId AND isArchived = 0
        ORDER BY createdAtEpoch DESC
    """)
    suspend fun getReportsForClient(clientId: String): List<ReportEntity>
    
    /**
     * Получить отчёты, обновлённые после указанного timestamp.
     * Используется для инкрементальной синхронизации.
     */
    @Query("""
        SELECT * FROM reports 
        WHERE updatedAtEpoch > :updatedAtEpoch OR (updatedAtEpoch IS NULL AND createdAtEpoch > :updatedAtEpoch)
        ORDER BY updatedAtEpoch ASC, createdAtEpoch ASC
    """)
    suspend fun getReportsUpdatedAfter(updatedAtEpoch: Long): List<ReportEntity>
    
    /**
     * Получить максимальный updatedAtEpoch среди всех отчётов.
     * Используется для инкрементальной синхронизации.
     */
    @Query("""
        SELECT MAX(COALESCE(updatedAtEpoch, createdAtEpoch)) FROM reports
    """)
    suspend fun getMaxUpdatedAtEpoch(): Long?
    
    /**
     * Получить отчёт по ID.
     */
    @Query("SELECT * FROM reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): ReportEntity?
}




