package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Запись об удаленном объекте для синхронизации с удаленной БД
 */
@Entity(tableName = "deleted_records")
data class DeletedRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    /** Имя сущности (clients, sites, installations, etc.) - соответствует полю entity в DTO */
    val entity: String,
    /** ID удаленной записи */
    val recordId: String,
    /** Время удаления в миллисекундах */
    val deletedAtEpoch: Long = System.currentTimeMillis(),
    /** Флаг, что запись требует синхронизации */
    val dirtyFlag: Boolean = true,
    /** Статус синхронизации (0=SYNCED, 1=QUEUED, 2=CONFLICT) */
    val syncStatus: Int = 1 // SyncStatus.QUEUED.value
)


