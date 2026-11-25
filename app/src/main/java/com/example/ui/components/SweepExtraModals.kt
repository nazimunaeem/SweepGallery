package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.model.BurstGroup
import com.example.model.PhotoItem
import com.example.model.formatFileSize
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.SweepLogoAccentDark
import com.example.ui.theme.SweepLogoStart

fun shareMediaItem(context: Context, photo: PhotoItem) {
    val mime = photo.mimeType.ifBlank { if (photo.isVideo) "video/*" else "image/*" }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, photo.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, photo.displayName).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val launched = runCatching {
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
    if (!launched) {
        Toast.makeText(context, "No app available to share this item", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurstCompareSheet(
    burstGroup: BurstGroup?,
    onDismiss: () -> Unit,
    onKeepBestAndTrashRest: (BurstGroup, PhotoItem) -> Unit,
    onOpenLightbox: (PhotoItem) -> Unit,
) {
    if (burstGroup == null || burstGroup.photos.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val recommendedId = remember(burstGroup.id) {
        burstGroup.photos.maxWithOrNull(
            compareBy<PhotoItem> { it.width.toLong() * it.height.toLong() }.thenBy { it.sizeBytes },
        )?.id ?: burstGroup.photos.first().id
    }
    var selectedKeepId by remember(burstGroup.id) { mutableLongStateOf(recommendedId) }
    val selectedPhoto = burstGroup.photos.firstOrNull { it.id == selectedKeepId } ?: burstGroup.photos.first()
    val trashCount = (burstGroup.photos.size - 1).coerceAtLeast(0)
    val freedEstimate = (burstGroup.totalBytes - selectedPhoto.sizeBytes).coerceAtLeast(0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = SweepLogoAccentDark,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Compare ${burstGroup.photos.size} Similar Shots",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap the sharpest photo to keep; the remaining $trashCount will move to Trash.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(burstGroup.photos, key = { it.id }) { photo ->
                    val isSelected = photo.id == selectedKeepId
                    val isRecommended = photo.id == recommendedId
                    val request = remember(photo.id, context) {
                        ImageRequest.Builder(context)
                            .data(photo.uri)
                            .size(560, 720)
                            .precision(Precision.INEXACT)
                            .build()
                    }
                    Surface(
                        onClick = { selectedKeepId = photo.id },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) KeepEmerald else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .width(176.dp)
                            .testTag("burst_item_${photo.id}"),
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.78f)
                                    .background(Color.Black),
                            ) {
                                AsyncImage(
                                    model = request,
                                    contentDescription = photo.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (isRecommended) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SweepLogoStart.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                    ) {
                                        Text(
                                            text = "Sharpest",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected to keep",
                                        tint = KeepEmerald,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                    )
                                }
                                Surface(
                                    onClick = { onOpenLightbox(photo) },
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.62f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                ) {
                                    Text(
                                        text = "Zoom",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = photo.formattedSize,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) KeepEmerald else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = photo.resolutionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onKeepBestAndTrashRest(burstGroup, selectedPhoto) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
                    .testTag("burst_keep_selected_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepEmerald,
                    contentColor = Color(0xFF042619),
                ),
            ) {
                Text(
                    text = "Keep Selected • Trash $trashCount (${formatFileSize(freedEstimate)})",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckJumpSheet(
    visible: Boolean,
    deckPhotos: List<PhotoItem>,
    favoriteIds: Set<Long>,
    onDismiss: () -> Unit,
    onSelectPhoto: (PhotoItem) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = "Jump in Deck (${deckPhotos.size})",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Tap any item to bring it straight to the top card",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(deckPhotos.take(120), key = { it.id }) { photo ->
                    val thumbReq = remember(photo.id, context) {
                        ImageRequest.Builder(context)
                            .data(photo.uri)
                            .size(300, 300)
                            .precision(Precision.INEXACT)
                            .build()
                    }
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSelectPhoto(photo) }
                            .testTag("jump_item_${photo.id}"),
                    ) {
                        AsyncImage(
                            model = thumbReq,
                            contentDescription = photo.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                                    ),
                                ),
                        )
                        Text(
                            text = photo.formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                        )
                        if (photo.isVideo) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleFilled,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(28.dp),
                            )
                        }
                        if (photo.id in favoriteIds) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Starred",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
