package com.example.wassertech.data.dao

import androidx.room.*
import com.example.wassertech.data.entities.ComponentTemplateEntity
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
}
