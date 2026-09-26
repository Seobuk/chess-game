package com.seobuk.chess.ui

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.seobuk.chess.ai.AiLevels
import com.seobuk.chess.core.Side
import com.seobuk.chess.data.ProgressStore
import com.seobuk.chess.learn.OpeningBook
import com.seobuk.chess.learn.StrategyGuide
import com.seobuk.chess.learn.StrategyTopic
import com.seobuk.chess.ui.game.GameScreen
import com.seobuk.chess.ui.game.GameViewModel
import com.seobuk.chess.ui.screens.HomeScreen
import com.seobuk.chess.ui.screens.LevelSelectScreen
import com.seobuk.chess.ui.screens.OpeningDetailScreen
import com.seobuk.chess.ui.screens.OpeningsScreen
import com.seobuk.chess.ui.screens.SideChoice
import com.seobuk.chess.ui.screens.StatsScreen
import com.seobuk.chess.ui.screens.StatsUi
import com.seobuk.chess.ui.screens.StrategyDetailScreen
import com.seobuk.chess.ui.screens.StrategyScreen
import com.seobuk.chess.ui.theme.AppThemes
import com.seobuk.chess.ui.theme.ChessTheme
import com.seobuk.chess.ui.theme.LocalAppColors
import com.seobuk.chess.ui.theme.LocalReducedMotion
import java.io.Serializable
import java.time.LocalDate

/** Serializable so the back stack survives process death (see [AppViewModel]). */
sealed interface Screen : Serializable {
    data object Home : Screen
    data class LevelSelect(val practiceOpeningId: String? = null) : Screen
    /** [resumeUci]: moves after the practice line of an unfinished game, set only when saving the stack. */
    data class Game(
        val level: Int,
        val side: SideChoice,
        val startMovesUci: List<String> = emptyList(),
        val resumeUci: List<String>? = null,
    ) : Screen
    data object Openings : Screen
    data class OpeningDetail(val id: String) : Screen
    data object Strategy : Screen
    data class StrategyDetail(val id: String) : Screen
    data object Stats : Screen
}

class Entry(val id: Int, val screen: Screen) : Serializable

/**
 * Activity-scoped: the back stack, the progress store and one ViewModelStore per entry, so a
 * screen's ViewModel (the game) survives rotation and is cleared exactly when its entry is popped.
 * The stack, with each unfinished game's moves, is also saved for process death.
 */
class AppViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    val store = ProgressStore(app)

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    val stack = mutableStateListOf<Entry>().apply {
        addAll(saved.get<Bundle>(NAV)?.getSerializable(NAV) as? ArrayList<Entry> ?: listOf(Entry(0, Screen.Home)))
    }
    private var nextId = stack.maxOf { it.id } + 1
    private val stores = HashMap<Int, ViewModelStore>()
    val games = HashMap<Int, GameViewModel>()

    init {
        saved.setSavedStateProvider(NAV) { Bundle().apply { putSerializable(NAV, ArrayList(stack.map(::restorable))) } }
    }

    private fun restorable(e: Entry): Entry {
        val s = e.screen as? Screen.Game ?: return e
        val vm = games[e.id] ?: return e
        val moves = vm.unfinishedMoves() ?: return Entry(e.id, s.copy(resumeUci = null))
        val side = if (vm.playerSide == Side.WHITE) SideChoice.WHITE else SideChoice.BLACK
        return Entry(e.id, s.copy(side = side, resumeUci = moves))
    }

    fun push(screen: Screen) {
        if (stack.last().screen != screen) stack += Entry(nextId++, screen) // ignores double taps
    }

    /** Pops until [match] is on top (never below the root); returns the dropped entry ids. */
    fun popTo(match: (Entry) -> Boolean): List<Int> = buildList {
        while (stack.size > 1 && !match(stack.last())) add(drop(stack.removeAt(stack.lastIndex)))
    }

    fun storeFor(id: Int): ViewModelStore = stores.getOrPut(id) { ViewModelStore() }

    private fun drop(e: Entry): Int {
        games.remove(e.id)
        stores.remove(e.id)?.clear()
        return e.id
    }

    override fun onCleared() = stores.values.forEach { it.clear() }

    private companion object {
        const val NAV = "nav"
    }
}

/** The strategy example shown on the home screen: one topic with a board position per day of the year. */
private fun dailyTip(): StrategyTopic =
    StrategyGuide.topics.filter { it.exampleFen != null }.let { it[LocalDate.now().dayOfYear % it.size] }

