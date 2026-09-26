package com.seobuk.chess.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Squares
import com.seobuk.chess.learn.StrategyTopic
import com.seobuk.chess.ui.board.BoardArrow
import com.seobuk.chess.ui.board.ChessBoard
import com.seobuk.chess.ui.components.AppCard
import com.seobuk.chess.ui.components.ListRow
import com.seobuk.chess.ui.components.PillTag
import com.seobuk.chess.ui.components.ScreenHeader
import com.seobuk.chess.ui.components.SectionHeader
import com.seobuk.chess.ui.components.bottomInset
import com.seobuk.chess.ui.components.screenInsets
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.tabular

internal fun StrategyTopic.examplePosition(): Position? = exampleFen?.let { runCatching { Position.fromFen(it) }.getOrNull() }

internal fun StrategyTopic.boardArrows(color: Color): List<BoardArrow> = arrows.mapNotNull { a ->
    val f = Squares.parse(a.from)
    val t = Squares.parse(a.to)
    if (f < 0 || t < 0) null else BoardArrow(f, t, color)
}

@Composable
fun StrategyScreen(topics: List<StrategyTopic>, onOpen: (id: String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val grouped = remember(topics) { topics.groupBy { it.category } }
    Column(modifier.fillMaxSize().screenInsets()) {
        ScreenHeader("전략 가이드", onBack, subtitle = "${topics.size}가지 핵심 원리를 그림으로 배워요")
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + bottomInset()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.forEach { (category, list) ->
                item(key = category) {
                    Column {
                        SectionHeader(category)
                        AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                            list.forEach { topic ->
                                ListRow(topic.title, { onOpen(topic.id) }, subtitle = topic.summary) {
                                    if (topic.minLevel > 1) PillTag("Lv.${topic.minLevel}+")
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
fun StrategyDetailScreen(topic: StrategyTopic, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val position = remember(topic.id) { topic.examplePosition() }
    val highlights = remember(topic.id, c.boardArrow) {
        topic.highlights.map { Squares.parse(it) }.filter { it >= 0 }.associateWith { c.boardArrow.copy(alpha = 0.35f) }
    }
    val arrows = remember(topic.id, c.boardArrow) { topic.boardArrows(c.boardArrow) }
    Column(modifier.fillMaxSize().screenInsets()) {
        ScreenHeader(topic.title, onBack, subtitle = topic.category)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = bottomInset()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                if (topic.minLevel > 1) {
                    PillTag("Lv.${topic.minLevel} 상대부터 중요해요")
                    Spacer(Modifier.height(10.dp))
                }
                Text(topic.summary, style = MaterialTheme.typography.bodyLarge)
            }
            if (position != null) {
                Column {
                    ChessBoard(
                        position,
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        highlights = highlights,
                        arrows = arrows,
                        checkSquare = if (position.isInCheck()) position.kingSquare(position.sideToMove) else null,
                    )
                    if (topic.exampleCaption != null) {
                        Row(Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp)) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(20.dp), tint = c.ink)
                            Spacer(Modifier.width(10.dp))
                            Text(topic.exampleCaption, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("핵심 포인트")
                topic.points.forEachIndexed { i, point ->
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.size(28.dp).background(c.soft, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge.tabular(), color = c.ink)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(point, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
