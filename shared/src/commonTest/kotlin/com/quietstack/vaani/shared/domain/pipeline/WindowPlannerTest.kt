package com.quietstack.vaani.shared.domain.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowPlannerTest {

    @Test
    fun singleWindowWhenDurationFitsInsideOneWindow() {
        val planner = WindowPlanner(windowLengthMs = 30_000, overlapMs = 3_000)
        val windows = planner.plan(totalDurationMs = 10_000)

        assertEquals(1, windows.size)
        assertEquals(0L, windows.first().startMs)
        assertEquals(10_000L, windows.first().endMs)
    }

    @Test
    fun consecutiveWindowsOverlapByTheConfiguredAmount() {
        val planner = WindowPlanner(windowLengthMs = 30_000, overlapMs = 3_000)
        val windows = planner.plan(totalDurationMs = 65_000)

        for (i in 0 until windows.size - 1) {
            val overlap = windows[i].endMs - windows[i + 1].startMs
            assertEquals(3_000L, overlap, "window $i should overlap the next by 3000ms")
        }
    }

    @Test
    fun lastWindowEndsExactlyAtFileDurationAndIsNotDropped() {
        val planner = WindowPlanner(windowLengthMs = 30_000, overlapMs = 3_000)
        val totalDuration = 65_000L
        val windows = planner.plan(totalDuration)

        assertEquals(totalDuration, windows.last().endMs)
        assertTrue(windows.last().durationMs > 0)
    }

    @Test
    fun twoHourFileProducesABoundedEvenlySpacedPlan() {
        val planner = WindowPlanner(windowLengthMs = 30_000, overlapMs = 3_000)
        val twoHoursMs = 2 * 60 * 60 * 1000L
        val windows = planner.plan(twoHoursMs)

        assertTrue(windows.isNotEmpty())
        assertEquals(twoHoursMs, windows.last().endMs)
        // stride is 27s, so roughly twoHoursMs / 27_000 windows
        assertTrue(windows.size in 260..270)
    }

    @Test
    fun zeroOrNegativeDurationProducesNoWindows() {
        val planner = WindowPlanner()
        assertEquals(emptyList(), planner.plan(0))
        assertEquals(emptyList(), planner.plan(-1))
    }
}
