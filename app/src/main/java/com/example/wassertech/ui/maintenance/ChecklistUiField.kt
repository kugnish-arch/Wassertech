
package com.example.wassertech.ui.maintenance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import com.example.wassertech.data.types.FieldType

@Stable
class ChecklistUiField(
    val key: String,
    val label: String,
    val type: FieldType,
    val unit: String?,
    val min: Double?,
    val max: Double?
) {
    var boolValue: Boolean by mutableStateOf(false)
    var numberValue: String by mutableStateOf("")
    var textValue: String by mutableStateOf("")
}
