package ru.wassertech.data

import androidx.room.TypeConverter
import ru.wassertech.data.types.ComponentType
import ru.wassertech.data.types.FieldType
import ru.wassertech.data.types.Severity
import ru.wassertech.data.types.SyncStatus

class Converters {
    @TypeConverter
    fun toComponentType(value: String?): ComponentType {
        if (value == null) return ComponentType.COMMON
        return runCatching { ComponentType.valueOf(value) }.getOrDefault(ComponentType.COMMON)
    }

    @TypeConverter
    fun fromComponentType(value: ComponentType?): String? = value?.name

    @TypeConverter
    fun toFieldType(value: String?): FieldType? =
        value?.let { runCatching { FieldType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromFieldType(value: FieldType?): String? = value?.name

    @TypeConverter
    fun toSeverity(value: String?): Severity? =
        value?.let { runCatching { Severity.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromSeverity(value: Severity?): String? = value?.name

    @TypeConverter
    fun toSyncStatus(value: Int?): SyncStatus {
        if (value == null) return SyncStatus.SYNCED
        return SyncStatus.fromInt(value)
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): Int = value?.value ?: SyncStatus.SYNCED.value
}