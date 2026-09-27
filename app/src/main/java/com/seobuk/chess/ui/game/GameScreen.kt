package com.seobuk.chess.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.seobuk.chess.core.GameStatus
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.ui.board.BoardArrow
import com.seobuk.chess.ui.board.ChessBoard
import com.seobuk.chess.ui.board.PieceIcon
import com.seobuk.chess.ui.board.SquareBadge
import com.seobuk.chess.ui.components.CapturedPieces
import com.seobuk.chess.ui.components.CoachBubble
import com.seobuk.chess.ui.components.Confetti
import com.seobuk.chess.ui.components.EvalBar
import com.seobuk.chess.ui.components.IconCircleButton
import com.seobuk.chess.ui.components.LevelAvatar
import com.seobuk.chess.ui.components.MoveList
import com.seobuk.chess.ui.components.PillTag
import com.seobuk.chess.ui.components.PromotionPicker
import com.seobuk.chess.ui.components.ThinkingDots
import com.seobuk.chess.ui.components.color
import com.seobuk.chess.ui.components.pressScale
import com.seobuk.chess.ui.sound.LocalSfx
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.Tones
import com.seobuk.chess.ui.theme.tabular
import kotlinx.coroutines.delay
import kotlin.math.min

private enum class Confirm { LEAVE, RESIGN, NEW }

private val THINKING = AiLine("", "")

@Composable
fun GameScreen(
    vm: GameViewModel,
    rating: Int,
    onLeave: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true, // false while animating out after being popped: back presses belong to the new top
) {
    val u = vm.ui
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var showResult by remember { mutableStateOf(false) }
    LaunchedEffect(u.result) {
        showResult = false
        if (u.result != null) { delay(900); showResult = true } // let the final move land first
    }
    val haptic = LocalHapticFeedback.current
    val plies = u.sanMoves.size
    var lastPlies by remember { mutableIntStateOf(plies) }
    LaunchedEffect(plies) {
        if (plies > lastPlies) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        lastPlies = plies
    }
    val sfx = LocalSfx.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(sfx, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.sounds.collect(sfx::play) } }
    BackHandler(enabled = active && u.inProgress) { confirm = Confirm.LEAVE }
    BackHandler(enabled = active && u.promotion != null) { vm.onPromotion(null) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // Short screens (small phones, split screen, large fonts) give the opponent's line one row instead of two.
        val compact = maxHeight < 700.dp
        // The coach's least height: card padding, title row (or the quality chip if taller) and one whole line of
        // the message. Measured, like the opponent line: with non-linear font scaling sp arithmetic is off.
        val measurer = rememberTextMeasurer()
        val type = MaterialTheme.typography
        val density = LocalDensity.current
        val minCoach = remember(type, measurer, density) {
            fun h(style: TextStyle) = measurer.measure("가", style).size.height
            with(density) { 24.dp.roundToPx() + maxOf(26.dp.roundToPx(), h(type.labelLarge), h(type.labelMedium) + 6.dp.roundToPx()) } + h(type.bodyMedium)
        }
        // While the promotion picker or the result sheet is up, the game underneath is out of TalkBack's reach.
        val modal = u.promotion != null || (showResult && u.result != null)
        GameLayout(
            minCoach,
            Modifier.fillMaxSize().then(if (modal) Modifier.clearAndSetSemantics {} else Modifier).safeDrawingPadding().padding(horizontal = 12.dp),
            top = {
                Column(Modifier.padding(bottom = 8.dp)) {
                    TopBar(u, onBack = { if (u.inProgress) confirm = Confirm.LEAVE else onLeave() }, onFlip = vm::flip)
                    OpponentRow(vm, u, lines = if (compact) 1 else 2)
                }
            },
            board = { BoardArea(u, vm::onSquareTap) },
            middle = {
                Column(Modifier.padding(vertical = 8.dp)) {
                    PlayerRow(u, rating)
                    Spacer(Modifier.height(6.dp))
                    // Fixed height (empty text or chips), so the board never resizes on the first move.
                    MoveList(u.sanMoves, Modifier.height(with(LocalDensity.current) { 21.sp.toDp() } + 16.dp), qualities = u.qualities)
                }
            },
            coach = {
                CoachBubble(u.coach.text, title = u.coach.title, icon = if (u.coach.hint) Icons.Outlined.Lightbulb else Icons.Outlined.School, quality = u.coach.quality, thinking = u.coach.thinking)
            },
            bottom = {
                Box(Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                    ActionBar(
                        u,
                        onHint = vm::hint,
                        onUndo = vm::undo,
                        onResign = { confirm = Confirm.RESIGN },
                        onResult = { showResult = true },
                        onNew = { if (u.inProgress) confirm = Confirm.NEW else vm.newGame() },
                    )
                }
            },
        )

        if (u.promotion != null) {
            Scrim()
            PromotionPicker(u.playerSide, onPick = vm::onPromotion, Modifier.align(Alignment.Center), onDismiss = { vm.onPromotion(null) })
        }

        val reduced = LocalReducedMotion.current
        val slide = with(LocalDensity.current) { 16.dp.roundToPx() }
        AnimatedVisibility(
            showResult && u.result != null,
            enter = if (reduced) EnterTransition.None else fadeIn(tween(220)) + slideInVertically(tween(220, easing = FastOutSlowInEasing)) { slide },
            exit = if (reduced) ExitTransition.None else fadeOut(tween(150)),
        ) {
            u.result?.let { r ->
                ResultOverlay(
                    r, vm.level,
                    onRematch = vm::newGame,
                    onLevels = onLevels,
                    onHome = onHome,
                    onViewBoard = { showResult = false },
                )
            }
        }
        Confetti(u.result?.outcome == Outcome.WIN && showResult)

        when (confirm) {
            Confirm.LEAVE -> ConfirmDialog("대국을 그만둘까요?", "지금 나가면 이번 대국은 기록되지 않아요.", "나가기", { confirm = null; onLeave() }, { confirm = null })
            Confirm.RESIGN -> ConfirmDialog("기권할까요?", "기권하면 패배로 기록되고 레이팅이 내려가요.", "기권하기", { confirm = null; vm.resign() }, { confirm = null })
            Confirm.NEW -> ConfirmDialog("새 게임을 시작할까요?", "진행 중인 대국은 기록되지 않아요.", "새로 시작", { confirm = null; vm.newGame() }, { confirm = null })
            null -> Unit
        }
    }
}

