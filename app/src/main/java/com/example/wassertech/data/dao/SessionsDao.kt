package com.example.wassertech.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.wassertech.data.entities.*

@Dao
interface SessionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(s: MaintenanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservations(list: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIssues(list: List<IssueEntity>)

    @Query("SELECT * FROM maintenance_sessions WHERE siteId = :siteId ORDER BY startedAtEpoch DESC")
    fun observeSessions(siteId: String): Flow<List<MaintenanceSessionEntity>>
}
