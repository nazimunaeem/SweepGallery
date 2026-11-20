package com.example

import com.example.model.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatFileSize_formatsBytesKbMbCorrectly() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("512 B", formatFileSize(512L))
        assertEquals("2 KB", formatFileSize(2048L))
        assertEquals("1.5 MB", formatFileSize(1_572_864L))
    }
}
