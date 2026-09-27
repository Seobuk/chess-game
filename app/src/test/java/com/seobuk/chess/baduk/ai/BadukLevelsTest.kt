package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.BadukGame
import com.seobuk.chess.baduk.core.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class BadukLevelsTest {
    @Test fun tenLevelsInOrder() {
        val all = BadukLevels.all
        assertEquals((1..10).toList(), all.map { it.level })
        assertEquals(10, all.map { it.nameKo }.toSet().size)
        for ((a, b) in all.zipWithNext()) assertTrue("${b.level}", b.approxKyu < a.approxKyu)
        assertEquals(30, all.first().approxKyu)
        for (l in all) {
            assertTrue(l.nameKo, l.nameKo.isNotBlank() && l.description.endsWith("요."))
            assertTrue(l.description, l.description.none { it in "—–!" })
        }
        assertEquals(all[0], BadukLevels.get(0))
        assertEquals(all[9], BadukLevels.get(11))
        assertEquals(all[4], BadukLevels.get(5))
    }

    /** Wins of [a] against [b] in [games] games on 9x9, colours alternating. */
    private fun wins(a: (Long) -> (BadukGame) -> Int, b: (Long) -> (BadukGame) -> Int, games: Int): Int {
        var won = 0
        for (g in 0 until games) {
            val aBlack = g % 2 == 0
            val pa = a(1000L + g)
            val pb = b(2000L + g)
            val r = if (aBlack) SelfPlay.play(9, pa, pb) else SelfPlay.play(9, pb, pa)
            if (r.winner == (if (aBlack) Stone.BLACK else Stone.WHITE)) won++
        }
        return won
    }

    @Test fun levelOneLosesToSomeoneWhoOnlyCaptures() {
        // Level 1's own play plus one skill: never missing a capture. That alone must win more often than not.
        val same = wins({ SelfPlay.capturing(1, it) }, { SelfPlay.level(1, it) }, 30)
        // Without any feel for territory (random points) capturing still wins a game now and then.
        val random = wins({ SelfPlay.captureBot(it) }, { SelfPlay.level(1, it) }, 30)
        println("against level 1: level 1 that never misses a capture $same of 30, random capture bot $random of 30")
        assertTrue("capturing won $same of 30", same >= 15)
        assertTrue("the random capture bot won $random of 30", random >= 2)
    }

    @Test fun strongestLevelKeepsItsTimeLimit() {
        for ((size, limit) in listOf(9 to 3300L, 19 to 4300L)) {
            val game = BadukGame(size)
            game.play(size * size / 2)
            val start = System.nanoTime()
            val m = BadukPlayer(BadukLevels.get(10), 1).chooseMove(game)
            val ms = (System.nanoTime() - start) / 1_000_000
            println("level 10 on ${size}x$size: $ms ms")
            assertTrue("$size: $ms ms", ms in 2500..limit)
            assertTrue(game.board.isLegal(m) && m >= 0)
        }
        // The weakest levels answer at once.
        val start = System.nanoTime()
        BadukPlayer(BadukLevels.get(1), 1).chooseMove(BadukGame(19))
        assertTrue((System.nanoTime() - start) / 1_000_000 < 600)
    }

    @Test fun chooseMoveCanBeCancelled() {
        val start = System.nanoTime()
        BadukPlayer(BadukLevels.get(10), 1).chooseMove(BadukGame(19)) { System.nanoTime() - start > 100_000_000 }
        assertTrue((System.nanoTime() - start) / 1_000_000 < 600)
    }

    @Test fun strongerLevelsWin() {
        val three = wins({ SelfPlay.level(3, it) }, { SelfPlay.level(1, it) }, 10)
        val six = wins({ SelfPlay.level(6, it) }, { SelfPlay.level(3, it) }, 10)
        println("level 3 against level 1: $three of 10, level 6 against level 3: $six of 10")
        assertTrue("level 3 won $three of 10 against level 1", three >= 8)
        assertTrue("level 6 won $six of 10 against level 3", six >= 7)
    }

    /**
     * The whole ladder, several minutes: BADUK_SELFPLAY=<games per pair> ./gradlew :app:testDebugUnitTest
     * --tests "*BadukLevelsTest.selfPlayTable". Pairs with a level above 7 run on a quarter of the budget.
     */
    @Test fun selfPlayTable() {
        val games = System.getenv("BADUK_SELFPLAY")?.toIntOrNull() ?: 0
        assumeTrue(games > 0)
        val from = System.getenv("BADUK_SELFPLAY_FROM")?.toIntOrNull() ?: 1
        val to = System.getenv("BADUK_SELFPLAY_TO")?.toIntOrNull() ?: 9
        for (level in 1..3) if (level in from..to) {
            println("random capture bot against level $level: ${wins({ SelfPlay.captureBot(it) }, { SelfPlay.level(level, it) }, games)} of $games")
            println("level 1 that never misses a capture against level $level: ${wins({ SelfPlay.capturing(1, it) }, { SelfPlay.level(level, it) }, games)} of $games")
        }
        for (low in from..to) {
            val budget = if (low + 1 > 7) 0.25 else 1.0
            val t = System.nanoTime()
            val won = wins({ SelfPlay.level(low + 1, it, budget) }, { SelfPlay.level(low, it, budget) }, games)
            println("level ${low + 1} against level $low: $won of $games (budget $budget, ${(System.nanoTime() - t) / 1_000_000_000} s)")
        }
    }
}
