package com.seobuk.chess.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PerftTest {
    companion object {
        // Real Kiwipete (the task brief had a typo "4P3" on rank 5).
        const val KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        const val POS3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
        const val POS4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
        const val POS5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        val ALL = listOf(Position.START_FEN, KIWIPETE, POS3, POS4, POS5)
    }

    /** Contract-API perft; asserts hash after every unmake and FEN after unmakes at depth >= fenDepth. */
    private fun perft(p: Position, depth: Int, fenDepth: Int): Long {
        if (depth == 0) return 1
        val hash = p.hash
        val fen = if (depth >= fenDepth) p.fen() else null
        var nodes = 0L
        for (m in p.legalMoves()) {
            p.makeMove(m)
            nodes += perft(p, depth - 1, fenDepth)
            p.unmakeMove()
            if (p.hash != hash) fail("hash not restored after ${m.uci()} in $fen")
            if (fen != null && p.fen() != fen) fail("FEN not restored after ${m.uci()}: $fen vs ${p.fen()}")
        }
        return nodes
    }

    /** Allocation-free fast path (generateMoves/tryMakeMove), as the engine uses it. */
    private fun perftFast(p: Position, depth: Int, bufs: Array<IntArray>): Long {
        val buf = bufs[depth]
        val n = p.generateMoves(buf)
        var nodes = 0L
        for (i in 0 until n) {
            if (!p.tryMakeMove(buf[i])) continue
            nodes += if (depth == 1) 1 else perftFast(p, depth - 1, bufs)
            p.unmakeMove()
        }
        return nodes
    }

    private fun check(fen: String, vararg expected: Long, fenDepth: Int = 2) {
        val p = Position.fromFen(fen)
        expected.forEachIndexed { i, n -> assertEquals("perft(${i + 1}) $fen", n, perft(p, i + 1, fenDepth)) }
        assertEquals(fen, p.fen())
    }

    @Test fun startPosition() {
        val t = System.nanoTime()
        check(Position.START_FEN, 20, 400, 8902, 197281, 4865609)
        println("perft start d1..d5 contract API: ${(System.nanoTime() - t) / 1_000_000} ms")
        val p = Position.start()
        val bufs = Array(7) { IntArray(256) }
        perftFast(p, 5, bufs) // warm-up
        val t2 = System.nanoTime()
        assertEquals(4865609L, perftFast(p, 5, bufs))
        println("perft(5) start fast path: ${(System.nanoTime() - t2) / 1_000_000} ms")
    }

    @Test fun kiwipete() {
        check(KIWIPETE, 48, 2039, 97862)
        val t = System.nanoTime()
        assertEquals(4085603L, perftFast(Position.fromFen(KIWIPETE), 4, Array(6) { IntArray(256) }))
        println("perft(4) kiwipete fast path: ${(System.nanoTime() - t) / 1_000_000} ms")
    }

    @Test fun position3() = check(POS3, 14, 191, 2812, 43238, 674624)

    @Test fun position4() = check(POS4, 6, 264, 9467)

    @Test fun position5() = check(POS5, 44, 1486, 62379)

    @Test fun incrementalHashMatchesScratch() {
        fun walk(p: Position, depth: Int) {
            assertEquals(p.fen(), p.computeHash(), p.hash)
            assertEquals(p.fen(), Position.fromFen(p.fen()).hash, p.hash)
            if (depth == 0) return
            for (m in p.legalMoves()) {
                p.makeMove(m); walk(p, depth - 1); p.unmakeMove()
            }
        }
        for (fen in ALL) walk(Position.fromFen(fen), 3)
    }

    @Test fun nullMoveRestores() {
        val p = Position.fromFen("rnbqkbnr/ppp1pppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 3")
        val fen = p.fen()
        val hash = p.hash
        p.makeNullMove()
        assertEquals(Side.WHITE, p.sideToMove)
        assertEquals(-1, p.enPassantSquare)
        assertEquals(p.computeHash(), p.hash)
        p.unmakeNullMove()
        assertEquals(fen, p.fen())
        assertEquals(hash, p.hash)
    }

    @Test fun tacticalGeneration() {
        val p = Position.fromFen(KIWIPETE)
        val buf = IntArray(256)
        val n = p.generateMoves(buf, tacticalOnly = true)
        val codes = (0 until n).map { buf[it] }
        for (c in codes) {
            val m = Move.decode(c)
            assertTrue(m.uci(), p.isCapture(m) || m.promotion == PieceType.QUEEN)
        }
        val legalCaptures = p.legalMoves().count { p.isCapture(it) }
        assertEquals(legalCaptures, codes.count { p.tryMakeMove(it).also { ok -> if (ok) p.unmakeMove() } })
    }
}
