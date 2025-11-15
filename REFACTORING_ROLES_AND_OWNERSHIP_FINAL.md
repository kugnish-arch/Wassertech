# Финальный отчёт о рефакторинге: Роли, владельцы и права доступа в Wassertech API

**Дата:** 2025-01-13  
**Версия:** 2.0 (Final)  
**Проект:** Wassertech CRM API (PHP 8.2 + MySQL)

---

## Краткий обзор

В рамках этого рефакторинга была полностью реализована система ролей и владения данными для разделения прав доступа между инженерами Wassertech (работающими в CRM) и клиентами (работающими в приложении Wassertech Client).

### Ключевые концепции

1. **Роли пользователей:**
   - `ADMIN` — полный доступ в веб-админке и API
   - `ENGINEER` — инженер Wassertech (работает в CRM, создаёт/редактирует объекты и установки клиентов)
   - `CLIENT` — пользователь, работающий в приложении Wassertech Client (заказчик)

2. **Привязка пользователя к клиенту:**
   - Пользователи с ролью `CLIENT` привязаны к одному клиенту через поле `users.client_id`
   - Для роли `CLIENT` поле `client_id` обязательно (NOT NULL)
   - Для ролей `ADMIN` и `ENGINEER` поле `client_id` может быть NULL

3. **Владение данными (origin):**
   - Все бизнес-сущности (sites, installations, components, component_templates, maintenance_sessions, maintenance_values) имеют поле `origin`
   - `origin = 'CRM'` — данные созданы инженером в CRM (клиент видит, но не может редактировать)
   - `origin = 'CLIENT'` — данные созданы самим клиентом в app-client (клиент может редактировать)

4. **Авторство (created_by_user_id):**
   - Все бизнес-сущности имеют поле `created_by_user_id` для отслеживания, кто создал запись
   - Поле опциональное (NULL для исторических данных)

5. **Временные метки:**
   - Все таблицы используют `createdAtEpoch` и `updatedAtEpoch` (BIGINT, Unix timestamp в миллисекундах)
   - НЕ используются поля `created_at`/`updated_at` (DATETIME)

---

## Структура базы данных

### Важные уточнения

1. **Тип `clients.id`:**
   - Тип: `VARCHAR(255)` (UUID)
   - НЕ `AUTO_INCREMENT INT`
   - Генерируется через функцию `generateUuid()` (UUID v4)

2. **Временные метки:**
   - Используются поля `createdAtEpoch` и `updatedAtEpoch` (BIGINT, миллисекунды)
   - НЕ используются `created_at`/`updated_at` (DATETIME)
   - Все существующие таблицы уже имеют эти поля (миграция `009_sync_timestamp_fields.sql`)

---

## Что было добавлено/изменено

### 1. База данных (миграция `011_roles_and_ownership.sql`)

#### Таблица `users`:
- ✅ Добавлено поле `role` VARCHAR(32) NOT NULL DEFAULT 'ENGINEER'
- ✅ Добавлено поле `client_id` VARCHAR(255) NULL (FK → clients.id)
- ✅ Добавлены индексы: `idx_role`, `idx_client_id`
- ✅ Добавлен внешний ключ: `fk_users_client_id`
- ✅ Миграция существующих данных: роль определяется из `user_role`, если поле `role` пустое

#### Таблица `sites`:
- ✅ Добавлено поле `origin` VARCHAR(16) NOT NULL DEFAULT 'CRM'
- ✅ Добавлено поле `created_by_user_id` VARCHAR(255) NULL (FK → users.id)
- ✅ Добавлены индексы: `idx_origin`, `idx_created_by_user_id`
- ✅ Добавлен внешний ключ: `fk_sites_created_by`
- ✅ Миграция существующих данных: все записи помечены как `origin = 'CRM'`

#### Таблица `installations`:
- ✅ Добавлено поле `origin` VARCHAR(16) NOT NULL DEFAULT 'CRM'
- ✅ Добавлено поле `created_by_user_id` VARCHAR(255) NULL (FK → users.id)
- ✅ Добавлены индексы: `idx_origin`, `idx_created_by_user_id`
- ✅ Добавлен внешний ключ: `fk_installations_created_by`
- ✅ Миграция существующих данных: все записи помечены как `origin = 'CRM'`

