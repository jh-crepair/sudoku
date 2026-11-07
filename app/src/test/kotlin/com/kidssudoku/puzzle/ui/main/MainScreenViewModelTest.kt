package com.kidssudoku.puzzle.ui.main

import com.kidssudoku.puzzle.data.SudokuGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

class MainScreenViewModelTest {
    @Test
    fun testSudokuGeneration() {
        val generator = SudokuGenerator()

        // 4x4 generation test
        val puzzle4 = generator.generate(4, "EASY", 1, 10, Random(42))
        assertEquals(4, puzzle4.gridSize)
        assertEquals(1, puzzle4.orderIndex)
        assertNotNull(puzzle4.solution)

        // 6x6 generation test
        val puzzle6 = generator.generate(6, "MEDIUM", 1, 15, Random(42))
        assertEquals(6, puzzle6.gridSize)
        assertEquals(1, puzzle6.orderIndex)
        assertNotNull(puzzle6.solution)

        // 9x9 generation test
        val puzzle9 = generator.generate(9, "HARD", 1, 26, Random(42))
        assertEquals(9, puzzle9.gridSize)
        assertEquals(1, puzzle9.orderIndex)
        assertNotNull(puzzle9.solution)
    }
}
