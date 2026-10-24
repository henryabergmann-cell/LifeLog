package app.lifelog.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            if (values.isEmpty()) return@Canvas
            val max = values.max().coerceAtLeast(1)
            val slot = size.width / values.size
            val barWidth = slot * 0.6f
            values.forEachIndexed { index, value ->
                val barHeight = size.height * value / max
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = if (index == highlightIndex) highlightColor else barColor,
                        topLeft = Offset(slot * index + (slot - barWidth) / 2, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 4, barWidth / 4),
                    )
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
