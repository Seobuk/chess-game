package com.seobuk.chess.ui.game

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seobuk.chess.ai.AiLevel
import com.seobuk.chess.ai.AiPlayer
import com.seobuk.chess.ai.Coach
import com.seobuk.chess.ai.Ko
import com.seobuk.chess.ai.MATE
import com.seobuk.chess.ai.MoveQuality
import com.seobuk.chess.core.Game
import com.seobuk.chess.core.GameStatus
import com.seobuk.chess.core.Move
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Side
import com.seobuk.chess.data.ProgressStore
import com.seobuk.chess.learn.Opening
import com.seobuk.chess.learn.OpeningBook
import com.seobuk.chess.ui.components.figurine
import com.seobuk.chess.ui.screens.SideChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class Outcome { WIN, DRAW, LOSS }

data class GameResult(
    val outcome: Outcome,
    val reason: String,
    val accuracy: Int?,                     // null when no move of the player was reviewed
    val qualityCounts: Map<MoveQuality, Int>,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val rated: Boolean = true,              // false: practice from a trap line, nothing was recorded
)

data class CoachLine(
    val title: String,
    val text: String,
    val hint: Boolean = false, // hint card: lightbulb icon instead of the coach icon
    val quality: MoveQuality? = null,
    val thinking: Boolean = false,
)

/** What the AI just played: figurine SAN + neutral description. */
data class AiLine(val san: String, val text: String)

/** Immutable screen state. [position] is a snapshot, never the live game position. */
data class GameUi(
    val position: Position,
    val playerSide: Side,
    val flipped: Boolean,
    val coach: CoachLine,
    val lastMove: Move? = null,
    val sanMoves: List<String> = emptyList(),
    val qualities: Map<Int, MoveQuality> = emptyMap(),   // ply -> quality (player's moves)
    val selected: Int? = null,
    val targets: Set<Int> = emptySet(),
    val promotion: Move? = null,                          // from/to waiting for the piece choice
    val aiThinking: Boolean = false,
    val aiLine: AiLine? = null,
    val badge: Pair<Int, MoveQuality>? = null,            // square -> quality of the player's last move
    val hintMove: Move? = null,
    val hintLoading: Boolean = false,
    val evalCp: Int = 0,                                  // WHITE POV, may be a mate score
    val opening: Opening? = null,
    val status: GameStatus = GameStatus.ONGOING,
    val canUndo: Boolean = false,
    val result: GameResult? = null,
    val startPly: Int = 0,                                // plies replayed from a practice line
) {
    val isPlayerTurn: Boolean
        get() = result == null && status == GameStatus.ONGOING && position.sideToMove == playerSide && !aiThinking
    val inProgress: Boolean get() = result == null && sanMoves.size > startPly
}

/**
 * Replays a practice line. A line that ends the game (the trap lines end in mate) is rewound to the loser's
 * last real choice, so the trap can still be sprung or avoided instead of starting at mate in one.
 */
internal fun practiceGame(startMoves: List<String>): Game {
    val game = Game()
    for (uci in startMoves) if (!game.play(Move.fromUci(uci) ?: break)) break
    if (game.isOver) {
        val loser = game.position.sideToMove
        while (game.undo()) if (game.position.sideToMove == loser && game.position.legalMoves().size > 1) break
    }
    return game
}

/**
 * One game against [level]. All engine work runs on Dispatchers.Default and is cancelled on undo,
 * new game and onCleared; every background result is dropped unless [gen] is unchanged.
 * The live [game] is only touched on the main thread.
 */
