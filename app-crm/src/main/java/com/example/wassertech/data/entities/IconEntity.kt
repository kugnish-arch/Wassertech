package ru.wassertech.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность иконки (icons).
 * Иконка может использоваться для объектов (SITE), установок (INSTALLATION), компонентов (COMPONENT) или для всех типов (ANY).
 * 
 * Иконки являются справочниками, синхронизируются с сервером через sync/pull.
 * Они не создаются и не редактируются в мобильных приложениях (origin всегда 'CRM').
 */
@Entity(
    tableName = "icons",
    foreignKeys = [
        ForeignKey(
            entity = IconPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("packId"),
        Index("code"),
        Index("entityType"),
        Index("isActive"),
        Index("origin"),
        Index("created_by_user_id")
    ]
)
data class IconEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "packId") val packId: String, // FK → icon_packs.id
    val code: String, // Уникальный код иконки в рамках пака
    val label: String, // Название иконки для отображения
    @ColumnInfo(name = "entityType") val entityType: String, // Тип сущности: "SITE", "INSTALLATION", "COMPONENT", "ANY"
    @ColumnInfo(name = "imageUrl") val imageUrl: String? = null, // URL изображения иконки (для загрузки с сервера)
    @ColumnInfo(name = "thumbnailUrl") val thumbnailUrl: String? = null, // URL миниатюры
    @ColumnInfo(name = "thumbnailLocalPath") val thumbnailLocalPath: String? = null, // Локальный путь к сохранённой миниатюре
    @ColumnInfo(name = "androidResName") val androidResName: String? = null, // Имя ресурса Android (например, "ic_site_default")
    @ColumnInfo(name = "isActive") val isActive: Boolean = true, // Активна ли иконка (для фильтрации)
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
    
    /**
     * Проверяет, подходит ли иконка для указанного типа сущности.
     */
    fun matchesEntityType(type: IconEntityType): Boolean {
        return entityType == "ANY" || entityType == type.name
    }
}

/**
 * Тип сущности для фильтрации иконок.
 */
enum class IconEntityType {
    SITE,
    INSTALLATION,
    COMPONENT,
    ANY
}


