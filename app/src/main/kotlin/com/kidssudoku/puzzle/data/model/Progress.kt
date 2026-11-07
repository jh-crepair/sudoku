package com.kidssudoku.puzzle.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PuzzleStatus {
    LOCKED, UNLOCKED, CLEARED
}

@Entity(
    tableName = "progress",
    foreignKeys = [ForeignKey(
        entity = Puzzle::class,
        parentColumns = ["id"],
        childColumns = ["puzzleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("puzzleId")]
)
data class Progress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val puzzleId: Int,
    val status: String = PuzzleStatus.LOCKED.name,
    val hintUsedCount: Int = 0,  // max 5
    val elapsedSeconds: Int = 0,
    val starRating: Int = 0,     // 0 = not cleared, 1-3 stars
    val currentState: String = "", // JSON: user's current input state
    val updatedAt: Long = System.currentTimeMillis()
)
