package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.wassertech.client.data.types.ComponentType

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: ComponentType,
    val orderIndex: Int,
    @ColumnInfo(name = "templateId") val templateId: String? = null,
    // TODO: Добавить поля ownerClientId и origin после миграции БД
    val ownerClientId: String? = null, // FK → clients.id
    val origin: String? = null // "CRM" или "CLIENT"
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     * Переименовано из getOrigin() чтобы избежать конфликта с Room (Room генерирует getOrigin() для поля origin).
     */
    fun getOriginType(): ru.wassertech.client.auth.OriginType {
        return ru.wassertech.client.auth.OriginType.fromString(origin)
    }
}
