
package com.example.wassertech.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(templateId: String) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Конструктор: " + templateId) }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("Здесь будет конструктор полей (TEXT / NUMBER / CHECKBOX) для шаблона.")
            Text("В следующем шаге подключим DAO/VM и CRUD полей.")
        }
    }
}
