package com.kidssudoku.puzzle.data

import com.kidssudoku.puzzle.data.db.ProgressDao
import com.kidssudoku.puzzle.data.db.PuzzleDao
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import com.kidssudoku.puzzle.data.preference.GamePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

class PuzzleSeeder(
    private val puzzleDao: PuzzleDao,
    private val progressDao: ProgressDao,
    private val preferences: GamePreferences
) {
    private val mutex = Mutex()

    companion object {
        private const val CURRENT_SEEDER_VERSION = 3
    }

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentVersion = preferences.seederVersion
            val puzzleCount = puzzleDao.count()

            if (currentVersion >= CURRENT_SEEDER_VERSION && puzzleCount > 0) {
                puzzleDao.removeDuplicates()
                return@withContext
            }

            // Version changed or empty database: reseed puzzles
            puzzleDao.deleteAll()
            progressDao.deleteAll()

            val generator = SudokuGenerator()
            val puzzleList = mutableListOf<Puzzle>()

            // (gridSize, difficulty, count, startRevealCount, endRevealCount)
            // 4단: 초급 10개, 중급 20개, 상급 30개
            // 6단: 초급 20개, 중급 30개, 상급 40개
            // 9단: 초급 20개, 중급 30개, 상급 50개
            val configs = listOf(
                DifficultyConfig(4, "EASY",   count = 10, startReveal = 11, endReveal = 9),
                DifficultyConfig(4, "MEDIUM", count = 20, startReveal = 8,  endReveal = 6),
                DifficultyConfig(4, "HARD",   count = 30, startReveal = 6,  endReveal = 4),
                DifficultyConfig(6, "EASY",   count = 20, startReveal = 22, endReveal = 18),
                DifficultyConfig(6, "MEDIUM", count = 30, startReveal = 17, endReveal = 13),
                DifficultyConfig(6, "HARD",   count = 40, startReveal = 13, endReveal = 10),
                DifficultyConfig(9, "EASY",   count = 20, startReveal = 44, endReveal = 36),
                DifficultyConfig(9, "MEDIUM", count = 30, startReveal = 34, endReveal = 28),
                DifficultyConfig(9, "HARD",   count = 50, startReveal = 26, endReveal = 21)
            )

            var seed = 12345L
            for (config in configs) {
                for (orderIndex in 1..config.count) {
                    val revealCount = calculateRevealCount(
                        startReveal = config.startReveal,
                        endReveal   = config.endReveal,
                        orderIndex  = orderIndex,
                        totalCount  = config.count
                    )
                    val puzzle = generator.generate(
                        gridSize    = config.gridSize,
                        difficulty  = config.difficulty,
                        orderIndex  = orderIndex,
                        revealCount = revealCount,
                        rng         = Random(seed++)
                    )
                    puzzleList.add(puzzle)
                }
            }

            puzzleDao.insertAll(puzzleList)
            preferences.seederVersion = CURRENT_SEEDER_VERSION
            unlockFirstPuzzles()
        }
    }

    private fun calculateRevealCount(startReveal: Int, endReveal: Int, orderIndex: Int, totalCount: Int): Int {
        if (totalCount <= 1) return startReveal
        val progress = (orderIndex - 1).toFloat() / (totalCount - 1).toFloat()
        return (startReveal - progress * (startReveal - endReveal)).roundToInt()
    }

    private suspend fun unlockFirstPuzzles() {
        val configs = listOf(
            Pair(4, "EASY"), Pair(4, "MEDIUM"), Pair(4, "HARD"),
            Pair(6, "EASY"), Pair(6, "MEDIUM"), Pair(6, "HARD"),
            Pair(9, "EASY"), Pair(9, "MEDIUM"), Pair(9, "HARD")
        )
        for ((gridSize, difficulty) in configs) {
            val firstId = puzzleDao.getFirstPuzzleId(gridSize, difficulty)
            if (firstId != null) {
                progressDao.upsert(
                    Progress(
                        puzzleId = firstId,
                        status = PuzzleStatus.UNLOCKED.name
                    )
                )
            }
        }
    }

    private data class DifficultyConfig(
        val gridSize: Int,
        val difficulty: String,
        val count: Int,
        val startReveal: Int,
        val endReveal: Int
    )
}
