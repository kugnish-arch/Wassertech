# Отчёт: Автоматическая синхронизация с UI для Android-приложений Wassertech

**Дата:** 2025-01-13  
**Версия:** 1.0  
**Проект:** Wassertech Android (app-crm и app-client)

---

## Краткое описание

Реализована система автоматической синхронизации с красивым UI для Android-приложений app-crm и app-client. Система включает:

1. **Блокирующую синхронизацию после логина** с overlay и прогрессом
2. **Неблокирующую синхронизацию при запуске** приложения (фоновую)
3. **Обработку ошибок** с понятными диалогами
4. **Таймауты** и диалог долгой синхронизации
5. **Переиспользуемые компоненты** в core:ui

---

## Что было реализовано

### 1. Core модуль (core:ui)

#### Модели состояния (`core/ui/src/main/java/ru/wassertech/core/ui/sync/`)

- **`SyncUiState.kt`** — состояние UI синхронизации:
  - `isRunning` — запущена ли синхронизация
  - `currentStep` — текущий шаг (SyncStep)
  - `progress` — прогресс от 0.0 до 1.0
  - `error` — ошибка синхронизации (SyncError)
  - `isBlocking` — блокирующая или фоновая синхронизация
  - `showLongSyncDialog` — показывать ли диалог долгой синхронизации

- **`SyncStep`** (enum) — шаги синхронизации:
  - PUSH_CLIENTS, PUSH_SITES, PUSH_INSTALLATIONS, PUSH_COMPONENTS, ...
  - PULL_CLIENTS, PULL_SITES, PULL_INSTALLATIONS, PULL_COMPONENTS, ...
  - COMPLETED

- **`SyncError`** (data class) — информация об ошибке:
  - Тип ошибки (Network, Server, Auth, Parse, Unknown)
  - Сообщение и HTTP код
  - Метод `getUserMessage()` для форматированного сообщения пользователю

#### Оркестратор синхронизации

- **`SyncOrchestrator.kt`** — управление синхронизацией:
  - `startSync(syncFunction, isBlocking)` — запуск синхронизации
  - `cancelSync()` — отмена синхронизации
  - `checkTimeout()` — проверка таймаута (25 секунд)
  - `createSyncFunction()` — вспомогательная функция для создания SyncFunction из SyncEngine

#### ViewModel

- **`SyncViewModel.kt`** — ViewModel для управления состоянием:
  - `startBlockingSync()` — блокирующая синхронизация
  - `startBackgroundSync()` — фоновая синхронизация
  - `retrySync()` — повтор после ошибки
  - `goOffline()` — переход в оффлайн режим
  - `waitMore()` — обработка диалога долгой синхронизации

#### UI компоненты

- **`SyncOverlay.kt`** — компоненты UI:
  - `SyncOverlay()` — блокирующий overlay с прогрессом (после логина)
  - `SyncIndicator()` — неблокирующий индикатор (при запуске)
  - `SyncErrorDialog()` — диалог ошибки синхронизации
  - `LongSyncDialog()` — диалог долгой синхронизации

### 2. App-CRM интеграция

#### Вспомогательные функции

- **`app-crm/src/main/java/com/example/wassertech/sync/SyncHelper.kt`**:
  - `createSyncFunction(context)` — создаёт SyncFunction из SyncEngine
  - `createFullSyncFunction(context)` — для ручной синхронизации

#### Экран синхронизации после логина

- **`app-crm/src/main/java/com/example/wassertech/ui/sync/PostLoginSyncScreen.kt`**:
  - Показывает блокирующий overlay
  - Обрабатывает ошибки и таймауты
  - Переходит на основной экран после успешной синхронизации

#### Фоновая синхронизация

- **`app-crm/src/main/java/com/example/wassertech/ui/sync/BackgroundSyncHandler.kt`**:
  - Неблокирующий индикатор синхронизации
  - Интегрирован в AppScaffold

#### Навигация

- **`app-crm/src/main/java/com/example/wassertech/MainActivity.kt`**:
  - Добавлен маршрут "sync" между логином и основным экраном
  - После логина → экран синхронизации → основной экран

### 3. App-Client интеграция

Аналогично app-crm:

