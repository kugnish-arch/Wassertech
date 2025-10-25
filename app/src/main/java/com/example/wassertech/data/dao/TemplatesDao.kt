package com.example.wassertech.data.dao

import androidx.room.*
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.types.ComponentType

@Dao
interface TemplatesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(t: ChecklistTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFields(fields: List<ChecklistFieldEntity>)

    @Transaction
    @Query("SELECT * FROM checklist_templates WHERE componentType = :type LIMIT 1")
    suspend fun getTemplateByType(type: ComponentType): ChecklistTemplateEntity?

    @Query("SELECT * FROM checklist_fields WHERE templateId = :templateId ORDER BY id")
    suspend fun getFieldsForTemplate(templateId: String): List<ChecklistFieldEntity>
}
