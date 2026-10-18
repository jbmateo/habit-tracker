package app.habittracker.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class KeysTest {
    @Test fun unknownIconFallsBack() = assertEquals("checklist", IconKeys.resolve("rocket"))
    @Test fun knownIconResolves() = assertEquals("run", IconKeys.resolve("run"))
    @Test fun unknownColorFallsBack() = assertEquals("gray", ColorKeys.resolve("mauve"))
    @Test fun iconCatalogHas36UniqueKeys() {
        assertEquals(36, IconKeys.all.size)
        assertEquals(36, IconKeys.all.toSet().size)
    }
    @Test fun colorCatalogHas10Keys() = assertEquals(10, ColorKeys.all.size)
}
