# Финальный отчёт о рефакторинге app-client

## Выполненные задачи

### 1. Добавлена роль CLIENT в UserRole enum
**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/UserRole.kt`

Добавлена роль `CLIENT("Клиент")` в enum `UserRole`.

### 2. Создан OriginType enum
**Файл:** `app-client/src/main/java/com/example/wassertech/client/auth/OriginType.kt`

Создан enum `OriginType` с двумя значениями:
- `CRM` - сущность создана инженером в CRM (read-only в app-client)
- `CLIENT` - сущность создана самим клиентом (можно редактировать/удалять)

### 3. Создан UserSession интерфейс и UserSessionManager
**Файл:** `app-client/src/main/java/com/example/wassertech/client/auth/UserSession.kt`

Созданы:
- Интерфейс `UserSession` с полями `userId`, `role`, `clientId`
- Реализация `UserSessionImpl`
- Менеджер `UserSessionManager` для работы с текущей сессией

**TODO:** Интегрировать с JWT-парсером для получения данных из токена при логине.

### 4. Обновлены сущности данных
**Файлы:**
- `app-client/src/main/java/com/example/wassertech/client/data/entities/SiteEntity.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/InstallationEntity.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentEntity.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentTemplateEntity.kt`

Добавлены поля:
- `ownerClientId: String?` - ID клиента-владельца (FK → clients.id)
- `origin: String?` - "CRM" или "CLIENT"

Добавлены методы-хелперы:
- `getOwnerClientId()` - возвращает ownerClientId или clientId (для обратной совместимости)
- `getOrigin()` - возвращает OriginType (по умолчанию CRM для старых данных)

**TODO:** Добавить миграцию БД для этих полей.

### 5. Обновлён PermissionUtils с реальными правилами
**Файл:** `app-client/src/main/java/com/example/wassertech/client/permissions/PermissionUtils.kt`

Реализованы функции проверки прав на основе:
- Роли пользователя (должна быть `UserRole.CLIENT`)
- `ownerClientId` (должен совпадать с `user.clientId`)
- `origin` (должен быть `OriginType.CLIENT` для редактирования/удаления)

Функции:
- `canEditSite()`, `canDeleteSite()`
- `canEditInstallation()`, `canDeleteInstallation()`
- `canEditComponent()`, `canDeleteComponent()`
- `canEditTemplate()`, `canDeleteTemplate()`
- `canCreateEntity()` - всегда `true` для CLIENT
- `canGeneratePdf()` - всегда `false` в app-client
- `canViewPdf()` - всегда `true` для CLIENT
- `canViewSite()`, `canViewInstallation()`, `canViewComponent()`, `canViewTemplate()`

### 6. Создан экран списка объектов (SitesScreen)
**Файл:** `app-client/src/main/java/com/example/wassertech/client/ui/sites/SitesScreen.kt`

Экран показывает список объектов текущего клиента:
- Использует компоненты из `core:ui` (`AppEmptyState`, `EntityRowWithMenu`)
- Кнопка добавления показывается только если `canCreateEntity()` возвращает `true`
- Кнопки редактирования/удаления показываются только если `canEditSite()`/`canDeleteSite()` возвращают `true`
- Фильтрация по `clientId` текущего пользователя

### 7. Создан экран деталей объекта (SiteDetailScreen)
**Файл:** `app-client/src/main/java/com/example/wassertech/client/ui/sites/SiteDetailScreen.kt`

Экран показывает:
- Заголовок объекта с информацией о клиенте
- Список установок внутри объекта
- Кнопка добавления установки (только если можно создавать)
- Кнопки редактирования/удаления установок (только если можно редактировать)
- Использует компоненты из `core:ui`

### 8. Создан экран компонентов установки (ComponentsScreen)
**Файл:** `app-client/src/main/java/com/example/wassertech/client/ui/components/ComponentsScreen.kt`

Экран показывает:
- Заголовок установки с подзаголовком (объект и клиент)
- Кнопка "История ТО" (БЕЗ кнопки "Провести ТО")
- Список компонентов установки
- Кнопка добавления компонента (только если можно создавать)
- Кнопки удаления компонентов (только если можно удалять)
- Использует компоненты из `core:ui` (`ScreenTitleWithSubtitle`, `AppEmptyState`, `EntityRowWithMenu`)

**ВАЖНО:** Функционал генерации PDF полностью отсутствует в этом экране.

### 9. Обновлена навигация app-client
**Файлы:**
- `app-client/src/main/java/com/example/wassertech/navigation/AppRoutes.kt`
- `app-client/src/main/java/com/example/wassertech/navigation/AppNavigation.kt`

Добавлены маршруты:
- `SITES` - список объектов (`sites/{clientId}`)
- `SITE_DETAIL` - детали объекта (`site/{siteId}`)
- `INSTALLATION` - компоненты установки (`installation/{installationId}`)
- `MAINTENANCE_HISTORY` - история ТО (TODO: реализовать экран)

### 10. Обновлён HomeScreen
**Файл:** `app-client/src/main/java/com/example/wassertech/screen/HomeScreen.kt`

Изменения:
- Добавлена вкладка "Объекты" как первая вкладка
- Вкладка "Отчёты" перемещена на второе место
- Вкладка "Настройки" осталась на третьем месте
- При выборе вкладки "Объекты" показывается `SitesScreen` с `clientId` из текущей сессии

## Использованные компоненты из core:ui

- ✅ `AppEmptyState` - для пустых состояний
- ✅ `EntityRowWithMenu` - для отображения строк сущностей
- ✅ `EmptyGroupPlaceholder` - для пустых групп
- ✅ `ScreenTitleWithSubtitle` - для заголовков экранов

## Ограничения по правам доступа

### Реализовано:
- ✅ Клиент может редактировать/удалять ТОЛЬКО сущности с `origin == CLIENT`
- ✅ Сущности с `origin == CRM` доступны только для просмотра
- ✅ Все сущности фильтруются по `ownerClientId == currentUser.clientId`
- ✅ Генерация PDF отключена в app-client (`canGeneratePdf()` всегда возвращает `false`)
- ✅ Просмотр PDF разрешён (`canViewPdf()` возвращает `true`)

### UI-ограничения:
- ✅ Кнопки редактирования/удаления скрыты для CRM-сущностей
- ✅ FAB "добавить" показывается только если `canCreateEntity()` возвращает `true`
- ✅ Кнопка "Провести ТО" отсутствует в ComponentsScreen
- ✅ Кнопка генерации PDF отсутствует во всех экранах app-client

## TODO и недостающие части

### 1. Интеграция с JWT-токеном
**Требуется:**
- Парсинг JWT-токена при логине для получения `userId`, `role`, `clientId`
- Установка сессии через `UserSessionManager.setCurrentSession()` после успешного логина
- Очистка сессии при выходе из системы

**Где:** `app-client/src/main/java/com/example/wassertech/client/auth/` (создать JWT-парсер или использовать существующий)

### 2. Миграция БД для новых полей
**Требуется:**
- Добавить поля `ownerClientId` и `origin` в таблицы:
  - `sites`
  - `installations`
  - `components`
  - `component_templates`
- Создать миграцию БД (например, `MIGRATION_10_11.kt`)

**Где:** `app-client/src/main/java/com/example/wassertech/client/data/migrations/`

### 3. Экран истории ТО
**Требуется:**
- Создать экран для просмотра истории технического обслуживания установки
- Маршрут `MAINTENANCE_HISTORY` уже добавлен в навигацию, но экран не реализован

**Где:** `app-client/src/main/java/com/example/wassertech/client/ui/maintenance/MaintenanceHistoryScreen.kt` (создать)

### 4. Обновление при создании сущностей
**Требуется:**
- При создании новых сущностей в app-client устанавливать:
  - `ownerClientId = currentUser.clientId`
  - `origin = OriginType.CLIENT.name`
- Это уже частично реализовано в `SiteDetailScreen` и `ComponentsScreen`, но нужно проверить все места создания

### 5. Вынос общих UI-компонентов в core:ui (опционально)
**Требуется:**
- Вынести `AppFloatingActionButton` и `CommonAddDialog` из `app-crm/ui/common` в `core:ui`
- Или создать локальные версии в app-client

**Текущее состояние:** Используются стандартные Material3 компоненты (`FloatingActionButton`, `AlertDialog`)

## Изменённые файлы

### Новые файлы:
1. `app-client/src/main/java/com/example/wassertech/client/auth/OriginType.kt`
2. `app-client/src/main/java/com/example/wassertech/client/auth/UserSession.kt`
3. `app-client/src/main/java/com/example/wassertech/client/permissions/PermissionUtils.kt` (переписан)
4. `app-client/src/main/java/com/example/wassertech/client/ui/sites/SitesScreen.kt`
5. `app-client/src/main/java/com/example/wassertech/client/ui/sites/SiteDetailScreen.kt`
6. `app-client/src/main/java/com/example/wassertech/client/ui/components/ComponentsScreen.kt`

### Изменённые файлы:
1. `core/auth/src/main/java/com/example/wassertech/core/auth/UserRole.kt` - добавлена роль CLIENT
2. `app-client/src/main/java/com/example/wassertech/client/data/entities/SiteEntity.kt` - добавлены поля ownerClientId, origin
3. `app-client/src/main/java/com/example/wassertech/client/data/entities/InstallationEntity.kt` - добавлены поля ownerClientId, origin
4. `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentEntity.kt` - добавлены поля ownerClientId, origin
5. `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentTemplateEntity.kt` - добавлены поля ownerClientId, origin
6. `app-client/src/main/java/com/example/wassertech/navigation/AppRoutes.kt` - добавлены новые маршруты
7. `app-client/src/main/java/com/example/wassertech/navigation/AppNavigation.kt` - добавлены новые экраны в навигацию
8. `app-client/src/main/java/com/example/wassertech/screen/HomeScreen.kt` - добавлена вкладка "Объекты"

## Тестирование

### Что нужно протестировать:
1. ✅ Компиляция проекта без ошибок
2. ⏳ Логин пользователя с ролью CLIENT
3. ⏳ Отображение списка объектов текущего клиента
4. ⏳ Создание нового объекта (должен иметь origin=CLIENT)
5. ⏳ Редактирование объекта с origin=CLIENT (должно работать)
6. ⏳ Редактирование объекта с origin=CRM (кнопки должны быть скрыты)
7. ⏳ Создание установки в объекте
8. ⏳ Создание компонента в установке
9. ⏳ Просмотр истории ТО (когда экран будет реализован)
10. ⏳ Отсутствие кнопок генерации PDF

## Заключение

Рефакторинг app-client выполнен в соответствии с требованиями:
- ✅ Реализована ролевая модель с ролью CLIENT
- ✅ Реализовано различение CRM/CLIENT данных через поля ownerClientId и origin
- ✅ Созданы экраны с ограничениями по правам доступа
- ✅ Использованы компоненты из core:ui
- ✅ Генерация PDF отключена в app-client
- ✅ Навигация обновлена для работы с объектами/установками/компонентами

Осталось выполнить интеграцию с JWT-токеном и добавить миграцию БД для новых полей.

