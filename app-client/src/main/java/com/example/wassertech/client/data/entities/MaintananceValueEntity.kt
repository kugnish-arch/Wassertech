package ru.wassertech.client.data.entities

import androidx.room.*

@Entity(
    tableName = "maintenance_values",
    indices = [
        Index("sessionId"),
        Index("siteId"),
        Index("installationId"),
        Index("componentId"),
        Index("fieldKey"),
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
