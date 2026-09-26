package com.seobuk.chess.ai

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import com.seobuk.chess.core.Squares
import com.seobuk.chess.learn.OpeningBook
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MoveQuality(val labelKo: String, val symbol: String) {
    BEST("최선", "★"),
    EXCELLENT("훌륭함", "!"),
    GOOD("좋음", "✓"),
    BOOK("정석", "정"),
    INACCURACY("부정확", "?!"),
    MISTAKE("실수", "?"),
    BLUNDER("블런더", "??"),
}

/** Evals are WHITE POV and may be mate scores (see [isMateScore]); cpLoss is mover POV, >= 0 (see [Coach.norm]). */
data class MoveFeedback(
    val move: Move,
    val san: String,
    val quality: MoveQuality,
    val bestMove: Move?,
    val bestSan: String?,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val cpLoss: Int,
    val explanation: String,
)

data class Hint(val move: Move, val san: String, val explanation: String, val evalCp: Int /* WHITE POV */)

object Coach {
    // Coach may be called from several coroutines at once: each call checks an engine out, so usually only one
    // or two exist and their TTs stay warm across moves, whichever worker thread runs the call.
    private val engines = ConcurrentLinkedQueue<Engine>()

    private inline fun <T> withEngine(block: (Engine) -> T): T {
        val e = engines.poll() ?: Engine(17)
        try {
            return block(e)
        } finally {
            engines.offer(e)
        }
    }

    /** Best move for the side to move with a short reason; null if there is no legal move. */
    fun hint(position: Position, thinkMs: Long = 800, isCancelled: () -> Boolean = { false }): Hint? {
        val r = withEngine { it.search(position, timeLimitMs = thinkMs, isCancelled = isCancelled) }
        val move = r.bestMove ?: return null
        val text = StringBuilder(describeMove(position, move))
        val mateIn = mateInMoves(r.scoreCp)
        if (mateIn != null && mateIn > 1) {
            text.append(" ${mateIn}수 안에 메이트할 수 있어요.")
        } else if (mateIn == null && r.pv.size >= 3) {
            followUp(position, r.pv)?.let { text.append(' ').append(it) }
        }
        return Hint(move, position.san(move), text.toString(), whitePov(position.sideToMove, r.scoreCp))
    }

    /** Judges [played] from [before]; [playedUciBefore] = moves from the start position (for BOOK detection). */
    fun review(
        before: Position,
        played: Move,
        playedUciBefore: List<String>,
        thinkMs: Long = 700,
        isCancelled: () -> Boolean = { false },
    ): MoveFeedback = withEngine { eng ->
        require(before.isLegal(played)) { "Illegal move ${played.uci()} in ${before.fen()}" }
        val mover = before.sideToMove
        val san = before.san(played)
        val rb = eng.search(before, timeLimitMs = max(1, thinkMs * 55 / 100), isCancelled = isCancelled)
        val best = rb.bestMove
        val bestScore = rb.scoreCp
        val after = before.copy().also { it.makeMove(played) }
        val playedScore: Int
        val reply: List<Move> // expected opponent reply line after the played move
        if (played == best) {
            playedScore = bestScore
            reply = rb.pv.drop(1)
        } else {
            val ra = eng.search(after, max(1, rb.depth - 1), max(1, thinkMs * 45 / 100), isCancelled)
            playedScore = -ra.scoreCp
            reply = ra.pv
        }
        val cpLoss = if (played == best) 0 else max(0, norm(bestScore) - norm(playedScore))
        val bestSan = best?.let { before.san(it) }

        val book = Book.linesFrom(before, playedUciBefore).filter { it.steps[playedUciBefore.size].uci == played.uci() }
        val missedMate = bestScore >= MATE - MATE_BAND && playedScore < MATE - MATE_BAND
        val allowsMate = playedScore <= -(MATE - MATE_BAND) && bestScore > -(MATE - MATE_BAND)
        val quality = when {
            book.isNotEmpty() -> MoveQuality.BOOK
            played == best -> MoveQuality.BEST
            allowsMate -> MoveQuality.BLUNDER
            // Missing a mate while still clearly winning is a lesson, not a disaster.
            else -> byLoss(cpLoss).let { if (missedMate && playedScore >= 500 && it > MoveQuality.MISTAKE) MoveQuality.MISTAKE else it }
        }

        val explanation = when (quality) {
            MoveQuality.BOOK -> {
                val line = playedUciBefore + played.uci()
                val named = OpeningBook.detect(line)?.takeIf { it in book }
                val comment = (named ?: book.first()).steps[playedUciBefore.size].comment
                if (named != null) "${named.nameKo}의 정석 수예요. $comment" else "오프닝 정석 수예요. $comment"
            }
            MoveQuality.BEST -> "최선의 수예요. ${describeMove(before, played)}"
            MoveQuality.EXCELLENT -> "훌륭한 수예요. ${describeMove(before, played)}"
            MoveQuality.GOOD -> "좋은 수예요. ${describeMove(before, played)}"
            else -> badMoveText(quality, before, after, best, bestSan, bestScore, playedScore, cpLoss, reply)
        }
        MoveFeedback(
            move = played,
            san = san,
            quality = quality,
            bestMove = best,
            bestSan = bestSan,
            evalBeforeCp = whitePov(mover, bestScore),
            evalAfterCp = whitePov(mover, playedScore),
            cpLoss = cpLoss,
            explanation = explanation,
        )
    }

