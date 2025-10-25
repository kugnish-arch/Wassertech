package com.example.wassertech.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun toComponentType(v: String?) = v?.let { ComponentType.valueOf(it) }
    @TypeConverter fun fromComponentType(v: ComponentType?) = v?.name
    @TypeConverter fun toFieldType(v: String?) = v?.let { FieldType.valueOf(it) }
    @TypeConverter fun fromFieldType(v: FieldType?) = v?.name
    @TypeConverter fun toSeverity(v: String?) = v?.let { Severity.valueOf(it) }
    @TypeConverter fun fromSeverity(v: Severity?) = v?.name
}
