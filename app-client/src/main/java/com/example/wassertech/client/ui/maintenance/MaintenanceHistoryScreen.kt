package ru.wassertech.client.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.MaintenanceSessionEntity
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaintenanceHistoryScreen(
    installationId: String,
    onOpenSession: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val sdf = remember { SimpleDateFormat("d MMMM yyyy (HH:mm)", Locale.forLanguageTag("ru")) }
    
    val sessionsFlow = remember(installationId) {
        db.sessionsDao().observeSessionsByInstallation(installationId)
    }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (sessions.isEmpty()) {
            AppEmptyState(
                icon = Icons.Filled.History,
                title = "История обслуживания",
                description = "Записей технического обслуживания пока нет."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    val dateText = session.startedAtEpoch?.let { epoch ->
                        sdf.format(Date(epoch))
                    } ?: "Неизвестно"
                    
                    EntityRowWithMenu(
                        title = dateText,
                        subtitle = session.technician?.takeIf { it.isNotBlank() },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = "Сессия ТО",
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        isEditMode = false,
                        isArchived = false,
                        onClick = { onOpenSession(session.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    )
                }
            }
        }
    }
}





