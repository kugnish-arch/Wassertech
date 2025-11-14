package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.ComponentTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentTemplatesDao {

    @Query("SELECT * FROM component_templates WHERE isArchived = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<ComponentTemplateEntity>>

    @Query("SELECT * FROM component_templates ORDER BY isArchived, sortOrder, name")
    fun observeAll(): Flow<List<ComponentTemplateEntity>>

    @Query("SELECT * FROM component_templates WHERE id = :id")
    suspend fun getById(id: String): ComponentTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ComponentTemplateEntity)

    @Update
    suspend fun update(item: ComponentTemplateEntity)

    @Query("UPDATE component_templates SET isArchived = :arch WHERE id = :id")
    suspend fun setArchived(id: String, arch: Boolean)

    @Query("UPDATE component_templates SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Delete
    suspend fun delete(item: ComponentTemplateEntity)
    
    // ========== Методы синхронизации ==========
    
    /** Получить все "грязные" шаблоны компонентов */
    @Query("SELECT * FROM component_templates WHERE dirtyFlag = 1")
    fun getDirtyComponentTemplatesNow(): List<ComponentTemplateEntity>
    
    /** Пометить шаблоны компонентов как синхронизированные */
    @Query("""
        UPDATE component_templates 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markComponentTemplatesAsSynced(ids: List<String>)
    
    /** Пометить шаблоны компонентов как конфликтные */
    @Query("""
        UPDATE component_templates 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markComponentTemplatesAsConflict(ids: List<String>)
    
    /** Снять флаг "грязный" у шаблонов компонентов */
    @Query("""
        UPDATE component_templates 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearComponentTemplatesDirtyFlag(ids: List<String>)
}
