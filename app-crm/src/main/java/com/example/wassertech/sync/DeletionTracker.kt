package com.example.wassertech.sync

import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.DeletedRecordEntity

/**
 * Утилита для отслеживания удалений объектов для синхронизации с удаленной БД
 */
object DeletionTracker {
    
    /**
     * Записывает факт удаления объекта для последующей синхронизации
     */
    suspend fun markAsDeleted(db: AppDatabase, tableName: String, recordId: String) {
        db.deletedRecordsDao().addDeletedRecord(
            DeletedRecordEntity(
                tableName = tableName,
                recordId = recordId
            )
        )
    }
    
    /**
     * Помечает группу клиентов как удаленную
     */
    suspend fun markClientGroupDeleted(db: AppDatabase, groupId: String) {
        markAsDeleted(db, "client_groups", groupId)
    }
    
    /**
     * Помечает клиента как удаленного
     */
    suspend fun markClientDeleted(db: AppDatabase, clientId: String) {
        markAsDeleted(db, "clients", clientId)
    }
    
    /**
     * Помечает объект как удаленный
     */
    suspend fun markSiteDeleted(db: AppDatabase, siteId: String) {
        markAsDeleted(db, "sites", siteId)
    }
    
    /**
     * Помечает установку как удаленную
     */
    suspend fun markInstallationDeleted(db: AppDatabase, installationId: String) {
        markAsDeleted(db, "installations", installationId)
    }
    
    /**
     * Помечает компонент как удаленный
     */
    suspend fun markComponentDeleted(db: AppDatabase, componentId: String) {
        markAsDeleted(db, "components", componentId)
    }
    
    /**
     * Помечает шаблон как удаленный
     */
    suspend fun markTemplateDeleted(db: AppDatabase, templateId: String) {
        markAsDeleted(db, "checklist_templates", templateId)
    }
    
    /**
     * Помечает поле шаблона как удаленное
     */
    suspend fun markFieldDeleted(db: AppDatabase, fieldId: String) {
        markAsDeleted(db, "checklist_fields", fieldId)
    }
    
    /**
     * Помечает сессию ТО как удаленную
     */
    suspend fun markSessionDeleted(db: AppDatabase, sessionId: String) {
        markAsDeleted(db, "maintenance_sessions", sessionId)
    }
}


