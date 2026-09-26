package com.seobuk.chess.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seobuk.chess.ai.MoveQuality
import com.seobuk.chess.ai.isMateScore
import com.seobuk.chess.ai.mateInMoves
import com.seobuk.chess.ui.board.PieceArt
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.Tone
import com.seobuk.chess.ui.theme.Tones
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

fun MoveQuality.tone(): Tone = when (this) {
    MoveQuality.BEST -> Tones.best
    MoveQuality.EXCELLENT -> Tones.excellent
    MoveQuality.GOOD -> Tones.good
    MoveQuality.BOOK -> Tones.book
    MoveQuality.INACCURACY -> Tones.inaccuracy
    MoveQuality.MISTAKE -> Tones.mistake
    MoveQuality.BLUNDER -> Tones.blunder
}

/** Solid fill for the on-board badge; white text on it is >= 4.5:1. For text use [textColor]. */
fun MoveQuality.color(): Color = tone().solid

/** Quality colour for text on this mode's surfaces. */
@Composable
fun MoveQuality.textColor(): Color = tone().text(LocalAppColors.current.dark)

/**
 * Pill with the quality symbol and the Korean label ("?! 부정확") on the quality's tinted background. The symbol is
 * hidden from TalkBack (the label says it), and left out for BOOK, whose symbol is the label's first syllable.
 */
@Composable
fun QualityChip(quality: MoveQuality, modifier: Modifier = Modifier) {
    val dark = LocalAppColors.current.dark
    val tone = quality.tone()
    val style = MaterialTheme.typography.labelMedium
    Row(
        modifier
            .background(tone.container(dark), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (quality != MoveQuality.BOOK) {
            Text(quality.symbol, Modifier.padding(end = 5.dp).clearAndSetSemantics {}, color = tone.text(dark), style = style.copy(fontWeight = FontWeight.Bold))
        }
        Text(quality.labelKo, color = tone.text(dark), style = style)
    }
}

/** Three dots rising in turn; shown only while someone is thinking. Static with reduced motion. */
@Composable
fun ThinkingDots(modifier: Modifier = Modifier, color: Color = LocalAppColors.current.muted, dotSize: Dp = 8.dp) {
    val t = if (LocalReducedMotion.current) 0.5f else {
        val v by rememberInfiniteTransition(label = "dots").animateFloat(0f, 1f, infiniteRepeatable(tween(1100)), label = "dotsT")
        v
    }
    Canvas(modifier.size(width = dotSize * 5, height = dotSize * 2.2f)) {
        val r = dotSize.toPx() / 2
        for (i in 0..2) {
            val phase = ((t - i * 0.16f) % 1f + 1f) % 1f
            val bounce = if (phase < 0.4f) kotlin.math.sin(phase / 0.4f * Math.PI).toFloat() else 0f
            drawCircle(
                color.copy(alpha = 0.45f + 0.55f * bounce),
                radius = r,
                center = Offset(r + i * r * 3.4f + r * 0.3f, size.height - r - bounce * r * 1.6f),
            )
        }
    }
}

/**
 * Evaluation bar. [evalCp] is WHITE POV centipawns and may be a mate score.
 * Horizontal: white fills from the left (from the right when [flipped]); vertical: white from the bottom (top when flipped).
 * Neutral: the white side is the light neutral, the black side the dark neutral of the theme.
 */
@Composable
fun EvalBar(evalCp: Int, modifier: Modifier = Modifier, flipped: Boolean = false, vertical: Boolean = false) {
    val c = LocalAppColors.current
    val mate = if (isMateScore(evalCp)) mateInMoves(evalCp) else null
    val target = when {
        mate != null -> if (evalCp > 0) 1f else 0f
        else -> (1.0 / (1.0 + 10.0.pow(-evalCp / 400.0))).toFloat().coerceIn(0.04f, 0.96f)
    }
    val spec = if (LocalReducedMotion.current) snap() else spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessVeryLow)
    val white by animateFloatAsState(target, spec, label = "eval")
    val label = when {
        mate == 0 -> "#"
        mate != null -> "M${abs(mate)}"
        else -> String.format(Locale.US, "%.1f", abs(evalCp) / 100f)
    }
    // In dark mode the black side is surface2, not the page colour, and the muted outline (>= 3:1 on bg) shows the full length.
    val whiteColor = if (c.dark) c.text else c.surface
    val blackColor = if (c.dark) c.surface2 else c.text
    val outline = c.muted.copy(alpha = 0.75f)
    val whiteAhead = evalCp >= 0
    val who = if (whiteAhead) "백" else "흑"
    val description = when {
        mate == 0 -> "평가: 체크메이트"
        mate != null -> "평가: $who ${abs(mate)}수 안에 메이트"
        else -> "평가: $who +$label"
    }
    Canvas(modifier.then(if (vertical) Modifier.width(18.dp) else Modifier.height(20.dp).fillMaxWidth()).semantics { contentDescription = description }) {
        val len = if (vertical) size.height else size.width
        val thick = if (vertical) size.width else size.height
        val cr = CornerRadius(thick / 2)
        drawRoundRect(blackColor, cornerRadius = cr)
        val wl = len * white
        // white segment starts at the "white" end of the bar
        val whiteAtStart = if (vertical) flipped else !flipped
        val start = if (whiteAtStart) 0f else len - wl
        val (o, s) = if (vertical) Offset(0f, start) to Size(thick, wl) else Offset(start, 0f) to Size(wl, thick)
        drawRoundRect(whiteColor, topLeft = o, size = s, cornerRadius = cr)
        drawRoundRect(outline, cornerRadius = cr, style = Stroke(1.dp.toPx()))

        // label at the leading side's end, in the opposite neutral
        val ts = if (vertical) thick * 0.5f else thick * 0.62f
        val labelColor = if (whiteAhead) blackColor else whiteColor
        val atStart = whiteAhead == whiteAtStart
        val pad = if (vertical) thick * 0.9f else thick * 1.3f
        val pos = if (atStart) pad else len - pad
        drawIntoCanvas {
            if (vertical) PieceArt.drawLabel(it.nativeCanvas, label, thick / 2, pos, ts, labelColor.toArgb())
            else PieceArt.drawLabel(it.nativeCanvas, label, pos, thick / 2, ts, labelColor.toArgb())
        }
    }
}

