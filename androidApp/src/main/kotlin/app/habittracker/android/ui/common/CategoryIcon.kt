package app.habittracker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.habittracker.android.ui.icons.habitIcon
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.android.ui.theme.categoryOnColor

/** A habit's icon as a solid circle in its category color, with a glyph that stays readable on it. Decorative. */
@Composable
fun CategoryIcon(iconKey: String, colorKey: String, size: Dp = 34.dp, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier.size(size).background(categoryColor(colorKey, dark), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(habitIcon(iconKey), contentDescription = null, tint = categoryOnColor(colorKey, dark), modifier = Modifier.size(size * 0.55f))
    }
}