#### Таблица `components`:
- ✅ Добавлено поле `origin` VARCHAR(16) NOT NULL DEFAULT 'CRM'
- ✅ Добавлено поле `created_by_user_id` VARCHAR(255) NULL (FK → users.id)
- ✅ Добавлены индексы: `idx_origin`, `idx_created_by_user_id`
- ✅ Добавлен внешний ключ: `fk_components_created_by`
- ✅ Миграция существующих данных: все записи помечены как `origin = 'CRM'`

#### Таблица `component_templates`:
- ✅ Добавлено поле `origin` VARCHAR(16) NOT NULL DEFAULT 'CRM'
- ✅ Добавлено поле `created_by_user_id` VARCHAR(255) NULL (FK → users.id)
- ✅ Добавлены индексы: `idx_origin`, `idx_created_by_user_id`
- ✅ Добавлен внешний ключ: `fk_component_templates_created_by`
- ✅ Миграция существующих данных: все записи помечены как `origin = 'CRM'`

#### Таблица `maintenance_sessions`:
- ✅ Добавлено поле `origin` VARCHAR(16) NOT NULL DEFAULT 'CRM'
- ✅ Добавлено поле `created_by_user_id` VARCHAR(255) NULL (FK → users.id)
- ✅ Добавлены индексы: `idx_origin`, `idx_created_by_user_id`
- ✅ Добавлен внешний ключ: `fk_maintenance_sessions_created_by`
- ✅ Миграция существующих данных: все записи помечены как `origin = 'CRM'`

#### Таблица `maintenance_values`:
- ✅ Добавлено поле `origin` VARCHAR(16) NOT NULL DEFAULT 'CRM'
- ✅ Добавлено поле `created_by_user_id` VARCHAR(255) NULL (FK → users.id)
- ✅ Добавлены индексы: `idx_origin`, `idx_created_by_user_id`
- ✅ Добавлен внешний ключ: `fk_maintenance_values_created_by`
- ✅ Миграция существующих данных: все записи помечены как `origin = 'CRM'`

### 2. PHP-код

#### Новый класс `src/UserContext.php`:
- ✅ Класс для хранения контекста текущего пользователя
- ✅ Содержит: `userId`, `role`, `clientId`, `login`, `name`, `email`
- ✅ Методы: `isAdmin()`, `isEngineer()`, `isClient()`, `canEdit()`
- ✅ Статические методы: `fromToken()`, `fromUserId()` для создания из JWT токена или user ID
- ✅ Поддержка fallback на legacy таблицу `user_role` для обратной совместимости

#### Новый класс `src/AccessControlHelper.php`:
- ✅ Централизованная утилита для проверки прав доступа
- ✅ Функция `canViewEntity()` — проверка права на просмотр сущности
- ✅ Функция `canEditEntityAccess()` — проверка права на редактирование/удаление сущности
- ✅ Функция `setEntityOwnershipFields()` — автоматическая установка полей `origin`, `client_id`, `created_by_user_id` при создании сущности

#### Обновлён `src/AuthController.php`:
- ✅ Обновлён `handleLogin()` — возвращает `clientId` в ответе
- ✅ Обновлён `handleGetMe()` — возвращает `clientId` и корректную роль из поля `users.role`

#### Обновлён `src/SyncController.php`:
- ✅ **`handleSyncPull()`:**
  - Интегрирован `UserContext` для определения роли пользователя
  - Для `CLIENT` пользователей: фильтрация данных по `client_id = userContext->clientId`
  - Для `ADMIN` и `ENGINEER`: возвращаются все данные (с сохранением обратной совместимости через `user_membership`)
  - Запросы для `clients`, `sites`, `installations`, `components`, `maintenance_sessions`, `maintenance_values` учитывают роль пользователя
  - `component_templates` и `component_template_fields` доступны всем ролям (глобальные)
- ✅ **`handleSyncPush()`:**
  - Интегрирован `UserContext` для определения роли пользователя
  - Для новых сущностей: автоматическая установка `origin`, `client_id`, `created_by_user_id` через `setEntityOwnershipFields()`
  - Для существующих сущностей: проверка прав через `canEditEntityAccess()` (CLIENT может редактировать только свои данные с `origin = 'CLIENT'`)
  - Обновлены `allowedFields` для всех сущностей: добавлены `origin` и `created_by_user_id`

