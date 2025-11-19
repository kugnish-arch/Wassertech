package ru.wassertech.core.screens.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.core.ui.theme.SegmentedButtonStyle
import ru.wassertech.core.screens.templates.ui.TemplateEditorUiState
import ru.wassertech.core.screens.templates.ui.TemplateFieldUi

/**
 * Shared-экран для редактирования шаблона компонента.
 * Используется как в app-crm, так и в app-client.
 * 
 * @param state UI State с данными шаблона и полей
 * @param onNameChange Коллбек при изменении имени шаблона
 * @param onIsHeadComponentChange Коллбек при изменении флага "Заглавный компонент"
 * @param onFieldLabelChange Коллбек при изменении label поля (принимает fieldId, newLabel, autoKey)
 * @param onFieldKeyChange Коллбек при изменении key поля (принимает fieldId, newKey)
 * @param onFieldTypeChange Коллбек при изменении типа поля (принимает fieldId, newType)
 * @param onFieldIsCharacteristicChange Коллбек при изменении флага "Характеристика" (принимает fieldId, isCharacteristic)
 * @param onFieldUnitChange Коллбек при изменении единицы измерения (принимает fieldId, newUnit)
 * @param onFieldMinChange Коллбек при изменении минимального значения (принимает fieldId, newMin)
 * @param onFieldMaxChange Коллбек при изменении максимального значения (принимает fieldId, newMax)
 * @param onFieldRemove Коллбек при удалении поля (принимает fieldId)
 * @param onFieldAdd Коллбек при добавлении нового поля
 * @param onFieldsReordered Коллбек при изменении порядка полей (принимает новый порядок fieldId)
 * @param onSaveClick Коллбек при сохранении шаблона
 * @param translitFunction Функция транслитерации для автоматического генерации ключа (опционально)
 * @param externalPaddingValues Внешние отступы (например, от topBar/bottomBar), используемые для позиционирования контента и FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreenShared(
    state: TemplateEditorUiState,
    onNameChange: (String) -> Unit,
    onIsHeadComponentChange: (Boolean) -> Unit,
    onFieldLabelChange: (String, String, String) -> Unit, // fieldId, newLabel, autoKey
    onFieldKeyChange: (String, String) -> Unit = { _, _ -> }, // fieldId, newKey
    onFieldTypeChange: (String, String) -> Unit, // fieldId, type (as String: "TEXT", "CHECKBOX", "NUMBER")
    onFieldIsCharacteristicChange: (String, Boolean) -> Unit,
    onFieldUnitChange: (String, String) -> Unit,
    onFieldMinChange: (String, String) -> Unit,
    onFieldMaxChange: (String, String) -> Unit,
    onFieldRemove: (String) -> Unit,
    onFieldAdd: () -> Unit,
    onFieldsReordered: ((List<String>) -> Unit)? = null,
    onSaveClick: () -> Unit,
    translitFunction: ((String) -> String)? = null,
    externalPaddingValues: PaddingValues? = null
) {
    // Состояния для тултипов (вынесены на уровень Scaffold)
    var globalShowHeadComponentInfo by remember { mutableStateOf(false) }
    var globalHeadComponentInfoPosition by remember { mutableStateOf<Offset?>(null) }
    var globalShowCharacteristicInfo by remember { mutableStateOf<Pair<String, Offset?>?>(null) }

    // Вычисляем отступы для контента и FAB
    val topPadding = externalPaddingValues?.calculateTopPadding() ?: 0.dp
    val bottomPadding = externalPaddingValues?.calculateBottomPadding() ?: 0.dp
    val fabBottomPadding = bottomPadding + 24.dp // Отступ для FAB над bottomBar (увеличен)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSaveClick,
                containerColor = Color(0xFF4CAF50),
                shape = CircleShape,
                modifier = if (fabBottomPadding > 0.dp) {
                    Modifier.padding(bottom = fabBottomPadding)
                } else {
                    Modifier
                }
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Сохранить",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        var infoIconBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = topPadding + 4.dp, // Учитываем TopBar (уменьшен отступ)
                bottom = bottomPadding + 80.dp // Учитываем bottomBar и FAB
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
                            value = state.templateName,
                            onValueChange = onNameChange,
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
                                IconButton(
                                    onClick = { 
                                        infoIconBounds?.let { rect ->
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
                                                val position = coordinates.localToWindow(Offset.Zero)
                                                val size = coordinates.size
                                                val bounds = androidx.compose.ui.geometry.Rect(
                                                    position.x,
                                                    position.y,
                                                    position.x + size.width,
                                                    position.y + size.height
                                                )
                                                infoIconBounds = bounds
                                            },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = state.isHeadComponent,
                                onCheckedChange = onIsHeadComponentChange
                            )
                        }
                    }
                }
            }

            // Список полей с использованием ReorderableLazyColumn
            item {
                if (state.localFieldOrder.isEmpty()) {
                    EmptyGroupPlaceholder(text = "Поля отсутствуют", indent = 16.dp)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 800.dp)
                    ) {
                        ReorderableLazyColumn(
                            items = state.localFieldOrder,
                            onMove = { fromIndex, toIndex ->
                                val mutable = state.localFieldOrder.toMutableList()
                                val item = mutable.removeAt(fromIndex)
                                mutable.add(toIndex, item)
                                onFieldsReordered?.invoke(mutable)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            key = { it },
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) { fieldId, isDragging, reorderableState ->
                            val field = state.fields.find { it.id == fieldId } ?: return@ReorderableLazyColumn
                            val index = state.localFieldOrder.indexOf(fieldId)
                            
                            FieldCard(
                                field = field,
                                index = index,
                                reorderableState = reorderableState,
                                onLabelChange = { newLabel ->
                                    val autoKey = translitFunction?.invoke(newLabel) ?: ""
                                    onFieldLabelChange(field.id, newLabel, autoKey)
                                },
                                onTypeChange = { newType -> onFieldTypeChange(field.id, newType) }, // newType is String
                                onIsCharacteristicChange = { isChar -> onFieldIsCharacteristicChange(field.id, isChar) },
                                onUnitChange = { newUnit -> onFieldUnitChange(field.id, newUnit) },
                                onMinChange = { newMin -> onFieldMinChange(field.id, newMin) },
                                onMaxChange = { newMax -> onFieldMaxChange(field.id, newMax) },
                                onRemove = { onFieldRemove(field.id) },
                                onInfoClick = { rect ->
                                    val position = Offset(rect.center.x, rect.top)
                                    globalShowCharacteristicInfo = Pair(field.id, position)
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
                        onClick = onFieldAdd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
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
    
    var containerPosition by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var tooltipSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    val tooltipOffset = remember(anchorPosition, containerPosition, containerSize, tooltipSize, screenWidthPx, density) {
        if (anchorPosition == null || containerPosition == null || tooltipSize.width == 0f || tooltipSize.height == 0f) {
            Offset.Zero
        } else {
            val tooltipWidthPx = tooltipSize.width
            val relativeX = anchorPosition.x - containerPosition!!.x
            val relativeY = anchorPosition.y - containerPosition!!.y
            val desiredX = relativeX - tooltipWidthPx / 2f
            val minX = 16f
            val maxX = if (containerSize.width > 0f) {
                containerSize.width - tooltipWidthPx - 16f
            } else {
                screenWidthPx - tooltipWidthPx - 16f
            }
            val x = when {
                desiredX < minX -> minX
                desiredX > maxX -> maxX
                else -> desiredX
            }
            val y = relativeY
            Offset(x, y)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .onGloballyPositioned { coordinates ->
                containerPosition = coordinates.localToWindow(Offset.Zero)
                containerSize = androidx.compose.ui.geometry.Size(
                    width = coordinates.size.width.toFloat(),
                    height = coordinates.size.height.toFloat()
                )
            },
        contentAlignment = Alignment.TopStart
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        ) {}

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
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEB3B).copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Подсказка",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
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
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }
}

// Компонент карточки поля для редактирования
@Composable
private fun FieldCard(
    field: TemplateFieldUi,
    index: Int,
    reorderableState: ReorderableState?,
    onLabelChange: (String) -> Unit,
    onTypeChange: (String) -> Unit, // type as String: "TEXT", "CHECKBOX", "NUMBER"
    onIsCharacteristicChange: (Boolean) -> Unit,
    onUnitChange: (String) -> Unit,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onRemove: () -> Unit,
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
                        onCheckedChange = onIsCharacteristicChange
                    )
                }
            }

            // Имя (label)
            OutlinedTextField(
                value = field.label,
                onValueChange = onLabelChange,
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
                    SegmentedButton(
                        selected = field.type == "TEXT",
                        onClick = { onTypeChange("TEXT") },
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
                        selected = field.type == "CHECKBOX",
                        onClick = { onTypeChange("CHECKBOX") },
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
                        selected = field.type == "NUMBER",
                        onClick = { onTypeChange("NUMBER") },
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
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                        contentDescription = "Удалить"
                    )
                }
            }

            // Только для NUMBER — ед. изм. + Min/Max
            if (field.type == "NUMBER") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = field.unit.orEmpty(),
                        onValueChange = onUnitChange,
                        label = { Text("Ед. изм.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = field.min.orEmpty(),
                        onValueChange = onMinChange,
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = field.max.orEmpty(),
                        onValueChange = onMaxChange,
                        label = { Text("Max") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
    }
}

