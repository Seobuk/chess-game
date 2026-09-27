package com.seobuk.chess.baduk.core

/**
 * A game from the empty board. Over after two consecutive passes, a resignation or, in [captureGo],
 * the first capture.
 */
class BadukGame(val size: Int, val komi: Double = 6.5, val captureGo: Boolean = false) {
    private val live = Board(size)
    private val moveList = ArrayList<Int>()

    /** Live position; don't mutate it (copy() first). */
    val board: Board get() = live

    /** PASS included. */
    val moves: List<Int> get() = moveList.toList()

    var resigned: Stone? = null
        private set

    /** captureGo only: who made the first capture. */
    var captureWinner: Stone? = null
        private set

    val isOver: Boolean get() = resigned != null || captureWinner != null || live.passes >= 2

    fun play(p: Int): Boolean {
        if (isOver) return false
        val mover = live.toMove
        if (!live.play(p)) return false
        moveList.add(p)
        if (captureGo && live.lastCaptureCount > 0) captureWinner = mover
        return true
    }

    fun pass(): Boolean = play(PASS)

    fun resign(by: Stone) {
        if (!isOver) resigned = by
    }

    /** Takes back the last move; after a resignation it takes back only the resignation. */
    fun undo(): Boolean {
        if (resigned != null) {
            resigned = null
            return true
        }
        if (moveList.isEmpty()) return false
        moveList.removeAt(moveList.lastIndex)
        // ponytail: replays the game (a few hundred moves at most); keep snapshots if undo ever gets hot.
        live.copyFrom(boardAt(moveList.size))
        captureWinner = null // the winning capture is always the last move
        return true
    }

    /** Fresh copy of the position after [n] moves. */
    fun boardAt(n: Int): Board {
        require(n in 0..moveList.size) { "n $n out of 0..${moveList.size}" }
        val b = Board(size)
        for (i in 0 until n) b.play(moveList[i])
        return b
    }

    fun copy(): BadukGame = BadukGame(size, komi, captureGo).also {
        it.live.copyFrom(live)
        it.moveList.addAll(moveList)
        it.resigned = resigned
        it.captureWinner = captureWinner
    }
}
