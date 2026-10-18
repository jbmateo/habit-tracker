package app.habittracker.android.ui.theme

import androidx.compose.ui.graphics.Color
import app.habittracker.domain.model.ColorKeys

private class CategoryPalette(val light: Color, val dark: Color)

private val palette: Map<String, CategoryPalette> = mapOf(
    "red" to CategoryPalette(Color(0xFFE5484D), Color(0xFFFF6369)),
    "orange" to CategoryPalette(Color(0xFFF76B15), Color(0xFFFF8B3E)),
    "amber" to CategoryPalette(Color(0xFFFFB224), Color(0xFFFFCA16)),
    "green" to CategoryPalette(Color(0xFF30A46C), Color(0xFF3DD68C)),
    "teal" to CategoryPalette(Color(0xFF12A594), Color(0xFF0BD8B6)),
    "blue" to CategoryPalette(Color(0xFF0090FF), Color(0xFF70B8FF)),
    "indigo" to CategoryPalette(Color(0xFF3E63DD), Color(0xFF849DFF)),
    "purple" to CategoryPalette(Color(0xFF8E4EC6), Color(0xFFBF7AF0)),
    "pink" to CategoryPalette(Color(0xFFD6409F), Color(0xFFFF8DCC)),
    "gray" to CategoryPalette(Color(0xFF8B8D98), Color(0xFFB0B4BA)),
)

/** Unknown keys resolve to gray through [ColorKeys.resolve]. */
fun categoryColor(key: String, dark: Boolean): Color {
    val entry = palette.getValue(ColorKeys.resolve(key))
    return if (dark) entry.dark else entry.light
}

/**
 * Readable foreground (black or white) for glyphs drawn on [categoryColor], chosen per mode by whichever has
 * the higher WCAG contrast against that fill.
 */
fun categoryOnColor(key: String, dark: Boolean): Color =
    if (contrastAgainstBlack(categoryColor(key, dark)) >= contrastAgainstWhite(categoryColor(key, dark))) Color.Black else Color.White

/** WCAG contrast ratio between the fill for [key] and its [categoryOnColor]. */
fun categoryOnColorContrast(key: String, dark: Boolean): Double {
    val fill = categoryColor(key, dark)
    return maxOf(contrastAgainstBlack(fill), contrastAgainstWhite(fill))
}

private fun contrastAgainstBlack(fill: Color) = (luminance(fill) + 0.05) / 0.05
private fun contrastAgainstWhite(fill: Color) = 1.05 / (luminance(fill) + 0.05)

/** WCAG relative luminance of an sRGB color. */
private fun luminance(c: Color): Double {
    fun channel(v: Float): Double = v.toDouble().let { if (it <= 0.03928) it / 12.92 else Math.pow((it + 0.055) / 1.055, 2.4) }
    return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
}
