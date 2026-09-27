package com.seobuk.chess.ai

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachTest {
    private fun hangul(s: String) = s.any { it in '가'..'힣' }

    @Test
    fun queenHangIsBlunder() {
        // 1.e4 d5 2.Qg4?? Bxg4
        val p = Position.fromFen("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2")
        val fb = Coach.review(p, Move.fromUci("d1g4")!!, listOf("e2e4", "d7d5"))
        println(fb)
        assertEquals(MoveQuality.BLUNDER, fb.quality)
        assertTrue(fb.cpLoss > 300)
        assertTrue(fb.explanation, fb.explanation.contains("퀸"))
        assertNotNull(fb.bestSan)
    }

    @Test
    fun winningTheQueenIsBest() {
        // 1.e4 e5 2.Nf3 Qh4?? 3.Nxh4
        val p = Position.fromFen("rnb1kbnr/pppp1ppp/8/4p3/4P2q/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")
        val fb = Coach.review(p, Move.fromUci("f3h4")!!, listOf("e2e4", "e7e5", "g1f3", "d8h4"))
        println(fb)
        assertEquals(MoveQuality.BEST, fb.quality)
        assertEquals("Nxh4", fb.san)
        assertTrue(fb.evalAfterCp > 500)
        assertTrue(hangul(fb.explanation))
    }

    @Test
    fun missingMateInOneIsFlagged() {
        val p = Position.fromFen(EngineTest.BACK_RANK_MATE)
        val fb = Coach.review(p, Move.fromUci("a1b1")!!, emptyList())
        println(fb)
        assertTrue(fb.quality == MoveQuality.MISTAKE || fb.quality == MoveQuality.BLUNDER)
        assertTrue(fb.explanation, fb.explanation.contains("Ra8#"))
    }

    @Test
    fun allowingMateIsBlunder() {
        // 1.f3 e5 2.g4?? Qh4#
        val p = Position.fromFen("rnbqkbnr/pppp1ppp/8/4p3/8/5P2/PPPPP1PP/RNBQKBNR w KQkq e6 0 2")
        val fb = Coach.review(p, Move.fromUci("g2g4")!!, listOf("f2f3", "e7e5"))
        println(fb)
        assertEquals(MoveQuality.BLUNDER, fb.quality)
        assertTrue(fb.explanation, fb.explanation.contains("Qh4#"))
    }

    @Test
    fun lossIsNotSaturatedInLopsidedPositions() {
        // At +18, Ra2?? hangs the rook to ...Bxa2: still winning, but a real blunder.
        val won = Coach.review(Position.fromFen("6k1/5pp1/4b2p/8/8/8/5PPP/R2Q1RK1 w - - 0 1"), Move.fromUci("a1a2")!!, emptyList())
        println(won)
        assertTrue("$won", won.quality >= MoveQuality.MISTAKE && won.cpLoss > 300)
        // Lost anyway, but Kh8?? walks into mate in one instead of lasting longer.
        val lost = Coach.review(Position.fromFen("6k1/5ppp/8/8/8/8/8/RQ4K1 b - - 0 1"), Move.fromUci("g8h8")!!, emptyList())
        println(lost)
        assertTrue("$lost", lost.quality > MoveQuality.GOOD && lost.cpLoss > 0)
    }

    @Test
    fun noFakeForks() {
        // The king can take neither the defended rook nor the defended knight.
        val king = Coach.describeMove(Position.fromFen("4k3/2p3p1/3r1n2/8/4K3/8/8/8 w - - 0 1"), Move.fromUci("e4e5")!!)
        // ...exd5 simply wins the knight.
        val hanging = Coach.describeMove(Position.fromFen("4k3/2q1r3/4p3/8/8/2N5/8/4K3 w - - 0 1"), Move.fromUci("c3d5")!!)
        for (t in listOf(king, hanging)) assertTrue(t, hangul(t) && "포크" !in t && "공격해요" !in t)
        // A real one: Nc7+ hits king and rook.
        val fork = Coach.describeMove(Position.fromFen("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1"), Move.fromUci("b5c7")!!)
        assertTrue(fork, "포크" in fork)
    }

    @Test
    fun bookMovesAreBook() {
        val fb = Coach.review(Position.start(), Move.fromUci("e2e4")!!, emptyList())
        println(fb)
        assertEquals(MoveQuality.BOOK, fb.quality)
        val p = Position.start().apply { listOf("e2e4", "e7e5", "g1f3", "b8c6").forEach { makeMove(Move.fromUci(it)!!) } }
        val italian = Coach.review(p, Move.fromUci("f1c4")!!, listOf("e2e4", "e7e5", "g1f3", "b8c6"))
        println(italian)
        assertEquals(MoveQuality.BOOK, italian.quality)
        assertTrue(italian.explanation.contains("이탈리안"))
    }

    @Test
    fun describesCapturesCastlingChecksAndDevelopment() {
        val cases = mapOf(
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2" to "e4d5",
            "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 1 5" to "e1g1",
            "4k3/8/8/8/8/8/8/R3K3 w - - 0 1" to "a1a8",
            Position.START_FEN to "g1f3",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2" to "d1h5",
        )
        val expected = listOf("잡", "캐슬링", "체크", "전개", "")
        for ((i, e) in cases.entries.withIndex()) {
            val text = Coach.describeMove(Position.fromFen(e.key), Move.fromUci(e.value)!!)
            println("${e.value}: $text")
            assertTrue(text, hangul(text) && text.contains(expected[i]))
        }
        val mate = Coach.describeMove(Position.fromFen(EngineTest.BACK_RANK_MATE), Move.fromUci("a1a8")!!)
        assertTrue(mate, mate.contains("체크메이트"))
    }

    @Test
    fun hintSuggestsALegalMove() {
        val p = Position.fromFen(EngineTest.KIWIPETE)
        val h = Coach.hint(p, thinkMs = 400)!!
        println(h)
        assertTrue(p.isLegal(h.move))
        assertTrue(hangul(h.explanation))
        val mate = Coach.hint(Position.fromFen(EngineTest.BACK_RANK_MATE), thinkMs = 300)!!
        assertEquals("Ra8#", mate.san)
        assertTrue(mate.evalCp > MATE - 10)
    }

    @Test
    fun evaluateWhiteSigns() {
        assertTrue(Coach.evaluateWhite(Position.fromFen("4k3/8/8/8/8/8/8/QQ2K3 b - - 0 1"), 200) > 1000)
        assertTrue(Coach.evaluateWhite(Position.fromFen("qq2k3/8/8/8/8/8/8/4K3 w - - 0 1"), 200) < -1000)
        assertEquals(0, Coach.evaluateWhite(Position.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"), 100)) // stalemate
    }

    @Test
    fun accuracySanity() {
        assertEquals(100, Coach.accuracy(emptyList()))
        assertEquals(100, Coach.accuracy(listOf(0, 0, 0)))
        val good = Coach.accuracy(listOf(10, 20, 0, 30))
        val bad = Coach.accuracy(listOf(300, 150, 500, 80))
        assertTrue("$good vs $bad", good > 85 && bad < 60 && good > bad)
        assertTrue(Coach.accuracy(listOf(5000)) in 0..20)
    }

    @Test
    fun koreanParticles() {
        assertEquals("나이트를", Ko.eulReul("나이트"))
        assertEquals("폰을", Ko.eulReul("폰"))
        assertEquals("Nf3으로", Ko.ro("Nf3"))
        assertEquals("Ra8#로", Ko.ro("Ra8#"))
        assertEquals("퀸과", Ko.wa("퀸"))
        assertEquals("비숍이", Ko.iGa("비숍"))
        assertEquals("v0.2.0이", Ko.iGa("v0.2.0")) // the update card: 영 takes 이
        assertEquals("v1.0.2가", Ko.iGa("v1.0.2"))
    }
}
