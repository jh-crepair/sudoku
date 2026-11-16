package com.jh.app.cherry.puzzle.sudoku.ui.puzzlelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jh.app.cherry.puzzle.sudoku.data.model.Progress
import com.jh.app.cherry.puzzle.sudoku.data.model.Puzzle
import com.jh.app.cherry.puzzle.sudoku.data.repository.PuzzleRepository
import kotlinx.coroutines.flow.*

class PuzzleListViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _params = MutableStateFlow(Pair(4, "EASY"))

    val puzzles: StateFlow<List<Puzzle>> = _params
        .flatMapLatest { (gs, diff) -> repository.getPuzzles(gs, diff) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressMap: StateFlow<Map<Int, Progress>> = _params
        .flatMapLatest { (gs, diff) ->
            repository.getProgressList(gs, diff).map { list ->
                list.associateBy { it.puzzleId }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun load(gridSize: Int, difficulty: String) {
        _params.value = Pair(gridSize, difficulty)
    }

    class Factory(private val repository: PuzzleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PuzzleListViewModel(repository) as T
        }
    }
}
