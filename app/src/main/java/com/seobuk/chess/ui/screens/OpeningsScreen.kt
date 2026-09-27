package com.seobuk.chess.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.seobuk.chess.core.Move
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import com.seobuk.chess.learn.Opening
import com.seobuk.chess.learn.OpeningStep
import com.seobuk.chess.ui.board.BoardArrow
import com.seobuk.chess.ui.board.ChessBoard
import com.seobuk.chess.ui.board.PieceIcon
import com.seobuk.chess.ui.components.AppCard
import com.seobuk.chess.ui.components.IconCircleButton
import com.seobuk.chess.ui.components.ListRow
import com.seobuk.chess.ui.components.MoveList
import com.seobuk.chess.ui.components.PillTag
import com.seobuk.chess.ui.components.PrimaryButton
import com.seobuk.chess.ui.components.ScreenHeader
import com.seobuk.chess.ui.components.SectionHeader
import com.seobuk.chess.ui.components.bottomInset
import com.seobuk.chess.ui.components.figurine
import com.seobuk.chess.ui.components.screenInsets
import com.seobuk.chess.ui.sound.LocalSfx
import com.seobuk.chess.ui.sound.SfxEvent
import com.seobuk.chess.ui.sound.sfxFor
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.tabular
import kotlinx.coroutines.delay

private fun difficultyLabel(d: Int) = when (d) { 1 -> "입문"; 2 -> "중급"; else -> "고급" }

/** Replays UCI steps from the start; stops at the first illegal/unparseable move. */
private class OpeningLine(steps: List<OpeningStep>) {
    val positions = mutableListOf(Position.start())
    val moves = mutableListOf<Move>()
    val sans = mutableListOf<String>()

    init {
        val p = Position.start()
        for (step in steps) {
            val m = Move.fromUci(step.uci)?.takeIf { p.isLegal(it) } ?: break
            sans += p.san(m)
            p.makeMove(m)
            moves += m
            positions += p.copy()
        }
    }
}

