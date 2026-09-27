package com.seobuk.chess.baduk.core

object Scoring {
    /**
     * [blackTerritory]/[whiteTerritory]: empty points (and points under dead stones) surrounded by one colour.
     * [blackPrisoners]: white stones held by Black = Black's captures + dead white stones (same for White).
     * [black]/[white]: totals under the rule that produced the score, komi included in [white].
     * [winner] is null on a tie. [owner]: per point, who owns an empty-or-dead point; null = neutral (공배)
     * or a living stone.
     */
    data class Score(
        val blackTerritory: Int,
        val whiteTerritory: Int,
        val blackPrisoners: Int,
        val whitePrisoners: Int,
        val komi: Double,
        val black: Double,
        val white: Double,
        val winner: Stone?,
        val margin: Double,
        val owner: Array<Stone?>,
    )

    /**
     * Korean rules: territory + prisoners, White gets komi.
     * ponytail: seki eyes are counted as territory; detect seki groups if scores near seki ever matter.
     */
    fun territory(board: Board, dead: Set<Int>, komi: Double): Score = score(board, dead, komi, area = false)

    /** Living stones + surrounded points, White gets komi. The prisoner fields are filled in but not counted. */
    fun area(board: Board, dead: Set<Int>, komi: Double): Score = score(board, dead, komi, area = true)

    private fun score(board: Board, dead: Set<Int>, komi: Double, area: Boolean): Score {
        val n = board.size * board.size
        val nbr = board.grid.nbr
        // A dead point takes its whole group with it; empty points in 'dead' are ignored.
        val cell = IntArray(n) { board.color(it) }
        val deadCount = IntArray(3)
        for (d in dead) for (s in board.group(d)) {
            if (cell[s] != Board.EMPTY) deadCount[cell[s]]++
            cell[s] = Board.EMPTY
        }

        val owner = arrayOfNulls<Stone>(n)
        val points = IntArray(3)
        val seen = BooleanArray(n)
        val region = IntArray(n)
        for (start in 0 until n) {
            if (cell[start] != Board.EMPTY || seen[start]) continue
            var count = 0
            var borders = 0 // bit 1 = touches black, bit 2 = touches white
            region[count++] = start
            seen[start] = true
            var i = 0
            while (i < count) {
                val k = region[i++] * 4
                for (j in k until k + 4) {
                    val q = nbr[j]
                    if (q < 0) break
                    if (cell[q] != Board.EMPTY) borders = borders or cell[q]
                    else if (!seen[q]) {
                        seen[q] = true
                        region[count++] = q
                    }
                }
            }
            if (borders == Board.BLACK_CODE || borders == Board.WHITE_CODE) {
                points[borders] += count
                val stone = if (borders == Board.BLACK_CODE) Stone.BLACK else Stone.WHITE
                for (j in 0 until count) owner[region[j]] = stone
            }
        }

        val blackPrisoners = board.captured(Stone.BLACK) + deadCount[Board.WHITE_CODE]
        val whitePrisoners = board.captured(Stone.WHITE) + deadCount[Board.BLACK_CODE]
        val black = points[Board.BLACK_CODE] +
            (if (area) cell.count { it == Board.BLACK_CODE } else blackPrisoners).toDouble()
        val white = points[Board.WHITE_CODE] + komi +
            (if (area) cell.count { it == Board.WHITE_CODE } else whitePrisoners)
        return Score(
            blackTerritory = points[Board.BLACK_CODE],
            whiteTerritory = points[Board.WHITE_CODE],
            blackPrisoners = blackPrisoners,
            whitePrisoners = whitePrisoners,
            komi = komi,
            black = black,
            white = white,
            winner = if (black > white) Stone.BLACK else if (white > black) Stone.WHITE else null,
            margin = kotlin.math.abs(black - white),
            owner = owner,
        )
    }
}
