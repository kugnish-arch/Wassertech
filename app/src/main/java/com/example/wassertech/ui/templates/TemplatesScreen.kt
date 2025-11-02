
package com.example.wassertech.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun TemplatesScreen(
    onOpenTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val templatesFlow: Flow<List<ChecklistTemplateEntity>> = remember { db.templatesDao().observeAllTemplates() }
    val templates by templatesFlow.collectAsState(initial = emptyList())

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 12.dp
            )
        ) {
            items(templates, key = { it.id }) { t ->
                ElevatedCard {
                    ListItem(
                        headlineContent = { Text(t.title) },
                        supportingContent = { Text(t.componentType.name) },
                        modifier = Modifier.clickable { onOpenTemplate(t.id) }
                    )
                }
            }
        }
    }
}
