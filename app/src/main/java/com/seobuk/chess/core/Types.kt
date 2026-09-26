package com.seobuk.chess.core

enum class Side {
    WHITE, BLACK;

    val opposite: Side get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val value: Int) { PAWN(100), KNIGHT(320), BISHOP(330), ROOK(500), QUEEN(900), KING(0) }

data class Piece(val type: PieceType, val side: Side)

/** Squares are 0..63: a1=0, b1=1 ... h8=63. */
object Squares {
    fun file(sq: Int): Int = sq and 7
    fun rank(sq: Int): Int = sq shr 3

    fun name(sq: Int): String {
        require(sq in 0..63) { "Bad square $sq" }
        return "${'a' + file(sq)}${'1' + rank(sq)}"
    }

    fun parse(name: String): Int {
        if (name.length != 2) return -1
        val f = name[0].lowercaseChar() - 'a'
        val r = name[1] - '1'
        return if (f in 0..7 && r in 0..7) r * 8 + f else -1
    }
}

data class Move(val from: Int, val to: Int, val promotion: PieceType? = null) {
    fun uci(): String =
        Squares.name(from) + Squares.name(to) + (promotion?.let { PROMO_CHARS[it.ordinal].toString() } ?: "")

    companion object {
        private const val PROMO_CHARS = "pnbrqk"
        private val TYPES = PieceType.entries.toTypedArray()
        private val cache = arrayOfNulls<Move>(5 shl 12)

        fun fromUci(uci: String): Move? {
            if (uci.length != 4 && uci.length != 5) return null
            val from = Squares.parse(uci.substring(0, 2))
            val to = Squares.parse(uci.substring(2, 4))
            if (from < 0 || to < 0 || from == to) return null
            val promo = if (uci.length == 5) {
                when (uci[4].lowercaseChar()) {
                    'n' -> PieceType.KNIGHT
                    'b' -> PieceType.BISHOP
                    'r' -> PieceType.ROOK
                    'q' -> PieceType.QUEEN
                    else -> return null
                }
            } else null
            return Move(from, to, promo)
        }

        /**
         * Int form used by the Position fast path:
         * `from | (to shl 6) | (promotion.ordinal shl 12)`, promotion bits 0 = none (KNIGHT=1 .. QUEEN=4).
         */
        fun encode(move: Move): Int = move.from or (move.to shl 6) or ((move.promotion?.ordinal ?: 0) shl 12)

        /** Inverse of [encode]; instances are cached, so this does not allocate after warm-up. */
        fun decode(code: Int): Move = cache[code] ?: Move(
            code and 63,
            (code shr 6) and 63,
            if (code shr 12 == 0) null else TYPES[code shr 12],
        ).also { cache[code] = it }
    }
}
