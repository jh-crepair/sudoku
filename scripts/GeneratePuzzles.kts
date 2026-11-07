#!/usr/bin/env kotlin
/**
 * Sudoku Puzzle Generator — generates 180 valid puzzles (20 per gridSize×difficulty combo)
 * Run with: kotlinc -script GeneratePuzzles.kts
 * Output: ../app/src/main/assets/puzzles.json
 */

import kotlin.random.Random

// ── Solver / Validator ──────────────────────────────────────────────────────

fun boxDims(size: Int): Pair<Int,Int> = when(size) {
    4 -> 2 to 2
    6 -> 2 to 3
    else -> 3 to 3
}

fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int, size: Int): Boolean {
    if (num in grid[row]) return false
    for (r in 0 until size) if (grid[r][col] == num) return false
    val (br, bc) = boxDims(size)
    val sr = (row / br) * br; val sc = (col / bc) * bc
    for (r in sr until sr+br) for (c in sc until sc+bc) if (grid[r][c] == num) return false
    return true
}

fun solve(grid: Array<IntArray>, size: Int, rng: Random? = null): Boolean {
    for (row in 0 until size) for (col in 0 until size) {
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
    return true
}

/** Count solutions — returns early at 2 to check uniqueness */
fun countSolutions(grid: Array<IntArray>, size: Int): Int {
    for (row in 0 until size) for (col in 0 until size) {
        if (grid[row][col] == 0) {
            var count = 0
            for (num in 1..size) {
                if (isValid(grid, row, col, num, size)) {
                    grid[row][col] = num
                    count += countSolutions(grid, size)
                    grid[row][col] = 0
                    if (count >= 2) return count
                }
            }
            return count
        }
    }
    return 1
}

fun generateFullGrid(size: Int, rng: Random): Array<IntArray> {
    val grid = Array(size) { IntArray(size) }
    solve(grid, size, rng)
    return grid
}

// ── Puzzle creation ──────────────────────────────────────────────────────────

val REVEAL_COUNTS = mapOf(
    4 to mapOf("EASY" to 10, "MEDIUM" to 7, "HARD" to 5),
    6 to mapOf("EASY" to 20, "MEDIUM" to 15, "HARD" to 11),
    9 to mapOf("EASY" to 40, "MEDIUM" to 31, "HARD" to 22)
)

data class SudokuPuzzle(
    val gridSize: Int,
    val difficulty: String,
    val orderIndex: Int,
    val givenCells: Map<Int,Int>,  // index → value
    val solution: List<Int>
)

fun makePuzzle(size: Int, difficulty: String, orderIndex: Int, rng: Random): SudokuPuzzle {
    val revealCount = REVEAL_COUNTS[size]!![difficulty]!!
    val totalCells = size * size

    while (true) {
        val full = generateFullGrid(size, rng)
        val solution = full.flatMap { it.asList() }

        // Start with all cells revealed, then remove one by one
        val revealed = (0 until totalCells).toMutableList()
        revealed.shuffle(rng)
        val toRemove = totalCells - revealCount

        val given = revealed.toMutableList()
        var removed = 0

        for (idx in revealed) {
            if (removed >= toRemove) break
            val r = idx / size; val c = idx % size
            val backup = full[r][c]
            full[r][c] = 0
            given.remove(idx)

            // Check uniqueness — for large grids limit check for performance
            val unique = if (size == 9 && removed > toRemove - 5) {
                countSolutions(full.map { it.copyOf() }.toTypedArray(), size) == 1
            } else {
                countSolutions(full.map { it.copyOf() }.toTypedArray(), size) == 1
            }

            if (!unique) {
                full[r][c] = backup
                given.add(idx)
            } else {
                removed++
            }
        }

        if (removed >= toRemove - 2) { // accept within 2 cells of target
            val givenMap = given.associate { idx -> idx to solution[idx] }
            return SudokuPuzzle(size, difficulty, orderIndex, givenMap, solution)
        }
        // else retry
    }
}

// ── Main ─────────────────────────────────────────────────────────────────────

val rng = Random(42)
val puzzles = mutableListOf<SudokuPuzzle>()

val gridSizes = listOf(4, 6, 9)
val difficulties = listOf("EASY", "MEDIUM", "HARD")

for (size in gridSizes) {
    for (diff in difficulties) {
        println("Generating $size×$size $diff puzzles...")
        for (i in 1..20) {
            print("  puzzle $i... ")
            val p = makePuzzle(size, diff, i, rng)
            puzzles.add(p)
            println("done (${p.givenCells.size} given cells)")
        }
    }
}

// ── Serialize to JSON ─────────────────────────────────────────────────────────

fun toJson(p: SudokuPuzzle): String {
    val givenStr = p.givenCells.entries.joinToString(",") { (k,v) -> "\"$k\":$v" }
    val solutionStr = p.solution.joinToString(",")
    return """{"gridSize":${p.gridSize},"difficulty":"${p.difficulty}","orderIndex":${p.orderIndex},"givenCells":{$givenStr},"solution":[$solutionStr]}"""
}

val json = "[\n" + puzzles.joinToString(",\n") { toJson(it) } + "\n]"

val outFile = java.io.File("../app/src/main/assets/puzzles.json")
outFile.writeText(json)
println("\n✅ Generated ${puzzles.size} puzzles → ${outFile.absolutePath}")
