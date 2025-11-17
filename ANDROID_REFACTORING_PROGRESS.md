# Промежуточный отчёт: Рефакторинг Android-приложений под новую систему ролей

**Дата:** 2025-01-13  
**Статус:** В процессе

---

## Выполнено

### 1. Обновлён UserRole enum (core/auth)
- ✅ Добавлена роль `ENGINEER`
- ✅ Добавлен метод `toServerValue()` для преобразования ролей в серверный формат
- ✅ Обновлён `fromString()` с поддержкой ENGINEER и fallback на ENGINEER по умолчанию

### 2. Создан OriginType enum (core/auth)
- ✅ Вынесен в общий модуль `core/auth`
- ✅ Соответствует серверным значениям "CRM" и "CLIENT"

### 3. Создан UserSession интерфейс (core/auth)
- ✅ Интерфейс `UserSession` с полями: userId, login, role, clientId, name, email
- ✅ Реализация `UserSessionImpl` с методами `isAdmin()`, `isEngineer()`, `isClient()`, `canEditAll()`

### 4. Обновлены Room-сущности в app-crm
- ✅ `SiteEntity` - добавлены поля `origin` и `created_by_user_id`
- ✅ `InstallationEntity` - добавлены поля `origin` и `created_by_user_id`
- ✅ `ComponentEntity` - добавлены поля `origin` и `created_by_user_id`
- ✅ `ComponentTemplateEntity` - добавлены поля `origin` и `created_by_user_id`
- ✅ `MaintenanceSessionEntity` - добавлены поля `origin` и `created_by_user_id`
- ✅ `MaintenanceValueEntity` - добавлены поля `origin` и `created_by_user_id`
- ✅ Все сущности имеют метод `getOriginType()` для получения OriginType

### 5. Создана миграция БД для app-crm
- ✅ `MIGRATION_12_13` - добавляет поля `origin` и `created_by_user_id` во все таблицы
- ✅ Все существующие записи получают `origin = 'CRM'`
- ✅ Созданы индексы для новых полей
- ✅ Версия БД обновлена с 12 до 13

---

## В процессе

### 6. Обновление Room-сущностей в app-client
- ⏳ Нужно обновить сущности аналогично app-crm (уже частично сделано ранее)
- ⏳ Обновить импорты OriginType на `ru.wassertech.core.auth.OriginType`

### 7. Создание миграции БД для app-client
- ⏳ Создать `MIGRATION_10_11` для app-client
- ⏳ Обновить версию БД с 10 до 11

### 8. Обновление DTO для синхронизации
- ⏳ Добавить поля `origin` и `created_by_user_id` в:
  - `SyncSiteDto`
  - `SyncInstallationDto`
  - `SyncComponentDto`
  - `SyncComponentTemplateDto`
  - `SyncMaintenanceSessionDto`
  - `SyncMaintenanceValueDto`

### 9. Обновление методов toSyncDto и toEntity
- ⏳ Обновить `SyncEngine.toSyncDto()` методы для включения новых полей
- ⏳ Обновить `SyncEngine.toEntity()` методы для маппинга новых полей

### 10. Создание LocalPermissionChecker
- ⏳ Создать утилиту для проверки прав доступа на основе:
  - Роли пользователя (UserSession.role)
  - Origin сущности (origin = "CRM" или "CLIENT")
  - Принадлежности клиенту (clientId)

### 11. Обновление SyncEngine
- ⏳ При создании новых сущностей в app-crm устанавливать:
  - `origin = "CRM"`
  - `created_by_user_id = currentUser.id`
- ⏳ При создании новых сущностей в app-client устанавливать:
  - `origin = "CLIENT"`
  - `created_by_user_id = currentUser.id`
  - `clientId = currentUser.clientId`

### 12. Обновление UI app-client
- ⏳ Применить LocalPermissionChecker в UI для скрытия/блокировки кнопок редактирования
- ⏳ Обновить `SitesScreen`, `SiteDetailScreen`, `ComponentsScreen`
- ⏳ Скрыть кнопки "Провести ТО" и генерации PDF для CLIENT

### 13. Обновление логики создания сущностей
- ⏳ Обновить все места создания сущностей для установки origin и created_by_user_id

---

## Необходимые действия

### Критичные (для работы системы):

1. **Завершить миграции БД:**
   - Создать `MIGRATION_10_11` для app-client
   - Протестировать миграции на обеих БД

2. **Обновить DTO:**
   - Добавить поля в DTO для синхронизации
   - Обновить методы маппинга в SyncEngine

3. **Создать LocalPermissionChecker:**
   - Реализовать все методы проверки прав
   - Протестировать логику для всех ролей

4. **Обновить SyncEngine:**
   - Установка origin и created_by_user_id при создании
   - Проверка прав при обновлении (для CLIENT)

### Важные (для корректной работы UI):

5. **Обновить UI app-client:**
   - Применить LocalPermissionChecker
   - Скрыть/заблокировать недоступные действия

6. **Обновить логику создания:**
   - Все места создания сущностей должны устанавливать origin и created_by_user_id

---

## Изменённые файлы

### Новые файлы:
- `core/auth/src/main/java/com/example/wassertech/core/auth/OriginType.kt`
- `core/auth/src/main/java/com/example/wassertech/core/auth/UserSession.kt`
- `app-crm/src/main/java/com/example/wassertech/data/migrations/MIGRATION_12_13.kt`

### Изменённые файлы (app-crm):
- `core/auth/src/main/java/com/example/wassertech/core/auth/UserRole.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/SiteEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/InstallationEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/MaintenanceSessionEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/MaintananceValueEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/AppDatabase.kt`

---

## Следующие шаги

1. Завершить миграцию для app-client
2. Обновить DTO для синхронизации
3. Создать LocalPermissionChecker
4. Обновить SyncEngine
5. Применить права в UI
6. Протестировать все сценарии

---

**Примечание:** Рефакторинг выполняется поэтапно для обеспечения стабильности системы. Все изменения обратно совместимы с существующими данными.


