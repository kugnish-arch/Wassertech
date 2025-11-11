package com.example.wassertech.sync

import com.example.wassertech.data.AppDatabase

/**
 * Утилита для безопасного удаления объектов с отслеживанием для синхронизации
 */
object SafeDeletionHelper {
    
    /**
     * Удаляет группу клиентов и помечает её для синхронизации
     */
    suspend fun deleteClientGroup(db: AppDatabase, groupId: String) {
        // Сначала удаляем всех клиентов из группы (они перейдут в "без группы")
        val clients = db.clientDao().getClientsNow(groupId)
        clients.forEach { client ->
            db.clientDao().setClientGroup(client.id, null, System.currentTimeMillis())
        }
        
        // Удаляем саму группу
        db.clientDao().deleteGroup(groupId)
        DeletionTracker.markClientGroupDeleted(db, groupId)
    }
    
    /**
     * Удаляет клиента и помечает его для синхронизации
     */
    suspend fun deleteClient(db: AppDatabase, clientId: String) {
        db.archiveDao().hardDeleteClient(clientId)
        DeletionTracker.markClientDeleted(db, clientId)
    }
    
    /**
     * Удаляет объект (site) и помечает его для синхронизации
     */
    suspend fun deleteSite(db: AppDatabase, siteId: String) {
        db.hierarchyDao().deleteSite(siteId)
        DeletionTracker.markSiteDeleted(db, siteId)
    }
    
    /**
     * Удаляет установку и помечает её для синхронизации
     */
    suspend fun deleteInstallation(db: AppDatabase, installationId: String) {
        db.hierarchyDao().deleteInstallation(installationId)
        DeletionTracker.markInstallationDeleted(db, installationId)
    }
    
    /**
     * Удаляет компонент и помечает его для синхронизации
     */
    suspend fun deleteComponent(db: AppDatabase, componentId: String) {
        db.hierarchyDao().deleteComponent(componentId)
        DeletionTracker.markComponentDeleted(db, componentId)
    }
    
    /**
     * Удаляет шаблон и помечает его для синхронизации
     */
    suspend fun deleteTemplate(db: AppDatabase, templateId: String) {
        db.templatesDao().deleteTemplate(templateId)
        DeletionTracker.markTemplateDeleted(db, templateId)
        
        // Также удаляем все поля шаблона
        val fields = db.templatesDao().getFieldsForTemplate(templateId)
        fields.forEach { field ->
            db.templatesDao().deleteField(field.id)
            DeletionTracker.markFieldDeleted(db, field.id)
        }
    }
    
    /**
     * Удаляет сессию ТО и помечает её для синхронизации
     */
    suspend fun deleteSession(db: AppDatabase, sessionId: String) {
        // Удаляем значения сессии
        val values = db.sessionsDao().getValuesForSession(sessionId)
        values.forEach { value ->
            db.sessionsDao().deleteValue(value.id)
            DeletionTracker.markAsDeleted(db, "maintenance_values", value.id)
        }
        
        // Удаляем саму сессию
        db.sessionsDao().deleteSession(sessionId)
        DeletionTracker.markSessionDeleted(db, sessionId)
    }
}

