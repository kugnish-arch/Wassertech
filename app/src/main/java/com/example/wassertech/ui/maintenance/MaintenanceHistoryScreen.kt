package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaintenanceHistoryScreen(
    installationId: String?,
    onOpenSession: (sessionId: String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    val sessionsFlow: Flow<List<MaintenanceSessionEntity>> = remember(installationId) {
        if (installationId == null) db.sessionsDao().observeAllSessions()
        else db.sessionsDao().observeSessionsByInstallation(installationId)
    }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy (HH:mm)", Locale("ru"))
            .apply { timeZone = TimeZone.getDefault() }
    }

    data class ClientUi(val name: String, val isCorporate: Boolean)
    var installNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var clientBySession by remember { mutableStateOf<Map<String, ClientUi>>(emptyMap()) }

    LaunchedEffect(sessions) {
        withContext(Dispatchers.IO) {
            // Installations
            val instMap = sessions.mapNotNull { it.installationId }
                .distinct()
                .associateWith { id ->
                    db.hierarchyDao().getInstallation(id)?.name ?: id
                }

            // Clients
            val byClient = mutableMapOf<String, ClientUi>()
            for (s in sessions) {
                val siteId = s.siteId ?: continue
                val site = db.hierarchyDao().getSite(siteId) ?: continue
                val client = db.clientDao().getClient(site.clientId) ?: continue
                byClient[s.id] = ClientUi(name = client.name, isCorporate = client.isCorporate)
            }

            // back to main to update state
            withContext(Dispatchers.Main) {
                installNames = instMap
                clientBySession = byClient
            }
        }
    }

    if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Записей ТО пока нет")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { s ->
                val installationName = s.installationId?.let { installNames[it] } ?: "Установка"
                val startedText = remember(s.startedAtEpoch) {
                    dateFormatter.format(Date(s.startedAtEpoch))
                }
                val clientUi = clientBySession[s.id]

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable { onOpenSession(s.id) }
                ) {
                    ListItem(
                        headlineContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = if (clientUi?.isCorporate == true)
                                        Icons.Outlined.Business else Icons.Outlined.Person
                                    Icon(icon, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(clientUi?.name ?: "Клиент")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.SettingsApplications, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(installationName)
                                }
                                Text(startedText, style = MaterialTheme.typography.bodyMedium)
                            }
                        },
                        supportingContent = {
                            if (!s.notes.isNullOrBlank()) {
                                Text(s.notes!!)
                            }
                        }
                    )
                }
            }
        }
    }
}