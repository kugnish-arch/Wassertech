package com.example.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType

@Dao
interface TemplatesDao {

    @Query("SELECT * FROM checklist_templates WHERE componentType = :type LIMIT 1")
    suspend fun getTemplateByType(type: ComponentType): ChecklistTemplateEntity?

    @Query("SELECT * FROM checklist_fields WHERE templateId = :templateId ORDER BY id")
    suspend fun getFieldsForTemplate(templateId: String): List<ChecklistFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(t: ChecklistTemplateEntity)

    // ⬇️ добавлено: чтобы сидер мог по одному писать поля
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(f: ChecklistFieldEntity)

    // опционально — если захочется батчем:
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFields(list: List<ChecklistFieldEntity>)
}
