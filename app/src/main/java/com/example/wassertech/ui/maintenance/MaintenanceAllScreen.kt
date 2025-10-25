package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.types.FieldType
import com.example.wassertech.viewmodel.HierarchyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class UiField(
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

data class ComponentGroup(
    val componentId: String,
    val componentName: String,
    val fields: List<UiField>,
    val expanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceAllScreen(
    installationId: String,
    onDone: () -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.get(context) }

    var installationName by remember { mutableStateOf("Установка") }
    var groups by remember { mutableStateOf(listOf<ComponentGroup>()) }

    LaunchedEffect(installationId) {
        withContext(Dispatchers.IO) {
            val inst = db.hierarchyDao().getInstallation(installationId)
            val comps = db.hierarchyDao().observeComponents(installationId).first()
            val tdao = db.templatesDao()
            val built = comps.map { c ->
                val template = tdao.getTemplateByType(c.type)
                val fields = if (template != null) tdao.getFieldsForTemplate(template.id) else emptyList()
                ComponentGroup(
                    componentId = c.id,
                    componentName = c.name,
                    fields = fields.map { f ->
                        UiField(
                            key = f.key, label = f.label, type = f.type,
                            unit = f.unit, min = f.min, max = f.max
                        )
                    },
                    expanded = false
                )
            }
            installationName = inst?.name ?: "Установка"
            groups = built
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("ТО: $installationName") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Нет компонентов или шаблонов для этой установки.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    itemsIndexed(groups, key = { _, g -> g.componentId }) { index, g ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column {
                                ListItem(
                                    headlineContent = { Text(g.componentName) },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            groups = groups.toMutableList().also { list ->
                                                val current = list[index]
                                                list[index] = current.copy(expanded = !current.expanded)
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (g.expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                                if (g.expanded) {
                                    Column(
                                        Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        g.fields.forEach { f ->
                                            when (f.type) {
                                                FieldType.CHECKBOX -> {
                                                    var checked by remember { mutableStateOf(f.boolValue) }
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(f.label)
                                                        Checkbox(checked = checked, onCheckedChange = { v -> checked = v; f.boolValue = v })
                                                    }
                                                }
                                                FieldType.NUMBER -> {
                                                    var text by remember { mutableStateOf(f.numberValue) }
                                                    OutlinedTextField(
                                                        value = text,
                                                        onValueChange = { v -> text = v; f.numberValue = v },
                                                        label = { Text(if (f.unit != null) "${f.label}, ${f.unit}" else f.label) },
                                                        singleLine = true
                                                    )
                                                }
                                                FieldType.TEXT -> {
                                                    var text by remember { mutableStateOf(f.textValue) }
                                                    OutlinedTextField(
                                                        value = text,
                                                        onValueChange = { v -> text = v; f.textValue = v },
                                                        label = { Text(f.label) },
                                                        singleLine = false,
                                                        minLines = 2
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Отмена") }
                    Button(onClick = { onDone() }, modifier = Modifier.weight(1f)) { Text("Сохранить") }
                }
            }
        }
    }
}
