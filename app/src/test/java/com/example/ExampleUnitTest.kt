package com.example

import com.example.data.GitHubUpdateChecker
import com.example.model.formatFileSize
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
    fun formatMonthKey_formatsYearAndMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }
        assertEquals("2026-11", formatMonthKey(cal.timeInMillis))
        assertEquals("0000-00", formatMonthKey(0L))
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
        assertEquals("nazim/TindPhoto", GitHubUpdateChecker.normalizeRepoSlug("nazim/TindPhoto"))
        assertEquals(
            "nazim/TindPhoto",
            GitHubUpdateChecker.normalizeRepoSlug("https://github.com/nazim/TindPhoto.git"),
        )
        assertEquals("", GitHubUpdateChecker.normalizeRepoSlug("invalid-repo"))
    }
}
