package ru.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.wassertech.data.entities.ClientEntity
import ru.wassertech.data.entities.ClientGroupEntity


/**
 * Расширенный DAO: добавлены observeClients(...) и getClient(...)
 * чтобы не править существующий HierarchyViewModel.
 */
@Dao
interface ClientDao {

    // ---------- Groups ----------

    /** Все группы (включая архивные). */
    @Query(
        """
        SELECT * FROM client_groups
        ORDER BY 
            CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END ASC,
            sortOrder ASC
        """
    )
    fun getAllGroupsNow(): List<ClientGroupEntity>

    @Upsert
    fun upsertGroup(group: ClientGroupEntity)

    /** Обновление порядка групп. */
    @Query(
        """
        UPDATE client_groups 
        SET sortOrder = :order, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1
        WHERE id = :id
        """
    )
    fun updateGroupOrder(id: String, order: Int, ts: Long = System.currentTimeMillis())

    // ---------- Clients ----------

    /**
     * Наблюдение за клиентами. Если includeArchived=false — скрываем архивные.
     * В SQLite булевы параметры маппятся в 0/1, условие (:includeArchived = 1) работает как "показывать все".
     */
    @Query(
        """
        SELECT * FROM clients 
        WHERE (:includeArchived = 1) 
              OR (isArchived IS NULL OR isArchived = 0)
        ORDER BY 
            CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END ASC,
            sortOrder ASC
        """
    )
    fun observeClients(includeArchived: Boolean = false): Flow<List<ClientEntity>>

    /**
     * Клиенты по группе (моментальный снимок):
     *  - groupId == NULL -> клиенты без группы
     *  - иначе клиенты указанной группы
     */
    @Query(
        """
        SELECT * FROM clients 
        WHERE 
            (:groupId IS NULL AND clientGroupId IS NULL) OR 
            (:groupId IS NOT NULL AND clientGroupId = :groupId)
        ORDER BY 
            CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END ASC,
            sortOrder ASC
        """
    )
    fun getClientsNow(groupId: String?): List<ClientEntity>

    /** Найти клиента по id (название как в старой версии, чтобы не ломать VM). */
    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClient(id: String): ClientEntity?

    /** Оставлен для совместимости с новым кодом. */
    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClientByIdNow(id: String): ClientEntity?

    @Upsert
    fun upsertClient(client: ClientEntity)

    /** Назначить/снять группу у клиента. */
    @Query(
        """
        UPDATE clients 
        SET clientGroupId = :groupId, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1
        WHERE id = :clientId
        """
    )
    fun setClientGroup(clientId: String, groupId: String?, ts: Long)

    /** Обновление порядка клиентов. */
    @Query(
        """
        UPDATE clients 
        SET sortOrder = :order, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1
        WHERE id = :id
        """
    )
    fun updateClientOrder(id: String, order: Int, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeClientRaw(id: String): kotlinx.coroutines.flow.Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients")
    fun observeAllClients(): Flow<List<ClientEntity>>

    // Переименовать имя клиента
    @Query("UPDATE clients SET name = :newName, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1 WHERE id = :clientId")
    suspend fun updateClientName(clientId: String, newName: String, ts: Long = System.currentTimeMillis()): Int

    // Перенести клиента в другую группу (или убрать из группы, если null)
    @Query("UPDATE clients SET clientGroupId = :groupId, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1 WHERE id = :clientId")
    suspend fun assignClientToGroup(clientId: String, groupId: String?, ts: Long = System.currentTimeMillis()): Int

    // Переименовать группу
    @Query("UPDATE client_groups SET title = :newTitle, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1 WHERE id = :groupId")
    suspend fun updateGroupTitle(groupId: String, newTitle: String, ts: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientNow(id: String): ClientEntity

    /** Получить всех клиентов для синхронизации */
    @Query("SELECT * FROM clients")
    fun getAllClientsNow(): List<ClientEntity>

    /** Удалить группу клиентов навсегда */
    @Query("DELETE FROM client_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    /** Удалить клиента навсегда */
    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: String)

    // ========== Методы синхронизации ==========
    
    /** Получить всех "грязных" клиентов (требующих синхронизации) */
    @Query("SELECT * FROM clients WHERE dirtyFlag = 1")
    fun getDirtyClientsNow(): List<ClientEntity>
    
    /** Получить все "грязные" группы клиентов */
    @Query("SELECT * FROM client_groups WHERE dirtyFlag = 1")
    fun getDirtyGroupsNow(): List<ClientGroupEntity>
    
    /** Пометить клиентов как синхронизированные */
    @Query("""
        UPDATE clients 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markClientsAsSynced(ids: List<String>)
    
    /** Пометить группы клиентов как синхронизированные */
    @Query("""
        UPDATE client_groups 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markGroupsAsSynced(ids: List<String>)
    
    /** Пометить клиентов как конфликтные */
    @Query("""
        UPDATE clients 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markClientsAsConflict(ids: List<String>)
    
    /** Пометить группы клиентов как конфликтные */
    @Query("""
        UPDATE client_groups 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markGroupsAsConflict(ids: List<String>)
    
    /** Снять флаг "грязный" у клиентов */
    @Query("""
        UPDATE clients 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearClientsDirtyFlag(ids: List<String>)
    
    /** Снять флаг "грязный" у групп клиентов */
    @Query("""
        UPDATE client_groups 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearGroupsDirtyFlag(ids: List<String>)

}

