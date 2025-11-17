# Финальный отчёт: Рефакторинг Android-приложений под новую систему ролей и прав доступа

**Дата:** 2025-01-13  
**Статус:** Частично завершён (базовая инфраструктура готова)

---

## Краткий обзор

Выполнен рефакторинг базовой инфраструктуры для поддержки новой системы ролей (ADMIN, ENGINEER, CLIENT) и владения данными (origin = CRM/CLIENT) в Android-приложениях Wassertech. Добавлены необходимые поля в Room-сущности, созданы миграции БД, обновлены DTO для синхронизации, созданы утилиты для проверки прав доступа.

---

## Выполненные задачи

### 1. Обновлён UserRole enum (core/auth)
**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/UserRole.kt`

- ✅ Добавлена роль `ENGINEER`
- ✅ Добавлен метод `toServerValue()` для преобразования ролей в серверный формат
- ✅ Обновлён `fromString()` с поддержкой ENGINEER и fallback на ENGINEER по умолчанию
- ✅ USER и VIEWER оставлены для обратной совместимости, маппятся на ENGINEER на сервере

### 2. Создан OriginType enum (core/auth)
**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/OriginType.kt`

- ✅ Вынесен в общий модуль `core/auth`
- ✅ Соответствует серверным значениям "CRM" и "CLIENT"
- ✅ Метод `fromString()` с безопасным fallback на CRM

### 3. Создан UserSession интерфейс и SessionManager (core/auth)
**Файлы:**
- `core/auth/src/main/java/com/example/wassertech/core/auth/UserSession.kt`
- `core/auth/src/main/java/com/example/wassertech/core/auth/SessionManager.kt`

- ✅ Интерфейс `UserSession` с полями: userId, login, role, clientId, name, email
- ✅ Реализация `UserSessionImpl` с методами `isAdmin()`, `isEngineer()`, `isClient()`, `canEditAll()`
- ✅ `SessionManager` для управления сессией с сохранением в SharedPreferences
- ✅ Восстановление сессии при перезапуске приложения

### 4. Создан LocalPermissionChecker (core/auth)
**Файл:** `core/auth/src/main/java/com/example/wassertech/core/auth/LocalPermissionChecker.kt`

- ✅ Утилита для проверки прав доступа на основе ролей и origin
- ✅ Методы для проверки прав на просмотр/редактирование/удаление:
  - `canViewSite()`, `canEditSite()`, `canDeleteSite()`
  - `canViewInstallation()`, `canEditInstallation()`, `canDeleteInstallation()`
  - `canViewComponent()`, `canEditComponent()`, `canDeleteComponent()`
  - `canViewComponentTemplate()`, `canEditComponentTemplate()`, `canDeleteComponentTemplate()`
  - `canViewMaintenanceSession()`, `canEditMaintenanceSession()`, `canCreateMaintenanceSession()`, `canDeleteMaintenanceSession()`
  - `canGeneratePdf()`, `canViewPdf()`, `canCreateEntity()`
- ✅ Логика соответствует серверному AccessControlHelper

### 5. Обновлены Room-сущности в app-crm
**Файлы:**
- `app-crm/src/main/java/com/example/wassertech/data/entities/SiteEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/InstallationEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/MaintenanceSessionEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/MaintananceValueEntity.kt`

- ✅ Добавлены поля `origin: String?` и `created_by_user_id: String?` во все сущности
- ✅ Добавлены индексы для новых полей
- ✅ Все сущности имеют метод `getOriginType()` для получения OriginType

### 6. Обновлены Room-сущности в app-client
**Файлы:**
- `app-client/src/main/java/com/example/wassertech/client/data/entities/SiteEntity.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/InstallationEntity.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentEntity.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentTemplateEntity.kt`

- ✅ Добавлены поля `origin: String?` и `created_by_user_id: String?` во все сущности
- ✅ Обновлены импорты OriginType на `ru.wassertech.core.auth.OriginType`
- ✅ Добавлены индексы для новых полей
- ✅ Все сущности имеют метод `getOriginType()` для получения OriginType

### 7. Созданы миграции БД
**Файлы:**
- `app-crm/src/main/java/com/example/wassertech/data/migrations/MIGRATION_12_13.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/migrations/MIGRATION_10_11.kt`

- ✅ Миграция для app-crm (версия 12 → 13): добавляет поля `origin` и `created_by_user_id` во все таблицы
- ✅ Миграция для app-client (версия 10 → 11): добавляет поля `origin` и `created_by_user_id` во все таблицы
- ✅ Все существующие записи получают `origin = 'CRM'` по умолчанию
- ✅ Созданы индексы для новых полей
- ✅ Версии БД обновлены: app-crm (13), app-client (11)

