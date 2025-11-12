package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Запись об удаленном объекте для синхронизации с удаленной БД
 */
@Entity(tableName = "deleted_records")
data class DeletedRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Имя таблицы, из которой была удалена запись (clients, sites, installations, etc.) */
    val tableName: String,
    /** ID удаленной записи */
    val recordId: String,
    /** Время удаления в миллисекундах */
    val deletedAtEpoch: Long = System.currentTimeMillis()
)