@Composable
fun OpeningsScreen(openings: List<Opening>, onOpen: (id: String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val categories = remember(openings) { openings.map { it.category }.distinct() }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val grouped = remember(openings, filter) { openings.filter { filter == null || it.category == filter }.groupBy { it.category } }
    val reduced = LocalReducedMotion.current
    Column(modifier.fillMaxSize().screenInsets()) {
        ScreenHeader("오프닝 배우기", onBack, subtitle = "${openings.size}개의 오프닝을 한 수씩 배워요")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { CategoryChip("전체", filter == null) { filter = null } }
            items(categories) { c -> CategoryChip(c.substringBefore(" ("), filter == c) { filter = c } }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + bottomInset()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.forEach { (category, list) ->
                item(key = category) {
                    Column(if (reduced) Modifier else Modifier.animateItem()) {
                        SectionHeader(category)
                        AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                            list.forEach { o ->
                                ListRow(o.nameKo, { onOpen(o.id) }, subtitle = "${o.nameEn} · ${o.eco}") {
                                    Text(difficultyLabel(o.difficulty), style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.muted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected, onClick, label = { Text(text) }, shape = CircleShape)
}

@Composable
fun OpeningDetailScreen(
    opening: Opening,
    onPractice: (Opening) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = remember(opening.id) { OpeningLine(opening.steps) }
    val total = line.moves.size
    var ply by rememberSaveable(opening.id) { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var flipped by rememberSaveable(opening.id) { mutableStateOf(false) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        if (ply >= total) { ply = 0; delay(500) }
        while (ply < total) { delay(1500); ply++ }
        playing = false
    }
    val position = line.positions[ply]
    val next = line.moves.getOrNull(ply)
    // One step forward sounds like the move it is; any other change (back, jump, restart) is a plain move.
    val sfx = LocalSfx.current
    var heardPly by remember { mutableIntStateOf(ply) }
    LaunchedEffect(ply) {
        if (ply == heardPly + 1) sfx.play(sfxFor(line.positions[ply - 1], line.moves[ply - 1], position))
        else if (ply != heardPly) sfx.play(SfxEvent.MOVE)
        heardPly = ply
    }

    Column(modifier.fillMaxSize().screenInsets()) {
        ScreenHeader(opening.nameKo.substringBefore(" ("), onBack, subtitle = "${opening.nameEn} · ${opening.eco}") {
            IconCircleButton(Icons.Outlined.SwapVert, "판 돌리기", { flipped = !flipped })
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = bottomInset()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChessBoard(
                position,
                Modifier.fillMaxWidth().padding(top = 4.dp),
                flipped = flipped,
                lastMove = line.moves.getOrNull(ply - 1),
                checkSquare = if (position.isInCheck()) position.kingSquare(position.sideToMove) else null,
                arrows = if (!playing && next != null) listOf(BoardArrow(next.from, next.to, LocalAppColors.current.boardArrow)) else emptyList(),
            )
            StepControls(
                ply, total, playing,
                onFirst = { playing = false; ply = 0 },
                onPrev = { playing = false; if (ply > 0) ply-- },
                onPlay = { playing = !playing },
                onNext = { playing = false; if (ply < total) ply++ },
                onLast = { playing = false; ply = total },
            )
            StepComment(opening, line, ply)
            AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                MoveList(line.sans, currentPly = ply, onPlyClick = { playing = false; ply = it })
            }
            AppCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillTag(opening.category.substringBefore(" ("))
                    PillTag("난이도 ${difficultyLabel(opening.difficulty)}")
                }
                Spacer(Modifier.height(10.dp))
                Text(opening.summary, style = MaterialTheme.typography.bodyLarge)
            }
            IdeasCard("백의 계획", Side.WHITE, opening.ideasWhite)
            IdeasCard("흑의 계획", Side.BLACK, opening.ideasBlack)
            PrimaryButton(
                "이 오프닝으로 연습하기",
                onClick = { onPractice(opening) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StepControls(
    ply: Int, total: Int, playing: Boolean,
    onFirst: () -> Unit, onPrev: () -> Unit, onPlay: () -> Unit, onNext: () -> Unit, onLast: () -> Unit,
) {
    val c = LocalAppColors.current
    val target = if (total == 0) 0f else ply / total.toFloat()
    val progress by animateFloatAsState(target, if (LocalReducedMotion.current) snap() else tween(220), label = "stepProgress")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f),
                color = c.primary,
                trackColor = c.soft,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(12.dp))
            Text("$ply / $total", style = MaterialTheme.typography.labelLarge.tabular(), color = c.muted)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(Icons.Outlined.SkipPrevious, "처음으로", onFirst, enabled = ply > 0)
            IconCircleButton(Icons.AutoMirrored.Outlined.NavigateBefore, "이전 수", onPrev, enabled = ply > 0)
            IconCircleButton(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if (playing) "멈춤" else "자동 재생",
                onPlay,
                size = 60.dp,
                emphasized = true,
            )
            IconCircleButton(Icons.AutoMirrored.Outlined.NavigateNext, "다음 수", onNext, enabled = ply < total)
            IconCircleButton(Icons.Outlined.SkipNext, "끝으로", onLast, enabled = ply < total)
        }
    }
}

@Composable
private fun StepComment(opening: Opening, line: OpeningLine, ply: Int) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val slide = with(LocalDensity.current) { 16.dp.roundToPx() }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        AnimatedContent(
            targetState = ply,
            transitionSpec = {
                val dir = if (targetState >= initialState) 1 else -1
                if (reduced) EnterTransition.None togetherWith ExitTransition.None
                else (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { dir * slide } + fadeIn(tween(220)))
                    .togetherWith(fadeOut(tween(120)))
            },
            label = "stepComment",
        ) { p ->
            Column {
                if (p == 0) {
                    Text("시작 위치", style = MaterialTheme.typography.titleMedium, color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    Text("재생 버튼을 누르거나 다음 수로 넘기며 ${opening.nameKo}의 수순을 따라가 봐요. 화살표가 다음 수예요.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    val white = p % 2 == 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${(p + 1) / 2}${if (white) "." else "..."} ${figurine(line.sans[p - 1])}",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.ink,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(if (white) "백" else "흑", style = MaterialTheme.typography.labelMedium, color = c.muted)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(opening.steps[p - 1].comment, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun IdeasCard(title: String, side: Side, ideas: List<String>) {
    if (ideas.isEmpty()) return
    val c = LocalAppColors.current
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieceIcon(Piece(PieceType.KING, side), 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        ideas.forEach { idea ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Box(Modifier.padding(top = 8.dp).size(6.dp).background(c.primary, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(idea, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
        }
    }
}
