package com.jh.app.cherry.puzzle.sudoku.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class GameLogicTest {

    @Test
    fun testGridSizeTimeLimits() {
        // 4단: (초,중,상급 모두 10분 = 600초)
        assertEquals(600, GameViewModel.getTimeLimit(4))
        assertEquals(600, GameViewModel.getTimeLimit(4, "EASY"))
        assertEquals(600, GameViewModel.getTimeLimit(4, "MEDIUM"))
        assertEquals(600, GameViewModel.getTimeLimit(4, "HARD"))

        // 6단: (초,중,상급 모두 20분 = 1200초)
        assertEquals(1200, GameViewModel.getTimeLimit(6))
        assertEquals(1200, GameViewModel.getTimeLimit(6, "EASY"))
        assertEquals(1200, GameViewModel.getTimeLimit(6, "MEDIUM"))
        assertEquals(1200, GameViewModel.getTimeLimit(6, "HARD"))

        // 9단: 초급/중급 30분, 상급 50분
        assertEquals(1800, GameViewModel.getTimeLimit(9))
        assertEquals(1800, GameViewModel.getTimeLimit(9, "EASY"))
        assertEquals(1800, GameViewModel.getTimeLimit(9, "MEDIUM"))
        assertEquals(3000, GameViewModel.getTimeLimit(9, "HARD"))
    }

    @Test
    fun testTimeWarningThreshold() {
        val limit4 = GameViewModel.getTimeLimit(4) // 600s (10분)
        val warningThreshold = limit4 * 0.2f // 120s

        // 480초 진행 -> 남은 시간 120초 (딱 20% 남음) -> 경고 색상 대상
        val remainingAt480 = limit4 - 480
        assertTrue(remainingAt480 <= warningThreshold)

        // 479초 진행 -> 남은 시간 121초 (> 20%) -> 경고 색상 아님
        val remainingAt479 = limit4 - 479
        assertFalse(remainingAt479 <= warningThreshold)

        // 580초 진행 -> 남은 시간 20초 (< 20%) -> 경고 색상 대상
        val remainingAt580 = limit4 - 580
        assertTrue(remainingAt580 <= warningThreshold)
    }

    @Test
    fun testMaxFailCount() {
        // 오류 횟수는 등급별 모두 10회로 제한
        assertEquals(10, GameViewModel.MAX_FAIL_COUNT)
    }

    @Test
    fun testCellMarkersCanBeSelectedBeforeNumberAndAreMutuallyExclusive() {
        val cell = 2 to 3
        val checks = mutableSetOf<Pair<Int, Int>>()
        val questions = mutableSetOf<Pair<Int, Int>>()

        assertTrue(toggleCellMarker(cell, checks, questions))
        assertTrue(cell in checks)
        assertTrue(toggleCellMarker(cell, questions, checks))
        assertTrue(checks.isEmpty())
        assertTrue(cell in questions)
        assertTrue(toggleCellMarker(cell, questions, checks))
        assertTrue(questions.isEmpty())
    }

    @Test
    fun testClearAllUserInputRetainsHintCellsAndFixedCells() {
        // 4x4 그리드 초기화
        val grid = MutableList(4) { MutableList(4) { 0 } }
        val fixedCells = setOf(0 to 0)
        val hintCells = setOf(1 to 1)

        grid[0][0] = 1 // 고정 셀
        grid[1][1] = 2 // 힌트로 채워진 셀
        grid[0][1] = 3 // 사용자가 입력한 셀
        grid[2][2] = 4 // 사용자가 입력한 셀
        grid[3][3] = 0 // 빈 셀

        clearNonHintUserInput(grid, fixedCells, hintCells)

        // 고정 셀과 힌트 셀은 유지되어야 함
        assertEquals(1, grid[0][0])
        assertEquals(2, grid[1][1])

        // 일반 사용자 입력 셀은 0으로 지워져야 함
        assertEquals(0, grid[0][1])
        assertEquals(0, grid[2][2])
        assertEquals(0, grid[3][3])
    }

    @Test
    fun testClearAllWithoutHintsClearsAllUserInputCells() {
        val grid = MutableList(4) { MutableList(4) { 0 } }
        val fixedCells = setOf(0 to 0)
        val hintCells = emptySet<Pair<Int, Int>>()

        grid[0][0] = 1 // 고정 셀
        grid[0][1] = 2 // 사용자 입력
        grid[1][0] = 3 // 사용자 입력

        clearNonHintUserInput(grid, fixedCells, hintCells)

        assertEquals(1, grid[0][0])
        assertEquals(0, grid[0][1])
        assertEquals(0, grid[1][0])
    }
}