### 8. Обновлены DTO для синхронизации
**Файлы:**
- `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncSiteDto.kt`
- `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncInstallationDto.kt`
- `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncComponentDto.kt`
- `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncComponentTemplateDto.kt`
- `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncMaintenanceSessionDto.kt`
- `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncMaintenanceValueDto.kt`

- ✅ Добавлены поля `origin: String?` и `created_by_user_id: String?` во все DTO
- ✅ Поля опциональные для обратной совместимости

### 9. Обновлены методы маппинга в SyncEngine (app-crm)
**Файл:** `app-crm/src/main/java/com/example/wassertech/sync/SyncEngine.kt`

- ✅ Обновлены методы `toSyncDto()` для включения полей `origin` и `created_by_user_id`
- ✅ Обновлены методы `toEntity()` для маппинга полей `origin` и `created_by_user_id`
- ✅ По умолчанию устанавливается `origin = "CRM"` для старых данных

### 10. Обновлён LoginResponse DTO
**Файл:** `core/network/src/main/java/com/example/wassertech/core/network/dto/LoginResponse.kt`

- ✅ Добавлен опциональный объект `user: LoginUserDto?` для обратной совместимости
- ✅ Создан `LoginUserDto` с полями: id, login, name, email, role, clientId, permissions

---

## Оставшиеся задачи

### Критичные (для полной работы системы):

#### 1. Интеграция SessionManager с логином
**Требуется:**
- Обновить `AuthRepository` для установки сессии через `SessionManager.setCurrentSession()` после успешного логина
- Парсинг JWT-токена для извлечения `userId`, `role`, `clientId` (если объект `user` не приходит в ответе)
- Или использовать объект `user` из `LoginResponse`, если сервер его возвращает

**Где:**
- `core/auth/src/main/java/com/example/wassertech/core/auth/AuthRepositoryImpl.kt`
- `app-client/src/main/java/com/example/wassertech/client/auth/AuthRepository.kt`
- Возможно, нужен JWT-парсер для извлечения claims из токена

#### 2. Обновление SyncEngine для установки origin и created_by_user_id при создании
**Требуется:**
- При создании новых сущностей в app-crm устанавливать:
  - `origin = "CRM"`
  - `created_by_user_id = currentUser.id`
- При создании новых сущностей в app-client устанавливать:
  - `origin = "CLIENT"`
  - `created_by_user_id = currentUser.id`
  - `clientId = currentUser.clientId` (для sites)

**Где:**
- Все места создания сущностей в app-crm и app-client
- Методы `buildSyncPushRequest()` в SyncEngine (если нужно устанавливать при отправке)

#### 3. Обновление логики создания сущностей
**Требуется:**
- Обновить все ViewModel/UseCase/Repository, которые создают сущности
- Устанавливать `origin` и `created_by_user_id` при создании
- Использовать `SessionManager.getCurrentSession()` для получения текущего пользователя

**Где:**
- Все экраны создания/редактирования в app-crm и app-client
- ViewModel для создания сущностей

#### 4. Применение LocalPermissionChecker в UI app-client
**Требуется:**
- Обновить `SitesScreen`, `SiteDetailScreen`, `ComponentsScreen` для использования `LocalPermissionChecker`
- Скрывать/блокировать кнопки редактирования/удаления для CRM-сущностей
- Скрывать кнопки "Провести ТО" и генерации PDF для CLIENT
- Использовать `SessionManager.getCurrentSession()` вместо локального `UserSessionManager`

**Где:**
- `app-client/src/main/java/com/example/wassertech/client/ui/sites/SitesScreen.kt`
- `app-client/src/main/java/com/example/wassertech/client/ui/sites/SiteDetailScreen.kt`
- `app-client/src/main/java/com/example/wassertech/client/ui/components/ComponentsScreen.kt`

#### 5. Обновление PermissionUtils в app-client
**Требуется:**
- Обновить `app-client/src/main/java/com/example/wassertech/client/permissions/PermissionUtils.kt`
- Использовать `LocalPermissionChecker` из `core/auth` вместо локальной реализации
- Или переписать на основе `LocalPermissionChecker`

### Важные (для корректной работы):

#### 6. Обновление UserMeResponse DTO
**Требуется:**
- Добавить поле `clientId` в `UserMeResponse` (если сервер его возвращает)
- Обновить парсинг ответа `/auth/me` для установки сессии

**Где:**
- `core/network/src/main/java/com/example/wassertech/core/network/dto/UserMeResponse.kt`

