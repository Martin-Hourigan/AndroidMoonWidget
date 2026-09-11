package dev.mahourigan.moonwidget.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The compass dial, drawn into a [Bitmap] for the widget.
 *
 * Mirrors `ui/CompassRose.kt`, which does the same job with Compose for the
 * app, in the same way [MoonRenderer] mirrors `ui/MoonShape.kt`.
 *
 * Carries no north label, unlike the app's. The star does that job on its own —
 * a compass rose's long point reads as north without being told — and dropping
 * the letter frees the margin it needed, so the dial itself comes out larger in
 * the same small tile.
 */
object CompassRenderer {

    data class Palette(
        val ring: Int,
        val label: Int,
        val marker: Int,
        /** Dimmed marker, for when the Moon is below the horizon. */
        val markerDim: Int,
    )

    fun render(
        widthPx: Int,
        heightPx: Int,
        azimuthDegrees: Double,
        moonIsUp: Boolean,
        palette: Palette,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centreX = width / 2f
        val centreY = height / 2f
        // Only the marker's own overhang has to be left for now that the label
        // is gone, so the dial fills very nearly the whole tile.
        val radius = min(width, height) / 2f * 0.78f

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (radius * 0.05f).coerceAtLeast(1f)
            color = withAlpha(palette.ring, 140)
        }
        canvas.drawCircle(centreX, centreY, radius, stroke)

        // The rose itself. Filled faintly and outlined, exactly as the app
        // draws it — at this size the outline is what actually reads, and the
        // fill only keeps it from looking like bare wireframe.
        val star = starPath(centreX, centreY, radius)
        canvas.drawPath(
            star,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(palette.ring, 46) },
        )
        canvas.drawPath(
            star,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (radius * 0.035f).coerceAtLeast(1f)
                color = withAlpha(palette.ring, 150)
            },
        )

        // Cardinal ticks only. The star's short points already sit on the
        // diagonals, so the app's faint diagonal ticks would just double up.
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (radius * 0.07f).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
            color = withAlpha(palette.ring, 190)
        }
        listOf(0.0, 90.0, 180.0, 270.0).forEach { bearing ->
            val inner = pointOn(centreX, centreY, radius * 0.86f, bearing)
            val outer = pointOn(centreX, centreY, radius, bearing)
            canvas.drawLine(inner.first, inner.second, outer.first, outer.second, tick)
        }

        // The Moon's bearing. Dimmed while it is down, matching the app: the
        // dot is then showing where it will rise, not where it is.
        val markerColor = if (moonIsUp) palette.marker else palette.markerDim
        val marker = pointOn(centreX, centreY, radius, azimuthDegrees)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(markerColor, 80) }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = markerColor }

        canvas.drawCircle(marker.first, marker.second, radius * 0.24f, glow)
        canvas.drawCircle(marker.first, marker.second, radius * 0.15f, dot)

        return bitmap
    }

    /**
     * The eight-pointed rose, long points at the cardinals and short ones at
     * the diagonals. Same construction as `compassStar` in `ui/CompassRose.kt`.
     */
    private fun starPath(centreX: Float, centreY: Float, radius: Float): Path {
        val inner = radius * 0.1f
        val longTip = radius * 0.88f
        val shortTip = radius * 0.5f

        return Path().apply {
            for (i in 0 until 16) {
                // Even indices are the eight points, alternating long and
                // short; odd ones are the waist between each pair.
                val pointRadius = when {
                    i % 2 != 0 -> inner
                    (i / 2) % 2 == 0 -> longTip
                    else -> shortTip
                }
                val (x, y) = pointOn(centreX, centreY, pointRadius, i * 22.5)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

    /** A point [radius] out, [bearingDegrees] clockwise from straight up. */
    private fun pointOn(
        centreX: Float,
        centreY: Float,
        radius: Float,
        bearingDegrees: Double,
    ): Pair<Float, Float> {
        val radians = Math.toRadians(bearingDegrees)
        return (centreX + radius * sin(radians).toFloat()) to
            (centreY - radius * cos(radians).toFloat())
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
