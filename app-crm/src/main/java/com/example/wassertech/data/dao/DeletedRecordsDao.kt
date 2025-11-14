package ru.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.wassertech.data.entities.DeletedRecordEntity
import ru.wassertech.data.types.SyncStatus

@Dao
interface DeletedRecordsDao {
    /** Получить все "грязные" записи об удалениях (требующие синхронизации) */
    @Query("SELECT * FROM deleted_records WHERE dirtyFlag = 1 AND syncStatus = :queued ORDER BY deletedAtEpoch ASC")
    fun getDirtyDeletedRecordsNow(queued: Int = SyncStatus.QUEUED.value): List<DeletedRecordEntity>
    
    /** Получить все удаленные записи для синхронизации */
    @Query("SELECT * FROM deleted_records ORDER BY deletedAtEpoch ASC")
    fun getAllDeletedRecordsNow(): List<DeletedRecordEntity>
    
    /** Добавить запись об удалении */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DeletedRecordEntity)
    
    /** Добавить несколько записей об удалении */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DeletedRecordEntity>)
    
    /** Пометить записи как синхронизированные */
    @Query("UPDATE deleted_records SET dirtyFlag = 0, syncStatus = :synced WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, synced: Int = SyncStatus.SYNCED.value)
    
    /** Удалить все синхронизированные записи об удалениях */
    @Query("DELETE FROM deleted_records WHERE syncStatus = :synced")
    suspend fun deleteAllSynced(synced: Int = SyncStatus.SYNCED.value)
    
    /** Удалить все записи об удалениях (после успешной синхронизации) */
    @Query("DELETE FROM deleted_records")
    suspend fun clearAllDeletedRecords()
    
    /** Получить удаленные записи по имени сущности */
    @Query("SELECT * FROM deleted_records WHERE entity = :entity")
    fun getDeletedRecordsByEntity(entity: String): List<DeletedRecordEntity>
    
    // Обратная совместимость (deprecated)
    /** @deprecated Используйте insert() */
    @Deprecated("Используйте insert()", ReplaceWith("insert(record)"))
    suspend fun addDeletedRecord(record: DeletedRecordEntity) = insert(record)
}


