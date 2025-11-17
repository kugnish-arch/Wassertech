package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность значения поля технического обслуживания.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
@Entity(
    tableName = "maintenance_values",
    indices = [
        Index("sessionId"),
        Index("siteId"),
        Index("installationId"),
        Index("componentId"),
        Index("fieldKey"),
        Index("dirtyFlag"),
        Index("syncStatus"),
        Index("origin"),
        Index("created_by_user_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MaintenanceValueEntity(
    @PrimaryKey val id: String,
    val sessionId: String,       // связь с MaintenanceSessionEntity.id
    val siteId: String,          // добавил: для быстрого поиска по объекту
    val installationId: String?, // установка может быть null (если ТО общего узла)
    val componentId: String,
    val fieldKey: String,        // ключ поля из шаблона
    val valueText: String?,      // для TEXT / NUMBER (храним строкой)
    val valueBool: Boolean?,     // для CHECKBOX
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT", по умолчанию "CRM" для старых данных
    @androidx.room.ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null // FK → users.id
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     */
    fun getOriginType(): ru.wassertech.core.auth.OriginType {
        return ru.wassertech.core.auth.OriginType.fromString(origin)
    }
}
