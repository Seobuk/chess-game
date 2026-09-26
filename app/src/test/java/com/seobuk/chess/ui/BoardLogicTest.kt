package com.seobuk.chess.ui

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Squares
import com.seobuk.chess.learn.OpeningBook
import com.seobuk.chess.ui.board.changeBetween
import com.seobuk.chess.ui.board.squareLabel
import com.seobuk.chess.ui.components.figurine
import com.seobuk.chess.ui.game.practiceGame
import com.seobuk.chess.ui.screens.SideChoice
import com.seobuk.chess.ui.screens.withGwa
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLogicTest {
    private fun sq(n: String) = Squares.parse(n)
    private fun after(fen: String, uci: String) = Position.fromFen(fen).apply { makeMove(Move.fromUci(uci)!!) }

    @Test
    fun castlingSlidesKingAndRook() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        val c = changeBetween(Position.fromFen(fen), after(fen, "e1c1"), Move.fromUci("e1c1"))
        assertEquals(listOf(sq("e1") to sq("c1"), sq("a1") to sq("d1")), c.movers.map { it.from to it.to })
        assertNull(c.captured)
    }

    @Test
    fun enPassantFadesThePawnBesideTheTarget() {
        val fen = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
        val c = changeBetween(Position.fromFen(fen), after(fen, "e5d6"), Move.fromUci("e5d6"))
        assertEquals(sq("d5"), c.captured?.to)
    }

    @Test
    fun anythingElseCrossfades() {
        val start = Position.start()
        val moved = after(Position.START_FEN, "e2e4")
        // undo: going back to the start with no matching last move
        val undo = changeBetween(moved, start, null)
        assertNotNull(undo.old)
        assertTrue(undo.movers.isEmpty())
        // a last move that does not explain the change also crossfades
        assertTrue(changeBetween(start, moved, Move.fromUci("d2d4")).movers.isEmpty())
        // first composition: no animation at all
        assertNull(changeBetween(null, start, null).old)
    }

    @Test
    fun figurineAndParticles() {
        assertEquals("♞︎f3", figurine("Nf3"))
        assertEquals("exd8=♛︎+", figurine("exd8=Q+"))
        assertEquals("O-O-O", figurine("O-O-O"))
        assertEquals("b4", figurine("b4"))
        assertEquals("여우와", withGwa("여우"))
        assertEquals("드래곤과", withGwa("드래곤"))
    }

    @Test
    fun practiceNeverStartsAtMateInOne() {
        for (o in OpeningBook.all) {
            val g = practiceGame(o.steps.map { it.uci })
            assertTrue(o.id, !g.isOver)
            val p = g.position
            val mates = p.legalMoves().filter { m -> p.copy().apply { makeMove(m) }.let { it.isInCheck() && !it.hasLegalMove() } }
            assertTrue("${o.id}: ${mates.map { it.uci() }}", mates.isEmpty())
        }
        // Trap lines stop at the victim's real choice: before 2.g4, before 3...Nf6, before 5...Bxd1.
        assertEquals(2, practiceGame(OpeningBook.byId("fools_mate")!!.steps.map { it.uci }).moves.size)
        assertEquals(5, practiceGame(OpeningBook.byId("scholars_mate")!!.steps.map { it.uci }).moves.size)
        assertEquals("f3e5", practiceGame(OpeningBook.byId("legal_trap")!!.steps.map { it.uci }).moves.last().uci())
    }

    @Test
    fun backStackSurvivesSerialization() {
        val stack = arrayListOf(
            Entry(0, Screen.Home),
            Entry(3, Screen.LevelSelect("italian")),
            Entry(4, Screen.Game(5, SideChoice.BLACK, listOf("e2e4", "e7e5"), resumeUci = listOf("g1f3"))),
        )
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(stack) }.toByteArray()
        @Suppress("UNCHECKED_CAST")
        val back = ObjectInputStream(ByteArrayInputStream(bytes)).readObject() as ArrayList<Entry>
        assertEquals(stack.map { it.id to it.screen }, back.map { it.id to it.screen })
    }

    @Test
    fun squareLabelsForTalkBack() {
        assertEquals("e1 백 킹", squareLabel(Position.start(), sq("e1"), false))
        assertEquals("e4 빈 칸, 이동 가능", squareLabel(Position.start(), sq("e4"), true))
    }
}
