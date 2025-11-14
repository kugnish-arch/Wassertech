package ru.wassertech.sync

import ru.wassertech.data.entities.*
import ru.wassertech.data.types.SyncStatus

/**
 * Утилиты для корректной проставки sync-мета-полей для синхронизируемых сущностей.
 * 
 * Эти функции должны вызываться в UI/репозиториях при создании/изменении/архивировании сущностей,
 * чтобы SyncEngine мог обнаружить локальные изменения и отправить их через /sync/push.
 * 
 * Примечание: поскольку сущности - это data class с val полями, функции возвращают новые объекты через copy().
 */

/**
 * Получить текущее время в миллисекундах с эпохи Unix.
 */
fun nowEpoch(): Long = System.currentTimeMillis()

/**
 * Пометка новой, только что созданной сущности как локально созданной и требующей синхронизации.
 */
fun ClientEntity.markCreatedForSync(): ClientEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ClientGroupEntity.markCreatedForSync(): ClientGroupEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun SiteEntity.markCreatedForSync(): SiteEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun InstallationEntity.markCreatedForSync(): InstallationEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentEntity.markCreatedForSync(): ComponentEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ChecklistTemplateEntity.markCreatedForSync(): ChecklistTemplateEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ChecklistFieldEntity.markCreatedForSync(): ChecklistFieldEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentTemplateEntity.markCreatedForSync(): ComponentTemplateEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun MaintenanceSessionEntity.markCreatedForSync(): MaintenanceSessionEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun MaintenanceValueEntity.markCreatedForSync(): MaintenanceValueEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = deletedAtEpoch,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

/**
 * Пометка изменённой сущности как требующей синхронизации.
 */
fun ClientEntity.markUpdatedForSync(): ClientEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ClientGroupEntity.markUpdatedForSync(): ClientGroupEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun SiteEntity.markUpdatedForSync(): SiteEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun InstallationEntity.markUpdatedForSync(): InstallationEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentEntity.markUpdatedForSync(): ComponentEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ChecklistTemplateEntity.markUpdatedForSync(): ChecklistTemplateEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ChecklistFieldEntity.markUpdatedForSync(): ChecklistFieldEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentTemplateEntity.markUpdatedForSync(): ComponentTemplateEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun MaintenanceSessionEntity.markUpdatedForSync(): MaintenanceSessionEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun MaintenanceValueEntity.markUpdatedForSync(): MaintenanceValueEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

/**
 * Пометка сущности как архивированной.
 */
fun ClientEntity.markArchivedForSync(): ClientEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ClientGroupEntity.markArchivedForSync(): ClientGroupEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun SiteEntity.markArchivedForSync(): SiteEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun InstallationEntity.markArchivedForSync(): InstallationEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentEntity.markArchivedForSync(): ComponentEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ChecklistTemplateEntity.markArchivedForSync(): ChecklistTemplateEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentTemplateEntity.markArchivedForSync(): ComponentTemplateEntity {
    val now = nowEpoch()
    return copy(
        isArchived = true,
        archivedAtEpoch = archivedAtEpoch ?: now,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

/**
 * Пометка сущности как разархивированной.
 */
fun ClientEntity.markUnarchivedForSync(): ClientEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ClientGroupEntity.markUnarchivedForSync(): ClientGroupEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun SiteEntity.markUnarchivedForSync(): SiteEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun InstallationEntity.markUnarchivedForSync(): InstallationEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentEntity.markUnarchivedForSync(): ComponentEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ChecklistTemplateEntity.markUnarchivedForSync(): ChecklistTemplateEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentTemplateEntity.markUnarchivedForSync(): ComponentTemplateEntity {
    val now = nowEpoch()
    return copy(
        isArchived = false,
        archivedAtEpoch = null,
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

/**
 * Пометка сущности как логически удалённой (если используется deletedAtEpoch).
 * Вызывать только там, где по бизнес-логике удаление именно синхронизируется.
 */
fun ClientEntity.markDeletedForSync(): ClientEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        deletedAtEpoch = deletedAtEpoch ?: now,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun SiteEntity.markDeletedForSync(): SiteEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        deletedAtEpoch = deletedAtEpoch ?: now,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun InstallationEntity.markDeletedForSync(): InstallationEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        deletedAtEpoch = deletedAtEpoch ?: now,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

fun ComponentEntity.markDeletedForSync(): ComponentEntity {
    val now = nowEpoch()
    return copy(
        createdAtEpoch = if (createdAtEpoch == 0L) now else createdAtEpoch,
        deletedAtEpoch = deletedAtEpoch ?: now,
        updatedAtEpoch = now,
        dirtyFlag = true,
        syncStatus = SyncStatus.QUEUED.value
    )
}

