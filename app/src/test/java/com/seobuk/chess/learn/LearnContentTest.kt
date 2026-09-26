package com.seobuk.chess.learn

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Squares
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LearnContentTest {
    @Test
    fun openingIdsAreUniqueAndFieldsSane() {
        val all = OpeningBook.all
        assertTrue(all.size >= 10)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (o in all) {
            assertTrue(o.id, o.keyPlies in 1..o.steps.size)
            assertTrue(o.id, o.difficulty in 1..3)
            assertTrue(o.id, o.nameKo.isNotBlank() && o.summary.isNotBlank() && o.emoji.isNotBlank())
            assertTrue(o.id, o.steps.all { it.comment.isNotBlank() })
            assertEquals(o, OpeningBook.byId(o.id))
        }
    }

    @Test
    fun openingLinesAreLegal() {
        for (o in OpeningBook.all) {
            val p = Position.start()
            for ((i, step) in o.steps.withIndex()) {
                val m = Move.fromUci(step.uci)
                if (m == null || !p.isLegal(m)) fail("${o.id}: ply ${i + 1} '${step.uci}' is illegal in ${p.fen()}")
                p.makeMove(m!!)
            }
        }
    }

    @Test
    fun detectFindsEachOpeningOnItsOwnLine() {
        for (o in OpeningBook.all) {
            val line = o.steps.map { it.uci }
            val d = OpeningBook.detect(line)
            assertTrue("${o.id} -> ${d?.id}", d != null && d.keyPlies >= o.keyPlies)
            // A different result must be a more specific opening that shares o's identifying moves.
            if (d != o) assertEquals(o.id, line.take(o.keyPlies), d!!.steps.take(o.keyPlies).map { it.uci })
        }
        assertEquals(null, OpeningBook.detect(emptyList()))
    }

    @Test
    fun continuationsAreLegal() {
        for (o in OpeningBook.all) {
            val played = o.steps.take(o.keyPlies - 1).map { it.uci }
            val p = Position.start().apply { played.forEach { makeMove(Move.fromUci(it)!!) } }
            val next = OpeningBook.continuations(played)
            assertTrue(o.id, o.steps[played.size].uci in next)
            assertTrue(o.id, next.all { p.isLegal(Move.fromUci(it)!!) })
        }
    }

    @Test
    fun strategyTopicsAreValid() {
        val topics = StrategyGuide.topics
        assertEquals(topics.size, topics.map { it.id }.toSet().size)
        for (t in topics) {
            assertTrue(t.id, t.category in StrategyGuide.categories)
            assertTrue(t.id, t.minLevel in 1..10)
            assertTrue(t.id, t.title.isNotBlank() && t.points.isNotEmpty())
            assertEquals(t, StrategyGuide.byId(t.id))
            (t.highlights + t.arrows.flatMap { listOf(it.from, it.to) }).forEach {
                assertTrue("${t.id}: bad square '$it'", Squares.parse(it) >= 0)
            }
            val fen = t.exampleFen ?: continue
            // fromFen rejects wrong king counts and a side-not-to-move in check.
            val p = try {
                Position.fromFen(fen)
            } catch (e: IllegalArgumentException) {
                fail("${t.id}: ${e.message}"); return
            }
            for (side in listOf(com.seobuk.chess.core.Side.WHITE, com.seobuk.chess.core.Side.BLACK)) {
                assertEquals(t.id, 1, p.pieceCount(PieceType.KING, side))
            }
            assertTrue(t.id, !p.isSquareAttacked(p.kingSquare(p.sideToMove.opposite), p.sideToMove))
            assertTrue(t.id, t.exampleCaption != null)
        }
    }

    /** Copy rules: plain sentences, no cheer fragments ("캐슬링!"), no em or en dashes. "!" after SAN (Nxf7!) is notation. */
    @Test
    fun copyHasNoExclamationsOrDashes() {
        val texts = OpeningBook.all.flatMap { o -> listOf(o.nameKo, o.summary, o.category) + o.ideasWhite + o.ideasBlack + o.steps.map { it.comment } } +
            StrategyGuide.topics.flatMap { t -> listOf(t.title, t.summary, t.category, t.exampleCaption.orEmpty()) + t.points }
        val bad = Regex("[가-힣)]!|[–—]")
        texts.filter { bad.containsMatchIn(it) }.let { assertTrue(it.joinToString("\n"), it.isEmpty()) }
    }
}
