package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.os.Build
import android.provider.DocumentsContract
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShutterSpeed
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
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

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
            Canvas(modifier = Modifier.size(24.dp)) {
                val w = size.width
                val h = size.height
                // Back frosted card tilted left
                rotate(degrees = -12f, pivot = Offset(w * 0.42f, h * 0.48f)) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(w * 0.22f, h * 0.16f),
                        size = Size(w * 0.48f, h * 0.62f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                    )
                }
                // Two subtle sweep motion lines on left edge
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(w * 0.08f, h * 0.46f),
                    end = Offset(w * 0.20f, h * 0.46f),
                    strokeWidth = w * 0.065f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(w * 0.12f, h * 0.58f),
                    end = Offset(w * 0.22f, h * 0.58f),
                    strokeWidth = w * 0.065f,
                    cap = StrokeCap.Round,
                )
                // Front crisp white photo card tilted right
                rotate(degrees = 7f, pivot = Offset(w * 0.58f, h * 0.52f)) {
                    val cardLeft = w * 0.30f
                    val cardTop = h * 0.16f
                    val cardW = w * 0.54f
                    val cardH = h * 0.68f
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cardLeft, cardTop),
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(w * 0.13f, w * 0.13f),
                    )
                    // Inner photo window in brand gradient
                    val winLeft = cardLeft + cardW * 0.12f
                    val winTop = cardTop + cardH * 0.11f
                    val winW = cardW * 0.76f
                    val winH = cardH * 0.58f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(SweepLogoStart, SweepLogoEnd),
                            start = Offset(winLeft, winTop),
                            end = Offset(winLeft + winW, winTop + winH),
                        ),
                        topLeft = Offset(winLeft, winTop),
                        size = Size(winW, winH),
                        cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
                    )
                    // White sun dot & mountain ridge inside window
                    drawCircle(
                        color = Color.White,
                        radius = winW * 0.11f,
                        center = Offset(winLeft + winW * 0.73f, winTop + winH * 0.28f),
                    )
                    val ridge = Path().apply {
                        moveTo(winLeft, winTop + winH * 0.80f)
                        lineTo(winLeft + winW * 0.30f, winTop + winH * 0.48f)
                        lineTo(winLeft + winW * 0.55f, winTop + winH * 0.72f)
                        lineTo(winLeft + winW * 0.74f, winTop + winH * 0.56f)
                        lineTo(winLeft + winW, winTop + winH * 0.82f)
                        lineTo(winLeft + winW, winTop + winH)
                        lineTo(winLeft, winTop + winH)
                        close()
                    }
                    drawPath(path = ridge, color = Color.White)
                }
            }
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
                if (photo.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 84.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(photo.uri)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
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
                }

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
                        val durationExtra = if (photo.isVideo && photo.durationMillis > 0L) {
                            " • ${photo.formattedDuration}"
                        } else {
                            ""
                        }
                        Text(
                            text = "${photo.resolutionLabel} • ${photo.formattedSize}$durationExtra",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { shareMediaItem(context, photo) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .testTag("lightbox_share_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.share_photo),
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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

private data class PhotoExifMetadata(
    val cameraName: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val dateTakenExif: String? = null,
    val locationCoords: String? = null,
) {
    val hasAny: Boolean
        get() = listOf(cameraName, aperture, shutterSpeed, iso, focalLength, dateTakenExif, locationCoords)
            .any { !it.isNullOrBlank() }
}

private suspend fun readPhotoExifMetadata(
    context: Context,
    photo: PhotoItem,
): PhotoExifMetadata = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(photo.uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
            val cameraName = when {
                model.isNotBlank() && make.isNotBlank() &&
                    model.startsWith(make, ignoreCase = true) -> model
                make.isNotBlank() && model.isNotBlank() -> "$make $model"
                model.isNotBlank() -> model
                make.isNotBlank() -> make
                else -> null
            }

            val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0)
            val aperture = if (fNumber > 0.0) {
                String.format(Locale.US, "f/%.1f", fNumber).removeSuffix(".0")
            } else {
                null
            }

            val exposureSeconds = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0)
            val shutter = when {
                exposureSeconds <= 0.0 -> null
                exposureSeconds < 1.0 -> {
                    val denom = (1.0 / exposureSeconds).roundToInt().coerceAtLeast(1)
                    "1/$denom s"
                }
                else -> String.format(Locale.US, "%.1f s", exposureSeconds).replace(".0 s", " s")
            }

            @Suppress("DEPRECATION")
            val isoRaw = (exif.getAttribute("PhotographicSensitivity")
                ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS))?.trim()
            val iso = isoRaw?.takeIf { it.isNotBlank() }?.let { "ISO $it" }

            val focalRaw = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0)
            val focal = if (focalRaw > 0.0) {
                String.format(Locale.US, "%.1f mm", focalRaw).replace(".0 mm", " mm")
            } else {
                null
            }

            val dateTaken = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            @Suppress("DEPRECATION")
            val latLong = FloatArray(2)
            @Suppress("DEPRECATION")
            val hasCoords = exif.getLatLong(latLong)
            val coords = if (hasCoords) {
                String.format(Locale.US, "%.4f, %.4f", latLong[0], latLong[1])
            } else {
                null
            }

            PhotoExifMetadata(
                cameraName = cameraName,
                aperture = aperture,
                shutterSpeed = shutter,
                iso = iso,
                focalLength = focal,
                dateTakenExif = dateTaken,
                locationCoords = coords,
            )
        } ?: PhotoExifMetadata()
    }.getOrDefault(PhotoExifMetadata())
}

