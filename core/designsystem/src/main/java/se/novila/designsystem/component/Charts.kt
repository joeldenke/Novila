package se.novila.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import kotlin.math.min
import kotlin.math.sqrt

private val ChartHeight = 132.dp
private val DotInset = 10.dp

/**
 * A week (or any short series) as a smooth curve with a soft fill that draws itself in from the
 * left. Null values are days without data; the curve passes over them. The last point is today
 * and gets a halo. An optional [goal] is a dashed line. Read as [contentDescription] by screen
 * readers, so describe what the chart shows in words.
 */
@Composable
fun NovilaLineChart(
    values: List<Float?>,
    labels: List<String>,
    valueRange: ClosedFloatingPointRange<Float>,
    role: NovilaColorRole,
    contentDescription: String,
    modifier: Modifier = Modifier,
    goal: Float? = null,
) {
    val reveal by rememberReveal()
    val line = role.color
    val card = NovilaTheme.surfaces.card
    val grid = NovilaTheme.surfaces.cardOutline
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
            if (values.isEmpty()) return@Canvas
            val inset = DotInset.toPx()
            val top = inset
            val bottom = size.height - inset
            val step = size.width / values.size
            val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
            fun y(value: Float) = bottom - (value.coerceIn(valueRange) - valueRange.start) / span * (bottom - top)

            drawGrid(grid, top, bottom)
            goal?.let { drawGoal(line, y(it)) }

            val points = values.mapIndexedNotNull { i, v -> v?.let { Offset(step * (i + 0.5f), y(it)) } }
            if (points.isEmpty()) return@Canvas
            val curve = smoothPath(points)
            val fill = Path().apply {
                addPath(curve)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            val front = size.width * reveal
            clipRect(right = front) {
                drawPath(
                    fill,
                    Brush.verticalGradient(listOf(line.copy(alpha = 0.24f), line.copy(alpha = 0f)), top, size.height),
                )
                drawPath(
                    curve,
                    line,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            points.forEachIndexed { i, point ->
                // Each dot appears as the line reaches it.
                val shown = ((front - point.x) / (step * 0.5f)).coerceIn(0f, 1f)
                if (shown > 0f) drawDot(point, line, card, today = i == points.lastIndex, scale = shown)
            }
        }
        ChartLabels(labels)
    }
}

/**
 * Bars that grow one after another, on soft tracks. Bars at or above the [goal] are solid,
 * the others lighter, so good days stand out without reading any numbers.
 */
@Composable
fun NovilaBarChart(
    values: List<Float?>,
    labels: List<String>,
    maxValue: Float,
    role: NovilaColorRole,
    contentDescription: String,
    modifier: Modifier = Modifier,
    goal: Float? = null,
) {
    val reveal by rememberReveal(durationMillis = 1400)
    val bar = role.color
    val track = role.container
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
            if (values.isEmpty()) return@Canvas
            val step = size.width / values.size
            val width = min(step * 0.46f, 24.dp.toPx())
            val radius = CornerRadius(width / 2f)
            val stagger = 0.1f
            values.forEachIndexed { i, value ->
                val left = step * (i + 0.5f) - width / 2f
                drawRoundRect(track, Offset(left, 0f), Size(width, size.height), radius)
                val grow = (reveal * (1f + stagger * (values.size - 1)) - stagger * i).coerceIn(0f, 1f)
                val fraction = ((value ?: 0f) / maxValue).coerceIn(0f, 1f) * grow
                if (fraction > 0f) {
                    val height = (size.height * fraction).coerceAtLeast(width)
                    val reached = goal == null || (value ?: 0f) >= goal
                    drawRoundRect(
                        color = if (reached) bar else bar.copy(alpha = 0.5f),
                        topLeft = Offset(left, size.height - height),
                        size = Size(width, height),
                        cornerRadius = radius,
                    )
                }
            }
            goal?.let { drawGoal(bar, size.height * (1f - (it / maxValue).coerceIn(0f, 1f))) }
        }
        ChartLabels(labels)
    }
}

