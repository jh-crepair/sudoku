package com.kidssudoku.puzzle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import com.kidssudoku.puzzle.data.preference.GamePreferences
import com.kidssudoku.puzzle.data.repository.PuzzleRepository
import com.kidssudoku.puzzle.sound.SoundManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: PuzzleRepository,
    private val preferences: GamePreferences,
    private val soundManager: SoundManager
) : ViewModel() {

    val onlyAvailableNumbers: StateFlow<Boolean> = preferences.onlyAvailableNumbers
    val soundEnabled: StateFlow<Boolean> = preferences.soundEnabled

    fun setOnlyAvailableNumbers(enabled: Boolean) {
        preferences.setOnlyAvailableNumbers(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        preferences.setSoundEnabled(enabled)
        soundManager.setMuted(!enabled)
    }

    fun resetProgress(gridSize: Int, difficulty: String) {
        viewModelScope.launch {
            repository.resetProgress(gridSize, difficulty)
            // Re-unlock the first puzzle
            val puzzles     = repository.getPuzzles(gridSize, difficulty).first()
            val firstPuzzle = puzzles.minByOrNull { it.orderIndex } ?: return@launch
            repository.saveProgress(
                Progress(
                    puzzleId = firstPuzzle.id,
                    status   = PuzzleStatus.UNLOCKED.name
                )
            )
        }
    }

    fun resetGridSizeProgress(gridSize: Int) {
        viewModelScope.launch {
            repository.resetProgressByGridSize(gridSize)
            // Re-unlock the first puzzle for each difficulty in this gridSize
            val diffs = listOf("EASY", "MEDIUM", "HARD")
            for (diff in diffs) {
                val puzzles = repository.getPuzzles(gridSize, diff).first()
                val firstPuzzle = puzzles.minByOrNull { it.orderIndex }
                if (firstPuzzle != null) {
                    repository.saveProgress(
                        Progress(
                            puzzleId = firstPuzzle.id,
                            status   = PuzzleStatus.UNLOCKED.name
                        )
                    )
                }
            }
        }
    }

    class Factory(
        private val repository: PuzzleRepository,
        private val preferences: GamePreferences,
        private val soundManager: SoundManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, preferences, soundManager) as T
        }
    }
}
