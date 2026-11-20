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
import com.example.R
import com.example.model.SwipeDirection
import com.example.ui.components.PermissionGateCard
import com.example.ui.components.PhotoInfoBottomSheet
import com.example.ui.components.PhotoLightboxOverlay
import com.example.ui.screens.StudioStatsScreen
import com.example.ui.screens.SwipeDeckScreen
import com.example.ui.screens.TrashBinScreen
import com.example.ui.theme.FlameCoral
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.TindPhotoTheme
import com.example.ui.viewmodel.TindPhotoViewModel
import com.example.ui.viewmodel.UpdateUiState

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

private fun galleryPermissionName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun hasGalleryPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        galleryPermissionName(),
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun TindPhotoApp(
    viewModel: TindPhotoViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) permissionDeniedOnce = true
        viewModel.onPermissionUpdated(granted)
    }

    val deleteSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        val granted = hasGalleryPermission(context)
        viewModel.onPermissionUpdated(granted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onPermissionUpdated(hasGalleryPermission(context))
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
                                permissionLauncher.launch(galleryPermissionName())
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
                                        )
                                    }

                                    composable(ROUTE_TRASH) {
                                        TrashBinScreen(
                                            state = state,
                                            onRestorePhoto = viewModel::restoreFromTrash,
                                            onRestoreAll = viewModel::restoreAllFromTrash,
                                            onInspectPhoto = viewModel::openLightbox,
                                            onDeleteAllConfirmed = viewModel::requestBatchDelete,
                                            onGoToSwipe = { navigateToTab(ROUTE_SWIPE) },
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
                                            onSaveGithubRepo = viewModel::saveGithubRepo,
                                            onOpenUpdateDialog = { viewModel.setShowUpdateDialog(true) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fullscreen Pinch-to-Zoom Lightbox
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

            // GitHub Release Update Dialog
            val updateStatus = state.updateStatus
            if (state.showUpdateDialog && updateStatus is UpdateUiState.Available) {
                val release = updateStatus.release
                AlertDialog(
                    onDismissRequest = { viewModel.setShowUpdateDialog(false) },
                    title = { Text("Update Available: ${release.tagName}") },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Current: v${state.currentVersionName} → Latest: ${release.tagName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = KeepEmerald,
                            )
                            if (release.notes.isNotBlank()) {
                                Text(
                                    text = release.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "A newer build of TindPhoto is published on GitHub Releases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.setShowUpdateDialog(false)
                                val targetUrl = release.apkDownloadUrl ?: release.htmlUrl
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                context.startActivity(intent)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KeepEmerald,
                                contentColor = Color(0xFF042619),
                            ),
                            modifier = Modifier.testTag("open_github_release_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download on GitHub")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setShowUpdateDialog(false) }) {
                            Text("Later")
                        }
                    },
                )
            }
        }
    }
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
