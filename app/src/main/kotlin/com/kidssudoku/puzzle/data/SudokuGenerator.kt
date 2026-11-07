package com.kidssudoku.puzzle.data

import com.kidssudoku.puzzle.data.model.Puzzle
import kotlin.random.Random

class SudokuGenerator {

    private fun boxDims(size: Int): Pair<Int, Int> = when (size) {
        4    -> 2 to 2
        6    -> 2 to 3
        else -> 3 to 3
    }

    private fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int, size: Int): Boolean {
        if (num in grid[row]) return false
        for (r in 0 until size) if (grid[r][col] == num) return false
        val (br, bc) = boxDims(size)
        val sr = (row / br) * br
        val sc = (col / bc) * bc
        for (r in sr until sr + br)
            for (c in sc until sc + bc)
                if (grid[r][c] == num) return false
        return true
    }

    private fun solve(grid: Array<IntArray>, size: Int, rng: Random? = null): Boolean {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (grid[row][col] == 0) {
                    val nums = (1..size).toMutableList().also { if (rng != null) it.shuffle(rng) }
                    for (num in nums) {
                        if (isValid(grid, row, col, num, size)) {
                            grid[row][col] = num
                            if (solve(grid, size, rng)) return true
                            grid[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    /** Count solutions, capping at 2 for efficiency */
    private fun countSolutions(grid: Array<IntArray>, size: Int, limit: Int = 2): Int {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (grid[row][col] == 0) {
                    var count = 0
                    for (num in 1..size) {
                        if (isValid(grid, row, col, num, size)) {
                            grid[row][col] = num
                            count += countSolutions(grid, size, limit)
                            grid[row][col] = 0
                            if (count >= limit) return count
                        }
                    }
                    return count
                }
            }
        }
        return 1
    }

    private fun generateFullGrid(size: Int, rng: Random): Array<IntArray> {
        val grid = Array(size) { IntArray(size) }
        solve(grid, size, rng)
        return grid
    }

    fun generate(
        gridSize: Int,
        difficulty: String,
        orderIndex: Int,
        revealCount: Int,
        rng: Random
    ): Puzzle {
        val totalCells = gridSize * gridSize
        val toRemove = totalCells - revealCount

        while (true) {
            val full = generateFullGrid(gridSize, rng)
            val solution = full.flatMap { it.asList() }

            // Work on a copy
            val work = Array(gridSize) { r -> full[r].copyOf() }

            val indices = (0 until totalCells).toMutableList().also { it.shuffle(rng) }
            var removed = 0

            for (idx in indices) {
                if (removed >= toRemove) break
                val r = idx / gridSize
                val c = idx % gridSize
                val backup = work[r][c]
                work[r][c] = 0

                val testCopy = Array(gridSize) { row -> work[row].copyOf() }
                if (countSolutions(testCopy, gridSize) == 1) {
                    removed++
                } else {
                    work[r][c] = backup
                }
            }

            // Collect revealed cells
            val givenMap = mutableMapOf<Int, Int>()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (work[r][c] != 0) {
                        givenMap[r * gridSize + c] = work[r][c]
                    }
                }
            }

            if (givenMap.size >= revealCount - 2) {
                val givenJson = buildString {
                    append("{")
                    givenMap.entries.joinTo(this, ",") { (k, v) -> "\"$k\":$v" }
                    append("}")
                }
                val solutionJson = "[${solution.joinToString(",")}]"
                return Puzzle(
                    gridSize = gridSize,
                    difficulty = difficulty,
                    orderIndex = orderIndex,
                    givenCells = givenJson,
                    solution = solutionJson
                )
            }
        }
    }
}
