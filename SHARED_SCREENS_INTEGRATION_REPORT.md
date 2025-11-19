# Отчёт: Интеграция Shared-экранов иерархии

## Выполненные задачи

### 1. Создание shared-экранов в core:screens

#### 1.1. Обновление UI State классов (`HierarchyUiState.kt`)

**Файл**: `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/ui/HierarchyUiState.kt`

**Изменения**:
- Добавлены поля для иконок в `SiteItemUi`, `InstallationItemUi`, `ComponentItemUi`:
  - `iconAndroidResName: String?` - имя ресурса Android
  - `iconCode: String?` - код иконки для fallback
  - `iconLocalImagePath: String?` - локальный путь к изображению
- Добавлены флаги прав доступа:
  - `canReorder: Boolean` - для drag-and-drop
  - `canStartMaintenance: Boolean`, `canViewMaintenanceHistory: Boolean` - для установок
  - `templateName: String?` - для компонентов
- Расширены UI State классы:
  - `ClientSitesUiState`: добавлены `includeArchived`, `canEditClient`, `error`
  - `SiteInstallationsUiState`: добавлены `clientName`, `includeArchived`, `canEditSite`, `error`
  - `InstallationComponentsUiState`: добавлены `siteName`, `clientName`, `includeArchived`, `canEditInstallation`, `error`

#### 1.2. ClientSitesScreenShared

**Файл**: `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/ClientSitesScreenShared.kt`

**Реализовано**:
- Полноценный shared-экран для списка объектов клиента
- Поддержка drag-and-drop через `ReorderableLazyColumn`
- Отображение иконок через `IconResolver.IconImage`
- Режим редактирования с кнопками архивации, восстановления, удаления, изменения иконки
- Диалог подтверждения удаления
- FAB для добавления объектов (показывается только если `canAddSite == true`)
- Автоматическое включение режима редактирования при начале перетаскивания

**Компоненты**:
- `ClientSitesScreenShared` - основной composable
- `SiteRowShared` - компонент строки объекта

#### 1.3. SiteInstallationsScreenShared

**Файл**: `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/SiteInstallationsScreenShared.kt`

**Реализовано**:
- Shared-экран для списка установок объекта
- Поддержка drag-and-drop
- Отображение иконок
- Режим редактирования
- Сегментированные кнопки для ТО ("Провести ТО", "История ТО")
- Диалог подтверждения удаления
- FAB для добавления установок

**Компоненты**:
- `SiteInstallationsScreenShared` - основной composable
- `InstallationRowShared` - компонент строки установки

#### 1.4. InstallationComponentsScreenShared

**Файл**: `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/InstallationComponentsScreenShared.kt`

**Реализовано**:
- Shared-экран для списка компонентов установки
- Поддержка drag-and-drop
- Отображение иконок
- Режим редактирования
- Отображение типа и шаблона компонента
- Диалог подтверждения удаления
- FAB для добавления компонентов

**Компоненты**:
- `InstallationComponentsScreenShared` - основной composable
- `ComponentRowShared` - компонент строки компонента

### 2. Вспомогательные функции для app-crm

#### 2.1. HierarchyUiStateMapper

**Файл**: `app-crm/src/main/java/com/example/wassertech/ui/hierarchy/HierarchyUiStateMapper.kt`

**Реализовано**:
- Extension функции для преобразования Entity в ItemUi:
  - `SiteEntity.toSiteItemUi()` - преобразование объекта
  - `InstallationEntity.toInstallationItemUi()` - преобразование установки
  - `ComponentEntity.toComponentItemUi()` - преобразование компонента
- Загрузка локальных путей к изображениям иконок через `IconRepository`
- Установка всех прав доступа в `true` для CRM (ADMIN/ENGINEER имеют полный доступ)

### 3. Интеграция в app-crm

#### 3.1. InstallationsScreen.kt

**Файл**: `app-crm/src/main/java/com/example/wassertech/ui/hierarchy/InstallationsScreen.kt`

**Реализовано**:
- Интегрирован `SiteInstallationsScreenShared`
- Загрузка иконок для всех установок
- Преобразование `InstallationEntity` → `InstallationItemUi` через `HierarchyUiStateMapper`
- Создание `SiteInstallationsUiState` с данными объекта
- Все коллбеки подключены к существующим методам ViewModel:
  - `onInstallationClick` → `onOpenInstallation`
  - `onAddInstallationClick` → открывает диалог добавления
  - `onInstallationArchive` → `vm.archiveInstallation()`
  - `onInstallationRestore` → `vm.restoreInstallation()`
  - `onInstallationDelete` → `vm.deleteInstallation()`
  - `onInstallationsReordered` → `vm.reorderInstallations()`
  - `onStartMaintenance` → `onStartMaintenance`
  - `onOpenMaintenanceHistory` → `onOpenMaintenanceHistory`

**Статус**: ✅ Интегрировано

#### 3.2. ComponentsScreen.kt и ClientDetailScreen.kt

**Статус**: ⏳ Требует доработки

