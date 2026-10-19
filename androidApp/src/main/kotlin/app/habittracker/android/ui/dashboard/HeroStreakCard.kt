package app.habittracker.android.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.habittracker.android.ui.common.Flame
import app.habittracker.android.ui.common.WeekComparisonLine
import app.habittracker.android.ui.theme.CardShape
import app.habittracker.android.ui.theme.DisplayFont
import app.habittracker.android.ui.theme.HabitTheme
import app.habittracker.android.ui.theme.rememberAnimationsDisabled
import app.habittracker.domain.usecase.TodayDashboard
import kotlinx.coroutines.withContext

/**
 * Today's indigo hero: date, the perfect-day streak with its flame, today's progress and the week comparison.
 * Each change of [pulseTrigger] (other than the first) flashes a gold outline — the stand-in for confetti
 * when animations are off.
 */
@Composable
fun HeroStreakCard(dashboard: TodayDashboard, dateText: String, pulseTrigger: Int, modifier: Modifier = Modifier) {
    val colors = HabitTheme.colors
    val animationsOff = rememberAnimationsDisabled()
    val streak = dashboard.streak.current
    val fraction = if (dashboard.due == 0) 0f else dashboard.done.toFloat() / dashboard.due
    val progress by animateFloatAsState(fraction, if (animationsOff) tween(0) else tween(450), label = "progress")
    val barColor by animateColorAsState(if (dashboard.isPerfect) colors.gold else colors.flame, label = "bar")

    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger == 0) return@LaunchedEffect
        // The pulse is what replaces confetti when animations are off, and Compose scales every animation by the
        // system animator scale (0 = instant). Run this short fade on an unscaled clock so it stays visible.
        withContext(UnscaledMotion) {
            pulse.animateTo(1f, tween(100))
            pulse.animateTo(1f, tween(150))
            pulse.animateTo(0f, tween(400))
        }
    }

    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().background(colors.hero, CardShape).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(dateText, style = MaterialTheme.typography.titleSmall, color = colors.heroSubtext)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(
                    Modifier.weight(1f).clearAndSetSemantics {
                        contentDescription = "$streak day streak, best ${dashboard.streak.best}"
                    },
                ) {
                    AnimatedContent(
                        targetState = streak,
                        transitionSpec = {
                            if (animationsOff) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            else (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        },
                        label = "streak",
                    ) { value ->
                        Text(
                            "$value",
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp,
                            lineHeight = 48.sp,
                            color = colors.heroText,
                        )
                    }
                    Text(
                        "day streak · best ${dashboard.streak.best}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.heroSubtext,
                    )
                }
                Flame(streak)
            }
            if (dashboard.due > 0) {
                Box(
                    Modifier.fillMaxWidth().height(10.dp).background(colors.heroTrack, CircleShape)
                        .clearAndSetSemantics { contentDescription = "${dashboard.done} of ${dashboard.due} habits done" },
                ) {
                    if (progress > 0f) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(barColor, CircleShape))
                    }
                }
                Text(
                    if (dashboard.isPerfect) "All done today ✨" else "${dashboard.done} of ${dashboard.due} done today",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dashboard.isPerfect) colors.gold else colors.heroText,
                )
            }
            WeekComparisonLine(dashboard.week, onHero = true)
        }
        Box(
            Modifier.matchParentSize().alpha(pulse.value).border(3.dp, colors.gold, CardShape),
        )
    }
}

private object UnscaledMotion : MotionDurationScale {
    override val scaleFactor: Float = 1f
}