#### 7. Миграция app-client с локального UserSessionManager на общий SessionManager
**Требуется:**
- Заменить использование `ru.wassertech.client.auth.UserSessionManager` на `ru.wassertech.core.auth.SessionManager`
- Обновить все импорты в app-client

**Где:**
- Все файлы в app-client, использующие `UserSessionManager`

#### 8. Обновление MaintenanceSessionEntity и MaintenanceValueEntity в app-client
**Требуется:**
- Проверить, есть ли эти сущности в app-client
- Если есть, добавить поля `origin` и `created_by_user_id`

---

## Изменённые файлы

### Новые файлы:
1. `core/auth/src/main/java/com/example/wassertech/core/auth/OriginType.kt`
2. `core/auth/src/main/java/com/example/wassertech/core/auth/UserSession.kt`
3. `core/auth/src/main/java/com/example/wassertech/core/auth/SessionManager.kt`
4. `core/auth/src/main/java/com/example/wassertech/core/auth/LocalPermissionChecker.kt`
5. `app-crm/src/main/java/com/example/wassertech/data/migrations/MIGRATION_12_13.kt`
6. `app-client/src/main/java/com/example/wassertech/client/data/migrations/MIGRATION_10_11.kt`

### Изменённые файлы (core):
1. `core/auth/src/main/java/com/example/wassertech/core/auth/UserRole.kt` - добавлена роль ENGINEER
2. `core/network/src/main/java/com/example/wassertech/core/network/dto/LoginResponse.kt` - добавлен объект user
3. `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncSiteDto.kt` - добавлены origin, created_by_user_id
4. `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncInstallationDto.kt` - добавлены origin, created_by_user_id
5. `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncComponentDto.kt` - добавлены origin, created_by_user_id
6. `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncComponentTemplateDto.kt` - добавлены origin, created_by_user_id
7. `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncMaintenanceSessionDto.kt` - добавлены origin, created_by_user_id
8. `core/network/src/main/java/com/example/wassertech/core/network/dto/sync/SyncMaintenanceValueDto.kt` - добавлены origin, created_by_user_id

### Изменённые файлы (app-crm):
1. `app-crm/src/main/java/com/example/wassertech/data/entities/SiteEntity.kt` - добавлены origin, created_by_user_id
2. `app-crm/src/main/java/com/example/wassertech/data/entities/InstallationEntity.kt` - добавлены origin, created_by_user_id
3. `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt` - добавлены origin, created_by_user_id
4. `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt` - добавлены origin, created_by_user_id
5. `app-crm/src/main/java/com/example/wassertech/data/entities/MaintenanceSessionEntity.kt` - добавлены origin, created_by_user_id
6. `app-crm/src/main/java/com/example/wassertech/data/entities/MaintananceValueEntity.kt` - добавлены origin, created_by_user_id
7. `app-crm/src/main/java/com/example/wassertech/data/AppDatabase.kt` - версия 13, добавлена миграция MIGRATION_12_13
8. `app-crm/src/main/java/com/example/wassertech/sync/SyncEngine.kt` - обновлены методы toSyncDto и toEntity

### Изменённые файлы (app-client):
1. `app-client/src/main/java/com/example/wassertech/client/data/entities/SiteEntity.kt` - добавлены origin, created_by_user_id, обновлены импорты
2. `app-client/src/main/java/com/example/wassertech/client/data/entities/InstallationEntity.kt` - добавлены origin, created_by_user_id, обновлены импорты
3. `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentEntity.kt` - добавлены origin, created_by_user_id, обновлены импорты
4. `app-client/src/main/java/com/example/wassertech/client/data/entities/ComponentTemplateEntity.kt` - добавлены origin, created_by_user_id, обновлены импорты
5. `app-client/src/main/java/com/example/wassertech/client/data/AppDatabase.kt` - версия 11, добавлена миграция MIGRATION_10_11

---

## Миграции БД

### app-crm: MIGRATION_12_13
- Версия БД: 12 → 13
- Добавляет поля `origin` и `created_by_user_id` в таблицы:
  - `sites`
  - `installations`
  - `components`
  - `component_templates`
  - `maintenance_sessions`
  - `maintenance_values`
- Все существующие записи получают `origin = 'CRM'`
- Созданы индексы для новых полей

### app-client: MIGRATION_10_11
- Версия БД: 10 → 11
- Добавляет поля `origin` и `created_by_user_id` в таблицы:
  - `sites`
  - `installations`
  - `components`
  - `component_templates`
  - `maintenance_sessions` (если существует)
  - `maintenance_values` (если существует)
- Все существующие записи получают `origin = 'CRM'`
- Созданы индексы для новых полей
- Миграция устойчива к повторному запуску (проверяет существование полей)

