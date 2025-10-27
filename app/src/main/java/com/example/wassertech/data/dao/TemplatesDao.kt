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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: ChecklistTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: ChecklistFieldEntity)
}
