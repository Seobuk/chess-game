package com.seobuk.chess.baduk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uniformly random playouts that never fill an eye, through the allocation-free fast path. */
class PlayoutTest {
    private var seed = 0x2545F4914F6CDD1DL

    private fun random(bound: Int): Int {
        seed = seed xor (seed shl 13)
        seed = seed xor (seed ushr 7)
        seed = seed xor (seed shl 17)
        return ((seed ushr 33) % bound).toInt()
    }

    /** Plays [b] out until two passes; returns the number of moves. */
    private fun playout(b: Board): Int {
        val limit = b.size * b.size * 3
        var moves = 0
        while (b.passes < 2 && moves < limit) {
            val count = b.emptyCount
            var played = false
            if (count > 0) {
                var i = random(count)
                val me = b.toMove
                for (tries in 0 until count) {
                    val p = b.emptyAt(i)
                    if (!b.isEyeLike(p, me) && b.play(p)) {
                        played = true
                        break
                    }
                    if (++i == count) i = 0
                }
            }
            if (!played) b.play(PASS)
            moves++
        }
        return moves
    }

    private fun perSecond(size: Int, millis: Long): Double {
        val root = Board(size)
        val b = Board(size)
        var done = 0
        var moves = 0L
        val start = System.nanoTime()
        while (System.nanoTime() - start < millis * 1_000_000) {
            repeat(50) {
                b.copyFrom(root)
                moves += playout(b)
            }
            done += 50
        }
        val seconds = (System.nanoTime() - start) / 1e9
        println("baduk playouts ${size}x$size: %.0f/s, %.0f moves each".format(done / seconds, moves.toDouble() / done))
        return done / seconds
    }

    @Test fun playoutsEndWithEveryPointSettled() {
        repeat(200) {
            val b = Board(9)
            val moves = playout(b)
            assertTrue("moves $moves", moves < 9 * 9 * 3)
            assertEquals(2, b.passes)
            // Only eyes are left: area scoring gives every point to somebody.
            val s = Scoring.area(b, emptySet(), 0.0)
            assertEquals(81.0, s.black + s.white, 0.0)
        }
    }

    @Test fun speed() {
        perSecond(9, 1000) // warm-up
        val nine = perSecond(9, 1000)
        perSecond(13, 500)
        perSecond(19, 1000)
        // Target of the engine contract, an order of magnitude below what a desktop JVM does.
        assertTrue("9x9 playouts/s: $nine", nine >= 3000)
    }
}
