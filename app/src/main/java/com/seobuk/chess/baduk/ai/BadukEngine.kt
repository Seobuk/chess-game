package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.PASS
import com.seobuk.chess.baduk.core.Stone
import kotlin.math.abs

data class Candidate(val point: Int, val visits: Int, val winRate: Double)

/**
 * [best] may be PASS. [winRate] is for the side to move. [ownership]: per point, +1 = surely black ..
 * -1 = surely white. [candidates]: most visited first; late in the game only moves worth playing
 * (empty when [best] is PASS because nothing is).
 */
data class BadukSearch(
    val best: Int,
    val winRate: Double,
    val playouts: Int,
    val ownership: FloatArray,
    val candidates: List<Candidate>,
)

/** 3x3 neighbourhoods in a fixed direction order and edge distances, per board size. */
internal class Geo private constructor(val size: Int) {
    val n = size * size

    /** 8 slots per point, row by row around it (NW N NE W E SW S SE); -1 = off the board. */
    val nb8 = IntArray(n * 8)

    /** 0 = first line. */
    val line = IntArray(n)

    init {
        for (p in 0 until n) {
            val x = p % size
            val y = p / size
            line[p] = minOf(minOf(x, y), minOf(size - 1 - x, size - 1 - y))
            var k = p * 8
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val on = x + dx in 0 until size && y + dy in 0 until size
                nb8[k++] = if (on) (y + dy) * size + x + dx else -1
            }
        }
    }

    /** Index into [Pat3.match]: 2 bits per neighbour (0 empty, 1 black, 2 white, 3 off the board). */
    fun code(b: Board, p: Int): Int {
        var c = 0
        val k = p * 8
        for (i in 0 until 8) {
            val q = nb8[k + i]
            c = c or ((if (q < 0) 3 else b.color(q)) shl (2 * i))
        }
        return c
    }

    fun distance(a: Int, b: Int): Int = abs(a % size - b % size) + abs(a / size - b / size)

    companion object {
        private val cache = arrayOfNulls<Geo>(20)
        fun of(size: Int): Geo = cache[size] ?: Geo(size).also { cache[size] = it }
    }
}

/**
 * The 3x3 playout patterns of MoGo (hane, cut, edge shapes) as one lookup table. In the source
 * X and O are the two colours (either way round), x = not X, o = not O, ? = anything,
 * space = off the board; the centre is the empty point to play.
 */
internal object Pat3 {
    private val source = arrayOf(
        "XOX...???", "XO....?.?", "XO?X..x.?", ".O.X.....",
        "XO?O.o?o?", "XO?O.X???", "?X?O.Oooo", "OX?o.O???",
        "X.?O.?   ", "OX?X.O   ", "?X?x.O   ", "?XOx.x   ", "?OXX.O   ",
    )

    val match = BooleanArray(1 shl 16)

    init {
        for (s in source) for (sym in 0 until 8) {
            var g = s
            repeat(sym and 3) { g = String(CharArray(9) { i -> g[(2 - i % 3) * 3 + i / 3] }) }
            if (sym and 4 != 0) g = String(CharArray(9) { i -> g[i / 3 * 3 + 2 - i % 3] })
            add(g)
            add(String(CharArray(9) { i -> "OXox".getOrNull("XOxo".indexOf(g[i])) ?: g[i] }))
        }
    }

    private fun add(g: String) {
        val cells = g.removeRange(4, 5)
        val masks = IntArray(8) {
            when (cells[it]) {
                '.' -> 1
                'X' -> 2
                'O' -> 4
                ' ' -> 8
                'x' -> 1 or 4 or 8
                'o' -> 1 or 2 or 8
                else -> 15
            }
        }
        fill(masks, 0, 0)
    }

    private fun fill(masks: IntArray, i: Int, code: Int) {
        if (i == 8) {
            match[code] = true
            return
        }
        for (state in 0 until 4) if (masks[i] and (1 shl state) != 0) fill(masks, i + 1, code or (state shl (2 * i)))
    }
}

/**
 * Monte Carlo tree search in the style of Michi: RAVE + prior knowledge instead of an exploration
 * term, playouts that answer ataris, play 3x3 patterns near the last moves and never fill an eye,
 * area scoring inside the playouts. NOT thread-safe: one instance per worker.
 *
 * [search] is territory-aware at the root (the game is scored by Korean rules): late in the game
 * it drops moves that gain nothing (inside settled territory) and answers PASS when none is left
 * or when the opponent passed and the official count says the side to move is ahead.
 */