- **`app-client/src/main/java/com/example/wassertech/client/sync/SyncHelper.kt`**
- **`app-client/src/main/java/com/example/wassertech/client/ui/sync/PostLoginSyncScreen.kt`**
- **`app-client/src/main/java/com/example/wassertech/navigation/AppNavigation.kt`** — добавлен маршрут SYNC

---

## Архитектура

### Поток данных

```
LoginScreen → PostLoginSyncScreen → MainScreen
                ↓
         SyncViewModel
                ↓
         SyncOrchestrator
                ↓
         SyncEngine (app-specific)
```

### Состояние синхронизации

```
SyncUiState (StateFlow)
    ├── isRunning: Boolean
    ├── currentStep: SyncStep?
    ├── progress: Float?
    ├── error: SyncError?
    ├── isBlocking: Boolean
    └── showLongSyncDialog: Boolean
```

---

## Сценарии использования

### 1. Первый логин с хорошей сетью

1. Пользователь вводит логин/пароль
2. После успешного логина показывается экран синхронизации
3. Overlay с прогрессом: "Синхронизация с сервером"
4. Прогресс-бар показывает текущий шаг (например, "Загрузка клиентов...")
5. После завершения → переход на основной экран

### 2. Первый логин с ошибкой сети

1. Показывается экран синхронизации
2. Через несколько секунд появляется ошибка
3. Диалог: "Ошибка подключения к серверу"
4. Варианты: [Повторить] [Оффлайн режим]
5. При выборе "Оффлайн режим" → переход на основной экран без синхронизации

### 3. Долгий ответ сервера (таймаут)

1. Синхронизация запущена
2. Через 25 секунд показывается диалог: "Синхронизация занимает больше обычного"
3. Варианты: [Подождать ещё] [Оффлайн режим]
4. При выборе "Подождать ещё" → диалог закрывается, синхронизация продолжается

### 4. Повторный запуск приложения с валидной сессией

1. Приложение запускается
2. SessionManager восстанавливает сессию
3. Если есть сеть → запускается фоновая синхронизация
4. Показывается небольшой индикатор вверху экрана (неблокирующий)
5. Пользователь может работать с приложением во время синхронизации

### 5. Ручная синхронизация из настроек

- Использует ту же систему (SyncViewModel + SyncOrchestrator)
- Можно обновить SettingsScreen для использования новой системы (см. TODO)

---

## Технические детали

### Таймауты

- **Таймаут синхронизации:** 25 секунд (`SYNC_TIMEOUT_MS = 25_000L`)
- **Интервал проверки:** 500 мс (`TIMEOUT_CHECK_INTERVAL_MS = 500L`)

### Прогресс синхронизации

Упрощённая модель прогресса на основе шагов:
- Push фаза: 0.0 - 0.45
- Pull фаза: 0.5 - 0.95
- Completed: 1.0

### Обработка ошибок

Типы ошибок:
- **Network** — нет интернета, timeout
- **Server** — HTTP 5xx
- **Auth** — HTTP 401, 403
- **Parse** — ошибка парсинга ответа
- **Unknown** — неизвестная ошибка

---

## Что осталось сделать (TODO)

### 1. Обновить ручную синхронизацию в настройках

**Файлы:**
- `app-crm/src/main/java/com/example/wassertech/ui/settings/SettingsScreen.kt`
- `app-client/src/main/java/com/example/wassertech/client/ui/settings/SettingsScreen.kt`

**Что сделать:**
- Заменить прямые вызовы `SyncEngine.syncFull()` на использование `SyncViewModel`
- Показывать прогресс синхронизации через `SyncIndicator` или `SyncOverlay`
- Использовать `SyncHelper.createFullSyncFunction()` для создания SyncFunction

**Пример:**
```kotlin
val viewModel: SyncViewModel = viewModel()
val syncState by viewModel.syncState.collectAsState()

Button(onClick = {
    val syncFunction = SyncHelper.createFullSyncFunction(context)
    viewModel.startBlockingSync(syncFunction)
}) {
    if (syncState.isRunning) {
        CircularProgressIndicator(...)
    } else {
        Text("Синхронизировать")
    }
}

// Показываем индикатор или overlay
SyncIndicator(state = syncState)
```

### 2. Добавить фоновую синхронизацию в app-client

