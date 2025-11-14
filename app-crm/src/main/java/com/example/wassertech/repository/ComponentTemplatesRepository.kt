
package ru.wassertech.repository

import ru.wassertech.data.dao.ComponentTemplatesDao
import ru.wassertech.data.entities.ComponentTemplateEntity
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.sync.markUpdatedForSync
import kotlinx.coroutines.flow.Flow

class ComponentTemplatesRepository(private val dao: ComponentTemplatesDao) {
    fun observeAll(): Flow<List<ComponentTemplateEntity>> = dao.observeAll()
    fun observeActive(): Flow<List<ComponentTemplateEntity>> = dao.observeActive()
    suspend fun upsert(item: ComponentTemplateEntity) {
        val existing = dao.getById(item.id)
        val markedItem = if (existing == null) {
            item.markCreatedForSync()
        } else {
            item.markUpdatedForSync()
        }
        dao.upsert(markedItem)
    }
    suspend fun archive(id: String, arch: Boolean) = dao.setArchived(id, arch)
    suspend fun setSort(id: String, order: Int) = dao.setSortOrder(id, order)
    suspend fun delete(item: ComponentTemplateEntity) = dao.delete(item)
    suspend fun getById(id: String) = dao.getById(id)
}
