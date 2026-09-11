package dev.mahourigan.moonwidget.astronomy

import kotlin.math.PI

/** Small shared helpers for the trigonometry-heavy position code. */

internal const val DEG_TO_RAD = PI / 180.0
internal const val RAD_TO_DEG = 180.0 / PI

internal val Double.radians: Double get() = this * DEG_TO_RAD
internal val Double.degrees: Double get() = this * RAD_TO_DEG

/** Wrap an angle in degrees to `[0, 360)`. */
internal fun normalizeDegrees(value: Double): Double {
    val wrapped = value % 360.0
    return if (wrapped < 0) wrapped + 360.0 else wrapped
}

/** Wrap an angle in degrees to `(-180, 180]`. */
internal fun normalizeSignedDegrees(value: Double): Double {
    val wrapped = normalizeDegrees(value)
    return if (wrapped > 180.0) wrapped - 360.0 else wrapped
}
