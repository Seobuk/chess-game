package com.seobuk.chess.core

import kotlin.random.Random

// Piece codes on the board: (side shl 3) or type, type 1..6 = PieceType.ordinal + 1, 0 = empty.
private const val PAWN_T = 1
private const val KNIGHT_T = 2
private const val BISHOP_T = 3
private const val ROOK_T = 4
private const val QUEEN_T = 5
private const val KING_T = 6
private const val WHITE_I = 0
private const val BLACK_I = 1
private const val NULL_MOVE = -1
private const val PIECE_CHARS = "?PNBRQK"

private fun onBoard(f: Int, r: Int) = f in 0..7 && r in 0..7

private fun stepTargets(offsets: Array<IntArray>): Array<IntArray> = Array(64) { sq ->
    offsets.filter { onBoard((sq and 7) + it[0], (sq shr 3) + it[1]) }
        .map { sq + it[0] + it[1] * 8 }.toIntArray()
}

private val KNIGHT_TARGETS = stepTargets(
    arrayOf(intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
        intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)),
)
private val KING_TARGETS = stepTargets(
    arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
        intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)),
)

// Directions 0..3 orthogonal (N S E W), 4..7 diagonal (NE NW SE SW). RAYS[(dir shl 6) or sq] in walking order.
private val DIR_DF = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)
private val DIR_DR = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1)
private val RAYS = Array(8 * 64) { idx ->
    val d = idx shr 6
    val sq = idx and 63
    val out = ArrayList<Int>(7)
    var f = (sq and 7) + DIR_DF[d]
    var r = (sq shr 3) + DIR_DR[d]
    while (onBoard(f, r)) {
        out.add(r * 8 + f); f += DIR_DF[d]; r += DIR_DR[d]
    }
    out.toIntArray()
}

// PAWN_ATTACKS[(side shl 6) or sq] = squares a pawn of `side` standing on sq attacks.
private val PAWN_ATTACKS = Array(2 * 64) { idx ->
    val side = idx shr 6
    val sq = idx and 63
    val r = (sq shr 3) + if (side == WHITE_I) 1 else -1
    intArrayOf(-1, 1).filter { onBoard((sq and 7) + it, r) }.map { r * 8 + (sq and 7) + it }.toIntArray()
}

// Castling bits K=1 Q=2 k=4 q=8; rights &= mask[from] & mask[to] on every move.
private val CASTLE_MASK = IntArray(64) { 15 }.also {
    it[0] = 15 and 2.inv(); it[4] = 15 and 3.inv(); it[7] = 15 and 1.inv()
    it[56] = 15 and 8.inv(); it[60] = 15 and 12.inv(); it[63] = 15 and 4.inv()
}

private val zrnd = Random(0x5EED_C4E55L)
private val Z_PIECE = LongArray(16 * 64) { zrnd.nextLong() }
private val Z_EP = LongArray(8) { zrnd.nextLong() }
private val Z_SIDE = zrnd.nextLong()
private val Z_CASTLE = LongArray(4) { zrnd.nextLong() }.let { bits ->
    LongArray(16) { m -> (0..3).fold(0L) { acc, b -> if (m shr b and 1 != 0) acc xor bits[b] else acc } }
}

private val PIECES: Array<Piece?> = Array(16) { code ->
    val t = code and 7
    if (t in 1..6) Piece(PieceType.entries[t - 1], Side.entries[code shr 3]) else null
}

private val SAN_RE = Regex("^([NBRQK])?([a-h])?([1-8])?[x:]?([a-h][1-8])(?:=?([NBRQnbrq]))?$")

/**
 * Mutable chess position (IntArray mailbox, incremental Zobrist, make/unmake with an undo stack).
 *
 * Fast path for search (allocation-free): moves as Int codes, see [Move.encode].
 * [generateMoves] writes pseudo-legal codes; [tryMakeMove] makes one and returns false (already undone)
 * if it leaves the mover's king in check. Undo either kind with [unmakeMove].
 */
class Position private constructor() {
    private val board = IntArray(64)
    private val counts = IntArray(16)
    private val kingSq = IntArray(2)
    private var stm = WHITE_I

