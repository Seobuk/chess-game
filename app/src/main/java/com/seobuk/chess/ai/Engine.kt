package com.seobuk.chess.ai

import com.seobuk.chess.core.Move
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Mate score: mate in n plies scores MATE - n (side-to-move POV); being mated in n plies scores -(MATE - n). */
const val MATE = 100_000

/** True for scores that encode a forced mate. */
fun isMateScore(score: Int): Boolean = abs(score) >= MATE - MATE_BAND

/** Full moves until mate for a mate score (positive: the side the score belongs to mates), null otherwise. */
fun mateInMoves(score: Int): Int? = when {
    score >= MATE - MATE_BAND -> (MATE - score + 1) / 2
    score <= -(MATE - MATE_BAND) -> -((MATE + score + 1) / 2)
    else -> null
}

internal const val MATE_BAND = 500

data class SearchResult(
    val bestMove: Move?,
    val scoreCp: Int, // side-to-move POV
    val depth: Int,
    val pv: List<Move>,
    val nodes: Long,
)

private const val INF = MATE + 1000
private const val MAX_PLY = 100
private const val EXACT = 0
private const val LOWER = 1
private const val UPPER = 2
private const val PAWN_T = 1
private const val QUEEN_PROMO = 4 // PieceType.QUEEN.ordinal

private object SearchAborted : RuntimeException() {
    private fun readResolve(): Any = SearchAborted
    override fun fillInStackTrace(): Throwable = this
}

/**
 * Alpha-beta searcher: iterative deepening, PVS, aspiration windows, TT, null move, light LMR,
 * check extension, quiescence. Not thread-safe; searches a private copy of the position.
 */
class Engine(ttBits: Int = 19) {
    private val ttMask = (1 shl ttBits) - 1
    private val ttKey = LongArray(1 shl ttBits)
    private val ttInfo = IntArray(1 shl ttBits) // move | depth shl 16 | flag shl 24
    private val ttScore = IntArray(1 shl ttBits)

    private var pos: Position = Position.start()
    private val moveBuf = Array(MAX_PLY) { IntArray(256) }
    private val orderBuf = Array(MAX_PLY) { IntArray(256) }
    private val killers = IntArray(MAX_PLY * 2)
    private val history = IntArray(2 * 64 * 64)
    private val pvTable = IntArray(MAX_PLY * MAX_PLY)
    private val pvLen = IntArray(MAX_PLY + 1)
    private var nodes = 0L
    private var deadline = Long.MAX_VALUE
    private var timeAbortAllowed = false
    private var cancelled: () -> Boolean = { false }
    private var rootPreferred = 0

    /** Uniform eval noise in +-[noiseAmp] cp, fixed per position (used to weaken AI levels). */
    internal var noiseAmp = 0
    internal var noiseSalt = 0L

    /** Static evaluation (no search), side-to-move POV. */
    fun evaluate(position: Position): Int = Eval.evaluate(position)

    fun search(
        position: Position,
        maxDepth: Int = 64,
        timeLimitMs: Long = 1000,
        isCancelled: () -> Boolean = { false },
    ): SearchResult {
        val start = System.nanoTime()
        prepare(position, isCancelled)
        val limitNs = timeLimitMs.coerceAtLeast(1) * 1_000_000
        deadline = start + limitNs
        val legal = pos.legalMoves()
        if (legal.isEmpty()) return SearchResult(null, if (pos.isInCheck()) -MATE else 0, 0, emptyList(), 0)

        var bestMove = legal[0]
        var bestScore = 0
        var bestPv = listOf(bestMove)
        var completed = 0
        rootPreferred = 0
        for (depth in 1..maxDepth.coerceIn(1, MAX_PLY - 20)) {
            timeAbortAllowed = depth > 1
            val score = try {
                aspiration(depth, bestScore)
            } catch (_: SearchAborted) {
                break
            }
            completed = depth
            bestScore = score
            if (pvLen[0] > 0) {
                rootPreferred = pvTable[0]
                bestMove = Move.decode(pvTable[0])
                bestPv = List(pvLen[0]) { Move.decode(pvTable[it]) }
            }
            if (abs(score) >= MATE - depth) break // mate within the horizon: deeper won't change it
            if (System.nanoTime() - start > limitNs / 2) break // next iteration would not finish
        }
        return SearchResult(bestMove, bestScore, completed, bestPv, nodes)
    }

