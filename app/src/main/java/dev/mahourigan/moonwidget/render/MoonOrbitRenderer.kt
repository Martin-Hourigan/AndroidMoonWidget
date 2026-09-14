package dev.mahourigan.moonwidget.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import dev.mahourigan.moonwidget.astronomy.MoonAppearance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Moon with the pass and the bearing drawn as rings around it.
 *
 * The alternative to the panel below the Moon. A dome can only show the half of
 * the cycle the Moon is up for, because it has nowhere to put the other half; a
 * full ring does. The bearing gains less from the change, but a widget that
 * shows one reading as a ring and the other as a dial looks like an accident,
 * so both are offered.
 *
 * Rings cost the disc rather than the layout: each one takes [RING_STEP_DP] off
 * the Moon's radius, so the whole assembly occupies exactly the square the Moon
 * alone would have filled and nothing above or beside it has to move.
 */
object MoonOrbitRenderer {

    /** Clearance between the Moon's edge and each ring, in dp. */
    const val RING_STEP_DP = 9f

    /**
     * Colours for the rings.
     *
     * Separate from [MoonRenderer.Palette] because these are lines on the
     * background, not parts of the Moon.
     */
    data class Palette(
        /** The arc the Moon is on now. */
        val track: Int,
        /** The arc it is not: below the horizon, drawn dotted. */
        val trackFaint: Int,
        /** The horizon itself, and the compass ticks. */
        val horizon: Int,
        /** North, which reaches further so the ring has an orientation. */
        val north: Int,
        /** The Moon's own position, and the bearing diamond. */
        val marker: Int,
        /** Painted behind the marker so it reads over the track it sits on. */
        val ground: Int,
    )

    /**
     * What to draw. A null means that ring is not wanted.
     *
     * @param passFraction how far through the pass, 0 at the rise and 1 at the set.
     * @param moonIsUp which half of the ring [passFraction] is measured along.
     * @param azimuthDegrees bearing, clockwise from north.
     */
    data class Rings(
        val passFraction: Double?,
        val moonIsUp: Boolean,
        val azimuthDegrees: Double?,
    ) {
        val count: Int
            get() = (if (passFraction != null) 1 else 0) +
                (if (azimuthDegrees != null) 1 else 0)
    }

    /**
     * @param sizePx width and height of the returned square bitmap. This is the
     *   Moon's whole footprint, rings included, not the diameter of the disc.
     */
    fun render(
        context: Context,
        sizePx: Int,
        density: Float,
        appearance: MoonAppearance,
        rings: Rings,
        moonPalette: MoonRenderer.Palette,
        palette: Palette,
        symbol: String? = null,
        blendSymbol: Boolean = false,
        useTexture: Boolean = false,
    ): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val side = size.toFloat()
        val centre = side / 2f
        val step = RING_STEP_DP * density

        // The bearing diamond and the horizon's overhang both sit outside the
        // outermost ring, so that ring cannot be allowed to reach the edge or
        // they are drawn half off the bitmap.
        val inset = 5f * density
        val usable = side - 2f * inset

        // Never let the rings eat the Moon entirely: on a tile resized to the
        // 110dp minimum the disc is small enough already. When that floor binds
        // the rings share what room is left rather than overlapping the disc.
        val moonSide = (usable - 2f * step * rings.count).coerceAtLeast(usable * 0.4f)
        val gap = (usable - moonSide) / 2f / rings.count.coerceAtLeast(1)

        var radius = moonSide / 2f
        var skyRadius = 0f
        var compassRadius = 0f
        if (rings.passFraction != null) {
            radius += gap
            skyRadius = radius
        }
        if (rings.azimuthDegrees != null) {
            radius += gap
            compassRadius = radius
        }

        // Tracks first. The sky ring's horizon runs through the centre, and it
        // should pass behind the Moon rather than be ruled across its face.
        if (skyRadius > 0f) drawSkyTrack(canvas, centre, skyRadius, density, palette)
        if (compassRadius > 0f) drawCompassTrack(canvas, centre, compassRadius, density, palette)

        canvas.save()
        canvas.translate((side - moonSide) / 2f, (side - moonSide) / 2f)
        MoonRenderer.draw(
            canvas, context, moonSide, appearance, moonPalette,
            symbol, blendSymbol, useTexture,
        )
        canvas.restore()

