package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.astronomy.MoonAppearance
import dev.mahourigan.moonwidget.render.MoonTexture
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The Moon, drawn with Compose.
 *
 * Mirrors `render/MoonRenderer.kt`, which does the same job with
 * `android.graphics` for the widget. Both take the geometry from
 * [MoonAppearance] so the two renderings always agree.
 */
@Composable
fun MoonShape(
    appearance: MoonAppearance,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    litColor: Color = MoonColors.moon,
    shadowColor: Color = MoonColors.moonShadow,
    /** Draw craters and maria across the lit face instead of a flat fill. */
    useTexture: Boolean = false,
    /** Ease into a changed angle instead of snapping to it. */
    animateRotation: Boolean = true,
    /** Zodiac glyph to lay over the disc, or null for none. */
    symbol: String? = null,
    /** Used where the glyph crosses the lit face. */
    symbolOnLit: Color = MoonColors.background,
    /** Used where it crosses the dark side, so it stays readable across both. */
    symbolOnShadow: Color = MoonColors.moon,
    /** Draw the glyph in one tone midway between the two, instead of inverting. */
    blendSymbol: Boolean = false,
) {
    val context = LocalContext.current
    val target = appearance.rotationDegrees.toFloat()

    // Animating the raw angle would unwind the long way round whenever it wraps
    // — 350 degrees to 10 is a 20 degree roll, not a 340 degree one. Tracking an
    // unwrapped value and always stepping by the short arc keeps it honest.
    var continuous by remember { mutableFloatStateOf(target) }
    LaunchedEffect(target) {
        continuous += ((target - continuous + 540f) % 360f) - 180f
    }

    val rotation by animateFloatAsState(
        targetValue = if (animateRotation) continuous else target,
        animationSpec = tween(durationMillis = ROTATION_MILLIS, easing = FastOutSlowInEasing),
        label = "moonRotation",
    )

    Canvas(
        modifier = modifier
            .size(diameter)
            .graphicsLayer { rotationZ = rotation }
    ) {
        drawMoon(
            appearance, litColor, shadowColor, useTexture, context, symbol,
            symbolOnLit, symbolOnShadow, rotation, blendSymbol,
        )
    }
}

/** Slow enough to read as the disc turning, short enough not to hold anything up. */
private const val ROTATION_MILLIS = 900

/** How opaque the dark-side scrim is — dark, but not a total block. Matches [MoonRenderer]. */
private const val DARK_SIDE_ALPHA = 0.84f

private fun DrawScope.drawMoon(
    appearance: MoonAppearance,
    litColor: Color,
    shadowColor: Color,
    useTexture: Boolean = false,
    context: android.content.Context? = null,
    symbol: String? = null,
    symbolOnLit: Color = Color.Black,
    symbolOnShadow: Color = Color.White,
    rotationDegrees: Float = 0f,
    blendSymbol: Boolean = false,
) {
    val side = min(size.width, size.height)
    val radius = side / 2f * 0.94f
    val centre = Offset(size.width / 2f, size.height / 2f)

    if (useTexture && context != null) {
        // The texture covers the whole disc uniformly; the unlit side is
        // dimmed with a translucent scrim rather than blocked out by a flat
        // fill, so its shading and colour still show faintly through instead
        // of vanishing.
        if (appearance.litOnRight) {
            drawTexturedDisc(context, centre, radius, litColor)
        } else {
            // Mirror horizontally about the centre, to match a lit shape
            // built the same way — see the dim-overlay clip below.
            scale(scaleX = -1f, scaleY = 1f, pivot = centre) {
                drawTexturedDisc(context, centre, radius, litColor)
            }
        }

        val dimColor = shadowColor.copy(alpha = DARK_SIDE_ALPHA)
        when {
            appearance.isEffectivelyNew -> drawCircle(color = dimColor, radius = radius, center = centre)
            appearance.isEffectivelyFull -> Unit
            else -> {
                val path = litPath(centre, radius, appearance).let { built ->
                    if (appearance.litOnRight) built else built.mirroredAbout(centre)
                }
                clipPath(path, ClipOp.Difference) {
                    drawCircle(color = dimColor, radius = radius, center = centre)
                }
            }
        }
    } else {
        drawCircle(color = shadowColor, radius = radius, center = centre)

        when {
            appearance.isEffectivelyNew -> Unit

            appearance.isEffectivelyFull ->
                drawCircle(color = litColor, radius = radius, center = centre)

            else -> {
                val path = litPath(centre, radius, appearance)
                if (appearance.litOnRight) {
                    drawPath(path, litColor)
                } else {
                    // Mirror horizontally about the centre.
                    scale(scaleX = -1f, scaleY = 1f, pivot = centre) {
                        drawPath(path, litColor)
                    }
                }
            }
        }
    }

    drawCircle(
        color = litColor.copy(alpha = Palette.OUTLINE_ALPHA),
        radius = radius,
        center = centre,
        style = Stroke(width = (side * 0.008f).coerceAtLeast(1f)),
    )

    symbol?.let {
        drawSymbol(
            it, centre, radius, appearance,
            symbolOnLit, symbolOnShadow, rotationDegrees,
            // The midpoint of the Moon's own two tones, so the glyph reads as
            // part of the disc rather than laid on top of it.
            blended = if (blendSymbol) lerp(litColor, shadowColor, 0.5f) else null,
        )
    }
}

