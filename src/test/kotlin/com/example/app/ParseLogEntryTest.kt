package com.example.app

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ParseLogEntryTest {
    @Test
    fun `parse valid log entry with milliseconds`() {
        val logEntry = parseLogEntry("[2024-01-01 18:59:22:123 INFO] the message")!!
        val expectedDateTime = LocalDateTime.of(2024, 1, 1, 18, 59, 22)
        assertEquals(expectedDateTime, logEntry.dateTime)
        assertEquals("the message", logEntry.message)
    }

    @Test
    fun `handle invalid log entry`() {
        assertNull(
            parseLogEntry("invalid log entry")
        )
    }
}