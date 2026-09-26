package com.seobuk.chess.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * One mode (light or dark) of an [AppTheme]. [primary] is the only accent; [ink] is primary-coloured text that
 * reads on [soft] and [surface] (>= 4.5:1); [on] is text on [primary]. Neutrals are tinted toward the theme hue.
 * The board tokens are shared by both modes of a theme, so the board looks the same by day and night;
 * [boardArrow] is the theme primary deep enough to read on the squares (use it for arrows on the board);
 * [boardInk] is the dark neutral drawn on the squares (coordinates, legal-move dots, the selection ring), >= 4.3:1 on both.
 */
@Immutable
data class AppPalette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val soft: Color,
    val on: Color,
    val ink: Color,
    val boardLight: Color,
    val boardDark: Color,
    val boardLastMove: Color,
    val boardSelected: Color,
    val boardLegal: Color,
    val boardArrow: Color,
    val boardInk: Color,
) {
    /** Colour of the soft float shadow: a neutral tinted toward the theme, never coloured. */
    val shadow: Color get() = if (dark) Color(0xFF050706) else text
}

@Immutable
class AppTheme(val id: String, val nameKo: String, val light: AppPalette, val dark: AppPalette) {
    fun palette(dark: Boolean): AppPalette = if (dark) this.dark else light
}

/**
 * A colour for real state only (move quality, check, win/loss delta). [solid] carries white text (>= 4.5:1) and
 * is the text colour in light mode; [onDark] is the text colour in dark mode.
 */
@Immutable
data class Tone(val solid: Color, val onDark: Color) {
    fun text(dark: Boolean): Color = if (dark) onDark else solid
    /** Tinted chip background; [text] on it stays >= 4.5:1. */
    fun container(dark: Boolean): Color = text(dark).copy(alpha = if (dark) 0.16f else 0.12f)
}

object Tones {
    val best = Tone(Color(0xFF2A6F5E), Color(0xFF7CC9B4))
    val excellent = Tone(Color(0xFF356F47), Color(0xFF86C79A))
    val good = Tone(Color(0xFF52694D), Color(0xFFA9C3A1))
    val book = Tone(Color(0xFF7A5B39), Color(0xFFD2B48E))
    val inaccuracy = Tone(Color(0xFF79601A), Color(0xFFD8BE6E))
    val mistake = Tone(Color(0xFF964E28), Color(0xFFE4A07A))
    val blunder = Tone(Color(0xFFA83737), Color(0xFFE7948F))
    val positive = excellent // win, rating up
    val negative = blunder   // loss, rating down, check
}

private fun palette(
    dark: Boolean,
    bg: Long, surface: Long, surface2: Long, line: Long, text: Long, muted: Long,
    primary: Long, soft: Long, on: Long, ink: Long = primary,
    board: Board,
) = AppPalette(
    dark, Color(bg), Color(surface), Color(surface2), Color(line), Color(text), Color(muted),
    Color(primary), Color(soft), Color(on), Color(ink),
    board.light, board.dark, board.lastMove, board.selected, board.legal, board.arrow, board.ink,
)

/**
 * Board squares and overlays. Last move and selection use one warm yellow, a hue apart from every theme's squares
 * (delta E >= 27 on both); arrows use the theme's light-mode primary, deep enough to read on both squares.
 */
private class Board(light: Long, dark: Long, primary: Long, text: Long) {
    val light = Color(light)
    val dark = Color(dark)
    val ink = Color(text)
    val lastMove = HIGHLIGHT.copy(alpha = 0.5f)
    val selected = HIGHLIGHT.copy(alpha = 0.7f)
    val legal = ink.copy(alpha = 0.55f) // >= 3.4:1 on the light square; drawn on a light halo on dark squares
    val arrow = Color(primary)
}

private val HIGHLIGHT = Color(0xFFF2DB4B)

