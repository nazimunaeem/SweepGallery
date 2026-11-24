package com.example

import com.example.data.BurstDetector
import com.example.data.GitHubUpdateChecker
import com.example.model.SmartCategory
import com.example.model.formatFileSize
import com.example.model.formatMediaDuration
import com.example.model.formatMonthKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun formatFileSize_formatsBytesKbMbCorrectly() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("512 B", formatFileSize(512L))
        assertEquals("2 KB", formatFileSize(2048L))
        assertEquals("1.5 MB", formatFileSize(1_572_864L))
    }

    @Test
    fun formatMediaDuration_formatsSecondsMinutesAndHours() {
        assertEquals("0:00", formatMediaDuration(0L))
        assertEquals("0:05", formatMediaDuration(5_000L))
        assertEquals("1:25", formatMediaDuration(85_000L))
        assertEquals("1:01:05", formatMediaDuration(3_665_000L))
    }

    @Test
    fun formatMonthKey_formatsYearAndMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }
        assertEquals("2026-11", formatMonthKey(cal.timeInMillis))
        assertEquals("0000-00", formatMonthKey(0L))
    }

    @Test
    fun burstDetector_computesHammingDistanceCorrectly() {
        assertEquals(0, BurstDetector.hammingDistance(0L, 0L))
        assertEquals(3, BurstDetector.hammingDistance(0b1011L, 0b0000L))
        assertEquals(64, BurstDetector.hammingDistance(0L, -1L))
    }

    @Test
    fun smartCategory_containsExpectedCleanupModes() {
        assertEquals(
            listOf(
                SmartCategory.ALL,
                SmartCategory.BURSTS,
                SmartCategory.VIDEOS,
                SmartCategory.LARGE_FILES,
                SmartCategory.SCREENSHOTS,
                SmartCategory.STARRED,
            ),
            SmartCategory.entries,
        )
    }

    @Test
    fun githubUpdateChecker_comparesVersionsCorrectly() {
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.1.1", "1.1.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.2.0", "1.1.9"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("1.1.10", "1.1.2"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.1.0", "1.1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.9", "1.1.0"))
    }

    @Test
    fun githubUpdateChecker_normalizesRepoSlug() {
        assertEquals("nazimunaeem/SweepGallery", GitHubUpdateChecker.normalizeRepoSlug("nazimunaeem/SweepGallery"))
        assertEquals(
            "nazimunaeem/SweepGallery",
            GitHubUpdateChecker.normalizeRepoSlug("https://github.com/nazimunaeem/SweepGallery.git"),
        )
        assertEquals("", GitHubUpdateChecker.normalizeRepoSlug("invalid-repo"))
    }
}
