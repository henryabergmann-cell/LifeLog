package app.lifelog.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lifelog.R
import app.lifelog.domain.model.Category

/** A preset category color with a spoken name, so color is never the only cue. */
data class PresetColor(val argb: Int, @param:StringRes val name: Int)

object CategoryPalette {
    val presets: List<PresetColor> = listOf(
        PresetColor(0xFFD50000.toInt(), R.string.color_tomato),
        PresetColor(0xFFE67C73.toInt(), R.string.color_flamingo),
        PresetColor(0xFFF4511E.toInt(), R.string.color_tangerine),
        PresetColor(0xFFF6BF26.toInt(), R.string.color_banana),
        PresetColor(0xFF33B679.toInt(), R.string.color_sage),
        PresetColor(0xFF0B8043.toInt(), R.string.color_basil),
        PresetColor(0xFF039BE5.toInt(), R.string.color_peacock),
        PresetColor(0xFF3F51B5.toInt(), R.string.color_blueberry),
        PresetColor(0xFF7986CB.toInt(), R.string.color_lavender),
        PresetColor(0xFF8E24AA.toInt(), R.string.color_grape),
        PresetColor(0xFF616161.toInt(), R.string.color_graphite),
        PresetColor(0xFF006A6A.toInt(), R.string.color_teal),
    )

    fun nameFor(argb: Int): Int? = presets.firstOrNull { it.argb == argb }?.name
}

/** Background color for an event: its category's color, else the theme's primary color. */
@Composable
fun eventColor(categoryId: Long?, categories: Map<Long, Category>): Color =
    categoryId?.let(categories::get)?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary

/** Readable text color on top of [background]. */
fun contentColorOn(background: Color): Color = if (background.luminance() > 0.4f) Color(0xFF1B1B1B) else Color.White

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(
        modifier
            .size(size)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}