**Файл:** `app-client/src/main/java/com/example/wassertech/client/ui/common/AppScaffold.kt`

**Что сделать:**
- Добавить `BackgroundSyncHandler` аналогично app-crm
- Показывать неблокирующий индикатор при запуске приложения

### 3. Улучшить прогресс синхронизации

**Текущая реализация:** Упрощённая модель на основе шагов

**Возможные улучшения:**
- Более детальный прогресс на основе реального количества обработанных записей
- Интеграция с `SyncPushStats` и `SyncPullStats` из SyncEngine
- Показ статистики: "Загружено 10 клиентов, 25 объектов..."

### 4. Оффлайн режим

**Текущая реализация:** При выборе "Оффлайн режим" синхронизация отменяется

**Возможные улучшения:**
- Сохранять флаг оффлайн режима в SharedPreferences
- Не запускать автоматическую синхронизацию, если включен оффлайн режим
- Показывать индикатор оффлайн режима в UI

---

## Изменённые файлы

### Новые файлы

**Core:**
- `core/ui/src/main/java/ru/wassertech/core/ui/sync/SyncUiState.kt`
- `core/ui/src/main/java/ru/wassertech/core/ui/sync/SyncOrchestrator.kt`
- `core/ui/src/main/java/ru/wassertech/core/ui/sync/SyncViewModel.kt`
- `core/ui/src/main/java/ru/wassertech/core/ui/sync/SyncOverlay.kt`

**App-CRM:**
- `app-crm/src/main/java/com/example/wassertech/sync/SyncHelper.kt`
- `app-crm/src/main/java/com/example/wassertech/ui/sync/PostLoginSyncScreen.kt`
- `app-crm/src/main/java/com/example/wassertech/ui/sync/BackgroundSyncHandler.kt`

**App-Client:**
- `app-client/src/main/java/com/example/wassertech/client/sync/SyncHelper.kt`
- `app-client/src/main/java/com/example/wassertech/client/ui/sync/PostLoginSyncScreen.kt`

### Изменённые файлы

- `core/ui/build.gradle.kts` — добавлены зависимости на lifecycle-viewmodel и core:network
- `app-crm/src/main/java/com/example/wassertech/MainActivity.kt` — добавлен маршрут "sync"
- `app-crm/src/main/java/com/example/wassertech/ui/Nav.kt` — добавлен BackgroundSyncHandler
- `app-client/src/main/java/com/example/wassertech/navigation/AppNavigation.kt` — добавлен маршрут SYNC
- `app-client/src/main/java/com/example/wassertech/navigation/AppRoutes.kt` — добавлен маршрут SYNC

---

## Тестирование

### Рекомендуемые тесты

1. **Первый логин с хорошей сетью:**
   - Войти в приложение
   - Проверить, что показывается overlay синхронизации
   - Проверить, что прогресс обновляется
   - Проверить переход на основной экран после завершения

2. **Первый логин с ошибкой сети:**
   - Отключить интернет
   - Войти в приложение
   - Проверить, что показывается диалог ошибки
   - Проверить переход в оффлайн режим

3. **Долгий ответ сервера:**
   - Замедлить сеть (через эмулятор или прокси)
   - Войти в приложение
   - Проверить, что через 25 секунд показывается диалог долгой синхронизации

4. **Повторный запуск приложения:**
   - Залогиниться и закрыть приложение
   - Открыть приложение снова
   - Проверить, что запускается фоновая синхронизация
   - Проверить, что показывается неблокирующий индикатор

5. **Ручная синхронизация:**
   - Открыть настройки
   - Нажать кнопку синхронизации
   - Проверить, что синхронизация запускается (после обновления SettingsScreen)

---

## Заключение

Реализована система автоматической синхронизации с красивым UI для Android-приложений Wassertech. Система включает блокирующую синхронизацию после логина, неблокирующую синхронизацию при запуске, обработку ошибок и таймауты. Все компоненты переиспользуемые и находятся в core:ui модуле.

**Статус:** ✅ Основная функциональность реализована  
**Осталось:** Обновить ручную синхронизацию в настройках (см. TODO выше)

---

**Автор:** AI Assistant (Cursor)  
**Дата:** 2025-01-13

