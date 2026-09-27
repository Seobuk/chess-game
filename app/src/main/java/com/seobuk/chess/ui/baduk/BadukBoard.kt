package com.seobuk.chess.ui.baduk

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.Points
import com.seobuk.chess.baduk.core.Stone
import com.seobuk.chess.ui.board.PieceArt
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.Tones
import com.seobuk.chess.ui.theme.floatShadow
import kotlin.math.abs
import kotlin.math.roundToInt

/** A mark on one point. On a stone it is drawn in the stone's contrast colour, on an empty point in the board ink. */
@Immutable
sealed interface BoardMark {
    data object Triangle : BoardMark
    data object Square : BoardMark
    data object Circle : BoardMark

    /** A wrong answer, in the negative tone. */
    data object Cross : BoardMark

    /** A letter or a move number. */
    data class Label(val text: String) : BoardMark
}

// Stones are objects, not theme surfaces: the same in every theme and mode. The edges are >= 3:1 on both wood tones.
internal val BlackStone = Color(0xFF26262B)
internal val BlackStoneEdge = Color(0xFF0A0A0C)
internal val WhiteStone = Color(0xFFF8F5EE)
internal val WhiteStoneEdge = Color(0xFF6E5F49)

private const val PLACE_MS = 120
private const val FADE_MS = 180

/** Lines of a [lines] x [lines] board drawn in a square of [side] px; [band] px of margin hold the coordinates. */
internal class BoardGeometry(val lines: Int, side: Float, val band: Float, val coordinates: Boolean) {
    val cell = (side - 2 * band) / lines
    val first = band + cell / 2
    val last = first + (lines - 1) * cell

    fun x(p: Int) = first + p % lines * cell
    fun y(p: Int) = first + p / lines * cell
    fun center(p: Int) = Offset(x(p), y(p))

    /** The nearest intersection; a tap in the margin snaps to the edge line. */
    fun pointAt(x: Float, y: Float): Int {
        val col = ((x - first) / cell).roundToInt().coerceIn(0, lines - 1)
        val row = ((y - first) / cell).roundToInt().coerceIn(0, lines - 1)
        return row * lines + col
    }
}

/** Coordinates need cells of 15dp or more: on mini boards they would be specks. */
internal fun Density.boardGeometry(lines: Int, side: Float, coordinates: Boolean): BoardGeometry {
    val shown = coordinates && side / lines >= 15.dp.toPx()
    val band = if (shown) (side * 0.05f).coerceIn(13.dp.toPx(), 20.dp.toPx()) else side * 0.02f
    return BoardGeometry(lines, side, band, shown)
}

/** 화점: 9 on 19 lines, the four corner points and the centre on smaller odd boards from 9 lines, none below. */
internal fun starPoints(lines: Int): IntArray {
    if (lines < 9 || lines % 2 == 0) return IntArray(0)
    val edge = if (lines >= 13) 3 else 2
    val mid = lines / 2
    val at = intArrayOf(edge, mid, lines - 1 - edge)
    return buildList { for (y in at) for (x in at) if (lines >= 19 || (x == mid) == (y == mid)) add(y * lines + x) }.toIntArray()
}

/** "D4 빈 점", "D4 흑돌, 활로 3개", with ", 표시 a" for a labelled point. */
internal fun pointLabel(board: Board, p: Int, mark: BoardMark? = null): String {
    val what = when (board[p]) {
        null -> "빈 점"
        Stone.BLACK -> "흑돌, 활로 ${board.liberties(p)}개"
        Stone.WHITE -> "백돌, 활로 ${board.liberties(p)}개"
    }
    return "${Points.name(p, board.size)} $what" + if (mark is BoardMark.Label) ", 표시 ${mark.text}" else ""
}

/**
 * Stateless Go board on the flat wood tone of the current mode. Pass snapshots (board.copy()), never a live
 * board: a change is noticed through [Board.hash]. New stones scale in, removed stones fade out (instant with
 * reduced motion); give a different position its own `key(..)` to switch without animating.
 *
 * [territory] (exact, small squares) and [ownership] (+1 surely black .. -1 surely white, translucent squares
 * that grow with certainty) are drawn on empty points and on stones of the other colour. [dead] stones are
 * dimmed and crossed, unless one of the two overlays already says whose point it is. [hint] rings an empty
 * point in the theme accent; [ghost] is the stone waiting for 착수 in tap-then-confirm mode.
 * With [onPointTap] every intersection is a TalkBack node and a tap snaps to the nearest one.
 */
