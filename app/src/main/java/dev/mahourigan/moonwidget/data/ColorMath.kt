package dev.mahourigan.moonwidget.data

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Colour conversion and contrast, as plain arithmetic.
 *
 * Kept out of the Compose layer and free of Android imports so the presets can
 * be contrast-checked and the conversions round-tripped in JVM unit tests.
 * `android.graphics.Color` has most of this built in, but none of it runs
 * outside an emulator.
 *
 * Colours are opaque ARGB in a Long throughout.
 */

/** "1A2B3C" (or "#1A2B3C") to opaque ARGB, or null if it is not six hex digits. */
fun parseHex(text: String): Long? {
    val cleaned = text.removePrefix("#")
    if (cleaned.length != 6 || !cleaned.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
        return null
    }
    return 0xFF000000L or cleaned.toLong(16)
}

/** "#RRGGBB". Alpha is always opaque here, so showing it would be noise. */
fun hexOf(argb: Long): String = "#%06X".format(argb and 0xFFFFFF)

/** ARGB to hue (0-360), saturation and brightness (both 0-1). */
fun rgbToHsv(argb: Long): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }

    return floatArrayOf(
        if (hue < 0f) hue + 360f else hue,
        if (max == 0f) 0f else delta / max,
        max,
    )
}

/** The inverse of [rgbToHsv]. Always fully opaque. */
fun hsvToRgb(hue: Float, saturation: Float, value: Float): Long {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)

    val chroma = v * s
    val x = chroma * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - chroma

    val (r, g, b) = when {
        h < 60f -> Triple(chroma, x, 0f)
        h < 120f -> Triple(x, chroma, 0f)
        h < 180f -> Triple(0f, chroma, x)
        h < 240f -> Triple(0f, x, chroma)
        h < 300f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }

    fun channel(component: Float): Long =
        ((component + m) * 255f).roundToInt().coerceIn(0, 255).toLong()

    return 0xFF000000L or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}

/**
 * Composite a translucent colour onto an opaque one.
 *
 * Muted text is the text colour at partial alpha, so working out whether it is
 * readable means knowing what it actually resolves to on the page.
 */
fun blend(foreground: Long, background: Long, alpha: Float): Long {
    val a = alpha.coerceIn(0f, 1f)
    fun channel(shift: Int): Long {
        val f = (foreground shr shift) and 0xFF
        val b = (background shr shift) and 0xFF
        return (f * a + b * (1 - a)).roundToInt().toLong().coerceIn(0, 255)
    }
    return 0xFF000000L or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

/** WCAG relative luminance, 0 (black) to 1 (white). */
fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val raw = (((argb shr shift) and 0xFF).toDouble()) / 255.0
        return if (raw <= 0.03928) raw / 12.92 else ((raw + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/**
 * WCAG contrast ratio between two opaque colours, from 1 (identical) to 21
 * (black on white).
 *
 * 4.5 is the usual floor for body text, 3.0 for large text and for graphics
 * that only need to be distinguishable.
 */
fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05) / (darker + 0.05)
}

/** The three kinds of dichromacy, for checking a palette still works. */
enum class ColourVision {
    /** Red-blind. Roughly 1% of men. */
    PROTANOPIA,

    /** Green-blind. The most common, roughly 1% of men again, and the milder
     *  anomalous form affects around 6%. */
    DEUTERANOPIA,

    /** Blue-blind. Rare, and affects men and women about equally. */
    TRITANOPIA,
}

/**
 * What [argb] looks like to someone with [vision].
 *
 * Uses the standard Vienot-Brettel-Mollon style transform, applied in linear
 * light rather than to the gamma-encoded bytes — doing it on the raw channels
 * is a common shortcut and it gets the luminance visibly wrong.
 *
 * This is an approximation of a thing that varies between people. It is used
 * for one purpose: checking that a palette does not collapse into itself. It is
 * not a claim about what any particular person sees.
 */
fun simulate(argb: Long, vision: ColourVision): Long {
    fun toLinear(channel: Long): Double {
        val v = channel / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    fun toSrgb(value: Double): Long {
        val clamped = value.coerceIn(0.0, 1.0)
        val encoded = if (clamped <= 0.0031308) {
            clamped * 12.92
        } else {
            1.055 * clamped.pow(1 / 2.4) - 0.055
        }
        return (encoded * 255).roundToInt().toLong().coerceIn(0, 255)
    }

    val r = toLinear((argb shr 16) and 0xFF)
    val g = toLinear((argb shr 8) and 0xFF)
    val b = toLinear(argb and 0xFF)

    val m = when (vision) {
        ColourVision.PROTANOPIA -> arrayOf(
            doubleArrayOf(0.152286, 1.052583, -0.204868),
            doubleArrayOf(0.114503, 0.786281, 0.099216),
            doubleArrayOf(-0.003882, -0.048116, 1.051998),
        )
        ColourVision.DEUTERANOPIA -> arrayOf(
            doubleArrayOf(0.367322, 0.860646, -0.227968),
            doubleArrayOf(0.280085, 0.672501, 0.047413),
            doubleArrayOf(-0.011820, 0.042940, 0.968881),
        )
        ColourVision.TRITANOPIA -> arrayOf(
            doubleArrayOf(1.255528, -0.076749, -0.178779),
            doubleArrayOf(-0.078411, 0.930809, 0.147602),
            doubleArrayOf(0.004733, 0.691367, 0.303900),
        )
    }

    return 0xFF000000L or
        (toSrgb(m[0][0] * r + m[0][1] * g + m[0][2] * b) shl 16) or
        (toSrgb(m[1][0] * r + m[1][1] * g + m[1][2] * b) shl 8) or
        toSrgb(m[2][0] * r + m[2][1] * g + m[2][2] * b)
}
