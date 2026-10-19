package app.habittracker.android

import app.habittracker.android.ui.common.FlameTier
import app.habittracker.android.ui.common.MilestoneCopy
import app.habittracker.android.ui.theme.DarkHabitColors
import app.habittracker.android.ui.theme.LightHabitColors
import app.habittracker.android.ui.theme.animationsDisabled
import app.habittracker.android.ui.theme.contrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeTest {
    @Test fun flameTierBoundaries() {
        val expected = listOf(
            0 to FlameTier.None, 6 to FlameTier.Small, 7 to FlameTier.Medium, 29 to FlameTier.Medium,
            30 to FlameTier.Large, 99 to FlameTier.Large, 100 to FlameTier.Legendary,
        )
        for ((streak, tier) in expected) assertEquals(tier, FlameTier.forStreak(streak), "streak $streak")
    }

    @Test fun milestoneCopy() {
        assertEquals("7-day streak", MilestoneCopy.title(7))
        assertEquals("30-day streak", MilestoneCopy.title(30))
        assertEquals("100-day streak", MilestoneCopy.title(100))
        assertEquals("365-day streak", MilestoneCopy.title(365))
        assertEquals("One week strong", MilestoneCopy.body(7))
        assertEquals("A month of perfect days", MilestoneCopy.body(30))
        assertEquals("100 days. Legendary.", MilestoneCopy.body(100))
        assertEquals("A whole year", MilestoneCopy.body(365))
    }

    @Test fun heroTextContrast() {
        for (colors in listOf(LightHabitColors, DarkHabitColors)) {
            assertTrue(contrastRatio(colors.heroText, colors.hero) >= 4.5, "heroText on hero")
            assertTrue(contrastRatio(colors.heroSubtext, colors.hero) >= 4.5, "heroSubtext on hero")
        }
    }

    @Test fun animationsDisabledAtZeroScale() {
        assertTrue(animationsDisabled(0f))
        assertFalse(animationsDisabled(1f))
        assertFalse(animationsDisabled(0.5f))
    }
}
