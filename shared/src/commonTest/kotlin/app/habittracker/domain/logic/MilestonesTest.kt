package app.habittracker.domain.logic

import app.habittracker.date
import app.habittracker.domain.model.MilestoneSeen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MilestonesTest {
    private val run = date("2026-10-11")


    @Test fun noMilestoneBelowSeven() = assertNull(pendingMilestone(6, run, null))

    @Test fun milestoneAtSeven() = assertEquals(7, pendingMilestone(7, run, null))

    @Test fun highestMilestoneWins() = assertEquals(30, pendingMilestone(40, run, null))

    @Test fun dismissedMilestoneStaysHidden() = assertNull(pendingMilestone(8, run, MilestoneSeen(run, 7)))

    @Test fun nextMilestoneAfterDismissedOne() = assertEquals(30, pendingMilestone(30, run, MilestoneSeen(run, 7)))

    @Test fun rebuiltRunShowsMilestoneAgain() =
        assertEquals(7, pendingMilestone(7, date("2026-11-01"), MilestoneSeen(run, 30)))
}
