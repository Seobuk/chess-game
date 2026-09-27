package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.BadukGame
import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.PASS

/** Answer of [BadukPlayer.chooseMove]: the AI gives up. */
const val RESIGN = -2

/** [approxKyu] is a rough guess for 9x9 play, not a measured rank. */
data class BadukLevel(val level: Int, val nameKo: String, val approxKyu: Int, val description: String)

object BadukLevels {
    val all: List<BadukLevel> = listOf(
        BadukLevel(1, "조약돌", 30, "단수를 자주 놓치고 돌을 쉽게 내줘요. 첫 대국 상대로 좋아요."),
        BadukLevel(2, "새싹", 27, "큰 돌이 잡힐 때만 알아채요. 아직 실수가 많아요."),
        BadukLevel(3, "시냇물", 24, "스스로 단수에 들어가지는 않지만 끊는 점을 자주 놓쳐요."),
        BadukLevel(4, "대나무", 21, "돌 몇 개가 걸린 단수는 놓치지 않아요."),
        BadukLevel(5, "소나무", 18, "따낼 수 있는 돌은 대부분 따내고 집도 챙기기 시작해요."),
        BadukLevel(6, "바위", 16, "실수가 줄고 약한 돌을 찾아 공격해요."),
        BadukLevel(7, "폭포", 14, "수읽기가 빨라지고 작은 빈틈도 파고들어요."),
        BadukLevel(8, "산마루", 12, "싸움과 집 계산이 모두 탄탄해요."),
        BadukLevel(9, "구름", 10, "깊이 읽어서 좀처럼 빈틈을 보이지 않아요."),
        BadukLevel(10, "큰 산", 8, "가장 강한 AI예요. 주어진 시간을 다 써서 끝까지 읽어요."),
    )

    fun get(level: Int): BadukLevel = all[level.coerceIn(1, 10) - 1]
}

/**
 * How a level plays: a search of at most [playouts] / [timeMs] (9x9; larger boards get more time),
 * then with probability [slip] a careless move instead, unless the best move takes or saves at
 * least [notice] stones. A careless move is any sensible-looking point near the last two moves or,
 * with probability [wild], anywhere on the board. [selfAtari]: careless moves may walk into atari.
 * [resignBelow]: win rate under which the level gives up (0 = never).
 */
private class Style(
    val playouts: Int,
    val timeMs: Long,
    val slip: Double,
    val notice: Int,
    val wild: Double = 0.0,
    val selfAtari: Boolean = false,
    val resignBelow: Double = 0.0,
)

private const val NEVER = 1_000

private val STYLES = arrayOf(
    Style(playouts = 30, timeMs = 600, slip = 0.90, notice = NEVER, wild = 0.3, selfAtari = true),
    Style(playouts = 50, timeMs = 600, slip = 0.72, notice = 6, wild = 0.2, selfAtari = true),
    Style(playouts = 80, timeMs = 600, slip = 0.55, notice = 4),
    Style(playouts = 120, timeMs = 700, slip = 0.40, notice = 3),
    Style(playouts = 200, timeMs = 800, slip = 0.27, notice = 2),
    Style(playouts = 400, timeMs = 1000, slip = 0.16, notice = 1),
    Style(playouts = 1000, timeMs = 1200, slip = 0.08, notice = 1, resignBelow = 0.04),
    Style(playouts = 3000, timeMs = 1600, slip = 0.03, notice = 1, resignBelow = 0.05),
    Style(playouts = 8000, timeMs = 2200, slip = 0.0, notice = 1, resignBelow = 0.05),
    Style(playouts = Int.MAX_VALUE, timeMs = 3000, slip = 0.0, notice = 1, resignBelow = 0.05),
)

/** Not thread-safe; use one instance per game/worker. */
class BadukPlayer(val level: BadukLevel, seed: Long = System.nanoTime()) {
    private val rng = java.util.Random(seed)
    private val engine = BadukEngine(seed)
    private val style = STYLES[level.level.coerceIn(1, 10) - 1]

    /** Test hook: scales playouts and thinking time. */
    internal var budget = 1.0

    /** Blocking. A legal point, PASS or [RESIGN]. */
    fun chooseMove(game: BadukGame, isCancelled: () -> Boolean = { false }): Int {
        val moves = game.moves
        return chooseMove(
            game.board.copy(), game.komi, game.captureGo,
            moves.lastOrNull() ?: PASS, moves.getOrElse(moves.size - 2) { PASS }, isCancelled,
        )
    }

    /** [last] and [before]: the two moves that led to [board], if known. */
    internal fun chooseMove(
        board: Board,
        komi: Double,
        captureGo: Boolean = false,
        last: Int = PASS,
        before: Int = PASS,
        isCancelled: () -> Boolean = { false },
    ): Int {
        val n = board.size * board.size
        // ponytail: only simple ko is a rule, so a triple ko could go round forever; the AI stops feeding it.
        if (board.moveNumber >= n * 4) return PASS
        // 3 s on 9x9 up to 4 s on 19x19 for the strongest level.
        val time = style.timeMs * (8 + board.size / 5) / 9.0 * budget
        val playouts = if (style.playouts == Int.MAX_VALUE) Int.MAX_VALUE else (style.playouts * budget).toInt().coerceAtLeast(8)
        val s = engine.search(board, komi, time.toLong(), playouts, captureGo, isCancelled, last)
        if (s.best == PASS) return PASS
        if (!captureGo && n - board.emptyCount >= n / 2 && s.playouts >= 300 && s.winRate < style.resignBelow) return RESIGN

        val me = board.toMove.ordinal + 1
        val stakes = captureSize(board, s.best, me) + rescueSize(board, s.best, me)
        if (stakes < style.notice && rng.nextDouble() < style.slip) {
            carelessMove(board, last, before, rng.nextDouble() < style.wild)?.let { return it }
        }
        return s.best
    }

    /**
     * A legal point, near the last two moves unless [wild], that is no eye of its own and overlooks
     * every stone in atari (it neither takes nor saves anything); late in the game only a point the
     * engine found worth playing (nothing inside settled territory).
     */
    private fun carelessMove(board: Board, last: Int, before: Int, wild: Boolean): Int? {
        val geo = Geo.of(board.size)
        val me = board.toMove
        val code = me.ordinal + 1
        val anywhere = wild || last < 0 && before < 0
        val pool = (engine.useful?.toList() ?: (0 until geo.n)).filter { p ->
            board.color(p) == Board.EMPTY && !board.isEyeLike(p, me) && board.isLegal(p) &&
                (anywhere || last >= 0 && geo.distance(p, last) <= 2 || before >= 0 && geo.distance(p, before) <= 2) &&
                (geo.line[p] > 0 || geo.n - board.emptyCount > geo.n / 6) &&
                captureSize(board, p, code) == 0 && rescueSize(board, p, code) == 0 &&
                (style.selfAtari || selfAtariSize(board, p, code) == 0)
        }
        return if (pool.isEmpty()) null else pool[rng.nextInt(pool.size)]
    }
}
