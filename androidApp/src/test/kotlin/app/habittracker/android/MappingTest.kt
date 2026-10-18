package app.habittracker.android

import app.habittracker.android.ui.icons.habitIcon
import app.habittracker.android.ui.icons.iconVectorFor
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.domain.model.ColorKeys
import app.habittracker.domain.model.IconKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MappingTest {
    @Test
    fun everyIconKeyIsMapped() {
        IconKeys.all.forEach { key -> assertTrue(iconVectorFor(key) != null, "missing icon for $key") }
    }

    @Test
    fun unknownIconKeyFallsBackToChecklist() {
        assertEquals(habitIcon("checklist"), habitIcon("no-such-icon"))
    }

    @Test
    fun everyColorKeyHasDistinctLightAndDark() {
        ColorKeys.all.forEach { key ->
            assertNotEquals(categoryColor(key, dark = false), categoryColor(key, dark = true), key)
        }
    }

    @Test
    fun unknownColorKeyFallsBackToGray() {
        assertEquals(categoryColor("gray", false), categoryColor("mauve", false))
        assertEquals(categoryColor("gray", true), categoryColor("mauve", true))
    }
}
