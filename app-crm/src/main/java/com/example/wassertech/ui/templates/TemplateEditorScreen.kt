package ru.wassertech.ui.templates

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.types.FieldType
import ru.wassertech.util.Translit
import ru.wassertech.viewmodel.TemplatesViewModel
import ru.wassertech.sync.markUpdatedForSync
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.text.style.TextOverflow
import ru.wassertech.core.ui.theme.SegmentedButtonStyle

private const val TAG = "TemplateEditorScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TemplateEditorScreen(
    templateId: String,
    vm: TemplatesViewModel = viewModel(),
    onSaved: () -> Unit = {}
) {
    val fields by vm.fields.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getInstance(ctx) }

    var templateName by remember { mutableStateOf<String>("Шаблон") }
    var isHeadComponent by remember { mutableStateOf<Boolean>(false) }

    // Локальный порядок полей для drag-and-drop
    var localFieldOrder by remember(fields.size) {
        mutableStateOf(fields.map { it.id })
    }

    // Обновляем локальный порядок при изменении списка полей
    LaunchedEffect(fields.size, fields.map { it.id }.toSet()) {
        val currentIds = fields.map { it.id }
        val newOrder = localFieldOrder.filter { it in currentIds } +
                currentIds.filter { it !in localFieldOrder }
        if (newOrder != localFieldOrder) {
            localFieldOrder = newOrder
        }
    }

    LaunchedEffect(templateId) {
        vm.load(templateId)
        // заголовок шаблона
        withContext(Dispatchers.IO) {
            try {
                val template = db.componentTemplatesDao().getById(templateId)
                template?.let {
                    templateName = it.name
                    isHeadComponent = it.isHeadComponent
                }
            } catch (_: Throwable) {
            }
        }
    }

    // Состояния для тултипов (вынесены на уровень Scaffold)
    var globalShowHeadComponentInfo by remember { mutableStateOf(false) }
    var globalHeadComponentInfoPosition by remember { mutableStateOf<Offset?>(null) }
    var globalShowCharacteristicInfo by remember { mutableStateOf<Pair<String, Offset?>?>(null) }
    
    // Референсы для получения позиций иконок
    var headComponentInfoIconRef by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var characteristicInfoIconRefs by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        Log.d(TAG, "Сохранение шаблона: templateId=$templateId")
                        vm.saveAll(localFieldOrder)
                        // Сохраняем имя шаблона и флаг isHeadComponent, если они изменились
                        withContext(Dispatchers.IO) {
                            try {
                                val template = db.componentTemplatesDao().getById(templateId)
                                template?.let { currentTemplate ->
                                    if (currentTemplate.name != templateName || currentTemplate.isHeadComponent != isHeadComponent) {
                                        val updatedTemplate = currentTemplate.copy(
                                            name = templateName,
                                            isHeadComponent = isHeadComponent
                                        ).markUpdatedForSync()
                                        Log.d(TAG, "Обновление шаблона: templateId=$templateId, " +
                                                "oldName=${currentTemplate.name}, newName=$templateName, " +
                                                "oldIsHeadComponent=${currentTemplate.isHeadComponent}, newIsHeadComponent=$isHeadComponent, " +
                                                "dirtyFlag=${updatedTemplate.dirtyFlag}, syncStatus=${updatedTemplate.syncStatus}, " +
                                                "updatedAtEpoch=${updatedTemplate.updatedAtEpoch}")
                                        db.componentTemplatesDao().upsert(updatedTemplate)
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.e(TAG, "Ошибка при сохранении шаблона", e)
                            }
                        }
                        Toast.makeText(ctx, "Шаблон сохранён", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                containerColor = Color(0xFF4CAF50), // Чисто зеленый цвет
                shape = CircleShape
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Сохранить",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 200.dp // Дополнительный отступ внизу для клавиатуры
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Поле для редактирования имени шаблона и переключатель "Заглавный компонент"
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFFFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Название шаблона") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Переключатель "Заглавный компонент"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Заглавный компонент",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                var infoIconBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                                IconButton(
                                    onClick = { 
                                        infoIconBounds?.let { rect ->
                                            // Преобразуем координаты из корня в окно
                                            val position = Offset(rect.center.x, rect.top)
                                            globalHeadComponentInfoPosition = position
                                            globalShowHeadComponentInfo = true
                                        }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Информация",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .onGloballyPositioned { coordinates ->
                                                // Сохраняем позицию иконки для использования при клике
                                                val position = coordinates.localToWindow(Offset.Zero)
                                                val size = coordinates.size
                                                val bounds = androidx.compose.ui.geometry.Rect(
                                                    position.x,
                                                    position.y,
                                                    position.x + size.width,
                                                    position.y + size.height
                                                )
                                                infoIconBounds = bounds
                                                headComponentInfoIconRef = bounds
                                            },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isHeadComponent,
                                onCheckedChange = { checked ->
                                    isHeadComponent = checked
                                }
                            )
                        }
                    }
                }
                
            }

            // Список полей с использованием ReorderableLazyColumn
            item {
                if (localFieldOrder.isEmpty()) {
                    EmptyGroupPlaceholder(text = "Поля отсутствуют", indent = 16.dp)
                } else {
                    // Используем Column с ограниченной высотой для встраивания ReorderableLazyColumn
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 800.dp) // Ограничиваем максимальную высоту
                    ) {
                        ReorderableLazyColumn(
                            items = localFieldOrder,
                            onMove = { fromIndex, toIndex ->
                                // Всегда обновляем локальное состояние для корректного отображения перетаскивания
                                val mutable = localFieldOrder.toMutableList()
                                val item = mutable.removeAt(fromIndex)
                                mutable.add(toIndex, item)
                                localFieldOrder = mutable
                                // Изменения сохраняются в БД при сохранении шаблона
                            },
                            modifier = Modifier.fillMaxWidth(),
                            key = { it },
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) { fieldId, isDragging, reorderableState ->
                            val f = fields.find { it.id == fieldId } ?: return@ReorderableLazyColumn
                            val index = localFieldOrder.indexOf(fieldId)
                            
                            FieldCard(
                                field = f,
                                index = index,
                                vm = vm,
                                reorderableState = reorderableState,
                                onInfoClick = { rect ->
                                    val position = Offset(rect.center.x, rect.top)
                                    globalShowCharacteristicInfo = Pair(f.id, position)
                                }
                            )
                        }
                    }
                }
            }

            // Кнопка "Добавить поле" внизу списка
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { vm.addField() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935), // Красный цвет
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Добавить поле")
                    }
                }
            }
        }
        
        // Тултипы поверх всего контента
        if (globalShowHeadComponentInfo && globalHeadComponentInfoPosition != null) {
            InfoTooltip(
                text = "Заглавный компонент - в готовом отчёте будет занимать всю ширину листа, а не 1/3 как обычные компоненты. Если заглавный элемент окажется в самом начале или конце компонента, то под него будет выделен отдельный визуальный раздел.",
                anchorPosition = globalHeadComponentInfoPosition,
                onDismiss = { 
                    globalShowHeadComponentInfo = false
                    globalHeadComponentInfoPosition = null
                }
            )
        }
        
        globalShowCharacteristicInfo?.let { (fieldId, position) ->
            if (position != null) {
                InfoTooltip(
                    text = "Характеристика - постоянное свойство компонента (паспортные данные железа). Оно будет сохраняться и выводиться в некоторых документах, но его значение не меняется при проведении обслуживания. Если переключатель выключен, поле относится к чек-листу ТО (параметры обслуживания).",
                    anchorPosition = position,
                    onDismiss = { 
                        globalShowCharacteristicInfo = null
                    }
                )
            }
        }
    }
}

