package app.habittracker.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.theme.HabitTheme

/** How big the streak flame is drawn: it grows with the perfect-day streak (spec §3). */
enum class FlameTier(val size: Dp) {
    None(28.dp), Small(28.dp), Medium(36.dp), Large(44.dp), Legendary(44.dp);

    companion object {
        fun forStreak(streak: Int): FlameTier = when {
            streak < 1 -> None
            streak < 7 -> Small
            streak < 30 -> Medium
            streak < 100 -> Large
            else -> Legendary
        }
    }
}

/** The streak flame: an outline with no streak, filled otherwise, with a gold core from 100 days. Decorative. */
@Composable
fun Flame(streak: Int, modifier: Modifier = Modifier) {
    val tier = FlameTier.forStreak(streak)
    val colors = HabitTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(
            if (tier == FlameTier.None) Icons.Outlined.LocalFireDepartment else Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (tier == FlameTier.None) colors.heroSubtext else colors.flame,
            modifier = Modifier.size(tier.size),
        )
        if (tier == FlameTier.Legendary) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(tier.size * 0.5f).offset(y = tier.size * 0.12f),
            )
        }
    }
}

/** Copy for the milestone card (spec §5). */
object MilestoneCopy {
    fun title(milestone: Int): String = "$milestone-day streak"

    fun body(milestone: Int): String = when (milestone) {
        7 -> "One week strong"
        30 -> "A month of perfect days"
        100 -> "100 days. Legendary."
        else -> "A whole year"
    }
}