class BadukEngine(seed: Long = System.nanoTime()) {
    private var rng = (seed xor 0x2545F4914F6CDD1DL).let { if (it == 0L) 0x9E3779B9L else it }

    /** The children of one position. Win counts are for [side], the colour code that moves here. */
    private class Node(val side: Int, count: Int) {
        val moves = IntArray(count)
        val v = IntArray(count)
        val w = FloatArray(count)
        val pv = IntArray(count)   // prior knowledge, as virtual visits
        val pw = FloatArray(count) // and virtual wins
        val av = IntArray(count)   // AMAF / RAVE
        val aw = FloatArray(count)
        val kids = arrayOfNulls<Node>(count)
    }

    /**
     * Points worth playing in the position of the last [search], or null if everything was (the
     * late-game filter did not run).
     */
    internal var useful: IntArray? = null
        private set

    private var geo = Geo.of(9)
    private var own = IntArray(0)
    private var cand = IntArray(0)
    private var slots = 0

    private fun rnd(bound: Int): Int {
        rng = rng xor (rng shl 13)
        rng = rng xor (rng ushr 7)
        rng = rng xor (rng shl 17)
        return ((rng ushr 33) % bound).toInt()
    }

    private fun chance(percent: Int): Boolean = rnd(100) < percent

    /**
     * Blocking. Stops at [timeLimitMs], [maxPlayouts] or [isCancelled], whichever comes first.
     * [lastMove] (the move that led to [board], if known) sharpens the local heuristics.
     */
    fun search(
        board: Board,
        komi: Double,
        timeLimitMs: Long,
        maxPlayouts: Int = Int.MAX_VALUE,
        captureGo: Boolean = false,
        isCancelled: () -> Boolean = { false },
        lastMove: Int = PASS,
    ): BadukSearch {
        val n = board.size * board.size
        prepare(board)
        slots = 0
        val late = !captureGo && (board.passes > 0 || n - board.emptyCount >= n / 3)
        val root = expand(board, lastMove, late)
        val passIdx = root.moves.indexOf(PASS)
        val scratch = Board(board.size)
        val amaf = IntArray(n)
        val nodes = arrayOfNulls<Node>(MAX_DEPTH)
        val picks = IntArray(MAX_DEPTH)
        val deadline = System.nanoTime() + timeLimitMs * 1_000_000
        var playouts = 0

        while (playouts < maxPlayouts && !isCancelled()) {
            if (playouts > 0 && playouts and 7 == 0 && System.nanoTime() >= deadline) break
            scratch.copyFrom(board)
            amaf.fill(0)
            var node = root
            var depth = 0
            var last = lastMove
            var last2 = PASS
            var passes = minOf(board.passes, 1)
            var blackWin = -1f
            while (true) {
                // A steady share of the playouts tries "what if I pass" so that pass has a real win rate.
                val i = if (depth == 0 && passIdx >= 0 && root.moves.size > 1 && playouts % 12 == 11) passIdx else select(node)
                val m = node.moves[i]
                nodes[depth] = node
                picks[depth] = i
                depth++
                scratch.play(m)
                last2 = last
                last = m
                if (m == PASS) {
                    if (++passes >= 2) break
                } else {
                    passes = 0
                    if (amaf[m] == 0) amaf[m] = node.side
                    if (captureGo && scratch.lastCaptureCount > 0) {
                        blackWin = if (node.side == Board.BLACK_CODE) 1f else 0f
                        break
                    }
                }
                var kid = node.kids[i]
                if (kid == null && node.v[i] >= EXPAND_VISITS && slots < MAX_SLOTS) {
                    kid = expand(scratch, last, false)
                    node.kids[i] = kid
                }
                if (kid == null || depth == MAX_DEPTH) break
                node = kid
            }
            if (blackWin < 0f) blackWin = playout(scratch, last, last2, komi, captureGo, amaf)

            for (d in 0 until depth) {
                val nd = nodes[d]!!
                val r = if (nd.side == Board.BLACK_CODE) blackWin else 1f - blackWin
                nd.v[picks[d]]++
                nd.w[picks[d]] += r
                for (j in nd.moves.indices) {
                    val m = nd.moves[j]
                    if (m >= 0 && amaf[m] == nd.side) {
                        nd.av[j]++
                        nd.aw[j] += r
                    }
                }
            }
            playouts++
        }

        val ownership = FloatArray(n) { if (playouts == 0) 0f else own[it].toFloat() / playouts }
        var order = root.moves.indices.filter { root.moves[it] != PASS }
            .sortedByDescending { root.v[it] * 1000.0 + root.pw[it] / root.pv[it] }
        fun rate(i: Int): Double = if (root.v[i] > 0) root.w[i].toDouble() / root.v[i] else 0.5
        useful = null
        if (late && order.isNotEmpty() && !isCancelled()) {
            order = worthPlaying(board, komi, root, order, passIdx, ownership, isCancelled)
            useful = IntArray(order.size) { root.moves[order[it]] }
        }
        val passNow = order.isEmpty()
        val picked = order.take(16).filterIndexed { k, i -> k == 0 || root.v[i] > 0 }
        val candidates = picked.map { Candidate(root.moves[it], root.v[it], rate(it)) }
        val total = root.v.sum()
        val winRate = when {
            !passNow -> candidates[0].winRate
            passIdx >= 0 && root.v[passIdx] > 0 -> rate(passIdx)
            total > 0 -> root.w.sum().toDouble() / total
            else -> 0.5
        }
        return BadukSearch(if (passNow) PASS else candidates[0].point, winRate, playouts, ownership, candidates)
    }

