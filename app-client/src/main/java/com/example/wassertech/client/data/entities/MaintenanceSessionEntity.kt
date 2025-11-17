package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_sessions",
    indices = [
        Index("origin"),
        Index("created_by_user_id")
    ]
)
data class MaintenanceSessionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val installationId: String? = null,
    val startedAtEpoch: Long,
    val finishedAtEpoch: Long? = null,
    val technician: String? = null,
    val notes: String? = null,
    val synced: Boolean = false,
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT", по умолчанию "CRM" для старых данных
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null // FK → users.id
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     */
    fun getOriginType(): ru.wassertech.core.auth.OriginType {
        return ru.wassertech.core.auth.OriginType.fromString(origin)
    }
}

