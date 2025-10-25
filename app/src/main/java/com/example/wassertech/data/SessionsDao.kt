package com.example.wassertech.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionsDao {
    @Query("SELECT * FROM maintenance_sessions WHERE siteId=:siteId ORDER BY startedAtEpoch DESC")
    fun observeSessions(siteId: String): Flow<List<MaintenanceSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(s: MaintenanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservations(list: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIssues(list: List<IssueEntity>)

    @Transaction
    suspend fun saveSessionBundle(
        session: MaintenanceSessionEntity,
        observations: List<ObservationEntity>,
        issues: List<IssueEntity>
    ) {
        upsertSession(session)
        upsertObservations(observations)
        upsertIssues(issues)
    }
}