@Composable
fun BadukBoard(
    board: Board,
    modifier: Modifier = Modifier,
    lastMove: Int? = null,
    marks: Map<Int, BoardMark> = emptyMap(),
    ownership: FloatArray? = null,
    territory: Array<Stone?>? = null,
    dead: Set<Int> = emptySet(),
    hint: Int? = null,
    ghost: Pair<Int, Stone>? = null,
    coordinates: Boolean = true,
    onPointTap: ((Int) -> Unit)? = null,
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val lines = board.size
    val n = lines * lines
    val key = board.hash + lines
    val stones = remember(key) { ByteArray(n) { board.color(it).toByte() } }
    val shown = remember { arrayOfNulls<ByteArray>(1) }
    // The stones before this change, or null when nothing is to be animated.
    val old = remember(key) { shown[0].also { shown[0] = stones }?.takeIf { it.size == n && !it.contentEquals(stones) } }
    val progress = remember(key) { Animatable(if (old == null || reduced) 1f else 0f) }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(FADE_MS, easing = LinearEasing)) }

    val tap by rememberUpdatedState(onPointTap)
    val shape = RoundedCornerShape(12.dp)
    val danger = Tones.negative
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .floatShadow(shape, c)
            .clip(shape)
            .background(c.goBoard)
            .then(if (onPointTap == null) Modifier.semantics { contentDescription = "바둑판" } else Modifier)
            .pointerInput(lines, coordinates, onPointTap != null) {
                if (onPointTap == null) return@pointerInput
                detectTapGestures { o -> tap?.invoke(boardGeometry(lines, size.width.toFloat(), coordinates).pointAt(o.x, o.y)) }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val g = boardGeometry(lines, size.minDimension, coordinates)
            val cell = g.cell
            val r = cell * 0.47f
            drawGrid(g, c.goLine, c.goStar)
            if (g.coordinates) drawCoordinates(g, c.goStar)

            val t = progress.value
            val placing = (t * FADE_MS / PLACE_MS).coerceAtMost(1f)
            for (p in 0 until n) {
                val now = stones[p].toInt()
                val before = old?.get(p)?.toInt() ?: now
                if (before != now && before != Board.EMPTY && t < 1f) drawStone(before, g.center(p), r, alpha = 1f - t)
                if (now == Board.EMPTY) continue
                val alpha = if (p in dead) 0.4f else 1f
                if (before != now) drawStone(now, g.center(p), r * (0.7f + 0.3f * placing), alpha * placing)
                else drawStone(now, g.center(p), r, alpha)
            }

            val squares = BooleanArray(n)
            for (p in 0 until n) {
                val owner = territory?.get(p)
                val v = ownership?.get(p) ?: 0f
                when {
                    owner != null -> drawOwner(owner.ordinal + 1, g.center(p), cell * 0.3f, 1f)
                    abs(v) >= 0.2f -> {
                        val code = if (v > 0) Board.BLACK_CODE else Board.WHITE_CODE
                        if (stones[p].toInt() == code) continue
                        drawOwner(code, g.center(p), cell * (0.14f + 0.2f * abs(v)), 0.35f + 0.5f * abs(v))
                    }
                    else -> continue
                }
                squares[p] = true
            }

            for (p in dead) {
                if (p !in 0 until n || stones[p].toInt() == Board.EMPTY || squares[p]) continue
                drawMark(BoardMark.Cross, g.center(p), cell, contrast(stones[p].toInt()))
            }
            for ((p, mark) in marks) {
                if (p !in 0 until n) continue
                val code = stones[p].toInt()
                if (code == Board.EMPTY) drawCircle(c.goBoard, cell * 0.36f, g.center(p)) // lines never cross a mark
                val ink = when {
                    mark == BoardMark.Cross -> if (code == Board.BLACK_CODE) danger.onDark else danger.solid
                    code == Board.EMPTY -> c.goStar
                    else -> contrast(code)
                }
                drawMark(mark, g.center(p), cell, ink)
            }
            lastMove?.takeIf { it in 0 until n && stones[it].toInt() != Board.EMPTY && it !in marks }?.let { p ->
                drawCircle(contrast(stones[p].toInt()), r * 0.5f, g.center(p), style = Stroke(maxOf(cell * 0.06f, 1.5.dp.toPx())))
            }
            hint?.takeIf { it in 0 until n }?.let { p ->
                drawCircle(c.goBoard, r * 0.86f, g.center(p))
                drawCircle(c.boardArrow.copy(alpha = 0.22f), r * 0.86f, g.center(p))
                drawCircle(c.boardArrow, r * 0.86f, g.center(p), style = Stroke(maxOf(cell * 0.07f, 2.dp.toPx())))
            }
            ghost?.takeIf { it.first in 0 until n }?.let { (p, stone) -> drawStone(stone.ordinal + 1, g.center(p), r, 0.55f) }
        }

        // TalkBack can't use the tap-gesture canvas: one labelled, clickable node per intersection, whose bounds
        // stay exactly on its point (no 48dp minimum), like the chess board's squares.
        if (onPointTap != null) {
            val exact = LocalViewConfiguration.current.let { base -> remember(base) { ExactBounds(base) } }
            val band = with(LocalDensity.current) { boardGeometry(lines, constraints.maxWidth.toFloat(), coordinates).band.toDp() }
            CompositionLocalProvider(LocalViewConfiguration provides exact) {
                Column(Modifier.fillMaxSize().padding(band)) {
                    for (y in 0 until lines) Row(Modifier.weight(1f)) {
                        for (x in 0 until lines) {
                            val p = y * lines + x
                            Box(
                                Modifier.weight(1f).fillMaxHeight().semantics {
                                    contentDescription = pointLabel(board, p, marks[p])
                                    role = Role.Button
                                    onClick { tap?.invoke(p); true }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private class ExactBounds(base: ViewConfiguration) : ViewConfiguration by base {
    override val minimumTouchTargetSize = DpSize.Zero
}

private fun contrast(code: Int) = if (code == Board.BLACK_CODE) WhiteStone else BlackStone

private fun DrawScope.drawGrid(g: BoardGeometry, line: Color, star: Color) {
    val thin = 1.dp.toPx()
    for (i in 0 until g.lines) {
        val at = g.first + i * g.cell
        val w = if (i == 0 || i == g.lines - 1) thin * 1.6f else thin // the edge lines frame the grid
        drawLine(line, Offset(g.first, at), Offset(g.last, at), w, StrokeCap.Square)
        drawLine(line, Offset(at, g.first), Offset(at, g.last), w, StrokeCap.Square)
    }
    val dot = maxOf(g.cell * 0.1f, 2.dp.toPx())
    for (p in starPoints(g.lines)) drawCircle(star, dot, g.center(p))
}

/** Numbers down the left margin (1 at the bottom), letters along the bottom one (no I). */
private fun DrawScope.drawCoordinates(g: BoardGeometry, ink: Color) {
    val ts = g.band * 0.62f
    val color = ink.copy(alpha = 0.8f).toArgb()
    drawIntoCanvas { c ->
        for (i in 0 until g.lines) {
            val at = g.first + i * g.cell
            val name = Points.name(i * g.lines + i, g.lines) // "C7": column letter, row number
            PieceArt.drawLabel(c.nativeCanvas, name.drop(1), g.band * 0.5f, at, ts, color)
            PieceArt.drawLabel(c.nativeCanvas, name.take(1), at, size.height - g.band * 0.5f, ts, color)
        }
    }
}

/** Flat fill with a 1dp edge. */
private fun DrawScope.drawStone(code: Int, center: Offset, radius: Float, alpha: Float = 1f) {
    if (alpha <= 0.01f) return
    val black = code == Board.BLACK_CODE
    val edge = 1.dp.toPx()
    drawCircle(if (black) BlackStone else WhiteStone, radius, center, alpha)
    drawCircle(if (black) BlackStoneEdge else WhiteStoneEdge, radius - edge / 2, center, alpha, style = Stroke(edge))
}

/** Whose point this is: a small square in the owner's stone colours. */
private fun DrawScope.drawOwner(code: Int, center: Offset, side: Float, alpha: Float) {
    val black = code == Board.BLACK_CODE
    val topLeft = center - Offset(side / 2, side / 2)
    drawRect(if (black) BlackStone else WhiteStone, topLeft, Size(side, side), alpha)
    drawRect(if (black) BlackStoneEdge else WhiteStoneEdge, topLeft, Size(side, side), alpha, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawMark(mark: BoardMark, center: Offset, cell: Float, ink: Color) {
    val stroke = Stroke(maxOf(cell * 0.07f, 1.5.dp.toPx()), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (mark) {
        BoardMark.Triangle -> {
            val a = cell * 0.29f
            val path = Path().apply {
                moveTo(center.x, center.y - a)
                lineTo(center.x + a * 0.866f, center.y + a * 0.5f)
                lineTo(center.x - a * 0.866f, center.y + a * 0.5f)
                close()
            }
            drawPath(path, ink, style = stroke)
        }
        BoardMark.Square -> (cell * 0.4f).let { s -> drawRect(ink, center - Offset(s / 2, s / 2), Size(s, s), style = stroke) }
        BoardMark.Circle -> drawCircle(ink, cell * 0.23f, center, style = stroke)
        BoardMark.Cross -> (cell * 0.19f).let { d ->
            drawLine(ink, center - Offset(d, d), center + Offset(d, d), stroke.width, StrokeCap.Round)
            drawLine(ink, center + Offset(-d, d), center + Offset(d, -d), stroke.width, StrokeCap.Round)
        }
        is BoardMark.Label -> drawIntoCanvas {
            val ts = cell * if (mark.text.length > 1) 0.46f else 0.6f
            PieceArt.drawLabel(it.nativeCanvas, mark.text, center.x, center.y, ts, ink.toArgb())
        }
    }
}
