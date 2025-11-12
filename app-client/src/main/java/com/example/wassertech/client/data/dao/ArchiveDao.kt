package ru.wassertech.client.data.dao

import androidx.room.Dao
import androidx.room.Query
import ru.wassertech.client.data.entities.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {

    @Query("""
        SELECT * FROM clients
        WHERE isArchived = 0
        ORDER BY name
    """)
    fun observeActiveClients(): Flow<List<ClientEntity>>

    @Query("""
        SELECT * FROM clients
        WHERE isArchived = 1
        ORDER BY archivedAtEpoch DESC
    """)
    fun observeArchivedClients(): Flow<List<ClientEntity>>

    @Query("""
        UPDATE clients
        SET isArchived = 1, archivedAtEpoch = :ts
        WHERE id = :clientId
    """)
    suspend fun archiveClient(clientId: String, ts: Long)

    @Query("""
        UPDATE clients
        SET isArchived = 0, archivedAtEpoch = NULL
        WHERE id = :clientId
    """)
    suspend fun restoreClient(clientId: String)

    @Query("""
        DELETE FROM clients
        WHERE id = :clientId
    """)
    suspend fun hardDeleteClient(clientId: String)
}