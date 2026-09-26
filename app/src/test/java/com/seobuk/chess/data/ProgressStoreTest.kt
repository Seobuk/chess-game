package com.seobuk.chess.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressStoreTest {
    @Test
    fun eloUpdate() {
        assertEquals(616, ProgressStore.newRating(600, 600, 1.0))
        assertEquals(584, ProgressStore.newRating(600, 600, 0.0))
        assertEquals(600, ProgressStore.newRating(600, 600, 0.5))
        // Beating a much stronger level pays more than beating a weaker one.
        assertEquals(632, ProgressStore.newRating(600, 2000, 1.0))
        assertEquals(608, ProgressStore.newRating(600, 400, 1.0))
        assertEquals(100, ProgressStore.newRating(110, 110, 0.0))
    }

    @Test
    fun oldBoardThemeIdsMapToAppThemes() {
        assertEquals("forest", ProgressStore.themeIdOf(null))
        assertEquals("walnut", ProgressStore.themeIdOf("wood"))
        assertEquals("forest", ProgressStore.themeIdOf("emerald"))
        assertEquals("ocean", ProgressStore.themeIdOf("ocean"))
        assertEquals("rose", ProgressStore.themeIdOf("rose"))
        assertEquals("slate", ProgressStore.themeIdOf("midnight"))
    }
}
