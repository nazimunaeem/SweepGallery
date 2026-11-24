package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

enum class MediaKind {
    PHOTO,
    VIDEO,
}

enum class SmartCategory(val label: String) {
    ALL("All Media"),
    BURSTS("Similar Bursts"),
    VIDEOS("Videos"),
    LARGE_FILES("Large >10 MB"),
    SCREENSHOTS("Screenshots"),
    STARRED("Starred"),
}

@Immutable
data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val albumName: String,
    val mimeType: String = "",
    val directoryPath: String = "",
    val mediaKind: MediaKind = MediaKind.PHOTO,
    val durationMillis: Long = 0L,
) {
    val effectiveDateMillis: Long =
        if (dateTakenMillis > 0L) dateTakenMillis else dateAddedSeconds * 1000L

    val isVideo: Boolean
        get() = mediaKind == MediaKind.VIDEO

    val isScreenshot: Boolean
        get() = albumName.contains("screenshot", ignoreCase = true) ||
            directoryPath.contains("screenshot", ignoreCase = true) ||
            displayName.startsWith("Screenshot", ignoreCase = true)

    val formattedSize: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatFileSize(sizeBytes)
    }

    val formattedDuration: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatMediaDuration(durationMillis)
    }

    val formattedDate: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatPhotoDate(effectiveDateMillis)
    }

    val formattedDateTime: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatPhotoDateTime(effectiveDateMillis)
    }

    val monthKey: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatMonthKey(effectiveDateMillis)
    }

    val monthLabel: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatMonthLabel(effectiveDateMillis)
    }

    val resolutionLabel: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (width > 0 && height > 0) "${width} × ${height}" else "Unknown"
    }

    val resolutionWithMpLabel: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (width > 0 && height > 0) {
            if (mediaKind == MediaKind.VIDEO) {
                "$width × $height"
            } else {
                val mp = (width.toLong() * height.toLong()) / 1_000_000.0
                if (mp >= 0.1) {
                    String.format(Locale.US, "%d × %d (%.1f MP)", width, height, mp)
                } else {
                    "$width × $height"
                }
            }
        } else {
            "Unknown"
        }
    }

    val formatLabel: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val extFromName = displayName.substringAfterLast('.', "").uppercase(Locale.US)
        when {
            mimeType.contains('/') -> mimeType.substringAfter('/').uppercase(Locale.US)
            extFromName.isNotBlank() -> extFromName
            mediaKind == MediaKind.VIDEO -> "Video"
            else -> "Image"
        }
    }

    val displayDirectory: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        directoryPath.trim('/').ifBlank { albumName }
    }
}

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    LARGEST("Largest"),
}

enum class SwipeDirection {
    LEFT_TRASH,
    RIGHT_KEEP,
    UP_FAVORITE,
}

@Immutable
data class SwipeRecord(
    val photo: PhotoItem,
    val direction: SwipeDirection,
)

@Immutable
data class TindPhotoStats(
    val totalDeletedCount: Int = 0,
    val totalKeptCount: Int = 0,
    val totalFreedBytes: Long = 0L,
    val currentStreakDays: Int = 0,
    val weeklyFreedBytes: Long = 0L,
)

@Immutable
data class AlbumSummary(
    val name: String,
    val count: Int,
    val totalBytes: Long,
    val remainingCount: Int = count,
    val isCompleted: Boolean = false,
)

@Immutable
data class MonthSummary(
    val key: String,
    val label: String,
    val totalCount: Int,
    val remainingCount: Int,
    val totalBytes: Long,
    val isCompleted: Boolean,
)

@Immutable
data class SmartCategorySummary(
    val category: SmartCategory,
    val count: Int,
    val totalBytes: Long,
)

@Immutable
data class BurstGroup(
    val id: String,
    val photos: List<PhotoItem>,
    val totalBytes: Long,
)

private val photoDateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
}

private val photoDateTimeFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
}

private val monthKeyFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM", Locale.US)
}

private val monthLabelFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("MMM yyyy", Locale.getDefault())
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return if (digitGroups >= 2) {
        String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    } else {
        String.format(Locale.US, "%.0f %s", value, units[digitGroups])
    }
}

fun formatMediaDuration(durationMillis: Long): String {
    if (durationMillis <= 0L) return "0:00"
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(1L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatPhotoDate(millis: Long): String {
    if (millis <= 0L) return "Unknown date"
    return photoDateFormatter.get()?.format(Date(millis)) ?: "Unknown date"
}

fun formatPhotoDateTime(millis: Long): String {
    if (millis <= 0L) return "Unknown date"
    return photoDateTimeFormatter.get()?.format(Date(millis)) ?: "Unknown date"
}

fun formatMonthKey(millis: Long): String {
    if (millis <= 0L) return "0000-00"
    return monthKeyFormatter.get()?.format(Date(millis)) ?: "0000-00"
}

fun formatMonthLabel(millis: Long): String {
    if (millis <= 0L) return "Unknown"
    return monthLabelFormatter.get()?.format(Date(millis)) ?: "Unknown"
}
