package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.IconPackEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с паками иконок (icon_packs).
 * Иконки и паки являются справочниками, синхронизируются с сервером через sync/pull.
 */
@Dao
interface IconPackDao {

    /**
     * Получить все паки иконок.
     */
    @Query("SELECT * FROM icon_packs ORDER BY name")
    fun observeAll(): Flow<List<IconPackEntity>>

    /**
     * Получить все паки иконок (моментальный снимок).
     */
    @Query("SELECT * FROM icon_packs ORDER BY name")
    suspend fun getAll(): List<IconPackEntity>

    /**
     * Получить пак по ID.
     */
    @Query("SELECT * FROM icon_packs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IconPackEntity?

    /**
     * Получить пак по коду.
     */
    @Query("SELECT * FROM icon_packs WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): IconPackEntity?

    /**
     * Upsert для синхронизации (last-write-wins).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pack: IconPackEntity)

    /**
     * Upsert нескольких паков.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(packs: List<IconPackEntity>)
}