    var castlingRights = 0; private set
    var enPassantSquare = -1; private set
    var halfmoveClock = 0; private set
    var fullmoveNumber = 1; private set
    var hash = 0L; private set

    // Undo stack; uHash doubles as the repetition history.
    private var sp = 0
    private var uMove = IntArray(128)
    private var uCaptured = IntArray(128)
    private var uCastle = IntArray(128)
    private var uEp = IntArray(128)
    private var uHalf = IntArray(128)
    private var uFull = IntArray(128)
    private var uHash = LongArray(128)

    val sideToMove: Side get() = if (stm == WHITE_I) Side.WHITE else Side.BLACK

    fun pieceAt(sq: Int): Piece? = PIECES[board[sq]]

    /** Raw board code for engines: 0 = empty, else `(side.ordinal shl 3) or (type.ordinal + 1)`. */
    fun pieceCode(sq: Int): Int = board[sq]

    fun kingSquare(side: Side): Int = kingSq[side.ordinal]

    fun pieceCount(type: PieceType, side: Side): Int = counts[(side.ordinal shl 3) or (type.ordinal + 1)]

    fun isInCheck(): Boolean = attacked(kingSq[stm], stm xor 1)

    fun isSquareAttacked(sq: Int, by: Side): Boolean = attacked(sq, by.ordinal)

    fun isCapture(move: Move): Boolean =
        board[move.to] != 0 ||
            ((board[move.from] and 7) == PAWN_T && move.to == enPassantSquare && (move.to and 7) != (move.from and 7))

    fun legalMoves(): List<Move> {
        val buf = IntArray(256)
        val n = generateMoves(buf)
        val res = ArrayList<Move>(n)
        for (i in 0 until n) {
            if (tryMakeMove(buf[i])) {
                unmakeMove()
                res.add(Move.decode(buf[i]))
            }
        }
        return res
    }

    fun isLegal(move: Move): Boolean {
        if (move.from !in 0..63 || move.to !in 0..63) return false
        if (move.promotion == PieceType.PAWN || move.promotion == PieceType.KING) return false
        val code = Move.encode(move)
        val buf = IntArray(256)
        val n = generateMoves(buf)
        for (i in 0 until n) {
            if (buf[i] == code) return tryMakeMove(code).also { if (it) unmakeMove() }
        }
        return false
    }

    fun hasLegalMove(): Boolean {
        val buf = IntArray(256)
        val n = generateMoves(buf)
        for (i in 0 until n) {
            if (tryMakeMove(buf[i])) {
                unmakeMove(); return true
            }
        }
        return false
    }

    /** Caller guarantees legality (use [isLegal] for untrusted input). */
    fun makeMove(move: Move) {
        val p = board[move.from]
        require(p != 0 && (p shr 3) == stm) { "No piece of the side to move on ${move.from}" }
        doMove(Move.encode(move))
    }

    /** Makes a pseudo-legal move code from [generateMoves]; if it is illegal, undoes it and returns false. */
    fun tryMakeMove(code: Int): Boolean {
        val us = stm
        doMove(code)
        if (attacked(kingSq[us], us xor 1)) {
            unmakeMove(); return false
        }
        return true
    }

    fun unmakeMove() {
        check(sp > 0) { "No move to unmake" }
        sp--
        stm = stm xor 1
        castlingRights = uCastle[sp]
        enPassantSquare = uEp[sp]
        halfmoveClock = uHalf[sp]
        fullmoveNumber = uFull[sp]
        hash = uHash[sp]
        val code = uMove[sp]
        if (code == NULL_MOVE) return
        val us = stm
        val from = code and 63
        val to = (code shr 6) and 63
        val placed = board[to]
        val piece = if (code shr 12 != 0) {
            counts[placed]--
            ((us shl 3) or PAWN_T).also { counts[it]++ }
        } else placed
        board[to] = 0
        board[from] = piece
        val type = piece and 7
        if (type == KING_T) {
            kingSq[us] = from
            if (to - from == 2) moveRook(from + 1, from + 3, us)
            else if (from - to == 2) moveRook(from - 1, from - 4, us)
        }
        val captured = uCaptured[sp]
        if (captured != 0) {
            val capSq = if (type == PAWN_T && to == enPassantSquare) (if (us == WHITE_I) to - 8 else to + 8) else to
            board[capSq] = captured
            counts[captured]++
        }
    }

