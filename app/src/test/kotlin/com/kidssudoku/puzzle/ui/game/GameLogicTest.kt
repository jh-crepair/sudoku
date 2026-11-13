package com.kidssudoku.puzzle.ui.game

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

        // 9단: (초,중,상급 모두 30분 = 1800초)
        assertEquals(1800, GameViewModel.getTimeLimit(9))
        assertEquals(1800, GameViewModel.getTimeLimit(9, "EASY"))
        assertEquals(1800, GameViewModel.getTimeLimit(9, "MEDIUM"))
        assertEquals(1800, GameViewModel.getTimeLimit(9, "HARD"))
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
}
