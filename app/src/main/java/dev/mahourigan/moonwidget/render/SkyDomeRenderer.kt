package dev.mahourigan.moonwidget.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Moon's visible pass, drawn as a dome for the widget.
 *
 * A flat horizon with a semicircle over it, a tick where the arc meets the
 * ground at each end, and a line up the middle marking the high point. The
 * marker sits at the fraction of the pass already elapsed.
 *
 * The arc is a stylised semicircle rather than the true altitude curve. That
 * is a fair approximation and not a fudge: placing the marker at angle
 * `pi * fraction` puts its height at `sin(pi * fraction)`, and the real
 * altitude through a pass is close to that same shape. At widget size the
 * difference is under a pixel, and the full curve is drawn honestly in the app.
 *
 * Only the visible half is shown here — the widget answers "how far through
 * tonight is it", and the hidden half is the app's job.
 */
object SkyDomeRenderer {

    data class Palette(
        val arc: Int,
        val horizon: Int,
        val marker: Int,
    )

    /**
     * @param widthPx width of the returned bitmap.
     * @param heightPx height, including the room below the horizon for the ticks.
     * @param fraction how far through the pass, 0 at the rise and 1 at the set.
     * @param moonIsUp false when the pass has not started, which parks the
     *   marker on the ground at the rise end instead of on the arc.
     */
    fun render(
        widthPx: Int,
        heightPx: Int,
        fraction: Double,
        moonIsUp: Boolean,
        palette: Palette,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(8)
        val height = heightPx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), width.toFloat(), height.toFloat(), fraction, moonIsUp, palette)
        return bitmap
    }

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        fraction: Double,
        moonIsUp: Boolean,
        palette: Palette,
    ) {
        val markerRadius = height * 0.09f
        val inset = markerRadius + 1f
        // Room under the horizon for the end ticks to drop into.
        val horizonY = height - height * 0.18f
        val left = inset
        val right = width - inset
        // A true semicircle, not an ellipse stretched to whatever box it was
        // handed: the smaller of the two half-axes wins and the arc is centred.
        // The horizon still spans the full width, so the dome sits on ground
        // that runs past it.
        val radius = minOf((right - left) / 2f, horizonY - inset)
        if (radius <= 0) return

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (height * 0.028f).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
        }

        // The dome.
        stroke.color = palette.arc
        val centreX = (left + right) / 2f
        val oval = RectF(centreX - radius, horizonY - radius, centreX + radius, horizonY + radius)
        canvas.drawArc(oval, 180f, 180f, false, stroke)

        // Horizon.
        val ground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (height * 0.022f).coerceAtLeast(1f)
            color = palette.horizon
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(0f, horizonY, width, horizonY, ground)

        // A tick at each end, where the arc meets the ground, and one up the
        // middle for the high point.
        val tick = height * 0.13f
        val archLeft = centreX - radius
        val archRight = centreX + radius
        canvas.drawLine(archLeft, horizonY - tick * 0.55f, archLeft, horizonY + tick * 0.55f, ground)
        canvas.drawLine(archRight, horizonY - tick * 0.55f, archRight, horizonY + tick * 0.55f, ground)

        val middle = Paint(ground).apply {
            pathEffect = DashPathEffect(floatArrayOf(height * 0.06f, height * 0.05f), 0f)
        }
        canvas.drawLine(centreX, horizonY - radius, centreX, horizonY, middle)

        // The Moon itself.
        val clamped = fraction.coerceIn(0.0, 1.0)
        val angle = PI * clamped
        val markerX = centreX - radius * cos(angle).toFloat()
        val markerY = if (moonIsUp) horizonY - radius * sin(angle).toFloat() else horizonY

        canvas.drawCircle(
            markerX,
            markerY,
            markerRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.marker
            },
        )
    }
}