    /** For null-move pruning; don't call while in check. Undo with [unmakeNullMove] (or [unmakeMove]). */
    fun makeNullMove() {
        push(NULL_MOVE)
        uCaptured[sp - 1] = 0
        var h = hash
        val ep = enPassantSquare
        if (ep >= 0 && epCapturable(ep, stm)) h = h xor Z_EP[ep and 7]
        enPassantSquare = -1
        halfmoveClock = 0 // keeps repetition detection from looking across the null move
        stm = stm xor 1
        hash = h xor Z_SIDE
    }

    fun unmakeNullMove() = unmakeMove()

    /**
     * Writes pseudo-legal move codes (see [Move.encode]) into [out] (size >= 256) and returns the count.
     * [tacticalOnly]: captures (incl. en passant) and queen promotions only, no castling.
     */
    fun generateMoves(out: IntArray, tacticalOnly: Boolean = false): Int {
        val us = stm
        var n = 0
        for (sq in 0 until 64) {
            val p = board[sq]
            if (p == 0 || (p shr 3) != us) continue
            n = when (p and 7) {
                PAWN_T -> genPawn(sq, us, out, n, tacticalOnly)
                KNIGHT_T -> genStep(sq, KNIGHT_TARGETS[sq], us, out, n, tacticalOnly)
                BISHOP_T -> genSlide(sq, 4, 8, us, out, n, tacticalOnly)
                ROOK_T -> genSlide(sq, 0, 4, us, out, n, tacticalOnly)
                QUEEN_T -> genSlide(sq, 0, 8, us, out, n, tacticalOnly)
                else -> genStep(sq, KING_TARGETS[sq], us, out, n, tacticalOnly)
            }
        }
        if (!tacticalOnly) n = genCastles(us, out, n)
        return n
    }

    /** Occurrences of the current position since the last irreversible move, counting itself (>= 1). */
    fun repetitionCount(): Int {
        var count = 1
        val limit = maxOf(0, sp - halfmoveClock)
        var i = sp - 2
        while (i >= limit) {
            if (uHash[i] == hash) count++
            i -= 2
        }
        return count
    }

    /** KvK, KBvK, KNvK, and bishops-only endings where every bishop stands on one square colour. */
    fun isInsufficientMaterial(): Boolean {
        for (c in 0..1) {
            val s = c shl 3
            if (counts[s or PAWN_T] + counts[s or ROOK_T] + counts[s or QUEEN_T] > 0) return false
        }
        val knights = counts[KNIGHT_T] + counts[8 or KNIGHT_T]
        val bishops = counts[BISHOP_T] + counts[8 or BISHOP_T]
        if (knights + bishops <= 1) return true
        if (knights > 0) return false
        var colours = 0
        for (sq in 0 until 64) {
            if ((board[sq] and 7) == BISHOP_T) colours = colours or (1 shl (((sq shr 3) + (sq and 7)) and 1))
        }
        return colours != 3
    }

    fun copy(): Position {
        val p = Position()
        board.copyInto(p.board)
        counts.copyInto(p.counts)
        kingSq.copyInto(p.kingSq)
        p.stm = stm
        p.castlingRights = castlingRights
        p.enPassantSquare = enPassantSquare
        p.halfmoveClock = halfmoveClock
        p.fullmoveNumber = fullmoveNumber
        p.hash = hash
        p.sp = sp
        p.uMove = uMove.copyOf()
        p.uCaptured = uCaptured.copyOf()
        p.uCastle = uCastle.copyOf()
        p.uEp = uEp.copyOf()
        p.uHalf = uHalf.copyOf()
        p.uFull = uFull.copyOf()
        p.uHash = uHash.copyOf()
        return p
    }

