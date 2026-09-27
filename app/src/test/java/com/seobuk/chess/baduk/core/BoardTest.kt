package com.seobuk.chess.baduk.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** A diagram plus its labels: `d['a']` is the point labelled a. */
class Dia(vararg rows: String, toMove: Stone = Stone.BLACK) {
    private val labels = Board.labels(rows.toList())
    val board: Board = Board.fromRows(rows.toList(), toMove)
    operator fun get(label: Char): Int = labels.getValue(label)
}

fun Board.sortedGroup(p: Int): List<Int> = group(p).sorted()

class BoardTest {
    @Test fun libertiesCentreEdgeCorner() {
        val d = Dia(
            "X a . . .",
            "b . c . .",
            "X d X e .",
            "f . g . .",
            ". . . . .",
        )
        val b = d.board
        assertEquals(2, b.liberties(0))
        assertEquals(listOf(d['a'], d['b']), b.libertyPoints(0).sorted())
        assertEquals(3, b.liberties(10))
        assertEquals(listOf(d['b'], d['d'], d['f']), b.libertyPoints(10).sorted())
        assertEquals(4, b.liberties(12))
        assertEquals(listOf(d['c'], d['d'], d['e'], d['g']), b.libertyPoints(12).sorted())
        assertEquals(0, b.liberties(d['a']))
        assertEquals(0, b.group(d['a']).size)
    }

    @Test fun sharedLibertyOfAGroupCountsOnce() {
        val d = Dia(
            ". a b . .",
            "c X X d .",
            "e X f . .",
            ". g . . .",
            ". . . . .",
        )
        val b = d.board
        assertEquals(listOf(6, 7, 11), b.sortedGroup(6))
        assertEquals(3, b.groupSize(11))
        assertEquals("abcdefg".map { d[it] }.sorted(), b.libertyPoints(7).sorted())
        for (p in b.group(6)) assertEquals(7, b.liberties(p))
        assertEquals(d['a'], b.firstLiberty(11))
    }

    @Test fun capturesOneStone() {
        val d = Dia(
            ". X . . .",
            "X O a . .",
            ". X . . .",
            ". . . . .",
            ". . . . .",
        )
        val b = d.board
        assertEquals(1, b.liberties(6))
        assertTrue(b.play(d['a']))
        assertNull(b[6])
        assertArrayEquals(intArrayOf(6), b.lastCaptured)
        assertEquals(1, b.captured(Stone.BLACK))
        assertEquals(0, b.captured(Stone.WHITE))
        assertEquals(-1, b.koPoint) // the capturing stone has three liberties: no ko
        assertEquals(Stone.WHITE, b.toMove)
        assertEquals(1, b.moveNumber)
        assertEquals(3, b.liberties(1)) // got the captured point back as a liberty
    }

    @Test fun capturesManyStones() {
        val d = Dia(
            ". X X X .",
            "X O O O a",
            ". X X X .",
            ". . . . .",
            ". . . . .",
        )
        val b = d.board
        assertTrue(b.play(d['a']))
        assertEquals(listOf(6, 7, 8), b.lastCaptured.sorted())
        assertEquals(3, b.captured(Stone.BLACK))
        assertEquals(
            listOf(
                ". X X X .",
                "X . . . X",
                ". X X X .",
                ". . . . .",
                ". . . . .",
            ),
            b.toRows(),
        )
        assertEquals(5, b.liberties(1))
        assertEquals(8, b.liberties(11))
        assertEquals(3, b.liberties(d['a']))
    }

    @Test fun capturesTwoGroupsAtOnce() {
        val d = Dia(
            ". X . X .",
            "X O a O X",
            ". X . X .",
            ". . . . .",
            ". . . . .",
        )
        val b = d.board
        assertTrue(b.play(d['a']))
        assertEquals(listOf(6, 8), b.lastCaptured.sorted())
        assertEquals(2, b.captured(Stone.BLACK))
        assertEquals(-1, b.koPoint)
        assertEquals(4, b.liberties(d['a']))
    }

    @Test fun rejectsOccupiedAndOffBoard() {
        val b = Dia(". X", ". .").board
        val hash = b.hash
        assertFalse(b.isLegal(1))
        assertEquals(Illegal.OCCUPIED, b.illegalReason(1))
        assertFalse(b.play(1))
        assertFalse(b.isLegal(4))
        assertFalse(b.isLegal(-2))
        assertFalse(b.play(99))
        assertEquals(hash, b.hash)
        assertEquals(Stone.BLACK, b.toMove)
        assertEquals(0, b.moveNumber)
        assertNull(b.illegalReason(0))
        assertNull(b.illegalReason(PASS))
        assertTrue(b.isLegal(PASS))
    }

