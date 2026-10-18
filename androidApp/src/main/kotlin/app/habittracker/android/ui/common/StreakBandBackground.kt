package app.habittracker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.presentation.calendar.StreakBand

/**
 * Tinted band behind consecutive streak days on a month grid, in the palette color [colorKey];
 * rounded only on the band's outer ends. Draw it first inside a full-size grid cell.
 */
@Composable
fun StreakBandBackground(band: StreakBand, colorKey: String) {
    if (band == StreakBand.None) return
    val pill = 50
    val shape = when (band) {
        StreakBand.Single -> RoundedCornerShape(percent = pill)
        StreakBand.Start -> RoundedCornerShape(topStartPercent = pill, bottomStartPercent = pill)
        StreakBand.End -> RoundedCornerShape(topEndPercent = pill, bottomEndPercent = pill)
        else -> RoundedCornerShape(0)
    }
    val dark = isSystemInDarkTheme()
    val tint = categoryColor(colorKey, dark).copy(alpha = if (dark) 0.55f else 0.25f)
    val roundStart = band == StreakBand.Single || band == StreakBand.Start
    val roundEnd = band == StreakBand.Single || band == StreakBand.End
    Box(
        Modifier
            .fillMaxSize()
            .padding(vertical = 3.dp)
            .padding(start = if (roundStart) 3.dp else 0.dp, end = if (roundEnd) 3.dp else 0.dp)
            .background(tint, shape),
    )
}
