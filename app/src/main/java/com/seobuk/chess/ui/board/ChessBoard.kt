package com.seobuk.chess.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import com.seobuk.chess.core.Squares
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.Tones
import com.seobuk.chess.ui.theme.floatShadow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class BoardArrow(val from: Int, val to: Int, val color: Color)

@Immutable
data class SquareBadge(val square: Int, val text: String, val color: Color)

internal class Sprite(val piece: Piece, val from: Int, val to: Int)

/** How to get from [old] to the new position: slide [movers] (one move) or, if movers is empty, crossfade. */
internal class BoardChange(val old: Position?, val movers: List<Sprite>, val captured: Sprite?)

private fun placement(p: Position) = p.fen().substringBefore(' ')

internal fun changeBetween(old: Position?, new: Position, lastMove: Move?): BoardChange {
    if (old == null) return BoardChange(null, emptyList(), null)
    if (lastMove != null && old.sideToMove != new.sideToMove && old.isLegal(lastMove)) {
        val test = old.copy().apply { makeMove(lastMove) }
        if (placement(test) == placement(new)) {
            val piece = old.pieceAt(lastMove.from)!!
            val movers = mutableListOf(Sprite(piece, lastMove.from, lastMove.to))
            val df = Squares.file(lastMove.to) - Squares.file(lastMove.from)
            if (piece.type == PieceType.KING && abs(df) == 2) {
                val rookFrom = if (df > 0) lastMove.from + 3 else lastMove.from - 4
                val rookTo = if (df > 0) lastMove.from + 1 else lastMove.from - 1
                old.pieceAt(rookFrom)?.let { movers += Sprite(it, rookFrom, rookTo) }
            }
            val captured = old.pieceAt(lastMove.to)?.let { Sprite(it, lastMove.to, lastMove.to) }
                ?: if (piece.type == PieceType.PAWN && df != 0) {
                    val ep = Squares.rank(lastMove.from) * 8 + Squares.file(lastMove.to)
                    old.pieceAt(ep)?.let { Sprite(it, ep, ep) }
                } else null
            return BoardChange(old, movers, captured)
        }
    }
    return BoardChange(old, emptyList(), null)
}

/**
 * Stateless board in the colours of the selected app theme. The caller owns selection logic; pass snapshots
 * (position.copy()), never a live object. One-move changes (matching [lastMove]) slide; anything else crossfades.
 * [arrows] usually use [com.seobuk.chess.ui.theme.AppPalette.boardArrow]; [highlights] soft or primary with alpha.
 */
