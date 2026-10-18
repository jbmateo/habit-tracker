package app.habittracker.android.ui.common

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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.icons.habitIcon
import app.habittracker.android.ui.theme.categoryColor
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
    val color = categoryColor(item.habit.category.colorKey, isSystemInDarkTheme())
    Row(
        modifier = modifier
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(habitIcon(item.habit.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(item.habit.name, style = MaterialTheme.typography.bodyLarge)
            if (status != null) {
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (item.streak > 0) {
            Text("🔥 ${item.streak}", style = MaterialTheme.typography.labelLarge)
        }
        IconButton(
            onClick = onToggle,
            enabled = toggleEnabled,
            modifier = Modifier.border(1.5.dp, if (item.isDone) color else MaterialTheme.colorScheme.outlineVariant, CircleShape).semantics {
                contentDescription = (if (item.isDone) "Mark not done: " else "Mark done: ") + item.habit.name
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (item.isDone) color else Color.Transparent,
            ),
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = if (item.isDone) Color.White else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
