package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AlbumSummary
import com.example.model.MonthSummary
import com.example.model.SortOrder
import com.example.model.formatFileSize
import com.example.ui.theme.FlameCoral
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.SweepLogoAccentDark
import com.example.ui.theme.SweepLogoAccentLight
import com.example.ui.theme.SweepLogoStart
import com.example.ui.theme.TrashCrimson
import com.example.ui.viewmodel.MONTH_ALL
import com.example.ui.viewmodel.TindPhotoUiState
import com.example.ui.viewmodel.UpdateUiState

private enum class StudioBrowseTab {
    MONTH,
    ALBUM,
}

@Composable
fun StudioStatsScreen(
    state: TindPhotoUiState,
    onSelectAlbumAndSwipe: (String) -> Unit,
    onSelectMonthAndSwipe: (String) -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onSetIncludeReviewed: (Boolean) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onResetKeptHistory: () -> Unit,
    onSeedSamples: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val specificMonths = state.months.filter { it.key != MONTH_ALL }
    var selectedBrowseTab by rememberSaveable { mutableStateOf(StudioBrowseTab.MONTH) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gridColumns = if (maxWidth >= 560.dp) 4 else 3
        val monthRows = specificMonths.chunked(gridColumns)
        val albumRows = state.albums.chunked(gridColumns)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("studio_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                ) {
                    Text(
                        text = "Studio & Stats",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Track space reclaimed and filter by month or album",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Simple Developer Credit & Support Link
            item {
                DeveloperSupportCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                )
            }

            // Storage Impact Hero Card
            item {
                StorageImpactCard(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                )
            }

            // Preferences & Controls Card
            item {
                PreferencesCard(
                    state = state,
                    onSetSortOrder = onSetSortOrder,
                    onSetIncludeReviewed = onSetIncludeReviewed,
                    onSetDarkMode = onSetDarkMode,
                    onResetKeptHistory = onResetKeptHistory,
                    onSeedSamples = onSeedSamples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                )
            }

            // App Updates Card
            item {
                AppUpdatesCard(
                    state = state,
                    onCheckForUpdates = onCheckForUpdates,
                    onOpenUpdateDialog = onOpenUpdateDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                )
            }

            // Two-tab Month / Album Square Grid Browser
            if (specificMonths.isNotEmpty() || state.albums.isNotEmpty()) {
                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(top = 4.dp),
                    ) {
                        SegmentedButton(
                            selected = selectedBrowseTab == StudioBrowseTab.MONTH,
                            onClick = { selectedBrowseTab = StudioBrowseTab.MONTH },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                SegmentedButtonDefaults.Icon(
                                    active = selectedBrowseTab == StudioBrowseTab.MONTH,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            modifier = Modifier.testTag("studio_tab_month"),
                        ) {
                            Text(
                                text = stringResource(R.string.studio_tab_month),
                                maxLines = 1,
                            )
                        }
                        SegmentedButton(
                            selected = selectedBrowseTab == StudioBrowseTab.ALBUM,
                            onClick = { selectedBrowseTab = StudioBrowseTab.ALBUM },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                SegmentedButtonDefaults.Icon(
                                    active = selectedBrowseTab == StudioBrowseTab.ALBUM,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            modifier = Modifier.testTag("studio_tab_album"),
                        ) {
                            Text(
                                text = stringResource(R.string.studio_tab_album),
                                maxLines = 1,
                            )
                        }
                    }
                }

                when (selectedBrowseTab) {
                    StudioBrowseTab.MONTH -> {
                        if (monthRows.isEmpty()) {
                            item {
                                Text(
                                    text = "No months available yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        } else {
                            items(
                                items = monthRows,
                                key = { row -> "month_row_${row.first().key}" },
                            ) { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowItems.forEach { month ->
                                        MonthSquareTile(
                                            month = month,
                                            isSelected = month.key == state.selectedMonth,
                                            onClick = { onSelectMonthAndSwipe(month.key) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f),
                                        )
                                    }
                                    repeat(gridColumns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    StudioBrowseTab.ALBUM -> {
                        if (albumRows.isEmpty()) {
                            item {
                                Text(
                                    text = "No albums available yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        } else {
                            items(
                                items = albumRows,
                                key = { row -> "album_row_${row.first().name}" },
                            ) { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowItems.forEach { album ->
                                        AlbumSquareTile(
                                            album = album,
                                            isSelected = album.name == state.selectedAlbum,
                                            onClick = { onSelectAlbumAndSwipe(album.name) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f),
                                        )
                                    }
                                    repeat(gridColumns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperSupportCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val supportUrl = stringResource(R.string.support_url)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SweepLogoAccentDark else SweepLogoAccentLight

    Surface(
        modifier = modifier.testTag("developer_credit_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SweepLogoStart.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.developer_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.developer_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            FilledTonalButton(
                onClick = { runCatching { uriHandler.openUri(supportUrl) } },
                modifier = Modifier.testTag("support_developer_button"),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SweepLogoStart.copy(alpha = 0.16f),
                    contentColor = accentColor,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.support_button),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MonthSquareTile(
    month: MonthSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("stats_month_row_${month.key}"),
        shape = MaterialTheme.shapes.medium,
        color = when {
            month.isCompleted -> KeepEmerald.copy(alpha = 0.09f)
            isSelected -> FlameCoral.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                month.isCompleted -> KeepEmerald.copy(alpha = 0.45f)
                isSelected -> FlameCoral.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                month.isCompleted -> KeepEmerald.copy(alpha = 0.2f)
                                isSelected -> FlameCoral.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (month.isCompleted) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.CalendarMonth
                        },
                        contentDescription = if (month.isCompleted) "Completed" else null,
                        tint = when {
                            month.isCompleted -> KeepEmerald
                            isSelected -> FlameCoral
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(15.dp),
                    )
                }

                Text(
                    text = if (!month.isCompleted && month.remainingCount in 1 until month.totalCount) {
                        "${month.remainingCount}/${month.totalCount}"
                    } else {
                        "${month.totalCount}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        month.isCompleted -> KeepEmerald
                        isSelected -> FlameCoral
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = month.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected || month.isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (month.totalCount == 1) "1 photo" else "${month.totalCount} photos",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (month.isCompleted) KeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatFileSize(month.totalBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StorageImpactCard(
    state: TindPhotoUiState,
    modifier: Modifier = Modifier,
) {
    val totalReviewed = state.stats.totalDeletedCount + state.stats.totalKeptCount
    val keptRatio = if (totalReviewed > 0) {
        state.stats.totalKeptCount.toFloat() / totalReviewed.toFloat()
    } else {
        0.5f
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(FlameCoral, FlameOrange),
                        ),
                    )
                    .padding(18.dp),
            ) {
                Column {
                    Text(
                        text = "TOTAL SPACE FREED",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatFileSize(state.stats.totalFreedBytes),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatPill(
                    icon = Icons.Rounded.Favorite,
                    label = "Kept",
                    value = state.stats.totalKeptCount.toString(),
                    accent = KeepEmerald,
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    icon = Icons.Rounded.DeleteSweep,
                    label = "Deleted",
                    value = state.stats.totalDeletedCount.toString(),
                    accent = TrashCrimson,
                    modifier = Modifier.weight(1f),
                )
            }

            if (totalReviewed > 0) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Keep ratio (${(keptRatio * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${state.deckPhotos.size} remaining in deck",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { keptRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = KeepEmerald,
                    trackColor = TrashCrimson.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreferencesCard(
    state: TindPhotoUiState,
    onSetSortOrder: (SortOrder) -> Unit,
    onSetIncludeReviewed: (Boolean) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onResetKeptHistory: () -> Unit,
    onSeedSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Deck Order",
                style = MaterialTheme.typography.titleMedium,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SortOrder.entries.forEachIndexed { index, order ->
                    SegmentedButton(
                        selected = state.sortOrder == order,
                        onClick = { onSetSortOrder(order) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SortOrder.entries.size,
                        ),
                        modifier = Modifier.testTag("stats_sort_${order.name.lowercase()}"),
                    ) {
                        Text(order.label, maxLines = 1)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column {
                        Text(
                            text = "Include kept photos",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Show photos you already swiped right on",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = state.includeReviewed,
                    onCheckedChange = onSetIncludeReviewed,
                    modifier = Modifier.testTag("include_reviewed_switch"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column {
                        Text(
                            text = "Darkroom Studio Theme",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Obsidian backdrop makes photos pop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = state.darkMode,
                    onCheckedChange = onSetDarkMode,
                    modifier = Modifier.testTag("dark_mode_switch"),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = onResetKeptHistory,
                    modifier = Modifier.testTag("reset_kept_history_button"),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset Kept History")
                }

                TextButton(
                    onClick = onSeedSamples,
                    enabled = !state.isSeeding,
                    modifier = Modifier.testTag("stats_add_samples_button"),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Sample Photos")
                }
            }
        }
    }
}

@Composable
private fun AlbumSquareTile(
    album: AlbumSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("album_row_${album.name}"),
        shape = MaterialTheme.shapes.medium,
        color = when {
            album.isCompleted -> KeepEmerald.copy(alpha = 0.09f)
            isSelected -> FlameCoral.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                album.isCompleted -> KeepEmerald.copy(alpha = 0.45f)
                isSelected -> FlameCoral.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                album.isCompleted -> KeepEmerald.copy(alpha = 0.2f)
                                isSelected -> FlameCoral.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (album.isCompleted) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Folder
                        },
                        contentDescription = if (album.isCompleted) "Completed" else null,
                        tint = when {
                            album.isCompleted -> KeepEmerald
                            isSelected -> FlameCoral
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(15.dp),
                    )
                }

                Text(
                    text = if (!album.isCompleted && album.remainingCount in 1 until album.count) {
                        "${album.remainingCount}/${album.count}"
                    } else {
                        "${album.count}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        album.isCompleted -> KeepEmerald
                        isSelected -> FlameCoral
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected || album.isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (album.count == 1) "1 photo" else "${album.count} photos",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (album.isCompleted) KeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatFileSize(album.totalBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AppUpdatesCard(
    state: TindPhotoUiState,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = FlameCoral,
                )
                Column {
                    Text(
                        text = "App Updates",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Installed: v${state.currentVersionName} (${state.currentVersionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (val status = state.updateStatus) {
                is UpdateUiState.Available -> {
                    Button(
                        onClick = onOpenUpdateDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("view_available_update_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepEmerald,
                            contentColor = Color(0xFF042619),
                        ),
                    ) {
                        Text("Update Available: ${status.release.tagName}")
                    }
                }
                is UpdateUiState.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = FlameCoral,
                        )
                        Text(
                            text = "Checking for updates…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is UpdateUiState.UpToDate -> {
                    Text(
                        text = if (status.latestTag != null) {
                            "You are on the latest version (${status.latestTag})"
                        } else {
                            "No newer releases available"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = KeepEmerald,
                    )
                }
                is UpdateUiState.Error -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TrashCrimson,
                    )
                }
                UpdateUiState.Idle -> Unit
            }

            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = state.updateStatus !is UpdateUiState.Checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("check_updates_button"),
                shape = CircleShape,
            ) {
                Text("Check for Updates")
            }
        }
    }
}