    @Test fun rejectsSingleStoneSuicide() {
        val d = Dia(
            "a X . . .",
            "X . . . .",
            ". . . . .",
            ". . . . .",
            ". . . . .",
            toMove = Stone.WHITE,
        )
        val b = d.board
        val before = b.toRows()
        assertFalse(b.isLegal(d['a']))
        assertEquals(Illegal.SUICIDE, b.illegalReason(d['a']))
        assertFalse(b.play(d['a']))
        assertEquals(before, b.toRows())
        assertEquals(Stone.WHITE, b.toMove)
        assertFalse(d['a'] in b.legalMoves())
        assertEquals(22, b.legalMoves().size)
        // Black may fill its own point: it connects to stones with liberties.
        b.setToMove(Stone.BLACK)
        assertTrue(b.isLegal(d['a']))
    }

    @Test fun rejectsMultiStoneSuicide() {
        val d = Dia(
            "O a X . .",
            "X X . . .",
            ". . . . .",
            ". . . . .",
            ". . . . .",
            toMove = Stone.WHITE,
        )
        val b = d.board
        assertEquals(Illegal.SUICIDE, b.illegalReason(d['a']))
        assertFalse(b.play(d['a']))
        assertEquals(Stone.WHITE, b[0])
        assertEquals(1, b.liberties(0))
    }

    @Test fun captureMakesAMoveWithoutLibertiesLegal() {
        val d = Dia(
            ". X O . .",
            "X O a O .",
            ". X O . .",
            ". . . . .",
            ". . . . .",
        )
        val b = d.board
        assertNull(b.illegalReason(d['a']))
        assertTrue(b.play(d['a']))
        assertArrayEquals(intArrayOf(6), b.lastCaptured)
        assertEquals(1, b.liberties(d['a']))
    }

    @Test fun koSequence() {
        val d = Dia(
            ". X O . .",
            "X W a O .",
            ". X O . .",
            ". . . . c",
            ". . . b d",
        )
        val b = d.board
        val ko = Board.marked(listOf(". X O . .", "X W a O .", ". X O . .", ". . . . c", ". . . b d")).single()
        assertTrue(b.play(d['a']))
        assertEquals(ko, b.koPoint)

        // Immediate recapture is forbidden.
        assertFalse(b.isLegal(ko))
        assertEquals(Illegal.KO, b.illegalReason(ko))
        assertFalse(b.play(ko))
        assertEquals(Stone.WHITE, b.toMove)
        assertFalse(ko in b.legalMoves())

        // After a move elsewhere and an answer it is allowed, and the ko turns around.
        val afterThreat = b.copy()
        assertTrue(afterThreat.play(d['b']))
        assertEquals(-1, afterThreat.koPoint)
        assertTrue(afterThreat.play(d['c']))
        assertTrue(afterThreat.play(ko))
        assertArrayEquals(intArrayOf(d['a']), afterThreat.lastCaptured)
        assertEquals(d['a'], afterThreat.koPoint)
        assertEquals(Illegal.KO, afterThreat.illegalReason(d['a']))
        assertEquals(1, afterThreat.captured(Stone.WHITE))

        // A pass clears the ko as well.
        assertTrue(b.play(PASS))
        assertEquals(-1, b.koPoint)
        assertEquals(1, b.passes)
        assertTrue(b.play(d['d']))
        assertEquals(0, b.passes)
        assertTrue(b.play(ko))
        assertArrayEquals(intArrayOf(d['a']), b.lastCaptured)
    }

    @Test fun snapback() {
        val d = Dia(
            ". . . . .",
            ". . . . .",
            ". X X X .",
            "X O O O X",
            "X b a O X",
        )
        val b = d.board
        assertTrue(b.play(d['a'])) // throw in
        assertEquals(1, b.liberties(d['a']))
        assertTrue(b.play(d['b'])) // White takes one stone ..
        assertArrayEquals(intArrayOf(d['a']), b.lastCaptured)
        assertEquals(-1, b.koPoint) // .. with a five stone group: not a ko
        assertEquals(1, b.liberties(d['b']))
        assertTrue(b.play(d['a'])) // and Black takes five back
        assertEquals(5, b.lastCaptured.size)
        assertEquals(5, b.captured(Stone.BLACK))
        assertEquals(1, b.captured(Stone.WHITE))
        assertEquals(
            listOf(
                ". . . . .",
                ". . . . .",
                ". X X X .",
                "X . . . X",
                "X . X . X",
            ),
            b.toRows(),
        )
    }

    @Test fun eyeLike() {
        val d = Dia(
            "a X b X .",
            "X X X O .",
            "X c X . .",
            "O X X . .",
            ". . . . .",
        )
        val b = d.board
        assertTrue(b.isEyeLike(d['a'], Stone.BLACK))
        assertFalse(b.isEyeLike(d['a'], Stone.WHITE))
        assertFalse(b.isEyeLike(d['b'], Stone.BLACK)) // edge point with a white diagonal: false eye
        assertTrue(b.isEyeLike(d['c'], Stone.BLACK))  // centre point tolerates one white diagonal
        assertFalse(b.isEyeLike(1, Stone.BLACK))      // occupied
        assertFalse(b.isEyeLike(24, Stone.BLACK))
    }

