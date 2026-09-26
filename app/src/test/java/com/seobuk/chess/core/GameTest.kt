package com.seobuk.chess.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    private fun Game.playAll(vararg uci: String) = uci.forEach { assertTrue(it, play(Move.fromUci(it)!!)) }

    @Test fun foolsMate() {
        val g = Game()
        g.playAll("f2f3", "e7e5", "g2g4", "d8h4")
        assertEquals(GameStatus.CHECKMATE, g.status)
        assertEquals(Side.BLACK, g.winner)
        assertTrue(g.isOver)
        assertEquals(listOf("f3", "e5", "g4", "Qh4#"), g.sanMoves)
        assertFalse(g.play(Move.fromUci("a2a3")!!))
    }

    @Test fun undoRestoresState() {
        val g = Game()
        g.playAll("f2f3", "e7e5", "g2g4", "d8h4")
        assertTrue(g.undo())
        assertEquals(GameStatus.ONGOING, g.status)
        assertNull(g.winner)
        repeat(3) { assertTrue(g.undo()) }
        assertFalse(g.undo())
        assertEquals(Position.START_FEN, g.position.fen())
        assertEquals(Position.start().hash, g.position.hash)
        assertTrue(g.moves.isEmpty() && g.sanMoves.isEmpty())
    }

    @Test fun rejectsIllegal() {
        val g = Game()
        assertFalse(g.play(Move.fromUci("e2e5")!!))
        assertFalse(g.play(Move.fromUci("e7e5")!!))
        assertFalse(g.play(Move(99, 3)))
        assertTrue(g.moves.isEmpty())
    }

    @Test fun stalemate() {
        val g = Game("7k/8/6K1/5Q2/8/8/8/8 w - - 0 1")
        g.playAll("f5f7")
        assertEquals(GameStatus.STALEMATE, g.status)
        assertNull(g.winner)
        assertEquals(GameStatus.STALEMATE, Game("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1").status)
    }

    @Test fun threefoldByKnightShuffle() {
        val g = Game()
        g.playAll("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1")
        assertEquals(GameStatus.ONGOING, g.status)
        g.playAll("f6g8")
        assertEquals(GameStatus.DRAW_THREEFOLD, g.status)
        assertNull(g.winner)
    }

    @Test fun fiftyMoveRule() {
        val g = Game("4k3/8/8/8/8/8/8/R3K3 w - - 99 80")
        assertEquals(GameStatus.ONGOING, g.status)
        g.playAll("a1a2")
        assertEquals(GameStatus.DRAW_FIFTY_MOVE, g.status)
        // Mate on the 100th half-move is still mate.
        val m = Game("6k1/5ppp/8/8/8/8/8/R3K3 w - - 99 80")
        m.playAll("a1a8")
        assertEquals(GameStatus.CHECKMATE, m.status)
        assertEquals(Side.WHITE, m.winner)
    }

    @Test fun insufficientMaterial() {
        for (f in listOf(
            "8/8/8/4k3/8/8/8/4K3 w - - 0 1",
            "8/8/8/4k3/8/8/8/2B1K3 w - - 0 1",
            "8/8/8/4k3/8/8/8/1N2K3 b - - 0 1",
            "5b2/8/8/4k3/8/8/8/2B1K3 w - - 0 1", // c1 and f8: both dark
        )) assertEquals(f, GameStatus.DRAW_INSUFFICIENT_MATERIAL, Game(f).status)
        for (f in listOf(
            "2b5/8/8/4k3/8/8/8/2B1K3 w - - 0 1", // opposite colours
            "8/8/8/4k3/8/8/8/1NN1K3 w - - 0 1",
            "8/8/8/4k3/8/8/4P3/4K3 w - - 0 1",
        )) assertEquals(f, GameStatus.ONGOING, Game(f).status)
        val g = Game("4k3/8/8/8/8/8/3r4/4K3 w - - 0 1")
        g.playAll("e1d2")
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, g.status)
    }

    @Test fun positionAtAndCopy() {
        val g = Game()
        g.playAll("e2e4", "e7e5", "g1f3")
        assertEquals(Position.START_FEN, g.positionAt(0).fen())
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", g.positionAt(1).fen())
        assertEquals(g.position.fen(), g.positionAt(3).fen())
        val c = g.copy()
        c.playAll("b8c6")
        assertEquals(3, g.moves.size)
        assertEquals(4, c.moves.size)
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), c.sanMoves)
        assertEquals(Game().apply { playAll("e2e4", "e7e5", "g1f3") }.position.fen(), g.position.fen())
    }
}
