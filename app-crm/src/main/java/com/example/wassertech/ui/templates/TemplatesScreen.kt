package ru.wassertech.ui.templates

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ComponentTemplateEntity
import ru.wassertech.sync.SafeDeletionHelper
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.core.screens.templates.TemplatesScreenShared
import ru.wassertech.core.screens.templates.ui.TemplatesUiState
import ru.wassertech.core.screens.templates.ui.TemplateItemUi

private const val TAG = "TemplatesScreen"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesScreen(
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onOpenTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.componentTemplatesDao() }
    val scope = rememberCoroutineScope()

    val templatesFlow: Flow<List<ComponentTemplateEntity>> =
        remember { dao.observeAll() }
    val templates by templatesFlow.collectAsState(initial = emptyList())

    // Диалог «Создать шаблон»
    var showCreate by remember { mutableStateOf(false) }

    // Локальный порядок для live-перестановки
    // Инициализируем из отсортированных шаблонов
    var localOrder by remember { 
        mutableStateOf<List<String>>(emptyList()) 
    }
    
    // Синхронизируем локальный порядок при изменении шаблонов или состояния редактирования
    LaunchedEffect(isEditing, templates) {
        if (templates.isEmpty()) {
            localOrder = emptyList()
        } else if (!isEditing) {
            // В обычном режиме показываем только неархивные, отсортированные по sortOrder
            val nonArchived = templates.filter { !it.isArchived }
            val sorted = nonArchived.sortedWith(
                compareBy<ComponentTemplateEntity> { it.sortOrder }
                    .thenBy { it.name.lowercase() }
            )
            localOrder = sorted.map { it.id }
        } else {
            // В режиме редактирования показываем все, отсортированные по sortOrder
            val sorted = templates.sortedWith(
                compareBy<ComponentTemplateEntity> { it.sortOrder }
                    .thenBy { it.name.lowercase() }
            )
            localOrder = sorted.map { it.id }
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
        onTemplateClick = onOpenTemplate,
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
                val entity = ComponentTemplateEntity(
                    id = id,
                    name = title,
                    category = null,
                    defaultParamsJson = null,
                    sortOrder = nextOrder
                ).markCreatedForSync(context)
                Log.d(TAG, "Создание шаблона: id=$id, name=$title, " +
                        "dirtyFlag=${entity.dirtyFlag}, syncStatus=${entity.syncStatus}, " +
                        "createdAtEpoch=${entity.createdAtEpoch}, updatedAtEpoch=${entity.updatedAtEpoch}")
                dao.upsert(entity)
                showCreate = false
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
            scope.launch(Dispatchers.IO) {
                try {
                    val components = db.hierarchyDao().getComponentsByTemplate(template.id)
                    if (components.isNotEmpty()) {
                        Log.e(TAG, "Шаблон используется в компонентах, но диалог не показал это")
                        return@launch
                    }
                    Log.d(TAG, "Удаление шаблона: id=${template.id}, name=${template.name}")
                    SafeDeletionHelper.deleteComponentTemplate(db, template.id)
                    Log.d(TAG, "Шаблон удален и помечен для синхронизации: id=${template.id}")
                    deleteDialogState = null
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Не удалось удалить шаблон: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка удаления шаблона", e)
                    deleteDialogState = null
                }
            }
        },
        installationsUsingTemplate = installationsUsingTemplate,
        isLoadingDeleteCheck = isLoadingDeleteCheck
    )
}
