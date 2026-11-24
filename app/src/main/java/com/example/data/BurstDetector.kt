package com.example.data

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.BurstGroup
import com.example.model.MediaKind
import com.example.model.PhotoItem
import kotlin.math.abs

/**
 * Fast, 100% on-device detector for similar burst photos and duplicate shots.
 * Combines timestamp/album/aspect-ratio clustering with 64-bit difference hashing (dHash).
 */
object BurstDetector {

    private const val MAX_BURST_GAP_MILLIS = 12_000L
    private const val MAX_BURST_WINDOW_MILLIS = 60_000L

    fun detectBurstGroups(photos: List<PhotoItem>): List<BurstGroup> {
        val candidates = photos
            .filter { it.mediaKind == MediaKind.PHOTO && it.effectiveDateMillis > 0L }
            .sortedBy { it.effectiveDateMillis }

        if (candidates.size < 2) return emptyList()

        val groups = mutableListOf<BurstGroup>()
        var current = mutableListOf(candidates.first())

        for (i in 1 until candidates.size) {
            val prev = current.last()
            val next = candidates[i]
            val delta = abs(next.effectiveDateMillis - prev.effectiveDateMillis)
            val totalSpan = abs(next.effectiveDateMillis - current.first().effectiveDateMillis)
            val sameAlbum = next.albumName == prev.albumName
            val sameOrientation = orientationCompatible(prev, next)

            if (delta <= MAX_BURST_GAP_MILLIS && totalSpan <= MAX_BURST_WINDOW_MILLIS && sameAlbum && sameOrientation) {
                current.add(next)
            } else {
                if (current.size >= 2) {
                    groups.add(buildBurstGroup(current))
                }
                current = mutableListOf(next)
            }
        }
        if (current.size >= 2) {
            groups.add(buildBurstGroup(current))
        }

        return groups.sortedByDescending { it.photos.first().effectiveDateMillis }
    }

    private fun buildBurstGroup(items: List<PhotoItem>): BurstGroup {
        val sorted = items.sortedByDescending { it.effectiveDateMillis }
        return BurstGroup(
            id = "burst_${sorted.first().id}",
            photos = sorted,
            totalBytes = sorted.sumOf { it.sizeBytes },
        )
    }

    private fun orientationCompatible(a: PhotoItem, b: PhotoItem): Boolean {
        if (a.width <= 0 || a.height <= 0 || b.width <= 0 || b.height <= 0) return true
        val aLandscape = a.width >= a.height
        val bLandscape = b.width >= b.height
        return aLandscape == bLandscape
    }

    /**
     * Computes a 64-bit horizontal gradient difference hash (dHash) for visual similarity.
     */
    fun computeDHash64(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        var bit = 0
        try {
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val left = luminance(scaled.getPixel(x, y))
                    val right = luminance(scaled.getPixel(x + 1, y))
                    if (left > right) {
                        hash = hash or (1L shl bit)
                    }
                    bit++
                }
            }
        } finally {
            if (scaled !== bitmap && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
        return hash
    }

    fun hammingDistance(hashA: Long, hashB: Long): Int {
        return java.lang.Long.bitCount(hashA xor hashB)
    }

    private fun luminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