@Composable
fun ChessBoard(
    position: Position,
    modifier: Modifier = Modifier,
    flipped: Boolean = false,
    selectedSquare: Int? = null,
    legalTargets: Set<Int> = emptySet(),
    lastMove: Move? = null,
    checkSquare: Int? = null,
    arrows: List<BoardArrow> = emptyList(),
    highlights: Map<Int, Color> = emptyMap(),
    badge: SquareBadge? = null,
    onSquareClick: ((Int) -> Unit)? = null,
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val start = if (reduced) 1f else 0f
    val key = position.fen()
    val shown = remember { arrayOfNulls<Position>(1) }
    val change = remember(key) {
        changeBetween(shown[0], position, lastMove).also { shown[0] = position.copy() }
    }
    val progress = remember(key) { Animatable(if (change.old == null) 1f else start) }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }

    val arrowAnim = remember(arrows) { Animatable(start) }
    LaunchedEffect(arrowAnim) { arrowAnim.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }

    val dotAnim = remember(selectedSquare, legalTargets) { Animatable(start) }
    LaunchedEffect(dotAnim) { dotAnim.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }

    val badgeAnim = remember(badge) { Animatable(start) }
    LaunchedEffect(badgeAnim) { badgeAnim.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)) }

    // Check is a static tint and ring; the ring pulses once when the check appears.
    val checkPulse = remember(checkSquare) { Animatable(start) }
    LaunchedEffect(checkPulse) { checkPulse.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    val danger = Tones.negative.solid

    val click by rememberUpdatedState(onSquareClick)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier
            .aspectRatio(1f)
            .floatShadow(shape, c)
            .clip(shape)
            .then(if (onSquareClick == null) Modifier.semantics { contentDescription = "체스판" } else Modifier)
            .pointerInput(flipped, onSquareClick != null) {
                if (onSquareClick == null) return@pointerInput
                detectTapGestures { o ->
                    val cell = min(size.width, size.height) / 8f
                    val col = (o.x / cell).toInt().coerceIn(0, 7)
                    val row = (o.y / cell).toInt().coerceIn(0, 7)
                    val file = if (flipped) 7 - col else col
                    val rank = if (flipped) row else 7 - row
                    click?.invoke(rank * 8 + file)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val g = Grid(size.minDimension, flipped)
            val cell = g.cell
            val sqSize = Size(cell, cell)

            drawRect(c.boardDark)
            for (sq in 0..63) if ((Squares.file(sq) + Squares.rank(sq)) % 2 == 1) drawRect(c.boardLight, g.topLeft(sq), sqSize)

            lastMove?.let {
                drawRect(c.boardLastMove, g.topLeft(it.from), sqSize)
                drawRect(c.boardLastMove, g.topLeft(it.to), sqSize)
            }
            highlights.forEach { (sq, color) -> if (sq in 0..63) drawRect(color, g.topLeft(sq), sqSize) }
            selectedSquare?.takeIf { it in 0..63 }?.let {
                drawRect(c.boardSelected, g.topLeft(it), sqSize)
                val w = 2.5.dp.toPx() // inset ring, so the selection also reads without colour
                drawRect(c.boardInk, g.topLeft(it) + Offset(w / 2, w / 2), Size(cell - w, cell - w), style = Stroke(w))
            }

            checkSquare?.takeIf { it in 0..63 }?.let { sq ->
                drawRect(danger.copy(alpha = 0.35f), g.topLeft(sq), sqSize)
                val p = checkPulse.value
                drawCircle(
                    danger.copy(alpha = 0.9f - 0.3f * p),
                    radius = cell * (0.46f - 0.08f * (1f - p)),
                    center = g.center(sq),
                    style = Stroke(cell * 0.07f),
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val g = Grid(size.minDimension, flipped)
            val cell = g.cell
            fun topLeft(sq: Int) = g.topLeft(sq)
            fun center(sq: Int) = g.center(sq)

            // Legal-move dots (rings on captures) on a light halo, so they keep >= 3:1 on dark squares too
            val s = dotAnim.value
            val halo = 1.5.dp.toPx()
            for (t in legalTargets) {
                if (t !in 0..63) continue
                if (position.pieceAt(t) != null) {
                    drawCircle(c.boardLight, radius = cell * 0.455f, center = center(t), style = Stroke((cell * 0.085f + halo * 2) * s))
                    drawCircle(c.boardLegal, radius = cell * 0.455f, center = center(t), style = Stroke(cell * 0.085f * s))
                } else {
                    drawCircle(c.boardLight, radius = (cell * 0.165f + halo) * s, center = center(t))
                    drawCircle(c.boardLegal, radius = cell * 0.165f * s, center = center(t))
                }
            }

            drawPieces(position, change, progress.value, cell, ::center)
            drawCoordinates(cell, flipped, c.boardInk, c.boardLight, c.boardDark)

            val a = arrowAnim.value
            for (arrow in arrows) {
                if (arrow.from !in 0..63 || arrow.to !in 0..63 || arrow.from == arrow.to) continue
                drawArrow(center(arrow.from), center(arrow.to), arrow.color, c.boardLight, cell, a)
            }

            badge?.takeIf { it.square in 0..63 }?.let { b ->
                val r = cell * 0.2f
                val center = topLeft(b.square) + Offset(cell - r - cell * 0.03f, r + cell * 0.03f)
                val sc = badgeAnim.value
                if (sc > 0.01f) {
                    drawCircle(c.boardLight, radius = r * sc, center = center)
                    drawCircle(b.color, radius = r * 0.84f * sc, center = center)
                    drawIntoCanvas { PieceArt.drawLabel(it.nativeCanvas, b.text, center.x, center.y, r * 1.05f * sc, android.graphics.Color.WHITE) }
                }
            }
        }

        // TalkBack can't use the tap-gesture canvas: one labelled, clickable node per square. Without the 48dp
        // minimum touch target, each node's bounds stay exactly on its square even when squares are smaller.
        if (onSquareClick != null) CompositionLocalProvider(LocalViewConfiguration provides ExactBounds(LocalViewConfiguration.current)) { Column(Modifier.fillMaxSize()) {
            for (row in 0..7) Row(Modifier.weight(1f)) {
                for (col in 0..7) {
                    val sq = (if (flipped) row else 7 - row) * 8 + (if (flipped) 7 - col else col)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().semantics {
                            contentDescription = squareLabel(position, sq, sq in legalTargets)
                            selected = sq == selectedSquare
                            role = Role.Button
                            onClick { click?.invoke(sq); true }
                        },
                    )
                }
            }
        } }
    }
}

private class ExactBounds(base: ViewConfiguration) : ViewConfiguration by base {
    override val minimumTouchTargetSize = DpSize.Zero
}

/** Square geometry on a board of side [side] px. */
private class Grid(side: Float, private val flipped: Boolean) {
    val cell = side / 8f
    fun topLeft(sq: Int) = Offset(
        (if (flipped) 7 - Squares.file(sq) else Squares.file(sq)) * cell,
        (if (flipped) Squares.rank(sq) else 7 - Squares.rank(sq)) * cell,
    )
    fun center(sq: Int) = topLeft(sq) + Offset(cell / 2, cell / 2)
}

private val PIECE_KO = arrayOf("폰", "나이트", "비숍", "룩", "퀸", "킹") // PieceType order

/** "e4 백 폰", "d5 빈 칸, 이동 가능". */
internal fun squareLabel(p: Position, sq: Int, target: Boolean): String {
    val piece = p.pieceAt(sq)
    val what = if (piece == null) "빈 칸" else (if (piece.side == Side.WHITE) "백 " else "흑 ") + PIECE_KO[piece.type.ordinal]
    return "${Squares.name(sq)} $what" + if (target) ", 이동 가능" else ""
}

/**
 * Files in the bottom-right corner of the bottom row, ranks in the top-left corner of the left column, drawn over
 * the pieces in [ink] (>= 4.3:1 on both squares) with a halo in their square's colour, so a piece's edge never hides
 * them, 4dp in from the edges so the board's rounded corners never clip them. Skipped on small boards (mini
 * previews), where they would be specks.
 */
private fun DrawScope.drawCoordinates(cell: Float, flipped: Boolean, ink: Color, light: Color, dark: Color) {
    if (cell < 28.dp.toPx()) return
    val ts = maxOf(cell * 0.2f, 10.dp.toPx())
    val off = 4.dp.toPx() + ts * 0.35f // edge gap plus half a glyph
    val color = ink.toArgb()
    fun square(file: Int, rank: Int) = (if ((file + rank) % 2 == 1) light else dark).toArgb()
    drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        for (i in 0..7) {
            val file = if (flipped) 7 - i else i
            val bottomRank = if (flipped) 7 else 0
            PieceArt.drawLabel(nc, ('a' + file).toString(), (i + 1) * cell - off, 8 * cell - off, ts, color, square(file, bottomRank))
            val rank = if (flipped) i else 7 - i
            val leftFile = if (flipped) 7 else 0
            PieceArt.drawLabel(nc, ('1' + rank).toString(), off, i * cell + off, ts, color, square(leftFile, rank))
        }
    }
}

