package com.kidssudoku.puzzle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.Progress

@Database(
    entities = [Puzzle::class, Progress::class],
    version = 1,
    exportSchema = false
)
abstract class SudokuDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
    abstract fun progressDao(): ProgressDao
}