    /**
     * Scores every legal root move (side-to-move POV) at [depth]. Moves more than [window] below the best
     * only get an upper bound. On cancellation, returns the scores gathered so far.
     */
    internal fun scoreRootMoves(
        position: Position,
        depth: Int,
        window: Int,
        isCancelled: () -> Boolean = { false },
    ): List<Pair<Move, Int>> {
        prepare(position, isCancelled)
        deadline = Long.MAX_VALUE
        timeAbortAllowed = false
        val root = pos.legalMoves().map { Move.encode(it) }
        val scores = IntArray(root.size)
        try {
            for (d in 1..max(1, depth)) {
                var best = -INF
                for (i in root.indices.sortedByDescending { scores[it] }) {
                    pos.tryMakeMove(root[i])
                    val alpha = if (best == -INF) -INF else best - window
                    scores[i] = -negamax(d - 1, -INF, -alpha, 1, true)
                    pos.unmakeMove()
                    if (scores[i] > best) best = scores[i]
                }
            }
        } catch (_: SearchAborted) {
            // keep partial scores
        }
        return root.mapIndexed { i, code -> Move.decode(code) to scores[i] }
    }

    private fun prepare(position: Position, isCancelled: () -> Boolean) {
        pos = position.copy()
        cancelled = isCancelled
        nodes = 0
        killers.fill(0)
        history.fill(0)
    }

    private fun aspiration(depth: Int, prev: Int): Int {
        if (depth >= 4 && abs(prev) < MATE - MATE_BAND) {
            val a = prev - 40
            val b = prev + 40
            val s = negamax(depth, a, b, 0, true)
            if (s > a && s < b) return s
        }
        return negamax(depth, -INF, INF, 0, true)
    }

    private fun checkAbort() {
        if ((nodes and 2047L) == 0L && (cancelled() || (timeAbortAllowed && System.nanoTime() >= deadline))) {
            throw SearchAborted
        }
    }

    private fun staticEval(): Int {
        val e = Eval.evaluate(pos)
        if (noiseAmp == 0) return e
        val h = (pos.hash xor noiseSalt) * -0x61c8864680b583ebL
        return e + ((h ushr 33).toInt() % (2 * noiseAmp + 1)) - noiseAmp
    }

    private fun ttIndex(key: Long): Int = (key xor (key ushr 32)).toInt() and ttMask