object AppThemes {
    val all: List<AppTheme> = listOf(
        Board(0xFFEBEFDF, 0xFF6F9467, 0xFF2B7A4B, 0xFF122016).let { b ->
            AppTheme(
                "forest", "포레스트",
                palette(false, 0xFFF0F7F1, 0xFFFFFFFF, 0xFFE3EFE5, 0xFFD1E2D5, 0xFF122016, 0xFF546D5A, 0xFF2B7A4B, 0xFFDFF2E4, 0xFFFFFFFF, 0xFF1F6B3E, board = b),
                palette(true, 0xFF0F1511, 0xFF162019, 0xFF1E2B22, 0xFF29382E, 0xFFE9F2EB, 0xFF93A898, 0xFF4ADE80, 0xFF1C3A27, 0xFF062812, board = b),
            )
        },
        Board(0xFFDEE3E6, 0xFF8CA2AD, 0xFF0A6BA8, 0xFF0F1F2B).let { b ->
            AppTheme(
                "ocean", "오션",
                palette(false, 0xFFEEF5FB, 0xFFFFFFFF, 0xFFE1EDF7, 0xFFCFE0EE, 0xFF0F1F2B, 0xFF536A7D, 0xFF0A6BA8, 0xFFDDEEFA, 0xFFFFFFFF, board = b),
                palette(true, 0xFF0D1419, 0xFF152029, 0xFF1D2B36, 0xFF283846, 0xFFE8F0F6, 0xFF93A6B5, 0xFF38BDF8, 0xFF17364A, 0xFF062033, board = b),
            )
        },
        Board(0xFFF0D9B5, 0xFFB58863, 0xFFB45309, 0xFF2A1E0A).let { b ->
            AppTheme(
                "walnut", "월넛",
                palette(false, 0xFFFFF7EA, 0xFFFFFFFF, 0xFFFBEBD1, 0xFFF0DDBD, 0xFF2A1E0A, 0xFF7C6039, 0xFFB45309, 0xFFFDEBCF, 0xFFFFFFFF, 0xFF924307, board = b),
                palette(true, 0xFF171309, 0xFF221C10, 0xFF2E2616, 0xFF3B3220, 0xFFF6EEDD, 0xFFB5A585, 0xFFF59E0B, 0xFF3D2F10, 0xFF2A1D02, board = b),
            )
        },
        Board(0xFFF4E4E8, 0xFFC48498, 0xFFD42670, 0xFF2A1420).let { b ->
            AppTheme(
                "rose", "로즈",
                palette(false, 0xFFFFF1F5, 0xFFFFFFFF, 0xFFFBE5EC, 0xFFF2D3DD, 0xFF2A1420, 0xFF82596B, 0xFFD42670, 0xFFFFE1EA, 0xFFFFFFFF, 0xFFB62160, board = b),
                palette(true, 0xFF1A1015, 0xFF241820, 0xFF30212B, 0xFF3D2C37, 0xFFF7EAF0, 0xFFB497A4, 0xFFF472B6, 0xFF3E1F30, 0xFF2A0B1C, board = b),
            )
        },
        Board(0xFFDCE1E5, 0xFF71808E, 0xFF0B7F73, 0xFF141A1D).let { b ->
            AppTheme(
                "slate", "슬레이트",
                palette(false, 0xFFF2F5F6, 0xFFFFFFFF, 0xFFE6EBED, 0xFFD6DDE0, 0xFF141A1D, 0xFF5F6B72, 0xFF0B7F73, 0xFFDDF3EF, 0xFFFFFFFF, 0xFF085E55, board = b),
                palette(true, 0xFF0F1214, 0xFF171C1F, 0xFF20272B, 0xFF2B3338, 0xFFE9EEF0, 0xFF98A4AA, 0xFF2DD4BF, 0xFF143732, 0xFF062421, board = b),
            )
        },
    )

    val default: AppTheme get() = all.first()

