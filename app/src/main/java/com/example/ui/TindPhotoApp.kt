package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import com.example.R
import com.example.data.GitHubReleaseInfo
import com.example.data.InAppUpdater
import com.example.data.VideoThumbnailFetcher
import com.example.model.SwipeDirection
import com.example.model.formatFileSize
import com.example.ui.components.BurstCompareSheet
import com.example.ui.components.DeckJumpSheet
import com.example.ui.components.PermissionGateCard
import com.example.ui.components.PhotoInfoBottomSheet
import com.example.ui.components.PhotoLightboxOverlay
import com.example.ui.screens.StudioStatsScreen
import com.example.ui.screens.SwipeDeckScreen
import com.example.ui.screens.TrashBinScreen
import com.example.ui.theme.FlameCoral
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.TindPhotoTheme
import com.example.ui.theme.TrashCrimson
import com.example.ui.theme.UndoAmber
import com.example.ui.viewmodel.TindPhotoViewModel
import com.example.ui.viewmodel.UpdateDownloadState
import com.example.ui.viewmodel.UpdateUiState
import java.io.File

const val ROUTE_SWIPE = "swipe"
const val ROUTE_TRASH = "trash"
const val ROUTE_STATS = "stats"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val TopDestinations = listOf(
    NavDestination(
        route = ROUTE_SWIPE,
        labelRes = R.string.nav_swipe,
        selectedIcon = Icons.Rounded.Style,
        unselectedIcon = Icons.Outlined.Style,
        testTag = "nav_tab_swipe",
    ),
    NavDestination(
        route = ROUTE_TRASH,
        labelRes = R.string.nav_trash,
        selectedIcon = Icons.Rounded.Delete,
        unselectedIcon = Icons.Outlined.DeleteOutline,
        testTag = "nav_tab_trash",
    ),
    NavDestination(
        route = ROUTE_STATS,
        labelRes = R.string.nav_stats,
        selectedIcon = Icons.Rounded.AutoAwesomeMosaic,
        unselectedIcon = Icons.Outlined.AutoAwesomeMosaic,
        testTag = "nav_tab_stats",
    ),
)

private fun galleryPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private data class GalleryAccessStatus(
    val hasAccess: Boolean,
    val isLimited: Boolean,
)

private fun evaluateGalleryAccess(context: Context): GalleryAccessStatus {
    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val hasFullVisual = isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            val hasSelected = isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            GalleryAccessStatus(
                hasAccess = hasFullVisual || hasSelected,
                isLimited = hasSelected && !hasFullVisual,
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            val hasMedia = isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            GalleryAccessStatus(hasAccess = hasMedia, isLimited = false)
        }
        else -> {
            val hasStorage = isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
            GalleryAccessStatus(hasAccess = hasStorage, isLimited = false)
        }
    }
}