    private fun negamax(depthIn: Int, alphaIn: Int, betaIn: Int, ply: Int, allowNull: Boolean): Int {
        pvLen[ply] = 0
        val p = pos
        val inCheck = p.isInCheck()
        // Fifty-move draw, before the leaf cut-off so quiet leaves count too; a mate on the 100th ply still wins.
        if (ply > 0 && p.halfmoveClock >= 100) return if (inCheck && !p.hasLegalMove()) -MATE + ply else 0
        val depth = if (inCheck) depthIn + 1 else depthIn
        if (depth <= 0) return quiesce(alphaIn, betaIn, ply)
        nodes++
        checkAbort()
        var alpha = alphaIn
        var beta = betaIn
        if (ply > 0) {
            if (p.repetitionCount() >= 2 || p.isInsufficientMaterial()) return 0
            alpha = max(alpha, -MATE + ply)
            beta = min(beta, MATE - ply - 1)
            if (alpha >= beta) return alpha
            if (ply >= MAX_PLY - 4) return staticEval()
        }
        val pvNode = beta - alpha > 1

        val key = p.hash
        val ti = ttIndex(key)
        var ttMove = 0
        if (ttKey[ti] == key) {
            val info = ttInfo[ti]
            ttMove = info and 0xFFFF
            if (ply > 0 && !pvNode && ((info shr 16) and 0xFF) >= depth) {
                val s = fromTt(ttScore[ti], ply)
                when (info shr 24) {
                    EXACT -> return s
                    LOWER -> if (s >= beta) return s
                    UPPER -> if (s <= alpha) return s
                }
            }
        }
        if (ply == 0 && rootPreferred != 0) ttMove = rootPreferred

        if (!pvNode && !inCheck && ply > 0 && abs(beta) < MATE - MATE_BAND) {
            val se = staticEval()
            // Reverse futility: far above beta near the leaves.
            if (depth <= 3 && se - 110 * depth >= beta) return se
            if (allowNull && depth >= 3 && se >= beta && hasPieces(p)) {
                val r = if (depth > 6) 3 else 2
                p.makeNullMove()
                val s = -negamax(depth - 1 - r, -beta, -beta + 1, ply + 1, false)
                p.unmakeNullMove()
                if (s >= beta) return beta
            }
        }

        val moves = moveBuf[ply]
        val order = orderBuf[ply]
        val n = p.generateMoves(moves)
        scoreMoves(moves, order, n, ply, ttMove)
        val k1 = killers[ply * 2]
        val k2 = killers[ply * 2 + 1]
        var legal = 0
        var bestScore = -INF
        var bestMove = 0
        for (i in 0 until n) {
            pickNext(moves, order, i, n)
            val m = moves[i]
            val quiet = order[i] < 800_000 || m == k1 || m == k2
            if (!p.tryMakeMove(m)) continue
            legal++
            val givesCheck = p.isInCheck()
            var score: Int
            if (legal == 1) {
                score = -negamax(depth - 1, -beta, -alpha, ply + 1, true)
            } else {
                val r = if (depth >= 3 && legal > 3 && quiet && m != k1 && m != k2 && !inCheck && !givesCheck) {
                    if (legal > 10 && !pvNode) 2 else 1
                } else 0
                score = -negamax(depth - 1 - r, -alpha - 1, -alpha, ply + 1, true)
                if (score > alpha && r > 0) score = -negamax(depth - 1, -alpha - 1, -alpha, ply + 1, true)
                if (score > alpha && score < beta) score = -negamax(depth - 1, -beta, -alpha, ply + 1, true)
            }
            p.unmakeMove()
            if (score > bestScore) {
                bestScore = score
                bestMove = m
                if (score > alpha) {
                    alpha = score
                    pvTable[ply * MAX_PLY] = m
                    val childLen = pvLen[ply + 1]
                    System.arraycopy(pvTable, (ply + 1) * MAX_PLY, pvTable, ply * MAX_PLY + 1, childLen)
                    pvLen[ply] = childLen + 1
                    if (score >= beta) {
                        if (quiet) {
                            if (m != k1) {
                                killers[ply * 2 + 1] = k1
                                killers[ply * 2] = m
                            }
                            val hi = (if (p.sideToMove == Side.WHITE) 0 else 4096) or (m and 4095)
                            history[hi] += depth * depth
                            if (history[hi] > 400_000) for (j in history.indices) history[j] = history[j] shr 1
                        }
                        break
                    }
                }
            }
        }
        if (legal == 0) return if (inCheck) -MATE + ply else 0

        val flag = when {
            bestScore >= beta -> LOWER
            bestScore > alphaIn -> EXACT
            else -> UPPER
        }
        ttKey[ti] = key
        ttInfo[ti] = (if (flag == UPPER) ttMove else bestMove) or (min(depth, 255) shl 16) or (flag shl 24)
        ttScore[ti] = toTt(bestScore, ply)
        return bestScore
    }

