package com.seobuk.chess.baduk.ai

import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.PASS

/**
 * Reading for small problems: can the stones at a target point be captured by force? The attacker
 * is the opponent of the target stones, whoever is to move on the board moves first (so the same
 * call answers "can I capture" and "am I safe if I leave it"), the defender may pass and the
 * attacker may not. The search is bounded to [MAX_DEPTH] plies.
 * ponytail: no ko threats are modelled, a ko goes to the side that takes it first; add an attacker
 * pass and a ko result if problems with ko ever need an exact answer.
 *
 * With a region (life and death): every empty point of the region is tried, exhaustively.
 * Without one (ladders, nets, snapbacks): only moves around the target are tried, the target has
 * escaped once it has three liberties on the attacker's turn, and the attacker gets at most
 * [QUIET] moves that are not atari.
 */
object Solver {
    const val MAX_DEPTH = 40
    const val QUIET = 2
    private const val ESCAPE_LIBERTIES = 3

    /** null = node budget exceeded. */
    fun capturable(board: Board, target: Int, region: Set<Int>? = null, maxNodes: Int = 200_000): Boolean? {
        require(board[target] != null) { "No stone at $target" }
        return try {
            Reader(board, target, region, maxNodes).search(board.copy(), MAX_DEPTH, QUIET)
        } catch (e: OutOfNodes) {
            null
        }
    }

    /**
     * Principal line for display, from the side to move: the winner of the fight plays winning moves
     * (the attacker the quickest capture), a doomed defender holds out as long as possible.
     * Empty if the budget ran out or nothing can be played.
     */
    fun refutation(board: Board, target: Int, region: Set<Int>? = null, maxNodes: Int = 200_000): List<Int> {
        require(board[target] != null) { "No stone at $target" }
        return try {
            Reader(board, target, region, maxNodes).line(board.copy())
        } catch (e: OutOfNodes) {
            emptyList()
        }
    }

    /** Nodes searched by the last call on this thread's reader; for speed reports. */
    internal fun count(board: Board, target: Int, region: Set<Int>?, maxNodes: Int): Int {
        val r = Reader(board, target, region, maxNodes)
        try {
            r.search(board.copy(), MAX_DEPTH, QUIET)
        } catch (e: OutOfNodes) {
            // the count is what matters
        }
        return r.nodes
    }

    /**
     * The room a group has for eyes: everything reached from [target] through empty points and its
     * own colour, plus opponent stones that sit inside (all their liberties are in the region).
     * The region to pass to [capturable] for life-and-death positions.
     */
    fun eyeRegion(board: Board, target: Int): Set<Int> {
        val defender = board.color(target)
        require(defender != Board.EMPTY) { "No stone at $target" }
        val nbr = board.grid.nbr
        val region = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        region.add(target)
        stack.add(target)
        while (true) {
            while (stack.isNotEmpty()) {
                val k = stack.removeLast() * 4
                for (i in k until k + 4) {
                    val q = nbr[i]
                    if (q < 0) break
                    if (board.color(q) != 3 - defender && region.add(q)) stack.add(q)
                }
            }
            for (p in region.toList()) {
                val k = p * 4
                for (i in k until k + 4) {
                    val q = nbr[i]
                    if (q < 0) break
                    if (q !in region && board.color(q) == 3 - defender && board.libertyPoints(q).all { it in region }) {
                        for (s in board.group(q)) if (region.add(s)) stack.add(s)
                    }
                }
            }
            if (stack.isEmpty()) return region
        }
    }

    private class OutOfNodes : RuntimeException()

    private class Reader(start: Board, val target: Int, region: Set<Int>?, val maxNodes: Int) {
        private val size = start.size
        private val n = size * size
        private val nbr = start.grid.nbr
        private val defender = start.color(target)
        private val inRegion = region?.let { r -> BooleanArray(n) { it in r } }
        private val boards = arrayOfNulls<Board>(MAX_DEPTH + 1)
        private val lists = arrayOfNulls<IntArray>(MAX_DEPTH + 1)
        private val stamp = IntArray(n)
        private var stampNow = 0

        // key -> fewest plies known to be enough (low byte) and most plies known to be too few + 1
        private val table = HashMap<Long, Int>()
        var nodes = 0

        private fun attacking(b: Board) = b.toMove.ordinal + 1 != defender

