package com.jarves.mh.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSecretEntryTest {

    @Test
    fun secretMaskingHidesSensitiveValue() {
        val entry = ProjectSecretEntry(
            keyName = "GEMINI_API_KEY",
            secretValue = "AIzaSyD-1234567890abcdef",
            projectId = "test_project",
        )
        assertTrue(entry.maskedValue.contains("••••••••"))
        assertTrue(entry.maskedValue.startsWith("AIz"))
        assertTrue(entry.maskedValue.endsWith("cdef"))
        assertEquals("GEMINI_API_KEY", entry.keyName)
    }

    @Test
    fun shortSecretMasking() {
        val entry = ProjectSecretEntry(
            keyName = "PIN",
            secretValue = "12345",
            projectId = "test_project",
        )
        assertEquals("••••••", entry.maskedValue)
    }
}