/**
 * Компонент подсказки с жёлтым фоном, иконкой лампочки и крестиком для закрытия
 * Позиционируется относительно anchorPosition (координаты иконки Info)
 */
@Composable
private fun InfoTooltip(
    text: String,
    anchorPosition: Offset?,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Позиция и размеры контейнера Box в window координатах
    var containerPosition by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    // Состояние для размеров тултипа
    var tooltipSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    // Вычисляем относительную позицию тултипа с учетом границ экрана
    val tooltipOffset = remember(anchorPosition, containerPosition, containerSize, tooltipSize, screenWidthPx, density) {
        if (anchorPosition == null || containerPosition == null || tooltipSize.width == 0f || tooltipSize.height == 0f) {
            Offset.Zero
        } else {
            val tooltipWidthPx = tooltipSize.width
            
            // Вычисляем относительную позицию относительно контейнера
            val relativeX = anchorPosition.x - containerPosition!!.x
            val relativeY = anchorPosition.y - containerPosition!!.y
            
            // Горизонтальная позиция: центрируем относительно иконки, но не выходим за края
            val desiredX = relativeX - tooltipWidthPx / 2f // Центрируем относительно иконки
            
            // Границы контейнера с учетом отступов от краев экрана
            val minX = 16f // Минимальная позиция с учетом отступа от левого края контейнера
            val maxX = if (containerSize.width > 0f) {
                containerSize.width - tooltipWidthPx - 16f // Максимальная позиция с учетом отступа от правого края контейнера
            } else {
                screenWidthPx - tooltipWidthPx - 16f // Fallback на размеры экрана
            }
            
            val x = when {
                desiredX < minX -> minX
                desiredX > maxX -> maxX
                else -> desiredX
            }
            
            // Вертикальная позиция: верхняя грань тултипа на уровне иконки
            val y = relativeY
            
            Offset(x, y)
        }
    }

    // Используем Box с fillMaxSize для верхнего слоя
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f) // Верхний слой
            .onGloballyPositioned { coordinates ->
                // Получаем позицию и размеры контейнера в window координатах
                containerPosition = coordinates.localToWindow(Offset.Zero)
                containerSize = androidx.compose.ui.geometry.Size(
                    width = coordinates.size.width.toFloat(),
                    height = coordinates.size.height.toFloat()
                )
            },
        contentAlignment = Alignment.TopStart
    ) {
        // Полупрозрачный фон - кликабельный для закрытия
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        ) {}

        // Карточка с подсказкой - позиционируется относительно иконки
        if (anchorPosition != null && containerPosition != null) {
            Card(
                modifier = Modifier
                    .offset(
                        x = with(density) { tooltipOffset.x.toDp() },
                        y = with(density) { tooltipOffset.y.toDp() }
                    )
                    .widthIn(max = 300.dp)
                    .onGloballyPositioned { coordinates ->
                        tooltipSize = androidx.compose.ui.geometry.Size(
                            width = coordinates.size.width.toFloat(),
                            height = coordinates.size.height.toFloat()
                        )
                    }
                    .clickable(enabled = false) {}, // Предотвращаем закрытие при клике на карточку
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEB3B).copy(alpha = 0.85f) // Жёлтый фон с прозрачностью 85%
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок с иконкой лампочки и крестиком
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFF57F17), // Тёмно-жёлтый для иконки
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Подсказка",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1A1A1A) // Тёмный текст на жёлтом фоне
                            )
                        }
                        IconButton(
                            onClick = {
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = Color(0xFF1A1A1A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Текст подсказки
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        } else {
            // anchorPosition == null или containerPosition == null, карточка не показывается
        }
    }
}

