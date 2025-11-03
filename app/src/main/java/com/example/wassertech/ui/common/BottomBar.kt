package com.example.wassertech.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

data class BarAction(
    val icon: ImageVector,
    val contentDescription: String?,
    val onClick: () -> Unit,
)

@Composable
fun EditDoneBottomBar(
    isEditing: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    actions: List<BarAction> = emptyList(), // ← обратно добавили, с дефолтом
) {
    val cfg = LocalConfiguration.current
    // фиксируем ширину кнопки ≈ 1/3 экрана
    val buttonWidth = (cfg.screenWidthDp * 0.40f).dp

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая кнопка: Изменить/Готово с иконкой
            FilledTonalButton(
                onClick = if (isEditing) onDone else onEdit,
                modifier = Modifier.width(buttonWidth),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isEditing)
                        Color(0xFF26A69A) // активное редактирование
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isEditing)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Filled.Save else Icons.Filled.Edit,
                    contentDescription = if (isEditing) "Сохранить" else "Редактировать"
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Готово" else "Изменить")
            }

            Spacer(Modifier.width(12.dp))

            // Доп. действия (если переданы)
            if (actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.forEach { action ->
                        IconButton(onClick = action.onClick) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Статус справа
            Text(
                text = if (isEditing) "Редактирование" else "Просмотр",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
