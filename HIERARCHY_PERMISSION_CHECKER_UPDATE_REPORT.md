# Отчёт: Обновление HierarchyPermissionChecker для использования user_membership

**Дата:** 2025-11-18  
**Статус:** ✅ Завершено

---

## Краткий обзор

Обновлён `HierarchyPermissionChecker` в модуле `core:auth` для реального использования `UserMembershipEntity` через промежуточный data class `UserMembershipInfo`. Реализована полная логика проверки прав доступа для ролей CLIENT, ADMIN и ENGINEER с учётом `user_membership` и `created_by_user_id`.

---

## Выполненные изменения

### 1. ✅ Создан UserMembershipInfo в core:auth

**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/HierarchyPermissionChecker.kt`

**Структура:**
```kotlin
data class UserMembershipInfo(
    val userId: String,
    val scope: String, // "CLIENT", "SITE", "INSTALLATION"
    val targetId: String,
    val isArchived: Boolean = false
)
```

**Назначение:**
- Простое представление записи `user_membership` без зависимостей от Room
- Используется в `HierarchyPermissionChecker` для избежания циклических зависимостей
- ViewModel'и преобразуют `UserMembershipEntity` в `UserMembershipInfo` перед передачей в checker

### 2. ✅ Обновлён HierarchyPermissionChecker

**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/HierarchyPermissionChecker.kt`

**Изменения сигнатур методов:**

#### Старые сигнатуры (удалены):
- `canViewSite(user, siteClientId, siteId, memberships: List<UserMembership>)`
- `canEditSite(user, siteClientId, siteOrigin, siteCreatedByUserId, siteId, memberships)`
- Использовался enum `MembershipScope` вместо строки

#### Новые сигнатуры:
- `canViewSite(siteId, siteClientId, currentUser, memberships: List<UserMembershipInfo>)`
- `canEditSite(siteCreatedByUserId, siteOrigin, currentUser)`
- `canDeleteSite(siteCreatedByUserId, siteOrigin, currentUser)`
- `canChangeIconForSite(siteCreatedByUserId, siteOrigin, currentUser)`
- `canCreateInstallationUnderSite(siteCreatedByUserId, currentUser)`
- `canViewInstallation(installationId, installationSiteId, siteClientId, currentUser, memberships)`
- `canEditInstallation(installationCreatedByUserId, installationOrigin, currentUser)`
- `canDeleteInstallation(installationCreatedByUserId, installationOrigin, currentUser)`
- `canChangeIconForInstallation(installationCreatedByUserId, installationOrigin, currentUser)`
- `canViewComponent(componentInstallationId, installationSiteId, siteClientId, currentUser, memberships)`
- `canEditComponent(componentCreatedByUserId, componentOrigin, currentUser)`
- `canDeleteComponent(componentCreatedByUserId, componentOrigin, currentUser)`
- `canChangeIconForComponent(componentCreatedByUserId, componentOrigin, currentUser)`
- `canCreateComponentUnderInstallation(installationCreatedByUserId, currentUser)`

**Ключевые изменения:**
- Методы принимают отдельные параметры вместо полных Entity (избегаем зависимостей от Room)
- Используется `UserMembershipInfo` вместо старого `UserMembership` с enum
- Параметр `currentUser` вместо `user` для ясности
- Упрощённые сигнатуры для методов редактирования (не требуют полных Entity)

### 3. ✅ Реализованы вспомогательные функции для membership

