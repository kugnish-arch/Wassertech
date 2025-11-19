package ru.wassertech.client.ui.templates

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ComponentTemplateEntity
import ru.wassertech.client.ui.common.LocalEditingState
import ru.wassertech.core.screens.templates.TemplatesScreenShared
import ru.wassertech.core.screens.templates.ui.TemplatesUiState
import ru.wassertech.core.screens.templates.ui.TemplateItemUi

private const val TAG = "TemplatesScreen"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesScreen(
    onOpenTemplate: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.componentTemplatesDao() }
    val scope = rememberCoroutineScope()
    
    // Получаем состояние редактирования из CompositionLocal
    val editingState = LocalEditingState.current
    val isEditing = editingState?.isEditing ?: false
    val onToggleEdit = editingState?.onToggle
    
    val templatesFlow: kotlinx.coroutines.flow.Flow<List<ComponentTemplateEntity>> =
        remember { dao.observeAll() }
    val templates by templatesFlow.collectAsState(initial = emptyList())

    // Диалог «Создать шаблон»
    var showCreate by remember { mutableStateOf(false) }

    // Локальный порядок для live-перестановки
    var localOrder by remember(templates, isEditing) { 
        mutableStateOf(templates.map { it.id }) 
    }
    
    // Синхронизируем локальный порядок при изменении состояния редактирования
    LaunchedEffect(isEditing, templates) {
        if (!isEditing) {
            localOrder = templates.map { it.id }
        } else {
            val allTemplatesOrdered = templates.sortedWith(
                compareBy<ComponentTemplateEntity> { it.sortOrder }
                    .thenBy { it.name.lowercase() }
            )
            localOrder = allTemplatesOrdered.map { it.id }
        }
    }
    
    // Сохранение порядка при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty()) {
            scope.launch {
                Log.d(TAG, "Сохранение порядка шаблонов: количество=${localOrder.size}")
                localOrder.forEachIndexed { index, id ->
                    dao.setSortOrder(id, index)
                }
            }
        }
    }

    // Диалог подтверждения удаления
    var deleteDialogState by remember { mutableStateOf<ComponentTemplateEntity?>(null) }
    var installationsUsingTemplate by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingDeleteCheck by remember { mutableStateOf(false) }

    // Базовый порядок — как в БД (sortOrder ↑, затем name для стабильности)
    val dbOrdered = remember(templates) {
        templates.sortedWith(
            compareBy<ComponentTemplateEntity> { it.sortOrder }
                .thenBy { it.name.lowercase() }
        )
    }

    // Преобразуем в UI State
    val uiState = remember(dbOrdered, localOrder, isEditing) {
        TemplatesUiState(
            templates = dbOrdered.map { template ->
                TemplateItemUi(
                    id = template.id,
                    name = template.name,
                    category = template.category,
                    isArchived = template.isArchived,
                    sortOrder = template.sortOrder
                )
            },
            localOrder = localOrder,
            isEditing = isEditing
        )
    }

    // Загружаем список установок, использующих шаблон, для диалога удаления
    LaunchedEffect(deleteDialogState?.id) {
        val template = deleteDialogState ?: return@LaunchedEffect
        isLoadingDeleteCheck = true
        withContext(Dispatchers.IO) {
            val components = db.hierarchyDao().getComponentsByTemplate(template.id)
            val installationIds = components.map { it.installationId }.distinct()
            val installations = installationIds.mapNotNull { id ->
                db.hierarchyDao().getInstallationNow(id)
            }
            installationsUsingTemplate = installations.map { "${it.name} (ID: ${it.id})" }
            isLoadingDeleteCheck = false
        }
    }

    TemplatesScreenShared(
        state = uiState,
        onTemplateClick = { templateId ->
            Log.d(TAG, "=== КЛИК ПО ШАБЛОНУ ===")
            Log.d(TAG, "templateId=$templateId")
            Log.d(TAG, "Вызов onOpenTemplate с templateId=$templateId")
            onOpenTemplate(templateId)
            Log.d(TAG, "onOpenTemplate вызван, ожидаем навигацию")
        },
        onCreateTemplateClick = { showCreate = true },
        onTemplateArchive = { templateId ->
            scope.launch {
                Log.d(TAG, "Архивирование шаблона: id=$templateId")
                dao.setArchived(templateId, true)
            }
        },
        onTemplateRestore = { templateId ->
            scope.launch {
                Log.d(TAG, "Разархивирование шаблона: id=$templateId")
                dao.setArchived(templateId, false)
            }
        },
        onTemplateDelete = { templateId ->
            val template = templates.find { it.id == templateId }
            if (template != null) {
                deleteDialogState = template
            }
        },
        onTemplatesReordered = { newOrder ->
            localOrder = newOrder
        },
        onToggleEdit = onToggleEdit,
        showCreateDialog = showCreate,
        onCreateDialogDismiss = { showCreate = false },
        onCreateDialogConfirm = { title ->
            scope.launch {
                val id = UUID.randomUUID().toString()
                val nextOrder = (templates.maxOfOrNull { it.sortOrder } ?: -1) + 1
                val currentTime = System.currentTimeMillis()
                val entity = ComponentTemplateEntity(
                    id = id,
                    name = title,
                    category = null,
                    defaultParamsJson = null,
                    sortOrder = nextOrder,
                    isArchived = false,
                    createdAtEpoch = currentTime,
                    updatedAtEpoch = currentTime
                )
                Log.d(TAG, "Создание шаблона: id=$id, name=$title, " +
                        "createdAtEpoch=${entity.createdAtEpoch}, updatedAtEpoch=${entity.updatedAtEpoch}")
                dao.upsert(entity)
                showCreate = false
                kotlinx.coroutines.delay(150)
                Log.d(TAG, "Вызов onOpenTemplate с id=$id")
                onOpenTemplate(id)
            }
        },
        showDeleteDialog = deleteDialogState != null,
        deleteDialogTemplate = deleteDialogState?.let { template ->
            TemplateItemUi(
                id = template.id,
                name = template.name,
                category = template.category,
                isArchived = template.isArchived,
                sortOrder = template.sortOrder
            )
        },
        onDeleteDialogDismiss = { deleteDialogState = null },
        onDeleteDialogConfirm = {
            val template = deleteDialogState ?: return@TemplatesScreenShared
            scope.launch {
                try {
                    val components = db.hierarchyDao().getComponentsByTemplate(template.id)
                    if (components.isNotEmpty()) {
                        Log.e(TAG, "Шаблон используется в компонентах, но диалог не показал это")
                        return@launch
                    }
                    Log.d(TAG, "Архивирование шаблона для удаления: id=${template.id}, name=${template.name}")
                    val currentTime = System.currentTimeMillis()
                    dao.setArchived(template.id, true, currentTime)
                    val updatedTemplate = template.copy(
                        isArchived = true,
                        archivedAtEpoch = template.archivedAtEpoch ?: currentTime,
                        updatedAtEpoch = currentTime,
                        dirtyFlag = true,
                        syncStatus = 1
                    )
                    dao.upsert(updatedTemplate)
                    Log.d(TAG, "Шаблон архивирован: id=${template.id}, isArchived=${updatedTemplate.isArchived}, dirtyFlag=${updatedTemplate.dirtyFlag}")
                    deleteDialogState = null
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка удаления шаблона", e)
                }
            }
        },
        installationsUsingTemplate = installationsUsingTemplate,
        isLoadingDeleteCheck = isLoadingDeleteCheck
    )
}

