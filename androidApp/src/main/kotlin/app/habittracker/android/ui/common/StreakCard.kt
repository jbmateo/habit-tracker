package app.habittracker.android.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.habittracker.domain.model.StreakInfo

@Composable
fun StreakCard(streak: StreakInfo, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("🔥 ${streak.current}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("day streak", style = MaterialTheme.typography.labelMedium)
            Text("Best ${streak.best}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
