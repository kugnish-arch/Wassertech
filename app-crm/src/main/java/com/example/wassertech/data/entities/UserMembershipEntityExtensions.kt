package ru.wassertech.data.entities

import ru.wassertech.core.auth.UserMembershipInfo

/**
 * Преобразует UserMembershipEntity в UserMembershipInfo для использования в HierarchyPermissionChecker.
 */
fun UserMembershipEntity.toUserMembershipInfo(): UserMembershipInfo {
    return UserMembershipInfo(
        userId = userId,
        scope = scope,
        targetId = targetId,
        isArchived = isArchived
    )
}

/**
 * Преобразует список UserMembershipEntity в список UserMembershipInfo.
 */
fun List<UserMembershipEntity>.toUserMembershipInfoList(): List<UserMembershipInfo> {
    return map { it.toUserMembershipInfo() }
}