    /**
     * Flat playouts from [board], no tree: who ends up owning each point (+1 black .. -1 white).
     */
    internal fun ownership(
        board: Board,
        komi: Double,
        playouts: Int,
        lastMove: Int = PASS,
        isCancelled: () -> Boolean = { false },
    ): FloatArray {
        prepare(board)
        val scratch = Board(board.size)
        var done = 0
        while (done < playouts && !isCancelled()) {
            scratch.copyFrom(board)
            playout(scratch, lastMove, PASS, komi, false, null)
            done++
        }
        return FloatArray(own.size) { if (done == 0) 0f else own[it].toFloat() / done }
    }

    private fun prepare(board: Board) {
        geo = Geo.of(board.size)
        own = IntArray(geo.n)
        if (cand.size < geo.n) cand = IntArray(maxOf(geo.n, 16))
    }

    /**
     * Late-game filter under territory scoring: the root children worth playing out of [order], in the
     * same order; empty = pass. Every child is judged, however few playouts the search had: a move is
     * worth playing when its point is still contested if the side to move passes (see
     * [settledPoints]; and it is no self-atari), when it captures or rescues stones that are not settled yet, or when the tree says
     * that passing instead costs real winning chances.
     */
    private fun worthPlaying(
        board: Board,
        komi: Double,
        root: Node,
        order: List<Int>,
        passIdx: Int,
        rootOwnership: FloatArray,
        isCancelled: () -> Boolean,
    ): List<Int> {
        val me = root.side
        val sign = if (me == Board.BLACK_CODE) 1f else -1f
        val ifPass = ownership(board.copy().also { it.play(PASS) }, komi, PASS_PLAYOUTS, isCancelled = isCancelled)
        val dead = deadStones(board, ifPass)
        val settled = settledPoints(board, ifPass, dead)

        fun rate(i: Int): Double = if (root.v[i] > 0) root.w[i].toDouble() / root.v[i] else 0.5
        val passRate = if (passIdx >= 0 && root.v[passIdx] >= MIN_PASS_VISITS) rate(passIdx) else -1.0
        val nbr = board.grid.nbr
        var forced = false
        val useful = order.filter { i ->
            val p = root.moves[i]
            val known = passRate >= 0 && root.v[i] >= MIN_PASS_VISITS
            var must = known && rate(i) - passRate >= 0.15
            val k = p * 4
            for (j in k until k + 4) {
                val q = nbr[j]
                if (q < 0) break
                if (board.color(q) == Board.EMPTY || board.liberties(q) != 1) continue
                // Opponent stones I can take that are not dead anyway; own stones I can still save.
                if (board.color(q) != me) { if (sign * ifPass[q] < SETTLED) must = true }
                else if (sign * rootOwnership[q] > -DEAD_BELOW && rescueSize(board, p, me) > 0) must = true
            }
            if (must) forced = true
            must || !settled[p] && selfAtariSize(board, p, me) == 0 && !(known && rate(i) < passRate - 0.05)
        }
        if (useful.isNotEmpty() && board.passes > 0 && !forced) {
            // The opponent passed and only small things are left: stop if the official count says I am ahead.
            var open = 0
            for (i in 0 until board.emptyCount) if (!settled[board.emptyAt(i)]) open++
            val mine = if (me == Board.BLACK_CODE) Stone.BLACK else Stone.WHITE
            if (open <= 2 + geo.n / 12 && officialWinner(board, dead, komi) == mine) return emptyList()
        }
        return useful
    }

