
package com.example.wassertech.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.wassertech.data.entities.*

data class ComponentLastTsRow(
    val componentId: String,
    val ts: Long?
)

@Dao
interface SessionsDao {

    // Последнее время ТО по каждому компоненту (если нужно в будущем)
    @Query("SELECT o.componentId AS componentId, MAX(s.startedAtEpoch) AS ts FROM observations o JOIN maintenance_sessions s ON o.sessionId = s.id GROUP BY o.componentId")
    fun observeLastTsByComponent(): Flow<List<ComponentLastTsRow>>

    // История по сайту (есть в проекте — оставляем)
    @Query("SELECT * FROM maintenance_sessions WHERE siteId = :siteId ORDER BY startedAtEpoch DESC")
    fun observeSessions(siteId: String): Flow<List<MaintenanceSessionEntity>>

    // История по установке
    @Query("SELECT * FROM maintenance_sessions WHERE installationId = :installationId ORDER BY startedAtEpoch DESC")
    fun observeSessionsByInstallation(installationId: String): Flow<List<MaintenanceSessionEntity>>

    // Глобальная история
    @Query("SELECT * FROM maintenance_sessions ORDER BY startedAtEpoch DESC")
    fun observeAllSessions(): Flow<List<MaintenanceSessionEntity>>

    // Детали сессии
    @Query("SELECT * FROM observations WHERE sessionId = :sessionId")
    suspend fun getObservations(sessionId: String): List<ObservationEntity>

    // Upserts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(s: MaintenanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservations(list: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIssues(list: List<IssueEntity>)
}
