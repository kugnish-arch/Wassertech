package ru.wassertech.ui.maintenance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import ru.wassertech.crm.R
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.MaintenanceSessionEntity
import ru.wassertech.sync.SafeDeletionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryScreen(
    installationId: String?,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onBack: () -> Unit = {}, // Не используется, но оставлен для совместимости API
    onOpenSession: (String) -> Unit,
    onOpenReports: () -> Unit = {},
    onClearFilter: (() -> Unit)? = null // Не используется, но оставлен для совместимости API
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val sdf = remember { SimpleDateFormat("d MMMM yyyy (HH:mm)", Locale.forLanguageTag("ru")) }

    val sessionsFlow: Flow<List<MaintenanceSessionEntity>> = remember(installationId) {
        if (installationId == null) db.sessionsDao().observeAllSessions()
        else db.sessionsDao().observeSessionsByInstallation(installationId)
    }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    var sessionDisplay by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var selectedSessions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessions) {
        withContext(Dispatchers.IO) {
            val result = sessions.map { s ->
                val site = s.siteId?.let { db.hierarchyDao().getSite(it) }
                val inst = s.installationId?.let { db.hierarchyDao().getInstallation(it) }
                val client = site?.let { db.clientDao().getClient(it.clientId) }

                val clientName = client?.name ?: "Без клиента"
                val siteName = site?.name ?: "Без объекта"
                val instName = inst?.name ?: "Без установки"
                val dateText =
                    s.startedAtEpoch?.let { epoch -> sdf.format(Date(epoch)) } ?: "Неизвестно"
                Triple(clientName, "$siteName — $instName", dateText)
            }
            sessionDisplay = result
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), // Убираем белое поле внизу
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (isEditing && selectedSessions.isNotEmpty()) {
                // Красный FAB для удаления выбранных записей
                FloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = Color(0xFFD32F2F), // Красный цвет
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                        contentDescription = "Удалить выбранные",
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (!isEditing && selectedSessions.isEmpty()) {
                // Черный FAB для отчётов (только вне режима редактирования)
                FloatingActionButton(
                    onClick = onOpenReports,
                    containerColor = Color(0xFF1E1E1E), // Черный цвет
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.document_pdf),
                        contentDescription = "Отчёты ТО",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp) // Уменьшаем отступ от апбара
                .padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = padding.calculateStartPadding(layoutDirection))
        ) {
            if (sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Записей ТО пока нет")
                }
            } else {
                Spacer(Modifier.height(8.dp)) // Отступ от подзаголовка до контента
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessionDisplay.zip(sessions)) { (display, s) ->
                        val isSelected = selectedSessions.contains(s.id)
                        // Карточка с новым стилем: белый фон, тонкая граница, скругление 12dp, elevation 1dp
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isEditing) {
                                        // В режиме редактирования - переключаем выбор
                                        selectedSessions = if (isSelected) {
                                            selectedSessions - s.id
                                        } else {
                                            selectedSessions + s.id
                                        }
                                    } else {
                                        // Обычный режим - открываем сессию
                                        onOpenSession(s.id)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Вертикальная линия-акцент слева (цвет #E53935)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(Color(0xFFE53935))
                                )
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 12.dp,
                                            end = 12.dp,
                                            top = 12.dp,
                                            bottom = 12.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Чекбокс выбора - только в режиме редактирования
                                    if (isEditing) {
                                        IconButton(
                                            onClick = {
                                                selectedSessions = if (isSelected) {
                                                    selectedSessions - s.id
                                                } else {
                                                    selectedSessions + s.id
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                                contentDescription = if (isSelected) "Снять выделение" else "Выбрать",
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.Business,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                display.first,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.HomeWork,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                display.second,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                display.third,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    // Иконка навигации - только вне режима редактирования
                                    if (!isEditing) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                                            contentDescription = "Открыть детали",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Конец записей
                    item {
                        Spacer(Modifier.height(12.dp)) // Отступ от последней карточки
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center // Выравнивание по центру
                        ) {
                            Text(
                                text = "Конец записей...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Диалог подтверждения удаления выбранных сессий
        if (showDeleteDialog && selectedSessions.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить выбранные записи?") },
                text = {
                    Text("Вы уверены, что хотите удалить ${selectedSessions.size} запис${if (selectedSessions.size == 1) "ь" else "ей"} ТО?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val count = selectedSessions.size
                                    val sessionsToDelete =
                                        selectedSessions.toList() // Копируем список перед очисткой
                                    withContext(Dispatchers.IO) {
                                        sessionsToDelete.forEach { sessionId ->
                                            SafeDeletionHelper.deleteSession(db, sessionId)
                                        }
                                    }
                                    selectedSessions = emptySet()
                                    showDeleteDialog = false
                                    snackbarHostState.showSnackbar(
                                        message = "Удалено записей: $count",
                                        duration = SnackbarDuration.Short
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        message = "Ошибка при удалении: ${e.message}",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}