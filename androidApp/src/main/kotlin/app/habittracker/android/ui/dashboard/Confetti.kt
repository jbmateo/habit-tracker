package app.habittracker.android.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.random.Random

private const val DURATION_S = 1.8f
private const val GRAVITY_DP = 900f

private class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val size: Float, val color: Color)

/** A short confetti burst from near the top of Today, each time [trigger] changes. Decorative: no touches, no semantics. */
@Composable
fun Confetti(trigger: Int, colors: List<Color>, modifier: Modifier = Modifier) {
    var pieces by remember { mutableStateOf(emptyList<Piece>()) }
    var elapsed by remember { mutableFloatStateOf(DURATION_S) }
    val density = LocalDensity.current.density

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val palette = colors.ifEmpty { listOf(Color(0xFFEF9F27)) }
        pieces = List(60) {
            Piece(
                x = Random.nextFloat() * 0.5f + 0.25f,
                vx = Random.nextFloat() * 520f - 260f,
                vy = -(Random.nextFloat() * 360f + 260f),
                spin = Random.nextFloat() * 24f - 12f,
                size = Random.nextFloat() * 5f + 7f,
                color = palette.random(),
            )
        }
        val start = withFrameNanos { it }
        elapsed = 0f
        while (elapsed < DURATION_S) {
            withFrameNanos { now -> elapsed = (now - start) / 1_000_000_000f }
        }
        pieces = emptyList()
    }

    if (pieces.isEmpty()) return
    Canvas(modifier.clearAndSetSemantics {}) {
        val t = elapsed
        val fade = (1f - t / DURATION_S).coerceIn(0f, 1f)
        for (p in pieces) {
            val x = p.x * size.width + p.vx * density * t
            val y = 140f * density + p.vy * density * t + GRAVITY_DP * density * t * t / 2f
            val w = p.size * density
            rotate(degrees = p.spin * t * 57.3f, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = p.color.copy(alpha = fade),
                    topLeft = Offset(x - w / 2, y - w / 4),
                    size = Size(w, w / 2),
                    cornerRadius = CornerRadius(1.5f * density),
                )
            }
        }
    }
}
