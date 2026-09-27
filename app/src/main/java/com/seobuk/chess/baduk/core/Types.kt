package com.seobuk.chess.baduk.core

/** A move that places no stone. Points are `y * size + x`, x from the left, y from the TOP. */
const val PASS = -1

enum class Stone {
    BLACK, WHITE;

    val opposite: Stone get() = if (this == BLACK) WHITE else BLACK
}

enum class Illegal { OCCUPIED, SUICIDE, KO }

object Points {
    private const val LETTERS = "ABCDEFGHJKLMNOPQRST" // no I

    fun of(x: Int, y: Int, size: Int): Int = y * size + x
    fun x(p: Int, size: Int): Int = p % size
    fun y(p: Int, size: Int): Int = p / size

    /** "D4": letter = column from the left, number = row from the BOTTOM. PASS -> "pass". */
    fun name(p: Int, size: Int): String {
        if (p == PASS) return "pass"
        require(p in 0 until size * size) { "Bad point $p on $size" }
        return "${LETTERS[x(p, size)]}${size - y(p, size)}"
    }

    /** Inverse of [name], case-insensitive; -2 if invalid or off the board. */
    fun parse(name: String, size: Int): Int {
        if (name.equals("pass", ignoreCase = true)) return PASS
        if (name.length !in 2..3 || !name.drop(1).all { it in '0'..'9' }) return -2
        val x = LETTERS.indexOf(name[0].uppercaseChar())
        val row = name.drop(1).toInt()
        return if (x in 0 until size && row in 1..size) of(x, size - row, size) else -2
    }
}

/** Per-size lookup tables, shared by every board of that size. */
class Grid private constructor(val size: Int) {
    val n = size * size

    /** 4 slots per point (index `p * 4 + i`): the orthogonal neighbours first, then -1 padding. */
    val nbr = IntArray(n * 4) { -1 }

    /** Same layout as [nbr] for the diagonal neighbours. */
    val diag = IntArray(n * 4) { -1 }

    // Seeded by size: hashes are the same in every run.
    private val rnd = kotlin.random.Random(0x5EED + size)
    internal val stoneKeys = LongArray(n * 2) { rnd.nextLong() } // [(colour code - 1) * n + p]
    internal val koKeys = LongArray(n) { rnd.nextLong() }
    internal val whiteToMoveKey = rnd.nextLong()

    init {
        for (p in 0 until n) {
            val x = p % size
            val y = p / size
            val l = x > 0
            val r = x < size - 1
            val u = y > 0
            val d = y < size - 1
            var k = p * 4
            if (u) nbr[k++] = p - size
            if (l) nbr[k++] = p - 1
            if (r) nbr[k++] = p + 1
            if (d) nbr[k] = p + size
            k = p * 4
            if (u && l) diag[k++] = p - size - 1
            if (u && r) diag[k++] = p - size + 1
            if (d && l) diag[k++] = p + size - 1
            if (d && r) diag[k] = p + size + 1
        }
    }

    companion object {
        private val cache = arrayOfNulls<Grid>(20)

        // Unsynchronised on purpose: a Grid is immutable and deterministic, a race only builds it twice.
        fun of(size: Int): Grid {
            require(size in 2..19) { "Bad board size $size" }
            return cache[size] ?: Grid(size).also { cache[size] = it }
        }
    }
}
