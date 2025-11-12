package ru.wassertech.ui.templates.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun MaintenanceTypeToggle(
    isForMaintenance: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = !isForMaintenance,
            onClick = { onChange(false) },
            label = { Text("Характеристика") }
        )
        FilterChip(
            selected = isForMaintenance,
            onClick = { onChange(true) },
            label = { Text("Параметр ТО") }
        )
    }
}