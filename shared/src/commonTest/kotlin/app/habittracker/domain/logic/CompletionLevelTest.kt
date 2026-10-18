package app.habittracker.domain.logic

import app.habittracker.domain.model.CompletionLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionLevelTest {
    @Test fun noDataHasNoLevel() = assertEquals(null, completionLevel(null))

    @Test fun eightyAndAboveIsGood() {
        assertEquals(CompletionLevel.Good, completionLevel(1.0))
        assertEquals(CompletionLevel.Good, completionLevel(0.8))
    }

    @Test fun fiftyToSeventyNineIsFair() {
        assertEquals(CompletionLevel.Fair, completionLevel(0.79))
        assertEquals(CompletionLevel.Fair, completionLevel(0.5))
    }

    @Test fun belowFiftyIsLow() {
        assertEquals(CompletionLevel.Low, completionLevel(0.49))
        assertEquals(CompletionLevel.Low, completionLevel(0.0))
    }

    @Test fun usesTheRoundedPercentShownOnScreen() {
        // 79.6% is displayed as "80%", so it must be green, not yellow.
        assertEquals(CompletionLevel.Good, completionLevel(0.796))
        assertEquals(CompletionLevel.Fair, completionLevel(0.4951))
    }
}