    private fun expand(b: Board, last: Int, withPass: Boolean): Node {
        val stone = b.toMove
        val me = stone.ordinal + 1
        var count = 0
        for (i in 0 until b.emptyCount) {
            val p = b.emptyAt(i)
            if (!b.isEyeLike(p, stone) && b.isLegal(p)) cand[count++] = p
        }
        val pass = count == 0 || withPass || b.passes > 0
        val node = Node(me, count + if (pass) 1 else 0)
        for (i in 0 until count) {
            val p = cand[i]
            var pv = PRIOR_EVEN
            var pw = PRIOR_EVEN / 2f
            val tactical = captureSize(b, p, me) + rescueSize(b, p, me)
            if (tactical > 0) {
                val k = if (tactical == 1) 15 else 30
                pv += k
                pw += k
            }
            if (Pat3.match[geo.code(b, p)]) {
                pv += 10
                pw += 10
            }
            if (last >= 0) {
                val d = geo.distance(p, last)
                if (d in 1..3) {
                    val k = if (d == 1) 24 else if (d == 2) 22 else 8
                    pv += k
                    pw += k
                }
            }
            val line = geo.line[p]
            if (line <= 2 && emptyAround(b, p)) {
                pv += 10
                if (line == 2) pw += 10 // third line good, first and second bad
            }
            if (selfAtariSize(b, p, me) > 0) pv += 10
            node.moves[i] = p
            node.pv[i] = pv
            node.pw[i] = pw
        }
        if (pass) {
            node.moves[count] = PASS
            node.pv[count] = PRIOR_EVEN
            node.pw[count] = PRIOR_EVEN / 2f
        }
        slots += node.moves.size
        return node
    }

    /** No stone within three steps of [p]. */
    private fun emptyAround(b: Board, p: Int): Boolean {
        val size = b.size
        val x = p % size
        val y = p / size
        for (dy in -3..3) for (dx in -(3 - abs(dy))..(3 - abs(dy))) {
            val qx = x + dx
            val qy = y + dy
            if (qx in 0 until size && qy in 0 until size && b.color(qy * size + qx) != Board.EMPTY) return false
        }
        return true
    }

    private fun select(node: Node): Int {
        val count = node.moves.size
        var best = 0
        var bestValue = -1f
        var i = rnd(count)
        for (k in 0 until count) {
            val v = node.v[i] + node.pv[i]
            var value = (node.w[i] + node.pw[i]) / v
            val av = node.av[i]
            if (av > 0) {
                val beta = av / (av + v + v.toFloat() * av / RAVE_EQUIV)
                value = beta * node.aw[i] / av + (1 - beta) * value
            }
            if (value > bestValue) {
                bestValue = value
                best = i
            }
            if (++i == count) i = 0
        }
        return best
    }

    /**
     * Plays [b] to the end. Returns Black's result (1 = big win .. 0 = big loss) and adds the final owner
     * of every point to [own]. [amaf]: first colour to play each point, filled in if given.
     */
    private fun playout(b: Board, lastIn: Int, last2In: Int, komi: Double, captureGo: Boolean, amaf: IntArray?): Float {
        val n = geo.n
        var last = lastIn
        var last2 = last2In
        var passes = 0
        var moves = 0
        val limit = n * 3
        while (passes < 2 && moves < limit) {
            val stone = b.toMove
            val me = stone.ordinal + 1
            var m = -1
            if (captureGo) m = captureGoMove(b, me)
            if (m < 0 && last >= 0) {
                if (chance(90)) m = respond(b, last, me)
                if (m < 0 && chance(95)) m = pattern(b, last, last2, me, stone)
            }
            if (m < 0) m = randomMove(b, me, stone, if (captureGo) 100 else 50)
            if (m < 0) {
                b.play(PASS)
                passes++
            } else {
                b.play(m)
                passes = 0
                if (amaf != null && amaf[m] == 0) amaf[m] = me
                if (captureGo && b.lastCaptureCount > 0) return if (me == Board.BLACK_CODE) 1f else 0f
            }
            last2 = last
            last = m
            moves++
        }
        // Area score: stones plus the empty points that touch one colour only.
        val nbr = b.grid.nbr
        var black = 0
        for (p in 0 until n) {
            var c = b.color(p)
            if (c == Board.EMPTY) {
                val k = p * 4
                for (i in k until k + 4) {
                    val q = nbr[i]
                    if (q < 0) break
                    c = c or b.color(q)
                }
            }
            if (c == Board.BLACK_CODE) {
                black++
                own[p]++
            } else if (c == Board.WHITE_CODE) {
                black--
                own[p]--
            }
        }
        // Mostly win or loss, plus a little for the margin: with the game decided either way the
        // search still prefers the bigger win and the smaller loss, as a human opponent expects.
        val score = (black - komi).toFloat()
        val win = if (score > 0) 1f else if (score < 0) 0f else 0.5f
        return WIN_SHARE * win + (1 - WIN_SHARE) * (0.5f + (score / (2 * n)).coerceIn(-0.5f, 0.5f))
    }