/** Back, the opening name (or 싱글 플레이) and flip. Turn, check and thinking show on the rows that own them. */
@Composable
private fun TopBar(u: GameUi, onBack: () -> Unit, onFlip: () -> Unit) {
    val reduced = LocalReducedMotion.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "뒤로", onBack)
        Spacer(Modifier.width(12.dp))
        AnimatedContent(
            u.opening,
            Modifier.weight(1f),
            transitionSpec = { if (reduced) EnterTransition.None togetherWith ExitTransition.None else fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "opening",
        ) { o ->
            Text(o?.nameKo ?: "싱글 플레이", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconCircleButton(Icons.Outlined.SwapVert, "판 돌리기", onFlip)
    }
}

@Composable
private fun OpponentRow(vm: GameViewModel, u: GameUi, lines: Int) {
    val level = vm.level
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        LevelAvatar(level.level, size = 48.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(level.nameKo, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                PillTag("Lv.${level.level}")
                Spacer(Modifier.width(8.dp))
                CapturedPieces(u.position, u.playerSide.opposite, Modifier.weight(1f), iconSize = 18.dp)
            }
            // Always [lines] tall, so the board doesn't move while the AI thinks and plays.
            // Measured rather than computed from the line height: with non-linear font scaling the two differ.
            val style = MaterialTheme.typography.bodySmall
            val measurer = rememberTextMeasurer()
            val height = with(LocalDensity.current) { remember(style, measurer, lines) { measurer.measure(List(lines) { "가" }.joinToString("\n"), style).size.height }.toDp() }
            AnimatedContent(
                targetState = if (u.aiThinking) THINKING else u.aiLine,
                modifier = Modifier.height(height),
                transitionSpec = { if (reduced) EnterTransition.None togetherWith ExitTransition.None else fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                contentAlignment = Alignment.CenterStart,
                label = "aiLine",
            ) { line ->
                when {
                    line === THINKING -> Row(verticalAlignment = Alignment.CenterVertically) {
                        ThinkingDots(dotSize = 6.dp, color = c.ink)
                        Spacer(Modifier.width(4.dp))
                        Text("생각 중이에요", style = MaterialTheme.typography.bodySmall, color = c.ink)
                    }
                    line == null -> Text("약 Elo ${level.approxElo} · ${level.description}", style = style, color = c.muted, maxLines = lines, overflow = TextOverflow.Ellipsis)
                    else -> Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.SemiBold)) { append(line.san) } // reads as notation
                            append(" ${line.text}")
                        },
                        style = style,
                        color = c.muted,
                        maxLines = lines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardArea(u: GameUi, onTap: (Int) -> Unit) {
    val check = remember(u.position) { if (u.position.isInCheck()) u.position.kingSquare(u.position.sideToMove) else null }
    val primary = LocalAppColors.current.boardArrow
    val arrows = remember(u.hintMove, primary) { u.hintMove?.let { listOf(BoardArrow(it.from, it.to, primary)) } ?: emptyList() }
    val badge = remember(u.badge) { u.badge?.let { (sq, q) -> SquareBadge(sq, q.symbol, q.color()) } }
    Row { // sized by GameLayout: BOARD_GUTTER + side wide, side tall
        EvalBar(u.evalCp, Modifier.fillMaxHeight(), flipped = u.flipped, vertical = true)
        Spacer(Modifier.width(6.dp))
        ChessBoard(
            u.position,
            Modifier.fillMaxHeight(),
            flipped = u.flipped,
            selectedSquare = u.selected,
            legalTargets = u.targets,
            lastMove = u.lastMove,
            checkSquare = check,
            arrows = arrows,
            badge = badge,
            onSquareClick = onTap,
        )
    }
}

private val BOARD_GUTTER = 24.dp // eval bar 18 + gap 6

/**
 * Stacks [top], board, [middle], [coach] and [bottom] (pinned to the bottom). The board is as wide as the screen
 * allows but shrinks before the coach gets less than its title and one whole line of text (short phones, split
 * screen, landscape); the coach may use any spare height and scrolls its message beyond that.
 */
@Composable
private fun GameLayout(
    minCoach: Int,
    modifier: Modifier,
    top: @Composable () -> Unit,
    board: @Composable () -> Unit,
    middle: @Composable () -> Unit,
    coach: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    Layout(listOf(top, board, middle, coach, bottom), modifier) { slots, cs ->
        val w = cs.maxWidth
        val h = cs.maxHeight
        val row = Constraints(minWidth = w, maxWidth = w)
        val t = slots[0].first().measure(row)
        val m = slots[2].first().measure(row)
        val a = slots[4].first().measure(row)
        val spare = (h - t.height - m.height - a.height).coerceAtLeast(0)
        val gutter = BOARD_GUTTER.roundToPx()
        val side = min(w - gutter, spare - minCoach).coerceAtLeast(0)
        val b = slots[1].first().measure(Constraints.fixed(side + gutter, side))
        val c = slots[3].first().measure(Constraints(minWidth = w, maxWidth = w, maxHeight = (spare - side).coerceAtLeast(0)))
        layout(w, h) {
            var y = 0
            for ((p, x) in listOf(t to 0, b to (w - b.width) / 2, m to 0, c to 0)) {
                p.place(x, y)
                y += p.height
            }
            a.place(0, h - a.height)
        }
    }
}

@Composable
private fun PlayerRow(u: GameUi, rating: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        PieceIcon(Piece(PieceType.KING, u.playerSide), 26.dp)
        Spacer(Modifier.width(6.dp))
        Text("나", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        Text("$rating", style = MaterialTheme.typography.labelMedium.tabular(), color = LocalAppColors.current.muted)
        Spacer(Modifier.width(8.dp))
        CapturedPieces(u.position, u.playerSide, Modifier.weight(1f), iconSize = 18.dp)
        // Always laid out, so the row keeps its height.
        val check = u.isPlayerTurn && u.position.isInCheck()
        PillTag(
            if (check) "체크" else "내 차례",
            Modifier.graphicsLayer { alpha = if (u.isPlayerTurn) 1f else 0f },
            tone = if (check) Tones.negative else null,
        )
    }
}

@Composable
private fun ActionBar(u: GameUi, onHint: () -> Unit, onUndo: () -> Unit, onResign: () -> Unit, onResult: () -> Unit, onNew: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(Icons.Outlined.Lightbulb, "힌트", u.isPlayerTurn && !u.hintLoading && u.hintMove == null, onHint, Modifier.weight(1f))
        ActionButton(Icons.AutoMirrored.Outlined.Undo, "무르기", u.canUndo, onUndo, Modifier.weight(1f))
        if (u.result == null) ActionButton(Icons.Outlined.Flag, "기권", u.status == GameStatus.ONGOING, onResign, Modifier.weight(1f))
        else ActionButton(Icons.Outlined.Flag, "결과", true, onResult, Modifier.weight(1f))
        ActionButton(Icons.Outlined.Refresh, "새 게임", true, onNew, Modifier.weight(1f))
    }
}

/** Tonal pill with the icon above its label. */
@Composable
private fun ActionButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick,
        modifier.heightIn(min = 60.dp).pressScale(source),
        enabled = enabled,
        // lighter than M3's grey when disabled, so an unavailable action never looks heavier than the others
        colors = ButtonDefaults.filledTonalButtonColors(disabledContainerColor = LocalAppColors.current.surface2),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        interactionSource = source,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
