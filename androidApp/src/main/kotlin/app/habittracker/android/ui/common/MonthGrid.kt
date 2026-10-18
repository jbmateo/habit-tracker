package app.habittracker.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/** "October 2026". */
fun monthTitle(month: YearMonth): String =
    "${java.time.Month.of(month.month.ordinal + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"

/** Monday-first month grid. [leadingBlanks] empty cells precede the 1st; [cells] is one composable per day. */
@Composable
fun MonthGrid(
    month: YearMonth,
    leadingBlanks: Int,
    cells: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    val total = leadingBlanks + cells.size
    val rows = (total + 6) / 7
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Calendar for ${monthTitle(month)}" }) {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        repeat(rows) { row ->
            // One height per week row so adjacent cells (and streak bands) line up to the pixel.
            Row(Modifier.fillMaxWidth().aspectRatio(7f)) {
                repeat(7) { col ->
                    val index = row * 7 + col - leadingBlanks
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        cells.getOrNull(index)?.takeIf { index >= 0 }?.invoke()
                    }
                }
            }
        }
    }
}

/** Month title with previous/next buttons; next is disabled once [canGoNext] is false. */
@Composable
fun MonthHeader(
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(monthTitle(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}
