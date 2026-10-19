package app.habittracker.android.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** The redesign's identity (spec §3): a cool canvas, cards and a deep indigo hero, light and dark. */
@Immutable
data class HabitColors(
    val canvas: Color,
    val card: Color,
    val hero: Color,
    val heroText: Color,
    val heroSubtext: Color,
    val heroTrack: Color,
    val flame: Color,
    val gold: Color,
    val accent: Color,
)

val LightHabitColors = HabitColors(
    canvas = Color(0xFFF4F3F8),
    card = Color(0xFFFFFFFF),
    hero = Color(0xFF3C3489),
    heroText = Color(0xFFEEEDFE),
    heroSubtext = Color(0xFFCECBF6),
    heroTrack = Color(0xFF534AB7),
    flame = Color(0xFFEF9F27),
    gold = Color(0xFFFFB224),
    accent = Color(0xFF3C3489),
)

val DarkHabitColors = HabitColors(
    canvas = Color(0xFF13121C),
    card = Color(0xFF1E1C2B),
    hero = Color(0xFF2A2466),
    heroText = Color(0xFFEEEDFE),
    heroSubtext = Color(0xFFCECBF6),
    heroTrack = Color(0xFF3C3489),
    flame = Color(0xFFEF9F27),
    gold = Color(0xFFFFCA16),
    accent = Color(0xFFAFA9EC),
)

val LocalHabitColors = staticCompositionLocalOf { LightHabitColors }

/** Cards across the app. */
val CardShape = RoundedCornerShape(20.dp)

/** Display numbers; Android has no system rounded face, so a heavy sans stands in. */
val DisplayFont: FontFamily = FontFamily.SansSerif

object HabitTheme {
    val colors: HabitColors
        @Composable get() = LocalHabitColors.current
}

private fun lightScheme(c: HabitColors) = lightColorScheme(
    primary = c.accent,
    onPrimary = Color.White,
    // Material draws the selected tab label and indicator from the secondary roles.
    secondary = c.accent,
    secondaryContainer = Color(0xFFE3E1FA),
    onSecondaryContainer = c.accent,
    background = c.canvas,
    surface = c.card,
    surfaceContainer = c.card,
    surfaceContainerLow = c.card,
)

private fun darkScheme(c: HabitColors) = darkColorScheme(
    primary = c.accent,
    onPrimary = Color(0xFF13121C),
    secondary = c.accent,
    secondaryContainer = Color(0xFF2F2B55),
    onSecondaryContainer = c.accent,
    background = c.canvas,
    surface = c.card,
    surfaceContainer = c.card,
    surfaceContainerLow = c.card,
)

@Composable
fun HabitTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkHabitColors else LightHabitColors
    CompositionLocalProvider(LocalHabitColors provides colors) {
        MaterialTheme(colorScheme = if (darkTheme) darkScheme(colors) else lightScheme(colors), content = content)
    }
}

/** "Remove animations" in Developer/Accessibility settings sets the animator scale to 0. */
fun animationsDisabled(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

@Composable
fun rememberAnimationsDisabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        animationsDisabled(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}