    /** Short search, WHITE POV; mates are +-(MATE - plies). */
    fun evaluateWhite(position: Position, thinkMs: Long = 300, isCancelled: () -> Boolean = { false }): Int =
        whitePov(position.sideToMove, withEngine { it.search(position, timeLimitMs = thinkMs, isCancelled = isCancelled) }.scoreCp)

    /** Neutral Korean description of what [move] does, e.g. "나이트를 전개하며 중앙(e5)을 노려요." */
    fun describeMove(before: Position, move: Move): String {
        val piece = before.pieceAt(move.from) ?: return ""
        if (!before.isLegal(move)) return ""
        val us = piece.side
        val them = us.opposite
        val name = nameOf(piece.type)
        val to = Squares.name(move.to)
        val after = before.copy().also { it.makeMove(move) }
        val check = after.isInCheck()
        if (check && !after.hasLegalMove()) return "체크메이트예요. ${Ko.ro(name)} 상대 킹을 완전히 가둬 게임을 끝내요."

        val captured: Piece? = before.pieceAt(move.to)
            ?: if (before.isCapture(move)) Piece(PieceType.PAWN, them) else null
        val main: String? = when {
            piece.type == PieceType.KING && abs(move.to - move.from) == 2 ->
                "캐슬링으로 킹을 안전한 곳에 숨기고 룩을 싸움에 참여시켜요."
            move.promotion != null -> {
                val promo = nameOf(move.promotion)
                if (captured != null) "폰이 ${Ko.eulReul(nameOf(captured.type))} 잡으며 ${to}에서 ${Ko.ro(promo)} 승진해요."
                else "폰이 ${to}에 도착해 ${Ko.ro(promo)} 승진해요."
            }
            captured != null -> {
                val cap = Ko.eulReul(nameOf(captured.type))
                val diff = captured.type.value - piece.type.value
                val lead = "${Ko.ro(name)} ${to}의 $cap"
                when {
                    piece.type == PieceType.PAWN && before.pieceAt(move.to) == null -> "앙파상으로 상대 폰을 잡아요."
                    !after.isSquareAttacked(move.to, them) -> "$lead 공짜로 잡아요."
                    piece.type == PieceType.KING -> "$lead 잡아요."
                    diff > 50 -> "$lead 잡아 이득을 봐요."
                    diff >= -50 -> "$lead 잡아 같은 가치의 기물을 교환해요."
                    else -> "$lead 잡지만 되잡힐 수 있어요."
                }
            }
            else -> null
        }

        // What the moved piece attacks now; nothing, if it can simply be taken.
        val targets = if (isThreatened(after, move.to, us)) emptyList() else attacksFrom(after, move.to).mapNotNull { sq ->
            val t = after.pieceAt(sq) ?: return@mapNotNull null
            if (t.side != them) return@mapNotNull null
            // A king can only win undefended pieces, whatever their value.
            val worth = t.type == PieceType.KING || (piece.type != PieceType.KING && t.type.value > piece.type.value) ||
                !after.isSquareAttacked(sq, them)
            if (worth) sq to t else null
        }
        val forked = targets.filter { it.second.type != PieceType.PAWN }
        val tactic: String? = when {
            forked.size >= 2 -> {
                val (a, b) = forked.sortedByDescending { valueOf(it.second.type) }
                val an = nameOf(a.second.type)
                val bn = nameOf(b.second.type)
                "${Ko.iGa(name)} ${Ko.wa(an)} ${Ko.eulReul(bn)} 동시에 노리는 포크예요."
            }
            check -> "체크로 상대 킹을 몰아붙여요."
            targets.size == 1 && captured == null -> {
                val (sq, t) = targets[0]
                "${Squares.name(sq)}의 ${Ko.eulReul(nameOf(t.type))} 공격해요."
            }
            else -> null
        }
        if (main != null) return if (tactic != null) "$main $tactic" else main

        val quiet: String = when {
            isThreatened(before, move.from, us) && !isThreatened(after, move.to, us) ->
                "공격받던 ${Ko.eulReul(name)} ${Ko.ro(to)} 안전하게 피신시켜요."
            piece.type == PieceType.PAWN && isPassed(after, move.to, us) ->
                "패스드 폰을 ${to}까지 밀어 승진을 노려요."
            (piece.type == PieceType.KNIGHT || piece.type == PieceType.BISHOP) && before.fullmoveNumber <= 12 &&
                Squares.rank(move.from) == (if (us == Side.WHITE) 0 else 7) -> {
                val center = attacksFrom(after, move.to).firstOrNull { it in CENTER }
                if (center != null) "${Ko.eulReul(name)} 전개하며 중앙(${Squares.name(center)})을 노려요."
                else "${Ko.eulReul(name)} ${Ko.ro(to)} 전개해 싸움에 참여시켜요."
            }
            piece.type == PieceType.PAWN && move.to in CENTER -> "폰으로 중앙(${to})을 차지해요."
            piece.type == PieceType.PAWN && Squares.file(move.to) in 2..5 && before.fullmoveNumber <= 12 ->
                "폰을 ${to}에 두어 중앙 싸움을 도와요."
            piece.type == PieceType.ROOK && Squares.file(move.from) != Squares.file(move.to) &&
                isOpenFile(after, Squares.file(move.to)) ->
                "룩을 열린 ${'a' + Squares.file(move.to)}파일로 옮겨 힘을 발휘하게 해요."
            piece.type == PieceType.KING && isEndgame(after) && centerDist(move.to) < centerDist(move.from) -> "킹을 ${Ko.ro(to)} 옮겨 적극적으로 싸움에 참여시켜요."
            else -> "${Ko.eulReul(name)} ${Ko.ro(to)} 옮겨요."
        }
        return if (tactic != null) "$quiet $tactic" else quiet
    }

