package com.kidssudoku.puzzle.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import com.kidssudoku.puzzle.data.repository.PuzzleRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

data class GameUiState(
    val puzzleInfo : String                  = "",
    val grid       : List<List<Int>>         = emptyList(),
    val gridSize   : Int                     = 9,
    val fixedCells : Set<Pair<Int, Int>>     = emptySet(),
    val errorCells : Set<Pair<Int, Int>>     = emptySet(),
    val hintCells  : Set<Pair<Int, Int>>     = emptySet(),
    val solution   : List<Int>               = emptyList()
)

class GameViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState          = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _selectedCell     = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCell: StateFlow<Pair<Int, Int>?> = _selectedCell.asStateFlow()

    private val _showNumberPicker = MutableStateFlow(false)
    val showNumberPicker: StateFlow<Boolean> = _showNumberPicker.asStateFlow()

    private val _isPaused         = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedSeconds   = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _hintsRemaining   = MutableStateFlow(5)
    val hintsRemaining: StateFlow<Int> = _hintsRemaining.asStateFlow()

    private val _isCleared        = MutableStateFlow(false)
    val isCleared: StateFlow<Boolean> = _isCleared.asStateFlow()

    private var currentPuzzle : Puzzle?                        = null
    private var timerJob      : Job?                           = null
    private var currentGrid   : MutableList<MutableList<Int>>  = mutableListOf()
    private var puzzleId      : Int                            = -1
    private var gridSize      : Int                            = 9

    // ── Load ─────────────────────────────────────────────────────────────────

    fun loadPuzzle(id: Int) {
        timerJob?.cancel()
        this.puzzleId = id
        _isCleared.value        = false
        _isPaused.value         = false
        _selectedCell.value     = null
        _showNumberPicker.value = false
        _elapsedSeconds.value   = 0
        _hintsRemaining.value   = 5
        _uiState.value          = GameUiState()

        viewModelScope.launch {
            val puzzle = repository.getPuzzleById(id) ?: return@launch
            currentPuzzle = puzzle
            gridSize      = puzzle.gridSize

            // Parse solution with org.json
            val solutionArray = JSONArray(puzzle.solution)
            val solution = (0 until solutionArray.length()).map { solutionArray.getInt(it) }

            // Parse given cells with org.json
            val givenObject = JSONObject(puzzle.givenCells)
            val fixedCells = mutableSetOf<Pair<Int, Int>>()
            val grid = MutableList(gridSize) { MutableList(gridSize) { 0 } }

            val keys = givenObject.keys()
            while (keys.hasNext()) {
                val indexStr = keys.next()
                val index = indexStr.toInt()
                val value = givenObject.getInt(indexStr)
                val r = index / gridSize
                val c = index % gridSize
                grid[r][c] = value
                fixedCells.add(r to c)
            }

            currentGrid = grid

            // Load saved progress
            val savedProgress = repository.getProgress(id).first()
            if (savedProgress != null && savedProgress.status != PuzzleStatus.CLEARED.name) {
                _hintsRemaining.value = (5 - savedProgress.hintUsedCount).coerceIn(0, 5)
                _elapsedSeconds.value = savedProgress.elapsedSeconds
                // Restore in-progress state
                if (savedProgress.currentState.isNotEmpty()) {
                    runCatching {
                        val saved = JSONArray(savedProgress.currentState)
                        for (idx in 0 until saved.length()) {
                            val r = idx / gridSize
                            val c = idx % gridSize
                            if (r to c !in fixedCells) {
                                currentGrid[r][c] = saved.getInt(idx)
                            }
                        }
                    }
                }
            } else {
                _hintsRemaining.value = 5
                _elapsedSeconds.value = 0
            }

            val diffLabel  = when (puzzle.difficulty) {
                "EASY" -> "초급"; "MEDIUM" -> "중급"; else -> "고급"
            }
            val gridLabel  = when (gridSize) { 4 -> "4단"; 6 -> "6단"; else -> "9단" }

            _uiState.value = GameUiState(
                puzzleInfo = "$gridLabel  $diffLabel  #${puzzle.orderIndex}",
                grid       = currentGrid.map { it.toList() },
                gridSize   = gridSize,
                fixedCells = fixedCells,
                solution   = solution
            )

            startTimer()
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isPaused.value && !_isCleared.value) {
                    _elapsedSeconds.value++
                }
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        if (_isPaused.value) saveProgress()
    }

    // ── Cell interaction ──────────────────────────────────────────────────────

    fun onCellClick(row: Int, col: Int) {
        val state = _uiState.value
        if (row to col in state.fixedCells) {
            _selectedCell.value = null
            _showNumberPicker.value = false
            return
        }
        _selectedCell.value     = row to col
        _showNumberPicker.value = true
    }

    fun dismissNumberPicker() {
        _showNumberPicker.value = false
        _selectedCell.value     = null
    }

    // ── Numbers ───────────────────────────────────────────────────────────────

    fun getAvailableNumbers(row: Int, col: Int): Set<Int> {
        val usedInRow = currentGrid[row].toSet()
        val usedInCol = (0 until gridSize).map { currentGrid[it][col] }.toSet()
        val (br, bc)  = boxDims(gridSize)
        val sr        = (row / br) * br
        val sc        = (col / bc) * bc
        val usedInBox = mutableSetOf<Int>()
        for (r in sr until sr + br) for (c in sc until sc + bc) usedInBox.add(currentGrid[r][c])
        return (1..gridSize).toSet() - usedInRow - usedInCol - usedInBox - setOf(0)
    }

    private fun boxDims(size: Int): Pair<Int, Int> = when (size) {
        4 -> 2 to 2; 6 -> 2 to 3; else -> 3 to 3
    }

    fun inputNumber(row: Int, col: Int, num: Int) {
        val state = _uiState.value
        if (row to col in state.fixedCells) return
        currentGrid[row][col] = num
        _showNumberPicker.value = false
        _selectedCell.value     = null
        refreshState()
        if (isSolved()) onPuzzleSolved()
        else saveProgress()
    }

    fun deleteNumber(row: Int, col: Int) {
        val state = _uiState.value
        if (row to col in state.fixedCells) return
        currentGrid[row][col] = 0
        _showNumberPicker.value = false
        _selectedCell.value     = null
        refreshState()
        saveProgress()
    }

    // ── Hint ──────────────────────────────────────────────────────────────────

    fun useHint() {
        if (_hintsRemaining.value <= 0) return
        val state    = _uiState.value
        val empties  = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (currentGrid[r][c] == 0 && r to c !in state.fixedCells) empties.add(r to c)
            }
        }
        if (empties.isEmpty()) return

        val (hr, hc)  = empties.random()
        val solutionVal = state.solution[hr * gridSize + hc]
        currentGrid[hr][hc] = solutionVal
        _hintsRemaining.value--

        val newHints = state.hintCells + (hr to hc)
        refreshState(extraHintCells = newHints)

        if (isSolved()) onPuzzleSolved()
        else saveProgress()
    }

    // ── Solve check ───────────────────────────────────────────────────────────

    private fun refreshState(extraHintCells: Set<Pair<Int,Int>>? = null) {
        val state = _uiState.value
        val errors = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val v = currentGrid[r][c]
                if (v == 0 || r to c in state.fixedCells) continue
                if (state.solution.isNotEmpty() && state.solution[r * gridSize + c] != v) {
                    errors.add(r to c)
                }
            }
        }
        _uiState.value = state.copy(
            grid       = currentGrid.map { it.toList() },
            errorCells = errors,
            hintCells  = extraHintCells ?: state.hintCells
        )
    }

    private fun isSolved(): Boolean {
        val solution = _uiState.value.solution
        if (solution.isEmpty()) return false
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (currentGrid[r][c] != solution[r * gridSize + c]) return false
            }
        }
        return true
    }

    private fun onPuzzleSolved() {
        timerJob?.cancel()
        _isCleared.value = true
        viewModelScope.launch {
            val stars   = calculateStars()
            val puzzle  = currentPuzzle ?: return@launch
            val existing = repository.getProgress(puzzleId).first()
            repository.saveProgress(
                (existing ?: Progress(puzzleId = puzzleId)).copy(
                    status       = PuzzleStatus.CLEARED.name,
                    hintUsedCount = 5 - _hintsRemaining.value,
                    elapsedSeconds = _elapsedSeconds.value,
                    starRating   = stars,
                    currentState = "",
                    updatedAt    = System.currentTimeMillis()
                )
            )
            // Unlock next puzzle
            val nextPuzzles = repository.getPuzzles(puzzle.gridSize, puzzle.difficulty).first()
            val nextPuzzle  = nextPuzzles.firstOrNull { it.orderIndex == puzzle.orderIndex + 1 }
            if (nextPuzzle != null) {
                val nextProgress = repository.getProgress(nextPuzzle.id).first()
                if (nextProgress == null || nextProgress.status == PuzzleStatus.LOCKED.name) {
                    repository.saveProgress(
                        (nextProgress ?: Progress(puzzleId = nextPuzzle.id)).copy(
                            status = PuzzleStatus.UNLOCKED.name
                        )
                    )
                }
            }
        }
    }

    // ── Stars ─────────────────────────────────────────────────────────────────

    fun calculateStars(): Int {
        val puzzle  = currentPuzzle ?: return 1
        val elapsed = _elapsedSeconds.value
        val (gold, silver) = starThresholds(puzzle.gridSize, puzzle.difficulty)
        return when {
            elapsed <= gold   -> 3
            elapsed <= silver -> 2
            else              -> 1
        }
    }

    private fun starThresholds(size: Int, diff: String): Pair<Int, Int> = when {
        size == 4 && diff == "EASY"   -> 60   to 120
        size == 4 && diff == "MEDIUM" -> 90   to 180
        size == 4 && diff == "HARD"   -> 120  to 240
        size == 6 && diff == "EASY"   -> 120  to 240
        size == 6 && diff == "MEDIUM" -> 180  to 360
        size == 6 && diff == "HARD"   -> 240  to 480
        size == 9 && diff == "EASY"   -> 300  to 600
        size == 9 && diff == "MEDIUM" -> 480  to 960
        else                          -> 720  to 1440
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun saveProgress() {
        viewModelScope.launch {
            val jsonArray = JSONArray()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    jsonArray.put(currentGrid[r][c])
                }
            }
            val existing = repository.getProgress(puzzleId).first()
            repository.saveProgress(
                (existing ?: Progress(puzzleId = puzzleId)).copy(
                    status        = PuzzleStatus.UNLOCKED.name,
                    hintUsedCount = 5 - _hintsRemaining.value,
                    elapsedSeconds = _elapsedSeconds.value,
                    currentState  = jsonArray.toString(),
                    updatedAt     = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        if (!_isCleared.value) saveProgress()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repository: PuzzleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(repository) as T
        }
    }
}
