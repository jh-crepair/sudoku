package com.kidssudoku.puzzle.ui.main

import com.kidssudoku.puzzle.data.SudokuGenerator
import org.junit.Test
import kotlin.random.Random

class MainScreenViewModelTest {
    @Test
    fun testPuzzleOrder() {
        val generator = SudokuGenerator()
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
            val list = mutableListOf<Int>()
            for (orderIndex in 1..20) {
                val puzzle = generator.generate(gridSize, difficulty, orderIndex, revealCount, Random(seed++))
                list.add(puzzle.orderIndex)
            }
            println("Config ($gridSize, $difficulty): $list")
        }
    }
}
