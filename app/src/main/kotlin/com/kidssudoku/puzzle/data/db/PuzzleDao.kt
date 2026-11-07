package com.kidssudoku.puzzle.data.db

import androidx.room.*
import com.kidssudoku.puzzle.data.model.Puzzle
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    @Query("SELECT * FROM puzzles WHERE gridSize = :gridSize AND difficulty = :difficulty GROUP BY orderIndex ORDER BY orderIndex ASC")
    fun getPuzzlesByTypeAndDifficulty(gridSize: Int, difficulty: String): Flow<List<Puzzle>>

    @Query("SELECT * FROM puzzles WHERE id = :id")
    suspend fun getPuzzleById(id: Int): Puzzle?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(puzzles: List<Puzzle>)

    @Query("SELECT COUNT(*) FROM puzzles")
    suspend fun count(): Int

    @Query("DELETE FROM puzzles WHERE id NOT IN (SELECT MIN(id) FROM puzzles GROUP BY gridSize, difficulty, orderIndex)")
    suspend fun removeDuplicates()

    @Query("SELECT id FROM puzzles WHERE gridSize = :gridSize AND difficulty = :difficulty ORDER BY orderIndex ASC LIMIT 1")
    suspend fun getFirstPuzzleId(gridSize: Int, difficulty: String): Int?

    @Query("SELECT * FROM puzzles WHERE gridSize = :gridSize AND difficulty = :difficulty AND orderIndex = :orderIndex LIMIT 1")
    suspend fun getPuzzleByOrder(gridSize: Int, difficulty: String, orderIndex: Int): Puzzle?

    @Query("DELETE FROM puzzles")
    suspend fun deleteAll()
}
