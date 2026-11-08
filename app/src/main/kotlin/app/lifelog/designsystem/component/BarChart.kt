package app.lifelog.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.lifelog.designsystem.motion.Motion
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple vertical bar chart. [description] is what TalkBack reads (the chart's data as
 * text), since the drawn bars carry no semantics. Bars at [highlightIndex] use [highlightColor].
 */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    description: String,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    highlightIndex: Int? = null,
    chartHeight: Dp = 140.dp,
    labelEvery: Int = 1,
) {
    val baseline = MaterialTheme.colorScheme.outlineVariant
    // When the data changes (another range picked), bars move from their old heights to the
    // new ones instead of jumping, so you can see what went up and what went down. A chart with
    // a different number of bars grows in from the baseline. The first appearance is static.
    var from by remember { mutableStateOf(values.map(Int::toFloat)) }
    var to by remember { mutableStateOf(values) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(values) {
        if (values == to) return@LaunchedEffect
        val p = progress.value
        from = if (values.size == to.size) to.indices.map { lerp(from.getOrElse(it) { 0f }, to[it].toFloat(), p) } else List(values.size) { 0f }
        to = values
        progress.snapTo(0f)
        progress.animateTo(1f, Motion.move())
    }
    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            if (to.isEmpty()) return@Canvas
            val p = progress.value
            val shown = to.indices.map { lerp(from.getOrElse(it) { 0f }, to[it].toFloat(), p) }
            val max = lerp(from.maxOrNull() ?: 0f, to.max().toFloat(), p).coerceAtLeast(1f)
            val slot = size.width / to.size
            val barWidth = slot * 0.6f
            // Only the tops are rounded, so bars stand on the baseline instead of floating.
            val radius = CornerRadius(minOf(barWidth / 4, 6.dp.toPx()))
            shown.forEachIndexed { index, value ->
                val barHeight = size.height * value / max
                if (barHeight > 0f) {
                    val left = slot * index + (slot - barWidth) / 2
                    val bar = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = size.height - barHeight,
                                right = left + barWidth,
                                bottom = size.height,
                                topLeftCornerRadius = radius,
                                topRightCornerRadius = radius,
                            ),
                        )
                    }
                    drawPath(bar, color = if (index == highlightIndex) highlightColor else barColor)
                }
            }
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = if (index % labelEvery == 0) label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
