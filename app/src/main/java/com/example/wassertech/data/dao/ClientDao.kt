package com.example.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wassertech.data.entities.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    // ===== Read / observe =====

    @Query("""
        SELECT * FROM clients
        WHERE isArchived = 0
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC
    """)
    fun observeActiveClients(): Flow<List<ClientEntity>>

    @Query("""
        SELECT * FROM clients
        WHERE isArchived = 1
        ORDER BY
                 (archivedAtEpoch IS NULL) ASC,     -- эмуляция NULLS LAST
                 archivedAtEpoch DESC,
                 name COLLATE NOCASE ASC
    """)
    fun observeArchivedClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClient(id: String): ClientEntity?

    /**
     * Поиск по имени/телефону/e-mail/адресу.
     * Передавай q вида "%term%".
     */
    @Query("""
        SELECT * FROM clients
        WHERE (:includeArchived OR isArchived = 0)
          AND (
                name        LIKE :q
             OR phone       LIKE :q
             OR phone2      LIKE :q
             OR email       LIKE :q
             OR addressFull LIKE :q
          )
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun searchClients(q: String, includeArchived: Boolean = false): Flow<List<ClientEntity>>

    // ===== Grouping =====

    /**
     * Клиенты по группе.
     * Если clientGroupId == NULL → «без группы».
     */
    @Query("""
        SELECT * FROM clients
        WHERE (:clientGroupId IS NULL AND clientGroupId IS NULL)
           OR (clientGroupId = :clientGroupId)
          AND (:includeArchived OR isArchived = 0)
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC
    """)
    fun observeClientsByGroup(
        clientGroupId: String?,
        includeArchived: Boolean = false
    ): Flow<List<ClientEntity>>

    @Query("UPDATE clients SET clientGroupId = :clientGroupId, updatedAtEpoch = :ts WHERE id = :clientId")
    suspend fun setClientGroup(clientId: String, clientGroupId: String?, ts: Long = System.currentTimeMillis())

    // ===== Create / update =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClient(client: ClientEntity)

    @Update
    suspend fun updateClients(list: List<ClientEntity>)  // удобно для reorder батчем

    // ===== Archive / restore =====

    @Query("UPDATE clients SET isArchived = 1, archivedAtEpoch = :ts WHERE id = :id")
    suspend fun archiveClient(id: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE clients SET isArchived = 0, archivedAtEpoch = NULL WHERE id = :id")
    suspend fun restoreClient(id: String)

    @Query("""
        SELECT * FROM clients
        WHERE (:includeArchived = 1) OR (isArchived = 0)
        ORDER BY name COLLATE NOCASE
    """)
    fun observeClients(includeArchived: Boolean = false): Flow<List<ClientEntity>>
}
