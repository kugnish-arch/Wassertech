package ru.wassertech.client.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ComponentTemplateEntity
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu

@Composable
fun TemplatesScreen(
    onOpenTemplate: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.componentTemplatesDao() }
    
    val templatesFlow = remember { dao.observeAll() }
    val templates by templatesFlow.collectAsState(initial = emptyList())
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (templates.isEmpty()) {
            AppEmptyState(
                icon = Icons.Filled.Lightbulb,
                title = "Шаблоны компонентов",
                description = "Шаблоны компонентов будут отображаться здесь после синхронизации с сервером."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = templates,
                    key = { it.id }
                ) { template ->
                    EntityRowWithMenu(
                        title = template.name,
                        subtitle = template.category?.takeIf { it.isNotBlank() },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lightbulb,
                                contentDescription = "Шаблон",
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        isEditMode = false,
                        isArchived = template.isArchived == true,
                        onClick = { onOpenTemplate(template.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

