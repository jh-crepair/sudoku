package com.kidssudoku.puzzle.data

import com.kidssudoku.puzzle.data.db.ProgressDao
import com.kidssudoku.puzzle.data.db.PuzzleDao
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class PuzzleSeeder(
    private val puzzleDao: PuzzleDao,
    private val progressDao: ProgressDao
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            puzzleDao.removeDuplicates()
            if (puzzleDao.count() > 0) return@withContext

            val generator = SudokuGenerator()
            val puzzleList = mutableListOf<Puzzle>()

            val configs = listOf(
                Triple(4, "EASY",   10),
                Triple(4, "MEDIUM",  7),
                Triple(4, "HARD",    5),
                Triple(6, "EASY",   20),
                Triple(6, "MEDIUM", 15),
                Triple(6, "HARD",   11),
                Triple(9, "EASY",   40),
                Triple(9, "MEDIUM", 31),
                Triple(9, "HARD",   22)
            )

            var seed = 12345L
            for ((gridSize, difficulty, revealCount) in configs) {
                for (orderIndex in 1..20) {
                    val puzzle = generator.generate(gridSize, difficulty, orderIndex, revealCount, Random(seed++))
                    puzzleList.add(puzzle)
                }
            }

            puzzleDao.insertAll(puzzleList)
            unlockFirstPuzzles()
        }
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
}
