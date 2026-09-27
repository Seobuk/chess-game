package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.Dia
import com.seobuk.chess.baduk.core.PASS
import com.seobuk.chess.baduk.core.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every border is closed and every group has room for many eyes: Black leads by 2.5. */
internal val SETTLED_ROWS = arrayOf(
    ". . X . X O . O .",
    ". . X . X O . O .",
    ". . X . X O . O .",
    ". . X . X O . O .",
    "X X X . X O O O O",
    ". . X . X O . O .",
    ". . X . X O . O .",
    ". . X . X O . O .",
    ". . X . X O . O .",
)

/** [rows] with [toMove] to play; [passed]: the opponent has just passed. */
internal fun position(rows: Array<String>, toMove: Stone, passed: Boolean = false): Board {
    val b = Board.fromRows(rows.toList(), if (passed) toMove.opposite else toMove)
    if (passed) b.play(PASS)
    return b
}

class BadukEngineTest {
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

    @Test fun movesAreLegalAndInTime() {
        BadukEngine(1).search(Board(9), 6.5, 300) // warm-up
        for (size in intArrayOf(9, 13, 19)) {
            val b = Board(size)
            val engine = BadukEngine(size.toLong())
            repeat(6) {
                val start = System.nanoTime()
                val s = engine.search(b, 6.5, 150)
                val ms = (System.nanoTime() - start) / 1_000_000
                assertTrue("$size: $ms ms", ms < 400)
                assertTrue("$size: ${s.playouts} playouts", s.playouts > 50)
                assertNotEquals(PASS, s.best)
                assertTrue(b.isLegal(s.best))
                assertFalse(b.isEyeLike(s.best, b.toMove))
                assertEquals(size * size, s.ownership.size)
                assertEquals(s.best, s.candidates.first().point)
                assertTrue(s.winRate in 0.0..1.0)
                for (c in s.candidates) assertTrue(b.isLegal(c.point))
                assertTrue(b.play(s.best))
            }
        }
    }

    @Test fun playoutLimitIsRespected() {
        val s = BadukEngine(3).search(Board(9), 6.5, 10_000, maxPlayouts = 200)
        assertEquals(200, s.playouts)
    }

    @Test fun speed() {
        val engine = BadukEngine(5)
        engine.search(Board(9), 6.5, 500)
        for (size in intArrayOf(9, 13, 19)) {
            val s = engine.search(Board(size), 6.5, 1000)
            println("baduk engine ${size}x$size: ${s.playouts} playouts/s")
            assertTrue("$size: ${s.playouts}", s.playouts >= (if (size == 9) 3000 else 500))
        }
    }

    @Test fun cancellationReturnsQuickly() {
        val start = System.nanoTime()
        val s = BadukEngine(7).search(Board(19), 6.5, 20_000, isCancelled = { System.nanoTime() - start > 100_000_000 })
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("$ms ms", ms < 600)
        assertTrue(s.best == PASS || Board(19).isLegal(s.best))

        val none = BadukEngine(7).search(Board(9), 6.5, 20_000, isCancelled = { true })
        assertEquals(0, none.playouts)
        assertTrue(Board(9).isLegal(none.best))
    }

    @Test fun takesALargeGroupInAtari() {
        for (seed in 1L..5L) {
            val s = BadukEngine(seed).search(fight.board, 6.5, 2000, maxPlayouts = 400)
            assertEquals("seed $seed", fight['a'], s.best)
            assertTrue(s.winRate > 0.6)
        }
        for (level in 5..10) {
            val player = BadukPlayer(BadukLevels.get(level), level.toLong()).also { it.budget = 0.1 }
            assertEquals("level $level", fight['a'], player.chooseMove(fight.board.copy(), 6.5))
        }
    }

    @Test fun savesItsOwnLargeGroupInAtari() {
        val b = fight.board.copy().also { it.setToMove(Stone.WHITE) }
        for (seed in 1L..5L) assertEquals("seed $seed", fight['a'], BadukEngine(seed).search(b, 6.5, 2000, maxPlayouts = 400).best)
        for (level in 5..10) {
            val player = BadukPlayer(BadukLevels.get(level), level.toLong()).also { it.budget = 0.1 }
            assertEquals("level $level", fight['a'], player.chooseMove(b.copy(), 6.5))
        }
    }

    @Test fun doesNotRunWhenRunningOnlyLosesMore() {
        // The marked stones are in a ladder that works: extending at a only makes the loss bigger.
        val d = Dia(
            ". . . . . . . . .",
            ". . X X . . . . .",
            ". X O a . . . . .",
            ". . X . . . . . .",
            ". . . . . . . . .",
            ". . . . . . . . .",
            ". . . . . . . X .",
            ". . . . O . . . .",
            ". . . . . . . . .",
            toMove = Stone.WHITE,
        )
        val s = BadukEngine(9).search(d.board, 6.5, 5000, maxPlayouts = 6000)
        assertNotEquals(d['a'], s.best)
    }

