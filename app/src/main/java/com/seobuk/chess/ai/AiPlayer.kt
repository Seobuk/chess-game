package com.seobuk.chess.ai

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Position
import com.seobuk.chess.learn.Opening
import com.seobuk.chess.learn.OpeningBook
import kotlin.math.max
import kotlin.math.min

data class AiLevel(
    val level: Int, // 1..10
    val nameKo: String,
    val emoji: String,
    val approxElo: Int,
    val description: String,
)

object AiLevels {
    val all: List<AiLevel> = listOf(
        AiLevel(1, "병아리", "🐣", 400, "기물을 자주 공짜로 내줘요. 첫 대국 상대로 좋아요."),
        AiLevel(2, "꼬마", "🐥", 550, "눈앞의 기물은 잡지만 아직 실수가 많아요."),
        AiLevel(3, "새싹", "🐤", 700, "기본 정석을 알고 한 수 앞을 내다보기 시작했어요."),
        AiLevel(4, "여우", "🦊", 850, "공짜 기물은 놓치지 않는 영리한 상대예요."),
        AiLevel(5, "늑대", "🐺", 1000, "간단한 전술을 노리며 날카롭게 달려들어요."),
        AiLevel(6, "부엉이", "🦉", 1200, "실수가 적고 몇 수 앞을 차분하게 계산해요."),
        AiLevel(7, "호랑이", "🐯", 1400, "전술에 강하고 작은 실수도 매섭게 파고들어요."),
        AiLevel(8, "사자", "🦁", 1600, "탄탄한 수읽기로 쉬지 않고 압박해요."),
        AiLevel(9, "드래곤", "🐉", 1800, "깊은 계산으로 좀처럼 빈틈을 보이지 않아요."),
        AiLevel(10, "그랜드마스터", "👑", 2000, "가장 강한 AI예요. 모든 수를 정확하게 계산해요."),
    )

    fun get(level: Int): AiLevel = all[level.coerceIn(1, 10) - 1]
}

/**
 * How a level plays. timeMs == 0: every root move is scored to [depth], then picked with gaussian [noise]
 * and, with [slipProb], a random move within [slipMargin] of the best. timeMs > 0: normal timed search
 * capped at [depth] with +-[noise] evaluation noise.
 */
private class Style(
    val depth: Int,
    val timeMs: Long,
    val noise: Int,
    val slipProb: Double,
    val slipMargin: Int,
    val bookProb: Double,
)

private const val ANY = 1_000_000

private val STYLES = arrayOf(
    Style(depth = 1, timeMs = 0, noise = 220, slipProb = 0.40, slipMargin = ANY, bookProb = 0.0),
    Style(depth = 1, timeMs = 0, noise = 120, slipProb = 0.22, slipMargin = ANY, bookProb = 0.0),
    Style(depth = 2, timeMs = 0, noise = 80, slipProb = 0.12, slipMargin = 500, bookProb = 0.5),
    Style(depth = 2, timeMs = 0, noise = 50, slipProb = 0.07, slipMargin = 350, bookProb = 0.6),
    Style(depth = 3, timeMs = 0, noise = 35, slipProb = 0.05, slipMargin = 250, bookProb = 0.7),
    Style(depth = 4, timeMs = 0, noise = 18, slipProb = 0.03, slipMargin = 150, bookProb = 0.75),
    Style(depth = 6, timeMs = 500, noise = 30, slipProb = 0.0, slipMargin = 0, bookProb = 0.8),
    Style(depth = 64, timeMs = 1000, noise = 12, slipProb = 0.0, slipMargin = 0, bookProb = 0.8),
    Style(depth = 64, timeMs = 1500, noise = 0, slipProb = 0.0, slipMargin = 0, bookProb = 0.85),
    Style(depth = 64, timeMs = 2500, noise = 0, slipProb = 0.0, slipMargin = 0, bookProb = 0.85),
)

/** Not thread-safe; use one instance per game/worker. */
class AiPlayer(val level: AiLevel, seed: Long = System.nanoTime()) {
    private val rng = java.util.Random(seed)
    private val style = STYLES[level.level.coerceIn(1, 10) - 1]
    private val engine = Engine(if (style.timeMs > 0) 19 else 16).also {
        if (style.timeMs > 0) {
            it.noiseAmp = style.noise
            it.noiseSalt = rng.nextLong()
        }
    }

    /** Test hook: caps the thinking time of timed levels. */
    internal var thinkCapMs = Long.MAX_VALUE

    /** Blocking. [playedUci] = moves from the standard start position (for the opening book). Requires a legal move. */
    fun chooseMove(position: Position, playedUci: List<String>, isCancelled: () -> Boolean = { false }): Move {
        val legal = position.legalMoves()
        require(legal.isNotEmpty()) { "No legal moves in ${position.fen()}" }
        if (legal.size == 1) return legal[0]
        if (style.bookProb > 0 && rng.nextDouble() < style.bookProb) {
            val lines = Book.linesFrom(position, playedUci)
            if (lines.isNotEmpty()) {
                val uci = lines[rng.nextInt(lines.size)].steps[playedUci.size].uci
                Move.fromUci(uci)?.takeIf { position.isLegal(it) }?.let { return it }
            }
        }
        if (style.timeMs > 0) {
            val r = engine.search(position, style.depth, min(style.timeMs, thinkCapMs), isCancelled)
            return r.bestMove ?: legal[rng.nextInt(legal.size)]
        }

        val window = max(style.slipMargin.coerceAtMost(4_000), 4 * style.noise)
        val scored = engine.scoreRootMoves(position, style.depth, window, isCancelled)
        val best = scored.maxBy { it.second }
        if (best.second >= MATE - MATE_BAND && level.level >= 4) return best.first
        if (rng.nextDouble() < style.slipProb) {
            val pool = scored.filter { best.second - it.second <= style.slipMargin }
            return pool[rng.nextInt(pool.size)].first
        }
        return scored.maxBy { it.second + rng.nextGaussian() * style.noise }.first
    }
}

internal object Book {
    /** Non-trap book lines that continue past [played], if [position] really is the start position + [played]. */
    fun linesFrom(position: Position, played: List<String>): List<Opening> {
        if (played.size >= 30) return emptyList()
        val lines = OpeningBook.all.filter { o ->
            o.category != OpeningBook.TRAPS && o.steps.size > played.size &&
                played.indices.all { o.steps[it].uci == played[it] }
        }
        if (lines.isEmpty()) return lines
        val replay = Position.start()
        for (u in played) {
            val m = Move.fromUci(u)
            if (m == null || !replay.isLegal(m)) return emptyList()
            replay.makeMove(m)
        }
        return if (replay.hash == position.hash) lines else emptyList()
    }
}