    fun fen(): String {
        val sb = StringBuilder(90)
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val c = board[rank * 8 + file]
                if (c == 0) {
                    empty++; continue
                }
                if (empty > 0) sb.append(empty)
                empty = 0
                sb.append(pieceChar(c))
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(if (stm == WHITE_I) " w " else " b ")
        if (castlingRights == 0) sb.append('-')
        for (i in 0..3) if (castlingRights shr i and 1 != 0) sb.append("KQkq"[i])
        sb.append(' ').append(if (enPassantSquare >= 0) Squares.name(enPassantSquare) else "-")
        sb.append(' ').append(halfmoveClock).append(' ').append(fullmoveNumber)
        return sb.toString()
    }

    override fun toString(): String = fen()

    fun san(move: Move): String {
        require(isLegal(move)) { "Illegal move ${move.uci()} in ${fen()}" }
        val code = Move.encode(move)
        val from = move.from
        val to = move.to
        val piece = board[from]
        val type = piece and 7
        val sb = StringBuilder(8)
        if (type == KING_T && (to - from == 2 || from - to == 2)) {
            sb.append(if (to > from) "O-O" else "O-O-O")
        } else if (type == PAWN_T) {
            if (isCapture(move)) sb.append('a' + (from and 7)).append('x')
            sb.append(Squares.name(to))
            move.promotion?.let { sb.append('=').append(PIECE_CHARS[it.ordinal + 1]) }
        } else {
            sb.append(PIECE_CHARS[type])
            var ambiguous = false
            var sameFile = false
            var sameRank = false
            val buf = IntArray(256)
            val n = generateMoves(buf)
            for (i in 0 until n) {
                val c = buf[i]
                val f = c and 63
                if (f == from || ((c shr 6) and 63) != to || board[f] != piece || !tryMakeMove(c)) continue
                unmakeMove()
                ambiguous = true
                if ((f and 7) == (from and 7)) sameFile = true
                if ((f shr 3) == (from shr 3)) sameRank = true
            }
            if (ambiguous) {
                when {
                    !sameFile -> sb.append('a' + (from and 7))
                    !sameRank -> sb.append('1' + (from shr 3))
                    else -> sb.append(Squares.name(from))
                }
            }
            if (isCapture(move)) sb.append('x')
            sb.append(Squares.name(to))
        }
        doMove(code)
        if (isInCheck()) sb.append(if (hasLegalMove()) '+' else '#')
        unmakeMove()
        return sb.toString()
    }

    /** Accepts san() output and common variants ("0-0", over-disambiguation, missing '=' or 'x', +#!? suffixes). */
    fun parseSan(san: String): Move? {
        val s = san.trim().removeSuffix("e.p.").trim().trimEnd('+', '#', '!', '?')
        val castle = s.replace('0', 'O').uppercase()
        if (castle == "O-O" || castle == "O-O-O") {
            val k = kingSq[stm]
            val m = Move(k, if (castle == "O-O") k + 2 else k - 2)
            return if (isLegal(m)) m else null
        }
        val g = SAN_RE.matchEntire(s)?.groupValues ?: return null
        val type = if (g[1].isEmpty()) PAWN_T else PIECE_CHARS.indexOf(g[1][0])
        val fromFile = if (g[2].isEmpty()) -1 else g[2][0] - 'a'
        val fromRank = if (g[3].isEmpty()) -1 else g[3][0] - '1'
        val to = Squares.parse(g[4])
        val promo = if (g[5].isEmpty()) 0 else PIECE_CHARS.indexOf(g[5][0].uppercaseChar()) - 1
        var found: Move? = null
        for (m in legalMoves()) {
            if (m.to != to || (board[m.from] and 7) != type) continue
            if (fromFile >= 0 && (m.from and 7) != fromFile) continue
            if (fromRank >= 0 && (m.from shr 3) != fromRank) continue
            if (type == PAWN_T && fromFile < 0 && (m.from and 7) != (to and 7)) continue
            val wanted = if (promo == 0 && m.promotion != null) PieceType.QUEEN.ordinal else promo
            if ((m.promotion?.ordinal ?: 0) != wanted) continue
            if (found != null) return null // ambiguous
            found = m
        }
        return found
    }

