package com.seobuk.chess.baduk.core

/**
 * Mutable Go position: simple ko, suicide illegal. Groups and their exact liberty sets are kept
 * incrementally (one bitset per group), so [liberties], [isLegal] and [play] never flood fill.
 *
 * Fast path for playouts (nothing here allocates): [copyFrom], [color], [play], [isLegal], [isEyeLike],
 * [emptyCount]/[emptyAt], [groupId], [groupSize], [nextInGroup], [liberties], [firstLiberty],
 * [lastCaptureCount]/[lastCapturedAt] and the neighbour tables in [grid].
 */
class Board(val size: Int) {
    val grid: Grid = Grid.of(size)
    private val n = grid.n
    private val nbr = grid.nbr
    private val words = (n + 63) ushr 6

    private val color = ByteArray(n)             // EMPTY, BLACK_CODE, WHITE_CODE
    private val head = IntArray(n)               // stone -> group id (one of the group's stones)
    private val next = IntArray(n)               // circular list of the group's stones
    private val gSize = IntArray(n)              // by group id
    private val gLibs = IntArray(n)              // by group id
    private val libBits = LongArray(n * words)   // by group id: bitset of liberty points
    private val empties = IntArray(n) { it }
    private val emptyPos = IntArray(n) { it }
    private val capBuf = IntArray(n)
    private val caps = IntArray(3)               // by colour code
    private var side = BLACK_CODE
    private var stoneHash = 0L

    /** Number of empty points; [emptyAt] lists them in no particular order. */
    var emptyCount = n
        private set
    var lastCaptureCount = 0
        private set
    var koPoint = -1
        private set
    var moveNumber = 0
        private set
    var passes = 0
        private set

    val toMove: Stone get() = if (side == BLACK_CODE) Stone.BLACK else Stone.WHITE

    /** Zobrist hash of stones, side to move and ko point. */
    val hash: Long
        get() = stoneHash xor
            (if (side == WHITE_CODE) grid.whiteToMoveKey else 0L) xor
            (if (koPoint >= 0) grid.koKeys[koPoint] else 0L)

    /** Stones removed by the last play. */
    val lastCaptured: IntArray get() = capBuf.copyOf(lastCaptureCount)

    operator fun get(p: Int): Stone? = when (color[p].toInt()) {
        EMPTY -> null
        BLACK_CODE -> Stone.BLACK
        else -> Stone.WHITE
    }

    /** [EMPTY], [BLACK_CODE] or [WHITE_CODE] (= `Stone.ordinal + 1`). */
    fun color(p: Int): Int = color[p].toInt()

    fun captured(by: Stone): Int = caps[by.ordinal + 1]

    fun emptyAt(i: Int): Int = empties[i]
    fun lastCapturedAt(i: Int): Int = capBuf[i]

    /** Id of the group at the stone [p]: equal for stones of one group, valid until the next play. */
    fun groupId(p: Int): Int = head[p]
    fun groupSize(p: Int): Int = if (color[p].toInt() == EMPTY) 0 else gSize[head[p]]

    /** Next stone of the same group (circular): `var s = p; do { .. s = nextInGroup(s) } while (s != p)`. */
    fun nextInGroup(p: Int): Int = next[p]

    /** Liberties of the group at [p]; 0 for an empty point. O(1). */
    fun liberties(p: Int): Int = if (color[p].toInt() == EMPTY) 0 else gLibs[head[p]]

    /** Lowest liberty point of the group at [p] (THE liberty when it is in atari), -1 if none. */
    fun firstLiberty(p: Int): Int {
        if (color[p].toInt() == EMPTY) return -1
        val base = head[p] * words
        for (w in 0 until words) {
            val bits = libBits[base + w]
            if (bits != 0L) return (w shl 6) + bits.countTrailingZeroBits()
        }
        return -1
    }

    fun libertyPoints(p: Int): IntArray {
        val out = IntArray(liberties(p))
        if (out.isEmpty()) return out
        val base = head[p] * words
        var k = 0
        for (w in 0 until words) {
            var bits = libBits[base + w]
            while (bits != 0L) {
                out[k++] = (w shl 6) + bits.countTrailingZeroBits()
                bits = bits and (bits - 1)
            }
        }
        return out
    }

    fun group(p: Int): IntArray {
        if (color[p].toInt() == EMPTY) return IntArray(0)
        val out = IntArray(gSize[head[p]])
        var s = p
        for (i in out.indices) {
            out[i] = s
            s = next[s]
        }
        return out
    }

    /** For the side to move. PASS is always legal; off-board points are not. */
    fun isLegal(p: Int): Boolean {
        if (p == PASS) return true
        if (p < 0 || p >= n || color[p].toInt() != EMPTY || p == koPoint) return false
        return !isSuicide(p)
    }

