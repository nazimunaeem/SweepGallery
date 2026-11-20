package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DeleteBatchResult
import com.example.data.GalleryRepository
import com.example.data.PersistedPreferences
import com.example.data.PreferencesRepository
import com.example.data.SamplePhotoSeeder
import com.example.model.AlbumSummary
import com.example.model.MonthSummary
import com.example.model.PhotoItem
import com.example.model.SortOrder
import com.example.model.SwipeDirection
import com.example.model.SwipeRecord
import com.example.model.TindPhotoStats
import com.example.model.formatFileSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val ALBUM_ALL = "All Photos"
const val MONTH_ALL = "ALL"

private data class SessionState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val isSeeding: Boolean = false,
    val isDeleting: Boolean = false,
    val allPhotos: List<PhotoItem> = emptyList(),
    val trashQueueIds: List<Long> = emptyList(),
    val sessionKeptIds: Set<Long> = emptySet(),
    val undoStack: List<SwipeRecord> = emptyList(),
    val selectedAlbum: String = ALBUM_ALL,
    val selectedMonth: String = MONTH_ALL,
    val inspectingPhoto: PhotoItem? = null,
    val lightboxPhoto: PhotoItem? = null,
    val pendingDeleteSender: IntentSender? = null,
    val pendingDeletePhotos: List<PhotoItem> = emptyList(),
    val snackbarMessage: String? = null,
)

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
)

class TindPhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val galleryRepository = GalleryRepository(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val samplePhotoSeeder = SamplePhotoSeeder(application)

    private val sessionState = MutableStateFlow(SessionState())

    val uiState: StateFlow<TindPhotoUiState> = combine(
        sessionState,
        preferencesRepository.preferencesFlow,
    ) { session, prefs ->
        buildUiState(session, prefs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TindPhotoUiState(),
    )

    private fun buildUiState(
        session: SessionState,
        prefs: PersistedPreferences,
    ): TindPhotoUiState {
        val photosById = session.allPhotos.associateBy { it.id }
        val trashSet = session.trashQueueIds.toSet()
        val trashPhotos = session.trashQueueIds.mapNotNull { photosById[it] }

        val reviewedIds = buildSet {
            addAll(trashSet)
            addAll(session.sessionKeptIds)
            addAll(prefs.keptPhotoIds)
        }

        val excludedFromDeckIds = buildSet {
            addAll(trashSet)
            addAll(session.sessionKeptIds)
            if (!prefs.includeReviewed) {
                addAll(prefs.keptPhotoIds)
            }
        }

        val albumFiltered = if (session.selectedAlbum == ALBUM_ALL) {
            session.allPhotos
        } else {
            session.allPhotos.filter { it.albumName == session.selectedAlbum }
        }

        // Build month summaries from the active album selection
        val monthSummaries = buildList {
            if (albumFiltered.isNotEmpty()) {
                val allRemaining = albumFiltered.count { it.id !in reviewedIds }
                add(
                    MonthSummary(
                        key = MONTH_ALL,
                        label = "All Months",
                        totalCount = albumFiltered.size,
                        remainingCount = allRemaining,
                        totalBytes = albumFiltered.sumOf { it.sizeBytes },
                        isCompleted = allRemaining == 0,
                    ),
                )
            }

            albumFiltered
                .groupBy { it.monthKey }
                .entries
                .sortedByDescending { it.key }
                .forEach { (monthKey, items) ->
                    val remaining = items.count { it.id !in reviewedIds }
                    add(
                        MonthSummary(
                            key = monthKey,
                            label = items.first().monthLabel,
                            totalCount = items.size,
                            remainingCount = remaining,
                            totalBytes = items.sumOf { it.sizeBytes },
                            isCompleted = items.isNotEmpty() && remaining == 0,
                        ),
                    )
                }
        }

        val effectiveSelectedMonth = if (
            session.selectedMonth == MONTH_ALL ||
            monthSummaries.any { it.key == session.selectedMonth }
        ) {
            session.selectedMonth
        } else {
            MONTH_ALL
        }

        val monthFiltered = if (effectiveSelectedMonth == MONTH_ALL) {
            albumFiltered
        } else {
            albumFiltered.filter { it.monthKey == effectiveSelectedMonth }
        }

        val sortedFiltered = when (prefs.sortOrder) {
            SortOrder.NEWEST -> monthFiltered.sortedByDescending { it.effectiveDateMillis }
            SortOrder.OLDEST -> monthFiltered.sortedBy { it.effectiveDateMillis }
            SortOrder.LARGEST -> monthFiltered.sortedByDescending { it.sizeBytes }
        }

        val deckPhotos = sortedFiltered.filter { it.id !in excludedFromDeckIds }

        val nextUnfinishedMonth = monthSummaries.firstOrNull {
            it.key != MONTH_ALL && it.key != effectiveSelectedMonth && !it.isCompleted
        }

        val albumSummaries = buildList {
            add(
                AlbumSummary(
                    name = ALBUM_ALL,
                    count = session.allPhotos.size,
                    totalBytes = session.allPhotos.sumOf { it.sizeBytes },
                ),
            )
            session.allPhotos
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

        return TindPhotoUiState(
            hasPermission = session.hasPermission,
            isLoading = session.isLoading,
            isSeeding = session.isSeeding,
            isDeleting = session.isDeleting,
            allPhotosCount = session.allPhotos.size,
            deckPhotos = deckPhotos,
            trashQueue = trashPhotos,
            trashTotalBytes = trashPhotos.sumOf { it.sizeBytes },
            canUndo = session.undoStack.isNotEmpty(),
            selectedAlbum = session.selectedAlbum,
            albums = albumSummaries,
            selectedMonth = effectiveSelectedMonth,
            months = monthSummaries,
            nextUnfinishedMonth = nextUnfinishedMonth,
            sortOrder = prefs.sortOrder,
            includeReviewed = prefs.includeReviewed,
            darkMode = prefs.darkMode,
            stats = prefs.stats,
            inspectingPhoto = session.inspectingPhoto,
            lightboxPhoto = session.lightboxPhoto,
            pendingDeleteSender = session.pendingDeleteSender,
            snackbarMessage = session.snackbarMessage,
        )
    }

    fun onPermissionUpdated(granted: Boolean) {
        val previouslyGranted = sessionState.value.hasPermission
        sessionState.update { it.copy(hasPermission = granted) }
        if (granted && (!previouslyGranted || sessionState.value.allPhotos.isEmpty())) {
            refreshGallery()
        }
    }

    fun refreshGallery() {
        viewModelScope.launch {
            sessionState.update { it.copy(isLoading = true) }
            val loaded = galleryRepository.loadDevicePhotos()
            val validIds = loaded.map { it.id }.toSet()
            sessionState.update { current ->
                current.copy(
                    isLoading = false,
                    allPhotos = loaded,
                    trashQueueIds = current.trashQueueIds.filter { it in validIds },
                    sessionKeptIds = current.sessionKeptIds.filter { it in validIds }.toSet(),
                )
            }
        }
    }

    fun seedSamplePhotos() {
        viewModelScope.launch {
            sessionState.update { it.copy(isSeeding = true) }
            val count = samplePhotoSeeder.seedSamplePhotos()
            val loaded = galleryRepository.loadDevicePhotos()
            sessionState.update {
                it.copy(
                    isSeeding = false,
                    allPhotos = loaded,
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
        viewModelScope.launch {
            when (direction) {
                SwipeDirection.RIGHT_KEEP -> {
                    preferencesRepository.markPhotoKept(photo.id)
                    sessionState.update { current ->
                        current.copy(
                            sessionKeptIds = current.sessionKeptIds + photo.id,
                            undoStack = current.undoStack + SwipeRecord(photo, direction),
                            lightboxPhoto = if (current.lightboxPhoto?.id == photo.id) null else current.lightboxPhoto,
                            inspectingPhoto = if (current.inspectingPhoto?.id == photo.id) null else current.inspectingPhoto,
                        )
                    }
                }
                SwipeDirection.LEFT_TRASH -> {
                    sessionState.update { current ->
                        val updatedTrash = if (photo.id in current.trashQueueIds) {
                            current.trashQueueIds
                        } else {
                            listOf(photo.id) + current.trashQueueIds
                        }
                        current.copy(
                            trashQueueIds = updatedTrash,
                            undoStack = current.undoStack + SwipeRecord(photo, direction),
                            lightboxPhoto = if (current.lightboxPhoto?.id == photo.id) null else current.lightboxPhoto,
                            inspectingPhoto = if (current.inspectingPhoto?.id == photo.id) null else current.inspectingPhoto,
                        )
                    }
                }
            }
        }
    }

    fun undoLastSwipe() {
        val lastRecord = sessionState.value.undoStack.lastOrNull() ?: return
        viewModelScope.launch {
            if (lastRecord.direction == SwipeDirection.RIGHT_KEEP) {
                preferencesRepository.unmarkPhotoKept(lastRecord.photo.id)
            }
            sessionState.update { current ->
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
        }
    }

    fun restoreFromTrash(photo: PhotoItem) {
        sessionState.update { current ->
            current.copy(
                trashQueueIds = current.trashQueueIds.filterNot { it == photo.id },
                undoStack = current.undoStack.filterNot { it.photo.id == photo.id },
                snackbarMessage = "Restored ${photo.displayName} to deck",
            )
        }
    }

    fun restoreAllFromTrash() {
        val count = sessionState.value.trashQueueIds.size
        if (count == 0) return
        sessionState.update { current ->
            val trashSet = current.trashQueueIds.toSet()
            current.copy(
                trashQueueIds = emptyList(),
                undoStack = current.undoStack.filterNot { it.photo.id in trashSet },
                snackbarMessage = "Restored $count photos back to deck",
            )
        }
    }

    fun requestBatchDelete() {
        val currentState = uiState.value
        val targets = currentState.trashQueue
        if (targets.isEmpty() || currentState.isDeleting) return

        viewModelScope.launch {
            sessionState.update { it.copy(isDeleting = true, pendingDeletePhotos = targets) }
            when (val result = galleryRepository.requestDeletePhotos(targets.map { it.uri })) {
                is DeleteBatchResult.RequiresConfirmation -> {
                    sessionState.update {
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
                    sessionState.update {
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
        val targets = sessionState.value.pendingDeletePhotos
        sessionState.update { it.copy(pendingDeleteSender = null) }
        if (confirmed && targets.isNotEmpty()) {
            viewModelScope.launch {
                finalizeDeletion(targets)
            }
        } else {
            sessionState.update {
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
        sessionState.update { current ->
            current.copy(
                isDeleting = false,
                allPhotos = refreshed,
                trashQueueIds = current.trashQueueIds.filterNot { it in deletedIds },
                undoStack = current.undoStack.filterNot { it.photo.id in deletedIds },
                pendingDeletePhotos = emptyList(),
                snackbarMessage = "Deleted ${targets.size} photos • Freed ${formatFileSize(freedBytes)}",
            )
        }
    }

    fun selectAlbum(album: String) {
        sessionState.update { it.copy(selectedAlbum = album) }
    }

    fun selectMonth(monthKey: String) {
        sessionState.update { it.copy(selectedMonth = monthKey) }
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
                sessionState.update { it.copy(sessionKeptIds = emptySet()) }
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
            sessionState.update {
                it.copy(
                    sessionKeptIds = emptySet(),
                    snackbarMessage = "Kept photo history cleared",
                )
            }
        }
    }

    fun openInspector(photo: PhotoItem?) {
        sessionState.update { it.copy(inspectingPhoto = photo) }
    }

    fun openLightbox(photo: PhotoItem?) {
        sessionState.update { it.copy(lightboxPhoto = photo) }
    }

    fun clearSnackbar() {
        sessionState.update { it.copy(snackbarMessage = null) }
    }
}