    private fun quiesce(alphaIn: Int, beta: Int, ply: Int): Int {
        pvLen[ply] = 0
        nodes++
        checkAbort()
        val p = pos
        if (ply >= MAX_PLY - 2) return staticEval()
        val inCheck = p.isInCheck()
        var alpha = alphaIn
        var best = -INF
        var stand = 0
        if (!inCheck) {
            stand = staticEval()
            if (stand >= beta) return stand
            if (stand > alpha) alpha = stand
            best = stand
        }
        val moves = moveBuf[ply]
        val order = orderBuf[ply]
        val n = p.generateMoves(moves, tacticalOnly = !inCheck)
        scoreMoves(moves, order, n, ply, 0)
        val them = if (p.sideToMove == Side.WHITE) Side.BLACK else Side.WHITE
        var legal = 0
        for (i in 0 until n) {
            pickNext(moves, order, i, n)
            val m = moves[i]
            if (!inCheck && m shr 12 == 0) {
                val to = (m shr 6) and 63
                val victim = VALUE[p.pieceCode(to) and 7].let { if (it == 0) 100 else it } // 0 = en passant
                // Delta pruning, and skipping captures of defended pieces by more valuable ones.
                if (stand + victim + 200 <= alpha) continue
                if (VALUE[p.pieceCode(m and 63) and 7] > victim && p.isSquareAttacked(to, them)) continue
            }
            if (!p.tryMakeMove(m)) continue
            legal++
            val s = -quiesce(-beta, -alpha, ply + 1)
            p.unmakeMove()
            if (s > best) {
                best = s
                if (s > alpha) {
                    alpha = s
                    if (s >= beta) break
                }
            }
        }
        if (inCheck && legal == 0) return -MATE + ply
        return best
    }

    private fun scoreMoves(moves: IntArray, order: IntArray, n: Int, ply: Int, ttMove: Int) {
        val p = pos
        val ep = p.enPassantSquare
        val k1 = killers[ply * 2]
        val k2 = killers[ply * 2 + 1]
        val hBase = if (p.sideToMove == Side.WHITE) 0 else 4096
        for (i in 0 until n) {
            val m = moves[i]
            val to = (m shr 6) and 63
            val attacker = p.pieceCode(m and 63) and 7
            var victim = p.pieceCode(to) and 7
            if (victim == 0 && attacker == PAWN_T && to == ep) victim = PAWN_T
            val promo = m shr 12
            order[i] = when {
                m == ttMove -> 3_000_000
                promo == QUEEN_PROMO -> 2_000_000 + victim * 10
                victim != 0 -> 1_000_000 + victim * 10 - attacker
                promo != 0 -> -1_000
                m == k1 -> 900_000
                m == k2 -> 800_000
                else -> min(history[hBase or (m and 4095)], 700_000)
            }
        }
    }

    private fun pickNext(moves: IntArray, order: IntArray, i: Int, n: Int) {
        var bi = i
        var bs = order[i]
        for (j in i + 1 until n) if (order[j] > bs) {
            bs = order[j]; bi = j
        }
        if (bi != i) {
            val m = moves[i]; moves[i] = moves[bi]; moves[bi] = m
            val s = order[i]; order[i] = order[bi]; order[bi] = s
        }
    }

    private fun hasPieces(p: Position): Boolean {
        val side = p.sideToMove
        return p.pieceCount(PieceType.KNIGHT, side) + p.pieceCount(PieceType.BISHOP, side) +
            p.pieceCount(PieceType.ROOK, side) + p.pieceCount(PieceType.QUEEN, side) > 0
    }

    private fun toTt(s: Int, ply: Int) = when {
        s >= MATE - MATE_BAND -> s + ply
        s <= -(MATE - MATE_BAND) -> s - ply
        else -> s
    }

    private fun fromTt(s: Int, ply: Int) = when {
        s >= MATE - MATE_BAND -> s - ply
        s <= -(MATE - MATE_BAND) -> s + ply
        else -> s
    }

    private companion object {
        val VALUE = intArrayOf(0, 100, 320, 330, 500, 900, 20_000)
    }
}
