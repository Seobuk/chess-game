package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.BadukGame
import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.Dia
import com.seobuk.chess.baduk.core.PASS
import com.seobuk.chess.baduk.core.Points
import com.seobuk.chess.baduk.core.Scoring
import com.seobuk.chess.baduk.core.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadukCoachTest {
    // Five white stones in atari cut Black in two; at a they would join their friends.
    private val fight = Dia(
        ". . X O . . . . .",
        ". . X O . . . . .",
        ". . X O . . . . .",
        "X . X O O O O O .",
        ". . X X X X X O .",
        ". X O O O O O a .",
        ". . X X X X X . .",
        ". . O . . . . O .",
        ". . . . b . . . .",
    )

    /** One plain Korean sentence in 해요체 per period, no hype. */
    private fun assertKorean(text: String) {
        assertTrue(text, text.isNotBlank() && text.endsWith("요."))
        assertTrue(text, text.any { it in '가'..'힣' })
        assertTrue(text, text.all { it in '가'..'힣' || it in ' '..'~' })
        assertTrue(text, text.none { it in "!?-" } && "  " !in text)
    }

    @Test fun estimateFindsDeadStonesAndLeavesTheLivingAlone() {
        // SETTLED_ROWS with one hopeless stone inside each side's area.
        val d = Dia(
            ". . X . X O . O .",
            ". . X . X O . O .",
            ". . X . X O B O .",
            ". . X . X O . O .",
            "X X X W X O O O O",
            ". . X . X O . O .",
            ". . X . X O . O .",
            ". . X . X O . O .",
            ". . X . X O . O .",
        )
        val deadWhite = Points.of(3, 4, 9)
        val deadBlack = Points.of(6, 2, 9)
        for (seed in 1L..5L) {
            val e = BadukCoach.estimate(d.board, 6.5, 400, seed)
            assertEquals("seed $seed", setOf(deadWhite, deadBlack), e.dead)
            assertEquals(81, e.ownership.size)
            for (p in 0 until 81) {
                val o = e.ownership[p]
                assertTrue("seed $seed, ${Points.name(p, 9)}: $o", if (p % 9 <= 4) o > 0.6f else o < -0.6f)
            }
            assertEquals(2.5, e.lead, 0.01)
            val s = Scoring.territory(d.board, e.dead, 6.5)
            assertEquals(Stone.BLACK, s.winner)
            assertEquals(2.5, s.margin, 0.0)
        }
        assertEquals(setOf(deadWhite, deadBlack), BadukCoach.estimate(d.board, 6.5).dead)
    }

    @Test fun estimateOfAnEmptyBoardIsOpen() {
        val e = BadukCoach.estimate(Board(9), 6.5, 300, 1)
        assertTrue(e.dead.isEmpty())
        assertTrue("${e.lead}", e.lead in -20.0..10.0)
    }

    @Test fun describeSaysWhatTheMoveDoes() {
        assertEquals("한 수 쉬었어요.", BadukCoach.describe(fight.board, PASS))
        assertEquals("H4에 두어 백돌 5개를 따냈어요.", BadukCoach.describe(fight.board, fight['a']))
        assertEquals("C5에는 둘 수 없어요.", BadukCoach.describe(fight.board, Points.parse("C5", 9)))

        val d = Dia(
            ". . . . . . . . .",
            ". . . a . . . . .",
            ". . X O b . . . .",
            ". . . X . . . . .",
            ". . . . . . . . .",
            ". . X c X . O . .",
            ". . . d . . . . .",
            ". . . . . O e O .",
            ". . . . . . . . .",
        )
        val said = mapOf(
            'a' to "단수",      // two liberties become one
            'c' to "이었어요",   // two black groups become one
            'd' to "호구",      // guards c without filling it
            'e' to "끊었어요",   // between two white stones
        )
        for ((label, word) in said) {
            val text = BadukCoach.describe(d.board, d[label])
            assertTrue("$label: $text", word in text && text.startsWith(Points.name(d[label], 9) + "에 두어 "))
            assertKorean(text)
        }
        // White running out of atari.
        val run = d.board.copy().also { it.play(d['a']) }
        assertTrue(BadukCoach.describe(run, d['b']), "살렸어요" in BadukCoach.describe(run, d['b']))
        // Every point of a real position gets a proper sentence.
        for (p in fight.board.legalMoves()) assertKorean(BadukCoach.describe(fight.board, p))
        assertKorean(BadukCoach.describe(Board(19), Points.parse("K10", 19)))
    }

    @Test fun describeNamesAKo() {
        val ko = Dia(
            ". . . . .",
            ". X O . .",
            "X a X O .",
            ". X O . .",
            ". . . . .",
            toMove = Stone.WHITE,
        )
        assertEquals("B3에 두어 패를 따냈어요.", BadukCoach.describe(ko.board, ko['a']))
    }

    @Test fun hintPointsAtTheCapture() {
        val h = BadukCoach.hint(fight.board, 6.5, thinkMs = 300, seed = 3)!!
        assertEquals(fight['a'], h.point)
        assertEquals("H4에 두면 백돌 5개를 따낼 수 있어요.", h.explanation)
        assertTrue(h.winRate > 0.6)

        val done = BadukCoach.hint(position(SETTLED_ROWS, Stone.BLACK), 6.5, thinkMs = 300, seed = 3)!!
        assertEquals(PASS, done.point)
        assertKorean(done.explanation)

        assertNull(BadukCoach.hint(fight.board, 6.5, thinkMs = 300, isCancelled = { true }))
        val over = BadukGame(9).also { it.pass(); it.pass() }
        assertNull(BadukCoach.hint(over, 200))
        assertNotNull(BadukCoach.hint(BadukGame(9).also { it.play(40) }, 200))
    }

    @Test fun reviewPraisesTheCaptureAndNamesTheMissedOne() {
        val good = BadukCoach.review(fight.board, fight['a'], 6.5, thinkMs = 500, seed = 5)
        assertEquals(BadukFeedback.Kind.GOOD, good.kind)
        assertNull(good.better)
        assertTrue(good.explanation, "따냈어요" in good.explanation)
        assertKorean(good.explanation)

        val missed = BadukCoach.review(fight.board, fight['b'], 6.5, thinkMs = 500, seed = 5)
        assertTrue("${missed.kind}", missed.kind >= BadukFeedback.Kind.MISTAKE)
        assertEquals(fight['a'], missed.better)
        assertEquals("H4에 두면 백돌 5개를 따낼 수 있었어요.", missed.explanation)

        val pass = BadukCoach.review(fight.board, PASS, 6.5, thinkMs = 500, seed = 5)
        assertEquals(BadukFeedback.Kind.MISTAKE, pass.kind)
        assertEquals(fight['a'], pass.better)
        assertKorean(pass.explanation)

        val rightPass = BadukCoach.review(position(SETTLED_ROWS, Stone.WHITE), PASS, 6.5, thinkMs = 500, seed = 5)
        assertEquals(BadukFeedback.Kind.GOOD, rightPass.kind)
    }

    @Test fun reviewCallsSelfAtariOfABigGroupABlunder() {
        // Three stones with two liberties: b joins them to their friends, a leaves them one liberty.
        val d = Dia(
            ". . . . . . . . .",
            ". O O O O . . . .",
            "O X X X a O . . .",
            ". O O b O . . . .",
            ". . X X X . . . .",
            ". . . . . . . . .",
            ". . . . . . O . .",
            ". . . X . . . . .",
            ". . . . . . . . .",
        )
        for (seed in 1L..3L) {
            val f = BadukCoach.review(d.board, d['a'], 6.5, thinkMs = 400, seed = seed)
            assertEquals(BadukFeedback.Kind.BLUNDER, f.kind)
            assertEquals("돌 4개가 단수에 몰렸어요. 상대가 D6에 두면 잡혀요.", f.explanation)
            assertNotNull(f.better)
            assertTrue(f.better != d['a'])
        }
    }

    @Test fun reviewNamesStonesLeftInAtari() {
        val white = fight.board.copy().also { it.setToMove(Stone.WHITE) }
        val f = BadukCoach.review(white, fight['b'], 6.5, thinkMs = 400, seed = 1)
        assertEquals(BadukFeedback.Kind.BLUNDER, f.kind)
        assertEquals(fight['a'], f.better)
        assertEquals("단수에 몰린 돌 5개를 그대로 두었어요. H4에 두면 살릴 수 있었어요.", f.explanation)
        val saved = BadukCoach.review(white, fight['a'], 6.5, thinkMs = 400, seed = 1)
        assertEquals(BadukFeedback.Kind.GOOD, saved.kind)
        assertTrue(saved.explanation, "살렸어요" in saved.explanation)
    }

    @Test fun reviewThroughAGame() {
        val game = BadukGame(9)
        for (name in listOf("E5", "E4", "D4", "F4")) assertTrue(game.play(Points.parse(name, 9)))
        val f = BadukCoach.review(game, Points.parse("E3", 9), thinkMs = 300)
        assertKorean(f.explanation)
        assertEquals(4, game.moves.size) // the game itself is untouched
        val edge = BadukCoach.review(game, Points.parse("A1", 9), thinkMs = 300)
        assertTrue("${edge.kind}", edge.kind >= BadukFeedback.Kind.OK)
        assertNotNull(edge.better)
    }

    @Test fun cancelledReviewComesBackAtOnce() {
        val start = System.nanoTime()
        BadukCoach.review(Board(19), 180, 6.5, thinkMs = 20_000, isCancelled = { true })
        assertTrue((System.nanoTime() - start) / 1_000_000 < 500)
    }
}
