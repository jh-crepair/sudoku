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

    private fun findBestCell(grid: Array<IntArray>, size: Int): Triple<Int, Int, List<Int>>? {
        var minOptions = size + 1
        var bestRow = -1
        var bestCol = -1
        var bestNums = emptyList<Int>()

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] == 0) {
                    val validNums = mutableListOf<Int>()
                    for (num in 1..size) {
                        if (isValid(grid, r, c, num, size)) {
                            validNums.add(num)
                        }
                    }
                    if (validNums.isEmpty()) return Triple(r, c, emptyList()) // 막다른 길
                    if (validNums.size < minOptions) {
                        minOptions = validNums.size
                        bestRow = r
                        bestCol = c
                        bestNums = validNums
                        if (minOptions == 1) return Triple(bestRow, bestCol, bestNums)
                    }
                }
            }
        }
        if (bestRow == -1) return null // 모두 채워짐
        return Triple(bestRow, bestCol, bestNums)
    }

    private fun solve(grid: Array<IntArray>, size: Int, rng: Random? = null): Boolean {
        val best = findBestCell(grid, size) ?: return true
        val (row, col, nums) = best
        if (nums.isEmpty()) return false

        val candidates = if (rng != null) nums.shuffled(rng) else nums
        for (num in candidates) {
            grid[row][col] = num
            if (solve(grid, size, rng)) return true
            grid[row][col] = 0
        }
        return false
    }

    /** Count solutions with MRV heuristic, capping at limit for lightning-fast pruning */
    private fun countSolutions(grid: Array<IntArray>, size: Int, limit: Int = 2): Int {
        val best = findBestCell(grid, size) ?: return 1
        val (row, col, nums) = best
        if (nums.isEmpty()) return 0

        var count = 0
        for (num in nums) {
            grid[row][col] = num
            count += countSolutions(grid, size, limit - count)
            grid[row][col] = 0
            if (count >= limit) return limit
        }
        return count
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