    @Test fun fromRowsReadsMarksLabelsAndSide() {
        val rows = listOf(
            "B . a",
            ". W .",
            "w X O",
        )
        val b = Board.fromRows(rows, Stone.WHITE)
        assertEquals(3, b.size)
        assertEquals(Stone.WHITE, b.toMove)
        assertEquals(listOf("X . .", ". O .", ". X O"), b.toRows())
        assertEquals(mapOf('a' to 2, 'w' to 6), Board.labels(rows))
        assertArrayEquals(intArrayOf(0, 4), Board.marked(rows))
        assertEquals(0, b.moveNumber)
        assertEquals(-1, b.koPoint)
        assertEquals(Stone.BLACK, Board.fromRows(rows).toMove)
        assertEquals("X . .\n. O .\n. X O", b.toString())
    }

    @Test fun fromRowsRejectsBadDiagrams() {
        fun bad(vararg rows: String) {
            assertThrows(IllegalArgumentException::class.java) { Board.fromRows(rows.toList()) }
        }
        bad(". . .", ". .", ". . .")          // ragged
        bad(". .", ". .", ". .")              // not square
        bad(". .  .", ". . .", ". . .")       // double space
        bad(". . .", ". x .", ". . .")        // x is not a label
        bad(". . .", ". o .", ". . .")        // neither is o
        bad(". . .", ". y .", ". . .")        // labels stop at w
        bad(". . .", ". ? .", ". . .")
        bad("O X .", "X . .", ". . .")        // white corner stone without liberties
        bad("X X", "X X")                     // full board
        assertThrows(IllegalArgumentException::class.java) { Board.labels(listOf("a a", ". .")) }
        assertThrows(IllegalArgumentException::class.java) { Board(1) }
        assertThrows(IllegalArgumentException::class.java) { Board(20) }
    }

    @Test fun pointNames() {
        assertEquals("A1", Points.name(Points.of(0, 18, 19), 19))
        assertEquals("T19", Points.name(Points.of(18, 0, 19), 19))
        assertEquals("A19", Points.name(0, 19))
        assertEquals("H1", Points.name(Points.of(7, 8, 9), 9))
        assertEquals("J9", Points.name(Points.of(8, 0, 9), 9)) // I is skipped
        assertEquals("D4", Points.name(Points.of(3, 5, 9), 9))
        assertEquals("pass", Points.name(PASS, 9))
        assertEquals(PASS, Points.parse("pass", 9))
        assertEquals(Points.of(3, 5, 9), Points.parse("d4", 9))
        for (bad in listOf("I5", "i5", "K1", "A0", "A10", "", "A", "4D", "D-4", "D+4", "D4 ", "AA1", "D４")) {
            assertEquals(bad, -2, Points.parse(bad, 9))
        }
        assertEquals(-2, Points.parse("A20", 19))
        assertEquals(-2, Points.parse("U1", 19))
        for (size in intArrayOf(5, 9, 13, 19)) {
            val names = HashSet<String>()
            for (p in 0 until size * size) {
                val name = Points.name(p, size)
                assertFalse(name, name.contains('I'))
                assertTrue(name, names.add(name))
                assertEquals(name, p, Points.parse(name, size))
                assertEquals(p, Points.of(Points.x(p, size), Points.y(p, size), size))
            }
        }
    }

    @Test fun hashIsTheSameForTransposedMoveOrders() {
        val a = Board(9)
        val b = Board(9)
        for (name in listOf("C3", "G7", "G3", "C7", "E5", "E3")) assertTrue(a.play(Points.parse(name, 9)))
        for (name in listOf("E5", "C7", "G3", "E3", "C3", "G7")) assertTrue(b.play(Points.parse(name, 9)))
        assertEquals(a.toRows(), b.toRows())
        assertEquals(a.hash, b.hash)

        val c = a.copy()
        assertEquals(a.hash, c.hash)
        c.setToMove(Stone.WHITE)
        assertNotEquals(a.hash, c.hash) // side to move is part of the hash
        assertTrue(c.play(PASS))
        assertEquals(a.hash, c.hash)
        assertNotEquals(Board(9).hash, a.hash)
        assertEquals(Board(9).hash, Board(9).hash)
    }

    @Test fun hashIncludesTheKo() {
        val d = Dia(
            ". X O . .",
            "X O a O .",
            ". X O . .",
            ". . . . .",
            ". . . . .",
        )
        val withKo = d.board
        assertTrue(withKo.play(d['a']))
        val same = Board.fromRows(withKo.toRows(), Stone.WHITE)
        assertEquals(-1, same.koPoint)
        assertNotEquals(withKo.hash, same.hash)
    }

    @Test fun copyIsIndependent() {
        val a = Board(9)
        a.play(40)
        val b = a.copy()
        assertTrue(b.play(41))
        assertNull(a[41])
        assertEquals(1, a.moveNumber)
        assertEquals(2, b.moveNumber)
        assertEquals(4, a.liberties(40))
        assertEquals(3, b.liberties(40))
        a.copyFrom(b)
        assertEquals(b.hash, a.hash)
        assertEquals(b.toRows(), a.toRows())
    }
}
