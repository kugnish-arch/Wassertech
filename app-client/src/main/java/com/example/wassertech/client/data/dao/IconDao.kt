package ru.wassertech.client.data.dao

import androidx.room.*
import ru.wassertech.client.data.entities.IconEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с иконками (icons).
 * Иконки являются справочниками, синхронизируются с сервером через sync/pull.
 */
@Dao
interface IconDao {

    /**
     * Получить все активные иконки.
     */
    @Query("SELECT * FROM icons WHERE isActive = 1 ORDER BY label")
    fun observeAllActive(): Flow<List<IconEntity>>

    /**
     * Получить все активные иконки (моментальный снимок).
     */
    @Query("SELECT * FROM icons WHERE isActive = 1 ORDER BY label")
    suspend fun getAllActive(): List<IconEntity>

    /**
     * Получить иконку по ID.
     */
    @Query("SELECT * FROM icons WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IconEntity?

    /**
     * Получить иконки по списку ID.
     */
    @Query("SELECT * FROM icons WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<IconEntity>

    /**
     * Получить иконки по пакету.
     */
    @Query("SELECT * FROM icons WHERE packId = :packId AND isActive = 1 ORDER BY label")
    suspend fun getByPackId(packId: String): List<IconEntity>
    
    /**
     * Получить все иконки по пакету (включая неактивные).
     * Используется для загрузки изображений.
     */
    @Query("SELECT * FROM icons WHERE packId = :packId ORDER BY label")
    suspend fun getAllByPackId(packId: String): List<IconEntity>

    /**
     * Получить иконки по типу сущности.
     * Фильтрует по entityType: либо "ANY", либо указанный тип.
     */
    @Query("""
        SELECT * FROM icons 
        WHERE isActive = 1 
        AND (entityType = 'ANY' OR entityType = :entityType)
        ORDER BY label
    """)
    suspend fun getByEntityType(entityType: String): List<IconEntity>

    /**
     * Получить иконки по пакету и типу сущности.
     */
    @Query("""
        SELECT * FROM icons 
        WHERE packId = :packId 
        AND isActive = 1 
        AND (entityType = 'ANY' OR entityType = :entityType)
        ORDER BY label
    """)
    suspend fun getByPackIdAndEntityType(packId: String, entityType: String): List<IconEntity>

    /**
     * Upsert для синхронизации (last-write-wins).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(icon: IconEntity)

    /**
     * Upsert нескольких иконок.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(icons: List<IconEntity>)
}


