package app.habittracker.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import app.habittracker.android.ui.form.REMINDER_HINT
import org.junit.Rule
import org.junit.Test

/** Runs on a fresh DB: the orchestrator clears app data before every test (see build.gradle.kts). */
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createHabitThenToggleUpdatesProgress() {
        rule.onNodeWithText("Create your first habit").performClick()
        rule.onNodeWithText("Name").performTextInput("Read")
        rule.onNodeWithText("Save").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithContentDescriptionCount("Mark done: Read") == 1
        }
        rule.onNodeWithContentDescription("0 of 1 habits done").assertIsDisplayed()

        rule.onNodeWithContentDescription("Mark done: Read").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithContentDescriptionCount("1 of 1 habits done") == 1
        }
        // "Today" is pinned by TestHabitApp, so the header never depends on when the test runs.
        rule.onNodeWithText("Wednesday, 14 October").assertIsDisplayed()
    }

    @Test
    fun completingOnlyHabitShowsAllDone() {
        rule.onNodeWithText("Create your first habit").performClick()
        rule.onNodeWithText("Name").performTextInput("Read")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescriptionCount("Mark done: Read") == 1 }

        rule.onNodeWithContentDescription("Mark done: Read").performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithText("All done today ✨").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("All done today ✨").assertIsDisplayed()
    }

    @Test
    fun reminderToggleShowsTimeField() {
        // Pre-grant so the system permission dialog doesn't cover the sheet.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName, android.Manifest.permission.POST_NOTIFICATIONS,
        )
        rule.onNodeWithText("Create your first habit").performClick()
        rule.onNodeWithContentDescription("Remind me").performClick()

        rule.onNodeWithText(REMINDER_HINT).assertIsDisplayed()
        rule.onNodeWithContentDescription("Reminder time").assertIsDisplayed()
    }
}

private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithContentDescriptionCount(
    description: String,
): Int = onAllNodesWithContentDescription(description).fetchSemanticsNodes().size