    /** Recomputes the Zobrist key from scratch (tests / FEN load). */
    internal fun computeHash(): Long {
        var h = 0L
        for (sq in 0 until 64) if (board[sq] != 0) h = h xor Z_PIECE[(board[sq] shl 6) or sq]
        h = h xor Z_CASTLE[castlingRights]
        val ep = enPassantSquare
        if (ep >= 0 && epCapturable(ep, stm)) h = h xor Z_EP[ep and 7]
        if (stm == BLACK_I) h = h xor Z_SIDE
        return h
    }

    // ---- internals ----

    private fun pieceChar(code: Int): Char {
        val c = PIECE_CHARS[code and 7]
        return if (code shr 3 == BLACK_I) c.lowercaseChar() else c
    }

    private fun push(code: Int) {
        if (sp == uMove.size) {
            val n = sp * 2
            uMove = uMove.copyOf(n); uCaptured = uCaptured.copyOf(n); uCastle = uCastle.copyOf(n)
            uEp = uEp.copyOf(n); uHalf = uHalf.copyOf(n); uFull = uFull.copyOf(n); uHash = uHash.copyOf(n)
        }
        uMove[sp] = code
        uCastle[sp] = castlingRights
        uEp[sp] = enPassantSquare
        uHalf[sp] = halfmoveClock
        uFull[sp] = fullmoveNumber
        uHash[sp] = hash
        sp++
    }

    private fun doMove(code: Int) {
        val from = code and 63
        val to = (code shr 6) and 63
        val promo = code shr 12
        val piece = board[from]
        val type = piece and 7
        val us = stm
        val them = us xor 1
        push(code)
        var h = hash
        val ep = enPassantSquare
        if (ep >= 0 && epCapturable(ep, us)) h = h xor Z_EP[ep and 7]

        // A pawn can only reach the en-passant square by capturing onto it.
        val capSq = if (type == PAWN_T && to == ep) (if (us == WHITE_I) to - 8 else to + 8) else to
        val captured = board[capSq]
        uCaptured[sp - 1] = captured
        if (captured != 0) {
            board[capSq] = 0
            counts[captured]--
            h = h xor Z_PIECE[(captured shl 6) or capSq]
        }
        board[from] = 0
        h = h xor Z_PIECE[(piece shl 6) or from]
        val placed = if (promo != 0) {
            counts[piece]--
            ((us shl 3) or (promo + 1)).also { counts[it]++ }
        } else piece
        board[to] = placed
        h = h xor Z_PIECE[(placed shl 6) or to]

        if (type == KING_T) {
            kingSq[us] = to
            if (to - from == 2) h = h xor moveRook(from + 3, from + 1, us)
            else if (from - to == 2) h = h xor moveRook(from - 4, from - 1, us)
        }
        h = h xor Z_CASTLE[castlingRights]
        castlingRights = castlingRights and CASTLE_MASK[from] and CASTLE_MASK[to]
        h = h xor Z_CASTLE[castlingRights]

        enPassantSquare = -1
        if (type == PAWN_T && (to - from == 16 || from - to == 16)) {
            val e = (from + to) shr 1
            enPassantSquare = e
            if (epCapturable(e, them)) h = h xor Z_EP[e and 7]
        }
        halfmoveClock = if (type == PAWN_T || captured != 0) 0 else halfmoveClock + 1
        if (us == BLACK_I) fullmoveNumber++
        stm = them
        hash = h xor Z_SIDE
    }

    /** Moves a rook and returns the Zobrist delta. */
    private fun moveRook(rf: Int, rt: Int, side: Int): Long {
        val rook = (side shl 3) or ROOK_T
        board[rf] = 0
        board[rt] = rook
        return Z_PIECE[(rook shl 6) or rf] xor Z_PIECE[(rook shl 6) or rt]
    }

