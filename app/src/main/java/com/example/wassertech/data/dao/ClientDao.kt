package com.example.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    /* ---- Клиенты ---- */

    @Query("""
        SELECT * FROM clients
        WHERE (:includeArchived = 1) OR (isArchived IS NULL OR isArchived = 0)
        ORDER BY (archivedAtEpoch IS NULL) ASC, sortOrder ASC, name COLLATE NOCASE ASC
    """)
    fun observeClients(includeArchived: Boolean = false): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClient(id: String): ClientEntity?

    @Upsert
    suspend fun upsertClient(client: ClientEntity)

    @Query("UPDATE clients SET clientGroupId = :clientGroupId, updatedAtEpoch = :ts WHERE id = :clientId")
    suspend fun setClientGroup(clientId: String, clientGroupId: String?, ts: Long)

    @Query("UPDATE clients SET isArchived = 1, archivedAtEpoch = :ts, updatedAtEpoch = :ts WHERE id = :id")
    suspend fun archiveClient(id: String, ts: Long)

    @Query("UPDATE clients SET isArchived = 0, archivedAtEpoch = NULL, updatedAtEpoch = strftime('%s','now')*1000 WHERE id = :id")
    suspend fun restoreClient(id: String)

    /* ---- Группы ---- */

    // Только активные
    @Query("""
        SELECT * FROM client_groups
        WHERE (isArchived IS NULL OR isArchived = 0)
        ORDER BY (archivedAtEpoch IS NULL) ASC, sortOrder ASC, title COLLATE NOCASE ASC
    """)
    fun observeActiveGroups(): Flow<List<ClientGroupEntity>>

    // Все группы (и архив)
    @Query("""
        SELECT * FROM client_groups
        ORDER BY (archivedAtEpoch IS NULL) ASC, sortOrder ASC, title COLLATE NOCASE ASC
    """)
    fun observeAllGroups(): Flow<List<ClientGroupEntity>>

    @Upsert
    suspend fun upsertGroup(group: ClientGroupEntity)

    @Query("UPDATE client_groups SET title = :newTitle, updatedAtEpoch = :ts WHERE id = :id")
    suspend fun renameGroup(id: String, newTitle: String, ts: Long)

    @Query("UPDATE client_groups SET isArchived = 1, archivedAtEpoch = :ts, updatedAtEpoch = :ts WHERE id = :id")
    suspend fun archiveGroup(id: String, ts: Long)

    @Query("UPDATE client_groups SET isArchived = 0, archivedAtEpoch = NULL, updatedAtEpoch = strftime('%s','now')*1000 WHERE id = :id")
    suspend fun restoreGroup(id: String)

    // Каскад на клиентов при архивации/восстановлении группы
    @Query("UPDATE clients SET isArchived = 1, archivedAtEpoch = :ts, updatedAtEpoch = :ts WHERE clientGroupId = :groupId")
    suspend fun archiveClientsByGroup(groupId: String, ts: Long)

    @Query("UPDATE clients SET isArchived = 0, archivedAtEpoch = NULL, updatedAtEpoch = strftime('%s','now')*1000 WHERE clientGroupId = :groupId")
    suspend fun restoreClientsByGroup(groupId: String)

    @Transaction
    suspend fun archiveGroupCascade(groupId: String, ts: Long) {
        archiveGroup(groupId, ts)
        archiveClientsByGroup(groupId, ts)
    }

    @Transaction
    suspend fun restoreGroupCascade(groupId: String, ts: Long) {
        restoreGroup(groupId)
        restoreClientsByGroup(groupId)
    }
}