@Composable
fun TindPhotoApp(
    viewModel: TindPhotoViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val snackbarHostState = remember { SnackbarHostState() }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    remember(appContext) {
        val loader = ImageLoader.Builder(appContext)
            .components {
                add(VideoThumbnailFetcher.Factory(appContext))
            }
            .build()
        Coil.setImageLoader(loader)
        loader
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val access = evaluateGalleryAccess(context)
        if (!access.hasAccess) permissionDeniedOnce = true
        viewModel.onPermissionUpdated(
            granted = access.hasAccess,
            isLimitedAccess = access.isLimited,
        )
    }

    val deleteSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onInstallPermissionReturned()
    }

    LaunchedEffect(Unit) {
        val access = evaluateGalleryAccess(context)
        viewModel.onPermissionUpdated(
            granted = access.hasAccess,
            isLimitedAccess = access.isLimited,
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val access = evaluateGalleryAccess(context)
        viewModel.onPermissionUpdated(
            granted = access.hasAccess,
            isLimitedAccess = access.isLimited,
        )
        viewModel.onInstallPermissionReturned()
    }

    LaunchedEffect(state.pendingApkInstallPath) {
        val path = state.pendingApkInstallPath
        if (path != null) {
            viewModel.consumePendingApkInstall()
            val apkFile = File(path)
            if (apkFile.exists()) {
                val installIntent = InAppUpdater.buildInstallApkIntent(context, apkFile)
                runCatching { context.startActivity(installIntent) }
            }
        }
    }

    LaunchedEffect(state.pendingDeleteSender) {
        val sender = state.pendingDeleteSender
        if (sender != null) {
            deleteSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    TindPhotoTheme(darkTheme = state.darkMode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: ROUTE_SWIPE

        val navigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (state.hasPermission && !useRail) {
                            TindPhotoBottomBar(
                                currentRoute = currentRoute,
                                trashCount = state.trashQueue.size,
                                onSelectTab = navigateToTab,
                            )
                        }
                    },
                ) { innerPadding ->
                    if (!state.hasPermission) {
                        PermissionGateCard(
                            showSettingsAction = permissionDeniedOnce,
                            onRequestPermission = {
                                permissionLauncher.launch(galleryPermissions())
                            },
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            if (useRail) {
                                TindPhotoNavigationRail(
                                    currentRoute = currentRoute,
                                    trashCount = state.trashQueue.size,
                                    onSelectTab = navigateToTab,
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                val updateStatus = state.updateStatus
                                if (updateStatus is UpdateUiState.Available && !updateStatus.bannerDismissed) {
                                    Surface(
                                        onClick = { viewModel.setShowUpdateDialog(true) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("update_available_banner"),
                                        shape = MaterialTheme.shapes.medium,
                                        color = KeepEmerald.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.5f)),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SystemUpdate,
                                                    contentDescription = null,
                                                    tint = KeepEmerald,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = "New release ${updateStatus.release.tagName} available",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = KeepEmerald,
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = { viewModel.setShowUpdateDialog(true) },
                                                ) {
                                                    Text("Update", color = KeepEmerald, fontWeight = FontWeight.Bold)
                                                }
                                                IconButton(
                                                    onClick = viewModel::dismissUpdateBanner,
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Dismiss update banner",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_SWIPE,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                    composable(ROUTE_SWIPE) {
                                        SwipeDeckScreen(
                                            state = state,
                                            onSwipe = viewModel::swipePhoto,
                                            onUndo = viewModel::undoLastSwipe,
                                            onInspectPhoto = viewModel::openInspector,
                                            onOpenLightbox = viewModel::openLightbox,
                                            onSelectAlbum = viewModel::selectAlbum,
                                            onSelectMonth = viewModel::selectMonth,
                                            onSelectSortOrder = viewModel::setSortOrder,
                                            onRefresh = viewModel::refreshGallery,
                                            onSeedSamples = viewModel::seedSamplePhotos,
                                            onIncludeReviewed = { viewModel.setIncludeReviewed(true) },
                                            onOpenTrashTab = { navigateToTab(ROUTE_TRASH) },
                                            onRequestMoreMedia = {
                                                permissionLauncher.launch(galleryPermissions())
                                            },
                                            onSelectSmartCategory = viewModel::selectSmartCategory,
                                            onOpenBurstCompare = viewModel::openBurstCompare,
                                            onOpenJumpSheet = { viewModel.setShowJumpSheet(true) },
                                        )
                                    }

                                    composable(ROUTE_TRASH) {
                                        TrashBinScreen(
                                            state = state,
                                            onRestorePhoto = viewModel::restoreFromTrash,
                                            onRestoreAll = viewModel::restoreAllFromTrash,
                                            onInspectPhoto = viewModel::openLightbox,
                                            onDeleteAllConfirmed = { viewModel.requestBatchDelete() },
                                            onGoToSwipe = { navigateToTab(ROUTE_SWIPE) },
                                            onRestoreSelected = { ids -> viewModel.restoreBatchFromTrash(ids) },
                                            onDeleteSelected = { targets -> viewModel.requestBatchDelete(targets) },
                                        )
                                    }

                                    composable(ROUTE_STATS) {
                                        StudioStatsScreen(
                                            state = state,
                                            onSelectAlbumAndSwipe = { album ->
                                                viewModel.selectAlbum(album)
                                                navigateToTab(ROUTE_SWIPE)
                                            },
                                            onSelectMonthAndSwipe = { monthKey ->
                                                viewModel.selectMonth(monthKey)
                                                navigateToTab(ROUTE_SWIPE)
                                            },
                                            onSetSortOrder = viewModel::setSortOrder,
                                            onSetIncludeReviewed = viewModel::setIncludeReviewed,
                                            onSetDarkMode = viewModel::setDarkMode,
                                            onResetKeptHistory = viewModel::resetKeptHistory,
                                            onSeedSamples = viewModel::seedSamplePhotos,
                                            onCheckForUpdates = viewModel::checkForUpdates,
                                            onOpenUpdateDialog = { viewModel.setShowUpdateDialog(true) },
                                            onSelectSmartCategoryAndSwipe = { category ->
                                                viewModel.selectSmartCategory(category)
                                                navigateToTab(ROUTE_SWIPE)
                                            },
                                            onSetUseSystemTrash = viewModel::setUseSystemTrash,
                                            onSetAutoCheckUpdates = viewModel::setAutoCheckUpdates,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fullscreen Pinch-to-Zoom & Video Lightbox
            PhotoLightboxOverlay(
                photo = state.lightboxPhoto,
                onClose = { viewModel.openLightbox(null) },
                onKeep = { photo -> viewModel.swipePhoto(photo, SwipeDirection.RIGHT_KEEP) },
                onTrash = { photo -> viewModel.swipePhoto(photo, SwipeDirection.LEFT_TRASH) },
            )

            // Photo Details Modal Sheet
            PhotoInfoBottomSheet(
                photo = state.inspectingPhoto,
                onDismiss = { viewModel.openInspector(null) },
                onOpenLightbox = viewModel::openLightbox,
                onKeep = { photo -> viewModel.swipePhoto(photo, SwipeDirection.RIGHT_KEEP) },
                onTrash = { photo -> viewModel.swipePhoto(photo, SwipeDirection.LEFT_TRASH) },
            )

            // Burst / Similar Shots Comparison Sheet
            BurstCompareSheet(
                burstGroup = state.comparingBurstGroup,
                onDismiss = { viewModel.openBurstCompare(null) },
                onKeepBestAndTrashRest = viewModel::keepOnlyPhotoInBurst,
                onOpenLightbox = viewModel::openLightbox,
            )

            // Deck Quick-Jump Sheet
            DeckJumpSheet(
                visible = state.showJumpSheet,
                deckPhotos = state.deckPhotos,
                favoriteIds = state.favoritePhotoIds,
                onDismiss = { viewModel.setShowJumpSheet(false) },
                onSelectPhoto = { photo -> viewModel.jumpToPhotoInDeck(photo.id) },
            )

            // GitHub Release Update Popup Modal
            val updateStatus = state.updateStatus
            if (state.showUpdateDialog && updateStatus is UpdateUiState.Available) {
                UpdateAvailableDialog(
                    currentVersionName = state.currentVersionName,
                    release = updateStatus.release,
                    downloadState = state.updateDownloadState,
                    onDownload = {
                        if (updateStatus.release.apkDownloadUrl != null) {
                            viewModel.startInAppUpdate(updateStatus.release)
                        } else {
                            viewModel.setShowUpdateDialog(false)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateStatus.release.htmlUrl))
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onInstallApk = viewModel::installDownloadedUpdate,
                    onGrantInstallPermission = {
                        val intent = InAppUpdater.buildManageUnknownSourcesIntent(context)
                        runCatching { unknownSourcesLauncher.launch(intent) }
                    },
                    onCancelDownload = viewModel::cancelInAppUpdate,
                    onOpenBrowser = {
                        val targetUrl = updateStatus.release.apkDownloadUrl ?: updateStatus.release.htmlUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                        runCatching { context.startActivity(intent) }
                    },
                    onDismiss = viewModel::dismissUpdateDialog,
                )
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    currentVersionName: String,
    release: GitHubReleaseInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onInstallApk: () -> Unit = {},
    onGrantInstallPermission: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
) {
    val isDownloading = downloadState is UpdateDownloadState.Downloading
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        modifier = Modifier.testTag("update_available_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = KeepEmerald,
                modifier = Modifier.size(30.dp),
            )
        },
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = KeepEmerald.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "v$currentVersionName → ${release.tagName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = KeepEmerald,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    if (release.apkSizeBytes > 0L) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = formatFileSize(release.apkSizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                if (release.title.isNotBlank() && release.title != release.tagName) {
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = release.notes.ifBlank {
                        "A newer build of SweepGallery is published on GitHub Releases."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        val hasTotal = downloadState.totalBytes > 0L
                        val fraction = if (hasTotal) {
                            (downloadState.bytesDownloaded.toFloat() / downloadState.totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_download_progress"),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = if (fraction != null) {
                                    "Downloading ${formatFileSize(downloadState.bytesDownloaded)} / ${formatFileSize(downloadState.totalBytes)} (${(fraction * 100).toInt()}%)"
                                } else {
                                    "Downloading ${formatFileSize(downloadState.bytesDownloaded)}…"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = KeepEmerald,
                            )
                            if (fraction != null) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = KeepEmerald,
                                )
                            } else {
                                androidx.compose.material3.LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = KeepEmerald,
                                )
                            }
                        }
                    }
                    is UpdateDownloadState.NeedsInstallPermission -> {
                        Text(
                            text = "Allow SweepGallery to install unknown apps in system settings, then Android will launch the update installer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UndoAmber,
                        )
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Text(
                            text = "APK verified and ready to install.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeepEmerald,
                        )
                    }
                    is UpdateDownloadState.Failed -> {
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TrashCrimson,
                        )
                    }
                    UpdateDownloadState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is UpdateDownloadState.Downloading -> {
                    TextButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.testTag("cancel_update_download_button"),
                    ) {
                        Text("Cancel")
                    }
                }
                is UpdateDownloadState.NeedsInstallPermission -> {
                    Button(
                        onClick = onGrantInstallPermission,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UndoAmber,
                            contentColor = Color(0xFF281800),
                        ),
                        modifier = Modifier.testTag("allow_install_unknown_apps_button"),
                    ) {
                        Text("Allow & Install")
                    }
                }
                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = onInstallApk,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepEmerald,
                            contentColor = Color(0xFF042619),
                        ),
                        modifier = Modifier.testTag("install_downloaded_apk_button"),
                    ) {
                        Text("Install Update")
                    }
                }
                is UpdateDownloadState.Failed, UpdateDownloadState.Idle -> {
                    Button(
                        onClick = onDownload,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepEmerald,
                            contentColor = Color(0xFF042619),
                        ),
                        modifier = Modifier.testTag("open_github_release_button"),
                    ) {
                        Icon(
                            imageVector = if (release.apkDownloadUrl != null) {
                                Icons.Rounded.SystemUpdate
                            } else {
                                Icons.AutoMirrored.Rounded.OpenInNew
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                downloadState is UpdateDownloadState.Failed -> "Retry Download"
                                release.apkDownloadUrl != null -> "Download & Install"
                                else -> "Open GitHub"
                            },
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isDownloading && release.apkDownloadUrl != null) {
                    TextButton(
                        onClick = onOpenBrowser,
                        modifier = Modifier.testTag("update_open_browser_button"),
                    ) {
                        Text("Browser")
                    }
                }
                if (!isDownloading) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_dialog_later_button"),
                    ) {
                        Text("Later")
                    }
                }
            }
        },
    )
}

