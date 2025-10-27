package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.wassertech.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryScreen(
    installationId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val sessionsFlow = remember(installationId) { db.sessionsDao().observeSessionsByInstallation(installationId) }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<List<ObservationDetail>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История ТО") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Записей ТО пока нет")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { s ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(sdf.format(Date(s.startedAtEpoch))) },
                            supportingContent = { Text(s.notes ?: "") },
                            modifier = Modifier.clickable { selectedSessionId = s.id }
                        )
                    }
                }
            }
        }
    }

    if (selectedSessionId != null) {
        val sid = selectedSessionId!!
        LaunchedEffect(sid) {
            val obs = db.sessionsDao().getObservations(sid)
            val detailsList = mutableListOf<ObservationDetail>()
            for (o in obs) {
                val comp = db.hierarchyDao().getComponent(o.componentId)
                val componentName = comp?.name ?: o.componentId
                val value = when {
                    o.valueText != null -> o.valueText
                    o.valueNumber != null -> o.valueNumber.toString()
                    o.valueBool != null -> if (o.valueBool) "Да" else "Нет"
                    else -> ""
                }
                detailsList.add(ObservationDetail(o.componentId, componentName, o.fieldKey, value))
            }
            details = detailsList
        }
        AlertDialog(
            onDismissRequest = { selectedSessionId = null },
            confirmButton = { TextButton(onClick = { selectedSessionId = null }) { Text("Закрыть") } },
            title = { Text("Детали ТО") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.forEach { d ->
                        Text("• ${d.componentName}: ${d.fieldKey} — ${d.valueText}")
                    }
                }
            }
        )
    }
}