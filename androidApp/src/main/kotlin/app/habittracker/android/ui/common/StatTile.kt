package app.habittracker.android.ui.common

import app.habittracker.android.ui.theme.HabitTheme
import app.habittracker.android.ui.theme.DisplayFont
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.domain.logic.completionLevel
import app.habittracker.domain.model.CompletionLevel

/** Value-over-label stat tile, tinted with the palette color [colorKey]; used by the calendar and habit detail. */
@Composable
fun StatTile(value: String, label: String, colorKey: String, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val color = categoryColor(colorKey, dark)
    // The meaning color washes over a card base, so tiles read the same on the canvas in both modes.
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = HabitTheme.colors.card) {
        Column(Modifier.background(color.copy(alpha = if (dark) 0.22f else 0.12f)).padding(12.dp)) {
            Text(value, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 22.sp, color = color, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Palette key for a completion rate: green ≥ 80%, amber (yellow) 50–79%, red < 50%, gray without data. */
fun completionColorKey(rate: Double?): String = when (completionLevel(rate)) {
    CompletionLevel.Good -> "green"
    CompletionLevel.Fair -> "amber"
    CompletionLevel.Low -> "red"
    null -> "gray"
}
