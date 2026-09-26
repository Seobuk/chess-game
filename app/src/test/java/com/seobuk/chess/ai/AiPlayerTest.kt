package com.seobuk.chess.ai

import com.seobuk.chess.core.Game
import com.seobuk.chess.core.GameStatus
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AiPlayerTest {
    @Test
    fun levelsAreOrdered() {
        assertEquals((1..10).toList(), AiLevels.all.map { it.level })
        assertTrue(AiLevels.all.zipWithNext().all { (a, b) -> a.approxElo < b.approxElo })
        assertEquals(10, AiLevels.get(99).level)
    }

    @Test
    fun everyLevelReturnsLegalMoves() {
        val rnd = Random(7)
        val positions = (0 until 20).map {
            val p = Position.start()
            repeat(4 + rnd.nextInt(50)) {
                val legal = p.legalMoves()
                if (legal.isNotEmpty() && p.halfmoveClock < 90) p.makeMove(legal[rnd.nextInt(legal.size)])
            }
            p
        }.filter { it.legalMoves().isNotEmpty() }
        assertTrue(positions.size >= 15)
        for (level in AiLevels.all) {
            val ai = AiPlayer(level, seed = level.level.toLong()).apply { thinkCapMs = 60 }
            val start = System.nanoTime()
            for (p in positions) {
                val fen = p.fen()
                val m = ai.chooseMove(p, emptyList())
                assertTrue("level ${level.level} played ${m.uci()} in $fen", p.isLegal(m))
                assertEquals(fen, p.fen())
            }
            println("level ${level.level}: ${(System.nanoTime() - start) / 1_000_000 / positions.size} ms/move")
        }
    }

    @Test
    fun strongLevelsFindMateInOne() {
        for (level in 5..10) {
            val ai = AiPlayer(AiLevels.get(level), seed = 1).apply { thinkCapMs = 500 }
            val m = ai.chooseMove(Position.fromFen(EngineTest.BACK_RANK_MATE), emptyList())
            assertEquals("level $level", "a1a8", m.uci())
        }
    }

    @Test
    fun strongLevelsDoNotHangTheQueen() {
        // White queen on d4 is attacked by Nc6 and must move somewhere safe.
        val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/3QP3/8/PPP2PPP/RNB1KBNR w KQkq - 0 5"
        for (level in 6..10) for (seed in 1L..3L) {
            val p = Position.fromFen(fen)
            val ai = AiPlayer(AiLevels.get(level), seed).apply { thinkCapMs = 300 }
            val m = ai.chooseMove(p, emptyList())
            p.makeMove(m)
            val queenTaken = p.legalMoves().any { p.pieceAt(it.to)?.let { x -> x.type == PieceType.QUEEN } == true }
            assertTrue("level $level seed $seed played ${m.uci()}", !queenTaken)
        }
    }

    @Test
    fun level10RespectsTimeBudget() {
        val ai = AiPlayer(AiLevels.get(10), seed = 3)
        val p = Position.fromFen(EngineTest.KIWIPETE)
        val start = System.nanoTime()
        val m = ai.chooseMove(p, emptyList())
        val ms = (System.nanoTime() - start) / 1_000_000
        println("level 10 thought $ms ms")
        assertTrue(p.isLegal(m))
        assertTrue("took $ms ms", ms < 3500)
    }

    @Test
    fun cancellationReturnsQuickly() {
        val ai = AiPlayer(AiLevels.get(10), seed = 3)
        val start = System.nanoTime()
        val m = ai.chooseMove(Position.fromFen(EngineTest.KIWIPETE), emptyList()) { System.nanoTime() - start > 50_000_000 }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue(Position.fromFen(EngineTest.KIWIPETE).isLegal(m))
        assertTrue("took $ms ms", ms < 500)
    }

    @Test
    fun bookMovesFollowTheLibrary() {
        val ai = AiPlayer(AiLevels.get(9), seed = 5)
        val g = Game()
        repeat(4) { g.play(ai.chooseMove(g.position, g.moves.map { it.uci() })) }
        assertEquals(4, g.moves.size)
    }

    @Test
    fun strongerLevelBeatsWeakerOne() {
        var strongWins = 0
        for (game in 0 until 3) {
            val strong = AiPlayer(AiLevels.get(8), seed = 100L + game).apply { thinkCapMs = 100 }
            val weak = AiPlayer(AiLevels.get(2), seed = 200L + game)
            val strongSide = if (game % 2 == 0) Side.WHITE else Side.BLACK
            val g = Game()
            while (!g.isOver && g.moves.size < 250) {
                val ai = if (g.position.sideToMove == strongSide) strong else weak
                assertTrue(g.play(ai.chooseMove(g.position, g.moves.map { it.uci() })))
            }
            println("game $game: ${g.status} winner ${g.winner} after ${g.moves.size} plies (strong = $strongSide)")
            if (g.status == GameStatus.CHECKMATE && g.winner == strongSide) strongWins++
        }
        assertTrue("strong side won $strongWins of 3", strongWins >= 2)
    }
}
