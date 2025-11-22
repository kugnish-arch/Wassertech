package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность пака иконок (icon_packs).
 * Пак иконок - это группа связанных иконок (например, "UI Icons", "Water Treatment", "Boiler").
 * 
 * Иконки и паки являются справочниками, синхронизируются с сервером через sync/pull.
 * Они не создаются и не редактируются в мобильных приложениях (origin всегда 'CRM').
 */
@Entity(
    tableName = "icon_packs",
    indices = [
        Index("code"),
        Index("origin"),
        Index("created_by_user_id")
    ]
)
data class IconPackEntity(
    @PrimaryKey val id: String,
    val code: String, // Уникальный код пака (например, "ui", "water_treatment", "boiler")
    val name: String, // Название пака (например, "UI Icons", "Water Treatment")
    val description: String? = null, // Описание пака
    val folder: String? = null, // Подпапка для иконок (например, "water", "car", "home")
    @ColumnInfo(name = "isBuiltin") val isBuiltin: Boolean = false, // Встроенный пак (нельзя удалить)
    @ColumnInfo(name = "isPremium") val isPremium: Boolean = false, // Премиум пак (требует подписки)
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT", по умолчанию "CRM"
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null, // FK → users.id
    // Временные метки
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     */
    fun getOriginType(): ru.wassertech.core.auth.OriginType {
        return ru.wassertech.core.auth.OriginType.fromString(origin)
    }
}





