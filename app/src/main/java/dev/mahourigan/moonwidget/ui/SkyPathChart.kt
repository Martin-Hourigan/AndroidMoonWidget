package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.astronomy.SkyPass
import dev.mahourigan.moonwidget.astronomy.SkyPoint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Moon's height through one lunar day, above the horizon and below it.
 *
 * The horizon is a straight line across the chart; the hidden stretch is drawn
 * under it, faint and dashed. Because the horizon cuts the Moon's daily circle
 * off-centre, the two stretches are rarely the same width — which is the point
 * of drawing it at all.
 */
@Composable
fun SkyPathChart(
    points: List<SkyPoint>,
    pass: SkyPass?,
    now: Instant,
    zone: ZoneId,
    force24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val context = LocalContext.current
    val palette = MoonColors

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            drawSkyPath(
                points = points,
                now = now,
                horizonColor = palette.muted,
                aboveColor = palette.moon,
                belowColor = palette.muted,
                markerColor = palette.accent,
            )
        }

        Spacer(Modifier.height(10.dp))

        // The rise and set times sit under the chart rather than on it: labels
        // inside a 120dp box collide with the curve at some declinations.
        pass?.let {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.path_rises),
                        color = palette.muted,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = TimeFormattingUi.short(it.rise, zone, context, force24Hour),
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.path_sets),
                        color = palette.muted,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = TimeFormattingUi.short(it.set, zone, context, force24Hour),
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.path_up_down,
                    hoursAndMinutes(it.duration),
                    hoursAndMinutes(lunarDayRemainder(it.duration)),
                ),
                color = palette.muted,
                fontSize = 12.sp,
            )
        }

        val peak = points.maxOf { it.altitude }
        if (peak > 0) {
            Text(
                text = stringResource(R.string.path_peak, peak.roundToInt()),
                color = palette.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun DrawScope.drawSkyPath(
    points: List<SkyPoint>,
    now: Instant,
    horizonColor: androidx.compose.ui.graphics.Color,
    aboveColor: androidx.compose.ui.graphics.Color,
    belowColor: androidx.compose.ui.graphics.Color,
    markerColor: androidx.compose.ui.graphics.Color,
) {
    val start = points.first().time
    val span = Duration.between(start, points.last().time).toMillis().toDouble()
    if (span <= 0) return

    // Symmetric vertical scale so the horizon sits in the middle. The visual
    // question is how much of the path is up versus down, and an asymmetric
    // scale would quietly distort exactly that.
    val extreme = points.maxOf { abs(heightAbovePlane(it.altitude)) }.coerceAtLeast(0.05)
    val topPad = 10f
    val usable = size.height - topPad * 2
    val horizonY = topPad + usable / 2f

    fun xFor(time: Instant): Float =
        (Duration.between(start, time).toMillis() / span).toFloat() * size.width

    fun yFor(altitude: Double): Float =
        horizonY - (heightAbovePlane(altitude) / extreme).toFloat() * (usable / 2f)

    // The two stretches are drawn as separate runs rather than one path, so the
    // hidden one can be dashed without dashing the whole curve, and so the fill
    // under the visible one follows the same smoothed edge as its stroke.
    var run = mutableListOf<SkyPoint>()
    var runIsAbove = points.first().altitude >= 0

    fun flush() {
        if (run.size < 2) return
        val screen = run.map { Offset(xFor(it.time), yFor(it.altitude)) }
        val curve = smoothCurveThrough(screen)

        if (runIsAbove) {
            // Shade under the visible stretch, closing the same curve down to
            // the horizon so the top edge of the fill matches the stroke.
            val filled = Path().apply {
                addPath(curve)
                lineTo(screen.last().x, horizonY)
                lineTo(screen.first().x, horizonY)
                close()
            }
            drawPath(filled, aboveColor.copy(alpha = 0.16f))
        }

        drawPath(
            path = curve,
            color = if (runIsAbove) aboveColor else belowColor.copy(alpha = 0.55f),
            style = Stroke(
                width = if (runIsAbove) 3f else 2f,
                pathEffect = if (runIsAbove) null else PathEffect.dashPathEffect(floatArrayOf(6f, 7f)),
            ),
        )
    }

    points.forEach { point ->
        val above = point.altitude >= 0
        if (above != runIsAbove) {
            // Carry the crossing point into both runs so there is no visible gap.
            run.add(point)
            flush()
            run = mutableListOf(run.last())
            runIsAbove = above
        }
        run.add(point)
    }
    flush()

    drawLine(
        color = horizonColor.copy(alpha = 0.7f),
        start = Offset(0f, horizonY),
        end = Offset(size.width, horizonY),
        strokeWidth = 1.5f,
    )

    // Where the Moon is at this moment.
    val nowX = xFor(now)
    val nowAltitude = points.minByOrNull {
        abs(Duration.between(it.time, now).toMillis())
    }?.altitude ?: 0.0

    drawLine(
        color = markerColor.copy(alpha = 0.4f),
        start = Offset(nowX, topPad),
        end = Offset(nowX, size.height - topPad),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f)),
    )
    drawCircle(color = markerColor, radius = 6f, center = Offset(nowX, yFor(nowAltitude)))
}

