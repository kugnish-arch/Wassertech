package ru.wassertech.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.IconPackEntity
import ru.wassertech.client.data.entities.IconEntity
import ru.wassertech.client.data.repository.IconRepository

/**
 * ViewModel для экрана управления икон-паками в app-client.
 * Загружает паки и иконки из локальной БД (Room).
 * Работает только с данными, которые уже синхронизированы с сервера (отфильтрованные для CLIENT).
 */
class ClientIconPacksViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getInstance(application)
    private val iconPackDao = db.iconPackDao()
    private val iconDao = db.iconDao()
    private val iconRepository = IconRepository(application)
    
    /**
     * UI состояние для списка паков.
     */
    data class IconPacksUiState(
        val packs: List<IconPackWithStatus> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedPackIds: Set<String> = emptySet(),
        val isDownloading: Boolean = false,
        val downloadProgress: DownloadProgress? = null // Прогресс загрузки выбранных паков
    )
    
    /**
     * Пак с подсчитанным количеством иконок и статусами.
     */
    data class IconPackWithStatus(
        val pack: IconPackEntity,
        val iconsCount: Int,
        val isDownloaded: Boolean = false, // Пак полностью загружен (проверяется по наличию файлов)
        val hasUpdate: Boolean = false // На сервере есть обновление (проверяется по updatedAtEpoch)
    )
    
    /**
     * Прогресс загрузки нескольких паков.
     */
    data class DownloadProgress(
        val currentPack: Int,
        val totalPacks: Int,
        val currentIcon: Int,
        val totalIcons: Int
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
     * Загружает все паки с подсчётом количества иконок и статусами.
     * В app-client показываются только те паки, которые уже синхронизированы с сервера
     * (сервер фильтрует их по доступности для данного клиента).
     */
    fun loadPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packsState.value = _packsState.value.copy(isLoading = true, error = null)
                
                val packs = iconPackDao.getAll()
                val allIcons = iconDao.getAllActive()
                val selectedPackIds = _packsState.value.selectedPackIds
                
                // Подсчитываем количество иконок для каждого пака и проверяем статусы
                val packsWithStatus = packs.map { pack ->
                    val packIcons = allIcons.filter { it.packId == pack.id }
                    val count = packIcons.size
                    
                    // Проверяем, загружены ли все иконки пака (проверяем наличие файлов)
                    val downloadableIcons = packIcons.filter { icon ->
                        icon.imageUrl.isNullOrBlank().not() && icon.androidResName.isNullOrBlank()
                    }
                    val downloadedIcons = downloadableIcons.count { icon ->
                        iconRepository.isIconDownloaded(icon.id)
                    }
                    val isDownloaded = downloadableIcons.isNotEmpty() && 
                                      downloadedIcons == downloadableIcons.size
                    
                    // В app-client нет lastSyncEpoch, поэтому hasUpdate всегда false
                    // Можно было бы хранить в SharedPreferences, но для простоты оставляем false
                    val hasUpdate = false
                    
                    IconPackWithStatus(
                        pack = pack,
                        iconsCount = count,
                        isDownloaded = isDownloaded,
                        hasUpdate = hasUpdate
                    )
                }
                
                _packsState.value = IconPacksUiState(
                    packs = packsWithStatus,
                    isLoading = false,
                    error = null,
                    selectedPackIds = selectedPackIds
                )
            } catch (e: Exception) {
                _packsState.value = IconPacksUiState(
                    packs = emptyList(),
                    isLoading = false,
                    error = "Ошибка при загрузке паков: ${e.message}",
                    selectedPackIds = _packsState.value.selectedPackIds
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
                val icons = if (pack != null) {
                    iconDao.getByPackId(packId)
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
     * Переключает выбор пака.
     */
    fun togglePackSelection(packId: String) {
        val currentSelected = _packsState.value.selectedPackIds.toMutableSet()
        if (currentSelected.contains(packId)) {
            currentSelected.remove(packId)
        } else {
            currentSelected.add(packId)
        }
        _packsState.value = _packsState.value.copy(selectedPackIds = currentSelected)
    }
    
    /**
     * Очищает выбор паков.
     */
    fun clearSelection() {
        _packsState.value = _packsState.value.copy(selectedPackIds = emptySet())
    }
    
    /**
     * Загружает изображения для выбранных паков.
     */
    fun downloadSelectedPacks() {
        val selectedPackIds = _packsState.value.selectedPackIds.toList()
        if (selectedPackIds.isEmpty()) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packsState.value = _packsState.value.copy(
                    isDownloading = true,
                    downloadProgress = DownloadProgress(0, selectedPackIds.size, 0, 0)
                )
                
                val result = iconRepository.downloadIconPacks(selectedPackIds) { currentPack, totalPacks, currentIcon, totalIcons ->
                    _packsState.value = _packsState.value.copy(
                        downloadProgress = DownloadProgress(currentPack, totalPacks, currentIcon, totalIcons)
                    )
                }
                
                if (result.isSuccess) {
                    // Обновляем список паков после загрузки
                    loadPacks()
                    // Очищаем выбор
                    _packsState.value = _packsState.value.copy(
                        selectedPackIds = emptySet(),
                        isDownloading = false,
                        downloadProgress = null
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                    _packsState.value = _packsState.value.copy(
                        error = errorMsg,
                        isDownloading = false,
                        downloadProgress = null
                    )
                }
            } catch (e: Exception) {
                _packsState.value = _packsState.value.copy(
                    error = "Ошибка: ${e.message}",
                    isDownloading = false,
                    downloadProgress = null
                )
            }
        }
    }
    
    /**
     * Получает локальный путь к изображению иконки.
     */
    suspend fun getLocalIconPath(icon: IconEntity): String? {
        return iconRepository.getLocalIconPath(icon.id)
    }
    
    /**
     * Получает локальный путь к миниатюре иконки.
     */
    suspend fun getLocalThumbnailPath(icon: IconEntity): String? {
        return iconRepository.getLocalThumbnailPath(icon.id)
    }
    
    /**
     * Проверяет, загружена ли иконка локально.
     */
    suspend fun isIconDownloaded(iconId: String): Boolean {
        return iconRepository.isIconDownloaded(iconId)
    }
}


