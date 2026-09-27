package com.seobuk.chess.baduk.ai

import com.seobuk.chess.ai.Ko
import com.seobuk.chess.baduk.core.BadukGame
import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.PASS
import com.seobuk.chess.baduk.core.Points
import com.seobuk.chess.baduk.core.Stone

/** [point] may be PASS. [winRate] is for the side to move. */
data class BadukHint(val point: Int, val explanation: String, val winRate: Double)

data class BadukFeedback(val kind: Kind, val explanation: String, val better: Int?) {
    enum class Kind { GOOD, OK, MISTAKE, BLUNDER }
}

/** [ownership]: +1 = surely black .. -1 = surely white. [lead]: + = Black ahead, territory scoring. */
data class BadukEstimate(val ownership: FloatArray, val dead: Set<Int>, val lead: Double)

/** Korean explanations (해요체) for the player. Every call is blocking and uses its own engine. */
object BadukCoach {
    fun hint(game: BadukGame, thinkMs: Long = 1200, isCancelled: () -> Boolean = { false }): BadukHint? =
        if (game.isOver) null
        else hint(game.board.copy(), game.komi, game.captureGo, game.moves.lastOrNull() ?: PASS, thinkMs, isCancelled)

    /** [last]: the move that led to [board], if known. */
    internal fun hint(
        board: Board,
        komi: Double,
        captureGo: Boolean = false,
        last: Int = PASS,
        thinkMs: Long = 1200,
        isCancelled: () -> Boolean = { false },
        seed: Long = System.nanoTime(),
    ): BadukHint? {
        val s = BadukEngine(seed).search(board, komi, thinkMs, captureGo = captureGo, isCancelled = isCancelled, lastMove = last)
        if (isCancelled()) return null
        val text = if (s.best == PASS) "더 둘 곳이 없어요. 한 수 쉬어도 좋아요." else sentence(board, s.best, Form.HINT)
        return BadukHint(s.best, text, s.winRate)
    }

    /** One sentence about what the move of the side to move on [before] does. */
    fun describe(before: Board, point: Int): String {
        if (point == PASS) return "한 수 쉬었어요."
        if (!before.isLegal(point)) return "${Points.name(point, before.size)}에는 둘 수 없어요."
        return sentence(before, point, Form.PAST)
    }

    /** [game] is the game BEFORE [played]. */
    fun review(game: BadukGame, played: Int, thinkMs: Long = 900, isCancelled: () -> Boolean = { false }): BadukFeedback =
        review(game.board.copy(), played, game.komi, game.captureGo, game.moves.lastOrNull() ?: PASS, thinkMs, isCancelled)

