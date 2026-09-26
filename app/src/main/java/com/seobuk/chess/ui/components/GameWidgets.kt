package com.seobuk.chess.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seobuk.chess.ai.MoveQuality
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import com.seobuk.chess.ui.board.PieceArt
import com.seobuk.chess.ui.board.PieceIcon
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.floatShadow
import com.seobuk.chess.ui.theme.tabular
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private val START_COUNT = mapOf(PieceType.PAWN to 8, PieceType.KNIGHT to 2, PieceType.BISHOP to 2, PieceType.ROOK to 2, PieceType.QUEEN to 1)
private val PAWN_UNITS = mapOf(PieceType.PAWN to 1, PieceType.KNIGHT to 3, PieceType.BISHOP to 3, PieceType.ROOK to 5, PieceType.QUEEN to 9, PieceType.KING to 0)

/**
 * Opponent pieces that [side] has captured (derived from what is missing on the board),
 * grouped and overlapped, plus "+N" when [side] is ahead in material.
 */
@Composable
fun CapturedPieces(position: Position, side: Side, modifier: Modifier = Modifier, iconSize: Dp = 20.dp) {
    val opp = side.opposite
    val groups = START_COUNT.map { (t, n) -> t to (n - position.pieceCount(t, opp)).coerceAtLeast(0) }.filter { it.second > 0 }
    val diff = PAWN_UNITS.entries.sumOf { (t, v) -> v * (position.pieceCount(t, side) - position.pieceCount(t, opp)) }
    Row(modifier.height(iconSize), verticalAlignment = Alignment.CenterVertically) {
        if (groups.isNotEmpty()) {
            val overlap = 0.42f
            val widthUnits = groups.sumOf { (_, n) -> (1f + (n - 1) * overlap).toDouble() }.toFloat() + (groups.size - 1) * 0.2f
            Canvas(Modifier.size(iconSize * widthUnits, iconSize)) {
                val s = size.height
                var x = 0f
                drawIntoCanvas { c ->
                    for ((t, n) in groups) {
                        repeat(n) { i -> PieceArt.draw(c.nativeCanvas, Piece(t, opp), x + s / 2 + i * s * overlap, s / 2, s * 1.1f) }
                        x += s * (1f + (n - 1) * overlap) + s * 0.2f
                    }
                }
            }
        }
        if (diff > 0) {
            Text(
                "+$diff",
                Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = LocalAppColors.current.muted,
            )
        }
    }
}

/** SAN with the piece letter replaced by a filled figurine glyph: "Nf3" -> "♞f3", "e8=Q+" -> "e8=♛+". */
fun figurine(san: String): String {
    fun g(c: Char): String? = when (c) {
        'K' -> PieceArt.glyph(PieceType.KING)
        'Q' -> PieceArt.glyph(PieceType.QUEEN)
        'R' -> PieceArt.glyph(PieceType.ROOK)
        'B' -> PieceArt.glyph(PieceType.BISHOP)
        'N' -> PieceArt.glyph(PieceType.KNIGHT)
        else -> null
    }
    if (san.startsWith("O-O")) return san
    val sb = StringBuilder()
    san.forEachIndexed { i, c -> sb.append(if (i == 0 || san.getOrNull(i - 1) == '=') g(c) ?: c else c) }
    return sb.toString()
}

/**
 * Horizontal move strip: "1." e4 e5 "2." ... as chips. The chip for ply [currentPly]-1 is highlighted
 * (soft fill, ink text) and scrolled into view. [qualities] is keyed by ply index (0 = white's first move).
 */
