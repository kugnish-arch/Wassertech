package com.example.wassertech.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wassertech.data.entities.DeletedRecordEntity

@Dao
interface DeletedRecordsDao {
    /** Получить все удаленные записи для синхронизации */
    @Query("SELECT * FROM deleted_records ORDER BY deletedAtEpoch ASC")
    fun getAllDeletedRecordsNow(): List<DeletedRecordEntity>
    
    /** Добавить запись об удалении */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDeletedRecord(record: DeletedRecordEntity)
    
    /** Удалить все записи об удалениях (после успешной синхронизации) */
    @Query("DELETE FROM deleted_records")
    suspend fun clearAllDeletedRecords()
    
    /** Удалить конкретные записи об удалениях (после успешной синхронизации) */
    @Query("DELETE FROM deleted_records WHERE id IN (:ids)")
    suspend fun clearDeletedRecords(ids: List<Long>)
    
    /** Получить удаленные записи по имени таблицы */
    @Query("SELECT * FROM deleted_records WHERE tableName = :tableName")
    fun getDeletedRecordsByTable(tableName: String): List<DeletedRecordEntity>
}


