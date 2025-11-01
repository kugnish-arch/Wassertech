package com.example.wassertech.data.entities

import androidx.room.*

@Entity(
    tableName = "maintenance_values",
    indices = [
        Index("sessionId"),
        Index("siteId"),
        Index("installationId"),
        Index("componentId"),
        Index("fieldKey")
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
    val valueBool: Boolean?      // для CHECKBOX
)
