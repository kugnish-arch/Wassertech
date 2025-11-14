
package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.ComponentTemplateFieldEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с полями шаблонов компонентов (чек-листы).
 * Обновлено для работы с новой моделью component_template_fields.
 * 
 * @deprecated Рекомендуется использовать ComponentTemplateFieldsDao напрямую
 */
@Dao
interface ChecklistDao {

    /**
     * Получить все поля шаблона (включая характеристики и чек-лист ТО)
     */
    @Query("""
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId
        ORDER BY sortOrder, rowid
    """)
    fun observeFields(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    /**
     * Получить все поля шаблона (включая характеристики и чек-лист ТО)
     */
    @Query("""
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId
        ORDER BY sortOrder, rowid
    """)
    suspend fun getFields(templateId: String): List<ComponentTemplateFieldEntity>

    /**
     * Получить только поля чек-листа ТО (isCharacteristic = false)
     */
    @Query("""
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId AND isCharacteristic = 0
        ORDER BY sortOrder, rowid
    """)
    fun observeMaintenanceFields(templateId: String): Flow<List<ComponentTemplateFieldEntity>>

    /**
     * Получить только поля чек-листа ТО (isCharacteristic = false)
     */
    @Query("""
        SELECT * FROM component_template_fields
        WHERE templateId = :templateId AND isCharacteristic = 0
        ORDER BY sortOrder, rowid
    """)
    suspend fun getMaintenanceFields(templateId: String): List<ComponentTemplateFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: ComponentTemplateFieldEntity)

    @Delete
    suspend fun deleteField(field: ComponentTemplateFieldEntity)

    @Query("DELETE FROM component_template_fields WHERE id = :id")
    suspend fun deleteFieldById(id: String)
}