    // The ep square only enters the hash when a pawn could actually capture there, so repetitions compare correctly.
    private fun epCapturable(ep: Int, side: Int): Boolean {
        val pawn = (side shl 3) or PAWN_T
        for (p in PAWN_ATTACKS[((side xor 1) shl 6) or ep]) if (board[p] == pawn) return true
        return false
    }

    private fun attacked(sq: Int, by: Int): Boolean {
        val b = board
        val s = by shl 3
        val pawn = s or PAWN_T
        for (p in PAWN_ATTACKS[((by xor 1) shl 6) or sq]) if (b[p] == pawn) return true
        val knight = s or KNIGHT_T
        for (p in KNIGHT_TARGETS[sq]) if (b[p] == knight) return true
        val king = s or KING_T
        for (p in KING_TARGETS[sq]) if (b[p] == king) return true
        val queen = s or QUEEN_T
        val rook = s or ROOK_T
        val bishop = s or BISHOP_T
        for (d in 0 until 8) {
            val slider = if (d < 4) rook else bishop
            for (t in RAYS[(d shl 6) or sq]) {
                val x = b[t]
                if (x != 0) {
                    if (x == slider || x == queen) return true
                    break
                }
            }
        }
        return false
    }

    private fun genStep(sq: Int, targets: IntArray, us: Int, out: IntArray, start: Int, tactical: Boolean): Int {
        var n = start
        for (t in targets) {
            val q = board[t]
            if (if (q == 0) !tactical else (q shr 3) != us) out[n++] = sq or (t shl 6)
        }
        return n
    }

    private fun genSlide(sq: Int, d0: Int, d1: Int, us: Int, out: IntArray, start: Int, tactical: Boolean): Int {
        var n = start
        for (d in d0 until d1) {
            for (t in RAYS[(d shl 6) or sq]) {
                val q = board[t]
                if (q == 0) {
                    if (!tactical) out[n++] = sq or (t shl 6)
                } else {
                    if ((q shr 3) != us) out[n++] = sq or (t shl 6)
                    break
                }
            }
        }
        return n
    }

    private fun genPawn(sq: Int, us: Int, out: IntArray, start: Int, tactical: Boolean): Int {
        var n = start
        val white = us == WHITE_I
        val one = if (white) sq + 8 else sq - 8
        val promoting = (one shr 3) == (if (white) 7 else 0)
        if (board[one] == 0) {
            if (promoting) {
                n = addPromos(sq, one, out, n, tactical)
            } else if (!tactical) {
                out[n++] = sq or (one shl 6)
                val two = if (white) one + 8 else one - 8
                if ((sq shr 3) == (if (white) 1 else 6) && board[two] == 0) out[n++] = sq or (two shl 6)
            }
        }
        for (t in PAWN_ATTACKS[(us shl 6) or sq]) {
            val q = board[t]
            if (q != 0 && (q shr 3) != us) {
                if (promoting) n = addPromos(sq, t, out, n, tactical) else out[n++] = sq or (t shl 6)
            } else if (t == enPassantSquare) {
                out[n++] = sq or (t shl 6)
            }
        }
        return n
    }

    private fun addPromos(from: Int, to: Int, out: IntArray, start: Int, queenOnly: Boolean): Int {
        var n = start
        val base = from or (to shl 6)
        out[n++] = base or (PieceType.QUEEN.ordinal shl 12)
        if (!queenOnly) {
            out[n++] = base or (PieceType.KNIGHT.ordinal shl 12)
            out[n++] = base or (PieceType.ROOK.ordinal shl 12)
            out[n++] = base or (PieceType.BISHOP.ordinal shl 12)
        }
        return n
    }

    private fun genCastles(us: Int, out: IntArray, start: Int): Int {
        var n = start
        val k = if (us == WHITE_I) 4 else 60
        if (kingSq[us] != k) return n
        val them = us xor 1
        val kingSide = if (us == WHITE_I) 1 else 4
        val queenSide = kingSide shl 1
        if (castlingRights and kingSide != 0 && board[k + 1] == 0 && board[k + 2] == 0 &&
            !attacked(k, them) && !attacked(k + 1, them) && !attacked(k + 2, them)
        ) out[n++] = k or ((k + 2) shl 6)
        if (castlingRights and queenSide != 0 && board[k - 1] == 0 && board[k - 2] == 0 && board[k - 3] == 0 &&
            !attacked(k, them) && !attacked(k - 1, them) && !attacked(k - 2, them)
        ) out[n++] = k or ((k - 2) shl 6)
        return n
    }

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun start(): Position = fromFen(START_FEN)

