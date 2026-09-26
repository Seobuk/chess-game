package com.seobuk.chess.ui.board

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Side

/**
 * Draws pieces from the filled Unicode chess glyphs (both colours), with an outline, a vertical
 * gradient fill and a soft drop shadow. Uses android.graphics directly for exact ink-bounds centring.
 * Main-thread only (shared Paint objects).
 */
object PieceArt {
    private const val REF = 100f

    fun glyph(type: PieceType): String = when (type) {
        PieceType.KING -> "♚"
        PieceType.QUEEN -> "♛"
        PieceType.ROOK -> "♜"
        PieceType.BISHOP -> "♝"
        PieceType.KNIGHT -> "♞"
        PieceType.PAWN -> "♟"
    } + "︎" // text presentation: keeps ♟ from turning into an emoji

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL_AND_STROKE; strokeJoin = Paint.Join.ROUND }
    private val labelBounds = Rect()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT; strokeJoin = Paint.Join.ROUND }

    // Ink bounds of each glyph at text size REF.
    private val bounds: Map<PieceType, Rect> by lazy {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = REF }
        PieceType.entries.associateWith { t -> Rect().also { val g = glyph(t); p.getTextBounds(g, 0, g.length, it) } }
    }

    private class SizeCache(val textSize: Float, val white: Shader, val black: Shader, val blur: BlurMaskFilter)
    private val caches = HashMap<Int, SizeCache>()

    private fun cacheFor(cell: Float): SizeCache {
        val key = cell.toInt().coerceAtLeast(1)
        return caches.getOrPut(key) {
            val king = bounds.getValue(PieceType.KING)
            val textSize = key * 0.78f * REF / king.height()
            val h = key * 0.4f
            SizeCache(
                textSize,
                LinearGradient(0f, -h, 0f, h, intArrayOf(0xFFFFFDF4.toInt(), 0xFFF6EAD0.toInt(), 0xFFDCC7A0.toInt()), null, Shader.TileMode.CLAMP),
                LinearGradient(0f, -h, 0f, h, intArrayOf(0xFF4A4A57.toInt(), 0xFF2A2A33.toInt(), 0xFF14141A.toInt()), null, Shader.TileMode.CLAMP),
                BlurMaskFilter(key * 0.035f + 0.5f, BlurMaskFilter.Blur.NORMAL),
            )
        }
    }

    /** Draws [piece] centred in a square cell of side [cell] px at ([cx],[cy]). */
    fun draw(canvas: Canvas, piece: Piece, cx: Float, cy: Float, cell: Float, alpha: Float = 1f, scale: Float = 1f, lift: Float = 0f) {
        if (alpha <= 0.01f || cell < 2f) return
        val c = cacheFor(cell)
        val k = c.textSize / REF
        val b = bounds.getValue(piece.type)
        val king = bounds.getValue(PieceType.KING)
        // Centre horizontally on the glyph's ink; share the king's baseline so all pieces stand on one floor.
        val x = -(b.left + b.width() / 2f) * k
        val y = -(king.top + king.height() / 2f) * k
        val g = glyph(piece.type)
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val white = piece.side == Side.WHITE

        canvas.save()
        canvas.translate(cx, cy)
        if (scale != 1f || lift != 0f) canvas.scale(scale * (1f + 0.08f * lift), scale * (1f + 0.08f * lift))

        // Hardware canvases ignore mask filters before API 28: the "soft" shadow would be a hard dark ghost glyph.
        if (Build.VERSION.SDK_INT >= 28) {
            shadow.textSize = c.textSize
            shadow.strokeWidth = cell * 0.05f
            shadow.maskFilter = c.blur
            shadow.color = android.graphics.Color.argb((a * (0.42f + 0.2f * lift)).toInt(), 0, 0, 0)
            canvas.drawText(g, x + cell * (0.015f + 0.03f * lift), y + cell * (0.04f + 0.06f * lift), shadow)
        }

        outline.textSize = c.textSize
        outline.strokeWidth = if (white) cell * 0.06f else cell * 0.055f
        outline.color = if (white) 0xFF2B2119.toInt() else 0xFFCFC6B6.toInt()
        outline.alpha = if (white) a else (a * 0.8f).toInt()
        canvas.drawText(g, x, y, outline)

        fill.textSize = c.textSize
        fill.shader = if (white) c.white else c.black
        fill.alpha = a
        canvas.drawText(g, x, y, fill)
        canvas.restore()
    }

    /** Bold label centred on ([cx],[cy]), used for coordinates and badges; [halo] outlines it (readable over pieces). */
    fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float, textSize: Float, color: Int, halo: Int? = null) {
        textPaint.textSize = textSize
        val r = labelBounds
        textPaint.getTextBounds(text, 0, text.length, r)
        val x = cx - r.left - r.width() / 2f
        val y = cy - r.top - r.height() / 2f
        if (halo != null) {
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = textSize * 0.3f
            textPaint.color = halo
            canvas.drawText(text, x, y, textPaint)
            textPaint.style = Paint.Style.FILL
        }
        textPaint.color = color
        canvas.drawText(text, x, y, textPaint)
    }
}

/** A single piece drawn exactly like on the board. */
@Composable
fun PieceIcon(piece: Piece, size: Dp, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Canvas(modifier.size(size)) {
        drawIntoCanvas { PieceArt.draw(it.nativeCanvas, piece, center.x, center.y, this.size.minDimension, alpha) }
    }
}
