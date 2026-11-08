package com.kidssudoku.puzzle.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import com.kidssudoku.puzzle.data.preference.GamePreferences
import com.kidssudoku.puzzle.data.repository.PuzzleRepository
import com.kidssudoku.puzzle.sound.SoundEffect
import com.kidssudoku.puzzle.sound.SoundManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

enum class GameOverReason {
    TIME_OVER,
    MISTAKES_EXCEEDED
}

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
    private val repository: PuzzleRepository,
    private val preferences: GamePreferences,
    private val soundManager: SoundManager
) : ViewModel() {

    companion object {
        const val MAX_FAIL_COUNT = 10

        fun getTimeLimit(difficulty: String): Int = when (difficulty) {
            "EASY"   -> 5 * 60   // 초급 5분
            "MEDIUM" -> 8 * 60   // 중급 8분
            else     -> 10 * 60  // 상급 10분
        }
    }

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

    private val _timeLimitSeconds = MutableStateFlow(300)
    val timeLimitSeconds: StateFlow<Int> = _timeLimitSeconds.asStateFlow()

    private val _failCount        = MutableStateFlow(0)
    val failCount: StateFlow<Int> = _failCount.asStateFlow()

    private val _isGameOver       = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _gameOverReason   = MutableStateFlow<GameOverReason?>(null)
    val gameOverReason: StateFlow<GameOverReason?> = _gameOverReason.asStateFlow()

    private val _hintsRemaining   = MutableStateFlow(3)
    val hintsRemaining: StateFlow<Int> = _hintsRemaining.asStateFlow()

    private val _isCleared        = MutableStateFlow(false)
    val isCleared: StateFlow<Boolean> = _isCleared.asStateFlow()

    // 원인이 되는 다른 셀들의 좌표 목록 (물결 효과 대상)
    private val _conflictingSourceCells = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val conflictingSourceCells: StateFlow<Set<Pair<Int, Int>>> = _conflictingSourceCells.asStateFlow()

    // 물결 효과 애니메이션 트리거 키
    private val _conflictTrigger = MutableStateFlow(0L)
    val conflictTrigger: StateFlow<Long> = _conflictTrigger.asStateFlow()

    val onlyAvailableNumbersAllowed: StateFlow<Boolean> = preferences.onlyAvailableNumbers

    private var currentPuzzle : Puzzle?                        = null
    private var timerJob      : Job?                           = null
    private var currentGrid   : MutableList<MutableList<Int>>  = mutableListOf()
    private var puzzleId      : Int                            = -1
    private var gridSize      : Int                            = 9

    // ── Load ─────────────────────────────────────────────────────────────────

    fun loadPuzzle(id: Int) {
        timerJob?.cancel()
        this.puzzleId = id
        _isCleared.value                = false
        _isPaused.value                 = false
        _isGameOver.value               = false
        _gameOverReason.value           = null
        _failCount.value                = 0
        _selectedCell.value             = null
        _showNumberPicker.value         = false
        _elapsedSeconds.value           = 0
        _hintsRemaining.value           = 3
        _conflictingSourceCells.value   = emptySet()
        _conflictTrigger.value          = 0L
        _uiState.value                  = GameUiState()

        viewModelScope.launch {
            val puzzle = repository.getPuzzleById(id) ?: return@launch
            currentPuzzle = puzzle
            gridSize      = puzzle.gridSize
            val maxHints  = getMaxHints(puzzle.difficulty)
            val limitSec  = getTimeLimit(puzzle.difficulty)
            _timeLimitSeconds.value = limitSec

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
                _hintsRemaining.value = (maxHints - savedProgress.hintUsedCount).coerceIn(0, maxHints)
                _elapsedSeconds.value = if (savedProgress.elapsedSeconds >= limitSec) 0 else savedProgress.elapsedSeconds

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
                _hintsRemaining.value = maxHints
                _elapsedSeconds.value = 0
            }

            val diffLabel  = when (puzzle.difficulty) {
                "EASY" -> "초급"; "MEDIUM" -> "중급"; else -> "상급"
            }
            val gridLabel  = when (gridSize) { 4 -> "4단"; 6 -> "6단"; else -> "9단" }

            _uiState.value = GameUiState(
                puzzleInfo = "$gridLabel  $diffLabel  #${puzzle.orderIndex}",
                grid       = currentGrid.map { it.toList() },
                gridSize   = gridSize,
                fixedCells = fixedCells,
                solution   = solution
            )

            // Validate visible conflicts
            refreshState()
            startTimer()
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isPaused.value && !_isCleared.value && !_isGameOver.value) {
                    _elapsedSeconds.value++
                    val limit = _timeLimitSeconds.value
                    if (limit > 0 && _elapsedSeconds.value >= limit) {
                        triggerGameOver(GameOverReason.TIME_OVER)
                        break
                    }
                }
            }
        }
    }

    private fun triggerGameOver(reason: GameOverReason) {
        timerJob?.cancel()
        _isGameOver.value = true
        _gameOverReason.value = reason
        _showNumberPicker.value = false
        _selectedCell.value = null
        soundManager.play(SoundEffect.GAME_OVER)
    }

    fun restartCurrentPuzzle() {
        val puzzle = currentPuzzle ?: return
        val fixed = _uiState.value.fixedCells
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (r to c !in fixed) {
                    currentGrid[r][c] = 0
                }
            }
        }
        _elapsedSeconds.value = 0
        _failCount.value = 0
        _hintsRemaining.value = getMaxHints(puzzle.difficulty)
        _isGameOver.value = false
        _gameOverReason.value = null
        _isPaused.value = false
        _isCleared.value = false
        _conflictingSourceCells.value = emptySet()
        _selectedCell.value = null
        _showNumberPicker.value = false

        viewModelScope.launch {
            repository.resetPuzzlePlayState(puzzleId)
        }
        refreshState()
        startTimer()
    }

    fun clearAllUserInput() {
        if (_isGameOver.value || _isCleared.value) return
        val fixed = _uiState.value.fixedCells
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (r to c !in fixed) {
                    currentGrid[r][c] = 0
                }
            }
        }
        _conflictingSourceCells.value = emptySet()
        _selectedCell.value = null
        _showNumberPicker.value = false
        soundManager.play(SoundEffect.BUTTON_CLICK)
        refreshState()
        saveProgress()
    }

    fun togglePause() {
        if (_isGameOver.value || _isCleared.value) return
        _isPaused.value = !_isPaused.value
        soundManager.play(SoundEffect.BUTTON_CLICK)
        if (_isPaused.value) saveProgress()
    }

    // ── Cell interaction ──────────────────────────────────────────────────────

    fun onCellClick(row: Int, col: Int) {
        if (_isGameOver.value || _isCleared.value || _isPaused.value) return
        val state = _uiState.value
        if (row to col in state.fixedCells) {
            _selectedCell.value = null
            _showNumberPicker.value = false
            return
        }

        // 셀을 클릭할 때에는 물결 효과를 출력하지 않음 (숫자 선택 팝업에서 선택 시에만 출력)
        _conflictingSourceCells.value = emptySet()

        soundManager.play(SoundEffect.CELL_SELECT)
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

    fun getSelectableNumbers(row: Int, col: Int): Set<Int> {
        return if (preferences.onlyAvailableNumbers.value) {
            getAvailableNumbers(row, col)
        } else {
            (1..gridSize).toSet()
        }
    }

    private fun boxDims(size: Int): Pair<Int, Int> = when (size) {
        4 -> 2 to 2; 6 -> 2 to 3; else -> 3 to 3
    }

    /**
     * Finds all currently visible cells that conflict with placing [num] at ([row], [col]).
     * Searches same row, column, and box.
     */
    fun findConflictingCells(row: Int, col: Int, num: Int): Set<Pair<Int, Int>> {
        if (num == 0) return emptySet()
        val sources = mutableSetOf<Pair<Int, Int>>()

        // 1. Same row
        for (c in 0 until gridSize) {
            if (c != col && currentGrid[row][c] == num) {
                sources.add(row to c)
            }
        }

        // 2. Same column
        for (r in 0 until gridSize) {
            if (r != row && currentGrid[r][col] == num) {
                sources.add(r to col)
            }
        }

        // 3. Same box
        val (br, bc) = boxDims(gridSize)
        val sr = (row / br) * br
        val sc = (col / bc) * bc
        for (r in sr until sr + br) {
            for (c in sc until sc + bc) {
                if ((r != row || c != col) && currentGrid[r][c] == num) {
                    sources.add(r to c)
                }
            }
        }
        return sources
    }

    private fun hasConflict(row: Int, col: Int, num: Int): Boolean {
        return findConflictingCells(row, col, num).isNotEmpty()
    }

    fun inputNumber(row: Int, col: Int, num: Int) {
        if (_isGameOver.value || _isCleared.value) return
        val state = _uiState.value
        if (row to col in state.fixedCells) return

        // Check conflicts with currently visible numbers on board
        val conflicts = findConflictingCells(row, col, num)
        if (conflicts.isNotEmpty()) {
            _failCount.value++
            soundManager.play(SoundEffect.ERROR_INPUT)
            // Trigger ripple wave effect on the source conflicting cells!
            _conflictingSourceCells.value = conflicts
            _conflictTrigger.value = System.currentTimeMillis()

            if (_failCount.value >= MAX_FAIL_COUNT) {
                currentGrid[row][col] = num
                _showNumberPicker.value = false
                _selectedCell.value     = null
                refreshState()
                triggerGameOver(GameOverReason.MISTAKES_EXCEEDED)
                return
            }
        } else {
            soundManager.play(SoundEffect.NUMBER_INPUT)
            _conflictingSourceCells.value = emptySet()
        }

        currentGrid[row][col] = num
        _showNumberPicker.value = false
        _selectedCell.value     = null
        refreshState()
        if (isSolved()) {
            soundManager.play(SoundEffect.PUZZLE_CLEAR)
            onPuzzleSolved()
        } else {
            saveProgress()
        }
    }

    fun deleteNumber(row: Int, col: Int) {
        if (_isGameOver.value || _isCleared.value) return
        val state = _uiState.value
        if (row to col in state.fixedCells) return
        soundManager.play(SoundEffect.BUTTON_CLICK)
        currentGrid[row][col] = 0
        _conflictingSourceCells.value = emptySet()
        _showNumberPicker.value = false
        _selectedCell.value     = null
        refreshState()
        saveProgress()
    }

    // ── Hint ──────────────────────────────────────────────────────────────────

    fun useHint() {
        if (_isGameOver.value || _isCleared.value || _isPaused.value) return
        if (_hintsRemaining.value <= 0) return
        val state    = _uiState.value
        val empties  = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (currentGrid[r][c] == 0 && r to c !in state.fixedCells) empties.add(r to c)
            }
        }
        if (empties.isEmpty()) return

        soundManager.play(SoundEffect.NUMBER_INPUT)
        val (hr, hc)  = empties.random()
        val solutionVal = state.solution[hr * gridSize + hc]
        currentGrid[hr][hc] = solutionVal
        _hintsRemaining.value--
        _conflictingSourceCells.value = emptySet()

        val newHints = state.hintCells + (hr to hc)
        refreshState(extraHintCells = newHints)

        if (isSolved()) {
            soundManager.play(SoundEffect.PUZZLE_CLEAR)
            onPuzzleSolved()
        } else {
            saveProgress()
        }
    }

    // ── Solve check & Conflict detection ───────────────────────────────────────

    /**
     * Determines error cells strictly based on visible conflicts on the board.
     * Any non-fixed cell whose current number collides with another cell in its
     * row, col, or box is marked as an error.
     */
    private fun refreshState(extraHintCells: Set<Pair<Int,Int>>? = null) {
        val state = _uiState.value
        val errors = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val v = currentGrid[r][c]
                if (v == 0 || r to c in state.fixedCells) continue
                if (hasConflict(r, c, v)) {
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

    private fun getMaxHints(difficulty: String): Int = when (difficulty) {
        "EASY"   -> 3
        "MEDIUM" -> 2
        else     -> 1
    }

    private fun onPuzzleSolved() {
        timerJob?.cancel()
        _isCleared.value = true
        _conflictingSourceCells.value = emptySet()
        viewModelScope.launch {
            val stars   = calculateStars()
            val puzzle  = currentPuzzle ?: return@launch
            val maxH    = getMaxHints(puzzle.difficulty)
            val existing = repository.getProgress(puzzleId).first()
            repository.saveProgress(
                (existing ?: Progress(puzzleId = puzzleId)).copy(
                    status       = PuzzleStatus.CLEARED.name,
                    hintUsedCount = (maxH - _hintsRemaining.value).coerceAtLeast(0),
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
            val maxH = currentPuzzle?.let { getMaxHints(it.difficulty) } ?: 3
            val existing = repository.getProgress(puzzleId).first()
            repository.saveProgress(
                (existing ?: Progress(puzzleId = puzzleId)).copy(
                    status        = PuzzleStatus.UNLOCKED.name,
                    hintUsedCount = (maxH - _hintsRemaining.value).coerceAtLeast(0),
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

    class Factory(
        private val repository: PuzzleRepository,
        private val preferences: GamePreferences,
        private val soundManager: SoundManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(repository, preferences, soundManager) as T
        }
    }
}
