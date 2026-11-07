package com.kidssudoku.puzzle.data.repository

import com.kidssudoku.puzzle.data.db.PuzzleDao
import com.kidssudoku.puzzle.data.db.ProgressDao
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import kotlinx.coroutines.flow.Flow

class PuzzleRepository(
    private val puzzleDao: PuzzleDao,
    private val progressDao: ProgressDao
) {
    fun getPuzzles(gridSize: Int, difficulty: String): Flow<List<Puzzle>> =
        puzzleDao.getPuzzlesByTypeAndDifficulty(gridSize, difficulty)

    suspend fun getPuzzleById(id: Int): Puzzle? = puzzleDao.getPuzzleById(id)

    fun getProgressList(gridSize: Int, difficulty: String): Flow<List<Progress>> =
        progressDao.getProgressByTypeAndDifficulty(gridSize, difficulty)

    fun getProgress(puzzleId: Int): Flow<Progress?> =
        progressDao.getProgressByPuzzleId(puzzleId)

    suspend fun saveProgress(progress: Progress) = progressDao.upsert(progress)

    suspend fun unlockNextPuzzle(puzzleId: Int) = progressDao.unlockPuzzle(puzzleId)

    suspend fun resetProgress(gridSize: Int, difficulty: String) {
        progressDao.deleteProgressByTypeAndDifficulty(gridSize, difficulty)
    }

    suspend fun getPuzzleCount(): Int = puzzleDao.count()

    suspend fun insertPuzzles(puzzles: List<Puzzle>) = puzzleDao.insertAll(puzzles)
}
