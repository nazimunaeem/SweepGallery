package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.PhotoItem
import com.example.ui.theme.FlameCoral
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.SweepLogoAccentDark
import com.example.ui.theme.SweepLogoAccentLight
import com.example.ui.theme.SweepLogoEnd
import com.example.ui.theme.SweepLogoStart
import com.example.ui.theme.TrashCrimson
import com.example.ui.util.rememberSwipeHaptics

@Composable
fun TindPhotoLogo(modifier: Modifier = Modifier) {
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val wordmarkAccent = if (isDarkSurface) SweepLogoAccentDark else SweepLogoAccentLight
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SweepLogoStart, SweepLogoEnd),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Sweep",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = wordmarkAccent,
            )
        }
    }
}

@Composable
fun TindPhotoArtwork(
    modifier: Modifier = Modifier,
    primaryColor: Color = SweepLogoStart,
    secondaryColor: Color = KeepEmerald,
    cardSurface: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Canvas(modifier = modifier.size(180.dp)) {
        val cardW = size.width * 0.56f
        val cardH = size.height * 0.72f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.5f,
            ),
            radius = size.minDimension * 0.5f,
            center = center,
        )

        // Left tilted card (Delete vibe)
        rotate(degrees = -12f, pivot = center) {
            val leftTop = Offset(center.x - cardW * 0.62f, center.y - cardH * 0.45f)
            drawRoundRect(
                color = cardSurface,
                topLeft = leftTop,
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(24f, 24f),
            )
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.55f),
                topLeft = leftTop,
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(24f, 24f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
            )
        }

        // Right tilted card (Keep vibe)
        rotate(degrees = 10f, pivot = center) {
            val rightTop = Offset(center.x - cardW * 0.36f, center.y - cardH * 0.48f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.9f),
                        SweepLogoEnd.copy(alpha = 0.9f),
                    ),
                    start = rightTop,
                    end = Offset(rightTop.x + cardW, rightTop.y + cardH),
                ),
                topLeft = rightTop,
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(24f, 24f),
            )
            // Inner horizon accent
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 16f,
                center = Offset(rightTop.x + cardW * 0.72f, rightTop.y + cardH * 0.28f),
            )
            drawRoundRect(
                color = secondaryColor,
                topLeft = Offset(rightTop.x + cardW * 0.16f, rightTop.y + cardH * 0.66f),
                size = Size(cardW * 0.68f, 14f),
                cornerRadius = CornerRadius(7f, 7f),
            )
        }
    }
}

@Composable
fun PermissionGateCard(
    showSettingsAction: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 460.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TindPhotoArtwork()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("grant_permission_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlameCoral,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.grant_access),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (showSettingsAction) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_settings_button"),
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.open_settings))
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoLightboxOverlay(
    photo: PhotoItem?,
    onClose: () -> Unit,
    onKeep: (PhotoItem) -> Unit,
    onTrash: (PhotoItem) -> Unit,
) {
    AnimatedVisibility(
        visible = photo != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (photo != null) {
            BackHandler(onBack = onClose)
            var scale by remember(photo.id) { mutableFloatStateOf(1f) }
            var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
            val context = LocalContext.current
            val swipeHaptics = rememberSwipeHaptics()

            val lightboxRequest = remember(photo.id, context) {
                ImageRequest.Builder(context)
                    .data(photo.uri)
                    .size(2048, 2048)
                    .crossfade(false)
                    .build()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f)),
            ) {
                AsyncImage(
                    model = lightboxRequest,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(photo.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                },
                            )
                        }
                        .pointerInput(photo.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = nextScale
                                offset = if (nextScale > 1f) {
                                    offset + pan
                                } else {
                                    Offset.Zero
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${photo.resolutionLabel} • ${photo.formattedSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .testTag("lightbox_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close_lightbox),
                            tint = Color.White,
                        )
                    }
                }

                // Bottom quick decision bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                ) {
                    Button(
                        onClick = {
                            swipeHaptics.onDeleteCommitted()
                            onTrash(photo)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("lightbox_trash_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrashCrimson,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            swipeHaptics.onKeepCommitted()
                            onKeep(photo)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("lightbox_keep_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepEmerald,
                            contentColor = Color(0xFF042619),
                        ),
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Keep")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoInfoBottomSheet(
    photo: PhotoItem?,
    onDismiss: () -> Unit,
    onOpenLightbox: (PhotoItem) -> Unit,
    onKeep: (PhotoItem) -> Unit,
    onTrash: (PhotoItem) -> Unit,
) {
    if (photo == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val swipeHaptics = rememberSwipeHaptics()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onOpenLightbox(photo)
                    },
                    modifier = Modifier.testTag("inspector_zoom_button"),
                ) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Zoom")
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetadataRow(
                        icon = Icons.Rounded.FolderOpen,
                        label = "Album",
                        value = photo.albumName,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.Storage,
                        label = "File Size",
                        value = photo.formattedSize,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.PhotoSizeSelectActual,
                        label = "Dimensions",
                        value = photo.resolutionLabel,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.CalendarToday,
                        label = "Date",
                        value = photo.formattedDate,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        swipeHaptics.onDeleteCommitted()
                        onTrash(photo)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("inspector_trash_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrashCrimson,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Trash")
                }
                Button(
                    onClick = {
                        swipeHaptics.onKeepCommitted()
                        onKeep(photo)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("inspector_keep_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KeepEmerald,
                        contentColor = Color(0xFF042619),
                    ),
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Keep")
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
