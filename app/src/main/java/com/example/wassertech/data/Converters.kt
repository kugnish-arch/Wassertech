package com.example.wassertech.data

import androidx.room.TypeConverter
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.data.types.FieldType
import com.example.wassertech.data.types.Severity

class Converters {
    @TypeConverter
    fun toComponentType(value: String?): ComponentType? =
        value?.let { runCatching { ComponentType.valueOf(it) }.getOrNull() }

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
}