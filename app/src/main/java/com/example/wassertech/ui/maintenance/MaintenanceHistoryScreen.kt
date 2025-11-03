package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaintenanceHistoryScreen(
    installationId: String?,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val sdf = remember { SimpleDateFormat("d MMMM yyyy (HH:mm)", Locale("ru")) }

    val sessionsFlow: Flow<List<MaintenanceSessionEntity>> = remember(installationId) {
        if (installationId == null) db.sessionsDao().observeAllSessions()
        else db.sessionsDao().observeSessionsByInstallation(installationId)
    }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    var sessionDisplay by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }

    LaunchedEffect(sessions) {
        withContext(Dispatchers.IO) {
            val result = sessions.map { s ->
                val site = s.siteId?.let { db.hierarchyDao().getSite(it) }
                val inst = s.installationId?.let { db.hierarchyDao().getInstallation(it) }
                val client = site?.let { db.clientDao().getClient(it.clientId) }

                val clientName = client?.name ?: "Без клиента"
                val siteName = site?.name ?: "Без объекта"
                val instName = inst?.name ?: "Без установки"
                val dateText = s.startedAtEpoch?.let { sdf.format(Date(it)) } ?: "Неизвестно"
                Triple(clientName, "$siteName — $instName", dateText)
            }
            sessionDisplay = result
        }
    }

    if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Записей ТО пока нет")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessionDisplay.zip(sessions)) { (display, s) ->
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSession(s.id) }
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(display.first, style = MaterialTheme.typography.titleMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.HomeWork, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            Text(display.second, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            Text(display.third, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
