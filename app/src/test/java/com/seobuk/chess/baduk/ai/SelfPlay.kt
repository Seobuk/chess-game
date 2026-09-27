package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.BadukGame
import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.PASS
import com.seobuk.chess.baduk.core.Scoring
import com.seobuk.chess.baduk.core.Stone

/** Test helpers: games between move choosers, scored the way the app scores them. */
internal object SelfPlay {
    class Result(val winner: Stone?, val game: BadukGame, val margin: Double, val how: String)

    /** Plays until the game is over or [maxMoves]; an illegal answer throws. */
    fun play(
        size: Int,
        black: (BadukGame) -> Int,
        white: (BadukGame) -> Int,
        captureGo: Boolean = false,
        maxMoves: Int = size * size * 3,
        onMove: (BadukGame, Int) -> Unit = { _, _ -> },
    ): Result {
        val game = BadukGame(size, captureGo = captureGo)
        while (!game.isOver && game.moves.size < maxMoves) {
            val mover = game.board.toMove
            val m = (if (mover == Stone.BLACK) black else white)(game)
            onMove(game, m)
            if (m == RESIGN) game.resign(mover) else check(game.play(m)) { "illegal move $m by $mover\n${game.board}" }
        }
        game.resigned?.let { return Result(it.opposite, game, 0.0, "resign") }
        game.captureWinner?.let { return Result(it, game, 0.0, "capture") }
        val dead = BadukCoach.estimate(game.board, game.komi, 400, seed = game.board.hash).dead
        val s = Scoring.territory(game.board, dead, game.komi)
        return Result(s.winner, game, if (s.winner == Stone.BLACK) s.margin else -s.margin, if (game.isOver) "count" else "move limit")
    }

    /**
     * A beginner who knows one thing: takes the biggest group it can take. Otherwise it plays any
     * legal point that is no eye of its own, no self-atari and not deep inside its own stones (it
     * does not fill its own area); it passes when there is none or when the opponent has just
     * passed (it takes the opponent's word that the game is over).
     */
    fun captureBot(seed: Long): (BadukGame) -> Int {
        val rng = java.util.Random(seed)
        return { game ->
            val b = game.board
            val geo = Geo.of(b.size)
            val code = b.toMove.ordinal + 1
            val moves = b.legalMoves().filter { !b.isEyeLike(it, b.toMove) }
            fun near(p: Int, colour: Int) = (0 until geo.n).any { geo.distance(p, it) <= 2 && b.color(it) == colour }
            val quiet = moves.filter { selfAtariSize(b, it, code) == 0 && (!near(it, code) || near(it, 3 - code)) }
            capture(b) ?: if (quiet.isEmpty() || b.passes > 0) PASS else quiet[rng.nextInt(quiet.size)]
        }
    }

    /** Plays like [level] but never misses a capture: the biggest one on the board comes first. */
    fun capturing(level: Int, seed: Long): (BadukGame) -> Int {
        val other = level(level, seed)
        return { game -> capture(game.board) ?: other(game) }
    }

    private fun capture(b: Board): Int? {
        val code = b.toMove.ordinal + 1
        return b.legalMoves().filter { captureSize(b, it, code) > 0 }.maxByOrNull { captureSize(b, it, code) }
    }

    fun level(level: Int, seed: Long, budget: Double = 1.0): (BadukGame) -> Int {
        val player = BadukPlayer(BadukLevels.get(level), seed)
        player.budget = budget
        return { player.chooseMove(it) }
    }

    fun rows(b: Board): String = b.toRows().mapIndexed { y, r -> "%2d %s".format(b.size - y, r) }.joinToString("\n")
}
