package com.seobuk.chess.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test
    fun versionCompare() {
        assertTrue(Updates.isNewer("0.2.0", "0.1.0"))
        assertFalse(Updates.isNewer("0.1.0", "0.2.0"))
        assertEquals(0, Updates.compare("0.1.0", "0.1"))
        assertTrue(Updates.isNewer("1.0.10", "1.0.9"))
        assertTrue(Updates.isNewer("v0.2.0", "0.1.0"))
        assertFalse(Updates.isNewer("v0.1.0", "0.1.0"))
        assertEquals("0.2.0", Updates.version("v0.2.0"))
    }

    @Test
    fun assetAllowlist() {
        val ok = Asset("chess-master-0.2.0.apk", Updates.DOWNLOAD_PREFIX + "v0.2.0/chess-master-0.2.0.apk", 10)
        val ok2 = Asset("second.apk", Updates.DOWNLOAD_PREFIX + "v0.2.0/second.apk", 11)
        val otherRepo = Asset("evil.apk", "https://github.com/Other/chess-game/releases/download/v9/evil.apk", 1)
        val otherHost = Asset("evil.apk", "https://example.com/Seobuk/chess-game/releases/download/v9/evil.apk", 1)
        val notApk = Asset("notes.txt", Updates.DOWNLOAD_PREFIX + "v0.2.0/notes.txt", 1)
        assertNull(Updates.pickAsset(listOf(otherRepo, otherHost, notApk)))
        assertEquals(ok, Updates.pickAsset(listOf(otherRepo, notApk, ok, ok2)))
        // The debug hook adds the emulator host, and only then.
        val local = Asset("app.apk", "http://10.0.2.2:8000/app.apk", 1)
        assertNull(Updates.pickAsset(listOf(local)))
        assertEquals(local, Updates.pickAsset(listOf(local), listOf(Updates.DOWNLOAD_PREFIX, "http://10.0.2.2")))
    }

    @Test
    fun throttle() {
        val at = 10_000_000L // later than one interval after epoch 0
        assertTrue(Updates.throttled(at, at))
        assertTrue(Updates.throttled(at, at + Updates.CHECK_INTERVAL_MS - 1))
        assertFalse(Updates.throttled(at, at + Updates.CHECK_INTERVAL_MS))
        assertFalse(Updates.throttled(0, at))
        assertFalse(Updates.throttled(at + 1, at)) // clock moved backwards: check again
    }

    @Test
    fun snooze() {
        val at = 1_000_000L
        assertTrue(Updates.snoozed("0.2.0", "0.2.0", at, at + 1))
        assertTrue(Updates.snoozed("0.2.0", "0.2.0", at, at + Updates.SNOOZE_MS - 1))
        assertFalse(Updates.snoozed("0.2.0", "0.2.0", at, at + Updates.SNOOZE_MS))
        assertFalse(Updates.snoozed("0.3.0", "0.2.0", at, at + 1))
        assertFalse(Updates.snoozed("0.2.0", null, 0, at))
    }

    @Test
    fun notesFirstLine() {
        assertEquals("테스트 릴리스", Updates.firstLine("테스트 릴리스\n둘째 줄"))
        assertEquals("버그 수정", Updates.firstLine("\n\n## 버그 수정\n- 상세"))
        assertNull(Updates.firstLine(""))
        assertNull(Updates.firstLine(null))
    }
}
