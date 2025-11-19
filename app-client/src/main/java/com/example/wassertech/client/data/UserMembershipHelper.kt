package ru.wassertech.client.data

import ru.wassertech.client.data.entities.UserMembershipEntity
import ru.wassertech.client.data.dao.UserMembershipDao
import ru.wassertech.core.auth.SessionManager
import android.content.Context
import java.util.UUID

/**
 * Вспомогательный класс для автоматической работы с user_membership в app-client.
 */
object UserMembershipHelper {
    
    /**
     * Создаёт membership для объекта (Site) после его создания.
     * Вызывается после успешного создания SiteEntity в БД.
     */
    suspend fun createSiteMembership(
        context: Context,
        siteId: String,
        userId: String
    ) {
        val db = AppDatabase.getInstance(context)
        val membershipDao = db.userMembershipDao()
        
        // Проверяем, не существует ли уже такая membership
        val existing = membershipDao.getForUserAndSite(userId, siteId)
        if (existing.isNotEmpty()) {
            // Membership уже существует, не создаём дубликат
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val membership = UserMembershipEntity(
            userId = userId,
            scope = "SITE",
            targetId = siteId,
            createdAtEpoch = currentTime,
            updatedAtEpoch = currentTime,
            isArchived = false,
            archivedAtEpoch = null,
            dirtyFlag = true, // Требует синхронизации
            syncStatus = 1 // QUEUED
        )
        
        membershipDao.upsert(membership)
    }
    
    /**
     * Создаёт membership для установки (Installation) после её создания.
     * Вызывается после успешного создания InstallationEntity в БД.
     */
    suspend fun createInstallationMembership(
        context: Context,
        installationId: String,
        userId: String
    ) {
        val db = AppDatabase.getInstance(context)
        val membershipDao = db.userMembershipDao()
        
        // Проверяем, не существует ли уже такая membership
        val existing = membershipDao.getForUserAndInstallation(userId, installationId)
        if (existing.isNotEmpty()) {
            // Membership уже существует, не создаём дубликат
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val membership = UserMembershipEntity(
            userId = userId,
            scope = "INSTALLATION",
            targetId = installationId,
            createdAtEpoch = currentTime,
            updatedAtEpoch = currentTime,
            isArchived = false,
            archivedAtEpoch = null,
            dirtyFlag = true, // Требует синхронизации
            syncStatus = 1 // QUEUED
        )
        
        membershipDao.upsert(membership)
    }
    
    /**
     * Архивирует все membership записи для установки при её удалении/архивации.
     * Вызывается после того, как установка помечена как архивная/удалена.
     */
    suspend fun archiveInstallationMemberships(
        context: Context,
        installationId: String
    ) {
        val db = AppDatabase.getInstance(context)
        val membershipDao = db.userMembershipDao()
        
        // Получаем все membership записи для этой установки
        val memberships = membershipDao.getForInstallation(installationId)
        
        if (memberships.isEmpty()) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Архивируем все записи
        memberships.forEach { membership ->
            val archived = membership.copy(
                isArchived = true,
                archivedAtEpoch = currentTime,
                updatedAtEpoch = currentTime,
                dirtyFlag = true, // Требует синхронизации
                syncStatus = 1 // QUEUED
            )
            membershipDao.update(archived)
        }
    }
    
    /**
     * Архивирует все membership записи для объекта при его удалении/архивации.
     * Вызывается после того, как объект помечен как архивный/удалён.
     */
    suspend fun archiveSiteMemberships(
        context: Context,
        siteId: String
    ) {
        val db = AppDatabase.getInstance(context)
        val membershipDao = db.userMembershipDao()
        
        // Получаем все membership записи для этого объекта
        val memberships = membershipDao.getForSite(siteId)
        
        if (memberships.isEmpty()) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Архивируем все записи
        memberships.forEach { membership ->
            val archived = membership.copy(
                isArchived = true,
                archivedAtEpoch = currentTime,
                updatedAtEpoch = currentTime,
                dirtyFlag = true, // Требует синхронизации
                syncStatus = 1 // QUEUED
            )
            membershipDao.update(archived)
        }
    }
}

