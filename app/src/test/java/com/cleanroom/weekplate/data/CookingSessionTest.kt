package com.cleanroom.weekplate.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CookingSessionTest {
    @Test
    fun `running session derives remaining time from its deadline`() {
        val running = CookingSession(
            recipeId = "recipe",
            step = 2,
            servings = 4,
            remainingSeconds = 30,
            running = true,
            endsAtEpochMillis = 10_000
        )

        assertEquals(5, running.remainingAt(5_000))
        assertEquals(0, running.remainingAt(11_000))
        assertEquals(30, running.copy(running = false).remainingAt(11_000))
    }
}