    /** [board] is the position BEFORE [played]; [last]: the move that led to it, if known. */
    internal fun review(
        board: Board,
        played: Int,
        komi: Double,
        captureGo: Boolean = false,
        last: Int = PASS,
        thinkMs: Long = 900,
        isCancelled: () -> Boolean = { false },
        seed: Long = System.nanoTime(),
    ): BadukFeedback {
        val size = board.size
        val me = board.toMove
        val code = me.ordinal + 1
        val engine = BadukEngine(seed)
        val s = engine.search(board, komi, thinkMs * 3 / 5, captureGo = captureGo, isCancelled = isCancelled, lastMove = last)
        val better = s.best.takeIf { it != played }
        fun name(p: Int) = Points.name(p, size)

        if (played == PASS) {
            return if (s.best == PASS) BadukFeedback(BadukFeedback.Kind.GOOD, "더 둘 곳이 없어서 쉬는 것이 맞아요.", null)
            else BadukFeedback(BadukFeedback.Kind.MISTAKE, "아직 둘 곳이 남아 있어요. ${sentence(board, s.best, Form.MISSED)}", s.best)
        }
        val after = board.copy()
        if (!after.play(played)) return BadukFeedback(BadukFeedback.Kind.MISTAKE, describe(board, played), better)

        val seen = s.candidates.firstOrNull { it.point == played && it.visits >= 60 }
        val mine = seen?.winRate
            ?: if (captureGo && after.lastCaptureCount > 0) 1.0
            else 1.0 - engine.search(after, komi, thinkMs * 2 / 5, captureGo = captureGo, isCancelled = isCancelled, lastMove = played).winRate
        val drop = if (better == null) 0.0 else (s.winRate - mine).coerceAtLeast(0.0)
        var kind = when {
            drop < 0.03 -> BadukFeedback.Kind.GOOD
            drop < 0.10 -> BadukFeedback.Kind.OK
            drop < 0.22 -> BadukFeedback.Kind.MISTAKE
            else -> BadukFeedback.Kind.BLUNDER
        }
        fun atLeast(k: BadukFeedback.Kind) { if (k > kind) kind = k }
        val big = BadukFeedback.Kind.BLUNDER
        val small = BadukFeedback.Kind.MISTAKE
        val opp = colour(me.opposite)

        // Concrete facts first: they are what a beginner can act on. The search has to agree, though:
        // its own choice is never scolded and a recommended point must be its choice or clearly better.
        fun agreed(p: Int) = p == s.best ||
            (s.candidates.firstOrNull { it.point == p && it.visits >= 30 }?.winRate ?: 0.0) > mine + 0.03
        if (better != null) {
            val sign = if (me == Stone.BLACK) 1 else -1
            val owner = s.ownership[played] * sign
            // Walking into atari with stones that were not lost anyway; taking more stones than are
            // put at risk is a trade, not a slip.
            val selfAtari = if (after.liberties(played) == 1 && after.koPoint < 0) after.groupSize(played) else 0
            val joined = after.group(played).filter { it != played }
            val worth = joined.maxOfOrNull { s.ownership[it] * sign } ?: owner
            if (after.lastCaptureCount < selfAtari && worth > -DEAD_BELOW && (selfAtari >= 3 || selfAtari > 0 && drop >= 0.05)) {
                atLeast(if (selfAtari >= 3) big else small)
                val l = name(after.firstLiberty(played))
                return BadukFeedback(kind, "돌 ${selfAtari}개가 단수에 몰렸어요. 상대가 ${l}에 두면 잡혀요.", better)
            }
            if (board.isEyeLike(played, me)) {
                atLeast(small)
                return BadukFeedback(kind, "내 집을 스스로 메웠어요. 집이 줄고 돌이 약해져요.", better)
            }
            var left = 0 // own stones left in atari although they could be saved
            var leftAt = -1
            var missed = 0 // opponent stones that could have been taken
            var missedAt = -1
            for (p in 0 until size * size) {
                if (board.color(p) == Board.EMPTY || board.groupId(p) != p || board.liberties(p) != 1) continue
                val l = board.firstLiberty(p)
                val stones = board.groupSize(p)
                if (!board.isLegal(l)) continue
                if (board.color(p) == code) {
                    if (after.color(p) == code && after.liberties(p) == 1 && stones > left && rescueSize(board, l, code) > 0) {
                        left = stones
                        leftAt = l
                    }
                } else if (after.color(p) != Board.EMPTY && stones > missed) {
                    missed = stones
                    missedAt = l
                }
            }
            if (left > 0 && agreed(leftAt) && (left >= 3 || drop >= 0.08)) {
                atLeast(if (left >= 3) big else small)
                return BadukFeedback(kind, "단수에 몰린 돌 ${left}개를 그대로 두었어요. ${name(leftAt)}에 두면 살릴 수 있었어요.", leftAt)
            }
            if (missed > 0 && agreed(missedAt) && (missed >= 3 || drop >= 0.08)) {
                atLeast(small)
                return BadukFeedback(kind, "${name(missedAt)}에 두면 ${opp}돌 ${missed}개를 따낼 수 있었어요.", missedAt)
            }
            if (owner < -0.8f && drop >= 0.05 && after.lastCaptureCount == 0) {
                atLeast(small)
                return BadukFeedback(kind, "상대 집 안이라 살기 어려운 자리예요. 잡히면 상대 집만 늘어요.", better)
            }
            if (owner > 0.85f && size * size - board.emptyCount >= size * size / 3 && !captureGo) {
                atLeast(BadukFeedback.Kind.OK)
                return BadukFeedback(kind, "이미 내 집인 곳에 두었어요. 한 집 손해예요.", better)
            }
        }

        val what = sentence(board, played, Form.PAST)
        val text = when (kind) {
            BadukFeedback.Kind.GOOD -> "$what 좋은 수예요."
            BadukFeedback.Kind.OK -> if (better == null || better == PASS) "$what 무난한 수예요." else "$what ${Ko.iGa(name(better))} 조금 더 좋았어요."
            else -> if (better == null || better == PASS) "$what 아쉬운 수예요." else "아쉬운 수예요. ${sentence(board, better, Form.MISSED)}"
        }
        return BadukFeedback(kind, text, better.takeIf { kind != BadukFeedback.Kind.GOOD })
    }

