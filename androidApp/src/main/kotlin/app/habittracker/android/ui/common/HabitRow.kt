package app.habittracker.android.ui.common

import app.habittracker.android.ui.theme.rememberAnimationsDisabled
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.android.ui.theme.categoryOnColor
import app.habittracker.domain.usecase.TodayHabitItem

@Composable
fun HabitRow(
    item: TodayHabitItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    toggleEnabled: Boolean = true,
    status: String? = null,
) {
    val dark = isSystemInDarkTheme()
    val color = categoryColor(item.habit.category.colorKey, dark)
    val animationsOff = rememberAnimationsDisabled()
    // Done rows on Today read as finished; the calendar's day panel (which shows a status) keeps plain text.
    val struck = item.isDone && status == null
    val fill by animateFloatAsState(if (item.isDone) 1f else 0f, if (animationsOff) tween(150) else spring(dampingRatio = 0.7f), label = "fill")
    val bounce = remember { Animatable(1f) }
    var lastDone by remember { mutableStateOf(item.isDone) }
    LaunchedEffect(item.isDone) {
        if (item.isDone && !lastDone && !animationsOff) {
            bounce.animateTo(1.25f, tween(120))
            bounce.animateTo(1f, spring(dampingRatio = 0.5f))
        } else if (bounce.value != 1f) {
            // A quick un-check cancels a bounce mid-way; settle back to rest instead of staying enlarged.
            bounce.snapTo(1f)
        }
        lastDone = item.isDone
    }
    Row(
        modifier = modifier
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(item.habit.iconKey, item.habit.category.colorKey)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.habit.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (struck) TextDecoration.LineThrough else null,
                color = if (struck) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (status != null) {
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (item.streak > 0) {
            Text("🔥 ${item.streak}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
        }
        IconButton(
            onClick = onToggle,
            enabled = toggleEnabled,
            modifier = Modifier
                .scale(bounce.value)
                // The category fill grows out from the centre (a plain fade with animations off).
                .drawBehind {
                    val grow = if (animationsOff) 1f else fill
                    drawCircle(color, radius = size.minDimension / 2 * grow, alpha = fill.coerceIn(0f, 1f))
                }
                .border(1.5.dp, if (item.isDone) color else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .semantics {
                    contentDescription = (if (item.isDone) "Mark not done: " else "Mark done: ") + item.habit.name
                },
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = if (item.isDone) categoryOnColor(item.habit.category.colorKey, dark) else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
