package com.example.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

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
) {
    val effectiveDateMillis: Long
        get() = if (dateTakenMillis > 0L) dateTakenMillis else dateAddedSeconds * 1000L

    val formattedSize: String
        get() = formatFileSize(sizeBytes)

    val formattedDate: String
        get() = formatPhotoDate(effectiveDateMillis)

    val monthKey: String
        get() = formatMonthKey(effectiveDateMillis)

    val monthLabel: String
        get() = formatMonthLabel(effectiveDateMillis)

    val resolutionLabel: String
        get() = if (width > 0 && height > 0) "${width} × ${height}" else "Unknown"
}

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    LARGEST("Largest"),
}

enum class SwipeDirection {
    LEFT_TRASH,
    RIGHT_KEEP,
}

data class SwipeRecord(
    val photo: PhotoItem,
    val direction: SwipeDirection,
)

data class TindPhotoStats(
    val totalDeletedCount: Int = 0,
    val totalKeptCount: Int = 0,
    val totalFreedBytes: Long = 0L,
)

data class AlbumSummary(
    val name: String,
    val count: Int,
    val totalBytes: Long,
)

data class MonthSummary(
    val key: String,
    val label: String,
    val totalCount: Int,
    val remainingCount: Int,
    val totalBytes: Long,
    val isCompleted: Boolean,
)

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

fun formatPhotoDate(millis: Long): String {
    if (millis <= 0L) return "Unknown date"
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun formatMonthKey(millis: Long): String {
    if (millis <= 0L) return "0000-00"
    val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
    return formatter.format(Date(millis))
}

fun formatMonthLabel(millis: Long): String {
    if (millis <= 0L) return "Unknown"
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
