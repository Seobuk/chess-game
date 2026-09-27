package com.seobuk.chess.baduk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BadukGameTest {
    private data class Snapshot(
        val rows: List<String>, val hash: Long, val ko: Int, val black: Int, val white: Int,
        val passes: Int, val moveNumber: Int, val toMove: Stone,
    )

    private fun snapshot(b: Board) = Snapshot(
        b.toRows(), b.hash, b.koPoint, b.captured(Stone.BLACK), b.captured(Stone.WHITE),
        b.passes, b.moveNumber, b.toMove,
    )

    private fun p(x: Int, y: Int) = Points.of(x, y, 5)

    /**
     * Builds the ko of BoardTest.koSequence on 5x5, Black takes it (move 9), White passes,
     * Black plays away and White takes the ko back.
     */
    private val koGame = intArrayOf(
        p(1, 0), p(2, 0), p(0, 1), p(3, 1), p(1, 2), p(2, 2), p(4, 4), p(1, 1),
        p(2, 1), PASS, p(4, 3), p(1, 1),
    )

    @Test fun undoRestoresEverything() {
        val g = BadukGame(5)
        val history = ArrayList<Snapshot>()
        history.add(snapshot(g.board))
        for (m in koGame) {
            assertTrue(Points.name(m, 5), g.play(m))
            history.add(snapshot(g.board))
        }
        assertEquals(p(1, 1), history[9].ko)
        assertEquals(1, history[9].black)
        assertEquals(1, history[10].passes)
        assertEquals(p(2, 1), history[12].ko)
        assertEquals(1, history[12].white)
        assertEquals(koGame.toList(), g.moves)

        for (n in koGame.size downTo 0) {
            assertEquals(history[n], snapshot(g.board))
            assertEquals(history[n], snapshot(g.boardAt(n)))
            assertEquals(n, g.moves.size)
            assertEquals(n > 0, g.undo())
        }
        assertEquals(Board(5).hash, g.board.hash)

        // The ko ban is back after undoing to the capture.
        for (m in koGame.take(9)) assertTrue(g.play(m))
        assertFalse(g.play(p(1, 1)))
        assertEquals(Illegal.KO, g.board.illegalReason(p(1, 1)))
    }

    @Test fun boardAtChecksItsRange() {
        val g = BadukGame(5)
        g.play(12)
        assertThrows(IllegalArgumentException::class.java) { g.boardAt(2) }
        assertThrows(IllegalArgumentException::class.java) { g.boardAt(-1) }
        val copy = g.boardAt(1)
        copy.play(13)
        assertNull(g.board[13])
    }

    @Test fun twoPassesEndTheGame() {
        val g = BadukGame(9)
        assertEquals(6.5, g.komi, 0.0)
        assertTrue(g.play(40))
        assertTrue(g.pass())
        assertFalse(g.isOver)
        assertTrue(g.play(41)) // a move between the passes: they are not consecutive
        assertTrue(g.pass())
        assertFalse(g.isOver)
        assertTrue(g.pass())
        assertTrue(g.isOver)
        assertEquals(listOf(40, PASS, 41, PASS, PASS), g.moves)
        assertFalse(g.play(42))
        assertFalse(g.pass())
        assertEquals(5, g.moves.size)
        assertNull(g.resigned)
        assertNull(g.captureWinner)

        assertTrue(g.undo())
        assertFalse(g.isOver)
        assertEquals(1, g.board.passes)
        assertTrue(g.play(42))
    }

    @Test fun rejectsIllegalMoves() {
        val g = BadukGame(9)
        assertTrue(g.play(40))
        assertFalse(g.play(40))
        assertFalse(g.play(81))
        assertEquals(listOf(40), g.moves)
        assertEquals(Stone.WHITE, g.board.toMove)
    }

    @Test fun resignation() {
        val g = BadukGame(9)
        g.play(40)
        g.resign(Stone.WHITE)
        assertTrue(g.isOver)
        assertEquals(Stone.WHITE, g.resigned)
        assertFalse(g.play(41))
        g.resign(Stone.BLACK) // too late
        assertEquals(Stone.WHITE, g.resigned)
        assertTrue(g.undo())
        assertNull(g.resigned)
        assertFalse(g.isOver)
        assertEquals(listOf(40), g.moves)
    }

    @Test fun captureGoEndsAtTheFirstCapture() {
        // White's corner stone is taken by Black's third move.
        val moves = intArrayOf(p(1, 0), p(0, 0), p(4, 4), p(3, 3), p(0, 1))
        val g = BadukGame(5, captureGo = true)
        for (m in moves.dropLast(1)) assertTrue(g.play(m))
        assertFalse(g.isOver)
        assertNull(g.captureWinner)
        assertTrue(g.play(moves.last()))
        assertTrue(g.isOver)
        assertEquals(Stone.BLACK, g.captureWinner)
        assertEquals(1, g.board.captured(Stone.BLACK))
        assertFalse(g.play(p(2, 2)))

        assertTrue(g.undo())
        assertFalse(g.isOver)
        assertNull(g.captureWinner)
        assertEquals(Stone.WHITE, g.board[0])

        // The same moves in a normal game go on.
        val normal = BadukGame(5)
        for (m in moves) assertTrue(normal.play(m))
        assertFalse(normal.isOver)
        assertNull(normal.captureWinner)
    }

    @Test fun captureGoWhiteCanWin() {
        val g = BadukGame(5, captureGo = true)
        for (m in intArrayOf(p(0, 0), p(1, 0), p(4, 4), p(0, 1))) assertTrue(g.play(m))
        assertEquals(Stone.WHITE, g.captureWinner)
        assertTrue(g.isOver)
    }

    @Test fun copyIsIndependent() {
        val g = BadukGame(5, komi = 0.5, captureGo = true)
        g.play(12)
        val c = g.copy()
        assertEquals(5, c.size)
        assertEquals(0.5, c.komi, 0.0)
        assertTrue(c.captureGo)
        assertTrue(c.play(13))
        c.resign(Stone.BLACK)
        assertEquals(listOf(12), g.moves)
        assertNull(g.board[13])
        assertNull(g.resigned)
        assertEquals(listOf(12, 13), c.moves)
        assertTrue(c.undo() && c.undo())
        assertEquals(g.board.hash, c.board.hash)
    }
}
