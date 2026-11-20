package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AlbumSummary
import com.example.model.MonthSummary
import com.example.model.SortOrder
import com.example.model.formatFileSize
import com.example.ui.theme.FlameCoral
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.TrashCrimson
import com.example.ui.viewmodel.MONTH_ALL
import com.example.ui.viewmodel.TindPhotoUiState

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
    modifier: Modifier = Modifier,
) {
    val specificMonths = state.months.filter { it.key != MONTH_ALL }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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

        // Month-wise Quick Clean Section
        if (specificMonths.isNotEmpty()) {
            item {
                Text(
                    text = "Filter by Month",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .padding(top = 4.dp),
                )
            }

            items(specificMonths, key = { "month_${it.key}" }) { month ->
                MonthRowCard(
                    month = month,
                    isSelected = month.key == state.selectedMonth,
                    onClick = { onSelectMonthAndSwipe(month.key) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                )
            }
        }

        // Album Quick-Clean Section
        if (state.albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums on Device",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .padding(top = 4.dp),
                )
            }

            items(state.albums, key = { it.name }) { album ->
                AlbumRowCard(
                    album = album,
                    isSelected = album.name == state.selectedAlbum,
                    onClick = { onSelectAlbumAndSwipe(album.name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                )
            }
        }
    }
}

@Composable
private fun MonthRowCard(
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
                isSelected -> FlameCoral.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = month.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected || month.isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (month.isCompleted) {
                            "${month.totalCount} photos • Done"
                        } else {
                            "${month.remainingCount} of ${month.totalCount} left • ${formatFileSize(month.totalBytes)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (month.isCompleted) KeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun AlbumRowCard(
    album: AlbumSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("album_row_${album.name}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            FlameCoral.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) FlameCoral.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) FlameCoral.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = if (isSelected) FlameCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${album.count} photos • ${formatFileSize(album.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