#### Обновлён `src/SitesController.php`:
- ✅ Интегрированы `UserContext` и `AccessControlHelper`
- ✅ **`handleGetSites()`:** Фильтрация для CLIENT по `clientId`
- ✅ **`handleGetSite()`:** Проверка прав доступа через `canViewEntity()`
- ✅ **`handleCreateSite()`:** Автоматическая установка `origin` и `created_by_user_id`, проверка что CLIENT может создавать только для своего клиента
- ✅ **`handleUpdateSite()`:** Проверка прав через `canEditEntityAccess()`
- ✅ **`handleArchiveSite()`:** Проверка прав через `canEditEntityAccess()`
- ✅ **`handleDeleteSite()`:** Требуется ADMIN или `canEdit()`
- ✅ Добавлено обновление `updatedAtEpoch` при изменениях

#### Обновлён `src/InstallationsController.php`:
- ✅ Интегрированы `UserContext` и `AccessControlHelper`
- ✅ **`handleGetInstallations()`:** Фильтрация для CLIENT по `clientId` (через JOIN с `sites`)
- ✅ **`handleGetInstallation()`:** Проверка прав доступа через `canViewEntity()` (через JOIN с `sites`)
- ✅ **`handleCreateInstallation()`:** Автоматическая установка `origin` и `created_by_user_id`, проверка что CLIENT может создавать только для своего клиента
- ✅ **`handleUpdateInstallation()`:** Проверка прав через `canEditEntityAccess()` (через JOIN с `sites`)
- ✅ **`handleArchiveInstallation()`:** Проверка прав через `canEditEntityAccess()`
- ✅ **`handleDeleteInstallation()`:** Требуется ADMIN или `canEdit()`
- ✅ Добавлено обновление `updatedAtEpoch` при изменениях

#### Обновлён `src/ComponentsController.php`:
- ✅ Интегрированы `UserContext` и `AccessControlHelper`
- ✅ **`handleGetComponents()`:** Фильтрация для CLIENT по `clientId` (через JOIN с `installations` и `sites`)
- ✅ **`handleGetComponent()`:** Проверка прав доступа через `canViewEntity()` (через JOIN)
- ✅ **`handleCreateComponent()`:** Автоматическая установка `origin` и `created_by_user_id`, проверка что CLIENT может создавать только для своего клиента
- ✅ **`handleUpdateComponent()`:** Проверка прав через `canEditEntityAccess()` (через JOIN)
- ✅ **`handleDeleteComponent()`:** Проверка прав через `canEditEntityAccess()`
- ✅ Добавлено обновление `updatedAtEpoch` при изменениях

#### Обновлён `src/ComponentTemplatesController.php`:
- ✅ Интегрированы `UserContext` и `AccessControlHelper`
- ✅ **`handleGetComponentTemplates()`:** Доступен всем ролям (шаблоны глобальные)
- ✅ **`handleCreateComponentTemplate()`:** Для CLIENT проверка что может создавать только шаблоны с `origin = 'CLIENT'`
- ✅ **`handleUpdateComponentTemplate()`:** Для CLIENT проверка что может редактировать только шаблоны с `origin = 'CLIENT'`
- ✅ **`handleArchiveComponentTemplate()`:** Для CLIENT проверка прав
- ✅ **`handleDeleteComponentTemplate()`:** Требуется ADMIN роль
- ✅ Добавлено обновление `updatedAtEpoch` при изменениях

#### Обновлён `src/MaintenanceController.php`:
- ✅ Интегрированы `UserContext` и `AccessControlHelper`
- ✅ **`handleGetMaintenanceSessions()`:** Фильтрация для CLIENT по `clientId` (через JOIN с `installations` и `sites`)
- ✅ **`handleGetMaintenanceSession()`:** Проверка прав доступа через `canViewEntity()` (через JOIN)
- ✅ **`handleCreateMaintenanceSession()`:** Автоматическая установка `origin` и `created_by_user_id`, проверка что CLIENT может создавать только для своего клиента
- ✅ **`handleUpdateMaintenanceSession()`:** Проверка прав через `canEditEntityAccess()` (через JOIN)
- ✅ **`handleDeleteMaintenanceSession()`:** Требуется ADMIN роль
- ✅ Добавлено обновление `updatedAtEpoch` при изменениях