    fun byId(id: String?): AppTheme = all.firstOrNull { it.id == id } ?: default
}

/** Tokens of the selected theme and mode, including those Material's colour scheme has no slot for. */
val LocalAppColors = staticCompositionLocalOf { AppThemes.default.light }

/** True when the system animator duration scale is 0: animations jump to their end, confetti is skipped. Re-read on resume. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private fun AppPalette.toColorScheme(): ColorScheme {
    val danger = Tones.negative.text(dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = on, primaryContainer = soft, onPrimaryContainer = ink, inversePrimary = ink,
        secondary = primary, onSecondary = on, secondaryContainer = soft, onSecondaryContainer = ink,
        tertiary = primary, onTertiary = on, tertiaryContainer = soft, onTertiaryContainer = ink,
        background = bg, onBackground = text,
        surface = surface, onSurface = text, surfaceVariant = surface2, onSurfaceVariant = muted, surfaceTint = surface,
        surfaceBright = surface, surfaceDim = surface2,
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surface, surfaceContainerHighest = surface2,
        inverseSurface = text, inverseOnSurface = bg,
        outline = line, outlineVariant = line,
        error = danger, onError = if (dark) bg else surface, errorContainer = Tones.negative.container(dark), onErrorContainer = danger,
        scrim = shadow,
    )
}

// Korean keep-all: break between phrases (eojeol), never inside a word. Phrase breaking needs the text tagged as
// Korean, so the styles carry the locale even on a device set to another language.
private val Korean = LocaleList("ko-KR")
private val BodyBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)
private val HeadingBreak = LineBreak.Heading.copy(wordBreak = LineBreak.WordBreak.Phrase)

private fun style(size: Int, line: Int, weight: FontWeight, heading: Boolean = false) = TextStyle(
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    lineBreak = if (heading) HeadingBreak else BodyBreak,
    localeList = Korean,
)

private val AppTypography = Typography(
    displayLarge = style(30, 38, FontWeight.Bold, heading = true),
    displayMedium = style(28, 36, FontWeight.Bold, heading = true),
    displaySmall = style(26, 34, FontWeight.Bold, heading = true),
    headlineLarge = style(24, 32, FontWeight.Bold, heading = true),
    headlineMedium = style(22, 30, FontWeight.Bold, heading = true),
    headlineSmall = style(20, 28, FontWeight.Bold, heading = true),
    titleLarge = style(18, 26, FontWeight.SemiBold, heading = true),
    titleMedium = style(16, 24, FontWeight.SemiBold, heading = true),
    titleSmall = style(15, 22, FontWeight.SemiBold, heading = true),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(15, 22, FontWeight.Normal),
    bodySmall = style(13, 20, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(13, 18, FontWeight.SemiBold),
    labelSmall = style(12, 16, FontWeight.Medium),
)

/** Tabular figures, for ratings, Elo, accuracy and counters that change in place. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

// Shape lock: small tiles 18, cards 22, hero cards and sheets 28. Buttons, chips and icon buttons are full pills/circles.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** The soft "float" shadow, only for genuinely floating things (board, sheets, a sticky bottom bar). */
fun Modifier.floatShadow(shape: Shape, palette: AppPalette): Modifier =
    shadow(10.dp, shape, clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)

@Composable
fun ChessTheme(themeId: String = AppThemes.default.id, dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = AppThemes.byId(themeId).palette(dark)
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    // The setting changes without a configuration change, and only while the app is in the background.
    var reduced by remember(resolver) { mutableStateOf(read()) }
    LifecycleResumeEffect(resolver) { reduced = read(); onPauseOrDispose {} }
    MaterialTheme(colorScheme = remember(p) { p.toColorScheme() }, typography = AppTypography, shapes = AppShapes) {
        CompositionLocalProvider(
            LocalAppColors provides p,
            LocalReducedMotion provides reduced,
            LocalContentColor provides p.text,
            content = content,
        )
    }
}
