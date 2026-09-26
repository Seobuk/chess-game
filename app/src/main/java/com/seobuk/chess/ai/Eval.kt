package com.seobuk.chess.ai

import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * PeSTO tapered evaluation (material + piece-square tables) plus bishop pair, rooks on open files,
 * passed pawns and a mop-up term for K+pieces vs lone K. Tables are written a8..h1 (as seen by White).
 */
internal object Eval {
    private val MG_VALUE = intArrayOf(82, 337, 365, 477, 1025, 0)
    private val EG_VALUE = intArrayOf(94, 281, 297, 512, 936, 0)
    private val PHASE_INC = intArrayOf(0, 0, 1, 1, 2, 4, 0) // by piece code type 1..6

    private val MG_PST = arrayOf(
        intArrayOf( // pawn
            0, 0, 0, 0, 0, 0, 0, 0,
            98, 134, 61, 95, 68, 126, 34, -11,
            -6, 7, 26, 31, 65, 56, 25, -20,
            -14, 13, 6, 21, 23, 12, 17, -23,
            -27, -2, -5, 12, 17, 6, 10, -25,
            -26, -4, -4, -10, 3, 3, 33, -12,
            -35, -1, -20, -23, -15, 24, 38, -22,
            0, 0, 0, 0, 0, 0, 0, 0,
        ),
        intArrayOf( // knight
            -167, -89, -34, -49, 61, -97, -15, -107,
            -73, -41, 72, 36, 23, 62, 7, -17,
            -47, 60, 37, 65, 84, 129, 73, 44,
            -9, 17, 19, 53, 37, 69, 18, 22,
            -13, 4, 16, 13, 28, 19, 21, -8,
            -23, -9, 12, 10, 19, 17, 25, -16,
            -29, -53, -12, -3, -1, 18, -14, -19,
            -105, -21, -58, -33, -17, -28, -19, -23,
        ),
        intArrayOf( // bishop
            -29, 4, -82, -37, -25, -42, 7, -8,
            -26, 16, -18, -13, 30, 59, 18, -47,
            -16, 37, 43, 40, 35, 50, 37, -2,
            -4, 5, 19, 50, 37, 37, 7, -2,
            -6, 13, 13, 26, 34, 12, 10, 4,
            0, 15, 15, 15, 14, 27, 18, 10,
            4, 15, 16, 0, 7, 21, 33, 1,
            -33, -3, -14, -21, -13, -12, -39, -21,
        ),
        intArrayOf( // rook
            32, 42, 32, 51, 63, 9, 31, 43,
            27, 32, 58, 62, 80, 67, 26, 44,
            -5, 19, 26, 36, 17, 45, 61, 16,
            -24, -11, 7, 26, 24, 35, -8, -20,
            -36, -26, -12, -1, 9, -7, 6, -23,
            -45, -25, -16, -17, 3, 0, -5, -33,
            -44, -16, -20, -9, -1, 11, -6, -71,
            -19, -13, 1, 17, 16, 7, -37, -26,
        ),
        intArrayOf( // queen
            -28, 0, 29, 12, 59, 44, 43, 45,
            -24, -39, -5, 1, -16, 57, 28, 54,
            -13, -17, 7, 8, 29, 56, 47, 57,
            -27, -27, -16, -16, -1, 17, -2, 1,
            -9, -26, -9, -10, -2, -4, 3, -3,
            -14, 2, -11, -2, -5, 2, 14, 5,
            -35, -8, 11, 2, 8, 15, -3, 1,
            -1, -18, -9, 10, -15, -25, -31, -50,
        ),
        intArrayOf( // king
            -65, 23, 16, -15, -56, -34, 2, 13,
            29, -1, -20, -7, -8, -4, -38, -29,
            -9, 24, 2, -16, -20, 6, 22, -22,
            -17, -20, -12, -27, -30, -25, -14, -36,
            -49, -1, -27, -39, -46, -44, -33, -51,
            -14, -14, -22, -46, -44, -30, -15, -27,
            1, 7, -8, -64, -43, -16, 9, 8,
            -15, 36, 12, -54, 8, -28, 24, 14,
        ),
    )

