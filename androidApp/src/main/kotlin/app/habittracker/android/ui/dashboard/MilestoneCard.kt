package app.habittracker.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.common.Flame
import app.habittracker.android.ui.common.MilestoneCopy
import app.habittracker.android.ui.theme.CardShape
import app.habittracker.android.ui.theme.DisplayFont
import app.habittracker.android.ui.theme.HabitTheme

/** A one-time streak milestone (7, 30, 100, 365 days), shown under the hero until dismissed. */
@Composable
fun MilestoneCard(milestone: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HabitTheme.colors
    val title = MilestoneCopy.title(milestone)
    val body = MilestoneCopy.body(milestone)
    Row(
        modifier.fillMaxWidth()
            .background(colors.hero, CardShape)
            .border(1.5.dp, colors.gold, CardShape)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Flame(milestone)
        Column(
            Modifier.weight(1f).padding(horizontal = 14.dp).clearAndSetSemantics {
                contentDescription = "Milestone: $title. $body"
            },
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = DisplayFont, fontWeight = FontWeight.Black, color = colors.gold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.heroText)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss milestone", tint = colors.heroSubtext)
        }
    }
}
