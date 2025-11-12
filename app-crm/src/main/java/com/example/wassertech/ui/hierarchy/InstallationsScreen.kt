package ru.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import ru.wassertech.core.ui.theme.SegmentedButtonStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationsScreen(
    siteId: String,
    onOpenInstallation: (String) -> Unit,
    onStartMaintenance: (String, String, String) -> Unit, // siteId, installationId, installationName
    onOpenMaintenanceHistory: (String) -> Unit, // installationId
    vm: HierarchyViewModel = viewModel()
) {
    val list by vm.installations(siteId).collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        floatingActionButton = {
            Column {
                FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { itn ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(itn.name, style = MaterialTheme.typography.titleMedium)
                            
                            // Сегментированные кнопки для ТО
                            var selectedButton by remember(itn.id) { mutableStateOf<Int?>(null) }
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SegmentedButton(
                                    selected = selectedButton == 0,
                                    onClick = {
                                        selectedButton = 0
                                        onStartMaintenance(siteId, itn.id, itn.name)
                                    },
                                    shape = SegmentedButtonStyle.getShape(index = 0, count = 2),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Провести ТО")
                                }
                                SegmentedButton(
                                    selected = selectedButton == 1,
                                    onClick = {
                                        selectedButton = 1
                                        onOpenMaintenanceHistory(itn.id)
                                    },
                                    shape = SegmentedButtonStyle.getShape(index = 1, count = 2),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("История ТО")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новая установка") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addInstallation(siteId, n)
                        name = TextFieldValue("")
                        showAdd = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