@Composable
private fun TindPhotoBottomBar(
    currentRoute: String,
    trashCount: Int,
    onSelectTab: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        TopDestinations.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(dest.route) },
                modifier = Modifier.testTag(dest.testTag),
                icon = {
                    if (dest.route == ROUTE_TRASH && trashCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = FlameCoral) {
                                    Text(trashCount.toString())
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = stringResource(dest.labelRes),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = stringResource(dest.labelRes),
                        )
                    }
                },
                label = { Text(stringResource(dest.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FlameCoral,
                    selectedTextColor = FlameCoral,
                    indicatorColor = FlameCoral.copy(alpha = 0.16f),
                ),
            )
        }
    }
}

@Composable
private fun TindPhotoNavigationRail(
    currentRoute: String,
    trashCount: Int,
    onSelectTab: (String) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        TopDestinations.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(dest.route) },
                modifier = Modifier.testTag(dest.testTag),
                icon = {
                    if (dest.route == ROUTE_TRASH && trashCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = FlameCoral) {
                                    Text(trashCount.toString())
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = stringResource(dest.labelRes),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = stringResource(dest.labelRes),
                        )
                    }
                },
                label = { Text(stringResource(dest.labelRes)) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = FlameCoral,
                    selectedTextColor = FlameCoral,
                    indicatorColor = FlameCoral.copy(alpha = 0.16f),
                ),
            )
        }
    }
}
