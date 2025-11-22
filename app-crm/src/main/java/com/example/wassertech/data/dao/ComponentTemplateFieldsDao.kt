package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.ComponentTemplateFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentTemplateFieldsDao {

    // ---------- OBSERVE / GET ----------

    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId
        ORDER BY sortOrder, rowid
        """
    )
    fun observeFieldsForTemplate(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId
        ORDER BY sortOrder, rowid
        """
    )
    suspend fun getFieldsForTemplate(templateId: String): List<ComponentTemplateFieldEntity>

    /**
     * Получить только характеристики (isCharacteristic = true)
     */
    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId AND isCharacteristic = 1
        ORDER BY sortOrder, rowid
        """
    )
    fun observeCharacteristicsForTemplate(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    /**
     * Получить только поля чек-листа ТО (isCharacteristic = false)
     */
    @Query(
        """
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId AND isCharacteristic = 0
        ORDER BY sortOrder, rowid
        """
    )
    fun observeChecklistFieldsForTemplate(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    @Query("SELECT * FROM component_template_fields WHERE id = :id LIMIT 1")
    suspend fun getFieldById(id: String): ComponentTemplateFieldEntity?

    // ---------- UPSERT / UPDATE ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: ComponentTemplateFieldEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFields(fields: List<ComponentTemplateFieldEntity>)

    @Update
    suspend fun updateField(field: ComponentTemplateFieldEntity)

    @Query(
        """
        UPDATE component_template_fields
        SET sortOrder = :order, updatedAtEpoch = :ts, dirtyFlag = 1, syncStatus = 1
        WHERE id = :id
        """
    )
    suspend fun updateFieldOrder(id: String, order: Int, ts: Long = System.currentTimeMillis())

    // Массовое удаление полей шаблона
    @Query("DELETE FROM component_template_fields WHERE templateId = :templateId")
    suspend fun deleteFieldsByTemplate(templateId: String)

    // ---------- DELETE ----------

    @Query("DELETE FROM component_template_fields WHERE id = :id")
    suspend fun deleteField(id: String)

    // ---------- МЕТОДЫ СИНХРОНИЗАЦИИ ----------

    /** Получить все "грязные" поля шаблонов компонентов */
    @Query("SELECT * FROM component_template_fields WHERE dirtyFlag = 1")
    fun getDirtyComponentTemplateFieldsNow(): List<ComponentTemplateFieldEntity>

    /** Пометить поля как синхронизированные */
    @Query("""
        UPDATE component_template_fields 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markComponentTemplateFieldsAsSynced(ids: List<String>)

    /** Пометить поля как конфликтные */
    @Query("""
        UPDATE component_template_fields 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markComponentTemplateFieldsAsConflict(ids: List<String>)

    /** Снять флаг "грязный" у полей */
    @Query("""
        UPDATE component_template_fields 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearComponentTemplateFieldsDirtyFlag(ids: List<String>)
}





