package ru.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.wassertech.data.entities.ComponentTemplateEntity
import ru.wassertech.data.entities.ComponentTemplateFieldEntity
import ru.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с шаблонами компонентов.
 * Обновлено для работы с новой моделью component_templates и component_template_fields.
 * 
 * @deprecated Рекомендуется использовать ComponentTemplatesDao и ComponentTemplateFieldsDao напрямую
 */
@Dao
interface TemplatesDao {

    // ---------- OBSERVE / GET ----------
    // Методы для работы с component_templates (новая модель)

    @Query(
        """
        SELECT * FROM component_templates
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeAllTemplates(): Flow<List<ComponentTemplateEntity>>

    @Query(
        """
        SELECT * FROM component_templates
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getTemplateById(id: String): ComponentTemplateEntity?

    // Поля шаблона (новая модель)
    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId
        ORDER BY sortOrder, rowid
        """
    )
    fun observeFields(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId
        ORDER BY sortOrder, rowid
        """
    )
    suspend fun getFieldsForTemplate(templateId: String): List<ComponentTemplateFieldEntity>

    // Только поля для ТО (isCharacteristic = false)
    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId AND isCharacteristic = 0
        ORDER BY sortOrder, rowid
        """
    )
    fun observeMaintenanceFields(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId AND isCharacteristic = 0
        ORDER BY sortOrder, rowid
        """
    )
    suspend fun getMaintenanceFieldsForTemplate(templateId: String): List<ComponentTemplateFieldEntity>

    // ---------- UPSERT / UPDATE ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: ComponentTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: ComponentTemplateFieldEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ComponentTemplateEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: ComponentTemplateFieldEntity)

    @Query(
        """
        UPDATE component_templates
        SET name = :newTitle, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1
        WHERE id = :id
        """
    )
    suspend fun renameTemplate(id: String, newTitle: String, ts: Long = System.currentTimeMillis())

    // Массовое удаление полей шаблона (иногда удобно перед пересохранением структуры)
    @Query("DELETE FROM component_template_fields WHERE templateId = :templateId")
    suspend fun deleteFieldsByTemplate(templateId: String)

    // ---------- DELETE ----------

    @Query("DELETE FROM component_template_fields WHERE id = :id")
    suspend fun deleteField(id: String)

    @Query("DELETE FROM component_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    // ---------- MISC ----------

    @Query("SELECT name FROM component_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateTitleById(id: String): String?

    // (Это выглядело лишним в этом DAO, но оставляю как было у тебя)
    @Query("SELECT DISTINCT ClientGroupID FROM clients ORDER BY ClientGroupID COLLATE NOCASE")
    fun observeClientGroups(): Flow<List<String>>

    // ---------- Сортировка/архив ----------

    @Query(
        """
        SELECT * FROM component_templates
        WHERE isArchived = 0
        ORDER BY sortOrder, name COLLATE NOCASE
        """
    )
    fun observeAllTemplatesOrderedActive(): Flow<List<ComponentTemplateEntity>>

    @Query(
        """
        UPDATE component_templates
        SET sortOrder = :order, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1
        WHERE id = :id
        """
    )
    suspend fun updateTemplateOrder(id: String, order: Int, ts: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE component_templates
        SET isArchived = :archived,
            archivedAtEpoch = CASE WHEN :archived = 1 THEN COALESCE(archivedAtEpoch, :archivedTs, :updatedTs) ELSE NULL END,
            updatedAtEpoch = :updatedTs,
            dirtyFlag = 1,
            syncStatus = 1
        WHERE id = :id
        """
    )
    suspend fun setTemplateArchived(
        id: String,
        archived: Boolean,
        archivedTs: Long?,
        updatedTs: Long
    )

    /** Получить все шаблоны для синхронизации */
    @Query("SELECT * FROM component_templates")
    fun getAllTemplatesNow(): List<ComponentTemplateEntity>

    /** Получить все поля для синхронизации */
    @Query("SELECT * FROM component_template_fields")
    fun getAllFieldsNow(): List<ComponentTemplateFieldEntity>
    
    // ========== Методы синхронизации ==========
    
    /** Получить все "грязные" шаблоны */
    @Query("SELECT * FROM component_templates WHERE dirtyFlag = 1")
    fun getDirtyTemplatesNow(): List<ComponentTemplateEntity>
    
    /** Получить все "грязные" поля шаблонов */
    @Query("SELECT * FROM component_template_fields WHERE dirtyFlag = 1")
    fun getDirtyFieldsNow(): List<ComponentTemplateFieldEntity>
    
    /** Пометить шаблоны как синхронизированные */
    @Query("""
        UPDATE component_templates 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markTemplatesAsSynced(ids: List<String>)
    
    /** Пометить поля как синхронизированные */
    @Query("""
        UPDATE component_template_fields 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markFieldsAsSynced(ids: List<String>)
    
    /** Пометить шаблоны как конфликтные */
    @Query("""
        UPDATE component_templates 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markTemplatesAsConflict(ids: List<String>)
    
    /** Пометить поля как конфликтные */
    @Query("""
        UPDATE component_template_fields 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markFieldsAsConflict(ids: List<String>)
    
    /** Снять флаг "грязный" у шаблонов */
    @Query("""
        UPDATE component_templates 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearTemplatesDirtyFlag(ids: List<String>)
    
    /** Снять флаг "грязный" у полей */
    @Query("""
        UPDATE component_template_fields 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearFieldsDirtyFlag(ids: List<String>)

}
