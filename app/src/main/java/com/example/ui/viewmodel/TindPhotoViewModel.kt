package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ApkDownloadResult
import com.example.data.BurstDetector
import com.example.data.DeleteBatchResult
import com.example.data.GalleryRepository
import com.example.data.GitHubReleaseInfo
import com.example.data.GitHubUpdateChecker
import com.example.data.InAppUpdater
import com.example.data.PreferencesRepository
import com.example.data.SamplePhotoSeeder
import com.example.data.UpdateCheckResult
import com.example.model.AlbumSummary
import com.example.model.BurstGroup
import com.example.model.MediaKind
import com.example.model.MonthSummary
import com.example.model.PhotoItem
import com.example.model.SmartCategory
import com.example.model.SmartCategorySummary
import com.example.model.SortOrder
import com.example.model.SwipeDirection
import com.example.model.SwipeRecord
import com.example.model.TindPhotoStats
import com.example.model.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val ALBUM_ALL = "All Photos"
const val MONTH_ALL = "ALL"
private const val LARGE_FILE_THRESHOLD_BYTES = 10L * 1024L * 1024L

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

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(
        val tagName: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : UpdateDownloadState
    data class ReadyToInstall(
        val tagName: String,
        val apkPath: String,
    ) : UpdateDownloadState
    data class NeedsInstallPermission(
        val tagName: String,
        val apkPath: String,
    ) : UpdateDownloadState
    data class Failed(
        val tagName: String,
        val message: String,
    ) : UpdateDownloadState
}

private data class MonthBucket(
    val key: String,
    val label: String,
    val photoIds: List<Long>,
    val totalBytes: Long,
)

private data class AlbumBucket(
    val name: String,
    val photoIds: List<Long>,
    val totalBytes: Long,
)

private data class BaseGalleryIndex(
    val allPhotosCount: Int = 0,
    val allPhotos: List<PhotoItem> = emptyList(),
    val photosById: Map<Long, PhotoItem> = emptyMap(),
    val albumBuckets: List<AlbumBucket> = emptyList(),
    val selectedAlbum: String = ALBUM_ALL,
    val selectedSmartCategory: SmartCategory = SmartCategory.ALL,
    val sortedAlbumPhotos: List<PhotoItem> = emptyList(),
    val monthBuckets: List<MonthBucket> = emptyList(),
    val albumTotalBytes: Long = 0L,
    val burstGroups: List<BurstGroup> = emptyList(),
    val burstGroupByPhotoId: Map<Long, BurstGroup> = emptyMap(),
    val burstPhotoIds: Set<Long> = emptySet(),
)

private data class GalleryFilterState(
    val allPhotos: List<PhotoItem> = emptyList(),
    val trashQueueIds: List<Long> = emptyList(),
    val sessionKeptIds: Set<Long> = emptySet(),
    val undoStack: List<SwipeRecord> = emptyList(),
    val selectedAlbum: String = ALBUM_ALL,
    val selectedMonth: String = MONTH_ALL,
    val selectedSmartCategory: SmartCategory = SmartCategory.ALL,
    val pinnedTopPhotoId: Long? = null,
)

private data class SwipeSelectionState(
    val trashQueueIds: List<Long>,
    val sessionKeptIds: Set<Long>,
    val canUndo: Boolean,
    val selectedMonth: String,
    val pinnedTopPhotoId: Long?,
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
    val selectedSmartCategory: SmartCategory = SmartCategory.ALL,
    val smartCategories: List<SmartCategorySummary> = emptyList(),
    val burstGroups: List<BurstGroup> = emptyList(),
    val burstGroupByPhotoId: Map<Long, BurstGroup> = emptyMap(),
)

private data class OverlayAndStatusState(
    val hasPermission: Boolean = false,
    val isLimitedAccess: Boolean = false,
    val isLoading: Boolean = false,
    val isSeeding: Boolean = false,
    val isDeleting: Boolean = false,
    val inspectingPhoto: PhotoItem? = null,
    val lightboxPhoto: PhotoItem? = null,
    val comparingBurstGroup: BurstGroup? = null,
    val showJumpSheet: Boolean = false,
    val pendingDeleteSender: IntentSender? = null,
    val pendingDeletePhotos: List<PhotoItem> = emptyList(),
    val snackbarMessage: String? = null,
    val updateStatus: UpdateUiState = UpdateUiState.Idle,
    val updateDownloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val pendingApkInstallPath: String? = null,
    val showUpdateDialog: Boolean = false,
)

