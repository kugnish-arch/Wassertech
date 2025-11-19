package ru.wassertech.core.auth

/**
 * Extension-функции для преобразования UserMembershipEntity в UserMembershipInfo.
 * 
 * Эти функции должны быть реализованы в app-crm и app-client модулях,
 * так как они зависят от Room Entity, которые находятся там.
 * 
 * Пример использования в app-crm:
 * ```kotlin
 * fun ru.wassertech.data.entities.UserMembershipEntity.toUserMembershipInfo(): UserMembershipInfo {
 *     return UserMembershipInfo(
 *         userId = userId,
 *         scope = scope,
 *         targetId = targetId,
 *         isArchived = isArchived
 *     )
 * }
 * ```
 * 
 * Пример использования в app-client:
 * ```kotlin
 * fun ru.wassertech.client.data.entities.UserMembershipEntity.toUserMembershipInfo(): UserMembershipInfo {
 *     return UserMembershipInfo(
 *         userId = userId,
 *         scope = scope,
 *         targetId = targetId,
 *         isArchived = isArchived
 *     )
 * }
 * ```
 */

