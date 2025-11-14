package ru.wassertech.sync

import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.DeletedRecordEntity
import java.util.UUID

/**
 * Утилита для отслеживания удалений объектов для синхронизации с удаленной БД
 */
object DeletionTracker {
    
    /**
     * Записывает факт удаления объекта для последующей синхронизации
     */
    suspend fun markAsDeleted(db: AppDatabase, entity: String, recordId: String) {
        db.deletedRecordsDao().insert(
            DeletedRecordEntity(
                id = UUID.randomUUID().toString(),
                entity = entity,
                recordId = recordId,
                deletedAtEpoch = System.currentTimeMillis(),
                dirtyFlag = true,
                syncStatus = 1 // SyncStatus.QUEUED.value
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
     * Помечает шаблон компонента как удаленный
     * @deprecated Используйте markComponentTemplateDeleted
     */
    @Deprecated("Используйте markComponentTemplateDeleted")
    suspend fun markTemplateDeleted(db: AppDatabase, templateId: String) {
        markComponentTemplateDeleted(db, templateId)
    }
    
    /**
     * Помечает поле шаблона компонента как удаленное
     * @deprecated Используйте markComponentTemplateFieldDeleted
     */
    @Deprecated("Используйте markComponentTemplateFieldDeleted")
    suspend fun markFieldDeleted(db: AppDatabase, fieldId: String) {
        markComponentTemplateFieldDeleted(db, fieldId)
    }
    
    /**
     * Помечает поле шаблона компонента как удаленное
     */
    suspend fun markComponentTemplateFieldDeleted(db: AppDatabase, fieldId: String) {
        markAsDeleted(db, "component_template_fields", fieldId)
    }
    
    /**
     * Помечает сессию ТО как удаленную
     */
    suspend fun markSessionDeleted(db: AppDatabase, sessionId: String) {
        markAsDeleted(db, "maintenance_sessions", sessionId)
    }
    
    /**
     * Помечает шаблон компонента как удаленный
     */
    suspend fun markComponentTemplateDeleted(db: AppDatabase, templateId: String) {
        markAsDeleted(db, "component_templates", templateId)
    }
}


