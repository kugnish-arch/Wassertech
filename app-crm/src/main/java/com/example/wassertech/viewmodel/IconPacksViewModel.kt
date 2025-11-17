package ru.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.IconPackEntity
import ru.wassertech.data.entities.IconEntity
import ru.wassertech.data.entities.IconPackSyncStatusEntity
import ru.wassertech.data.repository.IconRepository

/**
 * ViewModel для экрана управления икон-паками.
 * Загружает паки и иконки из локальной БД (Room) и управляет загрузкой изображений.
 */
class IconPacksViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getInstance(application)
    private val iconPackDao = db.iconPackDao()
    private val iconDao = db.iconDao()
    private val syncStatusDao = db.iconPackSyncStatusDao()
    private val iconRepository = IconRepository(application)
    
    /**
     * UI состояние для списка паков.
     */
    data class IconPacksUiState(
        val packs: List<IconPackWithIconCount> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val downloadProgress: Map<String, Pair<Int, Int>> = emptyMap() // packId -> (downloaded, total)
    )
    
    /**
     * Пак с подсчитанным количеством иконок.
     */
    data class IconPackWithIconCount(
        val pack: IconPackEntity,
        val iconsCount: Int
    )
    
    /**
     * UI состояние для детального просмотра пака.
     */
    data class IconPackDetailUiState(
        val pack: IconPackEntity? = null,
        val icons: List<IconEntity> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _packsState = MutableStateFlow(IconPacksUiState(isLoading = true))
    val packsState: StateFlow<IconPacksUiState> = _packsState.asStateFlow()
    
    private val _detailState = MutableStateFlow(IconPackDetailUiState(isLoading = false))
    val detailState: StateFlow<IconPackDetailUiState> = _detailState.asStateFlow()
    
    init {
        loadPacks()
    }
    
    /**
     * Загружает все паки с подсчётом количества иконок.
     */
    fun loadPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packsState.value = _packsState.value.copy(isLoading = true, error = null)
                
                val packs = iconPackDao.getAll()
                // Используем getAllByPackId для подсчета всех иконок (включая неактивные)
                // так как в экране просмотра паков нужно показывать все иконки
                val packsWithCounts = packs.map { pack ->
                    val count = iconDao.getAllByPackId(pack.id).size
                    IconPackWithIconCount(pack, count)
                }
                
                _packsState.value = IconPacksUiState(
                    packs = packsWithCounts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _packsState.value = IconPacksUiState(
                    packs = emptyList(),
                    isLoading = false,
                    error = "Ошибка при загрузке паков: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Загружает детальную информацию о паке и его иконках.
     */
    fun loadPackDetail(packId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _detailState.value = _detailState.value.copy(isLoading = true, error = null)
                
                val pack = iconPackDao.getById(packId)
                // Используем getAllByPackId для показа всех иконок (включая неактивные)
                // в экране просмотра пака нужно показывать все иконки
                val icons = if (pack != null) {
                    iconDao.getAllByPackId(packId)
                } else {
                    emptyList()
                }
                
                _detailState.value = IconPackDetailUiState(
                    pack = pack,
                    icons = icons,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _detailState.value = IconPackDetailUiState(
                    pack = null,
                    icons = emptyList(),
                    isLoading = false,
                    error = "Ошибка при загрузке пака: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Очищает состояние детального просмотра.
     */
    fun clearDetailState() {
        _detailState.value = IconPackDetailUiState()
    }
    
    /**
     * Загружает изображения для конкретного пака.
     */
    fun downloadPackImages(packId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentProgress = _packsState.value.downloadProgress.toMutableMap()
                currentProgress[packId] = Pair(0, 0)
                _packsState.value = _packsState.value.copy(downloadProgress = currentProgress)
                
                val result = iconRepository.downloadPackImages(packId) { downloaded, total ->
                    val updatedProgress = _packsState.value.downloadProgress.toMutableMap()
                    updatedProgress[packId] = Pair(downloaded, total)
                    _packsState.value = _packsState.value.copy(downloadProgress = updatedProgress)
                }
                
                if (result.isSuccess) {
                    // Обновляем список паков после загрузки
                    loadPacks()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                    _packsState.value = _packsState.value.copy(error = errorMsg)
                }
            } catch (e: Exception) {
                _packsState.value = _packsState.value.copy(error = "Ошибка: ${e.message}")
            }
        }
    }
    
    /**
     * Получает статус синхронизации пака.
     */
    suspend fun getPackSyncStatus(packId: String): IconPackSyncStatusEntity? {
        return syncStatusDao.getByPackId(packId)
    }
    
    /**
     * Получает локальный путь к изображению иконки.
     */
    fun getLocalIconPath(icon: IconEntity): String? {
        return iconRepository.getLocalIconPath(icon)
    }
    
    /**
     * Проверяет, загружена ли иконка локально.
     */
    fun isIconDownloaded(iconId: String): Boolean {
        return iconRepository.isIconDownloaded(iconId)
    }
}


