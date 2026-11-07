package com.kidssudoku.puzzle

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute    : NavKey
@Serializable data object SettingsRoute : NavKey

@Serializable data class DifficultyRoute(val gridSize: Int) : NavKey

@Serializable data class PuzzleListRoute(
    val gridSize: Int,
    val difficulty: String
) : NavKey

@Serializable data class GameRoute(val puzzleId: Int) : NavKey

@Serializable data class ClearRoute(
    val puzzleId: Int,
    val stars: Int,
    val elapsedSeconds: Int,
    val gridSize: Int,
    val difficulty: String
) : NavKey

// Legacy – kept so existing code that references Main still compiles
@Serializable data object Main : NavKey