**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/HierarchyPermissionChecker.kt`

**Функции:**
- `List<UserMembershipInfo>.hasClientAccessToClient(userId, clientId)` - проверка доступа к клиенту
- `List<UserMembershipInfo>.hasSiteAccess(userId, siteId)` - проверка доступа к объекту
- `List<UserMembershipInfo>.hasInstallationAccess(userId, installationId)` - проверка доступа к установке

**Назначение:**
- Упрощают код проверок прав
- Централизуют логику фильтрации membership по `userId`, `scope`, `targetId` и `isArchived`
- Используются во всех методах `canView*`

### 4. ✅ Реализована логика для разных ролей

#### ADMIN / ENGINEER:
- **canView*** → `true` (полный доступ ко всем сущностям)
- **canEdit*** → `true` (могут редактировать всё)
- **canDelete*** → `true` (могут удалять всё)
- **canChangeIcon*** → `true` (могут менять иконки)
- **canCreate*** → `true` (могут создавать новые сущности)

**Поведение:** Не изменено, сохраняется текущая логика CRM.

#### CLIENT:

**Просмотр (canView*):**
- **canViewSite**: 
  - Проверяет `user_membership` с `scope = "CLIENT"` и `targetId = site.clientId`
  - ИЛИ `scope = "SITE"` и `targetId = site.id`
  - ИЛИ fallback через `site.clientId == currentUser.clientId` (для обратной совместимости)
  
- **canViewInstallation**:
  - Проверяет `user_membership` с `scope = "CLIENT"` и `targetId = site.clientId`
  - ИЛИ `scope = "SITE"` и `targetId = installation.siteId`
  - ИЛИ `scope = "INSTALLATION"` и `targetId = installation.id`
  - ИЛИ fallback через `site.clientId == currentUser.clientId`
  
- **canViewComponent**:
  - Проверяет доступ через установку (`canViewInstallation`)

**Редактирование (canEdit*):**
- **canEditSite**: `site.createdByUserId == currentUser.userId` И `site.origin == "CLIENT"`
- **canEditInstallation**: `installation.createdByUserId == currentUser.userId` И `installation.origin == "CLIENT"`
- **canEditComponent**: `component.createdByUserId == currentUser.userId` И `component.origin == "CLIENT"`

**Удаление (canDelete*):**
- Логика совпадает с `canEdit*` (только свои сущности с `origin = CLIENT`)

**Изменение иконок (canChangeIcon*):**
- Логика совпадает с `canEdit*`

**Создание (canCreate*):**
- **canCreateInstallationUnderSite**: `site.createdByUserId == currentUser.userId`
- **canCreateComponentUnderInstallation**: `installation.createdByUserId == currentUser.userId`

### 5. ✅ Созданы extension-функции для преобразования Entity → Info

**Файлы:**
- `app-crm/src/main/java/com/example/wassertech/data/entities/UserMembershipEntityExtensions.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/UserMembershipEntityExtensions.kt`

**Функции:**
- `UserMembershipEntity.toUserMembershipInfo()` - преобразование одной записи
- `List<UserMembershipEntity>.toUserMembershipInfoList()` - преобразование списка

**Назначение:**
- Упрощают использование `HierarchyPermissionChecker` в ViewModel'ях
- Позволяют легко преобразовывать Room Entity в простой data class для checker

---

## Использование UserMembershipEntity

### Структура membership:

**Поля:**
- `userId: String` - ID пользователя
- `scope: String` - Область: `"CLIENT"`, `"SITE"`, `"INSTALLATION"`
- `targetId: String` - ID целевой сущности:
  - `scope = "CLIENT"` → `targetId = clientId`
  - `scope = "SITE"` → `targetId = siteId`
  - `scope = "INSTALLATION"` → `targetId = installationId`
- `isArchived: Boolean` - Флаг архивирования (неархивные записи используются для проверки прав)

### Обрабатываемые scope:

1. **CLIENT scope:**
   - Даёт доступ ко всем объектам клиента (`site.clientId == targetId`)
   - Используется в `canViewSite` и `canViewInstallation`

2. **SITE scope:**
   - Даёт доступ к конкретному объекту (`site.id == targetId`)
   - Используется в `canViewSite` и `canViewInstallation`

3. **INSTALLATION scope:**
   - Даёт доступ к конкретной установке (`installation.id == targetId`)
   - Используется в `canViewInstallation` и косвенно в `canViewComponent`

### Логика проверки прав:

**Для CLIENT роли:**

1. **Просмотр объекта:**
   ```
   hasClientMembership(userId, site.clientId) 
   OR hasSiteMembership(userId, site.id)
   OR site.clientId == currentUser.clientId (fallback)
   ```

2. **Просмотр установки:**
   ```
   hasClientMembership(userId, site.clientId)
   OR hasSiteMembership(userId, installation.siteId)
   OR hasInstallationMembership(userId, installation.id)
   OR site.clientId == currentUser.clientId (fallback)
   ```

3. **Редактирование:**
   ```
   entity.createdByUserId == currentUser.userId 
   AND entity.origin == "CLIENT"
   ```

4. **Создание:**
   ```
   parentEntity.createdByUserId == currentUser.userId
   ```

---

## Изменённые файлы

### Новые файлы:
1. `core/auth/src/main/java/com/example/wassertech/core/auth/UserMembershipInfoExtensions.kt` - документация для extension-функций
2. `app-crm/src/main/java/com/example/wassertech/data/entities/UserMembershipEntityExtensions.kt` - extension-функции для app-crm
3. `app-client/src/main/java/com/example/wassertech/client/data/entities/UserMembershipEntityExtensions.kt` - extension-функции для app-client

### Изменённые файлы:
1. `core/auth/src/main/java/com/example/wassertech/core/auth/HierarchyPermissionChecker.kt` - полностью переписан:
   - Удалён старый `UserMembership` data class с enum `MembershipScope`
   - Добавлен новый `UserMembershipInfo` data class
   - Обновлены все сигнатуры методов
   - Реализована полная логика для всех ролей
   - Добавлены вспомогательные функции для работы с membership

---

## Текущее использование HierarchyPermissionChecker

### В app-client:
- `app-client/src/main/java/com/example/wassertech/client/permissions/PermissionUtils.kt` - обёртка над `LocalPermissionChecker` (пока не использует `HierarchyPermissionChecker`)
- `app-client/src/main/java/com/example/wassertech/client/ui/sites/SitesScreen.kt` - использует `PermissionUtils` (пока не использует `HierarchyPermissionChecker`)
- `app-client/src/main/java/com/example/wassertech/client/ui/sites/SiteDetailScreen.kt` - использует `PermissionUtils` (пока не использует `HierarchyPermissionChecker`)

**Статус:** `HierarchyPermissionChecker` готов к использованию, но пока не интегрирован в существующие экраны. Это будет сделано на следующем этапе при обновлении ViewModel'ей.

### В app-crm:
- Пока не используется (CRM использует полные права для ADMIN/ENGINEER)

---

## Следующие шаги

### 1. Интеграция в ViewModel app-client
- Обновить ViewModel'и для использования `HierarchyPermissionChecker`
- Загружать `UserMembershipEntity` из DAO
- Преобразовывать в `UserMembershipInfo` через extension-функции
- Формировать `UiState` с правильными флагами прав доступа

### 2. Обновление PermissionUtils в app-client
- Перевести `PermissionUtils` на использование `HierarchyPermissionChecker`
- Или удалить `PermissionUtils`, если он больше не нужен

### 3. Доработка shared-экранов
- Завершить реализацию `ClientSitesScreenShared`
- Создать `SiteInstallationsScreenShared`
- Создать `InstallationComponentsScreenShared`

### 4. Интеграция shared-экранов
- Обновить app-client для использования shared-экранов
- Обновить app-crm для использования shared-экранов

---

## Проверка зависимостей

### ✅ Нет циклических зависимостей:
- `core:auth` не зависит от `app-crm` или `app-client`
- `HierarchyPermissionChecker` использует только `UserSession`, `UserRole`, `OriginType` из `core:auth`
- `UserMembershipInfo` - простой data class без зависимостей от Room

### ✅ Готовность к использованию:
- `HierarchyPermissionChecker` можно использовать в любом модуле, который зависит от `core:auth`
- ViewModel'и в `app-crm` и `app-client` могут использовать checker через extension-функции
- Shared-экраны в `core:screens` могут использовать checker (через параметры, передаваемые из ViewModel)

---

## Примеры использования

### В ViewModel app-client:

```kotlin
// Загружаем membership'ы для текущего пользователя
val memberships = userMembershipDao.getForUser(currentUser.userId)
    .map { it.toUserMembershipInfo() }