@Composable
fun MoveList(
    sanMoves: List<String>,
    modifier: Modifier = Modifier,
    currentPly: Int = sanMoves.size,
    qualities: Map<Int, MoveQuality> = emptyMap(),
    onPlyClick: ((ply: Int) -> Unit)? = null,
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val state = rememberLazyListState()
    // items: -1 - n = move-number label for move n, otherwise the ply index
    val items = remember(sanMoves) { buildList { sanMoves.indices.forEach { if (it % 2 == 0) add(-1 - it / 2); add(it) } } }
    LaunchedEffect(currentPly, items.size) {
        val idx = items.indexOf(currentPly - 1)
        if (idx < 0) return@LaunchedEffect
        val target = (idx - 2).coerceAtLeast(0)
        if (reduced) state.scrollToItem(target) else state.animateScrollToItem(target)
    }
    if (sanMoves.isEmpty()) {
        Text("아직 둔 수가 없어요", modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = c.muted)
        return
    }
    val numberStyle = MaterialTheme.typography.labelMedium.tabular()
    LazyRow(
        modifier.fillMaxWidth(),
        state = state,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(items, key = { _, v -> v }) { _, v ->
            if (v < 0) {
                Text("${-v}.", Modifier.padding(start = 6.dp), style = numberStyle, color = c.muted)
            } else {
                val current = v == currentPly - 1
                val q = qualities[v]?.takeIf { it != MoveQuality.GOOD && it != MoveQuality.BOOK } // only moves worth a mark
                val symbolColor = q?.textColor() ?: c.text
                Text(
                    buildAnnotatedString {
                        append(figurine(sanMoves[v]))
                        if (q != null) withStyle(SpanStyle(color = symbolColor, fontWeight = FontWeight.Bold)) { append(" " + q.symbol) }
                    },
                    Modifier
                        .then(if (onPlyClick != null) Modifier.pressable(CircleShape, pressedScale = 0.94f) { onPlyClick(v + 1) } else Modifier)
                        .background(if (current) c.soft else Color.Transparent, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (current) c.ink else c.text,
                    maxLines = 1,
                )
            }
        }
    }
}

private val PROMOTIONS = listOf(PieceType.QUEEN to "퀸", PieceType.ROOK to "룩", PieceType.BISHOP to "비숍", PieceType.KNIGHT to "나이트")

/** Opaque card with the four promotion choices; the queen, the usual pick, is outlined in primary. */
@Composable
fun PromotionPicker(side: Side, onPick: (PieceType) -> Unit, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    val c = LocalAppColors.current
    val shape = MaterialTheme.shapes.large
    val tile = MaterialTheme.shapes.small
    Column(
        modifier
            .floatShadow(shape, c)
            .background(c.surface, shape)
            .semantics { paneTitle = "승진할 기물 고르기" } // TalkBack announces the picker
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("어떤 기물로 승진할까요?", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PROMOTIONS.forEach { (t, name) ->
                val queen = t == PieceType.QUEEN
                Column(
                    Modifier
                        .width(68.dp)
                        .pressable(tile, pressedScale = 0.94f) { onPick(t) }
                        .background(if (queen) c.soft else c.surface2)
                        .then(if (queen) Modifier.border(1.5.dp, c.primary, tile) else Modifier)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PieceIcon(Piece(t, side), 52.dp)
                    Text(name, style = MaterialTheme.typography.labelMedium, color = if (queen) c.ink else c.muted)
                }
            }
        }
        if (onDismiss != null) TextButton(onDismiss, Modifier.padding(top = 8.dp)) { Text("취소") }
    }
}

/**
 * AI level avatar: the level's piece in a soft circle, from pawn (Lv.1-2) up to king (Lv.10). Decorative.
 * [container] replaces the soft fill where the avatar sits on a soft surface (a selected row).
 */
@Composable
fun LevelAvatar(level: Int, modifier: Modifier = Modifier, size: Dp = 48.dp, container: Color = LocalAppColors.current.soft) {
    val type = when (level) {
        in 1..2 -> PieceType.PAWN
        in 3..4 -> PieceType.KNIGHT
        in 5..6 -> PieceType.BISHOP
        in 7..8 -> PieceType.ROOK
        9 -> PieceType.QUEEN
        else -> PieceType.KING
    }
    Box(modifier.size(size).background(container, CircleShape), contentAlignment = Alignment.Center) {
        PieceIcon(Piece(type, Side.WHITE), size * 0.72f)
    }
}

private class ConfettiBit(rnd: Random, colors: Int) {
    val fromLeft = rnd.nextBoolean()
    val angle = Math.toRadians(if (fromLeft) -50.0 - rnd.nextDouble(35.0) else -130.0 + rnd.nextDouble(35.0))
    val speed = 0.9f + rnd.nextFloat() * 1.1f       // screen heights / s
    val w = 6f + rnd.nextFloat() * 6f               // dp
    val h = 4f + rnd.nextFloat() * 3f
    val spin = (rnd.nextFloat() - 0.5f) * 720f
    val flip = 4f + rnd.nextFloat() * 8f
    val sway = rnd.nextFloat() * 6.28f
    val delay = rnd.nextFloat() * 0.25f
    val round = rnd.nextFloat() < 0.25f
    val color = rnd.nextInt(colors)
}

private const val CONFETTI_SECONDS = 3f

/**
 * Win celebration: two short confetti bursts from the lower corners in the theme primary and neutrals.
 * Plays each time [play] turns true; skipped with reduced motion; ignores touches.
 */
@Composable
fun Confetti(play: Boolean, modifier: Modifier = Modifier) {
    if (LocalReducedMotion.current) return
    val c = LocalAppColors.current
    val colors = remember(c) { listOf(c.primary, c.primary, c.ink, c.soft, c.muted, c.surface2) }
    var time by remember { mutableFloatStateOf(-1f) }
    val bits = remember(play) { val r = Random(System.nanoTime()); List(110) { ConfettiBit(r, colors.size) } }
    LaunchedEffect(play) {
        time = -1f
        if (!play) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val t = (withFrameNanos { it } - start) / 1e9f
            if (t > CONFETTI_SECONDS) break
            time = t
        }
        time = -1f
    }
    Canvas(modifier.fillMaxSize()) {
        val t0 = time
        if (t0 < 0f) return@Canvas
        val hgt = size.height
        val k = 1.6f           // air drag
        val g = 0.6f * hgt     // gravity px/s^2 (terminal speed g/k)
        val alpha = ((CONFETTI_SECONDS - t0) / 0.8f).coerceIn(0f, 1f)
        for (b in bits) {
            val t = t0 - b.delay
            if (t <= 0f) continue
            val decay = (1 - exp(-k * t)) / k
            val vx = cos(b.angle).toFloat() * b.speed * hgt
            val vy = sin(b.angle).toFloat() * b.speed * hgt
            val x0 = if (b.fromLeft) 0f else size.width
            val x = x0 + vx * decay + sin(t * 3f + b.sway) * 18.dp.toPx() * (1 - exp(-t))
            val y = hgt * 0.78f + (vy - g / k) * decay + g * t / k
            if (y > hgt + 40f) continue
            val color = colors[b.color].copy(alpha = alpha)
            val wpx = b.w.dp.toPx() * abs(cos(t * b.flip)).coerceAtLeast(0.15f)
            val hpx = b.h.dp.toPx()
            rotate(b.spin * t, Offset(x, y)) {
                if (b.round) drawCircle(color, radius = hpx * 0.7f, center = Offset(x, y))
                else drawRect(color, Offset(x - wpx / 2, y - hpx / 2), Size(wpx, hpx))
            }
        }
    }
}