@Immutable
data class TindPhotoUiState(
    val hasPermission: Boolean = false,
    val isLimitedAccess: Boolean = false,
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
    val selectedSmartCategory: SmartCategory = SmartCategory.ALL,
    val smartCategories: List<SmartCategorySummary> = emptyList(),
    val burstGroups: List<BurstGroup> = emptyList(),
    val burstGroupByPhotoId: Map<Long, BurstGroup> = emptyMap(),
    val favoritePhotoIds: Set<Long> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val includeReviewed: Boolean = false,
    val darkMode: Boolean = true,
    val useSystemTrash: Boolean = false,
    val autoCheckUpdates: Boolean = true,
    val stats: TindPhotoStats = TindPhotoStats(),
    val inspectingPhoto: PhotoItem? = null,
    val lightboxPhoto: PhotoItem? = null,
    val comparingBurstGroup: BurstGroup? = null,
    val showJumpSheet: Boolean = false,
    val pendingDeleteSender: IntentSender? = null,
    val snackbarMessage: String? = null,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val updateStatus: UpdateUiState = UpdateUiState.Idle,
    val updateDownloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val pendingApkInstallPath: String? = null,
    val showUpdateDialog: Boolean = false,
)

@OptIn(FlowPreview::class)
class TindPhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val galleryRepository = GalleryRepository(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val samplePhotoSeeder = SamplePhotoSeeder(application)

    private val galleryFilterState = MutableStateFlow(GalleryFilterState())
    private val overlayState = MutableStateFlow(OverlayAndStatusState())
    private var mediaObserverJob: Job? = null
    private var updateDownloadJob: Job? = null
    private var hydratedTrashFromPrefs = false

    private val baseGalleryFlow = combine(
        galleryFilterState
            .map { Triple(it.allPhotos, it.selectedAlbum, it.selectedSmartCategory) }
            .distinctUntilChanged(),
        preferencesRepository.preferencesFlow
            .map { it.sortOrder to it.favoritePhotoIds }
            .distinctUntilChanged(),
    ) { (allPhotos, selectedAlbum, selectedSmartCategory), (sortOrder, favoritePhotoIds) ->
        buildBaseGalleryIndex(
            allPhotos = allPhotos,
            selectedAlbum = selectedAlbum,
            selectedSmartCategory = selectedSmartCategory,
            sortOrder = sortOrder,
            favoritePhotoIds = favoritePhotoIds,
        )
    }.flowOn(Dispatchers.Default)

    private val swipeSelectionFlow = galleryFilterState
        .map {
            SwipeSelectionState(
                trashQueueIds = it.trashQueueIds,
                sessionKeptIds = it.sessionKeptIds,
                canUndo = it.undoStack.isNotEmpty(),
                selectedMonth = it.selectedMonth,
                pinnedTopPhotoId = it.pinnedTopPhotoId,
            )
        }
        .distinctUntilChanged()

    private val deckComputationFlow = combine(
        baseGalleryFlow,
        swipeSelectionFlow,
        preferencesRepository.preferencesFlow
            .map { Triple(it.keptPhotoIds, it.favoritePhotoIds, it.includeReviewed) }
            .distinctUntilChanged(),
    ) { base, swipeSelection, (keptPhotoIds, favoritePhotoIds, includeReviewed) ->
        computeDeckData(
            base = base,
            swipe = swipeSelection,
            keptPhotoIds = keptPhotoIds,
            favoritePhotoIds = favoritePhotoIds,
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
            isLimitedAccess = overlay.isLimitedAccess,
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
            selectedSmartCategory = deck.selectedSmartCategory,
            smartCategories = deck.smartCategories,
            burstGroups = deck.burstGroups,
            burstGroupByPhotoId = deck.burstGroupByPhotoId,
            favoritePhotoIds = prefs.favoritePhotoIds,
            sortOrder = prefs.sortOrder,
            includeReviewed = prefs.includeReviewed,
            darkMode = prefs.darkMode,
            useSystemTrash = prefs.useSystemTrash,
            autoCheckUpdates = prefs.autoCheckUpdates,
            stats = prefs.stats,
            inspectingPhoto = overlay.inspectingPhoto,
            lightboxPhoto = overlay.lightboxPhoto,
            comparingBurstGroup = overlay.comparingBurstGroup,
            showJumpSheet = overlay.showJumpSheet,
            pendingDeleteSender = overlay.pendingDeleteSender,
            snackbarMessage = overlay.snackbarMessage,
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE,
            updateStatus = resolvedUpdateStatus,
            updateDownloadState = overlay.updateDownloadState,
            pendingApkInstallPath = overlay.pendingApkInstallPath,
            showUpdateDialog = overlay.showUpdateDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TindPhotoUiState(),
    )

    init {
        viewModelScope.launch {
            val prefs = runCatching { preferencesRepository.preferencesFlow.first() }.getOrNull()
            if (prefs != null) {
                if (!hydratedTrashFromPrefs && prefs.trashQueueIds.isNotEmpty()) {
                    hydratedTrashFromPrefs = true
                    galleryFilterState.update { current ->
                        if (current.trashQueueIds.isEmpty()) {
                            current.copy(trashQueueIds = prefs.trashQueueIds)
                        } else {
                            current
                        }
                    }
                }
                if (prefs.autoCheckUpdates && BuildConfig.GITHUB_REPO.isNotBlank()) {
                    checkForUpdatesInternal(silent = true)
                }
            }
        }
    }

    private fun buildBaseGalleryIndex(
        allPhotos: List<PhotoItem>,
        selectedAlbum: String,
        selectedSmartCategory: SmartCategory,
        sortOrder: SortOrder,
        favoritePhotoIds: Set<Long>,
    ): BaseGalleryIndex {
        if (allPhotos.isEmpty()) {
            return BaseGalleryIndex(
                selectedAlbum = selectedAlbum,
                selectedSmartCategory = selectedSmartCategory,
            )
        }

        val photosById = HashMap<Long, PhotoItem>(allPhotos.size)
        for (photo in allPhotos) {
            photosById[photo.id] = photo
        }

        val burstGroups = BurstDetector.detectBurstGroups(allPhotos)
        val burstGroupByPhotoId = HashMap<Long, BurstGroup>()
        val burstPhotoIds = HashSet<Long>()
        for (group in burstGroups) {
            for (photo in group.photos) {
                burstPhotoIds.add(photo.id)
                burstGroupByPhotoId[photo.id] = group
            }
        }

        val albumBuckets = buildList {
            add(
                AlbumBucket(
                    name = ALBUM_ALL,
                    photoIds = allPhotos.map { it.id },
                    totalBytes = allPhotos.sumOf { it.sizeBytes },
                ),
            )
            allPhotos
                .groupBy { it.albumName }
                .entries
                .sortedByDescending { it.value.size }
                .forEach { (album, items) ->
                    add(
                        AlbumBucket(
                            name = album,
                            photoIds = items.map { it.id },
                            totalBytes = items.sumOf { it.sizeBytes },
                        ),
                    )
                }
        }

        val smartFiltered = when (selectedSmartCategory) {
            SmartCategory.ALL -> allPhotos
            SmartCategory.BURSTS -> allPhotos.filter { it.id in burstPhotoIds }
            SmartCategory.VIDEOS -> allPhotos.filter { it.mediaKind == MediaKind.VIDEO }
            SmartCategory.LARGE_FILES -> allPhotos.filter { it.sizeBytes >= LARGE_FILE_THRESHOLD_BYTES }
            SmartCategory.SCREENSHOTS -> allPhotos.filter { it.isScreenshot }
            SmartCategory.STARRED -> allPhotos.filter { it.id in favoritePhotoIds }
        }

        val albumFiltered = if (selectedAlbum == ALBUM_ALL) {
            smartFiltered
        } else {
            smartFiltered.filter { it.albumName == selectedAlbum }
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
            allPhotos = allPhotos,
            photosById = photosById,
            albumBuckets = albumBuckets,
            selectedAlbum = selectedAlbum,
            selectedSmartCategory = selectedSmartCategory,
            sortedAlbumPhotos = sortedAlbumPhotos,
            monthBuckets = monthBuckets,
            albumTotalBytes = albumFiltered.sumOf { it.sizeBytes },
            burstGroups = burstGroups,
            burstGroupByPhotoId = burstGroupByPhotoId,
            burstPhotoIds = burstPhotoIds,
        )
    }

    private fun computeDeckData(
        base: BaseGalleryIndex,
        swipe: SwipeSelectionState,
        keptPhotoIds: Set<Long>,
        favoritePhotoIds: Set<Long>,
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
            if (!includeReviewed && base.selectedSmartCategory != SmartCategory.STARRED) {
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

        val filteredDeck = if (effectiveSelectedMonth == MONTH_ALL) {
            base.sortedAlbumPhotos.filter { it.id !in excludedFromDeckIds }
        } else {
            base.sortedAlbumPhotos.filter {
                it.monthKey == effectiveSelectedMonth && it.id !in excludedFromDeckIds
            }
        }

        val pinnedId = swipe.pinnedTopPhotoId
        val deckPhotos = if (pinnedId != null) {
            val pinnedItem = filteredDeck.firstOrNull { it.id == pinnedId }
            if (pinnedItem != null) {
                listOf(pinnedItem) + filteredDeck.filterNot { it.id == pinnedId }
            } else {
                filteredDeck
            }
        } else {
            filteredDeck
        }

        val nextUnfinishedMonth = monthSummaries.firstOrNull {
            it.key != MONTH_ALL && it.key != effectiveSelectedMonth && !it.isCompleted
        }

        val albumSummaries = base.albumBuckets.map { bucket ->
            val remaining = bucket.photoIds.count { it !in reviewedIds }
            AlbumSummary(
                name = bucket.name,
                count = bucket.photoIds.size,
                totalBytes = bucket.totalBytes,
                remainingCount = remaining,
                isCompleted = bucket.photoIds.isNotEmpty() && remaining == 0,
            )
        }

        val smartSummaries = buildSmartCategorySummaries(
            allPhotos = base.allPhotos,
            burstPhotoIds = base.burstPhotoIds,
            favoritePhotoIds = favoritePhotoIds,
        )

        return ComputedDeckData(
            allPhotosCount = base.allPhotosCount,
            deckPhotos = deckPhotos,
            trashQueue = trashPhotos,
            trashTotalBytes = trashPhotos.sumOf { it.sizeBytes },
            canUndo = swipe.canUndo,
            selectedAlbum = base.selectedAlbum,
            albums = albumSummaries,
            selectedMonth = effectiveSelectedMonth,
            months = monthSummaries,
            nextUnfinishedMonth = nextUnfinishedMonth,
            selectedSmartCategory = base.selectedSmartCategory,
            smartCategories = smartSummaries,
            burstGroups = base.burstGroups,
            burstGroupByPhotoId = base.burstGroupByPhotoId,
        )
    }

    private fun buildSmartCategorySummaries(
        allPhotos: List<PhotoItem>,
        burstPhotoIds: Set<Long>,
        favoritePhotoIds: Set<Long>,
    ): List<SmartCategorySummary> {
        val bursts = allPhotos.filter { it.id in burstPhotoIds }
        val videos = allPhotos.filter { it.mediaKind == MediaKind.VIDEO }
        val largeFiles = allPhotos.filter { it.sizeBytes >= LARGE_FILE_THRESHOLD_BYTES }
        val screenshots = allPhotos.filter { it.isScreenshot }
        val starred = allPhotos.filter { it.id in favoritePhotoIds }

        return listOf(
            SmartCategorySummary(SmartCategory.ALL, allPhotos.size, allPhotos.sumOf { it.sizeBytes }),
            SmartCategorySummary(SmartCategory.BURSTS, bursts.size, bursts.sumOf { it.sizeBytes }),
            SmartCategorySummary(SmartCategory.VIDEOS, videos.size, videos.sumOf { it.sizeBytes }),
            SmartCategorySummary(SmartCategory.LARGE_FILES, largeFiles.size, largeFiles.sumOf { it.sizeBytes }),
            SmartCategorySummary(SmartCategory.SCREENSHOTS, screenshots.size, screenshots.sumOf { it.sizeBytes }),
            SmartCategorySummary(SmartCategory.STARRED, starred.size, starred.sumOf { it.sizeBytes }),
        )
    }

    fun onPermissionUpdated(granted: Boolean, isLimitedAccess: Boolean = false) {
        val previouslyGranted = overlayState.value.hasPermission
        val previousLimited = overlayState.value.isLimitedAccess
        overlayState.update {
            it.copy(hasPermission = granted, isLimitedAccess = isLimitedAccess)
        }
        if (granted) {
            startMediaObserverIfNeeded()
            if (!previouslyGranted || previousLimited != isLimitedAccess || galleryFilterState.value.allPhotos.isEmpty()) {
                refreshGallery()
            }
        } else {
            mediaObserverJob?.cancel()
            mediaObserverJob = null
        }
    }

    private fun startMediaObserverIfNeeded() {
        if (mediaObserverJob?.isActive == true) return
        mediaObserverJob = viewModelScope.launch {
            galleryRepository.observeMediaChanges()
                .debounce(700L)
                .collect {
                    if (!overlayState.value.isDeleting && !overlayState.value.isSeeding) {
                        refreshGallerySilent()
                    }
                }
        }
    }

    private suspend fun refreshGallerySilent() {
        try {
            val loaded = galleryRepository.loadDevicePhotos()
            val persistedTrash = if (!hydratedTrashFromPrefs) {
                hydratedTrashFromPrefs = true
                preferencesRepository.preferencesFlow.first().trashQueueIds
            } else {
                emptyList()
            }
            val validIds = loaded.map { it.id }.toSet()
            var updatedTrashIds: List<Long> = emptyList()
            galleryFilterState.update { current ->
                val mergedTrash = (current.trashQueueIds.ifEmpty { persistedTrash }).filter { it in validIds }
                updatedTrashIds = mergedTrash
                current.copy(
                    allPhotos = loaded,
                    trashQueueIds = mergedTrash,
                    sessionKeptIds = current.sessionKeptIds.filter { it in validIds }.toSet(),
                )
            }
            preferencesRepository.saveTrashQueueIds(updatedTrashIds)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // Silent background refresh should never crash
        }
    }

    fun refreshGallery() {
        viewModelScope.launch {
            overlayState.update { it.copy(isLoading = true) }
            try {
                val loaded = galleryRepository.loadDevicePhotos()
                val persistedTrash = if (!hydratedTrashFromPrefs) {
                    hydratedTrashFromPrefs = true
                    preferencesRepository.preferencesFlow.first().trashQueueIds
                } else {
                    emptyList()
                }
                val validIds = loaded.map { it.id }.toSet()
                var updatedTrashIds: List<Long> = emptyList()
                galleryFilterState.update { current ->
                    val mergedTrash = (current.trashQueueIds.ifEmpty { persistedTrash }).filter { it in validIds }
                    updatedTrashIds = mergedTrash
                    current.copy(
                        allPhotos = loaded,
                        trashQueueIds = mergedTrash,
                        sessionKeptIds = current.sessionKeptIds.filter { it in validIds }.toSet(),
                    )
                }
                preferencesRepository.saveTrashQueueIds(updatedTrashIds)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                overlayState.update {
                    it.copy(snackbarMessage = t.localizedMessage ?: "Unable to scan device media")
                }
            } finally {
                overlayState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun seedSamplePhotos() {
        if (overlayState.value.isSeeding) return
        viewModelScope.launch {
            overlayState.update { it.copy(isSeeding = true) }
            try {
                val result = samplePhotoSeeder.seedSamplePhotosResult()
                val loaded = galleryRepository.loadDevicePhotos()
                val validIds = loaded.map { it.id }.toSet()
                galleryFilterState.update { current ->
                    current.copy(
                        allPhotos = loaded,
                        trashQueueIds = current.trashQueueIds.filter { it in validIds },
                        sessionKeptIds = current.sessionKeptIds.filter { it in validIds }.toSet(),
                    )
                }
                val message = when {
                    result.insertedCount > 0 ->
                        "Added ${result.insertedCount} sample photos across 3 months"
                    !result.errorMessage.isNullOrBlank() ->
                        "Could not add sample photos: ${result.errorMessage}"
                    else ->
                        "Could not add sample photos"
                }
                overlayState.update { it.copy(snackbarMessage = message) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                overlayState.update {
                    it.copy(
                        snackbarMessage = t.localizedMessage ?: "Could not add sample photos",
                    )
                }
            } finally {
                overlayState.update { it.copy(isSeeding = false) }
            }
        }
    }

    fun swipePhoto(photo: PhotoItem, direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.RIGHT_KEEP, SwipeDirection.UP_FAVORITE -> {
                val isFavorite = direction == SwipeDirection.UP_FAVORITE
                galleryFilterState.update { current ->
                    current.copy(
                        sessionKeptIds = current.sessionKeptIds + photo.id,
                        undoStack = current.undoStack + SwipeRecord(photo, direction),
                        pinnedTopPhotoId = if (current.pinnedTopPhotoId == photo.id) null else current.pinnedTopPhotoId,
                    )
                }
                viewModelScope.launch {
                    preferencesRepository.markPhotoKept(photo.id, isFavorite = isFavorite)
                }
                if (isFavorite) {
                    overlayState.update { it.copy(snackbarMessage = "Starred ${photo.displayName}") }
                }
            }
            SwipeDirection.LEFT_TRASH -> {
                var persistedQueue: List<Long> = emptyList()
                galleryFilterState.update { current ->
                    val updatedTrash = if (photo.id in current.trashQueueIds) {
                        current.trashQueueIds
                    } else {
                        listOf(photo.id) + current.trashQueueIds
                    }
                    persistedQueue = updatedTrash
                    current.copy(
                        trashQueueIds = updatedTrash,
                        undoStack = current.undoStack + SwipeRecord(photo, direction),
                        pinnedTopPhotoId = if (current.pinnedTopPhotoId == photo.id) null else current.pinnedTopPhotoId,
                    )
                }
                viewModelScope.launch {
                    preferencesRepository.saveTrashQueueIds(persistedQueue)
                    preferencesRepository.recordTrashSwipeStreak()
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
        var updatedTrash: List<Long> = emptyList()
        galleryFilterState.update { current ->
            val nextTrash = current.trashQueueIds.filterNot { it == lastRecord.photo.id }
            updatedTrash = nextTrash
            current.copy(
                undoStack = current.undoStack.dropLast(1),
                trashQueueIds = nextTrash,
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
        viewModelScope.launch {
            if (lastRecord.direction == SwipeDirection.LEFT_TRASH) {
                preferencesRepository.saveTrashQueueIds(updatedTrash)
            } else {
                preferencesRepository.unmarkPhotoKept(
                    lastRecord.photo.id,
                    removeFavorite = lastRecord.direction == SwipeDirection.UP_FAVORITE,
                )
            }
        }
    }

    fun restoreFromTrash(photo: PhotoItem) {
        restoreBatchFromTrash(setOf(photo.id), customMessage = "Restored ${photo.displayName} to deck")
    }

    fun restoreBatchFromTrash(ids: Set<Long>, customMessage: String? = null) {
        if (ids.isEmpty()) return
        var nextTrash: List<Long> = emptyList()
        galleryFilterState.update { current ->
            val filtered = current.trashQueueIds.filterNot { it in ids }
            nextTrash = filtered
            current.copy(
                trashQueueIds = filtered,
                undoStack = current.undoStack.filterNot { it.photo.id in ids },
            )
        }
        viewModelScope.launch {
            preferencesRepository.saveTrashQueueIds(nextTrash)
        }
        val msg = customMessage ?: "Restored ${ids.size} items back to deck"
        overlayState.update { it.copy(snackbarMessage = msg) }
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
        viewModelScope.launch {
            preferencesRepository.saveTrashQueueIds(emptyList())
        }
        overlayState.update { it.copy(snackbarMessage = "Restored $count items back to deck") }
    }

    fun keepOnlyPhotoInBurst(group: BurstGroup, keepPhoto: PhotoItem) {
        val toTrash = group.photos.filter { it.id != keepPhoto.id }
        val toTrashIds = toTrash.map { it.id }
        var nextTrash: List<Long> = emptyList()

        galleryFilterState.update { current ->
            val mergedTrash = (toTrashIds + current.trashQueueIds).distinct()
            nextTrash = mergedTrash
            current.copy(
                trashQueueIds = mergedTrash,
                sessionKeptIds = current.sessionKeptIds + keepPhoto.id,
            )
        }
        viewModelScope.launch {
            preferencesRepository.markPhotoKept(keepPhoto.id, isFavorite = true)
            preferencesRepository.saveTrashQueueIds(nextTrash)
        }
        overlayState.update {
            it.copy(
                comparingBurstGroup = null,
                snackbarMessage = "Kept ${keepPhoto.displayName} • Queued ${toTrash.size} similar shots",
            )
        }
    }

    fun requestBatchDelete(selectedTargets: List<PhotoItem>? = null) {
        val currentState = uiState.value
        val targets = selectedTargets ?: currentState.trashQueue
        if (targets.isEmpty() || currentState.isDeleting) return

        viewModelScope.launch {
            overlayState.update { it.copy(isDeleting = true, pendingDeletePhotos = targets) }
            when (
                val result = galleryRepository.requestDeletePhotos(
                    uris = targets.map { it.uri },
                    moveToSystemTrash = currentState.useSystemTrash,
                )
            ) {
                is DeleteBatchResult.RequiresConfirmation -> {
                    overlayState.update {
                        it.copy(
                            isDeleting = false,
                            pendingDeleteSender = result.intentSender,
                        )
                    }
                }
                is DeleteBatchResult.Completed -> {
                    finalizeDeletion(targets, movedToTrash = currentState.useSystemTrash)
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
        val movedToTrash = uiState.value.useSystemTrash
        overlayState.update { it.copy(pendingDeleteSender = null) }
        if (confirmed && targets.isNotEmpty()) {
            viewModelScope.launch {
                finalizeDeletion(targets, movedToTrash = movedToTrash)
            }
        } else {
            overlayState.update {
                it.copy(
                    pendingDeletePhotos = emptyList(),
                    snackbarMessage = "Action canceled — items remain in Trash queue",
                )
            }
        }
    }

    private suspend fun finalizeDeletion(targets: List<PhotoItem>, movedToTrash: Boolean = false) {
        val freedBytes = targets.sumOf { it.sizeBytes }
        val deletedIds = targets.map { it.id }.toSet()
        try {
            preferencesRepository.recordBatchDeletion(
                deletedCount = targets.size,
                freedBytes = freedBytes,
                deletedIds = deletedIds,
            )
            val refreshed = galleryRepository.loadDevicePhotos()
            var nextTrash: List<Long> = emptyList()
            galleryFilterState.update { current ->
                val filteredTrash = current.trashQueueIds.filterNot { it in deletedIds }
                nextTrash = filteredTrash
                current.copy(
                    allPhotos = refreshed,
                    trashQueueIds = filteredTrash,
                    undoStack = current.undoStack.filterNot { it.photo.id in deletedIds },
                )
            }
            preferencesRepository.saveTrashQueueIds(nextTrash)
            val actionVerb = if (movedToTrash) "Moved ${targets.size} to System Trash" else "Deleted ${targets.size} items"
            overlayState.update {
                it.copy(
                    snackbarMessage = "$actionVerb • Freed ${formatFileSize(freedBytes)}",
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            overlayState.update {
                it.copy(snackbarMessage = t.localizedMessage ?: "Failed to refresh after deletion")
            }
        } finally {
            overlayState.update {
                it.copy(
                    isDeleting = false,
                    pendingDeletePhotos = emptyList(),
                )
            }
        }
    }

    fun selectAlbum(album: String) {
        galleryFilterState.update { it.copy(selectedAlbum = album, pinnedTopPhotoId = null) }
    }

    fun selectMonth(monthKey: String) {
        galleryFilterState.update { it.copy(selectedMonth = monthKey, pinnedTopPhotoId = null) }
    }

    fun selectSmartCategory(category: SmartCategory) {
        galleryFilterState.update {
            it.copy(
                selectedSmartCategory = category,
                selectedAlbum = ALBUM_ALL,
                selectedMonth = MONTH_ALL,
                pinnedTopPhotoId = null,
            )
        }
    }

    fun jumpToPhotoInDeck(photoId: Long) {
        galleryFilterState.update { it.copy(pinnedTopPhotoId = photoId) }
        overlayState.update { it.copy(showJumpSheet = false) }
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

    fun setUseSystemTrash(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseSystemTrash(enabled)
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoCheckUpdates(enabled)
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

    fun openBurstCompare(group: BurstGroup?) {
        overlayState.update { it.copy(comparingBurstGroup = group) }
    }

    fun setShowJumpSheet(show: Boolean) {
        overlayState.update { it.copy(showJumpSheet = show) }
    }

    fun clearSnackbar() {
        overlayState.update { it.copy(snackbarMessage = null) }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            checkForUpdatesInternal(silent = false)
        }
    }

    fun startInAppUpdate(release: GitHubReleaseInfo) {
        if (updateDownloadJob?.isActive == true) return
        val app = getApplication<Application>()
        updateDownloadJob = viewModelScope.launch {
            overlayState.update {
                it.copy(
                    updateDownloadState = UpdateDownloadState.Downloading(
                        tagName = release.tagName,
                        bytesDownloaded = 0L,
                        totalBytes = release.apkSizeBytes,
                    ),
                )
            }
            when (
                val result = InAppUpdater.downloadApk(
                    context = app,
                    release = release,
                    userAgentVersion = BuildConfig.VERSION_NAME,
                ) { downloaded, total ->
                    overlayState.update {
                        it.copy(
                            updateDownloadState = UpdateDownloadState.Downloading(
                                tagName = release.tagName,
                                bytesDownloaded = downloaded,
                                totalBytes = total,
                            ),
                        )
                    }
                }
            ) {
                is ApkDownloadResult.Success -> {
                    val path = result.apkFile.absolutePath
                    if (InAppUpdater.canRequestPackageInstalls(app)) {
                        overlayState.update {
                            it.copy(
                                updateDownloadState = UpdateDownloadState.ReadyToInstall(release.tagName, path),
                                pendingApkInstallPath = path,
                            )
                        }
                    } else {
                        overlayState.update {
                            it.copy(
                                updateDownloadState = UpdateDownloadState.NeedsInstallPermission(release.tagName, path),
                            )
                        }
                    }
                }
                is ApkDownloadResult.Failure -> {
                    overlayState.update {
                        it.copy(
                            updateDownloadState = UpdateDownloadState.Failed(release.tagName, result.message),
                        )
                    }
                }
            }
        }
    }

    fun cancelInAppUpdate() {
        updateDownloadJob?.cancel()
        updateDownloadJob = null
        overlayState.update {
            it.copy(updateDownloadState = UpdateDownloadState.Idle)
        }
    }

    fun installDownloadedUpdate() {
        val currentDownload = overlayState.value.updateDownloadState
        val (tagName, path) = when (currentDownload) {
            is UpdateDownloadState.ReadyToInstall -> currentDownload.tagName to currentDownload.apkPath
            is UpdateDownloadState.NeedsInstallPermission -> currentDownload.tagName to currentDownload.apkPath
            else -> return
        }
        val app = getApplication<Application>()
        if (InAppUpdater.canRequestPackageInstalls(app)) {
            overlayState.update {
                it.copy(
                    updateDownloadState = UpdateDownloadState.ReadyToInstall(tagName, path),
                    pendingApkInstallPath = path,
                )
            }
        } else {
            overlayState.update {
                it.copy(
                    updateDownloadState = UpdateDownloadState.NeedsInstallPermission(tagName, path),
                )
            }
        }
    }

    fun onInstallPermissionReturned() {
        val currentDownload = overlayState.value.updateDownloadState
        if (currentDownload is UpdateDownloadState.NeedsInstallPermission) {
            val app = getApplication<Application>()
            if (InAppUpdater.canRequestPackageInstalls(app)) {
                overlayState.update {
                    it.copy(
                        updateDownloadState = UpdateDownloadState.ReadyToInstall(
                            tagName = currentDownload.tagName,
                            apkPath = currentDownload.apkPath,
                        ),
                        pendingApkInstallPath = currentDownload.apkPath,
                    )
                }
            }
        }
    }

    fun consumePendingApkInstall() {
        overlayState.update { it.copy(pendingApkInstallPath = null) }
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

    fun dismissUpdateDialog() {
        val currentStatus = uiState.value.updateStatus
        overlayState.update { it.copy(showUpdateDialog = false) }
        if (currentStatus is UpdateUiState.Available) {
            viewModelScope.launch {
                preferencesRepository.setDismissedPopupTag(currentStatus.release.tagName)
            }
        }
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
                val dismissedPopupTag = runCatching {
                    preferencesRepository.preferencesFlow.first().dismissedPopupTag
                }.getOrDefault("")
                val shouldOpenPopup = !silent || dismissedPopupTag != result.release.tagName
                overlayState.update {
                    it.copy(
                        updateStatus = UpdateUiState.Available(
                            release = result.release,
                            bannerDismissed = false,
                        ),
                        showUpdateDialog = shouldOpenPopup,
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