    fun illegalReason(p: Int): Illegal? {
        if (p == PASS) return null
        require(p in 0 until n) { "Bad point $p on $size" }
        return when {
            color[p].toInt() != EMPTY -> Illegal.OCCUPIED
            p == koPoint -> Illegal.KO
            isSuicide(p) -> Illegal.SUICIDE
            else -> null
        }
    }

    /** Legal points for the side to move, without PASS. */
    fun legalMoves(): IntArray {
        val out = IntArray(emptyCount)
        var k = 0
        for (p in 0 until n) if (isLegal(p)) out[k++] = p
        return out.copyOf(k)
    }

    /** Plays for the side to move; false (and no change) if illegal. */
    fun play(p: Int): Boolean {
        if (p == PASS) {
            passes++
        } else {
            if (!isLegal(p)) return false
            passes = 0
        }
        lastCaptureCount = 0
        koPoint = -1
        val me = side
        if (p != PASS) {
            put(p, me)
            val k = p * 4
            for (i in k until k + 4) {
                val q = nbr[i]
                if (q < 0) break
                if (color[q].toInt() == 3 - me && gLibs[head[q]] == 0) removeGroup(head[q], me)
            }
            caps[me] += lastCaptureCount
            // Ko: one stone taken by a lone stone that is now in atari itself.
            if (lastCaptureCount == 1 && gSize[head[p]] == 1 && gLibs[p] == 1) koPoint = capBuf[0]
        }
        side = 3 - me
        moveNumber++
        return true
    }

    /**
     * True if [p] is empty, every orthogonal neighbour is a [colour] stone and the opponent holds at
     * most one diagonal (none on the edge): the usual "do not fill this" test for playouts.
     */
    fun isEyeLike(p: Int, colour: Stone): Boolean {
        if (color[p].toInt() != EMPTY) return false
        val c = colour.ordinal + 1
        val k = p * 4
        for (i in k until k + 4) {
            val q = nbr[i]
            if (q < 0) break
            if (color[q].toInt() != c) return false
        }
        val diag = grid.diag
        var enemy = 0
        var count = 0
        for (i in k until k + 4) {
            val q = diag[i]
            if (q < 0) break
            count++
            if (color[q].toInt() == 3 - c) enemy++
        }
        return if (count == 4) enemy <= 1 else enemy == 0
    }

    fun copy(): Board = Board(size).also { it.copyFrom(this) }

    /** Overwrites this board with [other] (same size) without allocating. */
    fun copyFrom(other: Board) {
        require(other.size == size) { "Size ${other.size} != $size" }
        System.arraycopy(other.color, 0, color, 0, n)
        System.arraycopy(other.head, 0, head, 0, n)
        System.arraycopy(other.next, 0, next, 0, n)
        System.arraycopy(other.gSize, 0, gSize, 0, n)
        System.arraycopy(other.gLibs, 0, gLibs, 0, n)
        System.arraycopy(other.libBits, 0, libBits, 0, n * words)
        System.arraycopy(other.empties, 0, empties, 0, n)
        System.arraycopy(other.emptyPos, 0, emptyPos, 0, n)
        System.arraycopy(other.capBuf, 0, capBuf, 0, other.lastCaptureCount)
        System.arraycopy(other.caps, 0, caps, 0, 3)
        side = other.side
        stoneHash = other.stoneHash
        emptyCount = other.emptyCount
        lastCaptureCount = other.lastCaptureCount
        koPoint = other.koPoint
        moveNumber = other.moveNumber
        passes = other.passes
    }

    /** For diagrams. A ko ban belongs to the side that was to move, so changing sides drops it. */
    fun setToMove(s: Stone) {
        if (s == toMove) return
        side = 3 - side
        koPoint = -1
    }

    /** Setup for diagrams: puts a stone on an empty point WITHOUT capturing anything. */
    fun place(p: Int, stone: Stone) {
        require(p in 0 until n && color[p].toInt() == EMPTY) { "Cannot place on $p" }
        put(p, stone.ordinal + 1)
    }

    /** Diagram rows ('.', 'X', 'O'), top row first. */
    fun toRows(): List<String> = List(size) { y ->
        (0 until size).joinToString(" ") { x -> ".XO"[color[y * size + x].toInt()].toString() }
    }

    override fun toString(): String = toRows().joinToString("\n")

    private fun isSuicide(p: Int): Boolean {
        val k = p * 4
        for (i in k until k + 4) {
            val q = nbr[i]
            if (q < 0) break
            val c = color[q].toInt()
            if (c == EMPTY) return false
            val libs = gLibs[head[q]]
            if (c == side) { if (libs > 1) return false } else if (libs == 1) return false
        }
        return true
    }