class GameViewModel(
    val level: AiLevel,
    private val sideChoice: SideChoice,
    private val startMoves: List<String>,
    private val store: ProgressStore,
    resumeUci: List<String>? = null, // moves after the practice line, when rebuilding after process death
) : ViewModel() {
    var ui by mutableStateOf(initialUi())
        private set

    private lateinit var game: Game
    private lateinit var ai: AiPlayer
    private val aiLock = Mutex() // AiPlayer isn't thread-safe; a cancelled search may still be unwinding
    private val cpLoss = HashMap<Int, Int>()
    private val practice = OpeningBook.detect(startMoves)
    private val rated = practice?.category != OpeningBook.TRAPS // trap setups would farm (or cost) rating
    private var side = Side.WHITE
    private var startPly = 0
    private var gen = 0
    private var recorded = false
    private var turnJob: Job? = null
    private var hintJob: Job? = null
    private var evalJob: Job? = null

    /** The side actually played (resolves [SideChoice.RANDOM]). */
    val playerSide: Side get() = side

    init { start(resumeUci) }

    private fun initialUi() = GameUi(Position.start(), Side.WHITE, false, CoachLine("코치", ""))

    fun newGame() = start(null)

    /** Moves played after the practice line, if the game is unfinished; for rebuilding it after process death. */
    fun unfinishedMoves(): List<String>? =
        if (ui.result == null && game.status == GameStatus.ONGOING && game.moves.size > startPly) uciMoves().drop(startPly) else null

    private fun start(resume: List<String>?) {
        cancelJobs()
        gen++
        recorded = false
        cpLoss.clear()
        side = when (sideChoice) {
            SideChoice.WHITE -> Side.WHITE
            SideChoice.BLACK -> Side.BLACK
            SideChoice.RANDOM -> if (Random.nextBoolean()) Side.WHITE else Side.BLACK
        }
        ai = AiPlayer(level)
        game = practiceGame(startMoves)
        startPly = game.moves.size
        for (uci in resume.orEmpty()) if (!game.play(Move.fromUci(uci) ?: break)) break
        val greeting = when {
            !resume.isNullOrEmpty() -> "대국을 이어서 해요. 차분히 다음 수를 생각해 봐요."
            practice != null -> "${practice.nameKo} 수순까지 진행했어요. 여기서부터 직접 이어서 둬 볼까요? 막히면 힌트를 눌러요." +
                if (rated) "" else " 함정 연습이라 레이팅에는 반영되지 않아요."
            side == Side.WHITE -> "안녕하세요. 저는 코치예요. 수를 두면 바로 평가해 드릴게요. 먼저 두세요."
            else -> "안녕하세요. 이번엔 흑이에요. ${level.nameKo}의 첫 수를 보고 차분히 대응해 봐요."
        }
        ui = refreshed(GameUi(game.position.copy(), side, side == Side.BLACK, CoachLine("코치", greeting), aiLine = lastAiLine(), startPly = startPly))
        if (game.moves.isNotEmpty()) updateEval()
        if (game.position.sideToMove != side) {
            val g = gen
            val pos = game.position.copy()
            val played = uciMoves()
            turnJob = viewModelScope.launch {
                val move = async { computeAiMove(pos, played) }
                delay(350)
                aiReply(move, g)
            }
        }
    }

    fun onSquareTap(sq: Int) {
        val u = ui
        if (!u.isPlayerTurn || u.promotion != null || turnJob?.isActive == true) return
        val sel = u.selected
        if (sel != null && sq in u.targets) {
            val candidates = game.position.legalMoves().filter { it.from == sel && it.to == sq }
            if (candidates.any { it.promotion != null }) ui = u.copy(promotion = Move(sel, sq))
            else candidates.firstOrNull()?.let(::playerMove)
            return
        }
        val piece = game.position.pieceAt(sq)
        ui = if (piece != null && piece.side == u.playerSide && sq != sel) {
            u.copy(selected = sq, targets = game.position.legalMoves().filter { it.from == sq }.map { it.to }.toSet())
        } else {
            u.copy(selected = null, targets = emptySet())
        }
    }

    /** [type] null = the player cancelled the promotion. */
    fun onPromotion(type: PieceType?) {
        val p = ui.promotion ?: return
        if (type == null) ui = ui.copy(promotion = null, selected = null, targets = emptySet())
        else playerMove(Move(p.from, p.to, type))
    }

    fun flip() { ui = ui.copy(flipped = !ui.flipped) }

    fun hint() {
        val u = ui
        if (!u.isPlayerTurn || u.hintLoading || u.hintMove != null || turnJob?.isActive == true) return
        val g = gen
        val pos = game.position.copy()
        ui = u.copy(hintLoading = true, selected = null, targets = emptySet(), coach = CoachLine("힌트", "", hint = true, thinking = true))
        hintJob = viewModelScope.launch {
            val h = background { Coach.hint(pos, 800) { !isActive } }
            if (g != gen) return@launch
            ui = if (h == null) {
                ui.copy(hintLoading = false, coach = CoachLine("힌트", "지금은 힌트를 찾지 못했어요. 잠시 후 다시 눌러 주세요.", hint = true))
            } else {
                store.addHint()
                ui.copy(
                    hintLoading = false,
                    hintMove = h.move,
                    evalCp = h.evalCp,
                    coach = CoachLine("힌트 · ${figurine(h.san)}", h.explanation, hint = true),
                )
            }
        }
    }

    fun undo() {
        if (!ui.canUndo) return
        cancelJobs()
        gen++
        val qualities = ui.qualities.toMutableMap()
        while (game.moves.size > startPly) {
            val ply = game.moves.size - 1
            game.undo()
            qualities.remove(ply)
            cpLoss.remove(ply)
            if (isPlayerPly(ply)) break
        }
        ui = refreshed(ui).copy(
            qualities = qualities,
            aiThinking = false,
            aiLine = lastAiLine(),
            badge = null,
            hintMove = null,
            hintLoading = false,
            coach = CoachLine("코치", "한 수 물렀어요. 이번엔 어떤 수가 좋을지 다시 생각해 봐요."),
        )
        updateEval()
    }

    fun resign() {
        if (ui.result != null || ui.status != GameStatus.ONGOING) return
        cancelJobs()
        gen++
        finish(resigned = true)
    }

    override fun onCleared() = cancelJobs()

    // ---- turn flow ----

    private fun playerMove(move: Move) {
        val before = game.position.copy()
        val playedBefore = uciMoves()
        if (!game.play(move)) return
        hintJob?.cancel()
        evalJob?.cancel()
        val ply = game.moves.size - 1
        val g = ++gen
        ui = refreshed(ui).copy(hintMove = null, hintLoading = false, badge = null, coach = CoachLine("코치", "", thinking = true))
        val pos = game.position.copy()
        val played = uciMoves()
        val over = game.isOver
        turnJob = viewModelScope.launch {
            // The AI starts thinking while the coach reviews; its move is shown after the feedback.
            val reply = if (over) null else async { computeAiMove(pos, played) }
            val fb = background { Coach.review(before, move, playedBefore) { !isActive } }
            if (g != gen) return@launch
            ui = if (fb == null) {
                ui.copy(coach = CoachLine("코치", "이번 수는 분석하지 못했어요. 계속 둬 봐요."))
            } else {
                cpLoss[ply] = fb.cpLoss
                ui.copy(
                    qualities = ui.qualities + (ply to fb.quality),
                    badge = move.to to fb.quality,
                    evalCp = if (over) fb.evalBeforeCp else fb.evalAfterCp, // finish() reads the eval before the last move
                    coach = CoachLine("코치 · ${figurine(fb.san)}", fb.explanation, quality = fb.quality),
                )
            }
            if (reply == null) finish() else aiReply(reply, g)
        }
    }

    /** Shows the thinking indicator for a natural minimum time, then plays the AI's move. */
    private suspend fun aiReply(move: Deferred<Move?>, g: Int) {
        ui = ui.copy(aiThinking = true)
        val start = System.nanoTime()
        val minMs = 450L + Random.nextLong(450)
        val m = move.await() ?: game.position.legalMoves().randomOrNull()
        val waited = (System.nanoTime() - start) / 1_000_000
        if (waited < minMs) delay(minMs - waited)
        if (g != gen) return
        if (m == null || !game.position.isLegal(m)) { ui = ui.copy(aiThinking = false); return }
        val before = game.position.copy()
        val san = before.san(m)
        val text = runCatching { Coach.describeMove(before, m) }.getOrDefault("")
        game.play(m)
        gen++
        val badge = ui.badge?.takeIf { it.first != m.to }
        ui = refreshed(ui).copy(aiThinking = false, aiLine = AiLine(figurine(san), text), badge = badge)
        if (game.isOver) finish() else updateEval()
    }

    private suspend fun computeAiMove(pos: Position, played: List<String>): Move? {
        val player = ai
        return aiLock.withLock { background { player.chooseMove(pos, played) { !isActive } } }
    }

    private fun updateEval() {
        evalJob?.cancel()
        val g = gen
        val pos = game.position.copy()
        evalJob = viewModelScope.launch {
            val e = background { Coach.evaluateWhite(pos, 300) { !isActive } } ?: return@launch
            if (g == gen) ui = ui.copy(evalCp = e)
        }
    }

    private fun finish(resigned: Boolean = false) {
        if (recorded) return
        recorded = true
        val u = ui
        val outcome = when {
            resigned -> Outcome.LOSS
            game.status == GameStatus.CHECKMATE -> if (game.winner == u.playerSide) Outcome.WIN else Outcome.LOSS
            else -> Outcome.DRAW
        }
        val reason = if (resigned) "기권" else statusText(game.status)
        val counts = u.qualities.filterKeys(::isPlayerPly).values.groupingBy { it }.eachCount()
        val losses = cpLoss.filterKeys(::isPlayerPly).values.toList()
        val before = store.rating
        val score = when (outcome) { Outcome.WIN -> 1.0; Outcome.DRAW -> 0.5; Outcome.LOSS -> 0.0 }
        val after = if (rated) store.recordGame(level, score, counts) else before
        val lead = if (u.playerSide == Side.WHITE) u.evalCp else -u.evalCp // player POV, before the final move
        val line = when {
            outcome == Outcome.WIN -> "${Ko.ro(reason)} 이겼어요. 잘 뒀어요."
            resigned -> "기권했어요. 괜찮아요, 다음 판에서 다시 해 봐요."
            outcome == Outcome.LOSS -> "아쉽게 졌어요. 어디서 흐름이 바뀌었는지 기보를 다시 살펴봐요."
            game.status == GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "남은 기물로는 체크메이트할 수 없어서 무승부예요."
            lead > 300 -> "${Ko.ro(reason)} 무승부가 됐어요. 이기고 있던 판이니 다음엔 끝까지 마무리해 봐요."
            lead < -300 -> "${Ko.ro(reason)} 무승부예요. 불리한 판을 끝까지 잘 버텼어요."
            else -> "${Ko.ro(reason)} 무승부예요."
        }
        // Keep the review of the player's last move (e.g. the stalemate that threw away a win) above the result.
        val review = u.coach.takeIf { !resigned && it.quality != null }
        ui = refreshed(u).copy(
            aiThinking = false,
            hintLoading = false,
            hintMove = null,
            evalCp = when {
                game.status == GameStatus.CHECKMATE -> if (game.winner == Side.WHITE) MATE else -MATE
                resigned -> u.evalCp
                else -> 0
            },
            coach = review?.copy(text = "${review.text}\n$line") ?: CoachLine("코치", line),
            result = GameResult(outcome, reason, if (losses.isEmpty()) null else Coach.accuracy(losses), counts, before, after, rated),
        )
    }

    // ---- helpers ----

    private fun refreshed(u: GameUi): GameUi {
        val played = uciMoves()
        return u.copy(
            position = game.position.copy(),
            lastMove = game.moves.lastOrNull(),
            sanMoves = game.sanMoves,
            selected = null,
            targets = emptySet(),
            promotion = null,
            opening = OpeningBook.detect(played),
            status = game.status,
            // Not after a game-ending move either: it would take back a result about to be recorded.
            canUndo = !recorded && game.status == GameStatus.ONGOING && played.indices.any(::isPlayerPly),
        )
    }

    private fun uciMoves() = game.moves.map { it.uci() }

    private fun lastAiLine(): AiLine? {
        val ply = game.moves.size - 1
        if (ply < startPly || isPlayerPly(ply)) return null
        val before = game.positionAt(ply)
        val m = game.moves[ply]
        return AiLine(figurine(before.san(m)), runCatching { Coach.describeMove(before, m) }.getOrDefault(""))
    }

    // Games always start from the standard position, so even plies are White's. Practice-line plies are nobody's.
    private fun isPlayerPly(ply: Int) = ply >= startPly && (ply % 2 == 0) == (side == Side.WHITE)

    private fun cancelJobs() {
        turnJob?.cancel()
        hintJob?.cancel()
        evalJob?.cancel()
    }

    /** Runs blocking engine work off the main thread; any failure other than cancellation becomes null. */
    private suspend fun <T> background(block: CoroutineScope.() -> T): T? = try {
        withContext(Dispatchers.Default) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("GameViewModel", "engine task failed", e)
        null
    }

    companion object {
        fun statusText(status: GameStatus): String = when (status) {
            GameStatus.CHECKMATE -> "체크메이트"
            GameStatus.STALEMATE -> "스테일메이트"
            GameStatus.DRAW_FIFTY_MOVE -> "50수 규칙"
            GameStatus.DRAW_THREEFOLD -> "3회 반복"
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "기물 부족 무승부"
            GameStatus.ONGOING -> ""
        }
    }
}