---

## Использование LocalPermissionChecker

### Пример использования в UI:

```kotlin
val currentUser = SessionManager.getInstance(context).getCurrentSession()
val site = ... // получить из БД

if (currentUser != null && LocalPermissionChecker.canEditSite(
    user = currentUser,
    siteClientId = site.clientId,
    origin = site.getOriginType()
)) {
    // Показать кнопку редактирования
} else {
    // Скрыть кнопку редактирования
}
```

### Правила прав доступа:

1. **ADMIN и ENGINEER:**
   - Могут просматривать/редактировать/удалять все сущности
   - При создании устанавливается `origin = "CRM"`

2. **CLIENT:**
   - Может просматривать все сущности своего клиента (включая CRM)
   - Может редактировать/удалять только сущности с `origin = "CLIENT"` и принадлежащие его клиенту
   - Не может создавать/редактировать сессии ТО
   - Не может генерировать PDF-отчёты

---

## Недостающая информация

### 1. Формат ответа /auth/login
**Вопрос:** Возвращает ли сервер объект `user` с полями `role` и `clientId` в ответе `/auth/login`?

**Текущее состояние:**
- `LoginResponse` обновлён для поддержки объекта `user`
- Но нужно проверить, что сервер действительно возвращает эти данные
- Если нет, нужен JWT-парсер для извлечения claims из токена

**Требуется:**
- Подтверждение формата ответа `/auth/login`
- Или реализация JWT-парсера для извлечения `userId`, `role`, `clientId` из токена

### 2. Формат ответа /auth/me
**Вопрос:** Возвращает ли сервер поле `clientId` в ответе `/auth/me`?

**Текущее состояние:**
- `UserMeResponse` не содержит поле `clientId`
- Нужно добавить, если сервер его возвращает

**Требуется:**
- Подтверждение формата ответа `/auth/me`
- Обновление `UserMeResponse` при необходимости

### 3. JWT-парсер
**Вопрос:** Есть ли в проекте JWT-парсер для извлечения claims из токена?

**Требуется:**
- Если нет, создать утилиту для парсинга JWT
- Или использовать библиотеку (например, `io.jsonwebtoken:jjwt`)

---

## Рекомендации по дальнейшей работе

### Приоритет 1 (критично):
1. Интегрировать `SessionManager` с логином (парсинг JWT или использование объекта `user` из ответа)
2. Обновить все места создания сущностей для установки `origin` и `created_by_user_id`
3. Применить `LocalPermissionChecker` в UI app-client

### Приоритет 2 (важно):
4. Обновить `PermissionUtils` в app-client для использования `LocalPermissionChecker`
5. Мигрировать app-client с локального `UserSessionManager` на общий `SessionManager`
6. Обновить `UserMeResponse` при необходимости

### Приоритет 3 (желательно):
7. Добавить unit-тесты для `LocalPermissionChecker`
8. Добавить логирование для отладки прав доступа
9. Оптимизировать запросы к БД с учётом фильтрации по `clientId` для CLIENT

---

## Тестирование

### Рекомендуемые тесты:

1. **Миграции БД:**
   - Протестировать миграцию на реальной БД с данными
   - Проверить, что все записи получили `origin = 'CRM'`
   - Проверить создание индексов

2. **Синхронизация:**
   - Проверить, что новые поля корректно отправляются в `/sync/push`
   - Проверить, что новые поля корректно принимаются из `/sync/pull`
   - Проверить маппинг DTO ↔ Entity

3. **Права доступа:**
   - Протестировать `LocalPermissionChecker` для всех ролей
   - Проверить логику для CLIENT (только свои сущности с origin=CLIENT)
   - Проверить логику для ENGINEER/ADMIN (все сущности)

4. **UI app-client:**
   - Проверить скрытие кнопок редактирования для CRM-сущностей
   - Проверить скрытие кнопок "Провести ТО" и генерации PDF
   - Проверить создание новых сущностей с правильным origin

---

## Заключение

Базовая инфраструктура для системы ролей и прав доступа успешно создана:
- ✅ Room-сущности обновлены
- ✅ Миграции БД созданы
- ✅ DTO обновлены
- ✅ Утилиты для проверки прав созданы
- ✅ Методы маппинга обновлены

Осталось завершить интеграцию:
- ⏳ Интеграция SessionManager с логином
- ⏳ Установка origin и created_by_user_id при создании сущностей
- ⏳ Применение прав в UI

Все изменения обратно совместимы с существующими данными и API контрактами.

---

**Автор:** AI Assistant (Cursor)  
**Дата:** 2025-01-13


