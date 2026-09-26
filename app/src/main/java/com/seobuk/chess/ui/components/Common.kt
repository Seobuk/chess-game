package com.seobuk.chess.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.Tone
import com.seobuk.chess.ui.theme.Tones
import com.seobuk.chess.ui.theme.tabular

/** Press feedback: shrinks to [pressedScale] while [source] is pressed (instantly with reduced motion). */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.98f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val spec = if (LocalReducedMotion.current) snap() else spring<Float>(stiffness = Spring.StiffnessMedium)
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spec, label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Clickable with ripple and [pressScale], clipped to [shape]. For custom surfaces that no M3 component covers. */
fun Modifier.pressable(
    shape: Shape,
    enabled: Boolean = true,
    pressedScale: Float = 0.98f,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    pressScale(source, pressedScale)
        .clip(shape)
        .clickable(source, ripple(), enabled = enabled, role = role, onClick = onClick)
}

/** Card: surface fill with a 1dp line border and no shadow (22dp corners by default). Clickable when [onClick] is set. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable ColumnScope.() -> Unit = { Column(Modifier.padding(contentPadding), content = content) }
    if (onClick == null) {
        OutlinedCard(modifier, shape = shape, content = inner)
    } else {
        val source = remember { MutableInteractionSource() }
        OutlinedCard(onClick, modifier.pressScale(source), shape = shape, interactionSource = source, content = inner)
    }
}

/** The screen's one filled call to action: full pill, 52dp tall. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick,
        modifier.heightIn(min = 52.dp).pressScale(source),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp),
        interactionSource = source,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Icon-only button: a soft circle with a primary-ink icon ([emphasized]: primary circle, on-primary icon).
 * [contentDescription] is required: it is the button's only label for TalkBack.
 */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    emphasized: Boolean = false,
) {
    val c = LocalAppColors.current
    val source = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick,
        modifier.size(size).pressScale(source, 0.94f),
        enabled = enabled,
        shape = CircleShape,
        colors = if (emphasized) IconButtonDefaults.filledIconButtonColors(c.primary, c.on)
        else IconButtonDefaults.filledTonalIconButtonColors(c.soft, c.ink),
        interactionSource = source,
    ) {
        Icon(icon, contentDescription, Modifier.size(if (size >= 56.dp) 26.dp else 22.dp))
    }
}

/** Decorative soft circle holding an icon (list rows, the coach). */
@Composable
fun IconCircle(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val c = LocalAppColors.current
    Box(modifier.size(size).background(c.soft, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = c.ink)
    }
}

/** Screen top bar: back button, title and optional subtitle / trailing actions. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "뒤로", onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalAppColors.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

/** Heading of a group of rows or cards. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(top = 12.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = LocalAppColors.current.muted,
    )
}

/**
 * Small pill label: soft fill with ink text, or a semantic [tone] (win/loss, check).
 * [container] replaces the soft fill where the pill sits on a soft surface.
 */
@Composable
fun PillTag(text: String, modifier: Modifier = Modifier, tone: Tone? = null, container: Color? = null) {
    val c = LocalAppColors.current
    Text(
        text,
        modifier
            .background(container ?: tone?.container(c.dark) ?: c.soft, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = tone?.text(c.dark) ?: c.ink,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}

/** Rating change: "▲ 12" / "▼ 23" in the win/loss tone, "변동 없음" neutral. TalkBack hears words, not triangles. */
@Composable
fun DeltaPill(delta: Int, modifier: Modifier = Modifier) = PillTag(
    when {
        delta > 0 -> "▲ $delta"
        delta < 0 -> "▼ ${-delta}"
        else -> "변동 없음"
    },
    modifier.clearAndSetSemantics {
        contentDescription = when {
            delta > 0 -> "${delta}점 올랐어요"
            delta < 0 -> "${-delta}점 내려갔어요"
            else -> "변동 없음"
        }
    },
    tone = when {
        delta > 0 -> Tones.positive
        delta < 0 -> Tones.negative
        else -> null
    },
)

/** A rating as a hero card's big number: one style on Home and Stats. */
@Composable
fun HeroNumber(value: Int, modifier: Modifier = Modifier) =
    Text("$value", modifier, style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, lineHeight = 52.sp).tabular())

/** Top and side insets, outside a screen's scroller; the bottom one goes inside it ([bottomInset]). */
fun Modifier.screenInsets(): Modifier = composed { windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)) }

/** Bottom inset, as padding inside a scroller: content scrolls behind the gesture bar and ends above it. */
@Composable
fun bottomInset(): Dp = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()

/**
 * Tappable row of a grouped list (several rows inside one [AppCard] with 6dp padding, no dividers): optional
 * [leading] (an [IconCircle] or avatar), title and muted subtitle, optional [trailing] and a chevron.
 */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressable(MaterialTheme.shapes.small, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(22.dp), tint = c.muted)
    }
}
