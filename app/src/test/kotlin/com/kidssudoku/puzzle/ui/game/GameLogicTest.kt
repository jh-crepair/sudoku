package com.kidssudoku.puzzle.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class GameLogicTest {

    @Test
    fun testDifficultyTimeLimits() {
        // 초급은 제한 시간 5분 (300초)
        assertEquals(300, GameViewModel.getTimeLimit("EASY"))
        // 중급은 8분 (480초)
        assertEquals(480, GameViewModel.getTimeLimit("MEDIUM"))
        // 상급은 10분 (600초)
        assertEquals(600, GameViewModel.getTimeLimit("HARD"))
    }

    @Test
    fun testTimeWarningThreshold() {
        val easyLimit = GameViewModel.getTimeLimit("EASY") // 300s
        val warningThreshold = easyLimit * 0.2f // 60s

        // 240초 진행 -> 남은 시간 60초 (딱 20% 남음) -> 경고 색상 대상
        val remainingAt240 = easyLimit - 240
        assertTrue(remainingAt240 <= warningThreshold)

        // 239초 진행 -> 남은 시간 61초 (> 20%) -> 경고 색상 아님
        val remainingAt239 = easyLimit - 239
        assertFalse(remainingAt239 <= warningThreshold)

        // 290초 진행 -> 남은 시간 10초 (< 20%) -> 경고 색상 대상
        val remainingAt290 = easyLimit - 290
        assertTrue(remainingAt290 <= warningThreshold)
    }

    @Test
    fun testMaxFailCount() {
        // 오류 횟수는 등급별 모두 10회로 제한
        assertEquals(10, GameViewModel.MAX_FAIL_COUNT)
    }
}
