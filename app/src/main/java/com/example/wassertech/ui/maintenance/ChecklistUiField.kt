package com.example.wassertech.ui.maintenance

import com.example.wassertech.data.types.FieldType

data class ChecklistUiField(
    val key: String,
    val label: String,
    val type: FieldType,
    val unit: String?,
    val min: Double?,
    val max: Double?,
    var boolValue: Boolean = false,
    var numberValue: String = "",
    var textValue: String = ""
)