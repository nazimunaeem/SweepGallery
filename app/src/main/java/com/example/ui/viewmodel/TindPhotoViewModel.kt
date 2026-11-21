package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.DeleteBatchResult
import com.example.data.GalleryRepository
import com.example.data.GitHubReleaseInfo
import com.example.data.GitHubUpdateChecker
import com.example.data.PersistedPreferences
import com.example.data.PreferencesRepository
import com.example.data.SamplePhotoSeeder
import com.example.data.UpdateCheckResult
import com.example.model.AlbumSummary
import com.example.model.MonthSummary
import com.example.model.PhotoItem
import com.example.model.SortOrder
import com.example.model.SwipeDirection
import com.example.model.SwipeRecord
import com.example.model.TindPhotoStats
import com.example.model.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val ALBUM_ALL = "All Photos"
const val MONTH_ALL = "ALL"

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val latestTag: String?) : UpdateUiState
    data class Available(
        val release: GitHubReleaseInfo,
        val bannerDismissed: Boolean,
    ) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

private data class MonthBucket(
    val key: String,
    val label: String,
    val photoIds: List<Long>,
    val totalBytes: Long,
)

private data class BaseGalleryIndex(
    val allPhotosCount: Int = 0,
    val photosById: Map<Long, PhotoItem> = emptyMap(),
    val albumSummaries: List<AlbumSummary> = emptyList(),
    val selectedAlbum: String = ALBUM_ALL,
    val sortedAlbumPhotos: List<PhotoItem> = emptyList(),
    val monthBuckets: List<MonthBucket> = emptyList(),
    val albumTotalBytes: Long = 0L,
)

private data class GalleryFilterState(
    val allPhotos: List<PhotoItem> = emptyList(),
    val trashQueueIds: List<Long> = emptyList(),
    val sessionKeptIds: Set<Long> = emptySet(),
    val undoStack: List<SwipeRecord> = emptyList(),
    val selectedAlbum: String = ALBUM_ALL,
    val selectedMonth: String = MONTH_ALL,
)

private data class SwipeSelectionState(
    val trashQueueIds: List<Long>,
    val sessionKeptIds: Set<Long>,
    val canUndo: Boolean,
    val selectedMonth: String,
)

private data class ComputedDeckData(
    val allPhotosCount: Int = 0,
    val deckPhotos: List<PhotoItem> = emptyList(),
    val trashQueue: List<PhotoItem> = emptyList(),
    val trashTotalBytes: Long = 0L,
    val canUndo: Boolean = false,
    val selectedAlbum: String = ALBUM_ALL,
    val albums: List<AlbumSummary> = emptyList(),
    val selectedMonth: String = MONTH_ALL,
    val months: List<MonthSummary> = emptyList(),
    val nextUnfinishedMonth: MonthSummary? = null,
)

private data class OverlayAndStatusState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val isSeeding: Boolean = false,
    val isDeleting: Boolean = false,
    val inspectingPhoto: PhotoItem? = null,
    val lightboxPhoto: PhotoItem? = null,
    val pendingDeleteSender: IntentSender? = null,
    val pendingDeletePhotos: List<PhotoItem> = emptyList(),
    val snackbarMessage: String? = null,
    val updateStatus: UpdateUiState = UpdateUiState.Idle,
    val showUpdateDialog: Boolean = false,
)

@Immutable
data class TindPhotoUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val isSeeding: Boolean = false,
    val isDeleting: Boolean = false,
    val allPhotosCount: Int = 0,
    val deckPhotos: List<PhotoItem> = emptyList(),
    val trashQueue: List<PhotoItem> = emptyList(),
    val trashTotalBytes: Long = 0L,
    val canUndo: Boolean = false,
    val selectedAlbum: String = ALBUM_ALL,
    val albums: List<AlbumSummary> = emptyList(),
    val selectedMonth: String = MONTH_ALL,
    val months: List<MonthSummary> = emptyList(),
    val nextUnfinishedMonth: MonthSummary? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val includeReviewed: Boolean = false,
    val darkMode: Boolean = true,
    val stats: TindPhotoStats = TindPhotoStats(),
    val inspectingPhoto: PhotoItem? = null,
    val lightboxPhoto: PhotoItem? = null,
    val pendingDeleteSender: IntentSender? = null,
    val snackbarMessage: String? = null,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val updateStatus: UpdateUiState = UpdateUiState.Idle,
    val showUpdateDialog: Boolean = false,
)

class TindPhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val galleryRepository = GalleryRepository(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val samplePhotoSeeder = SamplePhotoSeeder(application)

    private val galleryFilterState = MutableStateFlow(GalleryFilterState())
    private val overlayState = MutableStateFlow(OverlayAndStatusState())

    private val baseGalleryFlow = combine(
        galleryFilterState
            .map { it.allPhotos to it.selectedAlbum }
            .distinctUntilChanged(),
        preferencesRepository.preferencesFlow
            .map { it.sortOrder }
            .distinctUntilChanged(),
    ) { (allPhotos, selectedAlbum), sortOrder ->
        buildBaseGalleryIndex(
            allPhotos = allPhotos,
            selectedAlbum = selectedAlbum,
            sortOrder = sortOrder,
        )
    }.flowOn(Dispatchers.Default)

    private val swipeSelectionFlow = galleryFilterState
        .map {
            SwipeSelectionState(
                trashQueueIds = it.trashQueueIds,
                sessionKeptIds = it.sessionKeptIds,
                canUndo = it.undoStack.isNotEmpty(),
                selectedMonth = it.selectedMonth,
            )
        }
        .distinctUntilChanged()

    private val deckComputationFlow = combine(
        baseGalleryFlow,
        swipeSelectionFlow,
        preferencesRepository.preferencesFlow
            .map { it.keptPhotoIds to it.includeReviewed }
            .distinctUntilChanged(),
    ) { base, swipeSelection, (keptPhotoIds, includeReviewed) ->
        computeDeckData(
            base = base,
            swipe = swipeSelection,
            keptPhotoIds = keptPhotoIds,
            includeReviewed = includeReviewed,
        )
    }.distinctUntilChanged()

    val uiState: StateFlow<TindPhotoUiState> = combine(
        deckComputationFlow,
        overlayState,
        preferencesRepository.preferencesFlow,
    ) { deck, overlay, prefs ->
        val resolvedUpdateStatus = when (val raw = overlay.updateStatus) {
            is UpdateUiState.Available -> raw.copy(
                bannerDismissed = prefs.dismissedUpdateTag == raw.release.tagName,
            )
            else -> raw
        }

        TindPhotoUiState(
            hasPermission = overlay.hasPermission,
            isLoading = overlay.isLoading,
            isSeeding = overlay.isSeeding,
            isDeleting = overlay.isDeleting,
            allPhotosCount = deck.allPhotosCount,
            deckPhotos = deck.deckPhotos,
            trashQueue = deck.trashQueue,
            trashTotalBytes = deck.trashTotalBytes,
            canUndo = deck.canUndo,
            selectedAlbum = deck.selectedAlbum,
            albums = deck.albums,
            selectedMonth = deck.selectedMonth,
            months = deck.months,
            nextUnfinishedMonth = deck.nextUnfinishedMonth,
            sortOrder = prefs.sortOrder,
            includeReviewed = prefs.includeReviewed,
            darkMode = prefs.darkMode,
            stats = prefs.stats,
            inspectingPhoto = overlay.inspectingPhoto,
            lightboxPhoto = overlay.lightboxPhoto,
            pendingDeleteSender = overlay.pendingDeleteSender,
            snackbarMessage = overlay.snackbarMessage,
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE,
            updateStatus = resolvedUpdateStatus,
            showUpdateDialog = overlay.showUpdateDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TindPhotoUiState(),
    )

    init {
        if (BuildConfig.GITHUB_REPO.isNotBlank()) {
            viewModelScope.launch {
                checkForUpdatesInternal(silent = true)
            }
        }
    }

    private fun buildBaseGalleryIndex(
        allPhotos: List<PhotoItem>,
        selectedAlbum: String,
        sortOrder: SortOrder,
    ): BaseGalleryIndex {
        if (allPhotos.isEmpty()) return BaseGalleryIndex(selectedAlbum = selectedAlbum)

        val photosById = HashMap<Long, PhotoItem>(allPhotos.size)
        for (photo in allPhotos) {
            photosById[photo.id] = photo
        }

        val albumSummaries = buildList {
            add(
                AlbumSummary(
                    name = ALBUM_ALL,
                    count = allPhotos.size,
                    totalBytes = allPhotos.sumOf { it.sizeBytes },
                ),
            )
            allPhotos
                .groupBy { it.albumName }
                .entries
                .sortedByDescending { it.value.size }
                .forEach { (album, items) ->
                    add(
                        AlbumSummary(
                            name = album,
                            count = items.size,
                            totalBytes = items.sumOf { it.sizeBytes },
                        ),
                    )
                }
        }

        val albumFiltered = if (selectedAlbum == ALBUM_ALL) {
            allPhotos
        } else {
            allPhotos.filter { it.albumName == selectedAlbum }
        }

        val sortedAlbumPhotos = when (sortOrder) {
            SortOrder.NEWEST -> albumFiltered.sortedByDescending { it.effectiveDateMillis }
            SortOrder.OLDEST -> albumFiltered.sortedBy { it.effectiveDateMillis }
            SortOrder.LARGEST -> albumFiltered.sortedByDescending { it.sizeBytes }
        }

        val monthBuckets = albumFiltered
            .groupBy { it.monthKey }
            .entries
            .sortedByDescending { it.key }
            .map { (monthKey, items) ->
                MonthBucket(
                    key = monthKey,
                    label = items.first().monthLabel,
                    photoIds = items.map { it.id },
                    totalBytes = items.sumOf { it.sizeBytes },
                )
            }

        return BaseGalleryIndex(
            allPhotosCount = allPhotos.size,
            photosById = photosById,
            albumSummaries = albumSummaries,
            selectedAlbum = selectedAlbum,
            sortedAlbumPhotos = sortedAlbumPhotos,
            monthBuckets = monthBuckets,
            albumTotalBytes = albumFiltered.sumOf { it.sizeBytes },
        )
    }

    private fun computeDeckData(
        base: BaseGalleryIndex,
        swipe: SwipeSelectionState,
        keptPhotoIds: Set<Long>,
        includeReviewed: Boolean,
    ): ComputedDeckData {
        val trashSet = swipe.trashQueueIds.toHashSet()
        val trashPhotos = swipe.trashQueueIds.mapNotNull { base.photosById[it] }

        val reviewedIds = HashSet<Long>(trashSet.size + swipe.sessionKeptIds.size + keptPhotoIds.size).apply {
            addAll(trashSet)
            addAll(swipe.sessionKeptIds)
            addAll(keptPhotoIds)
        }

        val excludedFromDeckIds = HashSet<Long>(trashSet.size + swipe.sessionKeptIds.size + keptPhotoIds.size).apply {
            addAll(trashSet)
            addAll(swipe.sessionKeptIds)
            if (!includeReviewed) {
                addAll(keptPhotoIds)
            }
        }

        val monthSummaries = buildList {
            if (base.sortedAlbumPhotos.isNotEmpty()) {
                val allRemaining = base.sortedAlbumPhotos.count { it.id !in reviewedIds }
                add(
                    MonthSummary(
                        key = MONTH_ALL,
                        label = "All Months",
                        totalCount = base.sortedAlbumPhotos.size,
                        remainingCount = allRemaining,
                        totalBytes = base.albumTotalBytes,
                        isCompleted = allRemaining == 0,
                    ),
                )
            }

            base.monthBuckets.forEach { bucket ->
                val remaining = bucket.photoIds.count { it !in reviewedIds }
                add(
                    MonthSummary(
                        key = bucket.key,
                        label = bucket.label,
                        totalCount = bucket.photoIds.size,
                        remainingCount = remaining,
                        totalBytes = bucket.totalBytes,
                        isCompleted = bucket.photoIds.isNotEmpty() && remaining == 0,
                    ),
                )
            }
        }

        val effectiveSelectedMonth = if (
            swipe.selectedMonth == MONTH_ALL ||
            monthSummaries.any { it.key == swipe.selectedMonth }
        ) {
            swipe.selectedMonth
        } else {
            MONTH_ALL
        }

        val deckPhotos = if (effectiveSelectedMonth == MONTH_ALL) {
            base.sortedAlbumPhotos.filter { it.id !in excludedFromDeckIds }
        } else {
            base.sortedAlbumPhotos.filter {
                it.monthKey == effectiveSelectedMonth && it.id !in excludedFromDeckIds
            }
        }

        val nextUnfinishedMonth = monthSummaries.firstOrNull {
            it.key != MONTH_ALL && it.key != effectiveSelectedMonth && !it.isCompleted
        }

        return ComputedDeckData(
            allPhotosCount = base.allPhotosCount,
            deckPhotos = deckPhotos,
            trashQueue = trashPhotos,
            trashTotalBytes = trashPhotos.sumOf { it.sizeBytes },
            canUndo = swipe.canUndo,
            selectedAlbum = base.selectedAlbum,
            albums = base.albumSummaries,
            selectedMonth = effectiveSelectedMonth,
            months = monthSummaries,
            nextUnfinishedMonth = nextUnfinishedMonth,
        )
    }

    fun onPermissionUpdated(granted: Boolean) {
        val previouslyGranted = overlayState.value.hasPermission
        overlayState.update { it.copy(hasPermission = granted) }
        if (granted && (!previouslyGranted || galleryFilterState.value.allPhotos.isEmpty())) {
            refreshGallery()
        }
    }

    fun refreshGallery() {
        viewModelScope.launch {
            overlayState.update { it.copy(isLoading = true) }
            val loaded = galleryRepository.loadDevicePhotos()
            val validIds = loaded.map { it.id }.toSet()
            galleryFilterState.update { current ->
                current.copy(
                    allPhotos = loaded,
                    trashQueueIds = current.trashQueueIds.filter { it in validIds },
                    sessionKeptIds = current.sessionKeptIds.filter { it in validIds }.toSet(),
                )
            }
            overlayState.update { it.copy(isLoading = false) }
        }
    }

    fun seedSamplePhotos() {
        viewModelScope.launch {
            overlayState.update { it.copy(isSeeding = true) }
            val count = samplePhotoSeeder.seedSamplePhotos()
            val loaded = galleryRepository.loadDevicePhotos()
            galleryFilterState.update { it.copy(allPhotos = loaded) }
            overlayState.update {
                it.copy(
                    isSeeding = false,
                    snackbarMessage = if (count > 0) {
                        "Added $count sample photos across 3 months"
                    } else {
                        "Could not add sample photos"
                    },
                )
            }
        }
    }

    fun swipePhoto(photo: PhotoItem, direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.RIGHT_KEEP -> {
                galleryFilterState.update { current ->
                    current.copy(
                        sessionKeptIds = current.sessionKeptIds + photo.id,
                        undoStack = current.undoStack + SwipeRecord(photo, direction),
                    )
                }
                viewModelScope.launch {
                    preferencesRepository.markPhotoKept(photo.id)
                }
            }
            SwipeDirection.LEFT_TRASH -> {
                galleryFilterState.update { current ->
                    val updatedTrash = if (photo.id in current.trashQueueIds) {
                        current.trashQueueIds
                    } else {
                        listOf(photo.id) + current.trashQueueIds
                    }
                    current.copy(
                        trashQueueIds = updatedTrash,
                        undoStack = current.undoStack + SwipeRecord(photo, direction),
                    )
                }
            }
        }
        overlayState.update { current ->
            current.copy(
                lightboxPhoto = if (current.lightboxPhoto?.id == photo.id) null else current.lightboxPhoto,
                inspectingPhoto = if (current.inspectingPhoto?.id == photo.id) null else current.inspectingPhoto,
            )
        }
    }

    fun undoLastSwipe() {
        val lastRecord = galleryFilterState.value.undoStack.lastOrNull() ?: return
        galleryFilterState.update { current ->
            current.copy(
                undoStack = current.undoStack.dropLast(1),
                trashQueueIds = current.trashQueueIds.filterNot { it == lastRecord.photo.id },
                sessionKeptIds = current.sessionKeptIds - lastRecord.photo.id,
                selectedAlbum = if (
                    current.selectedAlbum != ALBUM_ALL &&
                    current.selectedAlbum != lastRecord.photo.albumName
                ) {
                    lastRecord.photo.albumName
                } else {
                    current.selectedAlbum
                },
                selectedMonth = if (
                    current.selectedMonth != MONTH_ALL &&
                    current.selectedMonth != lastRecord.photo.monthKey
                ) {
                    lastRecord.photo.monthKey
                } else {
                    current.selectedMonth
                },
            )
        }
        if (lastRecord.direction == SwipeDirection.RIGHT_KEEP) {
            viewModelScope.launch {
                preferencesRepository.unmarkPhotoKept(lastRecord.photo.id)
            }
        }
    }

    fun restoreFromTrash(photo: PhotoItem) {
        galleryFilterState.update { current ->
            current.copy(
                trashQueueIds = current.trashQueueIds.filterNot { it == photo.id },
                undoStack = current.undoStack.filterNot { it.photo.id == photo.id },
            )
        }
        overlayState.update { it.copy(snackbarMessage = "Restored ${photo.displayName} to deck") }
    }

    fun restoreAllFromTrash() {
        val count = galleryFilterState.value.trashQueueIds.size
        if (count == 0) return
        galleryFilterState.update { current ->
            val trashSet = current.trashQueueIds.toSet()
            current.copy(
                trashQueueIds = emptyList(),
                undoStack = current.undoStack.filterNot { it.photo.id in trashSet },
            )
        }
        overlayState.update { it.copy(snackbarMessage = "Restored $count photos back to deck") }
    }

    fun requestBatchDelete() {
        val currentState = uiState.value
        val targets = currentState.trashQueue
        if (targets.isEmpty() || currentState.isDeleting) return

        viewModelScope.launch {
            overlayState.update { it.copy(isDeleting = true, pendingDeletePhotos = targets) }
            when (val result = galleryRepository.requestDeletePhotos(targets.map { it.uri })) {
                is DeleteBatchResult.RequiresConfirmation -> {
                    overlayState.update {
                        it.copy(
                            isDeleting = false,
                            pendingDeleteSender = result.intentSender,
                        )
                    }
                }
                is DeleteBatchResult.Completed -> {
                    finalizeDeletion(targets)
                }
                is DeleteBatchResult.Failed -> {
                    overlayState.update {
                        it.copy(
                            isDeleting = false,
                            pendingDeletePhotos = emptyList(),
                            snackbarMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onDeleteConfirmationResult(confirmed: Boolean) {
        val targets = overlayState.value.pendingDeletePhotos
        overlayState.update { it.copy(pendingDeleteSender = null) }
        if (confirmed && targets.isNotEmpty()) {
            viewModelScope.launch {
                finalizeDeletion(targets)
            }
        } else {
            overlayState.update {
                it.copy(
                    pendingDeletePhotos = emptyList(),
                    snackbarMessage = "Deletion canceled — photos remain in Trash queue",
                )
            }
        }
    }

    private suspend fun finalizeDeletion(targets: List<PhotoItem>) {
        val freedBytes = targets.sumOf { it.sizeBytes }
        val deletedIds = targets.map { it.id }.toSet()
        preferencesRepository.recordBatchDeletion(
            deletedCount = targets.size,
            freedBytes = freedBytes,
            deletedIds = deletedIds,
        )
        val refreshed = galleryRepository.loadDevicePhotos()
        galleryFilterState.update { current ->
            current.copy(
                allPhotos = refreshed,
                trashQueueIds = current.trashQueueIds.filterNot { it in deletedIds },
                undoStack = current.undoStack.filterNot { it.photo.id in deletedIds },
            )
        }
        overlayState.update {
            it.copy(
                isDeleting = false,
                pendingDeletePhotos = emptyList(),
                snackbarMessage = "Deleted ${targets.size} photos • Freed ${formatFileSize(freedBytes)}",
            )
        }
    }

    fun selectAlbum(album: String) {
        galleryFilterState.update { it.copy(selectedAlbum = album) }
    }

    fun selectMonth(monthKey: String) {
        galleryFilterState.update { it.copy(selectedMonth = monthKey) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            preferencesRepository.setSortOrder(sortOrder)
        }
    }

    fun setIncludeReviewed(include: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIncludeReviewed(include)
            if (include) {
                galleryFilterState.update { it.copy(sessionKeptIds = emptySet()) }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun resetKeptHistory() {
        viewModelScope.launch {
            preferencesRepository.resetKeptHistory()
            galleryFilterState.update { it.copy(sessionKeptIds = emptySet()) }
            overlayState.update { it.copy(snackbarMessage = "Kept photo history cleared") }
        }
    }

    fun openInspector(photo: PhotoItem?) {
        overlayState.update { it.copy(inspectingPhoto = photo) }
    }

    fun openLightbox(photo: PhotoItem?) {
        overlayState.update { it.copy(lightboxPhoto = photo) }
    }

    fun clearSnackbar() {
        overlayState.update { it.copy(snackbarMessage = null) }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            checkForUpdatesInternal(silent = false)
        }
    }

    fun dismissUpdateBanner() {
        val currentStatus = uiState.value.updateStatus
        if (currentStatus is UpdateUiState.Available) {
            viewModelScope.launch {
                preferencesRepository.setDismissedUpdateTag(currentStatus.release.tagName)
            }
        }
    }

    fun setShowUpdateDialog(show: Boolean) {
        overlayState.update { it.copy(showUpdateDialog = show) }
    }

    private suspend fun checkForUpdatesInternal(silent: Boolean) {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank()) {
            if (!silent) {
                overlayState.update {
                    it.copy(updateStatus = UpdateUiState.UpToDate(latestTag = "v${BuildConfig.VERSION_NAME}"))
                }
            }
            return
        }
        overlayState.update { it.copy(updateStatus = UpdateUiState.Checking) }
        when (
            val result = GitHubUpdateChecker.checkLatestRelease(
                repoInput = repo,
                currentVersionName = BuildConfig.VERSION_NAME,
            )
        ) {
            is UpdateCheckResult.UpdateAvailable -> {
                overlayState.update {
                    it.copy(
                        updateStatus = UpdateUiState.Available(
                            release = result.release,
                            bannerDismissed = false,
                        ),
                    )
                }
            }
            is UpdateCheckResult.UpToDate -> {
                overlayState.update {
                    it.copy(
                        updateStatus = if (silent) {
                            UpdateUiState.Idle
                        } else {
                            UpdateUiState.UpToDate(result.latestTag)
                        },
                    )
                }
            }
            is UpdateCheckResult.Error -> {
                overlayState.update {
                    it.copy(
                        updateStatus = if (silent) {
                            UpdateUiState.Idle
                        } else {
                            UpdateUiState.Error(result.message)
                        },
                    )
                }
            }
        }
    }
}