/**
 * Height above the horizon plane, from an altitude in degrees.
 *
 * The chart plots this rather than the angle. Altitude is an arcsine, and
 * arcsine steepens without limit as it nears 90 degrees, so a pass that
 * culminates close to overhead — as it does from Newcastle every month — draws
 * as a spike with two straight flanks and a corner on top. That shape is
 * correct and it looks broken.
 *
 * `sin(altitude)` is the vertical component of the direction to the Moon. Hour
 * angle runs at a constant rate, so this is very nearly a pure cosine in time
 * and comes out smooth on its own, with no averaging or fudging of the data.
 * Zero still means the horizon and the crossings still land in the same places;
 * only the vertical spacing changes. The peak angle is given in words beneath,
 * where a number is more use than a pixel height anyway.
 */
private fun heightAbovePlane(altitudeDegrees: Double): Double =
    sin(altitudeDegrees * PI / 180.0)

/**
 * A rounded curve through every one of [screen], in order.
 *
 * Straight segments between samples leave visible corners, worst of all at the
 * top of a pass that culminates near the zenith — the altitude really does turn
 * sharply there, and ten-minute sampling renders that as a hard angle. This is a
 * Catmull-Rom spline converted to cubic Béziers: each control point is a sixth
 * of the way along the line joining a point's two neighbours, which is what
 * makes the join smooth. It passes through the data rather than approximating
 * it, so the shape stays honest — only the faceting goes.
 */
private fun smoothCurveThrough(screen: List<Offset>): Path {
    val path = Path()
    if (screen.isEmpty()) return path

    path.moveTo(screen.first().x, screen.first().y)
    if (screen.size == 2) {
        path.lineTo(screen[1].x, screen[1].y)
        return path
    }

    for (index in 0 until screen.size - 1) {
        // Ends have no outer neighbour, so they stand in for themselves.
        val previous = screen[(index - 1).coerceAtLeast(0)]
        val from = screen[index]
        val to = screen[index + 1]
        val following = screen[(index + 2).coerceAtMost(screen.size - 1)]

        path.cubicTo(
            from.x + (to.x - previous.x) / 6f,
            from.y + (to.y - previous.y) / 6f,
            to.x - (following.x - from.x) / 6f,
            to.y - (following.y - from.y) / 6f,
            to.x,
            to.y,
        )
    }
    return path
}

/** What is left of a lunar day once the visible pass is taken out. */
private fun lunarDayRemainder(above: Duration): Duration =
    dev.mahourigan.moonwidget.astronomy.SkyPath.LUNAR_DAY.minus(above)

private fun hoursAndMinutes(duration: Duration): String {
    val total = duration.toMinutes().coerceAtLeast(0)
    return "${total / 60}h ${total % 60}m"
}

/** Sized for a label, not a sentence. */
private object TimeFormattingUi {
    fun short(
        instant: Instant,
        zone: ZoneId,
        context: android.content.Context,
        force24Hour: Boolean,
    ): String = dev.mahourigan.moonwidget.render.TimeFormatting.format(
        instant, zone, context, force24Hour,
    )
}
