package app.habittracker.android.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ring showing [done] of [due]. Bumping [celebrateTrigger] plays a short scale pulse. */
@Composable
fun ProgressRing(done: Int, due: Int, celebrateTrigger: Int, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val target = if (due == 0) 0f else (done.toFloat() / due).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, label = "ring")
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(celebrateTrigger) {
        if (celebrateTrigger > 0) {
            pulse.animateTo(1.2f)
            pulse.animateTo(1f)
        }
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(size)
            .scale(pulse.value)
            .semantics { contentDescription = "$done of $due habits done" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = this.size.minDimension * 0.12f, cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            drawArc(color, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = stroke)
        }
        Text("$done/$due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
