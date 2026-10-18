package app.habittracker.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.habittracker.domain.model.WeekComparison
import kotlin.math.roundToInt

/** "This week 86% · Last week 71%  ↑ 15 pts"; hidden when this week has no data yet. */
@Composable
fun WeekComparisonLine(week: WeekComparison, modifier: Modifier = Modifier) {
    val thisWeek = week.thisWeek?.let { (it * 100).roundToInt() } ?: return
    val lastWeek = week.lastWeek?.let { (it * 100).roundToInt() }
    val summary = "This week $thisWeek%" + (lastWeek?.let { " · Last week $it%" } ?: "")
    val delta = lastWeek?.let { thisWeek - it }
    val (trend, trendColor) = when {
        delta == null -> null to Color.Unspecified
        delta > 0 -> "↑ $delta pts" to Color(0xFF2E9E5B)
        delta < 0 -> "↓ ${-delta} pts" to MaterialTheme.colorScheme.error
        else -> "→ same" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier.clearAndSetSemantics {
            contentDescription = "Completion $summary" + (trend?.let { ", $it" } ?: "")
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trend != null) Text(trend, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = trendColor)
    }
}
