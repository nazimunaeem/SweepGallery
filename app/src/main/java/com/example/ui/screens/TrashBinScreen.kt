package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.model.PhotoItem
import com.example.model.formatFileSize
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.TrashCrimson
import com.example.ui.util.rememberSwipeHaptics
import com.example.ui.viewmodel.TindPhotoUiState

@Composable
fun TrashBinScreen(
    state: TindPhotoUiState,
    onRestorePhoto: (PhotoItem) -> Unit,
    onRestoreAll: () -> Unit,
    onInspectPhoto: (PhotoItem) -> Unit,
    onDeleteAllConfirmed: () -> Unit,
    onGoToSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    onRestoreSelected: (Set<Long>) -> Unit = {},
    onDeleteSelected: (List<PhotoItem>) -> Unit = {},
) {
    val swipeHaptics = rememberSwipeHaptics()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(state.trashQueue) {
        if (state.trashQueue.isEmpty()) {
            selectionMode = false
            selectedIds = emptySet()
        } else if (selectedIds.isNotEmpty()) {
            val validIds = state.trashQueue.mapTo(HashSet()) { it.id }
            selectedIds = selectedIds.filterTo(HashSet()) { it in validIds }
        }
    }

    val useSystemTrashUi = state.useSystemTrash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val deleteVerb = if (useSystemTrashUi) {
        stringResource(R.string.move_to_system_trash)
    } else {
        stringResource(R.string.delete_permanently)
    }

    val selectedPhotos = remember(state.trashQueue, selectedIds) {
        state.trashQueue.filter { it.id in selectedIds }
    }
    val selectedBytes = remember(selectedPhotos) { selectedPhotos.sumOf { it.sizeBytes } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trash Queue",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = when {
                        state.trashQueue.isEmpty() -> "No media waiting for deletion"
                        selectionMode -> "${selectedPhotos.size} selected • ${formatFileSize(selectedBytes)}"
                        else -> "${state.trashQueue.size} items • ${formatFileSize(state.trashTotalBytes)} to free"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.trashQueue.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    TextButton(
                        onClick = {
                            if (selectionMode) {
                                selectionMode = false
                                selectedIds = emptySet()
                            } else {
                                selectionMode = true
                            }
                        },
                        modifier = Modifier.testTag("trash_select_toggle"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (selectionMode) "Done" else "Select")
                    }

                    if (!selectionMode) {
                        TextButton(
                            onClick = onRestoreAll,
                            modifier = Modifier.testTag("restore_all_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestoreFromTrash,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.restore_all))
                        }
                    } else {
                        val allSelected = selectedIds.size == state.trashQueue.size
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    state.trashQueue.mapTo(HashSet()) { it.id }
                                }
                            },
                            modifier = Modifier.testTag("trash_select_all_button"),
                        ) {
                            Text(if (allSelected) "None" else "All")
                        }
                    }
                }
            }
        }

        if (state.trashQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 420.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(KeepEmerald.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = KeepEmerald,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.trash_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.trash_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onGoToSwipe,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("trash_empty_go_swipe_button"),
                        ) {
                            Text("Back to Swiping")
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.trashQueue, key = { it.id }) { photo ->
                    val isSelected = photo.id in selectedIds
                    TrashGridTile(
                        photo = photo,
                        selectionMode = selectionMode,
                        isSelected = isSelected,
                        onTap = {
                            if (selectionMode) {
                                selectedIds = if (isSelected) {
                                    selectedIds - photo.id
                                } else {
                                    selectedIds + photo.id
                                }
                            } else {
                                onInspectPhoto(photo)
                            }
                        },
                        onLongPress = {
                            selectionMode = true
                            selectedIds = selectedIds + photo.id
                        },
                        onRestore = { onRestorePhoto(photo) },
                    )
                }
            }

            // Bottom Batch Action Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, TrashCrimson.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
            ) {
                if (selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                onRestoreSelected(selectedIds)
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedPhotos.isNotEmpty() && !state.isDeleting,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("restore_selected_button"),
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestoreFromTrash,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Restore (${selectedPhotos.size})")
                        }

                        Button(
                            onClick = {
                                swipeHaptics.onDeleteCommitted()
                                onDeleteSelected(selectedPhotos)
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedPhotos.isNotEmpty() && !state.isDeleting,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("delete_selected_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrashCrimson,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = if (useSystemTrashUi) {
                                    Icons.Rounded.DeleteOutline
                                } else {
                                    Icons.Rounded.DeleteForever
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (useSystemTrashUi) {
                                    "Trash (${selectedPhotos.size})"
                                } else {
                                    "Delete (${selectedPhotos.size})"
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            swipeHaptics.onDeleteCommitted()
                            onDeleteAllConfirmed()
                        },
                        enabled = !state.isDeleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .height(54.dp)
                            .testTag("confirm_batch_delete_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrashCrimson,
                            contentColor = Color.White,
                        ),
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Icon(
                                imageVector = if (useSystemTrashUi) {
                                    Icons.Rounded.DeleteOutline
                                } else {
                                    Icons.Rounded.DeleteForever
                                },
                                contentDescription = null,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$deleteVerb (${formatFileSize(state.trashTotalBytes)})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashGridTile(
    photo: PhotoItem,
    selectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRestore: () -> Unit,
) {
    val context = LocalContext.current
    val thumbnailRequest = remember(photo.id, context) {
        ImageRequest.Builder(context)
            .data(photo.uri)
            .size(420, 420)
            .precision(Precision.INEXACT)
            .memoryCacheKey("thumb_${photo.id}")
            .crossfade(false)
            .build()
    }
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, TrashCrimson, MaterialTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            )
            .testTag("trash_tile_${photo.id}"),
    ) {
        AsyncImage(
            model = thumbnailRequest,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                ),
        )

        Text(
            text = photo.formattedSize,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )

        if (photo.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = photo.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }

        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) TrashCrimson else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
            )
        } else {
            IconButton(
                onClick = onRestore,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("restore_tile_${photo.id}"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.cd_restore_photo),
                    tint = KeepEmerald,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
