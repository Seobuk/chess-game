package com.seobuk.chess.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seobuk.chess.ai.AiLevel
import com.seobuk.chess.core.Piece
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Side
import com.seobuk.chess.ui.board.PieceIcon
import com.seobuk.chess.ui.components.LevelAvatar
import com.seobuk.chess.ui.components.PillTag
import com.seobuk.chess.ui.components.PrimaryButton
import com.seobuk.chess.ui.components.ScreenHeader
import com.seobuk.chess.ui.components.screenInsets
import com.seobuk.chess.ui.components.pressScale
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.Tones
import com.seobuk.chess.ui.theme.floatShadow
import com.seobuk.chess.ui.theme.tabular

data class LevelRecord(val wins: Int = 0, val draws: Int = 0, val losses: Int = 0) {
    val games: Int get() = wins + draws + losses
}

enum class SideChoice(val labelKo: String) { WHITE("백"), BLACK("흑"), RANDOM("랜덤") }

/** "여우와", "드래곤과": picks 와/과 from the last syllable. */
internal fun withGwa(word: String): String {
    val c = word.lastOrNull() ?: return word
    val batchim = c in '가'..'힣' && (c - '가') % 28 != 0
    return word + if (batchim) "과" else "와"
}

@Composable
fun LevelSelectScreen(
    levels: List<AiLevel>,
    records: Map<Int, LevelRecord>,
    recommendedLevel: Int,
    onStart: (level: Int, side: SideChoice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    var selected by rememberSaveable { mutableIntStateOf(recommendedLevel) }
    var side by rememberSaveable { mutableStateOf(SideChoice.WHITE) }
    // Start scrolled so the recommended level sits near the top.
    val first = remember { levels.indexOfFirst { it.level == recommendedLevel }.let { if (it >= 2) it - 1 else 0 } }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = first)
    val c = LocalAppColors.current
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).screenInsets()) {
            ScreenHeader("상대 고르기", onBack, subtitle = subtitle)
            LazyColumn(
                Modifier.weight(1f),
                state = list,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(levels, key = { it.level }) { level ->
                    LevelRow(
                        level,
                        records[level.level] ?: LevelRecord(),
                        recommended = level.level == recommendedLevel,
                        selected = level.level == selected,
                        onClick = { selected = level.level },
                    )
                }
            }
        }
        val chosen = levels.firstOrNull { it.level == selected } ?: levels.first()
        val barShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .floatShadow(barShape, c)
                .background(c.surface, barShape)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text("내 기물 색", style = MaterialTheme.typography.labelLarge, color = c.muted)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SideChoice.entries.forEachIndexed { i, choice ->
                    SegmentedButton(
                        selected = choice == side,
                        onClick = { side = choice },
                        shape = SegmentedButtonDefaults.itemShape(i, SideChoice.entries.size, CircleShape),
                        // the active segment shows M3's check, so the choice doesn't rest on the soft fill alone
                        icon = {
                            SegmentedButtonDefaults.Icon(choice == side) {
                                when (choice) {
                                    SideChoice.WHITE -> PieceIcon(Piece(PieceType.KING, Side.WHITE), 24.dp)
                                    SideChoice.BLACK -> PieceIcon(Piece(PieceType.KING, Side.BLACK), 24.dp)
                                    SideChoice.RANDOM -> Icon(Icons.Outlined.Shuffle, null, Modifier.size(18.dp))
                                }
                            }
                        },
                    ) { Text(choice.labelKo) }
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("${withGwa(chosen.nameKo)} 대국 시작", { onStart(chosen.level, side) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LevelRow(level: AiLevel, record: LevelRecord, recommended: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val source = remember { MutableInteractionSource() }
    OutlinedCard(
        onClick,
        Modifier.fillMaxWidth().pressScale(source).semantics { this.selected = selected },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = if (selected) c.soft else c.surface, contentColor = c.text),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) c.primary else c.line),
        interactionSource = source,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelAvatar(level.level, size = 52.dp, container = if (selected) c.surface else c.soft)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(level.nameKo, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (recommended) PillTag("추천", Modifier.padding(start = 8.dp), container = if (selected) c.surface else null)
                }
                Text("Lv.${level.level} · 약 Elo ${level.approxElo}", style = MaterialTheme.typography.labelMedium.tabular(), color = c.muted)
                Spacer(Modifier.height(4.dp))
                Text(level.description, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp)) // always shown, so every row has the same height
                RecordLine(record)
            }
        }
    }
}

/** "3승 1무 2패" as numbers with small semantic labels (win and loss tinted, draw muted); muted numbers before a first game. */
@Composable
internal fun RecordLine(record: LevelRecord, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val number = MaterialTheme.typography.labelLarge.tabular()
    val label = MaterialTheme.typography.labelMedium
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        listOf(
            Triple(record.wins, "승", Tones.positive.text(c.dark)),
            Triple(record.draws, "무", c.muted),
            Triple(record.losses, "패", Tones.negative.text(c.dark)),
        ).forEach { (n, word, color) ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$n", style = number, color = if (record.games == 0) c.muted else c.text)
                Text(word, Modifier.padding(start = 2.dp), style = label, color = color)
            }
        }
    }
}
