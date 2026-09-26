package com.seobuk.chess.ai

import com.seobuk.chess.core.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    @Test
    fun findsMateInOne() {
        val r = Engine().search(Position.fromFen(BACK_RANK_MATE), timeLimitMs = 2000)
        assertEquals("a1a8", r.bestMove!!.uci())
        assertEquals(MATE - 1, r.scoreCp)
        assertEquals(1, mateInMoves(r.scoreCp))
    }

    @Test
    fun findsMateInTwo() {
        // 1.Ra7 (or Rb7) Kg8 2.R(b/a)8#
        val p = Position.fromFen("7k/8/8/8/8/8/R7/1R4K1 w - - 0 1")
        val r = Engine().search(p, maxDepth = 4, timeLimitMs = 10_000)
        assertEquals(MATE - 3, r.scoreCp)
        // Every reply to the chosen move must allow a mate in one.
        val after = p.copy().also { it.makeMove(r.bestMove!!) }
        for (reply in after.legalMoves()) {
            val q = after.copy().also { it.makeMove(reply) }
            val mates = q.legalMoves().any { m ->
                val x = q.copy().also { it.makeMove(m) }
                x.isInCheck() && !x.hasLegalMove()
            }
            assertTrue("no mate after ${reply.uci()}", mates)
        }
    }

    @Test
    fun mateOnTheHundredthPlyBeatsTheFiftyMoveRule() {
        val p = Position.fromFen("7k/8/6K1/8/8/8/8/1Q6 w - - 99 120") // Qb8# is quiet: it lands on halfmove 100
        val r = Engine().search(p, timeLimitMs = 500)
        val after = p.copy().also { it.makeMove(r.bestMove!!) }
        assertTrue(r.bestMove!!.uci(), after.isInCheck() && !after.hasLegalMove())
        assertEquals(MATE - 1, r.scoreCp)
    }

    @Test
    fun searchLeavesPositionUntouched() {
        val p = Position.fromFen(KIWIPETE)
        val fen = p.fen()
        val hash = p.hash
        Engine().search(p, timeLimitMs = 200)
        assertEquals(fen, p.fen())
        assertEquals(hash, p.hash)
    }

    @Test
    fun evaluationIsColourSymmetric() {
        val e = Engine()
        for (fen in listOf(Position.START_FEN, KIWIPETE, "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")) {
            assertEquals(fen, e.evaluate(Position.fromFen(fen)), e.evaluate(Position.fromFen(mirror(fen))))
        }
        assertEquals(0, e.evaluate(Position.start()))
    }

    @Test
    fun cancellationStopsQuickly() {
        val start = System.nanoTime()
        val r = Engine().search(Position.fromFen(KIWIPETE), timeLimitMs = 60_000) {
            System.nanoTime() - start > 100_000_000
        }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $ms ms", ms < 600)
        assertNotNull(r.bestMove)
        assertTrue(Position.fromFen(KIWIPETE).isLegal(r.bestMove!!))
    }

    @Test
    fun nodesPerSecond() {
        val e = Engine()
        e.search(Position.fromFen(KIWIPETE), timeLimitMs = 500) // warm-up
        var nodes = 0L
        val start = System.nanoTime()
        for (fen in listOf(Position.START_FEN, KIWIPETE, "r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 8")) {
            val r = e.search(Position.fromFen(fen), timeLimitMs = 1000)
            println("depth ${r.depth} score ${r.scoreCp} pv ${r.pv.joinToString(" ") { it.uci() }} ($fen)")
            nodes += r.nodes
        }
        val sec = (System.nanoTime() - start) / 1e9
        val nps = (nodes / sec).toLong()
        println("ENGINE NPS: $nps ($nodes nodes in ${"%.2f".format(sec)} s)")
        assertTrue("nps $nps", nps > 150_000)
    }

    companion object {
        const val BACK_RANK_MATE = "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1"
        const val KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"

        /** Colour-flipped FEN (ranks mirrored, colours swapped). */
        fun mirror(fen: String): String {
            val f = fen.split(" ")
            val board = f[0].split("/").reversed().joinToString("/") { row ->
                row.map { if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar() }.joinToString("")
            }
            val side = if (f[1] == "w") "b" else "w"
            val castle = if (f[2] == "-") "-" else f[2].map {
                if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar()
            }.sortedBy { "KQkq".indexOf(it) }.joinToString("")
            val ep = if (f[3] == "-") "-" else "${f[3][0]}${9 - (f[3][1] - '0')}"
            return listOf(board, side, castle, ep, f[4], f[5]).joinToString(" ")
        }
    }
}