    private fun put(p: Int, c: Int) {
        color[p] = c.toByte()
        val i = emptyPos[p]
        val last = empties[--emptyCount]
        empties[i] = last
        emptyPos[last] = i
        stoneHash = stoneHash xor grid.stoneKeys[(c - 1) * n + p]

        head[p] = p
        next[p] = p
        gSize[p] = 1
        val base = p * words
        for (w in 0 until words) libBits[base + w] = 0L
        var libs = 0
        val k = p * 4
        for (j in k until k + 4) {
            val q = nbr[j]
            if (q < 0) break
            if (color[q].toInt() == EMPTY) {
                libBits[base + (q ushr 6)] = libBits[base + (q ushr 6)] or (1L shl q)
                libs++
            } else {
                val g = head[q]
                val at = g * words + (p ushr 6)
                val bit = 1L shl p
                if (libBits[at] and bit != 0L) {
                    libBits[at] = libBits[at] xor bit
                    gLibs[g]--
                }
            }
        }
        gLibs[p] = libs
        for (j in k until k + 4) {
            val q = nbr[j]
            if (q < 0) break
            if (color[q].toInt() == c && head[q] != head[p]) merge(head[q], head[p])
        }
    }

    private fun merge(a: Int, b: Int) {
        val big = if (gSize[a] >= gSize[b]) a else b
        val small = if (big == a) b else a
        var s = small
        do {
            head[s] = big
            s = next[s]
        } while (s != small)
        val t = next[big]
        next[big] = next[small]
        next[small] = t
        gSize[big] += gSize[small]
        var libs = 0
        for (w in 0 until words) {
            val bits = libBits[big * words + w] or libBits[small * words + w]
            libBits[big * words + w] = bits
            libs += bits.countOneBits()
        }
        gLibs[big] = libs
    }

    private fun removeGroup(g: Int, capturer: Int) {
        var s = g
        do {
            val following = next[s]
            color[s] = EMPTY.toByte()
            stoneHash = stoneHash xor grid.stoneKeys[(2 - capturer) * n + s]
            emptyPos[s] = emptyCount
            empties[emptyCount++] = s
            capBuf[lastCaptureCount++] = s
            val k = s * 4
            for (j in k until k + 4) {
                val q = nbr[j]
                if (q < 0) break
                if (color[q].toInt() == capturer) {
                    val h = head[q]
                    val at = h * words + (s ushr 6)
                    val bit = 1L shl s
                    if (libBits[at] and bit == 0L) {
                        libBits[at] = libBits[at] or bit
                        gLibs[h]++
                    }
                }
            }
            s = following
        } while (s != g)
    }

    companion object {
        const val EMPTY = 0
        const val BLACK_CODE = 1
        const val WHITE_CODE = 2

        /**
         * Parses a diagram: '.' empty, 'X'/'B' black, 'O'/'W' white (B and W carry a mark, see [marked]),
         * 'a'..'w' except 'o' = a labelled empty point (see [labels]).
         * @throws IllegalArgumentException on ragged rows, unknown cells or stones without liberties
         */
        fun fromRows(rows: List<String>, toMove: Stone = Stone.BLACK): Board {
            val cells = cells(rows)
            val board = Board(rows.size)
            for (p in cells.indices) when (cells[p]) {
                'X', 'B' -> board.place(p, Stone.BLACK)
                'O', 'W' -> board.place(p, Stone.WHITE)
            }
            for (p in cells.indices) {
                require(board[p] == null || board.liberties(p) > 0) {
                    "Stone without liberties at ${Points.name(p, board.size)}"
                }
            }
            board.setToMove(toMove)
            return board
        }

        /** Label -> point of a diagram. */
        fun labels(rows: List<String>): Map<Char, Int> {
            val out = LinkedHashMap<Char, Int>()
            cells(rows).forEachIndexed { p, c ->
                if (c.isLowerCase()) require(out.put(c, p) == null) { "Label '$c' used twice" }
            }
            return out
        }

        /** Points of the marked stones ('B', 'W') of a diagram. */
        fun marked(rows: List<String>): IntArray {
            val cells = cells(rows)
            return cells.indices.filter { cells[it] == 'B' || cells[it] == 'W' }.toIntArray()
        }

        private fun cells(rows: List<String>): CharArray {
            val size = rows.size
            val out = CharArray(size * size)
            rows.forEachIndexed { y, row ->
                val parts = row.split(' ')
                require(parts.size == size) { "Row $y has ${parts.size} cells, expected $size: \"$row\"" }
                parts.forEachIndexed { x, cell ->
                    require(cell.length == 1 && (cell[0] in ".XOBW" || (cell[0] in 'a'..'w' && cell[0] != 'o'))) {
                        "Bad cell \"$cell\" in row $y"
                    }
                    out[y * size + x] = cell[0]
                }
            }
            return out
        }
    }
}
