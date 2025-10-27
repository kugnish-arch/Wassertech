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

    @Query("""
        SELECT o.componentId AS componentId, MAX(s.startedAtEpoch) AS ts
        FROM observations o
        JOIN maintenance_sessions s ON o.sessionId = s.id
        WHERE s.installationId = :installationId
        GROUP BY o.componentId
    """)
    suspend fun getLastTimestampsByComponent(installationId: String): List<ComponentLastTsRow>

    @Query("SELECT * FROM maintenance_sessions WHERE installationId = :installationId ORDER BY startedAtEpoch DESC")
    fun observeSessionsByInstallation(installationId: String): Flow<List<MaintenanceSessionEntity>>

    @Query("SELECT * FROM observations WHERE sessionId = :sessionId ORDER BY rowid")
    suspend fun getObservations(sessionId: String): List<ObservationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(s: MaintenanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservations(list: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIssues(list: List<IssueEntity>)

    @Query("SELECT * FROM maintenance_sessions WHERE siteId = :siteId ORDER BY startedAtEpoch DESC")
    fun observeSessions(siteId: String): Flow<List<MaintenanceSessionEntity>>
}
