package ru.wassertech.client.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.res.painterResource
import ru.wassertech.core.ui.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.wassertech.feature.reports.ReportsDatabaseProvider
import ru.wassertech.feature.reports.InstallationData
import ru.wassertech.feature.reports.MaintenanceSessionData
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InstallationsReportsScreen(
    databaseProvider: ReportsDatabaseProvider,
    onNavigateToSessionDetail: (String) -> Unit = {}
) {
    var installations by remember { mutableStateOf<List<InstallationData>>(emptyList()) }
    var expandedInstallations by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var clientName by remember { mutableStateOf<String?>(null) }
    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
    }
    
    // Загружаем установки
    LaunchedEffect(databaseProvider) {
        loading = true
        try {
            installations = withContext(Dispatchers.IO) {
                databaseProvider.getAllNonArchivedInstallations()
            }
            // Получаем название клиента для первой установки
            if (installations.isNotEmpty()) {
                clientName = withContext(Dispatchers.IO) {
                    databaseProvider.getClientNameByInstallationId(installations.first().id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("InstallationsReportsScreen", "Error loading installations", e)
        } finally {
            loading = false
        }
    }
    
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (installations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.installation),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Установки отсутствуют",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Добавьте установки в системе",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(horizontal = padding.calculateStartPadding(LocalLayoutDirection.current)),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Плашка с названием клиента
                if (clientName != null) {
                    item {
                        ClientHeaderBar(clientName = clientName!!)
                    }
                }
                
                // Отступ перед списком установок
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(installations, key = { it.id }) { installation ->
                    val isExpanded = expandedInstallations.contains(installation.id)
                    
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        InstallationCard(
                            installation = installation,
                            isExpanded = isExpanded,
                            databaseProvider = databaseProvider,
                            dateFormatter = dateFormatter,
                            onCardClick = {
                                expandedInstallations = if (isExpanded) {
                                    expandedInstallations - installation.id
                                } else {
                                    expandedInstallations + installation.id
                                }
                            },
                            onSessionClick = { sessionId ->
                                onNavigateToSessionDetail(sessionId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientHeaderBar(clientName: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ru.wassertech.core.ui.theme.ClientsGroupExpandedBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                clientName,
                color = ru.wassertech.core.ui.theme.ClientsGroupExpandedText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(
            color = ru.wassertech.core.ui.theme.ClientsGroupBorder,
            thickness = 1.dp
        )
    }
}

@Composable
private fun InstallationCard(
    installation: InstallationData,
    isExpanded: Boolean,
    databaseProvider: ReportsDatabaseProvider,
    dateFormatter: SimpleDateFormat,
    onCardClick: () -> Unit,
    onSessionClick: (String) -> Unit
) {
    var maintenanceSessions by remember(installation.id) { mutableStateOf<List<MaintenanceSessionData>>(emptyList()) }
    
    // Загружаем сессии обслуживания для этой установки
    LaunchedEffect(installation.id, isExpanded, databaseProvider) {
        if (isExpanded) {
            databaseProvider.observeSessionsByInstallation(installation.id)
                .collect { sessions ->
                    maintenanceSessions = sessions
                }
        }
        // При сворачивании очищаем список (Flow автоматически отменяется при изменении ключей LaunchedEffect)
        if (!isExpanded) {
            maintenanceSessions = emptyList()
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Заголовок карточки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Иконка установки
                Icon(
                    painter = painterResource(R.drawable.installation),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = androidx.compose.ui.graphics.Color.Unspecified
                )
                
                // Название установки
                Text(
                    text = installation.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                // Иконка разворачивания
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Разворачиваемый список записей обслуживания
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    if (maintenanceSessions.isEmpty()) {
                        Text(
                            text = "Записи обслуживания отсутствуют",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        maintenanceSessions.forEach { session ->
                            MaintenanceSessionItem(
                                session = session,
                                dateFormatter = dateFormatter,
                                onClick = { onSessionClick(session.id) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceSessionItem(
    session: MaintenanceSessionData,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Начало: ${dateFormatter.format(Date(session.startedAtEpoch))}",
                style = MaterialTheme.typography.bodyMedium
            )
            val finishedAt = session.finishedAtEpoch
            if (finishedAt != null) {
                Text(
                    text = "Окончание: ${dateFormatter.format(Date(finishedAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val technician = session.technician
            if (technician != null) {
                Text(
                    text = "Техник: $technician",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val notes = session.notes
            if (notes != null && notes.isNotBlank()) {
                Text(
                    text = "Заметки: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

