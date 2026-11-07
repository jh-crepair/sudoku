package com.kidssudoku.puzzle.data.db

import androidx.room.*
import com.kidssudoku.puzzle.data.model.Progress
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE puzzleId = :puzzleId")
    fun getProgressByPuzzleId(puzzleId: Int): Flow<Progress?>

    @Query("SELECT * FROM progress WHERE puzzleId IN (SELECT id FROM puzzles WHERE gridSize = :gridSize AND difficulty = :difficulty)")
    fun getProgressByTypeAndDifficulty(gridSize: Int, difficulty: String): Flow<List<Progress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: Progress)

    @Query("DELETE FROM progress WHERE puzzleId IN (SELECT id FROM puzzles WHERE gridSize = :gridSize AND difficulty = :difficulty)")
    suspend fun deleteProgressByTypeAndDifficulty(gridSize: Int, difficulty: String)

    @Query("UPDATE progress SET status = 'UNLOCKED' WHERE puzzleId = :puzzleId AND status = 'LOCKED'")
    suspend fun unlockPuzzle(puzzleId: Int)

    @Query("DELETE FROM progress")
    suspend fun deleteAll()
}
