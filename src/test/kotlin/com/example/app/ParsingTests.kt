package com.example.app

import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ParsingTests {
    @Test
    fun `parse valid log entry with milliseconds`() {
        val logEntry = parseLogEntry("[2024-01-01 18:59:22:123 INFO] the message")!!
        val expectedDateTime = LocalDateTime.of(2024, 1, 1, 18, 59, 22)
        assertEquals(expectedDateTime, logEntry.time)
        assertEquals("the message", logEntry.message)
    }

    @Test
    fun `handle invalid log entry`() {
        assertNull(
            parseLogEntry("invalid log entry")
        )
    }


    @Test
    fun `parsePlayerConnectedMessage should parse valid message`() {
        // given
        val message = "Player connected: Bebeuz76, xuid: 2533274908113115"

        // when
        val result = parsePlayerConnectedMessage(message)

        // then
        assertEquals("Bebeuz76", result?.player?.username)
        assertEquals("2533274908113115", result?.player?.xuid)
    }

    @Test
    fun `parsePlayerConnectedMessage should handle usernames with special characters`() {
        // given
        val message = "Player connected: Player_123-[ABC], xuid: 2533274908113115"

        // when
        val result = parsePlayerConnectedMessage(message)

        // then
        assertEquals("Player_123-[ABC]", result?.player?.username)
        assertEquals("2533274908113115", result?.player?.xuid)
    }

    @Test
    fun `parsePlayerConnectedMessage should throw on invalid format`() {
        // given
        val message = "Invalid message format"

        // then
        assertThrows<NullPointerException> {
            parsePlayerConnectedMessage(message)!!
        }
    }

    @Test
    fun `parsePlayerDisconnectedMessage should parse valid message`() {
        // given
        val message = "Player disconnected: Bebeuz76, xuid: 2533274908113115, pfid: 2ce3b0927e996530"

        // when
        val result = parsePlayerDisconnectedMessage(message)

        // then
        assertEquals("Bebeuz76", result?.player?.username)
        assertEquals("2533274908113115", result?.player?.xuid)
        assertEquals("2ce3b0927e996530", result?.player?.pfid)
    }

    @Test
    fun `parsePlayerDisconnectedMessage should handle usernames with special characters`() {
        // given
        val message = "Player disconnected: Player_123-[ABC], xuid: 2533274908113115, pfid: 2ce3b0927e996530"

        // when
        val result = parsePlayerDisconnectedMessage(message)

        // then
        assertEquals("Player_123-[ABC]", result?.player?.username)
        assertEquals("2533274908113115", result?.player?.xuid)
        assertEquals("2ce3b0927e996530", result?.player?.pfid)
    }

    @Test
    fun `parsePlayerDisconnectedMessage should throw on invalid format`() {
        // given
        val message = "Invalid message format"

        // then
        assertThrows<NullPointerException> {
            parsePlayerDisconnectedMessage(message)!!
        }
    }

    @Test
    fun `parsePlayerSpawnedMessage should parse valid message`() {
        // given
        val message = "Player Spawned: Bebeuz76 xuid: 2533274908113115, pfid: 2ce3b0927e996530"

        // when
        val result = parsePlayerSpawnedMessage(message)

        // then
        assertEquals("Bebeuz76", result?.player?.username)
        assertEquals("2533274908113115", result?.player?.xuid)
        assertEquals("2ce3b0927e996530", result?.player?.pfid)
    }

    @Test
    fun `parsePlayerSpawnedMessage should handle usernames with special characters`() {
        // given
        val message = "Player Spawned: Player_123-[ABC] xuid: 2533274908113115, pfid: 2ce3b0927e996530"

        // when
        val result = parsePlayerSpawnedMessage(message)

        // then
        assertEquals("Player_123-[ABC]", result?.player?.username)
        assertEquals("2533274908113115", result?.player?.xuid)
        assertEquals("2ce3b0927e996530", result?.player?.pfid)
    }

    @Test
    fun `parsePlayerSpawnedMessage should throw on invalid format`() {
        // given
        val message = "Invalid message format"

        // then
        assertThrows<NullPointerException> {
            parsePlayerSpawnedMessage(message)!!
        }
    }

    @Test
    fun `parsePlayerSpawnedMessage should throw when pfid is missing`() {
        // given
        val message = "Player Spawned: Bebeuz76 xuid: 2533274908113115"

        // then
        assertThrows<NullPointerException> {
            parsePlayerSpawnedMessage(message)!!
        }
    }

    @Test
    fun `full event parsing integration test`() {
        // given
        val logMessages = listOf(
            "Player connected: Bebeuz76, xuid: 2533274908113115",
            "Player Spawned: Bebeuz76 xuid: 2533274908113115, pfid: 2ce3b0927e996530",
            "Player disconnected: Bebeuz76, xuid: 2533274908113115, pfid: 2ce3b0927e996530"
        )

        // when
        val connected = parsePlayerConnectedMessage(logMessages[0])
        val spawned = parsePlayerSpawnedMessage(logMessages[1])
        val disconnected = parsePlayerDisconnectedMessage(logMessages[2])

        // then
        assertEquals(connected?.player?.username, spawned?.player?.username)
        assertEquals(connected?.player?.username, disconnected?.player?.username)
        assertEquals(connected?.player?.xuid, spawned?.player?.xuid)
        assertEquals(connected?.player?.xuid, disconnected?.player?.xuid)
        assertEquals(spawned?.player?.pfid, disconnected?.player?.pfid)
    }
}