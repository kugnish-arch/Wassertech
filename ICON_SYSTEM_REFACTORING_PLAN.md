# План рефакторинга системы иконок

## Текущее состояние

### Что уже хорошо организовано:
1. **IconResolver** и **IconImage** - уже в `core:ui` ✅
2. **IconPickerDialog** - уже в `core:ui` ✅
3. **IconEntityType** - уже в `core:ui` ✅
4. **IconPackUiData** и **IconUiData** - уже в `core:ui` ✅
5. Экран стартовой синхронизации - уже использует общие компоненты из `core:ui` ✅

### Что нужно рефакторить:
1. **IconRepository** - находится только в `app-crm`, но нужен и в `app-client`
2. **IconPickerUiState** - определен в `HierarchyViewModel` (app-crm), должен быть в общем модуле
3. Логика загрузки иконок дублируется или отсутствует в `app-client`
4. Room-сущности дублируются (это нормально, но нужно убедиться в идентичности)

## Новая архитектура

### Структура модулей:

```
core:ui
  └── icons/
      ├── IconResolver.kt (уже есть) ✅
      ├── IconEntityType.kt (уже есть) ✅
      └── IconPickerUiState.kt (новый) ⬅️ вынести из HierarchyViewModel

core:ui
  └── components/
      ├── IconPickerDialog.kt (уже есть) ✅
      ├── IconGrid.kt (уже есть) ✅
      ├── IconPackCard.kt (уже есть) ✅
      └── IconPackBadgeRow.kt (уже есть) ✅

core:icons (новый модуль или пакет в существующем)
  └── IconDataSource.kt (интерфейс)
  └── IconService.kt (базовая логика, не зависящая от Room)

app-crm/data/repository/
  └── IconRepository.kt (реализация IconDataSource)
  
app-client/data/repository/
  └── IconRepository.kt (реализация IconDataSource)
```

### Интерфейс IconDataSource

```kotlin
interface IconDataSource {
    suspend fun getLocalIconPath(iconId: String): String?
    suspend fun isIconDownloaded(iconId: String): Boolean
    suspend fun downloadIconImage(iconId: String, imageUrl: String): Result<File>
    suspend fun downloadPackImages(packId: String, onProgress: ((Int, Int) -> Unit)?): Result<Unit>
    suspend fun getPackSyncStatus(packId: String): IconPackSyncStatusEntity?
    suspend fun clearPackImages(packId: String)
}
```

### Вынос IconPickerUiState

Переместить из `HierarchyViewModel` в `core:ui.icons`:

```kotlin
package ru.wassertech.core.ui.icons

data class IconPickerUiState(
    val packs: List<IconPackUiData>,
    val iconsByPack: Map<String, List<IconUiData>>
)
```

## План выполнения

### Этап 1: Подготовка
1. ✅ Создать план рефакторинга
2. Вынести `IconPickerUiState` в `core:ui.icons`
3. Создать интерфейс `IconDataSource` в новом пакете `core:icons` (или в `core:ui.icons`)

### Этап 2: Рефакторинг IconRepository
1. Создать интерфейс `IconDataSource`
2. Рефакторить `IconRepository` в `app-crm` для реализации интерфейса
3. Создать `IconRepository` в `app-client` с той же реализацией
4. Обновить все использования `IconRepository` на `IconDataSource`

### Этап 3: Обновление ViewModel
1. Обновить `HierarchyViewModel` для использования `IconDataSource` и общего `IconPickerUiState`
2. Проверить другие ViewModel, использующие иконки
3. Убедиться, что `app-client` имеет аналогичную функциональность

### Этап 4: Проверка и тестирование
1. Проверить работу IconPickerDialog в app-crm
2. Проверить работу IconPickerDialog в app-client
3. Проверить загрузку иконок по URL
4. Проверить отображение иконок на всех экранах

### Этап 5: Документация
1. Обновить `ICON_SYSTEM_DOCUMENTATION.md`
2. Создать `ICON_SYSTEM_REFACTORING_RESULT.md`

## Зависимости модулей

```
app-crm ──┐
          ├──> core:ui (IconResolver, IconPickerDialog, IconPickerUiState)
          └──> core:icons (IconDataSource) ──> data (IconRepository реализация)

app-client ──┐
             ├──> core:ui (IconResolver, IconPickerDialog, IconPickerUiState)
             └──> core:icons (IconDataSource) ──> data (IconRepository реализация)

core:ui ──> core:icons (только интерфейсы, без реализации)
```

## Риски и ограничения

1. **Room-сущности** остаются в отдельных модулях (это нормально для монорепозитория)
2. **IconRepository** зависит от конкретной БД (AppDatabase), поэтому реализация остается в app-модулях
3. Нужно убедиться, что `app-client` имеет все необходимые DAO для работы с иконками

## Обратная совместимость

- Все существующие экраны должны продолжить работать
- API `IconPickerDialog` не меняется
- API `IconResolver.IconImage` не меняется
- Только внутренняя структура репозиториев меняется

