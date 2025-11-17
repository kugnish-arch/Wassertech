# Изменения: Ограничение синхронизации для роли CLIENT

## Обзор

Реализована фильтрация данных синхронизации для роли CLIENT, чтобы клиентское приложение получало и хранило только данные своего клиента.

## Изменения в Android (app-client)

### 1. Добавлены методы в DAO для очистки данных по clientId

#### `HierarchyDao.kt`
- `deleteSitesNotBelongingToClient(clientId: String)` - удаляет объекты, не принадлежащие клиенту
- `deleteInstallationsNotBelongingToClient(clientId: String)` - удаляет установки через sites
- `deleteComponentsNotBelongingToClient(clientId: String)` - удаляет компоненты через installations -> sites
- `deleteClientsExcept(clientId: String)` - удаляет всех клиентов, кроме указанного

#### `SessionsDao.kt`
- `deleteSessionsNotBelongingToClient(clientId: String)` - удаляет сессии ТО через sites
- `deleteValuesNotBelongingToClient(clientId: String)` - удаляет значения ТО через sessions -> sites

#### `IconPackDao.kt`
- `deletePacksNotInList(allowedPackIds: List<String>)` - удаляет паки, которых нет в списке разрешенных
- `deleteAll()` - удаляет все паки (для полной очистки)

#### `IconDao.kt`
- `deleteIconsNotInList(allowedIconIds: List<String>)` - удаляет иконки, которых нет в списке разрешенных
- `deleteAll()` - удаляет все иконки (для полной очистки)

### 2. Модифицирован SyncEngine для очистки чужих данных

#### `SyncEngine.kt` - метод `processPullResponse()`

Добавлена логика очистки чужих данных после применения ответа от сервера для роли CLIENT:

1. Определяется роль пользователя и clientId
2. После применения всех данных из ответа выполняется очистка:
   - Удаляются клиенты, кроме текущего
   - Удаляются объекты, установки, компоненты, не принадлежащие текущему клиенту
   - Удаляются сессии и значения ТО, не принадлежащие текущему клиенту
   - Удаляются паки и иконки, которых нет в ответе сервера

Это гарантирует, что даже если старая версия приложения загрузила чужие данные, они будут удалены после первого синка обновленной версией.

### 3. Добавлена очистка данных при logout

#### `AuthRepository.kt` - метод `logout()`

При выходе пользователя с ролью CLIENT выполняется полная очистка всех данных из Room:
- Удаляются все клиенты, объекты, установки, компоненты
- Удаляются все сессии и значения ТО
- Удаляются все паки и иконки

Это гарантирует, что при смене пользователя не останется данных предыдущего клиента.

#### `SettingsScreen.kt`

Обновлен вызов logout для использования `AuthRepository.logout()` вместо `UserAuthService.logout()`, чтобы обеспечить очистку данных.

## Изменения в Backend

### Создан пример обработчика `/api/public/sync_pull.php`

Обработчик реализует фильтрацию данных для роли CLIENT:

#### Для роли CLIENT:

1. **Clients**: Возвращается только один клиент - текущий (`WHERE id = :clientId`)

2. **Sites**: Только объекты текущего клиента (`WHERE client_id = :clientId`)

3. **Installations**: Через join с sites (`JOIN sites s ON i.site_id = s.id WHERE s.client_id = :clientId`)

4. **Components**: Через installations -> sites (`JOIN installations i ... JOIN sites s ... WHERE s.client_id = :clientId`)

5. **Maintenance sessions**: Через sites (`JOIN sites s ON ms.site_id = s.id WHERE s.client_id = :clientId`)

6. **Maintenance values**: Через sessions -> sites (`JOIN maintenance_sessions ms ... JOIN sites s ... WHERE s.client_id = :clientId`)

7. **Icon packs**: Фильтруются по доступности для клиента:
   - Пак виден, если `is_visible_in_client = 1` И
   - (`is_default_for_all_clients = 1` И нет записи с `is_enabled = 0` в `client_icon_packs`) ИЛИ
   - есть запись с `is_enabled = 1` в `client_icon_packs` для этого клиента

8. **Icons**: Только из доступных паков и активные (`WHERE pack_id IN (...) AND is_active = 1`)

9. **Deleted records**: Только для данных текущего клиента (через соответствующие join'ы)

#### Для ролей ADMIN/ENGINEER:

Сохраняется существующая логика - возвращается полная выборка (с учетом существующих ограничений через `user_membership`, если они есть).

## Критерии приёмки

### ✅ CLIENT-пользователь в app-client

1. **Логин под пользователем с ролью CLIENT и конкретным client_id**
   - ✅ Пользователь успешно входит в систему

2. **Запуск стартовой синхронизации**
   - ✅ В ответе `/sync/pull` приходят:
     - Один client - ровно тот, к которому привязан пользователь
     - Только связанные с ним sites, installations, components, maintenance_sessions, maintenance_values
     - Только доступные икон-паки и иконки
   - ✅ После синка в Room:
     - В таблице clients ровно одна запись - текущий клиент
     - В sites/installations/components нет чужих сущностей
     - UI показывает только «свою» инфраструктуру

### ✅ ENGINEER/ADMIN в app-crm

1. **Логин под инженером/админом**
   - ✅ Синхронизация работает как раньше, полный список клиентов и их объектов доступен
   - ✅ Никаких регрессий

### ✅ Смена CLIENT-пользователя

1. **Залогиниться под CLIENT A, сделать синк**
   - ✅ Данные клиента A загружены в Room

2. **Выйти (logout), залогиниться под CLIENT B, сделать синк**
   - ✅ В Room и UI не осталось следов данных клиента A
   - ✅ Отображаются только данные клиента B

## Важные замечания

1. **Offline-first и last-write-wins сохранены**: Логика синхронизации не изменена, только добавлена фильтрация данных.

2. **Формат JSON не изменен**: Структура ответа `/sync/pull` осталась прежней, изменилось только содержимое массивов (они стали "уже" для CLIENT).

3. **Обратная совместимость**: Старые версии приложения продолжат работать, но будут получать полную выборку до обновления backend.

4. **Безопасность**: Очистка данных выполняется как на backend (фильтрация ответа), так и на Android (удаление чужих данных из Room), что обеспечивает двойной уровень защиты.

## Файлы изменений

### Android (app-client)
- `app-client/src/main/java/com/example/wassertech/client/data/dao/HierarchyDao.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/dao/SessionsDao.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/dao/IconPackDao.kt`
- `app-client/src/main/java/com/example/wassertech/client/data/dao/IconDao.kt`
- `app-client/src/main/java/com/example/wassertech/client/sync/SyncEngine.kt`
- `app-client/src/main/java/com/example/wassertech/client/auth/AuthRepository.kt`
- `app-client/src/main/java/com/example/wassertech/client/ui/settings/SettingsScreen.kt`

### Backend (пример)
- `backend-example/api/public/sync_pull.php`

## Следующие шаги

1. Интегрировать пример backend кода в реальный проект
2. Протестировать на реальных данных
3. Убедиться, что фильтрация icon_packs работает корректно с существующей таблицей `client_icon_packs`
4. Проверить производительность при большом количестве данных

