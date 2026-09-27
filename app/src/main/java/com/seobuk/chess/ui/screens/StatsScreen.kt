package com.seobuk.chess.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seobuk.chess.ai.AiLevel
import com.seobuk.chess.ai.AiLevels
import com.seobuk.chess.ai.MoveQuality
import com.seobuk.chess.ui.components.AppCard
import com.seobuk.chess.ui.components.DeltaPill
import com.seobuk.chess.ui.components.HeroNumber
import com.seobuk.chess.ui.components.IconCircle
import com.seobuk.chess.ui.components.LevelAvatar
import com.seobuk.chess.ui.components.ListRow
import com.seobuk.chess.ui.components.PillTag
import com.seobuk.chess.ui.components.QualityChip
import com.seobuk.chess.ui.components.ScreenHeader
import com.seobuk.chess.ui.components.bottomInset
import com.seobuk.chess.ui.components.pressScale
import com.seobuk.chess.ui.components.screenInsets
import com.seobuk.chess.ui.theme.AppTheme
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.Tones
import com.seobuk.chess.ui.theme.tabular
import com.seobuk.chess.ui.update.UpdateCheck
import kotlinx.coroutines.launch

data class StatsUi(
    val rating: Int,
    val ratingHistory: List<Int>,           // oldest first, ends with the current rating
    val levelRecords: Map<Int, LevelRecord>,
    val qualityTotals: Map<MoveQuality, Int>,
    val hintsUsed: Int,
    val recommendedLevel: AiLevel,
)