    /** Answer to the move at [last]: take it if it is in atari, else pull out own stones it put in atari. */
    private fun respond(b: Board, last: Int, me: Int): Int {
        val nbr = b.grid.nbr
        var best = -1
        var worth = 0
        if (b.liberties(last) == 1) {
            val l = b.firstLiberty(last)
            if (b.isLegal(l)) {
                best = l
                worth = b.groupSize(last) + 1
            }
        }
        val k = last * 4
        for (i in k until k + 4) {
            val q = nbr[i]
            if (q < 0) break
            if (b.color(q) != me || b.liberties(q) != 1) continue
            val size = b.groupSize(q)
            var s = q
            do {
                val ks = s * 4
                for (j in ks until ks + 4) {
                    val r = nbr[j]
                    if (r < 0) break
                    if (b.color(r) == 3 - me && b.liberties(r) == 1 && size + b.groupSize(r) > worth) {
                        val l = b.firstLiberty(r)
                        if (b.isLegal(l)) {
                            best = l
                            worth = size + b.groupSize(r)
                        }
                    }
                }
                s = b.nextInGroup(s)
            } while (s != q)
            val l = b.firstLiberty(q)
            if (size > worth && selfAtariSize(b, l, me) == 0 && b.isLegal(l)) {
                best = l
                worth = size
            }
        }
        return best
    }

    private fun pattern(b: Board, last: Int, last2: Int, me: Int, stone: Stone): Int {
        var count = 0
        var centre = last
        repeat(2) {
            if (centre >= 0) {
                val k = centre * 8
                for (i in 0 until 8) {
                    val p = geo.nb8[k + i]
                    if (p >= 0 && b.color(p) == Board.EMPTY && Pat3.match[geo.code(b, p)]) cand[count++] = p
                }
            }
            centre = last2
        }
        while (count > 0) {
            val i = rnd(count)
            val p = cand[i]
            if (!b.isEyeLike(p, stone) && b.isLegal(p) && !(selfAtariSize(b, p, me) > 0 && chance(90))) return p
            cand[i] = cand[--count]
        }
        return -1
    }

    /** Any legal point that is no own eye; self-atari is turned down with [rejectSelfAtari] percent (single stones half as often). */
    private fun randomMove(b: Board, me: Int, stone: Stone, rejectSelfAtari: Int): Int {
        val count = b.emptyCount
        if (count == 0) return -1
        var i = rnd(count)
        for (t in 0 until count) {
            val p = b.emptyAt(i)
            if (!b.isEyeLike(p, stone) && b.isLegal(p)) {
                val size = selfAtariSize(b, p, me)
                if (size == 0 || !chance(if (size == 1 || rejectSelfAtari == 100) rejectSelfAtari else 90)) return p
            }
            if (++i == count) i = 0
        }
        return -1
    }

    /** 따내기 바둑: take whatever is in atari, else pull own stones out of atari. */
    private fun captureGoMove(b: Board, me: Int): Int {
        var rescue = -1
        for (p in 0 until geo.n) {
            val c = b.color(p)
            if (c == Board.EMPTY || b.liberties(p) != 1) continue
            val l = b.firstLiberty(p)
            if (c != me) {
                if (b.isLegal(l)) return l
            } else if (rescue < 0 && rescueSize(b, l, me) > 0 && b.isLegal(l)) rescue = l
        }
        return rescue
    }

    private companion object {
        const val MAX_DEPTH = 64
        const val EXPAND_VISITS = 8
        const val RAVE_EQUIV = 3500f
        const val WIN_SHARE = 0.9f
        const val PRIOR_EVEN = 10
        const val PASS_PLAYOUTS = 160
        const val MIN_PASS_VISITS = 24

        // ponytail: the tree stops growing at about 14 MB (leaves keep running playouts);
        // recycle nodes between moves if long thinking times ever need more.
        const val MAX_SLOTS = 400_000
    }
}