### 3. Веб-админка

#### Обновлён `public/admin/users.php`:
- ✅ Добавлена колонка "Роль" с цветными бейджами (ADMIN=красный, ENGINEER=синий, CLIENT=зелёный)
- ✅ Добавлена колонка "Клиент" для отображения привязки пользователя к клиенту
- ✅ Обновлён SQL-запрос для JOIN с таблицей `clients`
- ✅ Добавлена кнопка "Редактировать" для каждого пользователя

#### Обновлён `public/admin/user_add.php`:
- ✅ Добавлен выбор роли: ADMIN, ENGINEER, CLIENT
- ✅ Добавлено поле выбора клиента (отображается только для роли CLIENT)
- ✅ Валидация: для роли CLIENT обязательно указание клиента
- ✅ JavaScript для динамического показа/скрытия поля клиента

#### Создан `public/admin/user_edit.php`:
- ✅ Страница редактирования пользователя
- ✅ Поддержка изменения роли и привязки к клиенту
- ✅ Опциональная смена пароля
- ✅ Валидация: для роли CLIENT обязательно указание клиента
- ✅ Автоматический сброс `client_id` при смене роли с CLIENT на другую

---

## Логика прав доступа

### Правила просмотра (SELECT):

1. **ADMIN и ENGINEER:**
   - Могут просматривать все сущности (независимо от `origin` и `client_id`)

2. **CLIENT:**
   - Может просматривать только сущности, которые принадлежат его клиенту (`client_id = user.client_id`)
   - Видит как данные с `origin = 'CRM'`, так и с `origin = 'CLIENT'`

### Правила редактирования/удаления (UPDATE/DELETE):

1. **ADMIN и ENGINEER:**
   - Могут редактировать/удалять все сущности (независимо от `origin` и `client_id`)

2. **CLIENT:**
   - Может редактировать/удалять только сущности, которые:
     - Принадлежат его клиенту (`client_id = user.client_id`)
     - Имеют `origin = 'CLIENT'` (не может редактировать данные, созданные инженерами в CRM)

### Примеры:

- Клиент создаёт объект в app-client → `origin = 'CLIENT'`, `client_id = клиент_пользователя` → клиент может редактировать
- Инженер создаёт объект в CRM → `origin = 'CRM'`, `client_id = клиент` → клиент видит, но не может редактировать
- Клиент пытается изменить объект с `origin = 'CRM'` → доступ запрещён (403 Forbidden)

---

## Изменения в REST API

### Эндпоинт `/auth/login`:
- Теперь возвращает `clientId` в объекте `user`:
```json
{
  "token": "...",
  "user": {
    "id": "uuid",
    "login": "user_login",
    "role": "CLIENT",
    "clientId": "uuid-клиента",
    ...
  }
}
```

### Эндпоинт `/auth/me`:
- Теперь возвращает `clientId` и корректную роль из поля `users.role`

### Эндпоинты GET / LIST (все контроллеры):
- **CLIENT:** Получает только данные своего клиента (`client_id = user.clientId`)
- **ENGINEER/ADMIN:** Получают все данные

### Эндпоинты CREATE (все контроллеры):
- **CLIENT:** Автоматически устанавливается `origin = 'CLIENT'`, `client_id = user.clientId`, `created_by_user_id = userId`
- **ENGINEER:** Автоматически устанавливается `origin = 'CRM'`, `client_id` из родительской сущности, `created_by_user_id = userId`
- **ADMIN:** Может явно указать `origin` и `client_id`, иначе по умолчанию `CRM`

### Эндпоинты UPDATE/DELETE (все контроллеры):
- **CLIENT:** Может редактировать/удалять только свои данные (`client_id = user.clientId` AND `origin = 'CLIENT'`)
- **ENGINEER/ADMIN:** Могут редактировать/удалять все данные