@Composable
fun StatsScreen(
    stats: StatsUi,
    themes: List<AppTheme>,
    selectedThemeId: String,
    onSelectTheme: (id: String) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabled: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = "",
    onCheckUpdate: suspend () -> UpdateCheck = { UpdateCheck.UP_TO_DATE },
) {
    val total = remember(stats.levelRecords) {
        stats.levelRecords.values.fold(LevelRecord()) { a, r -> LevelRecord(a.wins + r.wins, a.draws + r.draws, a.losses + r.losses) }
    }
    Column(modifier.fillMaxSize().screenInsets()) {
        ScreenHeader("내 기록", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = bottomInset()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RatingHero(stats)
            Totals(total)
            LevelRecords(stats.levelRecords)
            Qualities(stats.qualityTotals, stats.hintsUsed)
            ThemePicker(themes, selectedThemeId, onSelectTheme)
            SoundSetting(soundEnabled, onSoundEnabled)
            UpdateRow(versionName, onCheckUpdate)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RatingHero(stats: StatsUi) {
    val c = LocalAppColors.current
    val history = stats.ratingHistory
    val delta = history.firstOrNull()?.let { stats.rating - it } ?: 0
    AppCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("현재 레이팅", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = c.muted)
            PillTag(ratingTitle(stats.rating))
        }
        HeroNumber(stats.rating)
        if (history.size >= 2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeltaPill(delta)
                Text("최근 ${history.size}개 기록 대비", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Spacer(Modifier.height(16.dp))
            Sparkline(history, Modifier.fillMaxWidth().height(88.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("최저 ${history.min()}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.tabular(), color = c.muted)
                Text("최고 ${history.max()}", style = MaterialTheme.typography.labelSmall.tabular(), color = c.muted)
            }
        } else {
            Text("대국을 마치면 레이팅 변화가 여기에 그래프로 그려져요.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
        Spacer(Modifier.height(16.dp))
        val level = stats.recommendedLevel
        Row(
            Modifier.fillMaxWidth().border(1.dp, c.line, MaterialTheme.shapes.small).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LevelAvatar(level.level, size = 40.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text("지금 딱 맞는 상대", style = MaterialTheme.typography.labelMedium, color = c.muted)
                Text("Lv.${level.level} ${level.nameKo} · 약 Elo ${level.approxElo}", style = MaterialTheme.typography.titleSmall.tabular())
            }
        }
    }
}

/** Single-series line in the theme primary: no fill, no grid, no track; a small end dot marks the current rating. */
@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val summary = "레이팅 변화 그래프: 처음 ${values.first()}, 최고 ${values.max()}, 최저 ${values.min()}, 지금 ${values.last()}"
    Canvas(modifier.semantics { contentDescription = summary }) {
        val min = values.min().toFloat()
        val range = (values.max() - min).coerceAtLeast(20f)
        val pad = 6.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        // Centre a flat-ish series vertically instead of pinning it to the bottom.
        val lift = (h - (values.max() - min) / range * h) / 2
        fun pt(i: Int) = Offset(pad + w * i / (values.size - 1), pad + h - lift - (values[i] - min) / range * h)
        val line = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until values.size) lineTo(pt(i).x, pt(i).y)
        }
        drawPath(line, c.primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val end = pt(values.size - 1)
        drawCircle(c.surface, 6.dp.toPx(), end)
        drawCircle(c.primary, 4.dp.toPx(), end)
    }
}

@Composable
private fun Totals(total: LevelRecord) {
    val c = LocalAppColors.current
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("전적", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (total.games > 0) {
                Text("승률 ${total.wins * 100 / total.games}%", style = MaterialTheme.typography.labelLarge.tabular(), color = c.muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (total.games == 0) {
            Text("아직 대국 기록이 없어요. 첫 대국을 시작해 봐요.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(total.games, "대국", c.muted, Modifier.weight(1f))
                StatTile(total.wins, "승리", Tones.positive.text(c.dark), Modifier.weight(1f))
                StatTile(total.draws, "무승부", c.muted, Modifier.weight(1f))
                StatTile(total.losses, "패배", Tones.negative.text(c.dark), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String, labelColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(LocalAppColors.current.surface2, MaterialTheme.shapes.small).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = MaterialTheme.typography.headlineMedium.tabular())
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
    }
}

@Composable
private fun LevelRecords(records: Map<Int, LevelRecord>) {
    val c = LocalAppColors.current
    val played = AiLevels.all.filter { (records[it.level]?.games ?: 0) > 0 }
    AppCard(Modifier.fillMaxWidth()) {
        Text("레벨별 기록", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        if (played.isEmpty()) {
            Text("레벨별 승패가 여기에 쌓여요.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            played.forEach { level ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelAvatar(level.level, size = 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(level.nameKo, style = MaterialTheme.typography.titleSmall)
                        Text("Lv.${level.level}", style = MaterialTheme.typography.labelMedium.tabular(), color = c.muted)
                    }
                    RecordLine(records.getValue(level.level))
                }
            }
        }
    }
}

@Composable
private fun Qualities(totals: Map<MoveQuality, Int>, hintsUsed: Int) {
    val c = LocalAppColors.current
    AppCard(Modifier.fillMaxWidth()) {
        Text("수 품질 통계", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MoveQuality.entries.forEach { q ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QualityChip(q)
                    Spacer(Modifier.weight(1f))
                    Text("${totals[q] ?: 0}", style = MaterialTheme.typography.titleSmall.tabular())
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.Outlined.Lightbulb, null, Modifier.size(20.dp), tint = c.ink)
                Text("힌트 사용", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Text("${hintsUsed}회", style = MaterialTheme.typography.titleSmall.tabular())
            }
        }
    }
}

@Composable
private fun ThemePicker(themes: List<AppTheme>, selectedId: String, onSelect: (String) -> Unit) {
    val c = LocalAppColors.current
    AppCard(Modifier.fillMaxWidth()) {
        Text("테마", style = MaterialTheme.typography.titleMedium)
        Text("앱 전체와 체스판 색이 함께 바뀌어요.", style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            themes.forEach { t -> ThemeSwatch(t, t.id == selectedId, { onSelect(t.id) }, Modifier.weight(1f)) }
        }
    }
}

/** The whole row toggles, and TalkBack reads it as one switch. */
@Composable
private fun SoundSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth().toggleable(enabled, role = Role.Switch, onValueChange = onChange).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("효과음", style = MaterialTheme.typography.titleMedium)
                Text("수를 두거나 대국이 끝날 때 소리로 알려 줘요.", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Switch(enabled, onCheckedChange = null)
        }
    }
}

/** "업데이트 확인": a forced check whose outcome replaces the version line; with an update the caller goes Home. */
@Composable
private fun UpdateRow(versionName: String, check: suspend () -> UpdateCheck) {
    var note by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
        ListRow(
            "업데이트 확인",
            onClick = {
                if (note != CHECKING) scope.launch {
                    note = CHECKING
                    note = when (check()) {
                        UpdateCheck.HAS_UPDATE -> null
                        UpdateCheck.UP_TO_DATE -> "최신 버전이에요"
                        UpdateCheck.ERROR -> "확인하지 못했어요. 나중에 다시 시도해 주세요."
                    }
                }
            },
            subtitle = note ?: "현재 v$versionName",
            leading = { IconCircle(Icons.Outlined.SystemUpdate) },
        )
    }
}

private const val CHECKING = "확인하는 중이에요"

/** A 2x2 board-square preview with the theme's primary as a dot; the selected swatch gets a primary ring. */
@Composable
private fun ThemeSwatch(theme: AppTheme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val p = theme.palette(c.dark)
    val tile = MaterialTheme.shapes.small
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(source, 0.94f)
            .clip(tile)
            .selectable(selected, source, ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).border(2.dp, if (selected) c.primary else c.surface, tile).padding(4.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Canvas(Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraSmall)) {
                val s = Size(size.width / 2, size.height / 2)
                drawRect(p.boardLight, Offset.Zero, s)
                drawRect(p.boardDark, Offset(s.width, 0f), s)
                drawRect(p.boardDark, Offset(0f, s.height), s)
                drawRect(p.boardLight, Offset(s.width, s.height), s)
            }
            Box(Modifier.padding(4.dp).size(16.dp).background(c.surface, CircleShape).padding(2.dp).background(p.primary, CircleShape))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            theme.nameKo,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.ink else c.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