**Примечание**: `ComponentsScreen.kt` и `ClientDetailScreen.kt` имеют сложную структуру с множеством дополнительных функций (IconPickerDialog, редактирование, вложенные установки). Для полной интеграции требуется дополнительная работа по сохранению всей функциональности.

### 4. Интеграция в app-client с правами

#### 4.1. ClientHierarchyUiStateMapper

**Файл**: `app-client/src/main/java/com/example/wassertech/client/ui/hierarchy/ClientHierarchyUiStateMapper.kt`

**Реализовано**:
- Extension функции для преобразования Entity в ItemUi с учётом прав:
  - `SiteEntity.toSiteItemUi()` - проверяет `canViewSite()`, фильтрует недоступные объекты
  - `InstallationEntity.toInstallationItemUi()` - проверяет `canViewInstallation()`, фильтрует недоступные установки
  - `ComponentEntity.toComponentItemUi()` - проверяет `canViewComponent()`, фильтрует недоступные компоненты
- Использует `HierarchyPermissionChecker` для проверки всех прав доступа:
  - `canEdit*`, `canDelete*`, `canChangeIcon*`, `canCreate*`
- Загружает локальные пути к изображениям иконок через `IconRepository`
- Возвращает `null` для элементов, которые нельзя просматривать (фильтрация на уровне mapper'а)

**Статус**: ✅ Создан

#### 4.2. Интеграция в SitesScreen.kt, SiteDetailScreen.kt, ComponentsScreen.kt

**Статус**: ⏳ Требует доработки

**Примечание**: Для полной интеграции shared-экранов в app-client требуется:
1. Получить `UserMembershipEntity` для текущего пользователя
2. Преобразовать в `List<UserMembershipInfo>`
3. Использовать `ClientHierarchyUiStateMapper` для преобразования Entity → ItemUi
4. Фильтровать элементы, которые нельзя просматривать
5. Создать UI State и передать в shared-экраны

**Частично реализовано**: Автоматика user_membership добавлена в места создания/удаления сущностей.

### 5. Автоматическая работа с user_membership в app-client

#### 5.1. UserMembershipHelper

**Файл**: `app-client/src/main/java/com/example/wassertech/client/data/UserMembershipHelper.kt`

**Реализовано**:
- `createSiteMembership()` - создаёт membership для объекта после его создания
- `createInstallationMembership()` - создаёт membership для установки после её создания
- `archiveInstallationMemberships()` - архивирует все membership записи для установки
- `archiveSiteMemberships()` - архивирует все membership записи для объекта
- Проверка на дубликаты перед созданием
- Установка `dirtyFlag = true`, `syncStatus = 1` для синхронизации

#### 5.2. Интеграция в места создания/удаления

**Файл**: `app-client/src/main/java/com/example/wassertech/client/ui/sites/SitesScreen.kt`

**Реализовано**:
- После создания `SiteEntity` (строка 424) вызывается `UserMembershipHelper.createSiteMembership()`
- Создаётся membership с `scope = "SITE"`, `targetId = site.id`, `userId = currentUser.userId`

**Файл**: `app-client/src/main/java/com/example/wassertech/client/ui/sites/SiteDetailScreen.kt`

**Реализовано**:
- После создания `InstallationEntity` (строка 475) вызывается `UserMembershipHelper.createInstallationMembership()`
- Создаётся membership с `scope = "INSTALLATION"`, `targetId = installation.id`, `userId = currentUser.userId`
- После удаления `InstallationEntity` (строка 568) вызывается `UserMembershipHelper.archiveInstallationMemberships()`
- Все membership записи для удалённой установки архивируются

**Статус**: ✅ Реализовано

## Архитектура

### Модули и зависимости

```
core:screens
├── Зависит от:
│   ├── core:ui (ReorderableLazyColumn, IconResolver, AppEmptyState)
│   ├── core:auth (UserSession, OriginType - косвенно через UI state)
│   └── core:network (только для DTO, если нужно)
└── Экспортирует:
    ├── ClientSitesScreenShared
    ├── SiteInstallationsScreenShared
    ├── InstallationComponentsScreenShared
    └── UI State классы (SiteItemUi, InstallationItemUi, ComponentItemUi, etc.)

app-crm
├── Использует core:screens
├── HierarchyUiStateMapper для преобразования Entity → ItemUi
└── ViewModel'и готовят UI State и передают в shared-экраны

app-client
├── Использует core:screens
├── Использует core:auth (HierarchyPermissionChecker)
├── ViewModel'и готовят UI State с учётом прав доступа
└── Автоматически создаёт/архивирует user_membership при создании/удалении сущностей
```

### Поток данных

1. **app-crm**:
   - ViewModel получает `SiteEntity` из БД
   - Преобразует в `SiteItemUi` через `HierarchyUiStateMapper` (все права = true)
   - Создаёт `ClientSitesUiState`
   - Передаёт в `ClientSitesScreenShared`

2. **app-client**:
   - ViewModel получает `SiteEntity` из БД
   - Получает текущего пользователя и `UserMembershipEntity`
   - Фильтрует через `HierarchyPermissionChecker.canViewSite()`
   - Преобразует в `SiteItemUi` с правильными флагами прав
   - Создаёт `ClientSitesUiState`
   - Передаёт в `ClientSitesScreenShared`

## Тестирование

### Сценарии для проверки

1. **CRM**:
   - [ ] Открытие экрана клиента → отображаются все объекты
   - [ ] Drag-and-drop объектов → порядок сохраняется
   - [ ] Редактирование объекта → можно архивировать, удалять, менять иконку
   - [ ] Добавление объекта → FAB работает
   - [ ] Открытие экрана установок → отображаются все установки
   - [ ] Проведение ТО → кнопки работают
   - [ ] Открытие экрана компонентов → отображаются все компоненты

2. **Client**:
   - [ ] Логин клиента → отображаются только доступные объекты
   - [ ] Создание объекта → автоматически создаётся membership
   - [ ] Редактирование объекта → можно редактировать только свои объекты
   - [ ] Создание установки → автоматически создаётся membership
   - [ ] Удаление установки → membership архивируется
   - [ ] Просмотр установок → отображаются только доступные установки
   - [ ] Просмотр компонентов → отображаются все компоненты доступных установок

## Известные ограничения

1. **Загрузка иконок**: В текущей реализации иконки загружаются синхронно в mapper'е. Для больших списков может потребоваться оптимизация (ленивая загрузка, кэширование).

2. **Вложенные установки в ClientDetailScreen**: Текущая реализация `ClientDetailScreen` показывает установки внутри объектов. Эта функциональность может быть сохранена или упрощена при интеграции shared-экранов.

3. **Производительность**: При большом количестве объектов/установок/компонентов может потребоваться оптимизация загрузки иконок и преобразования Entity → ItemUi.

## Итоговый статус

### ✅ Выполнено

1. **Shared-экраны в core:screens**:
   - `ClientSitesScreenShared` - полная реализация
   - `SiteInstallationsScreenShared` - полная реализация
   - `InstallationComponentsScreenShared` - полная реализация
   - Обновлены UI State классы с полями для иконок и прав доступа

2. **Интеграция в app-crm**:
   - `InstallationsScreen.kt` - интегрирован `SiteInstallationsScreenShared`
   - `HierarchyUiStateMapper` - создан и используется

3. **Интеграция в app-client с правами**:
   - `ClientHierarchyUiStateMapper` - создан с использованием `HierarchyPermissionChecker`
   - Автоматика user_membership:
     - Создание membership при создании Site ✅
     - Создание membership при создании Installation ✅
     - Архивирование membership при удалении Installation ✅

### ⏳ Требует доработки

1. **app-crm**:
   - `ComponentsScreen.kt` - интеграция `InstallationComponentsScreenShared` (требует сохранения сложной функциональности)
   - `ClientDetailScreen.kt` - интеграция `ClientSitesScreenShared` (требует сохранения функциональности вложенных установок)

2. **app-client**:
   - `SitesScreen.kt` - полная интеграция `ClientSitesScreenShared` с использованием `ClientHierarchyUiStateMapper`
   - `SiteDetailScreen.kt` - полная интеграция `SiteInstallationsScreenShared` с правами
   - `ComponentsScreen.kt` - полная интеграция `InstallationComponentsScreenShared` с правами

### 📝 Созданные/изменённые файлы

**core:screens**:
- `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/ClientSitesScreenShared.kt` ✅
- `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/SiteInstallationsScreenShared.kt` ✅
- `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/InstallationComponentsScreenShared.kt` ✅
- `core/screens/src/main/java/ru/wassertech/core/screens/hierarchy/ui/HierarchyUiState.kt` ✅ (обновлён)

**app-crm**:
- `app-crm/src/main/java/com/example/wassertech/ui/hierarchy/HierarchyUiStateMapper.kt` ✅
- `app-crm/src/main/java/com/example/wassertech/ui/hierarchy/InstallationsScreen.kt` ✅ (интегрирован)

**app-client**:
- `app-client/src/main/java/com/example/wassertech/client/ui/hierarchy/ClientHierarchyUiStateMapper.kt` ✅
- `app-client/src/main/java/com/example/wassertech/client/data/UserMembershipHelper.kt` ✅
- `app-client/src/main/java/com/example/wassertech/client/ui/sites/SitesScreen.kt` ✅ (добавлена автоматика membership)
- `app-client/src/main/java/com/example/wassertech/client/ui/sites/SiteDetailScreen.kt` ✅ (добавлена автоматика membership)

## Заключение

Создана инфраструктура для shared-экранов и прав доступа. Частично интегрированы shared-экраны в app-crm (`InstallationsScreen`) и реализована автоматика user_membership в app-client. Для полной интеграции требуется доработка `ComponentsScreen.kt`, `ClientDetailScreen.kt` в app-crm и полная интеграция shared-экранов в app-client с использованием `ClientHierarchyUiStateMapper`.