// Проверяем права на просмотр объекта
val canView = HierarchyPermissionChecker.canViewSite(
    siteId = site.id,
    siteClientId = site.clientId,
    currentUser = currentUser,
    memberships = memberships
)

// Проверяем права на редактирование
val canEdit = HierarchyPermissionChecker.canEditSite(
    siteCreatedByUserId = site.createdByUserId,
    siteOrigin = site.origin,
    currentUser = currentUser
)

// Формируем UI State
val siteItemUi = SiteItemUi(
    id = site.id,
    name = site.name,
    // ... другие поля ...
    canEdit = canEdit,
    canDelete = canEdit, // Логика совпадает
    canChangeIcon = canEdit // Логика совпадает
)
```

### В ViewModel app-crm:

```kotlin
// Для CRM все права = true (ADMIN/ENGINEER)
val siteItemUi = SiteItemUi(
    id = site.id,
    name = site.name,
    // ... другие поля ...
    canEdit = true,
    canDelete = true,
    canChangeIcon = true
)
```

---

## Заключение

`HierarchyPermissionChecker` полностью обновлён и готов к использованию:

- ✅ Использует реальную структуру `UserMembershipEntity` через `UserMembershipInfo`
- ✅ Реализована полная логика для всех ролей (CLIENT, ADMIN, ENGINEER)
- ✅ Нет циклических зависимостей между модулями
- ✅ Созданы extension-функции для удобного преобразования Entity → Info
- ✅ Сигнатуры методов упрощены и не требуют полных Entity
- ✅ Готов к интеграции в ViewModel'и и shared-экраны

**Следующий этап:** Обновление ViewModel'ей в app-client для использования `HierarchyPermissionChecker` и формирования корректного `UiState` с учётом прав доступа.

---

**Автор:** AI Assistant (Cursor)  
**Дата:** 2025-11-18

