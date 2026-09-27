package com.seobuk.chess.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seobuk.chess.ai.AiLevel
import com.seobuk.chess.ai.Ko
import com.seobuk.chess.learn.StrategyTopic
import com.seobuk.chess.ui.board.ChessBoard
import com.seobuk.chess.ui.components.AppCard
import com.seobuk.chess.ui.components.HeroNumber
import com.seobuk.chess.ui.components.IconCircle
import com.seobuk.chess.ui.components.LevelAvatar
import com.seobuk.chess.ui.components.ListRow
import com.seobuk.chess.ui.components.PillTag
import com.seobuk.chess.ui.components.PrimaryButton
import com.seobuk.chess.ui.components.SectionHeader
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.tabular
import com.seobuk.chess.ui.update.UpdateState

fun ratingTitle(rating: Int): String = when {
    rating < 600 -> "입문자"
    rating < 900 -> "초급자"
    rating < 1200 -> "중급 도전자"
    rating < 1500 -> "중급자"
    rating < 1800 -> "상급자"
    else -> "마스터 후보"
}

/**
 * [tip]: a strategy topic whose example position is shown as a live mini board; tapping it opens the topic.
 * [update]: the self-update card above the list, hidden while [UpdateState.Idle].
 */
@Composable
fun HomeScreen(
    rating: Int,
    recommendedLevel: AiLevel,
    tip: StrategyTopic,
    onPlay: () -> Unit,
    onTip: () -> Unit,
    onOpenings: () -> Unit,
    onStrategy: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier,
    update: UpdateState = UpdateState.Idle,
    onUpdate: () -> Unit = {},
    onUpdateLater: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("체스 마스터", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.displayLarge)
        HeroCard(rating, recommendedLevel, onPlay)
        TipCard(tip, onTip)
        Column {
            // Inside this Column, not the spaced one above: a hidden AnimatedVisibility would still take a 16dp gap.
            val reduced = LocalReducedMotion.current
            AnimatedVisibility(
                update !is UpdateState.Idle,
                enter = if (reduced) EnterTransition.None else fadeIn() + expandVertically(),
                exit = if (reduced) ExitTransition.None else fadeOut() + shrinkVertically(),
            ) {
                var shown by remember { mutableStateOf(update) }
                if (update !is UpdateState.Idle) shown = update // keep the last content while animating out
                UpdateCard(shown, onUpdate, onUpdateLater, Modifier.padding(bottom = 4.dp))
            }
            SectionHeader("배우고 돌아보기")
            AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                ListRow("오프닝 배우기", onOpenings, subtitle = "유명한 첫 수를 한 수씩 배워요", leading = { IconCircle(Icons.AutoMirrored.Outlined.MenuBook) })
                ListRow("전략 가이드", onStrategy, subtitle = "전술부터 엔드게임까지 핵심 원리", leading = { IconCircle(Icons.Outlined.Psychology) })
                ListRow("내 기록", onStats, subtitle = "레이팅 변화, 전적, 테마 설정", leading = { IconCircle(Icons.AutoMirrored.Outlined.ShowChart) })
            }
        }
    }
}

@Composable
private fun HeroCard(rating: Int, level: AiLevel, onPlay: () -> Unit) {
    val c = LocalAppColors.current
    AppCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(20.dp)) {
        Text("내 레이팅", style = MaterialTheme.typography.labelLarge, color = c.muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroNumber(rating, Modifier.padding(end = 12.dp))
            PillTag(ratingTitle(rating))
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().border(1.dp, c.line, MaterialTheme.shapes.small).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LevelAvatar(level.level, size = 44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("추천 상대", style = MaterialTheme.typography.labelMedium, color = c.muted)
                Text(level.nameKo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Lv.${level.level} · 약 Elo ${level.approxElo}", style = MaterialTheme.typography.labelMedium.tabular(), color = c.muted)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("대국 시작", onPlay, Modifier.fillMaxWidth())
    }
}

@Composable
private fun TipCard(tip: StrategyTopic, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val position = remember(tip.id) { tip.examplePosition() }
    val arrows = remember(tip.id, c.boardArrow) { tip.boardArrows(c.boardArrow) }
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (position != null) ChessBoard(position, Modifier.size(120.dp), arrows = arrows)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text("오늘의 포지션", style = MaterialTheme.typography.labelMedium, color = c.muted)
                Text(tip.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(tip.exampleCaption ?: tip.summary, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun UpdateCard(state: UpdateState, onUpdate: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    AppCard(modifier.fillMaxWidth()) {
        when (state) {
            is UpdateState.Available -> {
                Text("새 버전 ${Ko.iGa("v${state.release.version}")} 있어요", style = MaterialTheme.typography.titleMedium)
                val size = "%.1f MB".format(state.release.sizeBytes / 1048576.0)
                Text(listOfNotNull(size, state.release.note).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("업데이트", onUpdate, Modifier.weight(1f))
                    TextButton(onLater, Modifier.padding(start = 8.dp)) { Text("나중에") }
                }
            }
            is UpdateState.Downloading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("새 버전을 받고 있어요", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.tabular(), color = c.muted)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator({ state.progress }, Modifier.fillMaxWidth(), color = c.primary, trackColor = c.soft)
            }
            UpdateState.Installing -> Text("설치 화면으로 넘어가요", style = MaterialTheme.typography.titleMedium)
            is UpdateState.Failed -> {
                Text(state.message, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.padding(top = 4.dp)) {
                    TextButton(onUpdate) { Text("다시 시도") }
                    TextButton(onLater, Modifier.padding(start = 8.dp)) { Text("나중에") }
                }
            }
            UpdateState.Idle -> {}
        }
    }
}
