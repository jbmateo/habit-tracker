package app.habittracker.android.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MobileOff
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import app.habittracker.domain.model.IconKeys

private val iconMap: Map<String, ImageVector> = mapOf(
    "run" to Icons.AutoMirrored.Rounded.DirectionsRun,
    "walk" to Icons.AutoMirrored.Rounded.DirectionsWalk,
    "gym" to Icons.Rounded.FitnessCenter,
    "bike" to Icons.AutoMirrored.Rounded.DirectionsBike,
    "swim" to Icons.Rounded.Pool,
    "yoga" to Icons.Rounded.SportsGymnastics,
    "meditate" to Icons.Rounded.SelfImprovement,
    "water" to Icons.Rounded.WaterDrop,
    "nutrition" to Icons.Rounded.Eco,
    "food" to Icons.Rounded.Restaurant,
    "sleep" to Icons.Rounded.Bedtime,
    "pill" to Icons.Rounded.Medication,
    "heart" to Icons.Rounded.Favorite,
    "book" to Icons.AutoMirrored.Rounded.MenuBook,
    "study" to Icons.Rounded.School,
    "code" to Icons.Rounded.Code,
    "language" to Icons.Rounded.Translate,
    "journal" to Icons.Rounded.EditNote,
    "music" to Icons.Rounded.MusicNote,
    "art" to Icons.Rounded.Brush,
    "pray" to Icons.Rounded.VolunteerActivism,
    "sun" to Icons.Rounded.WbSunny,
    "clean" to Icons.Rounded.CleaningServices,
    "money" to Icons.Rounded.Savings,
    "no_phone" to Icons.Rounded.MobileOff,
    "call" to Icons.Rounded.Call,
    "people" to Icons.Rounded.Group,
    "plant" to Icons.Rounded.LocalFlorist,
    "pet" to Icons.Rounded.Pets,
    "checklist" to Icons.Rounded.Checklist,
    "timer" to Icons.Rounded.Timer,
    "target" to Icons.Rounded.TrackChanges,
    "star" to Icons.Rounded.Star,
    "smile" to Icons.Rounded.Mood,
    "no_smoking" to Icons.Rounded.SmokeFree,
    "home" to Icons.Rounded.Home,
)

/** Exact lookup with no fallback; null when the key has no entry. */
internal fun iconVectorFor(key: String): ImageVector? = iconMap[key]

/** Unknown keys resolve to the checklist icon through [IconKeys.resolve]. */
fun habitIcon(key: String): ImageVector = iconMap.getValue(IconKeys.resolve(key))