    /** Lichess-style accuracy 0..100 from centipawn losses (each move's before-eval assumed ~0). */
    fun accuracy(cpLosses: List<Int>): Int {
        if (cpLosses.isEmpty()) return 100
        val avg = cpLosses.map { loss ->
            val drop = winPct(0) - winPct(-loss.coerceIn(0, 1000))
            (103.1668 * exp(-0.04354 * drop) - 3.1669).coerceIn(0.0, 100.0)
        }.average()
        return avg.roundToInt().coerceIn(0, 100)
    }

    // ---- helpers ----

    private val CENTER = setOf(27, 28, 35, 36) // d4 e4 d5 e5

    private fun winPct(cp: Int): Double = 50 + 50 * (2 / (1 + exp(-0.00368208 * cp)) - 1)

    private fun whitePov(stm: Side, score: Int) = if (stm == Side.WHITE) score else -score

    /**
     * Mover-POV value for loss math. Evals count up to +-20 pawns, so hanging a rook at +18 still costs 5 pawns;
     * a mate in n plies is worth 2500 - 25n (never below 2000), so missing a short mate or walking into a faster
     * one costs something too.
     */
    internal fun norm(score: Int): Int = when {
        score >= MATE - MATE_BAND -> 2500 - 25 * min(MATE - score, 20)
        score <= -(MATE - MATE_BAND) -> -(2500 - 25 * min(MATE + score, 20))
        else -> score.coerceIn(-2000, 2000)
    }

