package com.seobuk.chess.baduk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    /**
     * Finished 9x9 game. Black owns the three columns on the left, White the four on the right.
     * W = dead white stone inside Black's area, B = dead black stone inside White's area,
     * n = neutral point (공배), a = Black's last move, which captures the corner stone.
     */
    private val rows = listOf(
        ". . . X O . . . .",
        ". . . X O . . . .",
        ". . . X O . . B .",
        ". . . X O . . . .",
        ". W . X n O . . .",
        ". . . X O . . . .",
        ". . . X O . . . .",
        "a . . X O . . . .",
        "O X . X O . . . .",
    )

    private fun finished(): Board {
        val b = Board.fromRows(rows)
        assertTrue(b.play(Board.labels(rows).getValue('a')))
        assertTrue(b.play(PASS))
        assertTrue(b.play(PASS))
        return b
    }

    private val dead = Board.marked(rows).toSet()
    private val neutral = Board.labels(rows).getValue('n')

    @Test fun territoryScore() {
        val s = Scoring.territory(finished(), dead, 6.5)
        assertEquals(25, s.blackTerritory)   // 27 points minus the two black stones in the corner
        assertEquals(35, s.whiteTerritory)   // 36 points minus the white stone next to the neutral point
        assertEquals(2, s.blackPrisoners)    // one capture + one dead stone
        assertEquals(1, s.whitePrisoners)    // one dead stone
        assertEquals(6.5, s.komi, 0.0)
        assertEquals(27.0, s.black, 0.0)
        assertEquals(42.5, s.white, 0.0)
        assertEquals(Stone.WHITE, s.winner)
        assertEquals(15.5, s.margin, 0.0)

        assertNull(s.owner[neutral])
        assertEquals(Stone.BLACK, s.owner[Points.of(1, 4, 9)])  // under the dead white stone
        assertEquals(Stone.WHITE, s.owner[Points.of(7, 2, 9)])  // under the dead black stone
        assertEquals(Stone.BLACK, s.owner[Points.of(0, 8, 9)])  // where the corner stone was captured
        assertNull(s.owner[Points.of(3, 0, 9)])                 // living stones have no owner
        assertNull(s.owner[Points.of(0, 7, 9)])
        assertEquals(25, s.owner.count { it == Stone.BLACK })
        assertEquals(35, s.owner.count { it == Stone.WHITE })
    }

    @Test fun areaScore() {
        val s = Scoring.area(finished(), dead, 6.5)
        assertEquals(25, s.blackTerritory)
        assertEquals(35, s.whiteTerritory)
        assertEquals(36.0, s.black, 0.0)     // 11 living stones + 25
        assertEquals(50.5, s.white, 0.0)     // 9 living stones + 35 + 6.5
        assertEquals(Stone.WHITE, s.winner)
        assertEquals(14.5, s.margin, 0.0)
        assertEquals(81.0, s.black + s.white - s.komi + 1, 0.0) // + the neutral point = the whole board
        assertNull(s.owner[neutral])
        assertNull(s.owner[Points.of(3, 0, 9)])
    }

    @Test fun unmarkedDeadStonesMakeTheirRegionNeutral() {
        val s = Scoring.territory(finished(), emptySet(), 6.5)
        assertEquals(1, s.blackTerritory)    // only the captured corner point is sealed off
        assertEquals(0, s.whiteTerritory)
        assertEquals(1, s.blackPrisoners)
        assertEquals(0, s.whitePrisoners)
        assertEquals(2.0, s.black, 0.0)
        assertEquals(6.5, s.white, 0.0)
        assertEquals(4.5, s.margin, 0.0)
        assertNull(s.owner[Points.of(0, 0, 9)])
    }

    @Test fun oneDeadSideOnly() {
        val s = Scoring.territory(finished(), setOf(Points.of(1, 4, 9)), 0.5)
        assertEquals(25, s.blackTerritory)
        assertEquals(0, s.whiteTerritory)
        assertEquals(27.0, s.black, 0.0)
        assertEquals(0.5, s.white, 0.0)
        assertEquals(Stone.BLACK, s.winner)
        assertEquals(26.5, s.margin, 0.0)
    }

    @Test fun tieHasNoWinner() {
        val s = Scoring.territory(finished(), dead, -9.0)
        assertEquals(s.black, s.white, 0.0)
        assertNull(s.winner)
        assertEquals(0.0, s.margin, 0.0)
    }

    @Test fun deadPointTakesItsWholeGroup() {
        val b = Board.fromRows(
            listOf(
                ". X O . .",
                ". X O . .",
                ". X O X X",
                ". X O . .",
                ". X O . .",
            ),
        )
        val s = Scoring.territory(b, setOf(Points.of(4, 2, 5)), 0.0)
        assertEquals(5, s.blackTerritory)
        assertEquals(10, s.whiteTerritory)   // 8 empty points + 2 under the dead stones
        assertEquals(2, s.whitePrisoners)
        assertEquals(12.0, s.white, 0.0)
        val a = Scoring.area(b, setOf(Points.of(4, 2, 5)), 0.0)
        assertEquals(10.0, a.black, 0.0)
        assertEquals(15.0, a.white, 0.0)
    }

    @Test fun emptyBoardBelongsToNobody() {
        val s = Scoring.territory(Board(9), emptySet(), 6.5)
        assertEquals(0, s.blackTerritory)
        assertEquals(0, s.whiteTerritory)
        assertEquals(Stone.WHITE, s.winner)
        assertEquals(6.5, s.margin, 0.0)
        assertTrue(s.owner.all { it == null })
        // Empty points listed as dead are ignored.
        assertEquals(0, Scoring.territory(Board(9), setOf(3, 4), 6.5).blackPrisoners)
    }
}
