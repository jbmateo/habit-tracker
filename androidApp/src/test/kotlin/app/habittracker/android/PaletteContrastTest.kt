package app.habittracker.android

import app.habittracker.android.ui.theme.categoryOnColorContrast
import app.habittracker.domain.model.ColorKeys
import kotlin.test.Test
import kotlin.test.assertTrue

class PaletteContrastTest {
    @Test fun everyCategoryFillHasReadableOnColorInBothModes() {
        for (key in ColorKeys.all) for (dark in listOf(false, true)) {
            val ratio = categoryOnColorContrast(key, dark)
            assertTrue(ratio >= 3.0, "$key dark=$dark contrast $ratio < 3:1")
        }
    }
}
