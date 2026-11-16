package com.jh.app.cherry.puzzle.sudoku

import android.app.Application
import androidx.room.Room
import com.jh.app.cherry.puzzle.sudoku.data.PuzzleSeeder
import com.jh.app.cherry.puzzle.sudoku.data.db.SudokuDatabase
import com.jh.app.cherry.puzzle.sudoku.data.preference.GamePreferences
import com.jh.app.cherry.puzzle.sudoku.data.repository.PuzzleRepository
import com.jh.app.cherry.puzzle.sudoku.sound.SoundManager

class SudokuApplication : Application() {

    lateinit var database: SudokuDatabase
        private set

    lateinit var repository: PuzzleRepository
        private set

    lateinit var gamePreferences: GamePreferences
        private set

    lateinit var soundManager: SoundManager
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
        repository      = PuzzleRepository(database.puzzleDao(), database.progressDao())
        gamePreferences = GamePreferences(applicationContext)
        soundManager    = SoundManager(applicationContext).also {
            it.setMuted(!gamePreferences.soundEnabled.value)
        }
        puzzleSeeder    = PuzzleSeeder(database.puzzleDao(), database.progressDao(), gamePreferences)
    }

    override fun onTerminate() {
        super.onTerminate()
        soundManager.release()
    }
}