/** A small curve without axes, for tiles. Needs at least two values to draw anything. */
@Composable
internal fun NovilaSparkline(
    values: List<Float?>,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reveal by rememberReveal(delayMillis = 200)
    Canvas(modifier) {
        val inset = 3.dp.toPx()
        val step = if (values.size > 1) (size.width - 2 * inset) / (values.size - 1) else 0f
        val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
        val points = values.mapIndexedNotNull { i, v ->
            v?.let {
                val fraction = (it.coerceIn(valueRange) - valueRange.start) / span
                Offset(inset + step * i, size.height - inset - fraction * (size.height - 2 * inset))
            }
        }
        if (points.size < 2) return@Canvas
        val curve = smoothPath(points)
        clipRect(right = size.width * reveal) {
            drawPath(curve, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (reveal >= 1f) drawCircle(color, radius = 3.dp.toPx(), center = points.last())
    }
}

@Composable
private fun ChartLabels(labels: List<String>) {
    val style = NovilaTheme.typography.labelSmall
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            val today = i == labels.lastIndex
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = if (today) style.copy(fontWeight = FontWeight.Bold) else style,
                color = if (today) NovilaTheme.colors.onSurface else NovilaTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun DrawScope.drawGrid(color: Color, top: Float, bottom: Float) {
    val stroke = 1.dp.toPx()
    for (k in 0..2) {
        val y = top + (bottom - top) * k / 2f
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
    }
}

private fun DrawScope.drawGoal(color: Color, y: Float) {
    val dash = 4.dp.toPx()
    drawLine(
        color = color.copy(alpha = 0.55f),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 1.5f)),
    )
}

private fun DrawScope.drawDot(center: Offset, color: Color, fill: Color, today: Boolean, scale: Float) {
    val radius = (if (today) 5.5.dp else 3.5.dp).toPx() * scale
    if (today) drawCircle(color.copy(alpha = 0.18f), radius = 12.dp.toPx() * scale, center = center)
    drawCircle(fill, radius = radius, center = center)
    drawCircle(color, radius = radius, center = center, style = Stroke(width = 2.dp.toPx() * scale))
}

/** A smooth curve through [points] (sorted by x) that never overshoots them. */
internal fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    val slopes = monotoneSlopes(
        FloatArray(points.size) { points[it].x },
        FloatArray(points.size) { points[it].y },
    )
    for (i in 0 until points.lastIndex) {
        val a = points[i]
        val b = points[i + 1]
        val dx = (b.x - a.x) / 3f
        path.cubicTo(a.x + dx, a.y + slopes[i] * dx, b.x - dx, b.y - slopes[i + 1] * dx, b.x, b.y)
    }
    return path
}

/**
 * Tangent slopes for a monotone cubic Hermite curve (Fritsch–Carlson): where the data rises or
 * falls the curve does too, and peaks and valleys stay at the data points instead of bulging past
 * them. That keeps a week of sleep honest: the curve never shows a night longer than the longest.
 */
internal fun monotoneSlopes(xs: FloatArray, ys: FloatArray): FloatArray {
    val n = xs.size
    if (n < 2) return FloatArray(n)
    val secants = FloatArray(n - 1) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]) }
    val slopes = FloatArray(n)
    slopes[0] = secants[0]
    slopes[n - 1] = secants[n - 2]
    for (i in 1 until n - 1) {
        slopes[i] = if (secants[i - 1] * secants[i] <= 0f) 0f else (secants[i - 1] + secants[i]) / 2f
    }
    for (i in 0 until n - 1) {
        if (secants[i] == 0f) {
            slopes[i] = 0f
            slopes[i + 1] = 0f
        } else {
            val a = slopes[i] / secants[i]
            val b = slopes[i + 1] / secants[i]
            val length = a * a + b * b
            if (length > 9f) {
                val t = 3f / sqrt(length)
                slopes[i] = t * a * secants[i]
                slopes[i + 1] = t * b * secants[i]
            }
        }
    }
    return slopes
}
