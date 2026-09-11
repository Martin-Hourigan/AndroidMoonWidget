package dev.mahourigan.moonwidget.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A compass dial: a filled face, north labelled, the other three cardinals
 * ticked (with fainter ticks at the diagonals for shape), and a dot that
 * sweeps round the rim to the Moon's current bearing.
 *
 * Deliberately not a full 16-point rose with every label — the text above
 * this already gives the precise reading (see [bearingText]). This is the
 * "which way, roughly" picture, at a glance.
 */
@Composable
fun CompassRose(
    azimuthDegrees: Double,
    modifier: Modifier = Modifier,
    diameter: Dp = 140.dp,
    dotColor: Color = MoonColors.accent,
) {
    val faceColor = MoonColors.surface
    val ringColor = MoonColors.muted
    val labelColor = MoonColors.text

    Canvas(modifier = modifier.size(diameter)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        // Leaves room outside the dial for the N label without clipping it.
        val radius = size.minDimension / 2f - 16.dp.toPx()

        drawCircle(color = faceColor.copy(alpha = 0.55f), radius = radius, center = center)
        drawCircle(
            color = ringColor.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        val star = compassStar(center, radius)
        drawPath(star, color = ringColor.copy(alpha = 0.14f))
        drawPath(star, color = ringColor.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))

        // Diagonals are drawn first and fainter, so once both are down the
        // cardinal ticks still read as the stronger marks.
        listOf(45.0, 135.0, 225.0, 315.0).forEach { bearing ->
            drawLine(
                color = ringColor.copy(alpha = 0.25f),
                start = pointOnCircle(center, radius - 5.dp.toPx(), bearing),
                end = pointOnCircle(center, radius, bearing),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        listOf(0.0, 90.0, 180.0, 270.0).forEach { bearing ->
            drawLine(
                color = ringColor.copy(alpha = 0.7f),
                start = pointOnCircle(center, radius - 9.dp.toPx(), bearing),
                end = pointOnCircle(center, radius, bearing),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 13.sp.toPx()
                isFakeBoldText = true
                color = labelColor.toArgb()
            }
            val metrics = paint.fontMetrics
            val north = pointOnCircle(center, radius + 13.dp.toPx(), 0.0)
            // Centre on the glyph itself rather than its baseline.
            val baseline = north.y - (metrics.ascent + metrics.descent) / 2f
            canvas.nativeCanvas.drawText("N", north.x, baseline, paint)
        }

        val marker = pointOnCircle(center, radius, azimuthDegrees)
        drawCircle(color = dotColor.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = marker)
        drawCircle(color = dotColor, radius = 5.dp.toPx(), center = marker)
    }
}

/**
 * The classic compass-rose badge: an eight-pointed star, long points at the
 * cardinals and short points at the diagonals, sitting inside the dial as
 * decoration. Fixed to the face itself — it never moves; the dot is what
 * points at the Moon.
 */
private fun compassStar(center: Offset, radius: Float): Path {
    val innerRadius = radius * 0.1f
    val longTip = radius * 0.88f
    val shortTip = radius * 0.5f

    return Path().apply {
        for (i in 0 until 16) {
            // Even indices are the eight points, alternating long and short;
            // odd indices are the waist between each pair of points.
            val pointRadius = when {
                i % 2 != 0 -> innerRadius
                (i / 2) % 2 == 0 -> longTip
                else -> shortTip
            }
            val vertex = pointOnCircle(center, pointRadius, i * 22.5)
            if (i == 0) moveTo(vertex.x, vertex.y) else lineTo(vertex.x, vertex.y)
        }
        close()
    }
}

/** A point [radius] out from [center], [bearingDegrees] clockwise from up. */
private fun pointOnCircle(center: Offset, radius: Float, bearingDegrees: Double): Offset {
    val radians = Math.toRadians(bearingDegrees)
    return Offset(
        x = center.x + radius * sin(radians).toFloat(),
        y = center.y - radius * cos(radians).toFloat(),
    )
}
