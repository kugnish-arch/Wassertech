package com.example.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplatesDao {

    // ---------- OBSERVE / GET ----------

    @Query(
        """
        SELECT * FROM checklist_templates
        WHERE componentType = :type
        ORDER BY title COLLATE NOCASE
        """
    )
    fun observeTemplatesByType(type: ComponentType): Flow<List<ChecklistTemplateEntity>>

    @Query(
        """
        SELECT * FROM checklist_templates
        ORDER BY title COLLATE NOCASE
        """
    )
    fun observeAllTemplates(): Flow<List<ChecklistTemplateEntity>>

    @Query(
        """
        SELECT * FROM checklist_templates
        WHERE componentType = :type
        LIMIT 1
        """
    )
    suspend fun getTemplateByType(type: ComponentType): ChecklistTemplateEntity?

    @Query(
        """
        SELECT * FROM checklist_templates
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getTemplateById(id: String): ChecklistTemplateEntity?

    // Поля шаблона
    @Query(
        """
        SELECT * FROM checklist_fields
        WHERE templateId = :templateId
        ORDER BY rowid
        """
    )
    fun observeFields(templateId: String): Flow<List<ChecklistFieldEntity>>

    @Query(
        """
        SELECT * FROM checklist_fields
        WHERE templateId = :templateId
        ORDER BY rowid
        """
    )
    suspend fun getFieldsForTemplate(templateId: String): List<ChecklistFieldEntity>

    // Только поля для ТО
    @Query(
        """
        SELECT * FROM checklist_fields
        WHERE templateId = :templateId AND isForMaintenance = 1
        ORDER BY rowid
        """
    )
    fun observeMaintenanceFields(templateId: String): Flow<List<ChecklistFieldEntity>>

    @Query(
        """
        SELECT * FROM checklist_fields
        WHERE templateId = :templateId AND isForMaintenance = 1
        ORDER BY rowid
        """
    )
    suspend fun getMaintenanceFieldsForTemplate(templateId: String): List<ChecklistFieldEntity>

    // ---------- UPSERT / UPDATE ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: ChecklistTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: ChecklistFieldEntity)

    @Query(
        """
        UPDATE checklist_templates
        SET title = :newTitle
        WHERE id = :id
        """
    )
    suspend fun renameTemplate(id: String, newTitle: String)

    // Массовое удаление полей шаблона (иногда удобно перед пересохранением структуры)
    @Query("DELETE FROM checklist_fields WHERE templateId = :templateId")
    suspend fun deleteFieldsByTemplate(templateId: String)

    // ---------- DELETE ----------

    @Query("DELETE FROM checklist_fields WHERE id = :id")
    suspend fun deleteField(id: String)

    @Query("DELETE FROM checklist_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    // ---------- MISC ----------

    @Query("SELECT title FROM checklist_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateTitleById(id: String): String?

    // (Это выглядело лишним в этом DAO, но оставляю как было у тебя)
    @Query("SELECT DISTINCT ClientGroupID FROM clients ORDER BY ClientGroupID COLLATE NOCASE")
    fun observeClientGroups(): Flow<List<String>>

    // ---------- vNext: сортировка/архив (после миграции и добавления полей в Entity) ----------

    @Query(
        """
        SELECT * FROM checklist_templates
        WHERE isArchived = 0
        ORDER BY COALESCE(sortOrder, 2147483647), title COLLATE NOCASE
        """
    )
    fun observeAllTemplatesOrderedActive(): Flow<List<ChecklistTemplateEntity>>

    @Query(
        """
        UPDATE checklist_templates
        SET sortOrder = :order, updatedAtEpoch = :ts
        WHERE id = :id
        """
    )
    suspend fun updateTemplateOrder(id: String, order: Int, ts: Long)

    @Query(
        """
        UPDATE checklist_templates
        SET isArchived = :archived,
            archivedAtEpoch = :archivedTs,
            updatedAtEpoch = :updatedTs
        WHERE id = :id
        """
    )
    suspend fun setTemplateArchived(
        id: String,
        archived: Boolean,
        archivedTs: Long?,
        updatedTs: Long
    )

}