/**
 * The zodiac glyph, laid over the disc.
 *
 * Drawn twice and clipped, once to the lit region and once to everything else,
 * in opposing colours. A single colour would be invisible over one half or the
 * other — and which half changes nightly with the phase — so the glyph instead
 * inverts as it crosses the terminator.
 *
 * The disc may be turned to the Moon's real angle in the sky, and that turn is
 * applied to the whole canvas by the caller. The glyph is counter-rotated by
 * the same amount so it stays upright and readable while the Moon underneath it
 * does not.
 */
private fun DrawScope.drawSymbol(
    symbol: String,
    centre: Offset,
    radius: Float,
    appearance: MoonAppearance,
    onLit: Color,
    onShadow: Color,
    rotationDegrees: Float,
    blended: Color?,
) {
    fun paint(color: Color) = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = radius * 1.05f
        this.color = color.toArgb()
    }

    fun DrawScope.glyph(color: Color) {
        rotate(degrees = -rotationDegrees, pivot = centre) {
            drawIntoCanvas { canvas ->
                val p = paint(color)
                val metrics = p.fontMetrics
                // Centre on the glyph itself rather than its baseline.
                val baseline = centre.y - (metrics.ascent + metrics.descent) / 2f
                canvas.nativeCanvas.drawText(symbol, centre.x, baseline, p)
            }
        }
    }

    when {
        // One tone across the whole disc: softer, and no seam at the terminator.
        blended != null -> glyph(blended)
        appearance.isEffectivelyNew -> glyph(onShadow)
        appearance.isEffectivelyFull -> glyph(onLit)
        else -> {
            val path = litPath(centre, radius, appearance).let { built ->
                if (appearance.litOnRight) built else built.mirroredAbout(centre)
            }
            clipPath(path, ClipOp.Intersect) { glyph(onLit) }
            clipPath(path, ClipOp.Difference) { glyph(onShadow) }
        }
    }
}

/** The lit path is always built lit-on-the-right; this is the other case. */
private fun Path.mirroredAbout(centre: Offset): Path {
    val matrix = android.graphics.Matrix().apply {
        setScale(-1f, 1f, centre.x, centre.y)
    }
    return Path().also { out ->
        out.addPath(this)
        out.asAndroidPath().transform(matrix)
    }
}

/**
 * [MoonTexture], scaled to the disc and tinted with [tint].
 *
 * Multiply rather than a plain draw: the texture is painted in shades of
 * white, so multiplying it by the theme's Moon colour keeps the shading —
 * craters, maria — while taking on whatever hue the theme actually uses.
 */
private fun DrawScope.drawTexturedDisc(context: android.content.Context, centre: Offset, radius: Float, tint: Color) {
    drawImage(
        image = moonTextureImage(context),
        dstOffset = IntOffset((centre.x - radius).roundToInt(), (centre.y - radius).roundToInt()),
        dstSize = IntSize((radius * 2).roundToInt(), (radius * 2).roundToInt()),
        colorFilter = ColorFilter.tint(tint, BlendMode.Modulate),
    )
}

/** [MoonTexture], converted for Compose and cached — the bitmap never changes. */
private var cachedTextureImage: ImageBitmap? = null

private fun moonTextureImage(context: android.content.Context): ImageBitmap {
    cachedTextureImage?.let { return it }
    val image = MoonTexture.bitmap(context.applicationContext).asImageBitmap()
    cachedTextureImage = image
    return image
}

/**
 * Lit region: the illuminated limb (a semicircle) closed by the terminator
 * (a semi-ellipse). Always built lit-on-the-right; the caller mirrors it.
 */
private fun litPath(centre: Offset, radius: Float, appearance: MoonAppearance): Path {
    val disc = Rect(centre, radius)
    val semiAxis = appearance.terminatorCurveFactor.toFloat() * radius

    val terminator = Rect(
        offset = Offset(centre.x - abs(semiAxis), centre.y - radius),
        size = Size(abs(semiAxis) * 2f, radius * 2f),
    )

    return Path().apply {
        // Right limb: 12 o'clock round to 6 o'clock.
        arcTo(disc, startAngleDegrees = -90f, sweepAngleDegrees = 180f, forceMoveTo = true)
        // Terminator back to 12 o'clock — far half when gibbous, near half when crescent.
        arcTo(
            rect = terminator,
            startAngleDegrees = 90f,
            sweepAngleDegrees = if (semiAxis >= 0f) 180f else -180f,
            forceMoveTo = false,
        )
        close()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1026, widthDp = 200, heightDp = 200)
@Composable
private fun PreviewWaxingCrescent() {
    MoonShape(MoonAppearance.forPhase(phaseAngle = 45.0))
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1026, widthDp = 200, heightDp = 200)
@Composable
private fun PreviewFirstQuarter() {
    MoonShape(MoonAppearance.forPhase(phaseAngle = 90.0))
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1026, widthDp = 200, heightDp = 200)
@Composable
private fun PreviewWaxingGibbous() {
    MoonShape(MoonAppearance.forPhase(phaseAngle = 135.0))
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1026, widthDp = 200, heightDp = 200)
@Composable
private fun PreviewSouthernCrescent() {
    MoonShape(MoonAppearance.forPhase(phaseAngle = 45.0, observerLatitude = -32.9))
}