        /** Can the attacker capture the target within [depth] plies? */
        fun search(b: Board, depth: Int, quiet: Int): Boolean {
            if (b.color(target) != defender) return true
            val attacking = attacking(b)
            if (inRegion == null && attacking && b.liberties(target) >= ESCAPE_LIBERTIES) return false
            if (depth <= 0) return false
            val key = b.hash + quiet * 0x632BE59BD9B4E019L
            val known = table[key] ?: 0xFF
            if (known and 0xFF <= depth) return true
            if ((known ushr 8) - 1 >= depth) return false
            if (++nodes > maxNodes) throw OutOfNodes()

            val list = lists[depth] ?: IntArray(n + 1).also { lists[depth] = it }
            val count = candidates(b, list)
            val child = boards[depth] ?: Board(size).also { boards[depth] = it }
            var result = !attacking
            for (i in 0 until count) {
                child.copyFrom(b)
                if (!child.play(list[i])) continue
                val q = quietLeft(child, attacking, quiet)
                if (q < 0) continue
                if (search(child, depth - 1, q) == attacking) {
                    result = attacking
                    break
                }
            }
            val now = table[key] ?: 0xFF
            table[key] = if (result) (now and 0xFF.inv()) or minOf(now and 0xFF, depth)
            else (now and 0xFF) or (maxOf(now ushr 8, depth + 1) shl 8)
            return result
        }

        /** Quiet moves the attacker has left after the move that led to [child]; -1 = not allowed. */
        private fun quietLeft(child: Board, attackerMoved: Boolean, quiet: Int): Int =
            if (inRegion == null && attackerMoved && child.color(target) == defender && child.liberties(target) > 1) quiet - 1
            else quiet

        /**
         * Moves to try, most forcing first: the target's liberties, the liberties of weak attacker
         * groups around it, then the rest (region) or the points next to the liberties (no region).
         * The defender's list ends with PASS.
         */
        private fun candidates(b: Board, out: IntArray): Int {
            stampNow++
            var count = 0
            fun add(p: Int) {
                if (stamp[p] != stampNow && b.color(p) == Board.EMPTY && (inRegion == null || inRegion[p])) {
                    stamp[p] = stampNow
                    out[count++] = p
                }
            }
            val libs = b.libertyPoints(target)
            for (l in libs) add(l)
            fun weakAround(p: Int) {
                val k = p * 4
                for (i in k until k + 4) {
                    val q = nbr[i]
                    if (q < 0) break
                    if (b.color(q) == 3 - defender && b.liberties(q) <= 2) for (l in b.libertyPoints(q)) add(l)
                }
            }
            for (s in b.group(target)) weakAround(s)
            for (l in libs) weakAround(l)
            if (inRegion == null) {
                for (l in libs) {
                    val k = l * 4
                    for (i in k until k + 4) {
                        val q = nbr[i]
                        if (q < 0) break
                        add(q)
                    }
                }
            } else {
                for (i in 0 until b.emptyCount) add(b.emptyAt(i))
            }
            if (!attacking(b)) out[count++] = PASS
            return count
        }

        private fun pliesToCapture(b: Board, depth: Int, quiet: Int): Int {
            for (d in 0..depth) if (search(b, d, quiet)) return d
            return depth + 1
        }

        fun line(start: Board): List<Int> {
            val out = ArrayList<Int>()
            var b = start
            var depth = MAX_DEPTH
            var quiet = QUIET
            val won = search(b, depth, quiet)
            val list = IntArray(n + 1)
            while (b.color(target) == defender && depth > 0 && out.size < 16) {
                val attacking = attacking(b)
                if (inRegion == null && attacking && b.liberties(target) >= ESCAPE_LIBERTIES) break
                val count = candidates(b, list)
                var move = PASS
                var next: Board? = null
                var nextQuiet = quiet
                var best = Int.MIN_VALUE
                for (i in 0 until count) {
                    if (list[i] == PASS) continue
                    val c = b.copy()
                    if (!c.play(list[i])) continue
                    val q = quietLeft(c, attacking, quiet)
                    if (q < 0) continue
                    val captured = search(c, depth - 1, q)
                    val score = when {
                        attacking && won -> if (captured) -pliesToCapture(c, depth - 1, q) else continue // quickest capture
                        attacking -> 0                                   // failing attack: the most forcing try
                        won -> pliesToCapture(c, depth - 1, q)           // doomed defender: longest resistance
                        else -> if (captured) continue else 0            // safe defender: the first move that holds
                    }
                    if (score > best) {
                        best = score
                        move = list[i]
                        next = c
                        nextQuiet = q
                    }
                }
                if (next == null) break
                out.add(move)
                b = next
                quiet = nextQuiet
                depth--
                if (!won && out.size >= 8) break
            }
            return out
        }
    }
}