private fun openPhotoInFiles(context: Context, photo: PhotoItem) {
    val cleanDir = photo.directoryPath.trim('/')
    val docId = if (cleanDir.isNotBlank()) "primary:$cleanDir" else "primary:"
    val dirUri = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents",
        docId,
    )

    val attempts = listOf(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(dirUri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        Intent("android.provider.action.BROWSE").apply {
            data = dirUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(photo.uri, photo.mimeType.ifBlank { "image/*" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
    )

    for (intent in attempts) {
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (launched) return
    }

    val filesPackages = listOf(
        "com.google.android.documentsui",
        "com.android.documentsui",
        "com.google.android.apps.nbu.files",
    )
    for (pkg in filesPackages) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null && runCatching { context.startActivity(launchIntent); true }.getOrDefault(false)) {
            return
        }
    }

    Toast.makeText(
        context,
        context.getString(R.string.no_files_app_found),
        Toast.LENGTH_SHORT,
    ).show()
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
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val swipeHaptics = rememberSwipeHaptics()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SweepLogoAccentDark else SweepLogoAccentLight

    val exifMetadata by produceState(initialValue = PhotoExifMetadata(), key1 = photo.id) {
        value = readPhotoExifMetadata(context, photo)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { shareMediaItem(context, photo) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .testTag("inspector_share_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.share_photo),
                        tint = accentColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onOpenLightbox(photo)
                    },
                    modifier = Modifier.testTag("inspector_zoom_button"),
                ) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photo.isVideo) "Play" else "Zoom")
                }
            }

            // Directory & Show in Files card
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.directory_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = photo.displayDirectory,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("inspector_directory_value"),
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    FilledTonalButton(
                        onClick = { openPhotoInFiles(context, photo) },
                        modifier = Modifier.testTag("inspector_show_in_files_button"),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SweepLogoStart.copy(alpha = 0.16f),
                            contentColor = accentColor,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.show_in_files),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Core File Metadata
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
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Text(
                        text = stringResource(R.string.metadata_file_details),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.FolderOpen,
                        label = "Album",
                        value = photo.albumName,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.Image,
                        label = "Format",
                        value = photo.formatLabel,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.Storage,
                        label = "File Size",
                        value = photo.formattedSize,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.PhotoSizeSelectActual,
                        label = "Dimensions",
                        value = photo.resolutionWithMpLabel,
                    )
                    MetadataRow(
                        icon = Icons.Rounded.CalendarToday,
                        label = "Date & Time",
                        value = photo.formattedDateTime,
                    )
                }
            }

            // Camera / EXIF Metadata (shown when available)
            if (exifMetadata.hasAny) {
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
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.metadata_camera_exif),
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                        )
                        exifMetadata.cameraName?.let { camera ->
                            MetadataRow(
                                icon = Icons.Rounded.CameraAlt,
                                label = "Camera",
                                value = camera,
                            )
                        }
                        exifMetadata.focalLength?.let { focal ->
                            MetadataRow(
                                icon = Icons.Rounded.CenterFocusStrong,
                                label = "Focal Length",
                                value = focal,
                            )
                        }
                        val exposureSummary = listOfNotNull(
                            exifMetadata.aperture,
                            exifMetadata.shutterSpeed,
                        ).joinToString(" • ")
                        if (exposureSummary.isNotBlank()) {
                            MetadataRow(
                                icon = Icons.Rounded.ShutterSpeed,
                                label = "Exposure",
                                value = exposureSummary,
                            )
                        }
                        exifMetadata.iso?.let { iso ->
                            MetadataRow(
                                icon = Icons.Rounded.Exposure,
                                label = "Sensitivity",
                                value = iso,
                            )
                        }
                        exifMetadata.dateTakenExif?.let { exifDate ->
                            MetadataRow(
                                icon = Icons.Rounded.CalendarToday,
                                label = "Captured",
                                value = exifDate,
                            )
                        }
                        exifMetadata.locationCoords?.let { coords ->
                            MetadataRow(
                                icon = Icons.Rounded.LocationOn,
                                label = "Coordinates",
                                value = coords,
                            )
                        }
                    }
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
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
