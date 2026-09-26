package com.seobuk.chess.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.seobuk.chess.ai.AiLevel
import com.seobuk.chess.ai.MoveQuality
import com.seobuk.chess.ui.components.DeltaPill
import com.seobuk.chess.ui.components.PrimaryButton
import com.seobuk.chess.ui.components.QualityChip
import com.seobuk.chess.ui.components.SectionHeader
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import com.seobuk.chess.ui.theme.floatShadow
import com.seobuk.chess.ui.theme.tabular

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Dim layer under the promotion picker and the result sheet; hidden from TalkBack (each overlay has its own buttons). */
@Composable
internal fun Scrim(onTap: () -> Unit = {}) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .pointerInput(onTap) { detectTapGestures { onTap() } }
            .clearAndSetSemantics { },
    )
}

/**
 * Result sheet. ponytail: drawn in the game's own window rather than as a ModalBottomSheet, so the win confetti
 * stays on top of it. A tap on the scrim hides it like [onViewBoard]; the action bar's 결과 button brings it back.
 */
@Composable
internal fun ResultOverlay(
    result: GameResult,
    level: AiLevel,
    onRematch: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
    onViewBoard: () -> Unit,
) {
    val c = LocalAppColors.current
    val title = when (result.outcome) {
        Outcome.WIN -> "승리했어요"
        Outcome.DRAW -> "무승부예요"
        Outcome.LOSS -> "아쉽게 졌어요"
    }
    Box(Modifier.fillMaxSize()) {
        Scrim(onViewBoard)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(top = 40.dp) // a strip of scrim stays visible above the sheet
                .fillMaxWidth()
                .floatShadow(SheetShape, c)
                .background(c.surface, SheetShape)
                .pointerInput(Unit) { detectTapGestures { } } // taps on the sheet don't reach the scrim
                .semantics { paneTitle = title } // TalkBack announces the sheet
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${level.nameKo} Lv.${level.level} · ${result.reason}" + if (result.rated) "" else " (연습)",
                style = MaterialTheme.typography.bodyMedium,
                color = c.muted,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                // line border on surface, like every other panel: the semantic delta pill keeps >= 4.5:1 on it
                Modifier.fillMaxWidth().border(1.dp, c.line, MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccuracyRing(result.accuracy)
                Spacer(Modifier.width(20.dp))
                RatingChange(result.ratingBefore, result.ratingAfter, result.rated, Modifier.weight(1f))
            }
            QualityCounts(result.qualityCounts)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("다시 하기", onRematch, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onLevels, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("레벨 선택", maxLines = 1) }
                OutlinedButton(onHome, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("홈", maxLines = 1) }
            }
            TextButton(onViewBoard, Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp).heightIn(min = 48.dp)) {
                Text("판 다시 보기", color = c.muted)
            }
        }
    }
}

@Composable
private fun AccuracyRing(accuracy: Int?) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(accuracy) {
        val target = (accuracy ?: 0) / 100f
        if (reduced) sweep.snapTo(target) else sweep.animateTo(target, tween(700, delayMillis = 150, easing = FastOutSlowInEasing))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sw = 8.dp.toPx()
                val tl = Offset(sw / 2, sw / 2)
                val sz = Size(size.width - sw, size.height - sw)
                drawArc(c.line, 0f, 360f, false, tl, sz, style = Stroke(sw))
                drawArc(c.primary, -90f, 360f * sweep.value, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Text(if (accuracy == null) "-" else "$accuracy%", style = MaterialTheme.typography.titleLarge.tabular())
        }
        Spacer(Modifier.height(6.dp))
        Text("정확도", style = MaterialTheme.typography.labelMedium, color = c.muted)
    }
}

@Composable
private fun RatingChange(before: Int, after: Int, rated: Boolean, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(modifier) {
        Text("레이팅", style = MaterialTheme.typography.labelMedium, color = c.muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$after", style = MaterialTheme.typography.displaySmall.tabular())
            Spacer(Modifier.width(8.dp))
            DeltaPill(after - before)
        }
        Text(
            if (rated) "$before → $after" else "연습 대국은 반영되지 않아요",
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = c.muted,
        )
    }
}

/** The player's reviewed moves by quality, best first; only qualities that occurred. */
@Composable
private fun QualityCounts(counts: Map<MoveQuality, Int>) {
    val shown = MoveQuality.entries.filter { (counts[it] ?: 0) > 0 }
    if (shown.isEmpty()) return
    SectionHeader("내 수 분석", Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.forEach { q ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                QualityChip(q)
                Text("${counts[q]}", Modifier.padding(start = 6.dp, end = 4.dp), style = MaterialTheme.typography.titleSmall.tabular())
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
        title = { Text(title) },
        text = { Text(message) },
    )
}
