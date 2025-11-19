package ru.wassertech.client.data.dao

import androidx.room.*
import ru.wassertech.client.data.entities.UserMembershipEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с таблицей user_membership.
 */
@Dao
interface UserMembershipDao {
    
    /**
     * Получить все активные (неархивные) membership записи.
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0")
    fun observeAll(): Flow<List<UserMembershipEntity>>
    
    /**
     * Получить все активные membership записи для конкретного пользователя.
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND user_id = :userId")
    fun observeForUser(userId: String): Flow<List<UserMembershipEntity>>
    
    /**
     * Получить все активные membership записи для конкретного пользователя (suspend).
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND user_id = :userId")
    suspend fun getForUser(userId: String): List<UserMembershipEntity>
    
    /**
     * Получить membership записи для пользователя и объекта (SITE scope).
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND user_id = :userId AND scope = 'SITE' AND target_id = :siteId")
    suspend fun getForUserAndSite(userId: String, siteId: String): List<UserMembershipEntity>
    
    /**
     * Получить membership записи для пользователя и установки (INSTALLATION scope).
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND user_id = :userId AND scope = 'INSTALLATION' AND target_id = :installationId")
    suspend fun getForUserAndInstallation(userId: String, installationId: String): List<UserMembershipEntity>
    
    /**
     * Получить membership записи для пользователя и клиента (CLIENT scope).
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND user_id = :userId AND scope = 'CLIENT' AND target_id = :clientId")
    suspend fun getForUserAndClient(userId: String, clientId: String): List<UserMembershipEntity>
    
    /**
     * Получить все membership записи для конкретной установки (любого пользователя).
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND scope = 'INSTALLATION' AND target_id = :installationId")
    suspend fun getForInstallation(installationId: String): List<UserMembershipEntity>
    
    /**
     * Получить все membership записи для конкретного объекта (любого пользователя).
     */
    @Query("SELECT * FROM user_membership WHERE is_archived = 0 AND scope = 'SITE' AND target_id = :siteId")
    suspend fun getForSite(siteId: String): List<UserMembershipEntity>
    
    /**
     * Вставить или обновить membership записи.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(memberships: List<UserMembershipEntity>)
    
    /**
     * Вставить или обновить одну membership запись.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(membership: UserMembershipEntity)
    
    /**
     * Обновить membership запись.
     */
    @Update
    suspend fun update(membership: UserMembershipEntity)
    
    /**
     * Архивировать все membership записи для конкретной установки.
     */
    @Query("""
        UPDATE user_membership 
        SET is_archived = 1, archived_at_epoch = :archivedAtEpoch 
        WHERE scope = 'INSTALLATION' AND target_id = :installationId AND is_archived = 0
    """)
    suspend fun archiveForInstallation(installationId: String, archivedAtEpoch: Long = System.currentTimeMillis())
    
    /**
     * Архивировать все membership записи для конкретного объекта.
     */
    @Query("""
        UPDATE user_membership 
        SET is_archived = 1, archived_at_epoch = :archivedAtEpoch 
        WHERE scope = 'SITE' AND target_id = :siteId AND is_archived = 0
    """)
    suspend fun archiveForSite(siteId: String, archivedAtEpoch: Long = System.currentTimeMillis())
    
    /**
     * Удалить все membership записи для конкретной установки (полное удаление).
     */
    @Query("DELETE FROM user_membership WHERE scope = 'INSTALLATION' AND target_id = :installationId")
    suspend fun deleteForInstallation(installationId: String)
    
    /**
     * Удалить все membership записи для конкретного объекта (полное удаление).
     */
    @Query("DELETE FROM user_membership WHERE scope = 'SITE' AND target_id = :siteId")
    suspend fun deleteForSite(siteId: String)
    
    /**
     * Получить все dirty membership записи для синхронизации.
     */
    @Query("SELECT * FROM user_membership WHERE dirty_flag = 1 AND is_archived = 0")
    suspend fun getDirtyMembershipsNow(): List<UserMembershipEntity>
    
    /**
     * Пометить membership записи как синхронизированные.
     */
    @Query("""
        UPDATE user_membership 
        SET dirty_flag = 0, sync_status = 0 
        WHERE user_id = :userId AND scope = :scope AND target_id = :targetId
    """)
    suspend fun markAsSynced(userId: String, scope: String, targetId: String)
    
    /**
     * Пометить membership записи как конфликтные.
     */
    @Query("""
        UPDATE user_membership 
        SET sync_status = 2 
        WHERE user_id = :userId AND scope = :scope AND target_id = :targetId
    """)
    suspend fun markAsConflict(userId: String, scope: String, targetId: String)
}

