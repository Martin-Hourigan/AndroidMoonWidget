package dev.mahourigan.moonwidget.render

import dev.mahourigan.moonwidget.astronomy.MoonAppearance
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import kotlin.math.abs

/**
 * Draws the Moon into a [Bitmap].
 *
 * A widget is backed by RemoteViews, which cannot run arbitrary drawing code,
 * so the widget renders the Moon to a bitmap and shows it as an image. The app
 * draws the same geometry with Compose instead — see `ui/MoonShape.kt`. Both
 * share [MoonAppearance] so they can never disagree about the shape.
 */
object MoonRenderer {

    /**
     * Colours to draw with, always supplied by the caller.
     *
     * No defaults on purpose: a default here would render a Midnight-coloured
     * Moon under every theme, and it would do it silently.
     */
    data class Palette(
        val lit: Int,
        val shadow: Int,
        /** Faint outline so a new moon is still visible against its background. */
        val outline: Int,
        /** Zodiac glyph where it crosses the lit face. */
        val symbolOnLit: Int,
        /** And where it crosses the dark side, so it reads across both. */
        val symbolOnShadow: Int,
        /** One tone midway between the two, for the un-inverted style. */
        val symbolBlended: Int,
    )

    /**
     * @param sizePx width and height of the returned square bitmap.
     * @param appearance geometry to draw, from [MoonAppearance.forPhase].
     */
    fun render(
        context: Context,
        sizePx: Int,
        appearance: MoonAppearance,
        palette: Palette,
        symbol: String? = null,
        blendSymbol: Boolean = false,
        useTexture: Boolean = false,
    ): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas, context, size.toFloat(), appearance, palette, symbol, blendSymbol, useTexture)
        return bitmap
    }

    /**
     * Draw into an existing canvas, filling a square of [side] pixels.
     */
    fun draw(
        canvas: Canvas,
        context: Context,
        side: Float,
        appearance: MoonAppearance,
        palette: Palette,
        symbol: String? = null,
        blendSymbol: Boolean = false,
        useTexture: Boolean = false,
    ) {
        // Leave a little breathing room so the outline stroke is not clipped.
        val radius = side / 2f * 0.94f
        val centre = side / 2f

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (side * 0.008f).coerceAtLeast(1f)
            color = palette.outline
        }

        // Turn the whole disc to the Moon's real angle in the sky. Zero unless
        // true orientation is switched on, and the outline is a circle either
        // way, so nothing is clipped by rotating inside the same square.
        val turned = appearance.rotationDegrees != 0.0
        if (turned) {
            canvas.save()
            canvas.rotate(appearance.rotationDegrees.toFloat(), centre, centre)
        }

        if (useTexture) {
            // The texture covers the whole disc uniformly; the unlit side is
            // dimmed with a translucent scrim rather than blocked out by a
            // flat fill, so its shading and colour still show faintly
            // through instead of vanishing.
            drawTexturedDisc(canvas, context, centre, radius, palette.lit, mirror = !appearance.litOnRight)

            val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = withAlpha(palette.shadow, DARK_SIDE_ALPHA)
            }

            when {
                appearance.isEffectivelyNew -> canvas.drawCircle(centre, centre, radius, dim)
                appearance.isEffectivelyFull -> Unit
                else -> {
                    canvas.save()
                    canvas.clipOutPath(litPath(centre, radius, appearance))
                    canvas.drawCircle(centre, centre, radius, dim)
                    canvas.restore()
                }
            }
        } else {
            // The unlit disc always sits underneath.
            fill.color = palette.shadow
            canvas.drawCircle(centre, centre, radius, fill)

            when {
                appearance.isEffectivelyNew -> {
                    // Nothing lit — just the faint rim so the widget isn't empty.
                }

                appearance.isEffectivelyFull -> {
                    fill.color = palette.lit
                    canvas.drawCircle(centre, centre, radius, fill)
                }

                else -> {
                    fill.color = palette.lit
                    canvas.drawPath(litPath(centre, radius, appearance), fill)
                }
            }
        }

        canvas.drawCircle(centre, centre, radius, stroke)

        symbol?.let {
            drawSymbol(canvas, it, centre, radius, appearance, palette, blendSymbol,
                if (turned) appearance.rotationDegrees.toFloat() else 0f)
        }

        if (turned) canvas.restore()
    }

    /**
     * The zodiac glyph over the disc, matching the app's drawing.
     *
     * Clipped and drawn twice in opposing colours so it stays legible over both
     * halves — which half is which changes with the phase — unless the blended
     * style is chosen, where one mid tone runs straight across.
     *
     * Called inside the rotated canvas so the clip lines up with the lit shape,
     * then counter-rotated by the same angle so the glyph itself stays upright
     * while the Moon under it does not.
     */
    private fun drawSymbol(
        canvas: Canvas,
        symbol: String,
        centre: Float,
        radius: Float,
        appearance: MoonAppearance,
        palette: Palette,
        blendSymbol: Boolean,
        rotationDegrees: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = radius * 1.05f
        }

        fun glyph(color: Int) {
            paint.color = color
            canvas.save()
            canvas.rotate(-rotationDegrees, centre, centre)
            val metrics = paint.fontMetrics
            canvas.drawText(symbol, centre, centre - (metrics.ascent + metrics.descent) / 2f, paint)
            canvas.restore()
        }

        when {
            blendSymbol -> glyph(palette.symbolBlended)
            appearance.isEffectivelyNew -> glyph(palette.symbolOnShadow)
            appearance.isEffectivelyFull -> glyph(palette.symbolOnLit)
            else -> {
                val path = litPath(centre, radius, appearance)
                canvas.save()
                canvas.clipPath(path)
                glyph(palette.symbolOnLit)
                canvas.restore()

                canvas.save()
                // clipOutPath rather than the deprecated DIFFERENCE region op;
                // available from API 26, which is the app minimum.
                canvas.clipOutPath(path)
                glyph(palette.symbolOnShadow)
                canvas.restore()
            }
        }
    }

    /** How opaque the dark-side scrim is, out of 255 — dark, but not a total block. */
    private const val DARK_SIDE_ALPHA = 214

    /**
     * [MoonTexture], scaled to the disc and multiplied by [tint].
     *
     * Multiply rather than a plain draw: the texture is painted in shades of
     * white, so multiplying it by the theme's Moon colour keeps the shading —
     * craters, maria — while taking on whatever hue the theme actually uses.
     *
     * @param mirror flips the photograph left-right, to match a lit shape
     *   that was itself mirrored for a southern-hemisphere "upright" view —
     *   see [litPath]. Only the flat-fill shape mirrors on its own by being
     *   built from a pre-mirrored path; a bitmap needs the canvas mirrored
     *   around it instead.
     */
    private fun drawTexturedDisc(
        canvas: Canvas,
        context: Context,
        centre: Float,
        radius: Float,
        tint: Int,
        mirror: Boolean = false,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.MULTIPLY)
        }
        val dst = RectF(centre - radius, centre - radius, centre + radius, centre + radius)
        if (mirror) {
            canvas.save()
            canvas.scale(-1f, 1f, centre, centre)
        }
        canvas.drawBitmap(MoonTexture.bitmap(context), null, dst, paint)
        if (mirror) canvas.restore()
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    /**
     * The lit region: a semicircular limb joined to a semi-elliptical terminator.
     *
     * Built assuming the lit side is on the right, then mirrored if it isn't —
     * which keeps the arc-direction reasoning in one place.
     */
    private fun litPath(centre: Float, radius: Float, appearance: MoonAppearance): Path {
        val disc = RectF(centre - radius, centre - radius, centre + radius, centre + radius)

        // Signed horizontal semi-axis of the terminator ellipse.
        // Positive: terminator bulges past centre (gibbous). Negative: crescent.
        val semiAxis = appearance.terminatorCurveFactor.toFloat() * radius
        val terminator = RectF(
            centre - abs(semiAxis),
            centre - radius,
            centre + abs(semiAxis),
            centre + radius,
        )

        val path = Path().apply {
            // Right limb, 12 o'clock clockwise to 6 o'clock.
            addArc(disc, -90f, 180f)

            // Terminator back up to 12 o'clock. Which half of the ellipse we
            // need depends on the sign: gibbous uses the far (left) half,
            // crescent the near (right) half.
            val sweep = if (semiAxis >= 0f) 180f else -180f
            arcTo(terminator, 90f, sweep)

            close()
        }

        if (!appearance.litOnRight) {
            path.transform(
                Matrix().apply { setScale(-1f, 1f, centre, centre) }
            )
        }
        return path
    }
}
