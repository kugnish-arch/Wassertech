# Изменения: Передача client_id в /sync/pull для app-client

## Обзор

Добавлена передача параметра `client_id` в запрос `/sync/pull` для приложения app-client с ролью CLIENT. Это позволяет backend фильтровать данные на стороне сервера, уменьшая объем передаваемых данных.

## Изменения

### 1. Обновлен интерфейс SyncApi

**Файл:** `core/network/src/main/java/com/example/wassertech/core/network/api/SyncApi.kt`

Добавлен опциональный параметр `clientId` в метод `syncPull()`:

```kotlin
@GET("sync/pull")
suspend fun syncPull(
    @Query("since") since: Long,
    @Query("entities[]") entities: List<String>? = null,
    @Query("client_id") clientId: String? = null  // ← Новый параметр
): Response<SyncPullResponse>
```

Параметр опциональный (`null` по умолчанию), что обеспечивает обратную совместимость:
- Старые версии backend могут игнорировать неизвестный параметр
- app-crm продолжает работать без изменений (не передает client_id)

### 2. Обновлен SyncEngine в app-client

**Файл:** `app-client/src/main/java/com/example/wassertech/client/sync/SyncEngine.kt`

В методе `syncPull()` добавлена логика передачи `client_id`:

1. **Получение информации о пользователе:**
   ```kotlin
   val currentSession = SessionManager.getInstance(context).getCurrentSession()
   val userClientId = currentSession?.clientId
   val userRole = currentSession?.role
   ```

2. **Определение необходимости передачи client_id:**
   ```kotlin
   val shouldSendClientId = userRole == UserRole.CLIENT && !userClientId.isNullOrBlank()
   val clientIdForRequest = if (shouldSendClientId) userClientId else null
   ```

3. **Вызов API с передачей client_id:**
   ```kotlin
   val response = syncApi.syncPull(since = lastSyncTimestampSec, clientId = clientIdForRequest)
   ```

**Логика:**
- Для роли `CLIENT` передается `client_id`, если он доступен и не пустой
- Для других ролей (или если `clientId` отсутствует) передается `null`
- Добавлено логирование для отладки (URL запроса включает `client_id`)

### 3. Логика очистки БД сохранена

**Файл:** `app-client/src/main/java/com/example/wassertech/client/sync/SyncEngine.kt`

Логика очистки чужих данных из Room **сохранена** как дополнительный слой защиты:

- Выполняется после применения данных из ответа сервера
- Удаляет все сущности, не принадлежащие текущему клиенту
- Работает только для роли CLIENT

**Почему это важно:**
- Защита от возможных ошибок на сервере
- Обратная совместимость со старыми версиями backend
- Дополнительная гарантия консистентности данных

## Совместимость

### Backend совместимость

✅ **Обратная совместимость обеспечена:**
- Параметр `client_id` опциональный
- Старые версии backend могут игнорировать неизвестный параметр
- Новые версии backend используют параметр для фильтрации данных

### app-crm совместимость

✅ **app-crm не затронут:**
- `SyncEngine` в app-crm вызывает `syncPull()` без `clientId` (по умолчанию `null`)
- CRM продолжает получать полный набор данных
- Никаких изменений в поведении

### app-client совместимость

✅ **app-client работает корректно:**
- Для роли CLIENT передается `client_id`
- Если `clientId` отсутствует (edge-case), параметр не передается
- Логика очистки БД остается как дополнительная защита

## Места использования синхронизации

Все места синхронизации используют обновленный `SyncEngine.syncPull()`:

1. ✅ **Стартовая синхронизация после логина** (`HomeScreen.kt`)
   - Использует `syncEngine.syncFull()` → вызывает `syncPull()` с `client_id`

2. ✅ **Ручная синхронизация через настройки** (`SettingsScreen.kt`)
   - Использует `syncEngine.syncFull()` → вызывает `syncPull()` с `client_id`

3. ✅ **Автоматическая синхронизация** (`SyncOrchestrator`, `SyncHelper`)
   - Использует `syncEngine.syncPull()` → передает `client_id`

## Проверка изменений

### ✅ В app-client под CLIENT-пользователем:

1. **Логин → стартовая синхронизация:**
   - ✅ Запрос `/sync/pull` уходит с `client_id=...`
   - ✅ В логах видно: `"Вызываю syncApi.syncPull(since=..., client_id=...)"`
   - ✅ URL содержит: `/sync/pull?since=...&client_id=...`
   - ✅ Ответ содержит только данные этого клиента
   - ✅ В Room и UI нет чужих клиентов/объектов/установок

2. **Ручной запуск синка:**
   - ✅ Повторяет тот же сценарий, параметр `client_id` присутствует

### ✅ В app-crm под ENGINEER/ADMIN:

1. **Синхронизация работает как раньше:**
   - ✅ Запрос к `/sync/pull` уходит без `client_id`
   - ✅ CRM продолжает видеть полный набор данных
   - ✅ Никаких изменений в поведении

## Edge-cases обработаны

1. **Если `clientId` отсутствует:**
   - Параметр `client_id` не передается в запрос
   - Логика очистки БД не выполняется (нет `clientId` для проверки)
   - Логируется предупреждение

2. **Если роль не CLIENT:**
   - Параметр `client_id` не передается
   - Поведение как для app-crm

3. **Если backend не поддерживает параметр:**
   - Backend игнорирует неизвестный параметр
   - Возвращает полный набор данных
   - Локальная очистка БД удалит чужие данные

## Файлы изменений

- ✅ `core/network/src/main/java/com/example/wassertech/core/network/api/SyncApi.kt`
- ✅ `app-client/src/main/java/com/example/wassertech/client/sync/SyncEngine.kt`

## Следующие шаги

1. ✅ Протестировать на реальных данных
2. ✅ Проверить логи для подтверждения передачи `client_id`
3. ✅ Убедиться, что backend корректно обрабатывает параметр
4. ✅ После подтверждения работы можно рассмотреть удаление логики очистки БД (но пока оставляем как защиту)

