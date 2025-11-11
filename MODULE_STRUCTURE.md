# Модульная структура проекта Wassertech

## Обзор

Проект разделен на модули для улучшения организации кода и возможности переиспользования компонентов.

## Структура модулей

### Core модули

#### `:core:ui`
**Назначение:** Базовые UI компоненты, темы, цвета, типографика, Splash Screen

**Содержимое:**
- `theme/Theme.kt` - темы, цвета, типографика
- `splash/SplashScreen.kt` - базовый экран загрузки

**Зависимости:**
- Compose BOM
- Material3
- Core KTX

#### `:core:network`
**Назначение:** Сетевой слой (Retrofit/OkHttp клиент, interceptors, error mapping)

**Содержимое:**
- `ApiClient.kt` - Retrofit клиент
- `AuthInterceptor.kt` - Interceptor для добавления токена авторизации
- `ErrorMapper.kt` - Маппинг сетевых ошибок

**Зависимости:**
- `:core:auth` - для TokenManager
- Retrofit
- OkHttp
- Coroutines

#### `:core:auth`
**Назначение:** Управление авторизацией (без UI)

**Содержимое:**
- `TokenManager.kt` - управление токенами
- `SessionStore.kt` - хранение сессии пользователя
- `AuthRepository.kt` - репозиторий авторизации
- `UserRole.kt` - роли и права пользователей

**Зависимости:**
- Core KTX

### Feature модули

#### `:feature:auth`
**Назначение:** UI экраны авторизации (Compose)

**Содержимое:**
- `LoginScreen.kt` - экран входа
- `ForgotPasswordScreen.kt` - экран восстановления пароля
- `OnboardingScreen.kt` - экран онбординга

**Зависимости:**
- `:core:ui` - темы и базовые компоненты
- `:core:auth` - AuthRepository, TokenManager
- Compose BOM
- Navigation Compose
- Foundation Pager (для Onboarding)

### App модули

#### `:app-crm`
**Назначение:** Текущее CRM приложение

**Зависимости:**
- Все core модули
- Все feature модули
- Room
- Другие специфичные зависимости

#### `:app-client`
**Назначение:** Новое клиентское приложение

**Зависимости:**
- Все core модули
- Все feature модули
- Другие специфичные зависимости

## Зависимости между модулями

```
:app-crm / :app-client
    ├── :core:ui
    ├── :core:network
    │   └── :core:auth
    ├── :core:auth
    └── :feature:auth
        ├── :core:ui
        └── :core:auth
```

## Настройка

### settings.gradle.kts
Все модули включены в `settings.gradle.kts`:
```kotlin
include(":core:ui")
include(":core:network")
include(":core:auth")
include(":feature:auth")
include(":app-crm")
include(":app-client")
```

### build.gradle.kts
Каждый модуль имеет свой `build.gradle.kts` с:
- AGP 8.x
- Kotlin
- Compose (для UI/feature модулей)
- Минимальными зависимостями

## Следующие шаги

1. **Перенос существующего кода:**
   - Переместить существующие файлы из `app/` в `app-crm/`
   - Обновить импорты для использования модулей

2. **Реализация API:**
   - Настроить реальный BASE_URL в `ApiClient`
   - Реализовать API интерфейсы для Retrofit
   - Подключить к `LoginScreen`

3. **Ресурсы:**
   - Переместить drawable ресурсы в соответствующие модули
   - Обновить SplashScreen для использования ресурсов

4. **Тестирование:**
   - Убедиться, что все модули собираются
   - Проверить зависимости между модулями