    private val EG_PST = arrayOf(
        intArrayOf( // pawn
            0, 0, 0, 0, 0, 0, 0, 0,
            178, 173, 158, 134, 147, 132, 165, 187,
            94, 100, 85, 67, 56, 53, 82, 84,
            32, 24, 13, 5, -2, 4, 17, 17,
            13, 9, -3, -7, -7, -8, 3, -1,
            4, 7, -6, 1, 0, -5, -1, -8,
            13, 8, 8, 10, 13, 0, 2, -7,
            0, 0, 0, 0, 0, 0, 0, 0,
        ),
        intArrayOf( // knight
            -58, -38, -13, -28, -31, -27, -63, -99,
            -25, -8, -25, -2, -9, -25, -24, -52,
            -24, -20, 10, 9, -1, -9, -19, -41,
            -17, 3, 22, 22, 22, 11, 8, -18,
            -18, -6, 16, 25, 16, 17, 4, -18,
            -23, -3, -1, 15, 10, -3, -20, -22,
            -42, -20, -10, -5, -2, -20, -23, -44,
            -29, -51, -23, -15, -22, -18, -50, -64,
        ),
        intArrayOf( // bishop
            -14, -21, -11, -8, -7, -9, -17, -24,
            -8, -4, 7, -12, -3, -13, -4, -14,
            2, -8, 0, -1, -2, 6, 0, 4,
            -3, 9, 12, 9, 14, 10, 3, 2,
            -6, 3, 13, 19, 7, 10, -3, -9,
            -12, -3, 8, 10, 13, 3, -7, -15,
            -14, -18, -7, -1, 4, -9, -15, -27,
            -23, -9, -23, -5, -9, -16, -5, -17,
        ),
        intArrayOf( // rook
            13, 10, 18, 15, 12, 12, 8, 5,
            11, 13, 13, 11, -3, 3, 8, 3,
            7, 7, 7, 5, 4, -3, -5, -3,
            4, 3, 13, 1, 2, 1, -1, 2,
            3, 5, 8, 4, -5, -6, -8, -11,
            -4, 0, -5, -1, -7, -12, -8, -16,
            -6, -6, 0, 2, -9, -9, -11, -3,
            -9, 2, 3, -1, -5, -13, 4, -20,
        ),
        intArrayOf( // queen
            -9, 22, 22, 27, 27, 19, 10, 20,
            -17, 20, 32, 41, 58, 25, 30, 0,
            -20, 6, 9, 49, 47, 35, 19, 9,
            3, 22, 24, 45, 57, 40, 57, 36,
            -18, 28, 19, 47, 31, 34, 39, 23,
            -16, -27, 15, 6, 9, 17, 10, 5,
            -22, -23, -30, -16, -16, -23, -36, -32,
            -33, -28, -22, -43, -5, -32, -20, -41,
        ),
        intArrayOf( // king
            -74, -35, -18, -18, -11, 15, 4, -17,
            -12, 17, 14, 17, 17, 38, 23, 11,
            10, 17, 23, 15, 20, 45, 44, 13,
            -8, 22, 24, 27, 26, 33, 26, 3,
            -18, -4, 21, 24, 27, 23, 9, -11,
            -19, -3, 11, 21, 23, 16, 7, -9,
            -27, -11, 4, 13, 14, 4, -5, -17,
            -53, -34, -21, -11, -28, -14, -24, -43,
        ),
    )

    // Indexed by (pieceCode shl 6) or sq, White POV (black entries negative).
    private val MG = IntArray(16 * 64)
    private val EG = IntArray(16 * 64)

    // Squares in front of a pawn on its own and adjacent files; a pawn is passed if no enemy pawn is there.
    private val PASSED_W = LongArray(64)
    private val PASSED_B = LongArray(64)
    private val FILE_MASK = LongArray(8) { f -> (0 until 8).fold(0L) { acc, r -> acc or (1L shl (r * 8 + f)) } }

    // By rank relative to the pawn's own side.
    private val PASSED_MG = intArrayOf(0, 2, 5, 10, 15, 22, 30, 0)
    private val PASSED_EG = intArrayOf(0, 5, 10, 20, 35, 55, 80, 0)