private fun DrawScope.drawPieces(position: Position, change: BoardChange, p: Float, cell: Float, center: (Int) -> Offset) {
    val old = change.old
    val sliding = old != null && change.movers.isNotEmpty() && p < 1f
    val fading = old != null && change.movers.isEmpty() && p < 1f
    drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        if (fading) {
            for (sq in 0..63) {
                val o = old.pieceAt(sq) ?: continue
                if (o != position.pieceAt(sq)) center(sq).let { PieceArt.draw(nc, o, it.x, it.y, cell, alpha = 1f - p, scale = 1f - 0.1f * p) }
            }
        }
        for (sq in 0..63) {
            val piece = position.pieceAt(sq) ?: continue
            if (sliding && change.movers.any { it.to == sq }) continue
            val alpha = if (fading && old.pieceAt(sq) != piece) p else 1f
            val scale = if (fading && old.pieceAt(sq) != piece) 0.9f + 0.1f * p else 1f
            center(sq).let { PieceArt.draw(nc, piece, it.x, it.y, cell, alpha, scale) }
        }
        if (sliding) {
            change.captured?.let { cap ->
                center(cap.to).let { PieceArt.draw(nc, cap.piece, it.x, it.y, cell, alpha = 1f - p, scale = 1f - 0.35f * p) }
            }
            val lift = sin(p * PI).toFloat()
            for (m in change.movers) {
                val a = center(m.from)
                val b = center(m.to)
                PieceArt.draw(nc, m.piece, a.x + (b.x - a.x) * p, a.y + (b.y - a.y) * p, cell, lift = lift)
            }
        }
    }
}

/** Arrow in [color] with a thin [outline] (the light square colour), so it also reads over dark squares. */
private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color, outline: Color, cell: Float, progress: Float) {
    val d = to - from
    val len = d.getDistance()
    if (len < 1f || progress <= 0f) return
    val u = d / len
    val n = Offset(-u.y, u.x)
    val start = from + u * (cell * 0.22f)
    val fullTip = to - u * (cell * 0.08f)
    val total = (fullTip - start).getDistance() * progress
    val tip = start + u * total
    val headLen = min(cell * 0.44f, total)
    val sw = cell * 0.17f / 2
    val hw = cell * 0.48f / 2
    val base = tip - u * headLen
    val path = Path().apply {
        moveTo(start.x + n.x * sw, start.y + n.y * sw)
        lineTo(base.x + n.x * sw, base.y + n.y * sw)
        lineTo(base.x + n.x * hw, base.y + n.y * hw)
        lineTo(tip.x, tip.y)
        lineTo(base.x - n.x * hw, base.y - n.y * hw)
        lineTo(base.x - n.x * sw, base.y - n.y * sw)
        lineTo(start.x - n.x * sw, start.y - n.y * sw)
        close()
    }
    drawPath(path, outline.copy(alpha = 0.7f * color.alpha), style = Stroke(cell * 0.05f, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = color.alpha * 0.9f))
}
