package com.example.wassertech.data

import kotlinx.coroutines.flow.Flow

class ClientRepository(private val dao: ClientDao) {
    fun observeAll(): Flow<List<ClientEntity>> = dao.observeAll()
    suspend fun add(client: ClientEntity) = dao.insert(client)
    suspend fun remove(client: ClientEntity) = dao.delete(client)
}
