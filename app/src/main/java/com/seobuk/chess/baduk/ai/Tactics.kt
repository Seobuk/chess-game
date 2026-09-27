package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.Scoring
import com.seobuk.chess.baduk.core.Stone
import kotlin.math.abs

// Small allocation-free facts about one move, shared by the playouts, the tree priors and the coach.
// Colours are the codes of Board.color(): 1 = black, 2 = white.

/**
 * Size of the group that playing [p] as [me] would leave with one liberty or none;
 * 0 if the move is safe or captures something.
 */
internal fun selfAtariSize(b: Board, p: Int, me: Int): Int {
    val nbr = b.grid.nbr
    val k = p * 4
    var lib = -1
    for (i in k until k + 4) {
        val q = nbr[i]
        if (q < 0) break
        val c = b.color(q)
        if (c == Board.EMPTY) {
            if (lib >= 0) return 0
            lib = q
        } else if (c != me) {
            if (b.liberties(q) == 1) return 0
        } else if (b.liberties(q) >= 3) return 0
    }
    var size = 1
    var g0 = -1
    var g1 = -1
    var g2 = -1
    for (i in k until k + 4) {
        val q = nbr[i]
        if (q < 0) break
        if (b.color(q) != me) continue
        val g = b.groupId(q)
        if (g == g0 || g == g1 || g == g2) continue
        if (g0 < 0) g0 = g else if (g1 < 0) g1 = g else g2 = g
        size += b.groupSize(q)
        if (b.liberties(q) == 1) continue // its only liberty is p
        var s = q
        do {
            val ks = s * 4
            for (j in ks until ks + 4) {
                val r = nbr[j]
                if (r < 0) break
                if (r != p && b.color(r) == Board.EMPTY) {
                    if (lib >= 0 && lib != r) return 0
                    lib = r
                }
            }
            s = b.nextInGroup(s)
        } while (s != q)
    }
    return size
}

/** Stones of colour [colour] in atari next to [p], each group counted once. */
private fun atariStonesAround(b: Board, p: Int, colour: Int): Int {
    val nbr = b.grid.nbr
    val k = p * 4
    var total = 0
    var g0 = -1
    var g1 = -1
    var g2 = -1
    for (i in k until k + 4) {
        val q = nbr[i]
        if (q < 0) break
        if (b.color(q) != colour || b.liberties(q) != 1) continue
        val g = b.groupId(q)
        if (g == g0 || g == g1 || g == g2) continue
        if (g0 < 0) g0 = g else if (g1 < 0) g1 = g else g2 = g
        total += b.groupSize(q)
    }
    return total
}

/** Stones that [me] playing the empty point [p] would capture. */
internal fun captureSize(b: Board, p: Int, me: Int): Int = atariStonesAround(b, p, 3 - me)

/** Own stones in atari that playing [p] pulls out (the result has two liberties or captures). */
internal fun rescueSize(b: Board, p: Int, me: Int): Int {
    val saved = atariStonesAround(b, p, me)
    return if (saved > 0 && selfAtariSize(b, p, me) == 0) saved else 0
}

/**
 * Stones that cannot survive according to [ownership] (+1 = surely black .. -1 = surely white):
 * whole groups whose points end up with the other colour.
 */
internal fun deadStones(b: Board, ownership: FloatArray): Set<Int> {
    val dead = HashSet<Int>()
    for (p in ownership.indices) {
        val c = b.color(p)
        if (c == Board.EMPTY || b.groupId(p) != p) continue
        val stones = b.group(p)
        var sum = 0f
        for (s in stones) sum += ownership[s]
        val mine = (if (c == Board.BLACK_CODE) sum else -sum) / stones.size
        if (mine <= -DEAD_BELOW) for (s in stones) dead.add(s)
    }
    return dead
}

/**
 * Territory count from [ownership], Black minus White: clearly owned empty points, dead stones
 * (territory + prisoner), captures so far and komi. Unclear points count for nobody.
 */
internal fun lead(b: Board, ownership: FloatArray, dead: Set<Int>, komi: Double): Double {
    var black = b.captured(Stone.BLACK)
    var white = b.captured(Stone.WHITE)
    for (p in ownership.indices) {
        val c = b.color(p)
        if (c == Board.EMPTY) {
            if (ownership[p] >= OWNED) black++ else if (ownership[p] <= -OWNED) white++
        } else if (p in dead) {
            if (c == Board.WHITE_CODE) black += 2 else white += 2
        }
    }
    return black - white - komi
}

/** Official result if the game stopped here with [dead] lifted. */
internal fun officialWinner(b: Board, dead: Set<Int>, komi: Double): Stone? = Scoring.territory(b, dead, komi).winner

/**
 * Which points are decided according to [ownership]. A point on its own needs a clear owner. Inside
 * an area closed off by one colour ([dead] stones lifted) the mean of the whole area counts instead:
 * playouts let an invader live in a corner now and then, which says little about that one point.
 */
internal fun settledPoints(b: Board, ownership: FloatArray, dead: Set<Int>): BooleanArray {
    val n = ownership.size
    val nbr = b.grid.nbr
    val out = BooleanArray(n) { abs(ownership[it]) > SETTLED }
    val seen = BooleanArray(n)
    val area = IntArray(n)
    fun open(p: Int) = b.color(p) == Board.EMPTY || p in dead
    for (start in 0 until n) {
        if (seen[start] || !open(start)) continue
        var count = 0
        var borders = 0 // bit 1 = touches black, bit 2 = touches white
        var sum = 0f
        area[count++] = start
        seen[start] = true
        var i = 0
        while (i < count) {
            val p = area[i++]
            sum += ownership[p]
            val k = p * 4
            for (j in k until k + 4) {
                val q = nbr[j]
                if (q < 0) break
                if (!open(q)) borders = borders or b.color(q)
                else if (!seen[q]) {
                    seen[q] = true
                    area[count++] = q
                }
            }
        }
        val mean = sum / count
        if (borders == Board.BLACK_CODE && mean > CLOSED_AREA || borders == Board.WHITE_CODE && mean < -CLOSED_AREA) {
            for (j in 0 until count) out[area[j]] = true
        }
    }
    return out
}

internal const val DEAD_BELOW = 0.5f
internal const val OWNED = 0.4f
internal const val SETTLED = 0.7f
internal const val CLOSED_AREA = 0.5f
