package com.seobuk.chess.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.seobuk.chess.ui.theme.AppThemes
import com.seobuk.chess.ui.theme.Tones
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val (la, lb) = a.luminance() to b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    private fun check(what: String, fg: Color, bg: Color, min: Float) =
        assertTrue("$what ${"%.2f".format(ratio(fg, bg))} < $min", ratio(fg, bg) >= min)

    @Test
    fun everyThemeMeetsTextContrastInBothModes() {
        for (t in AppThemes.all) for (p in listOf(t.light, t.dark)) {
            val id = "${t.id}/${if (p.dark) "dark" else "light"}"
            for ((name, bg) in listOf("bg" to p.bg, "surface" to p.surface, "surface2" to p.surface2)) {
                check("$id text/$name", p.text, bg, 4.5f)
                check("$id muted/$name", p.muted, bg, 4.5f)
            }
            check("$id on/primary", p.on, p.primary, 4.5f)
            check("$id ink/soft", p.ink, p.soft, 4.5f)
            check("$id muted/soft", p.muted, p.soft, 4.5f) // descriptions on a selected (soft) row
            check("$id ink/surface", p.ink, p.surface, 4.5f)
            check("$id primary/bg", p.primary, p.bg, 3f)
        }
    }

    /** Coordinates and the selection ring (board ink) and legal-move dots (on the light square or its halo) read >= 3:1. */
    @Test
    fun boardMarksReadOnBothSquares() {
        for (t in AppThemes.all) {
            val p = t.light
            check("${t.id} ink/light", p.boardInk, p.boardLight, 3f)
            check("${t.id} ink/dark", p.boardInk, p.boardDark, 3f)
            check("${t.id} legal/light", p.boardLegal.compositeOver(p.boardLight), p.boardLight, 3f)
        }
    }

    @Test
    fun semanticTonesReadInBothModes() {
        val tones = listOf(Tones.best, Tones.excellent, Tones.good, Tones.book, Tones.inaccuracy, Tones.mistake, Tones.blunder)
        for ((i, tone) in tones.withIndex()) {
            check("tone $i white/solid", Color.White, tone.solid, 4.5f)
            for (t in AppThemes.all) for (p in listOf(t.light, t.dark)) {
                val text = tone.text(p.dark)
                check("tone $i ${t.id} text/surface", text, p.surface, 4.5f)
                check("tone $i ${t.id} text/chip", text, tone.container(p.dark).compositeOver(p.surface), 4.5f)
            }
        }
    }
}
