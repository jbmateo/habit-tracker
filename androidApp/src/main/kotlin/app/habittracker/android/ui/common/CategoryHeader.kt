package app.habittracker.android.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.theme.categoryColor
import androidx.compose.foundation.isSystemInDarkTheme
import app.habittracker.domain.model.Category

@Composable
fun CategoryHeader(category: Category, modifier: Modifier = Modifier) {
    Text(
        text = category.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = categoryColor(category.colorKey, isSystemInDarkTheme()),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