    /** Chebyshev distance from the centre, doubled: 1 for d4/e4/d5/e5, 7 on the edge. */
    private fun centerDist(sq: Int) = max(abs(2 * Squares.file(sq) - 7), abs(2 * Squares.rank(sq) - 7))

    private fun byLoss(loss: Int) = when {
        loss <= 10 -> MoveQuality.BEST
        loss <= 30 -> MoveQuality.EXCELLENT
        loss <= 80 -> MoveQuality.GOOD
        loss <= 150 -> MoveQuality.INACCURACY
        loss <= 300 -> MoveQuality.MISTAKE
        else -> MoveQuality.BLUNDER
    }

    private fun badMoveText(
        quality: MoveQuality,
        before: Position,
        after: Position,
        best: Move?,
        bestSan: String?,
        bestScore: Int,
        playedScore: Int,
        cpLoss: Int,
        reply: List<Move>,
    ): String {
        val better = if (best != null && bestSan != null) " 더 좋은 수: $bestSan." + describeMove(before, best).let { if (it.isEmpty()) "" else " $it" } else ""
        if (playedScore <= -(MATE - MATE_BAND)) {
            val first = reply.firstOrNull()?.takeIf { after.isLegal(it) }
            val n = -(mateInMoves(playedScore) ?: -1)
            val problem = if (n == 1 && first != null) "상대가 ${Ko.ro(after.san(first))} 바로 체크메이트할 수 있어요."
            else "이 수 뒤에는 상대가 ${n}수 안에 메이트할 수 있어요."
            return problem + better
        }
        if (bestScore >= MATE - MATE_BAND && bestSan != null) {
            val n = mateInMoves(bestScore) ?: 1
            return if (n == 1) "${Ko.ro(bestSan)} 바로 체크메이트할 수 있었어요."
            else "${Ko.ro(bestSan)} 시작하면 ${n}수 만에 메이트할 수 있었어요."
        }
        val r = reply.firstOrNull()?.takeIf { after.isLegal(it) && after.isCapture(it) }
        if (r != null) {
            val victim = after.pieceAt(r.to)?.type ?: PieceType.PAWN
            val attacker = after.pieceAt(r.from)!!.type
            val problem = "상대 ${Ko.iGa(nameOf(attacker))} ${Squares.name(r.to)}의 ${Ko.eulReul(nameOf(victim))} 잡을 수 있어요."
            return problem + better
        }
        if (best != null && bestSan != null && before.isCapture(best)) {
            val victim = before.pieceAt(best.to)?.type ?: PieceType.PAWN
            return "${Ko.ro(bestSan)} ${Squares.name(best.to)}의 ${Ko.eulReul(nameOf(victim))} 잡을 수 있었어요."
        }
        val pawns = (cpLoss + 5) / 10 / 10.0 // one decimal, locale-independent
        val problem = when (quality) {
            MoveQuality.INACCURACY -> "조금 아쉬운 수예요."
            MoveQuality.MISTAKE -> "형세가 약 ${pawns}폰만큼 나빠진 실수예요."
            else -> "큰 실수예요. 형세가 약 ${pawns}폰만큼 기울었어요."
        }
        return problem + better
    }

    /** "이어서 Bxf7+로 f7의 폰을 노릴 수 있어요." when the engine's line wins something on our next move. */
    private fun followUp(position: Position, pv: List<Move>): String? {
        val p = position.copy()
        for (i in 0..1) {
            if (!p.isLegal(pv[i])) return null
            p.makeMove(pv[i])
        }
        val next = pv[2]
        if (!p.isLegal(next) || !p.isCapture(next)) return null
        val victim = p.pieceAt(next.to)?.type ?: PieceType.PAWN
        return "이어서 ${Ko.ro(p.san(next))} ${Squares.name(next.to)}의 ${Ko.eulReul(nameOf(victim))} 노릴 수 있어요."
    }

    private fun nameOf(t: PieceType) = when (t) {
        PieceType.PAWN -> "폰"
        PieceType.KNIGHT -> "나이트"
        PieceType.BISHOP -> "비숍"
        PieceType.ROOK -> "룩"
        PieceType.QUEEN -> "퀸"
        PieceType.KING -> "킹"
    }

    private fun valueOf(t: PieceType) = if (t == PieceType.KING) 10_000 else t.value