@Composable
fun ChessApp() {
    val app: AppViewModel = viewModel()
    val store = app.store
    val holder = rememberSaveableStateHolder()
    fun popTo(match: (Entry) -> Boolean) = app.popTo(match).forEach(holder::removeState)
    val back: () -> Unit = { val below = app.stack.getOrNull(app.stack.size - 2); popTo { it === below } }
    BackHandler(enabled = app.stack.size > 1, onBack = back)

    ChessTheme(store.themeId) {
        val reduced = LocalReducedMotion.current
        val slide = with(LocalDensity.current) { 16.dp.roundToPx() }
        Box(Modifier.fillMaxSize().background(LocalAppColors.current.bg)) {
            AnimatedContent(
                targetState = app.stack.last(),
                transitionSpec = {
                    val dir = if (targetState.id > initialState.id) 1 else -1
                    if (reduced) EnterTransition.None togetherWith ExitTransition.None
                    else (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { dir * slide } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -dir * slide } + fadeOut(tween(150)))
                },
                contentKey = { it.id },
                label = "nav",
            ) { entry ->
                holder.SaveableStateProvider(entry.id) {
                    // Only the top entry may navigate: taps on a screen that is animating out are ignored.
                    val isTop = { app.stack.last() === entry }
                    val onBack = { if (isTop()) back() }
                    val go = { screen: Screen -> if (isTop()) app.push(screen) }
                    when (val s = entry.screen) {
                        Screen.Home -> {
                            val tip = remember { dailyTip() }
                            HomeScreen(
                                rating = store.rating,
                                recommendedLevel = store.recommendedLevel(),
                                tip = tip,
                                onPlay = { go(Screen.LevelSelect()) },
                                onTip = { go(Screen.StrategyDetail(tip.id)) },
                                onOpenings = { go(Screen.Openings) },
                                onStrategy = { go(Screen.Strategy) },
                                onStats = { go(Screen.Stats) },
                            )
                        }
                        is Screen.LevelSelect -> {
                            val practice = s.practiceOpeningId?.let(OpeningBook::byId)
                            LevelSelectScreen(
                                levels = AiLevels.all,
                                records = store.levelRecords,
                                recommendedLevel = store.recommendedLevel().level,
                                onStart = { level, side -> go(Screen.Game(level, side, practice?.steps?.map { it.uci }.orEmpty())) },
                                onBack = onBack,
                                subtitle = practice?.let { "${it.nameKo} 수순부터 이어서 연습해요" },
                            )
                        }
                        is Screen.Game -> {
                            val vm = remember(entry.id) {
                                val owner = object : ViewModelStoreOwner { override val viewModelStore = app.storeFor(entry.id) }
                                val factory = viewModelFactory {
                                    initializer { GameViewModel(AiLevels.get(s.level), s.side, s.startMovesUci, store, s.resumeUci) }
                                }
                                ViewModelProvider.create(owner, factory)[GameViewModel::class].also { app.games[entry.id] = it }
                            }
                            GameScreen(
                                vm, store.rating,
                                active = isTop(),
                                onLeave = onBack,
                                onLevels = {
                                    if (isTop()) {
                                        popTo { it.screen is Screen.LevelSelect }
                                        if (app.stack.last().screen !is Screen.LevelSelect) app.push(Screen.LevelSelect())
                                    }
                                },
                                onHome = { if (isTop()) popTo { it.screen == Screen.Home } },
                            )
                        }
                        Screen.Openings -> OpeningsScreen(OpeningBook.all, onOpen = { go(Screen.OpeningDetail(it)) }, onBack = onBack)
                        is Screen.OpeningDetail -> OpeningBook.byId(s.id)?.let {
                            OpeningDetailScreen(it, onPractice = { o -> go(Screen.LevelSelect(o.id)) }, onBack = onBack)
                        }
                        Screen.Strategy -> StrategyScreen(StrategyGuide.topics, onOpen = { go(Screen.StrategyDetail(it)) }, onBack = onBack)
                        is Screen.StrategyDetail -> StrategyGuide.byId(s.id)?.let { StrategyDetailScreen(it, onBack = onBack) }
                        Screen.Stats -> StatsScreen(
                            StatsUi(
                                rating = store.rating,
                                ratingHistory = store.ratingHistory,
                                levelRecords = store.levelRecords,
                                qualityTotals = store.qualityTotals,
                                hintsUsed = store.hintsUsed,
                                recommendedLevel = store.recommendedLevel(),
                            ),
                            AppThemes.all,
                            selectedThemeId = store.themeId,
                            onSelectTheme = { store.themeId = it },
                            onBack = onBack,
                        )
                    }
                }
            }
        }
    }
}
