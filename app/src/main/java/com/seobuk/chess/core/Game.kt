package com.seobuk.chess.core

enum class GameStatus { ONGOING, CHECKMATE, STALEMATE, DRAW_FIFTY_MOVE, DRAW_THREEFOLD, DRAW_INSUFFICIENT_MATERIAL }

class Game private constructor(
    val startFen: String,
    private val pos: Position,
    private val moveList: ArrayList<Move>,
    private val sanList: ArrayList<String>,
) {
    constructor(startFen: String = Position.START_FEN) :
        this(startFen, Position.fromFen(startFen), ArrayList(), ArrayList())

    /** Live position; don't mutate it (copy() first). */
    val position: Position get() = pos
    val moves: List<Move> get() = moveList.toList()
    val sanMoves: List<String> get() = sanList.toList()

    var status: GameStatus = computeStatus()
        private set

    val winner: Side? get() = if (status == GameStatus.CHECKMATE) pos.sideToMove.opposite else null
    val isOver: Boolean get() = status != GameStatus.ONGOING

    fun play(move: Move): Boolean {
        if (isOver || !pos.isLegal(move)) return false
        sanList.add(pos.san(move))
        moveList.add(move)
        pos.makeMove(move)
        status = computeStatus()
        return true
    }

    fun undo(): Boolean {
        if (moveList.isEmpty()) return false
        pos.unmakeMove()
        moveList.removeAt(moveList.lastIndex)
        sanList.removeAt(sanList.lastIndex)
        status = computeStatus()
        return true
    }

    fun positionAt(ply: Int): Position {
        require(ply in 0..moveList.size) { "ply $ply out of 0..${moveList.size}" }
        val p = pos.copy()
        repeat(moveList.size - ply) { p.unmakeMove() }
        return p
    }

    fun copy(): Game = Game(startFen, pos.copy(), ArrayList(moveList), ArrayList(sanList))

    // Checkmate outranks the fifty-move rule when both happen on the same move.
    private fun computeStatus(): GameStatus = when {
        !pos.hasLegalMove() -> if (pos.isInCheck()) GameStatus.CHECKMATE else GameStatus.STALEMATE
        pos.halfmoveClock >= 100 -> GameStatus.DRAW_FIFTY_MOVE
        pos.repetitionCount() >= 3 -> GameStatus.DRAW_THREEFOLD
        pos.isInsufficientMaterial() -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
        else -> GameStatus.ONGOING
    }
}