### Эндпоинт `/sync/pull`:
- **CLIENT:** Получает только данные своего клиента (`client_id = user.clientId`)
- **ENGINEER/ADMIN:** Получают все данные (с сохранением обратной совместимости через `user_membership`)

### Эндпоинт `/sync/push`:
- **CLIENT:** При создании новых записей автоматически устанавливается `origin = 'CLIENT'`, `client_id = user.clientId`, `created_by_user_id = userId`
- **ENGINEER:** При создании новых записей автоматически устанавливается `origin = 'CRM'`, `client_id` из сущности, `created_by_user_id = userId`
- **CLIENT:** При обновлении существующих записей проверяется право доступа (`canEditEntityAccess`)

---

## Обратная совместимость

- ✅ Все существующие пользователи получат роль `ENGINEER` по умолчанию (безопаснее, чем ADMIN)
- ✅ Все существующие записи в БД получат `origin = 'CRM'` (клиенты не смогут их редактировать)
- ✅ Старая система ролей через `user_role` продолжает работать как fallback
- ✅ Система `user_membership` продолжает работать для контроля доступа к установкам (для ENGINEER)
- ✅ Все существующие REST API контракты сохранены (добавлены только новые поля в ответах)

---

## Полный список изменённых файлов

### Новые файлы:
- `migrations/011_roles_and_ownership.sql` — миграция БД
- `src/UserContext.php` — класс контекста пользователя
- `src/AccessControlHelper.php` — централизованная утилита для проверки прав доступа
- `public/admin/user_edit.php` — страница редактирования пользователя
- `REFACTORING_ROLES_AND_OWNERSHIP.md` — первоначальный отчёт
- `REFACTORING_ROLES_AND_OWNERSHIP_FINAL.md` — этот финальный отчёт
- `DB_SCHEMA_ROLES_AND_OWNERSHIP_FULL.md` — подробная схема БД
- `DB_SCHEMA_FINAL.md` — финальная схема БД

### Изменённые файлы:
- `src/AuthController.php` — обновлён для возврата `clientId`
- `src/SyncController.php` — добавлена фильтрация по ролям в pull и автоматическая установка полей ownership в push
- `src/SitesController.php` — интегрированы проверки прав доступа для всех операций
- `src/InstallationsController.php` — интегрированы проверки прав доступа для всех операций
- `src/ComponentsController.php` — интегрированы проверки прав доступа для всех операций
- `src/ComponentTemplatesController.php` — интегрированы проверки прав доступа для всех операций
- `src/MaintenanceController.php` — интегрированы проверки прав доступа для всех операций
- `src/Utils.php` — обновлена функция `syncUpsert` для корректной обработки boolean значений
- `public/admin/users.php` — добавлены колонки роли и клиента
- `public/admin/user_add.php` — добавлен выбор роли и клиента

---

## Тестирование

### Рекомендуемые тесты:

1. **Создание пользователя CLIENT:**
   - Создать пользователя с ролью CLIENT и привязкой к клиенту
   - Проверить, что `client_id` установлен корректно
   - Проверить, что при логине возвращается `clientId`

2. **Проверка прав доступа (CLIENT):**
   - CLIENT не может редактировать объекты с `origin = 'CRM'`
   - CLIENT может редактировать объекты с `origin = 'CLIENT'` и `client_id = его_клиент`
   - CLIENT видит только данные своего клиента (не видит данные других клиентов)

3. **Проверка прав доступа (ENGINEER/ADMIN):**
   - ENGINEER может редактировать все объекты
   - ADMIN может редактировать все объекты

4. **Синхронизация (`/sync/push`):**
   - При создании объекта через `/sync/push` от CLIENT устанавливается `origin = 'CLIENT'`
   - При создании объекта через `/sync/push` от ENGINEER устанавливается `origin = 'CRM'`

5. **Синхронизация (`/sync/pull`):**
   - CLIENT получает только данные своего клиента
   - ENGINEER получает все данные

---

## Заключение

Рефакторинг успешно завершён. Система ролей и владения данными полностью интегрирована во все контроллеры, синхронизацию и веб-админку. Все изменения обратно совместимы с существующими данными и REST API контрактами.

**Статус:** ✅ Готово к использованию

---

**Автор:** AI Assistant (Cursor)  
**Дата:** 2025-01-13

