package com.kidssudoku.puzzle.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GridSize(val size: Int) {
    FOUR(4), SIX(6), NINE(9)
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}

@Entity(tableName = "puzzles")
data class Puzzle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gridSize: Int,         // 4, 6, or 9
    val difficulty: String,    // "EASY", "MEDIUM", "HARD"
    val orderIndex: Int,       // 1~20
    val givenCells: String,    // JSON: list of fixed cell values (index to value map)
    val solution: String       // JSON: full solution array
)