    @Test fun neverFillsAnEyeOrPlaysSuicide() {
        // Only c is left to play: a and b are White's eyes, d and e are Black's, and Black may not enter a or b.
        val d = Dia(
            "a O . O X d X . X",
            "O O O O X X X X X",
            "b O . O X e X . X",
            "O O O O X X X X X",
            "O . O c X . X . X",
            "O O O O X X X X X",
            "O . O O X . X . X",
            "O O O O X X X X X",
            "O . O O X . X . X",
        )
        for (mover in Stone.entries) for (seed in 1L..4L) {
            val b = d.board.copy().also { it.setToMove(mover) }
            val s = BadukEngine(seed).search(b, 6.5, 2000, maxPlayouts = 300)
            assertTrue("$mover seed $seed: ${s.best}", s.best == PASS || s.best == d['c'])
            for (c in s.candidates) assertEquals(d['c'], c.point)
        }
        for (level in 1..10) for (mover in Stone.entries) {
            val player = BadukPlayer(BadukLevels.get(level), 40L + level).also { it.budget = 0.05 }
            val m = player.chooseMove(d.board.copy().also { it.setToMove(mover) }, 6.5)
            assertTrue("level $level $mover: $m", m == PASS || m == d['c'] || m == RESIGN)
        }
    }

    @Test fun passesWhenTheGameIsFinished() {
        // Whoever is to move, ahead or behind, with or without a pass before: nothing is left to gain.
        for (mover in Stone.entries) for (passed in booleanArrayOf(false, true)) {
            val b = position(SETTLED_ROWS, mover, passed)
            assertEquals(if (passed) 1 else 0, b.passes)
            assertEquals(mover, b.toMove)
            for (seed in 1L..3L) {
                val s = BadukEngine(seed).search(b.copy(), 6.5, 2000, maxPlayouts = 500)
                assertEquals("$mover passed=$passed seed $seed", PASS, s.best)
                assertTrue(s.candidates.isEmpty())
                if (mover == Stone.BLACK) assertTrue("${s.winRate}", s.winRate > 0.7) else assertTrue("${s.winRate}", s.winRate < 0.3)
            }
            for (level in intArrayOf(1, 4, 7, 10)) {
                val player = BadukPlayer(BadukLevels.get(level), 70L + level).also { it.budget = 0.1 }
                val m = player.chooseMove(b.copy(), 6.5)
                // Black is ahead: only a strong White may give up instead of passing.
                assertTrue("level $level $mover passed=$passed: $m", m == PASS || m == RESIGN && mover == Stone.WHITE && level >= 7)
            }
        }
    }

    @Test fun keepsPlayingWhileTheBorderIsOpen() {
        // The opponent passed too early: the border between c and d is still open.
        val d = Dia(
            ". . X . X O . O .",
            ". . X . X O . O .",
            ". . X . X O . O .",
            ". . X . a b . O .",
            "X X X . c d O O O",
            ". . X . e f . O .",
            ". . X . X O . O .",
            ". . X . X O . O .",
            ". . X . X O . O .",
        )
        for (mover in Stone.entries) {
            val b = position(d.board.toRows().toTypedArray(), mover, passed = true)
            val s = BadukEngine(5).search(b, 6.5, 3000, maxPlayouts = 1500)
            assertTrue("$mover: ${s.best}", s.best in "abcdef".map { d[it] })
        }
    }

    @Test fun ownershipOfASettledPosition() {
        val b = Board.fromRows(SETTLED_ROWS.toList())
        val s = BadukEngine(11).search(b, 6.5, 3000, maxPlayouts = 1000)
        for (p in 0 until 81) {
            val black = p % 9 <= 4
            val o = s.ownership[p]
            assertTrue("$p: $o", if (black) o > 0.6f else o < -0.6f)
        }
    }

    @Test fun captureGoTakesTheFirstStone() {
        val d = Dia(
            ". . . . . . . . .",
            ". . . . . . . . .",
            ". . . X . . . . .",
            ". . X O a . . . .",
            ". . . X . . . . .",
            ". . . . . O . . .",
            ". . . . . . . . .",
            ". . . . . . . . .",
            ". . . . . . . . .",
        )
        val take = BadukEngine(13).search(d.board, 6.5, 2000, maxPlayouts = 300, captureGo = true)
        assertEquals(d['a'], take.best)
        assertTrue(take.winRate > 0.9)
        // White to move must run, or it loses on the spot.
        val run = BadukEngine(13).search(d.board.copy().also { it.setToMove(Stone.WHITE) }, 6.5, 2000, maxPlayouts = 300, captureGo = true)
        assertEquals(d['a'], run.best)
    }

    @Test fun wholeGamesAreSane() {
        for ((i, level) in intArrayOf(1, 2, 4, 6, 3).withIndex()) {
            val size = if (i == 3) 13 else if (i == 4) 19 else 9
            var passesBeforeTheEnd = 0
            val r = SelfPlay.play(size, SelfPlay.level(level, 300L + i), SelfPlay.level(level, 400L + i)) { game, m ->
                val b = game.board
                if (m >= 0) {
                    assertTrue("level $level: illegal $m", b.isLegal(m))
                    assertFalse("level $level fills its own eye at $m\n$b", b.isEyeLike(m, b.toMove))
                } else if (m == PASS && b.passes == 0) passesBeforeTheEnd++
            }
            assertTrue("level $level: ${r.how} after ${r.game.moves.size} moves", r.how == "count" || r.how == "resign")
            assertTrue("level $level: only ${r.game.moves.size} moves", r.game.moves.size >= size * size / 2)
            assertTrue("level $level: $passesBeforeTheEnd passes", passesBeforeTheEnd <= 3)
        }
    }
}
