package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    indices = [
        Index("isArchived")
    ]
)
data class SiteEntity(
    @PrimaryKey val id: String,
    val clientId: String, // TODO: Переименовать в ownerClientId после миграции БД
    val name: String,
    val address: String? = null,
    val orderIndex: Int = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    // TODO: Добавить поле origin: OriginType после миграции БД
    // Временно используем clientId как ownerClientId
    val ownerClientId: String? = null, // Если null, используем clientId
    val origin: String? = null // "CRM" или "CLIENT", преобразуется в OriginType
) {
    /**
     * Получает ownerClientId (использует ownerClientId если есть, иначе clientId для обратной совместимости).
     * Переименовано из getOwnerClientId() чтобы избежать конфликта с Room.
     */
    fun effectiveOwnerClientId(): String = ownerClientId ?: clientId
    
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     * Переименовано из getOrigin() чтобы избежать конфликта с Room (Room генерирует getOrigin() для поля origin).
     */
    fun getOriginType(): ru.wassertech.client.auth.OriginType {
        return ru.wassertech.client.auth.OriginType.fromString(origin)
    }
}