package com.seobuk.chess.ui.sound

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Position
import org.junit.Assert.assertEquals
import org.junit.Test

class SfxTest {
    private fun cue(fen: String, uci: String): SfxEvent {
        val before = Position.fromFen(fen)
        val move = Move.fromUci(uci)!!
        val after = before.copy().apply { makeMove(move) }
        return sfxFor(before, move, after)
    }

    private val castling = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"

    @Test fun plainMove() {
        assertEquals(SfxEvent.MOVE, cue(Position.START_FEN, "e2e4"))
        assertEquals(SfxEvent.MOVE, cue(Position.START_FEN, "g1f3"))
        assertEquals(SfxEvent.MOVE, cue(castling, "e1f1")) // king step, not castling
        assertEquals(SfxEvent.MOVE, cue(castling, "a1c1")) // rook two files, not castling
    }

    @Test fun capture() {
        assertEquals(SfxEvent.CAPTURE, cue("rnbqkbnr/pppp1ppp/8/4p3/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2", "e5d4"))
    }

    @Test fun enPassantIsACapture() {
        assertEquals(SfxEvent.CAPTURE, cue("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"))
    }

    @Test fun castlingBothSides() {
        assertEquals(SfxEvent.CASTLE, cue(castling, "e1g1"))
        assertEquals(SfxEvent.CASTLE, cue(castling, "e1c1"))
        val black = castling.replace(" w ", " b ")
        assertEquals(SfxEvent.CASTLE, cue(black, "e8g8"))
        assertEquals(SfxEvent.CASTLE, cue(black, "e8c8"))
    }

    @Test fun promotion() {
        val fen = "8/P7/6k1/8/8/8/8/4K3 w - - 0 1"
        assertEquals(SfxEvent.PROMOTE, cue(fen, "a7a8q"))
        assertEquals(SfxEvent.PROMOTE, cue(fen, "a7a8n"))
        assertEquals(SfxEvent.PROMOTE, cue("1n6/P7/6k1/8/8/8/8/4K3 w - - 0 1", "a7b8r")) // capturing promotion
    }

    @Test fun checkWinsOverEverything() {
        assertEquals(SfxEvent.CHECK, cue("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1", "a1a8"))          // plain move giving check
        assertEquals(SfxEvent.CHECK, cue("4k3/4p3/8/8/8/8/8/4RK2 w - - 0 1", "e1e7"))         // capture with check
        assertEquals(SfxEvent.CHECK, cue("4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8q"))          // promotion with check
        assertEquals(SfxEvent.CHECK, cue("3k4/8/8/8/8/8/8/R3K3 w Q - 0 1", "e1c1"))           // castling with check
    }
}
