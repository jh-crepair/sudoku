package com.jh.app.cherry.puzzle.sudoku.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jh.app.cherry.puzzle.sudoku.data.model.Puzzle
import com.jh.app.cherry.puzzle.sudoku.data.model.Progress

@Database(
    entities = [Puzzle::class, Progress::class],
    version = 1,
    exportSchema = false
)
abstract class SudokuDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
    abstract fun progressDao(): ProgressDao
}
