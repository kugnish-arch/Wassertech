package com.example.wassertech.data.types

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toComponentType(name: String?): ComponentType? = name?.let { ComponentType.valueOf(it) }
    @TypeConverter
    fun fromComponentType(t: ComponentType?): String? = t?.name

    @TypeConverter
    fun toFieldType(name: String?): FieldType? = name?.let { FieldType.valueOf(it) }
    @TypeConverter
    fun fromFieldType(t: FieldType?): String? = t?.name

    @TypeConverter
    fun toSeverity(name: String?): Severity? = name?.let { Severity.valueOf(it) }
    @TypeConverter
    fun fromSeverity(t: Severity?): String? = t?.name
}