// Компонент карточки поля для редактирования
@Composable
private fun FieldCard(
    field: TemplatesViewModel.UiField,
    index: Int,
    vm: TemplatesViewModel,
    reorderableState: ReorderableState?,
    onInfoClick: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    var characteristicInfoIconBounds by remember(field.id) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Заголовок карточки: ручка для перетаскивания, номер, переключатель "Характеристика" справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть: ручка и номер
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Перетащить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (reorderableState != null) {
                                    Modifier.detectReorder(reorderableState)
                                } else {
                                    Modifier
                                }
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Правая часть: переключатель "Характеристика" с иконкой info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Характеристика",
                            style = MaterialTheme.typography.labelMedium
                        )
                        IconButton(
                            onClick = {
                                characteristicInfoIconBounds?.let { rect ->
                                    onInfoClick(rect)
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Информация",
                                modifier = Modifier
                                    .size(16.dp)
                                    .onGloballyPositioned { coordinates ->
                                        val position = coordinates.localToWindow(Offset.Zero)
                                        val size = coordinates.size
                                        val bounds = androidx.compose.ui.geometry.Rect(
                                            position.x,
                                            position.y,
                                            position.x + size.width,
                                            position.y + size.height
                                        )
                                        characteristicInfoIconBounds = bounds
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = field.isCharacteristic,
                        onCheckedChange = { checked ->
                            vm.update(field.id) { it.copy(isCharacteristic = checked) }
                        }
                    )
                }
            }

            // Имя (label)
            OutlinedTextField(
                value = field.label,
                onValueChange = { newLabel ->
                    val prevAuto = Translit.ruToEnKey(field.label)
                    val looksAuto = field.key.isBlank() ||
                            field.key == prevAuto ||
                            field.key.startsWith("field_") ||
                            (field.key.any { it.isDigit() } && field.key.length >= 12)
                    val newAuto = Translit.ruToEnKey(newLabel)
                    vm.update(field.id) {
                        it.copy(
                            label = newLabel,
                            key = if (looksAuto) newAuto else it.key
                        )
                    }
                },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Сегменты типа поля и корзина справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    var selected by remember(field.id, field.type) { mutableStateOf(field.type) }
                    SegmentedButton(
                        selected = selected == FieldType.TEXT,
                        onClick = {
                            selected = FieldType.TEXT
                            vm.setType(field.id, FieldType.TEXT)
                        },
                        shape = SegmentedButtonStyle.getShape(index = 0, count = 3),
                        label = {
                            Text(
                                "TXT",
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    SegmentedButton(
                        selected = selected == FieldType.CHECKBOX,
                        onClick = {
                            selected = FieldType.CHECKBOX
                            vm.setType(field.id, FieldType.CHECKBOX)
                        },
                        shape = SegmentedButtonStyle.getShape(index = 1, count = 3),
                        label = {
                            Text(
                                "CHK",
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    SegmentedButton(
                        selected = selected == FieldType.NUMBER,
                        onClick = {
                            selected = FieldType.NUMBER
                            vm.setType(field.id, FieldType.NUMBER)
                        },
                        shape = SegmentedButtonStyle.getShape(index = 2, count = 3),
                        label = {
                            Text(
                                "NUM",
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
                IconButton(onClick = { vm.remove(field.id) }) {
                    Icon(
                        imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                        contentDescription = "Удалить"
                    )
                }
            }

            // Только для NUMBER — ед. изм. + Min/Max
            if (field.type == FieldType.NUMBER) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = field.unit.orEmpty(),
                        onValueChange = { new -> vm.update(field.id) { it.copy(unit = new) } },
                        label = { Text("Ед. изм.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = field.min.orEmpty(),
                        onValueChange = { new -> vm.update(field.id) { it.copy(min = new) } },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = field.max.orEmpty(),
                        onValueChange = { new -> vm.update(field.id) { it.copy(max = new) } },
                        label = { Text("Max") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
    }
}

