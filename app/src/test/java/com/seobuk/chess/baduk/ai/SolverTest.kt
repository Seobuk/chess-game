package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.Dia
import com.seobuk.chess.baduk.core.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverTest {
    private class Case(vararg val rows: String, toMove: Stone = Stone.BLACK) {
        val d = Dia(*rows, toMove = toMove)
        val board get() = d.board.copy()
        val target = Board.marked(rows.toList()).min()
        fun after(vararg labels: Char): Board = board.also { b -> labels.forEach { assertTrue("$it", b.play(d[it])) } }
        fun toMove(s: Stone): Board = board.also { it.setToMove(s) }
    }

    // The white stone runs towards the far corner; every extension leaves it two liberties.
    private val ladder = Case(
        ". . . . . . . . .",
        ". . X X . . . . .",
        ". X W b . . . . .",
        ". . a . . . . . .",
        ". . . . . . . . .",
        ". . . . . . . . .",
        ". . . . . . . . .",
        ". . . . . . . . .",
        ". . . . . . . . .",
    )

    @Test fun ladderAcrossTheBoard() {
        assertEquals(true, Solver.capturable(ladder.board, ladder.target))
        assertEquals(true, Solver.capturable(ladder.after('a'), ladder.target))
        // The atari from the wrong side lets the stone out.
        assertEquals(false, Solver.capturable(ladder.after('b'), ladder.target))
        // The defender moving first simply extends.
        assertEquals(false, Solver.capturable(ladder.toMove(Stone.WHITE), ladder.target))

        val line = Solver.refutation(ladder.board, ladder.target)
        assertEquals(ladder.d['a'], line.first())
        assertTrue("line $line", line.size >= 8)
        val b = ladder.board
        for (p in line) assertTrue(b.play(p))
    }

    @Test fun ladderBreaker() {
        val c = Case(
            ". . . . . . . . .",
            ". . X X . . . . .",
            ". X W . . . . . .",
            ". . a . . . . . .",
            ". . . . . . . . .",
            ". . . . . . . . .",
            ". . . . . . O . .",
            ". . . . . . . . .",
            ". . . . . . . . .",
        )
        assertEquals(false, Solver.capturable(c.after('a'), c.target))
    }

    // A white stone on the ladder's path: the ladder fails, the net still works.
    private val net = Case(
        ". . . . . . . . .",
        ". . . . . . . . .",
        ". . . X X . . . .",
        ". . X W b . . . .",
        ". . X c a . . . .",
        ". . . . . . . . .",
        ". . . . . . O . .",
        ". . . . . . . . .",
        ". . . . . . . . .",
    )

    @Test fun net() {
        assertEquals(true, Solver.capturable(net.board, net.target))
        assertEquals(true, Solver.capturable(net.after('a'), net.target))
        assertEquals(false, Solver.capturable(net.after('b'), net.target))
        assertEquals(false, Solver.capturable(net.after('c'), net.target))
        assertEquals(net.d['a'], Solver.refutation(net.board, net.target).first())
    }

    private val snapback = Case(
        ". . . . . . .",
        ". . . . . . .",
        ". . . . . . .",
        ". . . X X . .",
        "O O X X X X .",
        ". O X W W X .",
        ". O O a b X .",
    )

    @Test fun snapback() {
        assertEquals(true, Solver.capturable(snapback.board, snapback.target))
        assertEquals(true, Solver.capturable(snapback.after('a'), snapback.target))
        assertEquals(true, Solver.capturable(snapback.after('a', 'b'), snapback.target))
        assertEquals(false, Solver.capturable(snapback.after('b'), snapback.target))
        assertEquals(listOf('a', 'b', 'a').map { snapback.d[it] }, Solver.refutation(snapback.board, snapback.target))
    }

    private fun region(c: Case) = Solver.eyeRegion(c.board, c.target)

    // 곧은 3궁: whoever takes the middle point wins.
    private val straightThree = Case(
        "a b c B O",
        "X X X X O",
        "O O O O O",
        "O O O O O",
        "O . O . O",
    )

    @Test fun straightThree() {
        val c = straightThree
        val r = region(c)
        assertEquals("abc".map { c.d[it] }.toSet(), r.filter { c.board[it] == null }.toSet())
        assertEquals(true, Solver.capturable(c.toMove(Stone.WHITE), c.target, r))
        assertEquals(false, Solver.capturable(c.toMove(Stone.BLACK), c.target, r))
        assertEquals(false, Solver.capturable(c.after('b'), c.target, r))
        assertEquals(true, Solver.capturable(c.after('a'), c.target, r))
        assertEquals(true, Solver.capturable(c.after('c'), c.target, r))
        assertEquals(c.d['b'], Solver.refutation(c.toMove(Stone.WHITE), c.target, r).first())
        assertEquals(c.d['b'], Solver.refutation(c.after('a'), c.target, r).first())
    }

    // 굽은 3궁 in the corner.
    @Test fun bentThree() {
        val c = Case(
            "X X X . X",
            "X X X X X",
            ". X X W W",
            "X X W W c",
            "X X W b a",
        )
        val r = region(c)
        assertEquals(true, Solver.capturable(c.after('a'), c.target, r))
        assertEquals(false, Solver.capturable(c.after('b'), c.target, r))
        assertEquals(false, Solver.capturable(c.after('c'), c.target, r))
        assertEquals(false, Solver.capturable(c.toMove(Stone.WHITE), c.target, r))
    }

    // 곧은 4궁: alive as it stands, the two middle points are miai.
    @Test fun straightFourIsAlive() {
        val c = Case(
            "a b c d B O O",
            "X X X X X O O",
            "O O O O O O O",
            "O O O O O O O",
            "O . O O O . O",
            "O O O O O O O",
            "O O O O O O O",
        )
        val r = region(c)
        assertEquals(false, Solver.capturable(c.toMove(Stone.WHITE), c.target, r))
        for (l in "abcd") assertEquals("$l", false, Solver.capturable(c.toMove(Stone.WHITE).also { it.play(c.d[l]) }, c.target, r))
    }

    // 네모 4궁: dead as it stands, even if the defender moves first.
    @Test fun squareFourIsDead() {
        val c = Case(
            "a b B O O O",
            "c d X O O O",
            "X X X O . O",
            "O O O O O O",
            "O . O O O O",
            "O O O O O O",
        )
        val r = region(c)
        assertEquals(true, Solver.capturable(c.toMove(Stone.WHITE), c.target, r))
        assertEquals(true, Solver.capturable(c.toMove(Stone.BLACK), c.target, r))
        for (l in "abcd") assertEquals("$l", true, Solver.capturable(c.after(l), c.target, r))
    }

    // 삿갓 4궁 (pyramid four): the middle of the T decides.
    @Test fun pyramidFour() {
        val c = Case(
            "W W a W W X X",
            "W b c d W X X",
            "W W W W W X X",
            "X X X X X X X",
            "X . X X X . X",
            "X X X X X X X",
            "X X X X X X X",
        )
        val r = region(c)
        assertEquals("abcd".map { c.d[it] }.toSet(), r.filter { c.board[it] == null }.toSet())
        assertEquals(true, Solver.capturable(c.after('c'), c.target, r))
        for (l in "abd") assertEquals("$l", false, Solver.capturable(c.after(l), c.target, r))
        assertEquals(false, Solver.capturable(c.toMove(Stone.WHITE), c.target, r))
    }

    @Test fun twoEyesNeedNoRegionToBeSafe() {
        val c = Case(
            ". B . B O",
            "X X X X O",
            "O O O O O",
            "O O O O O",
            "O . O . O",
        )
        assertEquals(false, Solver.capturable(c.toMove(Stone.WHITE), c.target, region(c)))
    }

    @Test fun givesUpWhenTheBudgetRunsOut() {
        val c = straightThree
        assertEquals(null, Solver.capturable(c.toMove(Stone.WHITE), c.target, region(c), maxNodes = 2))
        assertEquals(emptyList<Int>(), Solver.refutation(c.toMove(Stone.WHITE), c.target, region(c), maxNodes = 2))
    }

    @Test fun speed() {
        // Life and death without a region on a small board: a few thousand nodes of plain search.
        val open = Case(
            ". . . . B O O",
            ". . . X X O O",
            "X X X X O O O",
            "O O O O O O O",
            "O . O O O . O",
            "O O O O O O O",
            "O O O O O O O",
        )
        val all = (0 until 49).toSet()
        var nodes = 0L
        var runs = 0
        val start = System.nanoTime()
        while (System.nanoTime() - start < 400_000_000L) {
            nodes += Solver.count(open.toMove(Stone.WHITE), open.target, all, 2_000_000)
            nodes += Solver.count(ladder.board, ladder.target, null, 2_000_000)
            runs++
        }
        val seconds = (System.nanoTime() - start) / 1e9
        println("baduk solver: %.0f nodes/s (%d nodes per run)".format(nodes / seconds, nodes / runs))
        assertTrue(nodes / seconds > 20_000)
    }
}