        // Markers last, so neither is buried under the disc.
        rings.passFraction?.let {
            drawSkyMarker(canvas, centre, skyRadius, density, it, rings.moonIsUp, palette)
        }
        rings.azimuthDegrees?.let {
            drawCompassMarker(canvas, centre, compassRadius, density, it, palette)
        }
        return bitmap
    }

    /**
     * The whole cycle as one ring: solid where the Moon is above the horizon,
     * dotted where it is below, with the horizon drawn across the middle.
     *
     * Android measures arcs clockwise from three o'clock with y running down,
     * so 0 to 180 is the lower half and 180 to 360 the upper.
     */
    private fun drawSkyTrack(
        canvas: Canvas,
        centre: Float,
        radius: Float,
        density: Float,
        palette: Palette,
    ) {
        val box = RectF(centre - radius, centre - radius, centre + radius, centre + radius)
        val stroke = 1.2f * density

        val below = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = palette.trackFaint
            pathEffect = DashPathEffect(floatArrayOf(1.5f * density, 2.5f * density), 0f)
        }
        canvas.drawArc(box, 0f, 180f, false, below)

        val above = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = palette.track
        }
        canvas.drawArc(box, 180f, 180f, false, above)

        // Its ends are the rise and the set, so it runs slightly proud of the ring.
        val horizon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = palette.horizon
        }
        val overhang = 2.5f * density
        canvas.drawLine(
            centre - radius - overhang, centre,
            centre + radius + overhang, centre,
            horizon,
        )
    }

    /** Where the Moon is on that ring: left at the rise, top at the culmination. */
    private fun drawSkyMarker(
        canvas: Canvas,
        centre: Float,
        radius: Float,
        density: Float,
        fraction: Double,
        moonIsUp: Boolean,
        palette: Palette,
    ) {
        val clamped = fraction.coerceIn(0.0, 1.0)
        // Up: left to right over the top. Down: right to left under the bottom,
        // which is the continuation of the same journey rather than a reset.
        val angle = if (moonIsUp) PI - PI * clamped else -PI * clamped
        val x = centre + radius * cos(angle).toFloat()
        val y = centre - radius * sin(angle).toFloat()

        canvas.drawCircle(
            x, y, 4.2f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.ground
            },
        )
        canvas.drawCircle(
            x, y, 2.7f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.marker
            },
        )
    }

    /**
     * The compass ring: four ticks pointing inward, north longer and brighter.
     *
     * No letters. At widget scale an "N" is six dp of smudge, and a tick that
     * reaches further says the same thing legibly.
     */
    private fun drawCompassTrack(
        canvas: Canvas,
        centre: Float,
        radius: Float,
        density: Float,
        palette: Palette,
    ) {
        canvas.drawCircle(
            centre, centre, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * density
                color = palette.horizon
            },
        )

        listOf(0, 90, 180, 270).forEach { degrees ->
            val angle = Math.toRadians(degrees - 90.0)
            val isNorth = degrees == 0
            val length = if (isNorth) 5f * density else 2.8f * density
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * density
                color = if (isNorth) palette.north else palette.horizon
            }
            canvas.drawLine(
                centre + (cos(angle) * radius).toFloat(),
                centre + (sin(angle) * radius).toFloat(),
                centre + (cos(angle) * (radius - length)).toFloat(),
                centre + (sin(angle) * (radius - length)).toFloat(),
                paint,
            )
        }
    }

    /** The bearing, as a small diamond with its long axis pointing outward. */
    private fun drawCompassMarker(
        canvas: Canvas,
        centre: Float,
        radius: Float,
        density: Float,
        azimuthDegrees: Double,
        palette: Palette,
    ) {
        val angle = Math.toRadians(azimuthDegrees - 90.0)
        val long = 4f * density
        val short = 2.6f * density

        val path = Path().apply {
            moveTo(long, 0f)
            lineTo(0f, -short)
            lineTo(-long, 0f)
            lineTo(0f, short)
            close()
        }

        canvas.save()
        canvas.translate(
            centre + (cos(angle) * radius).toFloat(),
            centre + (sin(angle) * radius).toFloat(),
        )
        canvas.rotate((azimuthDegrees - 90.0).toFloat())
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.marker
            },
        )
        canvas.restore()
    }
}
