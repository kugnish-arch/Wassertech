// app/src/main/java/com/example/wassertech/data/dao/SessionsDao.kt
package com.example.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.wassertech.data.entities.IssueEntity
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import com.example.wassertech.data.entities.MaintenanceValueEntity
import com.example.wassertech.data.entities.ObservationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Вспомогательная строка: последнее время ТО по каждому компоненту.
 * (Сохраняем твою исходную выборку по таблице observations)
 */
data class ComponentLastTsRow(
    val componentId: String,
    val ts: Long?
)

@Dao
interface SessionsDao {

    // --- Агрегации/обзоры ---

    /** Последнее время ТО по каждому компоненту (на основе observations + maintenance_sessions) */
    @Query(
        """
        SELECT o.componentId AS componentId, MAX(s.startedAtEpoch) AS ts
        FROM observations o
        JOIN maintenance_sessions s ON o.sessionId = s.id
        GROUP BY o.componentId
        """
    )
    fun observeLastTsByComponent(): Flow<List<ComponentLastTsRow>>

    /** История по сайту */
    @Query(
        """
        SELECT * FROM maintenance_sessions
        WHERE siteId = :siteId
        ORDER BY startedAtEpoch DESC
        """
    )
    fun observeSessions(siteId: String): Flow<List<MaintenanceSessionEntity>>

    /** История по установке */
    @Query(
        """
        SELECT * FROM maintenance_sessions
        WHERE installationId = :installationId
        ORDER BY startedAtEpoch DESC
        """
    )
    fun observeSessionsByInstallation(installationId: String): Flow<List<MaintenanceSessionEntity>>

    /** Глобальная история */
    @Query(
        """
        SELECT * FROM maintenance_sessions
        ORDER BY startedAtEpoch DESC
        """
    )
    fun observeAllSessions(): Flow<List<MaintenanceSessionEntity>>

    /** Наблюдение одной сессии (удобно для детального экрана, если понадобится) */
    @Query("SELECT * FROM maintenance_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSession(sessionId: String): Flow<MaintenanceSessionEntity?>

    // --- Детали сессии (твоё существующее) ---

    /** Детали наблюдений старого формата (observations) */
    @Query("SELECT * FROM observations WHERE sessionId = :sessionId")
    suspend fun getObservations(sessionId: String): List<ObservationEntity>

    // --- Значения полей ТО нового формата (maintenance_values) ---

    /** Все значения для конкретной сессии */
    @Query("SELECT * FROM maintenance_values WHERE sessionId = :sessionId")
    suspend fun getValuesForSession(sessionId: String): List<MaintenanceValueEntity>

    /** Массовая вставка значений */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValues(values: List<MaintenanceValueEntity>)

    // --- Upserts для сессий/старых сущностей ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(s: MaintenanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservations(list: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIssues(list: List<IssueEntity>)

    @Query("SELECT * FROM maintenance_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): MaintenanceSessionEntity?

    // --- Транзакция: сессия + значения ---

    /**
     * Атомарно сохранить сессию ТО и все её значения.
     * Если используешь новый формат (maintenance_values), достаточно звать этот метод.
     */
    @Transaction
    suspend fun insertSessionWithValues(
        session: MaintenanceSessionEntity,
        values: List<MaintenanceValueEntity>
    ) {
        upsertSession(session)
        if (values.isNotEmpty()) {
            insertValues(values)
        }
    }


}