/**
 * Coach card: a soft circle with [icon], the [title], the [quality] chip and the message. Shows [ThinkingDots]
 * while [thinking]; a new message fades in. Given less height than it needs, the card keeps its frame and only the
 * message scrolls, with a fade at the bottom while more text is below.
 */
@Composable
fun CoachBubble(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "코치",
    icon: ImageVector = Icons.Outlined.School,
    quality: MoveQuality? = null,
    thinking: Boolean = false,
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val scroll = remember(message, thinking) { ScrollState(0) }
    AppCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            IconCircle(icon, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.heightIn(min = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = c.muted, modifier = Modifier.weight(1f), maxLines = 1)
                    if (quality != null && !thinking) QualityChip(quality)
                }
                AnimatedContent(
                    targetState = if (thinking) null else message,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(if (reduced) Modifier else Modifier.animateContentSize())
                        .drawWithContent {
                            drawContent()
                            if (scroll.canScrollForward) {
                                val fade = minOf(20.dp.toPx(), size.height * 0.3f) // a single visible line stays readable
                                drawRect(Brush.verticalGradient(listOf(Color.Transparent, c.surface), size.height - fade, size.height), Offset(0f, size.height - fade))
                            }
                        }
                        .verticalScroll(scroll),
                    transitionSpec = {
                        if (reduced) EnterTransition.None togetherWith ExitTransition.None
                        else fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                    },
                    label = "coachText",
                ) { text ->
                    if (text == null) ThinkingDots(Modifier.padding(vertical = 6.dp))
                    else Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