    init {
        for (t in 0..5) for (sq in 0 until 64) {
            MG[((t + 1) shl 6) or sq] = MG_VALUE[t] + MG_PST[t][sq xor 56]
            EG[((t + 1) shl 6) or sq] = EG_VALUE[t] + EG_PST[t][sq xor 56]
            MG[((8 or (t + 1)) shl 6) or sq] = -(MG_VALUE[t] + MG_PST[t][sq])
            EG[((8 or (t + 1)) shl 6) or sq] = -(EG_VALUE[t] + EG_PST[t][sq])
        }
        for (sq in 0 until 64) {
            val f = sq and 7
            val r = sq shr 3
            for (ff in max(0, f - 1)..min(7, f + 1)) {
                for (rr in r + 1..7) PASSED_W[sq] = PASSED_W[sq] or (1L shl (rr * 8 + ff))
                for (rr in 0 until r) PASSED_B[sq] = PASSED_B[sq] or (1L shl (rr * 8 + ff))
            }
        }
    }

    /** Static evaluation in centipawns, side-to-move POV. */
    fun evaluate(p: Position): Int {
        var mg = 0
        var eg = 0
        var phase = 0
        var wp = 0L
        var bp = 0L
        var rooks = 0L
        var wBishops = 0
        var bBishops = 0
        var wPieces = 0
        var bPieces = 0
        for (sq in 0 until 64) {
            val c = p.pieceCode(sq)
            if (c == 0) continue
            mg += MG[(c shl 6) or sq]
            eg += EG[(c shl 6) or sq]
            phase += PHASE_INC[c and 7]
            when (c) {
                1 -> wp = wp or (1L shl sq)
                9 -> bp = bp or (1L shl sq)
                3 -> wBishops++
                11 -> bBishops++
                4, 12 -> rooks = rooks or (1L shl sq)
            }
            if (c != 6 && c != 14) if (c < 8) wPieces++ else bPieces++
        }
        if (wBishops >= 2) { mg += 30; eg += 45 }
        if (bBishops >= 2) { mg -= 30; eg -= 45 }

        var b = wp
        while (b != 0L) {
            val sq = java.lang.Long.numberOfTrailingZeros(b)
            b = b and (b - 1)
            if (bp and PASSED_W[sq] == 0L) {
                mg += PASSED_MG[sq shr 3]; eg += PASSED_EG[sq shr 3]
            }
        }
        b = bp
        while (b != 0L) {
            val sq = java.lang.Long.numberOfTrailingZeros(b)
            b = b and (b - 1)
            if (wp and PASSED_B[sq] == 0L) {
                mg -= PASSED_MG[7 - (sq shr 3)]; eg -= PASSED_EG[7 - (sq shr 3)]
            }
        }
        b = rooks
        while (b != 0L) {
            val sq = java.lang.Long.numberOfTrailingZeros(b)
            b = b and (b - 1)
            val white = p.pieceCode(sq) == 4
            val fm = FILE_MASK[sq and 7]
            if ((if (white) wp else bp) and fm != 0L) continue
            val open = (if (white) bp else wp) and fm == 0L
            val sMg = if (open) 25 else 12
            val sEg = if (open) 10 else 5
            if (white) { mg += sMg; eg += sEg } else { mg -= sMg; eg -= sEg }
        }

        // Mop-up: drive a lone king to the edge and bring our king closer.
        if (bPieces == 0 && wPieces > 0 && eg > 300) eg += mopUp(p, Side.BLACK)
        else if (wPieces == 0 && bPieces > 0 && eg < -300) eg -= mopUp(p, Side.WHITE)

        val ph = min(phase, 24)
        val score = (mg * ph + eg * (24 - ph)) / 24
        return if (p.sideToMove == Side.WHITE) score else -score
    }

    private fun mopUp(p: Position, loser: Side): Int {
        val lk = p.kingSquare(loser)
        val wk = p.kingSquare(loser.opposite)
        val lf = lk and 7
        val lr = lk shr 3
        val centerDist = max(3 - lf, lf - 4) + max(3 - lr, lr - 4)
        val kingDist = abs(lf - (wk and 7)) + abs(lr - (wk shr 3))
        return 12 * centerDist + 4 * (14 - kingDist)
    }
}
