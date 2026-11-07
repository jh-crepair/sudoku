package com.kidssudoku.puzzle

import android.app.Application
import androidx.room.Room
import com.kidssudoku.puzzle.data.PuzzleSeeder
import com.kidssudoku.puzzle.data.db.SudokuDatabase
import com.kidssudoku.puzzle.data.repository.PuzzleRepository

class SudokuApplication : Application() {

    lateinit var database: SudokuDatabase
        private set

    lateinit var repository: PuzzleRepository
        private set

    lateinit var puzzleSeeder: PuzzleSeeder
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            SudokuDatabase::class.java,
            "sudoku_database"
        ).build()
        repository    = PuzzleRepository(database.puzzleDao(), database.progressDao())
        puzzleSeeder  = PuzzleSeeder(database.puzzleDao(), database.progressDao())
    }
}
