package com.kidssudoku.puzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kidssudoku.puzzle.ui.clear.ClearScreen
import com.kidssudoku.puzzle.ui.difficulty.DifficultyScreen
import com.kidssudoku.puzzle.ui.game.GameScreen
import com.kidssudoku.puzzle.ui.game.GameViewModel
import com.kidssudoku.puzzle.ui.home.HomeScreen
import com.kidssudoku.puzzle.ui.puzzlelist.PuzzleListScreen
import com.kidssudoku.puzzle.ui.puzzlelist.PuzzleListViewModel
import com.kidssudoku.puzzle.ui.settings.SettingsScreen
import com.kidssudoku.puzzle.ui.settings.SettingsViewModel
import com.kidssudoku.puzzle.ui.theme.SudokuTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SudokuApplication

        // Seed puzzles on first launch (runs in background)
        lifecycleScope.launch {
            app.puzzleSeeder.seedIfNeeded()
        }

        setContent {
            SudokuTheme {
                SudokuApp(
                    app = app,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
        }
    }
}

@Composable
fun SudokuApp(
    app: SudokuApplication,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(HomeRoute)

    NavDisplay(
        backStack     = backStack,
        onBack        = { backStack.removeLastOrNull() },
        modifier      = modifier,
        entryProvider = entryProvider {

            // ── Home ─────────────────────────────────────────────────────
            entry<HomeRoute> {
                HomeScreen(
                    onGridSizeSelected = { gs ->
                        backStack.add(DifficultyRoute(gridSize = gs))
                    },
                    onSettingsClick = { backStack.add(SettingsRoute) }
                )
            }

            // ── Difficulty ───────────────────────────────────────────────
            entry<DifficultyRoute> { route ->
                DifficultyScreen(
                    gridSize              = route.gridSize,
                    onDifficultySelected  = { diff ->
                        backStack.add(PuzzleListRoute(route.gridSize, diff))
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            // ── Puzzle List ───────────────────────────────────────────────
            entry<PuzzleListRoute> { route ->
                val vm: PuzzleListViewModel = viewModel(
                    factory = PuzzleListViewModel.Factory(app.repository)
                )
                PuzzleListScreen(
                    gridSize                = route.gridSize,
                    difficulty              = route.difficulty,
                    onPuzzleSelected        = { puzzleId ->
                        backStack.add(GameRoute(puzzleId))
                    },
                    onClearedPuzzleSelected = { puzzleId, stars, elapsed ->
                        backStack.add(
                            ClearRoute(
                                puzzleId       = puzzleId,
                                stars          = stars,
                                elapsedSeconds = elapsed,
                                gridSize       = route.gridSize,
                                difficulty     = route.difficulty
                            )
                        )
                    },
                    onBack                  = { backStack.removeLastOrNull() },
                    viewModel               = vm
                )
            }

            // ── Game ─────────────────────────────────────────────────────
            entry<GameRoute> { route ->
                val vm: GameViewModel = viewModel(
                    key = "game_${route.puzzleId}",
                    factory = GameViewModel.Factory(app.repository, app.gamePreferences, app.soundManager)
                )
                GameScreen(
                    puzzleId = route.puzzleId,
                    onBack   = { backStack.removeLastOrNull() },
                    onClear  = { puzzleId, stars, elapsed ->
                        val listRoute = backStack
                            .filterIsInstance<PuzzleListRoute>()
                            .lastOrNull()
                        backStack.add(
                            ClearRoute(
                                puzzleId       = puzzleId,
                                stars          = stars,
                                elapsedSeconds = elapsed,
                                gridSize       = listRoute?.gridSize ?: 9,
                                difficulty     = listRoute?.difficulty ?: "EASY"
                            )
                        )
                    },
                    viewModel = vm
                )
            }

            // ── Clear ────────────────────────────────────────────────────
            entry<ClearRoute> { route ->
                val coroutineScope = rememberCoroutineScope()
                var hasNextPuzzle by remember { mutableStateOf(true) }

                LaunchedEffect(route.puzzleId) {
                    val puzzles = app.repository
                        .getPuzzles(route.gridSize, route.difficulty)
                        .first()
                    val current = puzzles.find { it.id == route.puzzleId }
                    hasNextPuzzle = (current != null && puzzles.any { it.orderIndex == current.orderIndex + 1 })
                }

                ClearScreen(
                    stars          = route.stars,
                    elapsedSeconds = route.elapsedSeconds,
                    hasNextPuzzle  = hasNextPuzzle,
                    onReplay       = {
                        coroutineScope.launch {
                            app.repository.resetPuzzlePlayState(route.puzzleId)
                            backStack.removeLastOrNull() // pop ClearRoute
                            if (backStack.lastOrNull() is GameRoute) {
                                backStack.removeLastOrNull() // pop previous GameRoute
                            }
                            backStack.add(GameRoute(route.puzzleId))
                        }
                    },
                    onNextPuzzle   = {
                        coroutineScope.launch {
                            val puzzles = app.repository
                                .getPuzzles(route.gridSize, route.difficulty)
                                .first()
                            val current = puzzles.find { it.id == route.puzzleId }
                            val next = if (current != null) {
                                puzzles.find { it.orderIndex == current.orderIndex + 1 }
                            } else null

                            backStack.removeLastOrNull() // pop ClearRoute
                            if (backStack.lastOrNull() is GameRoute) {
                                backStack.removeLastOrNull() // pop previous GameRoute
                            }
                            if (next != null) {
                                backStack.add(GameRoute(next.id))
                            }
                        }
                    },
                    onBackToList = {
                        backStack.removeLastOrNull() // pop ClearRoute
                        if (backStack.lastOrNull() is GameRoute) {
                            backStack.removeLastOrNull() // pop GameRoute
                        }
                    }
                )
            }

            // ── Settings ─────────────────────────────────────────────────
            entry<SettingsRoute> {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(app.repository, app.gamePreferences)
                )
                SettingsScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    viewModel = vm
                )
            }

            // Legacy main entry
            entry<Main> {
                HomeScreen(
                    onGridSizeSelected = { gs -> backStack.add(DifficultyRoute(gs)) },
                    onSettingsClick    = { backStack.add(SettingsRoute) }
                )
            }
        }
    )
}