    /** 형세 판단, also used to mark dead stones when the game ends. */
    fun estimate(board: Board, komi: Double, playouts: Int = 800): BadukEstimate = estimate(board, komi, playouts, System.nanoTime())

    internal fun estimate(board: Board, komi: Double, playouts: Int, seed: Long): BadukEstimate {
        val ownership = BadukEngine(seed).ownership(board, komi, playouts)
        val dead = deadStones(board, ownership)
        return BadukEstimate(ownership, dead, lead(board, ownership, dead, komi))
    }

    private fun colour(s: Stone) = if (s == Stone.BLACK) "흑" else "백"

    private enum class Form { PAST, HINT, MISSED }

    /** "D4에 두어 백돌 2개를 따냈어요." / "D4에 두면 .. 따낼 수 있어요." / "D4에 두었다면 .. 따낼 수 있었어요." */
    private fun sentence(before: Board, p: Int, form: Form): String {
        val size = before.size
        val geo = Geo.of(size)
        val nbr = before.grid.nbr
        val me = before.toMove.ordinal + 1
        val opp = colour(before.toMove.opposite)
        val name = Points.name(p, size)
        val after = before.copy().also { it.play(p) }

        // What stands next to the point before the move.
        val own = HashSet<Int>()
        val others = HashSet<Int>()
        var rescued = false
        var atari = 0
        val k = p * 4
        for (i in k until k + 4) {
            val q = nbr[i]
            if (q < 0) break
            when (before.color(q)) {
                Board.EMPTY -> {}
                me -> {
                    own.add(before.groupId(q))
                    if (before.liberties(q) == 1 && after.liberties(p) >= 2) rescued = true
                }
                else -> if (others.add(before.groupId(q)) && after.color(q) != Board.EMPTY &&
                    before.liberties(q) > 1 && after.liberties(q) == 1
                ) atari++
            }
        }
        var ownNear = 0
        var otherNear = 0
        var otherClose = false
        for (q in 0 until geo.n) {
            val c = before.color(q)
            val d = geo.distance(p, q)
            if (c == Board.EMPTY || d > 3) continue
            if (c == me) ownNear++ else {
                otherNear++
                if (d <= 2) otherClose = true
            }
        }
        val line = geo.line[p]
        val x = p % size
        val y = p / size
        val third = size / 3
        val inCorner = (x < third || x >= size - third) && (y < third || y >= size - third)

        // (said, can do, could have done)
        val tail: Triple<String, String, String> = when {
            after.koPoint >= 0 -> Triple("패를 따냈어요", "패를 따낼 수 있어요", "패를 따낼 수 있었어요")
            after.lastCaptureCount > 0 -> "${opp}돌 ${after.lastCaptureCount}개를".let {
                Triple("$it 따냈어요", "$it 따낼 수 있어요", "$it 따낼 수 있었어요")
            }
            after.liberties(p) == 1 -> Triple("스스로 단수에 들어갔어요", "스스로 단수에 들어가요", "스스로 단수에 들어갔을 거예요")
            rescued -> Triple("단수에 몰린 돌을 살렸어요", "단수에 몰린 돌을 살릴 수 있어요", "단수에 몰린 돌을 살릴 수 있었어요")
            atari >= 2 -> Triple("양단수를 쳤어요", "양단수를 칠 수 있어요", "양단수를 칠 수 있었어요")
            atari == 1 -> Triple("${opp}돌을 단수로 몰았어요", "${opp}돌을 단수로 몰 수 있어요", "${opp}돌을 단수로 몰 수 있었어요")
            own.size >= 2 -> Triple("돌을 이었어요", "돌을 이을 수 있어요", "돌을 이을 수 있었어요")
            others.size >= 2 -> Triple("${opp}돌 사이를 끊었어요", "${opp}돌 사이를 끊을 수 있어요", "${opp}돌 사이를 끊을 수 있었어요")
            makesTigerMouth(after, p, me) ->
                Triple("호구 모양으로 끊는 점을 지켰어요", "호구 모양으로 끊는 점을 지킬 수 있어요", "호구 모양으로 끊는 점을 지킬 수 있었어요")
            own.isNotEmpty() && others.isNotEmpty() -> Triple("${opp}돌을 막았어요", "${opp}돌을 막을 수 있어요", "${opp}돌을 막을 수 있었어요")
            others.isNotEmpty() -> Triple("${opp}돌에 붙였어요", "${opp}돌을 압박할 수 있어요", "${opp}돌을 압박할 수 있었어요")
            own.isNotEmpty() && otherClose -> Triple("돌을 늘어 튼튼하게 했어요", "돌이 튼튼해져요", "돌이 튼튼해졌을 거예요")
            ownNear == 0 && otherNear >= 2 -> Triple("$opp 진영에 침입했어요", "$opp 진영을 깨뜨릴 수 있어요", "$opp 진영을 깨뜨릴 수 있었어요")
            ownNear > 0 && line <= 1 -> Triple("집의 경계를 지켰어요", "집의 경계를 지킬 수 있어요", "집의 경계를 지킬 수 있었어요")
            ownNear > 0 -> Triple("집을 넓혔어요", "집을 넓힐 수 있어요", "집을 넓힐 수 있었어요")
            otherNear > 0 -> Triple("${opp}돌에 다가섰어요", "${opp}돌을 견제할 수 있어요", "${opp}돌을 견제할 수 있었어요")
            line == 0 -> Triple("", "", "")
            inCorner -> Triple("귀를 차지했어요", "귀를 차지할 수 있어요", "귀를 차지할 수 있었어요")
            line <= 3 -> Triple("변을 차지했어요", "변을 차지할 수 있어요", "변을 차지할 수 있었어요")
            else -> Triple("중앙을 차지했어요", "중앙을 차지할 수 있어요", "중앙을 차지할 수 있었어요")
        }
        if (tail.first.isEmpty()) return when (form) {
            Form.PAST -> "${name}에 두었어요."
            Form.HINT -> "${Ko.iGa(name)} 좋은 자리예요."
            Form.MISSED -> "${Ko.iGa(name)} 더 좋은 자리였어요."
        }
        return when (form) {
            Form.PAST -> "${name}에 두어 ${tail.first}."
            Form.HINT -> "${name}에 두면 ${tail.second}."
            Form.MISSED -> "${name}에 두었다면 ${tail.third}."
        }
    }

    /** The new stone at [p] guards an empty point next to it: own stones on every side of that point but one. */
    private fun makesTigerMouth(after: Board, p: Int, me: Int): Boolean {
        val nbr = after.grid.nbr
        val k = p * 4
        for (j in k until k + 4) {
            val m = nbr[j]
            if (m < 0) break
            if (after.color(m) != Board.EMPTY) continue
            var mine = 0
            var open = 0
            val km = m * 4
            for (t in km until km + 4) {
                val q = nbr[t]
                if (q < 0) break
                if (after.color(q) == me) mine++ else if (after.color(q) == Board.EMPTY) open++ else open += 9
            }
            if (mine >= 2 && open == 1) return true
        }
        return false
    }
}
