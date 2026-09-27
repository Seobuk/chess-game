package com.seobuk.chess.baduk.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Random games checked against a deliberately naive reference: flood fills for groups and liberties,
 * and the ko rule stated as "a move may not bring back the position from before the opponent's last move".
 */
class InvariantTest {
    private class Naive(val size: Int) {
        var cells = arrayOfNulls<Stone>(size * size)
        var beforeLastMove = cells.copyOf()
        val captures = IntArray(2)

        fun neighbours(p: Int): List<Int> {
            val x = p % size
            val y = p / size
            return listOfNotNull(
                if (y > 0) p - size else null,
                if (x > 0) p - 1 else null,
                if (x < size - 1) p + 1 else null,
                if (y < size - 1) p + size else null,
            )
        }

        /** Stones and liberties of the group at [p]. */
        fun flood(p: Int, on: Array<Stone?> = cells): Pair<Set<Int>, Set<Int>> {
            val stones = LinkedHashSet<Int>()
            val libs = HashSet<Int>()
            val todo = ArrayDeque(listOf(p))
            stones.add(p)
            while (todo.isNotEmpty()) {
                for (q in neighbours(todo.removeLast())) {
                    if (on[q] == null) libs.add(q)
                    else if (on[q] == on[p] && stones.add(q)) todo.add(q)
                }
            }
            return stones to libs
        }

        /** The position after [s] plays [p] and what it captured, or null if the move is illegal. */
        fun result(p: Int, s: Stone): Pair<Array<Stone?>, List<Int>>? {
            if (cells[p] != null) return null
            val after = cells.copyOf()
            after[p] = s
            val captured = ArrayList<Int>()
            for (q in neighbours(p)) {
                if (after[q] != s.opposite) continue
                val (stones, libs) = flood(q, after)
                if (libs.isEmpty()) stones.forEach { after[it] = null; captured.add(it) }
            }
            if (flood(p, after).second.isEmpty()) return null
            if (after.contentEquals(beforeLastMove)) return null
            return after to captured
        }

        fun play(p: Int, s: Stone): List<Int> {
            val before = cells
            val captured = if (p == PASS) emptyList() else {
                val (after, taken) = result(p, s)!!
                cells = after
                taken
            }
            beforeLastMove = before
            captures[s.ordinal] += captured.size
            return captured
        }
    }

    private fun checkGroups(b: Board, ref: Naive) {
        val n = b.size * b.size
        val done = BooleanArray(n)
        var empty = 0
        for (p in 0 until n) {
            assertEquals(ref.cells[p], b[p])
            if (b[p] == null) {
                empty++
                assertEquals(0, b.liberties(p))
                continue
            }
            if (done[p]) continue
            val (stones, libs) = ref.flood(p)
            assertTrue(libs.isNotEmpty())
            assertEquals(libs.sorted(), b.libertyPoints(p).sorted())
            assertEquals(stones.sorted(), b.group(p).sorted())
            assertTrue(b.firstLiberty(p) in libs)
            for (s in stones) {
                done[s] = true
                assertEquals(libs.size, b.liberties(s))
                assertEquals(stones.size, b.groupSize(s))
                assertEquals(b.groupId(p), b.groupId(s))
            }
        }
        assertEquals(empty, b.emptyCount)
        val listed = (0 until b.emptyCount).map { b.emptyAt(it) }.toSet()
        assertEquals(empty, listed.size)
        assertTrue(listed.all { b[it] == null })
        assertEquals(ref.captures[0], b.captured(Stone.BLACK))
        assertEquals(ref.captures[1], b.captured(Stone.WHITE))

        if (b.koPoint == -1) {
            val fresh = Board(b.size)
            for (p in 0 until n) b[p]?.let { fresh.place(p, it) }
            fresh.setToMove(b.toMove)
            assertEquals(fresh.hash, b.hash)
        }
    }

    private fun checkLegality(b: Board, ref: Naive) {
        val n = b.size * b.size
        val legal = (0 until n).filter { ref.result(it, b.toMove) != null }
        assertArrayEquals(legal.toIntArray(), b.legalMoves())
        for (p in 0 until n) assertEquals(p in legal, b.isLegal(p))
        if (b.koPoint != -1) assertTrue(b.koPoint !in legal && b[b.koPoint] == null)
    }

    private fun randomMoves(size: Int, moves: Int, seed: Long, legalityEvery: Int) {
        val rnd = Random(seed)
        val b = Board(size)
        val ref = Naive(size)
        var kos = 0
        var captures = 0
        repeat(moves) { step ->
            val legal = b.legalMoves()
            val p = if (legal.isEmpty() || rnd.nextInt(50) == 0) PASS else legal[rnd.nextInt(legal.size)]
            val mover = b.toMove
            val passes = b.passes
            assertTrue(b.play(p))
            assertEquals(ref.play(p, mover).sorted(), b.lastCaptured.sorted())
            assertEquals(if (p == PASS) passes + 1 else 0, b.passes)
            assertEquals(step + 1, b.moveNumber)
            assertEquals(mover.opposite, b.toMove)
            checkGroups(b, ref)
            if (step % legalityEvery == 0 || b.koPoint != -1) checkLegality(b, ref)
            if (b.koPoint != -1) kos++
            captures += b.lastCaptureCount

            // A copy carries the whole state.
            if (step % 97 == 0) {
                val c = b.copy()
                assertEquals(b.hash, c.hash)
                assertEquals(b.koPoint, c.koPoint)
                assertArrayEquals(b.lastCaptured, c.lastCaptured)
                checkGroups(c, ref)
                assertArrayEquals(b.legalMoves(), c.legalMoves())
            }
        }
        if (size >= 9) assertTrue("kos $kos", kos > 0)
        assertTrue("captures $captures", captures > moves / 20)
    }

    @Test fun tenThousandRandomMovesOn19() = randomMoves(19, 10_000, seed = 19, legalityEvery = 25)

    @Test fun tenThousandRandomMovesOn9() = randomMoves(9, 10_000, seed = 9, legalityEvery = 1)

    @Test fun randomMovesOnTinyBoards() {
        for (size in 2..5) randomMoves(size, 3_000, seed = size.toLong(), legalityEvery = 1)
    }
}