    /** Attacked by the opponent and either undefended or attackable by something cheaper. */
    private fun isThreatened(p: Position, sq: Int, owner: Side): Boolean {
        val piece = p.pieceAt(sq) ?: return false
        if (piece.type == PieceType.KING || !p.isSquareAttacked(sq, owner.opposite)) return false
        if (!p.isSquareAttacked(sq, owner)) return true
        val cheapest = (0 until 64).filter { s -> p.pieceAt(s)?.side == owner.opposite && sq in attacksFrom(p, s) }
            .minOfOrNull { valueOf(p.pieceAt(it)!!.type) } ?: return false
        return cheapest < piece.type.value
    }

    private fun isPassed(p: Position, sq: Int, side: Side): Boolean {
        val f = Squares.file(sq)
        val r = Squares.rank(sq)
        for (s in 0 until 64) {
            val x = p.pieceAt(s) ?: continue
            if (x.type != PieceType.PAWN || x.side == side || abs(Squares.file(s) - f) > 1) continue
            if (if (side == Side.WHITE) Squares.rank(s) > r else Squares.rank(s) < r) return false
        }
        return true
    }

    private fun isOpenFile(p: Position, file: Int) = (0 until 8).none { p.pieceAt(it * 8 + file)?.type == PieceType.PAWN }

    private fun isEndgame(p: Position) = (0 until 64).count {
        val t = p.pieceAt(it)?.type
        t != null && t != PieceType.PAWN && t != PieceType.KING
    } <= 4

    private val KNIGHT_D = arrayOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
    private val KING_D = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
    private val ROOK_D = KING_D.copyOfRange(0, 4)
    private val BISHOP_D = KING_D.copyOfRange(4, 8)

    /** Squares attacked by the piece on [sq] (including occupied ones it hits). */
    private fun attacksFrom(p: Position, sq: Int): List<Int> {
        val piece = p.pieceAt(sq) ?: return emptyList()
        val f = Squares.file(sq)
        val r = Squares.rank(sq)
        val out = ArrayList<Int>(16)
        fun step(d: Array<Pair<Int, Int>>) = d.forEach { (df, dr) ->
            if (f + df in 0..7 && r + dr in 0..7) out.add((r + dr) * 8 + f + df)
        }
        fun slide(d: Array<Pair<Int, Int>>) = d.forEach { (df, dr) ->
            var ff = f + df
            var rr = r + dr
            while (ff in 0..7 && rr in 0..7) {
                out.add(rr * 8 + ff)
                if (p.pieceAt(rr * 8 + ff) != null) break
                ff += df; rr += dr
            }
        }
        when (piece.type) {
            PieceType.PAWN -> {
                val dr = if (piece.side == Side.WHITE) 1 else -1
                step(arrayOf(-1 to dr, 1 to dr))
            }
            PieceType.KNIGHT -> step(KNIGHT_D)
            PieceType.KING -> step(KING_D)
            PieceType.BISHOP -> slide(BISHOP_D)
            PieceType.ROOK -> slide(ROOK_D)
            PieceType.QUEEN -> slide(KING_D)
        }
        return out
    }
}

/** Korean particle selection (받침-aware) for words, SAN and square names. */
internal object Ko {
    // Jongseong index of the word's last syllable as read aloud: 0 none, 8 = ㄹ.
    private fun jong(word: String): Int {
        var w = word.trimEnd('+', '#', '!', '?', ' ', '.')
        if (w.endsWith(")")) w = w.substringBeforeLast('(')
        val c = w.lastOrNull() ?: return 0
        return when {
            c in '가'..'힣' -> (c - '가') % 28
            c.isDigit() -> intArrayOf(21, 8, 0, 16, 0, 0, 1, 8, 8, 0)[c - '0'] // 영 일 이 삼 사 오 육 칠 팔 구
            else -> when (c.lowercaseChar()) {
                'l', 'r' -> 8
                'm' -> 16
                'n' -> 4
                else -> 0
            }
        }
    }

    fun iGa(w: String) = w + if (jong(w) != 0) "이" else "가"
    fun eulReul(w: String) = w + if (jong(w) != 0) "을" else "를"
    fun wa(w: String) = w + if (jong(w) != 0) "과" else "와"
    fun ro(w: String) = w + jong(w).let { if (it != 0 && it != 8) "으로" else "로" }
}
