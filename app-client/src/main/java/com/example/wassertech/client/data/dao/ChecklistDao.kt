
package ru.wassertech.client.data.dao

import androidx.room.*
import ru.wassertech.client.data.entities.ChecklistFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Query("""
        SELECT * FROM checklist_fields
        WHERE templateId = :templateId
        ORDER BY rowid
    """)
    fun observeFields(templateId: String): Flow<List<ChecklistFieldEntity>>

    @Query("""
        SELECT * FROM checklist_fields
        WHERE templateId = :templateId
        ORDER BY rowid
    """)
    suspend fun getFields(templateId: String): List<ChecklistFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: ChecklistFieldEntity)

    @Delete
    suspend fun deleteField(field: ChecklistFieldEntity)

    @Query("DELETE FROM checklist_fields WHERE id = :id")
    suspend fun deleteFieldById(id: String)
}
