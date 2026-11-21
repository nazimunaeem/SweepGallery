package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.model.MonthSummary
import com.example.model.PhotoItem
import com.example.model.SortOrder
import com.example.model.SwipeDirection
import com.example.model.formatFileSize
import com.example.ui.components.TindPhotoArtwork
import com.example.ui.components.TindPhotoLogo
import com.example.ui.theme.FlameCoral
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.TrashCrimson
import com.example.ui.theme.UndoAmber
import com.example.ui.util.SwipeHaptics
import com.example.ui.util.rememberSwipeHaptics
import com.example.ui.viewmodel.ALBUM_ALL
import com.example.ui.viewmodel.MONTH_ALL
import com.example.ui.viewmodel.TindPhotoUiState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DECK_IMAGE_MAX_DIM = 1080

private data class ExitingCardState(
    val photo: PhotoItem,
    val direction: SwipeDirection,
    val startX: Float,
    val startY: Float,
)

@Composable
fun SwipeDeckScreen(
    state: TindPhotoUiState,
    onSwipe: (PhotoItem, SwipeDirection) -> Unit,
    onUndo: () -> Unit,
    onInspectPhoto: (PhotoItem) -> Unit,
    onOpenLightbox: (PhotoItem) -> Unit,
    onSelectAlbum: (String) -> Unit,
    onSelectMonth: (String) -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onSeedSamples: () -> Unit,
    onIncludeReviewed: () -> Unit,
    onOpenTrashTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val swipeHaptics = rememberSwipeHaptics()
    val dragProgressState = remember { mutableFloatStateOf(0f) }

    var exitingCards by remember { mutableStateOf<List<ExitingCardState>>(emptyList()) }
    var pendingRemovalIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val activeDeckPhotos = remember(state.deckPhotos, pendingRemovalIds) {
        if (pendingRemovalIds.isEmpty()) {
            state.deckPhotos
        } else {
            state.deckPhotos.filter { it.id !in pendingRemovalIds }
        }
    }

    val topPhoto = activeDeckPhotos.firstOrNull()
    val upcomingPhotos = remember(activeDeckPhotos) { activeDeckPhotos.take(6) }

    LaunchedEffect(state.deckPhotos) {
        if (pendingRemovalIds.isNotEmpty()) {
            val presentIds = state.deckPhotos.mapTo(HashSet(state.deckPhotos.size)) { it.id }
            val stillPresent = pendingRemovalIds.filterTo(HashSet()) { it in presentIds }
            if (stillPresent.size != pendingRemovalIds.size) {
                pendingRemovalIds = stillPresent
            }
        }
    }

    LaunchedEffect(topPhoto?.id) {
        dragProgressState.floatValue = 0f
    }

    LaunchedEffect(upcomingPhotos) {
        val loader = context.imageLoader
        upcomingPhotos.forEach { photo ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(photo.uri)
                    .size(DECK_IMAGE_MAX_DIM, DECK_IMAGE_MAX_DIM)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey("deck_${photo.id}")
                    .build(),
            )
        }
    }

    val commitSwipe: (PhotoItem, SwipeDirection, Float, Float) -> Unit = { photo, direction, startX, startY ->
        dragProgressState.floatValue = 0f
        pendingRemovalIds = pendingRemovalIds + photo.id
        exitingCards = (exitingCards.filterNot { it.photo.id == photo.id } +
            ExitingCardState(photo = photo, direction = direction, startX = startX, startY = startY))
            .takeLast(3)
        onSwipe(photo, direction)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Header Bar
        SwipeTopBar(
            state = state,
            onSelectSortOrder = onSelectSortOrder,
            onRefresh = onRefresh,
            onOpenTrashTab = onOpenTrashTab,
        )

        // Month-wise & Album Filter Strip
        if (state.months.isNotEmpty() || state.albums.size > 1) {
            FilterStrip(
                state = state,
                onSelectAlbum = onSelectAlbum,
                onSelectMonth = onSelectMonth,
            )
        }

        // Main Card Stack Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = FlameCoral)
                        Text(
                            text = "Scanning your photos…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                activeDeckPhotos.isEmpty() && exitingCards.isEmpty() -> {
                    val activeMonth = state.months.firstOrNull { it.key == state.selectedMonth }
                    EmptyDeckCard(
                        hasAnyPhotosOnDevice = state.allPhotosCount > 0,
                        isAllAlbum = state.selectedAlbum == ALBUM_ALL,
                        isSpecificMonth = state.selectedMonth != MONTH_ALL,
                        selectedMonthLabel = activeMonth?.label,
                        isSelectedMonthCompleted = activeMonth?.isCompleted == true,
                        nextUnfinishedMonth = state.nextUnfinishedMonth,
                        trashCount = state.trashQueue.size,
                        trashSizeLabel = formatFileSize(state.trashTotalBytes),
                        isSeeding = state.isSeeding,
                        onSeedSamples = onSeedSamples,
                        onIncludeReviewed = onIncludeReviewed,
                        onShowAllAlbums = { onSelectAlbum(ALBUM_ALL) },
                        onShowAllMonths = { onSelectMonth(MONTH_ALL) },
                        onSelectNextMonth = { month -> onSelectMonth(month.key) },
                        onOpenTrashTab = onOpenTrashTab,
                    )
                }
                else -> {
                    val visibleCards = remember(activeDeckPhotos) { activeDeckPhotos.take(3) }
                    visibleCards.asReversed().forEach { photo ->
                        val stackIndex = visibleCards.indexOf(photo)
                        key(photo.id) {
                            SwipeablePhotoCard(
                                photo = photo,
                                stackIndex = stackIndex,
                                swipeHaptics = swipeHaptics,
                                dragProgressProvider = { dragProgressState.floatValue },
                                onDragProgress = { signedProgress ->
                                    if (stackIndex == 0) dragProgressState.floatValue = signedProgress
                                },
                                onCommitSwipe = { direction, startX, startY ->
                                    commitSwipe(photo, direction, startX, startY)
                                },
                                onTapZoom = { onOpenLightbox(photo) },
                                onTapInfo = { onInspectPhoto(photo) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // Non-blocking fly-out overlay so the promoted next card is interactive immediately
            exitingCards.forEach { exiting ->
                key("exit_${exiting.photo.id}") {
                    ExitingPhotoCard(
                        exiting = exiting,
                        onFinished = {
                            exitingCards = exitingCards.filterNot { it.photo.id == exiting.photo.id }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Bottom Simple & Smart Action Dock
        SwipeActionDock(
            hasActiveCard = topPhoto != null && !state.isLoading,
            canUndo = state.canUndo,
            dragProgressProvider = { dragProgressState.floatValue },
            onUndo = {
                exitingCards = emptyList()
                pendingRemovalIds = emptySet()
                dragProgressState.floatValue = 0f
                onUndo()
            },
            onTrashClick = {
                topPhoto?.let { photo ->
                    swipeHaptics.onDeleteCommitted()
                    commitSwipe(photo, SwipeDirection.LEFT_TRASH, 0f, 0f)
                }
            },
            onKeepClick = {
                topPhoto?.let { photo ->
                    swipeHaptics.onKeepCommitted()
                    commitSwipe(photo, SwipeDirection.RIGHT_KEEP, 0f, 0f)
                }
            },
        )
    }
}

@Composable
private fun FilterStrip(
    state: TindPhotoUiState,
    onSelectAlbum: (String) -> Unit,
    onSelectMonth: (String) -> Unit,
) {
    var albumMenuExpanded by remember { mutableStateOf(false) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.albums.size > 1) {
            item(key = "album_picker") {
                Box {
                    val albumActive = state.selectedAlbum != ALBUM_ALL
                    FilterChip(
                        selected = albumActive,
                        onClick = { albumMenuExpanded = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Filter by album",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            Text(
                                text = state.selectedAlbum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlameCoral.copy(alpha = 0.2f),
                            selectedLabelColor = FlameCoral,
                            selectedLeadingIconColor = FlameCoral,
                        ),
                        modifier = Modifier.testTag("album_dropdown_chip"),
                    )

                    DropdownMenu(
                        expanded = albumMenuExpanded,
                        onDismissRequest = { albumMenuExpanded = false },
                    ) {
                        state.albums.forEach { album ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${album.name} (${album.count})",
                                        fontWeight = if (album.name == state.selectedAlbum) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    )
                                },
                                trailingIcon = if (album.isCompleted) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Album completed",
                                            tint = KeepEmerald,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    albumMenuExpanded = false
                                    onSelectAlbum(album.name)
                                },
                            )
                        }
                    }
                }
            }
        }

        items(state.months, key = { it.key }) { month ->
            val selected = month.key == state.selectedMonth
            val isCompleted = month.isCompleted

            FilterChip(
                selected = selected,
                onClick = { onSelectMonth(month.key) },
                leadingIcon = {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Month completed",
                            tint = KeepEmerald,
                            modifier = Modifier.size(17.dp),
                        )
                    } else if (month.key != MONTH_ALL) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                label = {
                    val suffix = when {
                        isCompleted -> " (${month.totalCount})"
                        month.key == MONTH_ALL -> " (${month.remainingCount})"
                        else -> " (${month.remainingCount}/${month.totalCount})"
                    }
                    Text(
                        text = "${month.label}$suffix",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isCompleted && !selected) {
                        KeepEmerald.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    labelColor = if (isCompleted && !selected) {
                        KeepEmerald
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    selectedContainerColor = if (isCompleted) {
                        KeepEmerald.copy(alpha = 0.22f)
                    } else {
                        FlameCoral.copy(alpha = 0.2f)
                    },
                    selectedLabelColor = if (isCompleted) KeepEmerald else FlameCoral,
                    selectedLeadingIconColor = if (isCompleted) KeepEmerald else FlameCoral,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = if (isCompleted) {
                        KeepEmerald.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    selectedBorderColor = if (isCompleted) {
                        KeepEmerald.copy(alpha = 0.75f)
                    } else {
                        FlameCoral.copy(alpha = 0.65f)
                    },
                ),
                modifier = Modifier.testTag("month_chip_${month.key}"),
            )
        }
    }
}

@Composable
private fun SwipeTopBar(
    state: TindPhotoUiState,
    onSelectSortOrder: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onOpenTrashTab: () -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TindPhotoLogo()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.trashQueue.isNotEmpty()) {
                Surface(
                    onClick = onOpenTrashTab,
                    shape = CircleShape,
                    color = TrashCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, TrashCrimson.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("top_trash_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = TrashCrimson,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${state.trashQueue.size} • ${formatFileSize(state.trashTotalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TrashCrimson,
                        )
                    }
                }
            }

            Box {
                Surface(
                    onClick = { sortMenuExpanded = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.testTag("sort_order_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = "Sort photos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = state.sortOrder.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (order) {
                                        SortOrder.NEWEST -> "Newest first"
                                        SortOrder.OLDEST -> "Oldest first"
                                        SortOrder.LARGEST -> "Largest files first"
                                    },
                                    fontWeight = if (order == state.sortOrder) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                sortMenuExpanded = false
                                onSelectSortOrder(order)
                            },
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("refresh_gallery_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh gallery",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SwipeablePhotoCard(
    photo: PhotoItem,
    stackIndex: Int,
    swipeHaptics: SwipeHaptics,
    dragProgressProvider: () -> Float,
    onDragProgress: (Float) -> Unit,
    onCommitSwipe: (SwipeDirection, Float, Float) -> Unit,
    onTapZoom: () -> Unit,
    onTapInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTop = stackIndex == 0
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    val promotionAnim = remember(photo.id) { Animatable(if (stackIndex == 0) 1f else 0f) }
    val velocityTracker = remember(photo.id) { VelocityTracker() }
    val cardInteractionSource = remember(photo.id) { MutableInteractionSource() }

    val thresholdPx = remember(density) { with(density) { 72.dp.toPx() } }
    val flickVelocityPx = remember(density) { with(density) { 500.dp.toPx() } }
    val minFlickDistancePx = remember(density) { with(density) { 18.dp.toPx() } }
    val promotionOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }

    LaunchedEffect(isTop) {
        if (isTop && promotionAnim.value < 1f) {
            promotionAnim.animateTo(
                1f,
                animationSpec = tween(durationMillis = 105, easing = FastOutSlowInEasing),
            )
        }
    }

    val behindScaleBase = when (stackIndex) {
        0 -> 1f
        1 -> 0.94f
        else -> 0.89f
    }
    val behindScaleDelta = when (stackIndex) {
        0 -> 0f
        1 -> 0.06f
        else -> 0.05f
    }
    val behindTransBasePx = remember(density, stackIndex) {
        with(density) {
            when (stackIndex) {
                0 -> 0f
                1 -> 14.dp.toPx()
                else -> 26.dp.toPx()
            }
        }
    }
    val behindTransDeltaPx = remember(density, stackIndex) {
        with(density) {
            when (stackIndex) {
                0 -> 0f
                1 -> 14.dp.toPx()
                else -> 12.dp.toPx()
            }
        }
    }

    val cacheKey = "deck_${photo.id}"
    val imageRequest = remember(photo.id, context) {
        ImageRequest.Builder(context)
            .data(photo.uri)
            .size(DECK_IMAGE_MAX_DIM, DECK_IMAGE_MAX_DIM)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .crossfade(false)
            .build()
    }

    val dragModifier = if (isTop) {
        Modifier.pointerInput(photo.id) {
            var crossedThreshold = false
            detectDragGestures(
                onDragStart = {
                    crossedThreshold = abs(offsetX.value) >= thresholdPx
                    velocityTracker.resetTracking()
                },
                onDragEnd = {
                    val currentX = offsetX.value
                    val currentY = offsetY.value
                    val velocityX = velocityTracker.calculateVelocity().x
                    val shouldKeep = currentX > thresholdPx ||
                        (velocityX > flickVelocityPx && currentX > minFlickDistancePx)
                    val shouldTrash = currentX < -thresholdPx ||
                        (velocityX < -flickVelocityPx && currentX < -minFlickDistancePx)

                    when {
                        shouldKeep -> {
                            swipeHaptics.onKeepCommitted()
                            onCommitSwipe(SwipeDirection.RIGHT_KEEP, currentX, currentY)
                        }
                        shouldTrash -> {
                            swipeHaptics.onDeleteCommitted()
                            onCommitSwipe(SwipeDirection.LEFT_TRASH, currentX, currentY)
                        }
                        else -> {
                            crossedThreshold = false
                            onDragProgress(0f)
                            scope.launch {
                                launch {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                                launch {
                                    offsetY.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
                onDragCancel = {
                    crossedThreshold = false
                    onDragProgress(0f)
                    scope.launch {
                        launch {
                            offsetX.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                        launch {
                            offsetY.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val nextX = offsetX.value + dragAmount.x
                    val nextY = (offsetY.value + dragAmount.y * 0.32f).coerceIn(-120f, 120f)
                    velocityTracker.addPosition(change.uptimeMillis, Offset(nextX, nextY))
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        offsetX.snapTo(nextX)
                        offsetY.snapTo(nextY)
                    }
                    val signedProgress = (nextX / thresholdPx).coerceIn(-1f, 1f)
                    onDragProgress(signedProgress)

                    val nowCrossed = abs(nextX) >= thresholdPx
                    if (nowCrossed && !crossedThreshold) {
                        crossedThreshold = true
                        swipeHaptics.onThresholdCrossed(isDeleteDirection = nextX < 0f)
                    } else if (!nowCrossed && crossedThreshold) {
                        crossedThreshold = false
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                if (isTop) {
                    val x = offsetX.value
                    val settle = promotionAnim.value
                    val baseScale = 0.95f + (0.05f * settle)
                    scaleX = baseScale
                    scaleY = baseScale
                    translationX = x
                    translationY = offsetY.value + ((1f - settle) * promotionOffsetPx)
                    rotationZ = (x / 24f).coerceIn(-14f, 14f)
                } else {
                    val p = abs(dragProgressProvider()).coerceIn(0f, 1f)
                    val s = behindScaleBase + (behindScaleDelta * p)
                    scaleX = s
                    scaleY = s
                    translationY = behindTransBasePx - (behindTransDeltaPx * p)
                    alpha = if (stackIndex == 1) 0.92f + (0.08f * p) else 0.78f + (0.14f * p)
                }
            }
            .then(dragModifier)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (isTop) {
                    Modifier.clickable(
                        interactionSource = cardInteractionSource,
                        indication = null,
                        onClick = onTapZoom,
                    )
                } else {
                    Modifier
                },
            ),
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF0A0A0F),
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F)),
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = photo.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom dark scrim for crisp readable metadata
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(128.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )

            if (isTop) {
                // Real-time KEEP directional tint wash & border glow (right drag)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = ((offsetX.value - 6f) / (thresholdPx - 6f)).coerceIn(0f, 1f)
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    KeepEmerald.copy(alpha = 0.06f),
                                    KeepEmerald.copy(alpha = 0.34f),
                                ),
                            ),
                        )
                        .border(2.5.dp, KeepEmerald, MaterialTheme.shapes.large),
                )

                // Real-time DELETE directional tint wash & border glow (left drag)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = ((-offsetX.value - 6f) / (thresholdPx - 6f)).coerceIn(0f, 1f)
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TrashCrimson.copy(alpha = 0.36f),
                                    TrashCrimson.copy(alpha = 0.06f),
                                ),
                            ),
                        )
                        .border(2.5.dp, TrashCrimson, MaterialTheme.shapes.large),
                )

                // Top-right Zoom pill hint (fades out while dragging so DELETE overlay stays clear)
                Surface(
                    onClick = onTapZoom,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.52f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .graphicsLayer {
                            alpha = 1f - (abs(offsetX.value) / (thresholdPx * 0.45f)).coerceIn(0f, 1f)
                        },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = stringResource(R.string.cd_zoom),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                    )
                }

                // Fading KEEP Overlay Stamp (top-left) - driven solely in graphicsLayer
                SwipeStampBadge(
                    label = stringResource(R.string.stamp_keep),
                    icon = Icons.Rounded.Favorite,
                    color = KeepEmerald,
                    rotationDegrees = -14f,
                    alphaProvider = { ((offsetX.value - 6f) / (thresholdPx - 6f)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, top = 32.dp),
                )

                // Fading DELETE Overlay Stamp (top-right) - driven solely in graphicsLayer
                SwipeStampBadge(
                    label = stringResource(R.string.stamp_trash),
                    icon = Icons.Rounded.DeleteOutline,
                    color = TrashCrimson,
                    rotationDegrees = 14f,
                    alphaProvider = { ((-offsetX.value - 6f) / (thresholdPx - 6f)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp, top = 32.dp),
                )
            }

            // Bottom Photo Info Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = photo.formattedDate,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = photo.albumName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = FlameCoral.copy(alpha = 0.85f),
                        ) {
                            Text(
                                text = photo.formattedSize,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                if (isTop) {
                    IconButton(
                        onClick = onTapInfo,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.cd_inspect),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExitingPhotoCard(
    exiting: ExitingCardState,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val flyOutDistance = remember(density) { with(density) { 460.dp.toPx() } }
    val isRight = exiting.direction == SwipeDirection.RIGHT_KEEP
    val targetX = if (isRight) flyOutDistance else -flyOutDistance

    val animX = remember(exiting.photo.id) { Animatable(exiting.startX) }
    val progressAnim = remember(exiting.photo.id) { Animatable(0f) }

    val cacheKey = "deck_${exiting.photo.id}"
    val imageRequest = remember(exiting.photo.id, context) {
        ImageRequest.Builder(context)
            .data(exiting.photo.uri)
            .size(DECK_IMAGE_MAX_DIM, DECK_IMAGE_MAX_DIM)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .crossfade(false)
            .build()
    }

    LaunchedEffect(exiting.photo.id) {
        val flyOutJob = launch {
            animX.animateTo(
                targetX,
                animationSpec = tween(durationMillis = 155, easing = FastOutLinearInEasing),
            )
        }
        val fadeJob = launch {
            progressAnim.animateTo(
                1f,
                animationSpec = tween(durationMillis = 155, easing = FastOutLinearInEasing),
            )
        }
        flyOutJob.join()
        fadeJob.join()
        onFinished()
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                val x = animX.value
                translationX = x
                translationY = exiting.startY + (progressAnim.value * 28f)
                rotationZ = (x / 24f).coerceIn(-16f, 16f)
                alpha = (1f - (progressAnim.value * 0.45f)).coerceIn(0f, 1f)
            }
            .clip(MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF0A0A0F),
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F)),
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isRight) {
                            Brush.horizontalGradient(
                                listOf(KeepEmerald.copy(alpha = 0.08f), KeepEmerald.copy(alpha = 0.34f)),
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(TrashCrimson.copy(alpha = 0.36f), TrashCrimson.copy(alpha = 0.08f)),
                            )
                        },
                    )
                    .border(
                        width = 2.5.dp,
                        color = if (isRight) KeepEmerald else TrashCrimson,
                        shape = MaterialTheme.shapes.large,
                    ),
            )

            SwipeStampBadge(
                label = stringResource(if (isRight) R.string.stamp_keep else R.string.stamp_trash),
                icon = if (isRight) Icons.Rounded.Favorite else Icons.Rounded.DeleteOutline,
                color = if (isRight) KeepEmerald else TrashCrimson,
                rotationDegrees = if (isRight) -14f else 14f,
                alphaProvider = { 1f },
                modifier = Modifier
                    .align(if (isRight) Alignment.TopStart else Alignment.TopEnd)
                    .padding(
                        start = if (isRight) 24.dp else 0.dp,
                        end = if (isRight) 0.dp else 24.dp,
                        top = 32.dp,
                    ),
            )
        }
    }
}

@Composable
private fun SwipeStampBadge(
    label: String,
    icon: ImageVector,
    color: Color,
    rotationDegrees: Float,
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .graphicsLayer {
                val a = alphaProvider()
                val s = 0.86f + (0.16f * a)
                alpha = a
                scaleX = s
                scaleY = s
                rotationZ = rotationDegrees
            }
            .background(Color.Black.copy(alpha = 0.56f), RoundedCornerShape(12.dp))
            .border(width = 3.dp, color = color, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun SwipeActionDock(
    hasActiveCard: Boolean,
    canUndo: Boolean,
    dragProgressProvider: () -> Float,
    onUndo: () -> Unit,
    onTrashClick: () -> Unit,
    onKeepClick: () -> Unit,
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Simple & Smart X (Delete) Button
        Surface(
            onClick = onTrashClick,
            enabled = hasActiveCard,
            shape = CircleShape,
            color = if (hasActiveCard) TrashCrimson.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer {
                    val boost = if (hasActiveCard) (-dragProgressProvider()).coerceIn(0f, 1f) else 0f
                    val scale = 1f + (0.15f * boost)
                    scaleX = scale
                    scaleY = scale
                }
                .testTag("trash_action_button"),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_trash),
                    tint = if (hasActiveCard) TrashCrimson else disabledTint,
                    modifier = Modifier.size(30.dp),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (hasActiveCard) (-dragProgressProvider()).coerceIn(0f, 1f) else 0f
                        }
                        .background(TrashCrimson),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }

        // Centered Undo Button
        Surface(
            onClick = onUndo,
            enabled = canUndo,
            shape = CircleShape,
            color = if (canUndo) {
                UndoAmber.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            modifier = Modifier
                .size(48.dp)
                .testTag("undo_swipe_button"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.cd_undo),
                    tint = if (canUndo) UndoAmber else disabledTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Simple & Smart Heart (Keep) Button
        Surface(
            onClick = onKeepClick,
            enabled = hasActiveCard,
            shape = CircleShape,
            color = if (hasActiveCard) KeepEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer {
                    val boost = if (hasActiveCard) dragProgressProvider().coerceIn(0f, 1f) else 0f
                    val scale = 1f + (0.15f * boost)
                    scaleX = scale
                    scaleY = scale
                }
                .testTag("keep_action_button"),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = stringResource(R.string.cd_keep),
                    tint = if (hasActiveCard) KeepEmerald else disabledTint,
                    modifier = Modifier.size(28.dp),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (hasActiveCard) dragProgressProvider().coerceIn(0f, 1f) else 0f
                        }
                        .background(KeepEmerald),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF042619),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDeckCard(
    hasAnyPhotosOnDevice: Boolean,
    isAllAlbum: Boolean,
    isSpecificMonth: Boolean,
    selectedMonthLabel: String?,
    isSelectedMonthCompleted: Boolean,
    nextUnfinishedMonth: MonthSummary?,
    trashCount: Int,
    trashSizeLabel: String,
    isSeeding: Boolean,
    onSeedSamples: () -> Unit,
    onIncludeReviewed: () -> Unit,
    onShowAllAlbums: () -> Unit,
    onShowAllMonths: () -> Unit,
    onSelectNextMonth: (MonthSummary) -> Unit,
    onOpenTrashTab: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isSpecificMonth && isSelectedMonthCompleted) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(KeepEmerald.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Month completed",
                        tint = KeepEmerald,
                        modifier = Modifier.size(48.dp),
                    )
                }
            } else {
                TindPhotoArtwork()
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    !hasAnyPhotosOnDevice -> stringResource(R.string.empty_gallery_title)
                    isSpecificMonth && selectedMonthLabel != null -> "$selectedMonthLabel Done"
                    else -> stringResource(R.string.empty_deck_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    !hasAnyPhotosOnDevice -> stringResource(R.string.empty_gallery_subtitle)
                    isSpecificMonth && selectedMonthLabel != null ->
                        "All photos from $selectedMonthLabel have been reviewed."
                    else -> stringResource(R.string.empty_deck_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (trashCount > 0) {
                Button(
                    onClick = onOpenTrashTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_state_open_trash_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrashCrimson,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Review $trashCount Queued ($trashSizeLabel)")
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!hasAnyPhotosOnDevice) {
                Button(
                    onClick = onSeedSamples,
                    enabled = !isSeeding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("seed_sample_photos_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlameCoral,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isSeeding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_sample_photos))
                }
            } else {
                if (nextUnfinishedMonth != null) {
                    Button(
                        onClick = { onSelectNextMonth(nextUnfinishedMonth) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("next_unfinished_month_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlameCoral,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Next: ${nextUnfinishedMonth.label} (${nextUnfinishedMonth.remainingCount})")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (isSpecificMonth) {
                    OutlinedButton(
                        onClick = onShowAllMonths,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("show_all_months_button"),
                        shape = CircleShape,
                    ) {
                        Text("Show All Months")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (!isAllAlbum) {
                    OutlinedButton(
                        onClick = onShowAllAlbums,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("show_all_albums_button"),
                        shape = CircleShape,
                    ) {
                        Text(stringResource(R.string.switch_to_all_photos))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = onIncludeReviewed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("review_kept_again_button"),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.review_kept_again))
                }
            }
        }
    }
}
