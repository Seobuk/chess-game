package com.seobuk.chess.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RulesTest {
    private fun sq(name: String) = Squares.parse(name)
    private fun mv(uci: String) = Move.fromUci(uci)!!
    private fun san(fen: String, uci: String) = Position.fromFen(fen).san(mv(uci))

    @Test fun squaresRoundTrip() {
        for (s in 0..63) assertEquals(s, Squares.parse(Squares.name(s)))
        assertEquals(0, sq("a1")); assertEquals(63, sq("h8")); assertEquals(28, sq("e4"))
        assertEquals(4, Squares.file(28)); assertEquals(3, Squares.rank(28))
        for (bad in listOf("", "e", "i1", "a9", "a0", "e44")) assertEquals(bad, -1, Squares.parse(bad))
    }

    @Test fun uciRoundTrip() {
        assertEquals(Move(12, 28), Move.fromUci("e2e4"))
        assertEquals(Move(52, 60, PieceType.QUEEN), Move.fromUci("e7e8q"))
        for (u in listOf("e2e4", "g1f3", "e7e8q", "a2a1n", "h7g8r", "b2c1b")) assertEquals(u, mv(u).uci())
        for (bad in listOf("e2e9", "e2e4x", "e7e8k", "e2", "e2e2", "z1a1")) assertNull(bad, Move.fromUci(bad))
        for (from in 0..63) for (to in 0..63) for (pr in listOf(null, PieceType.KNIGHT, PieceType.QUEEN)) {
            val m = Move(from, to, pr)
            assertEquals(m, Move.decode(Move.encode(m)))
        }
    }

    @Test fun fenRoundTrip() {
        val fens = PerftTest.ALL + listOf(
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
            "r3k2r/8/8/8/8/8/8/R3K2R b Kq - 12 40",
            "8/8/8/4k3/8/8/8/4K3 w - - 0 1",
        )
        for (f in fens) assertEquals(f, Position.fromFen(f).fen())
        val p = Position.start()
        p.makeMove(mv("e2e4"))
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", p.fen())
        assertEquals(Position.fromFen(p.fen()).hash, p.hash)
        // Short FEN (4 fields) gets default clocks.
        assertEquals("8/8/8/4k3/8/8/8/4K3 w - - 0 1", Position.fromFen("8/8/8/4k3/8/8/8/4K3 w - -").fen())
    }

    @Test fun badFenThrows() {
        val bad = listOf(
            "", "garbage", "8/8/8/8/8/8/8/8 w - - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBN w KQkq - 0 1",
            "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkX - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e3 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - x 1",
            "4k3/8/8/8/8/8/8/4K2P w - - 0 1",
            "4k3/4Q3/8/8/8/8/8/4K3 w - - 0 1", // side not to move in check
        )
        for (f in bad) {
            try {
                Position.fromFen(f); fail("accepted bad FEN '$f'")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test fun castlingRightsLost() {
        val p = Position.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        p.makeMove(mv("h1h8")) // rook captures rook on its home square
        assertEquals("r3k2R/8/8/8/8/8/8/R3K3 b Qq - 0 1", p.fen())
        p.unmakeMove()
        p.makeMove(mv("e1d1"))
        assertEquals(12, p.castlingRights)
        // Can't castle out of, through or into check.
        Position.fromFen("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1").let { assertTrue(it.isLegal(mv("e1g1")) && it.isLegal(mv("e1c1"))) }
        assertFalse(Position.fromFen("4k3/4r3/8/8/8/8/8/R3K2R w KQ - 0 1").isLegal(mv("e1g1")))
        assertFalse(Position.fromFen("4k3/5r2/8/8/8/8/8/R3K2R w KQ - 0 1").isLegal(mv("e1g1")))
        assertFalse(Position.fromFen("4k3/6r1/8/8/8/8/8/R3K2R w KQ - 0 1").isLegal(mv("e1g1")))
        assertTrue(Position.fromFen("4k3/6r1/8/8/8/8/8/R3K2R w KQ - 0 1").isLegal(mv("e1c1")))
        // b1 attacked does not stop O-O-O, b1 occupied does.
        assertTrue(Position.fromFen("4k3/1r6/8/8/8/8/8/R3K2R w KQ - 0 1").isLegal(mv("e1c1")))
        assertFalse(Position.fromFen("4k3/8/8/8/8/8/8/RN2K2R w KQ - 0 1").isLegal(mv("e1c1")))
    }

    @Test fun enPassantPinnedHorizontally() {
        val p = Position.fromFen("8/8/8/K2pP2r/8/8/8/7k w - d6 0 1")
        assertFalse(p.isLegal(mv("e5d6")))
        val q = Position.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
        assertTrue(q.isLegal(mv("e5d6")))
        assertTrue(q.isCapture(mv("e5d6")))
        q.makeMove(mv("e5d6"))
        assertEquals("4k3/8/3P4/8/8/8/8/4K3 b - - 0 1", q.fen())
        q.unmakeMove()
        assertEquals("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", q.fen())
    }

    @Test fun promotions() {
        val p = Position.fromFen("1n5k/P7/8/8/8/8/8/4K3 w - - 0 1")
        val promos = p.legalMoves().filter { it.from == sq("a7") }
        assertEquals(8, promos.size) // a8 and xb8, four pieces each
        p.makeMove(mv("a7b8n"))
        assertEquals("1N5k/8/8/8/8/8/8/4K3 b - - 0 1", p.fen())
        assertEquals(1, p.pieceCount(PieceType.KNIGHT, Side.WHITE))
        assertEquals(0, p.pieceCount(PieceType.PAWN, Side.WHITE))
        p.unmakeMove()
        assertEquals(1, p.pieceCount(PieceType.PAWN, Side.WHITE))
        assertEquals(1, p.pieceCount(PieceType.KNIGHT, Side.BLACK))
    }

    @Test fun sanGeneration() {
        assertEquals("Nbd7", san("rnbqkb1r/ppp2ppp/4pn2/3p2B1/2PP4/2N5/PP2PPPP/R2QKBNR b KQkq - 1 4", "b8d7"))
        assertEquals("R1a3", san("4k3/8/8/R7/8/8/8/R3K3 w - - 0 1", "a1a3"))
        assertEquals("R5a3", san("4k3/8/8/R7/8/8/8/R3K3 w - - 0 1", "a5a3"))
        val q = "4k3/8/8/8/8/Q7/8/Q1Q1K3 w - - 0 1"
        assertEquals("Qa1b2", san(q, "a1b2"))
        assertEquals("Q3b2", san(q, "a3b2"))
        assertEquals("Qcb2", san(q, "c1b2"))
        assertEquals("Nd4", san("4r1k1/8/8/8/8/8/2N1N3/4K3 w - - 0 1", "c2d4")) // e2 knight is pinned
        assertEquals("exd6", san("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"))
        assertEquals("e8=Q+", san("7k/4P3/8/8/8/8/8/4K3 w - - 0 1", "e7e8q"))
        assertEquals("e8=N", san("7k/4P3/8/8/8/8/8/4K3 w - - 0 1", "e7e8n"))
        assertEquals("O-O", san("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1"))
        assertEquals("O-O-O", san("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1c1"))
        assertEquals("O-O-O+", san("3k4/8/8/8/8/8/8/R3K3 w Q - 0 1", "e1c1"))
        assertEquals("Qh4#", san("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq g3 0 2", "d8h4"))
        assertEquals("Rxa8+", san("r3k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a8"))
        assertEquals("Nf3", san(Position.START_FEN, "g1f3"))
        assertEquals("e4", san(Position.START_FEN, "e2e4"))
    }

    @Test fun parseSanCommonInputs() {
        val p = Position.start()
        assertEquals(mv("g1f3"), p.parseSan("Nf3"))
        assertEquals(mv("g1f3"), p.parseSan("Ngf3"))
        assertEquals(mv("g1f3"), p.parseSan("Nf3!?"))
        assertEquals(mv("e2e4"), p.parseSan("e4"))
        assertNull(p.parseSan("e5"))
        assertNull(p.parseSan("Nf4"))
        assertNull(p.parseSan("O-O"))
        assertNull(p.parseSan("xyz"))
        val d = Position.fromFen("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2")
        assertEquals(mv("e4d5"), d.parseSan("exd5"))
        assertEquals(mv("e4d5"), d.parseSan("exd5+"))
        val pr = Position.fromFen("7k/4P3/8/8/8/8/8/4K3 w - - 0 1")
        assertEquals(mv("e7e8q"), pr.parseSan("e8=Q"))
        assertEquals(mv("e7e8q"), pr.parseSan("e8Q+"))
        assertEquals(mv("e7e8q"), pr.parseSan("e8"))
        assertEquals(mv("e7e8r"), pr.parseSan("e8=R"))
        val c = Position.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertEquals(mv("e1g1"), c.parseSan("O-O"))
        assertEquals(mv("e1g1"), c.parseSan("0-0"))
        assertEquals(mv("e1c1"), c.parseSan("O-O-O+"))
        assertEquals(mv("e1c1"), c.parseSan("0-0-0"))
        val r = Position.fromFen("4k3/8/8/R7/8/8/8/R3K3 w - - 0 1")
        assertNull(r.parseSan("Ra3")) // ambiguous
        assertEquals(mv("a1a3"), r.parseSan("R1a3"))
        val m = Position.fromFen("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq g3 0 2")
        assertEquals(mv("d8h4"), m.parseSan("Qh4#"))
    }

    @Test fun parseSanInvertsSan() {
        for (fen in PerftTest.ALL) {
            val root = Position.fromFen(fen)
            for (m in root.legalMoves()) {
                assertEquals(fen, m, root.parseSan(root.san(m)))
                root.makeMove(m)
                for (m2 in root.legalMoves()) assertEquals(root.fen(), m2, root.parseSan(root.san(m2)))
                root.unmakeMove()
            }
            assertEquals(fen, root.fen())
        }
    }

    @Test fun repetitionAndCopy() {
        val p = Position.start()
        for (u in listOf("g1f3", "g8f6", "f3g1", "f6g8")) p.makeMove(mv(u))
        assertEquals(2, p.repetitionCount())
        val c = p.copy()
        c.makeMove(mv("e2e4"))
        assertEquals(Position.START_FEN.replace(" 0 1", " 4 3"), p.fen()) // original untouched
        c.unmakeMove(); c.unmakeMove() // the copy carries the undo history
        assertEquals("rnbqkb1r/pppppppp/5n2/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 3 2", c.fen())
    }
}