        /** Throws IllegalArgumentException on malformed or impossible FEN; drops castling rights the placement contradicts. */
        fun fromFen(fen: String): Position {
            val parts = fen.trim().split(Regex("\\s+"))
            require(parts.size in 4..6) { "FEN needs 4-6 fields: '$fen'" }
            val p = Position()
            val rows = parts[0].split('/')
            require(rows.size == 8) { "FEN needs 8 ranks: '$fen'" }
            for ((i, row) in rows.withIndex()) {
                val rank = 7 - i
                var file = 0
                for (c in row) {
                    if (c in '1'..'8') {
                        file += c - '0'
                    } else {
                        val t = PIECE_CHARS.indexOf(c.uppercaseChar())
                        require(t >= 1 && file < 8) { "Bad rank '$row' in '$fen'" }
                        val code = (if (c.isLowerCase()) BLACK_I shl 3 else 0) or t
                        p.board[rank * 8 + file] = code
                        p.counts[code]++
                        if (t == KING_T) p.kingSq[code shr 3] = rank * 8 + file
                        require(t != PAWN_T || rank in 1..6) { "Pawn on rank ${rank + 1} in '$fen'" }
                        file++
                    }
                    require(file <= 8) { "Bad rank '$row' in '$fen'" }
                }
                require(file == 8) { "Bad rank '$row' in '$fen'" }
            }
            require(p.counts[KING_T] == 1 && p.counts[8 or KING_T] == 1) { "Each side needs exactly one king: '$fen'" }
            p.stm = when (parts[1]) {
                "w" -> WHITE_I
                "b" -> BLACK_I
                else -> throw IllegalArgumentException("Bad side to move in '$fen'")
            }
            require(!p.attacked(p.kingSq[p.stm xor 1], p.stm)) { "Side not to move is in check: '$fen'" }

            var rights = 0
            if (parts[2] != "-") {
                for (c in parts[2]) {
                    val bit = "KQkq".indexOf(c)
                    require(bit >= 0) { "Bad castling field in '$fen'" }
                    rights = rights or (1 shl bit)
                }
            }
            val b = p.board
            val wr = ROOK_T
            val br = (BLACK_I shl 3) or ROOK_T
            if (b[4] != KING_T) rights = rights and 3.inv()
            if (b[60] != ((BLACK_I shl 3) or KING_T)) rights = rights and 12.inv()
            if (b[7] != wr) rights = rights and 1.inv()
            if (b[0] != wr) rights = rights and 2.inv()
            if (b[63] != br) rights = rights and 4.inv()
            if (b[56] != br) rights = rights and 8.inv()
            p.castlingRights = rights

            if (parts[3] != "-") {
                val e = Squares.parse(parts[3])
                val white = p.stm == WHITE_I
                require(e >= 0 && (e shr 3) == (if (white) 5 else 2)) { "Bad en passant square in '$fen'" }
                val pawnSq = if (white) e - 8 else e + 8
                val origin = if (white) e + 8 else e - 8
                require(b[pawnSq] == (((p.stm xor 1) shl 3) or PAWN_T) && b[e] == 0 && b[origin] == 0) {
                    "Impossible en passant square in '$fen'"
                }
                p.enPassantSquare = e
            }
            p.halfmoveClock = parts.getOrNull(4)?.let {
                requireNotNull(it.toIntOrNull()?.takeIf { v -> v >= 0 }) { "Bad halfmove clock in '$fen'" }
            } ?: 0
            p.fullmoveNumber = parts.getOrNull(5)?.let {
                requireNotNull(it.toIntOrNull()) { "Bad fullmove number in '$fen'" }.coerceAtLeast(1)
            } ?: 1
            p.hash = p.computeHash()
            return p
        }
    }
}
