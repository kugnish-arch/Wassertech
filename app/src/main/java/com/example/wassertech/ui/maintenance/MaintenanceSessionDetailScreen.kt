package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaintenanceSessionDetailScreen(
    sessionId: String
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    var session by remember { mutableStateOf<MaintenanceSessionEntity?>(null) }
    var header by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<DetailComponentGroup>>(emptyList()) }
    var clientName by remember { mutableStateOf<String>("Клиент") }
    var isCorporate by remember { mutableStateOf<Boolean>(false) }
    var installationName by remember { mutableStateOf<String>("Установка") }
    var dateTimeText by remember { mutableStateOf<String>("") }

    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy (HH:mm)", Locale("ru"))
            .apply { timeZone = TimeZone.getDefault() }
    }

    LaunchedEffect(sessionId) {
        withContext(Dispatchers.IO) {
            val s = db.sessionsDao().getSessionById(sessionId)
            session = s

            // Names and header
            val instName = s?.installationId?.let { id ->
                db.hierarchyDao().getInstallation(id)?.name ?: "Установка"
            } ?: "Установка"
            installationName = instName
            val dt = s?.startedAtEpoch?.let { dateFormatter.format(Date(it)) } ?: ""
            dateTimeText = dt
            header = "$instName — $dt"

            // Client
            val siteId = s?.siteId
            if (siteId != null) {
                val site = db.hierarchyDao().getSite(siteId)
                val client = site?.let { db.clientDao().getClient(it.clientId) }
                if (client != null) {
                    clientName = client.name
                    isCorporate = client.isCorporate
                }
            }

            // Values grouped by component, with field label resolution
            val values = db.sessionsDao().getValuesForSession(sessionId)
            val byComponent = values.groupBy { it.componentId }
            val list = mutableListOf<DetailComponentGroup>()
            for ((componentId, vals) in byComponent) {
                val comp = db.hierarchyDao().getComponent(componentId)
                val compName = comp?.name ?: componentId

                // Map fieldKey -> label via template, if available
                val labels: Map<String, String> = comp?.templateId?.let { tid ->
                    try {
                        db.templatesDao().getMaintenanceFieldsForTemplate(tid)
                            .associate { it.key to (it.label ?: it.key) }
                    } catch (e: Exception) { emptyMap() }
                } ?: emptyMap()

                val rows = vals.map { v ->
                    val rawKey = v.fieldKey ?: ""
                    val label = labels[rawKey] ?: rawKey.substringBefore('_', rawKey)
                    val valueText = when {
                        v.valueText != null -> v.valueText
                        v.valueBool != null -> if (v.valueBool == true) "Да" else "Нет"
                        else -> ""
                    } ?: ""
                    DetailFieldRow(fieldLabel = label, value = valueText)
                }.sortedBy { it.fieldLabel.lowercase(Locale.getDefault()) }
                list.add(DetailComponentGroup(componentName = compName, rows = rows))
            }
            groups = list.sortedBy { it.componentName.lowercase(Locale.getDefault()) }
        }
    }

    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Сессия не найдена")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header card (secondaryContainer)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = if (isCorporate) Icons.Outlined.Business else Icons.Outlined.Person
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(clientName, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SettingsApplications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(installationName, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(dateTimeText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(groups) { g ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(g.componentName) },
                    supportingContent = {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            g.rows.forEach { row ->
                                Text("• ${row.fieldLabel}: ${row.value}")
                            }
                        }
                    }
                )
            }
        }
    }
}

// Renamed to avoid clash with MaintenanceAllScreen.kt
private data class DetailComponentGroup(
    val componentName: String,
    val rows: List<DetailFieldRow>
)

private data class DetailFieldRow(
    val fieldLabel: String,
    val value: String
)