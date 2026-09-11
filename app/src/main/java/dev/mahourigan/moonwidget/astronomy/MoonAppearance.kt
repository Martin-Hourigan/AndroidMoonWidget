package dev.mahourigan.moonwidget.astronomy

import kotlin.math.abs
import kotlin.math.cos

/**
 * How the lit part of the Moon should be drawn, independent of any UI toolkit.
 *
 * The Moon is a sphere lit from one side, so the boundary between light and dark
 * (the terminator) is a circle seen at an angle — which projects to a *half
 * ellipse*, not an arc of a circle. Drawing it as a circular arc is the classic
 * mistake and looks wrong at every phase except the quarters.
 *
 * So the lit shape is built from two halves:
 *  - the outer limb, a semicircle of the disc
 *  - the terminator, a semi-ellipse with the same vertical radius but a
 *    horizontal radius of `cos(phaseAngle)` times the disc radius
 *
 * When [terminatorCurveFactor] is positive the terminator bulges away from the
 * lit limb (gibbous); when negative it cuts inward (crescent).
 */
data class MoonAppearance(
    /** Fraction of the disc that is lit, `0.0`–`1.0`. */
    val illumination: Double,
    /** True when the lit limb is on the right of the disc as drawn. */
    val litOnRight: Boolean,
    /**
     * Horizontal radius of the terminator ellipse as a signed fraction of the
     * disc radius, `-1.0`–`1.0`.
     *
     * Magnitude is how "open" the terminator is; sign says which way it curves.
     */
    val terminatorCurveFactor: Double,
    /** True if the disc is dark enough to draw as an unlit circle. */
    val isEffectivelyNew: Boolean,
    /** True if the disc is bright enough to draw as a plain lit circle. */
    val isEffectivelyFull: Boolean,
    /**
     * Clockwise rotation to apply to the whole disc, degrees.
     *
     * Zero for the simple orientation, where the terminator is always drawn
     * vertical. Set by [oriented] to the Moon's real angle in the sky.
     */
    val rotationDegrees: Double = 0.0,
) {
    companion object {
        /** Below this illumination there is nothing meaningful to draw. */
        private const val NEW_THRESHOLD = 0.01

        /** Above this illumination the terminator is too thin to be worth drawing. */
        private const val FULL_THRESHOLD = 0.99

        /**
         * Work out how to draw the Moon.
         *
         * @param phaseAngle elongation from the Sun, degrees `[0, 360)`.
         * @param observerLatitude used to flip the image for southern observers.
         *   Pass `0.0` to keep the conventional northern-hemisphere orientation.
         */
        fun forPhase(phaseAngle: Double, observerLatitude: Double = 0.0): MoonAppearance {
            val angle = normalizeDegrees(phaseAngle)
            val illumination = (1 - cos(angle.radians)) / 2

            // Waxing moons are lit on the right as seen from the northern
            // hemisphere. South of the equator the whole scene is inverted,
            // so the lit limb appears on the opposite side.
            val waxing = angle < 180.0
            val southern = observerLatitude < 0
            val litOnRight = if (southern) !waxing else waxing

            // cos of the phase angle gives the foreshortening of the terminator
            // circle. It runs +1 (new) -> 0 (quarter) -> -1 (full); negating it
            // gives us "bulges outward when gibbous".
            val curve = -cos(angle.radians)

            return MoonAppearance(
                illumination = illumination,
                litOnRight = litOnRight,
                terminatorCurveFactor = curve,
                isEffectivelyNew = illumination < NEW_THRESHOLD,
                isEffectivelyFull = illumination > FULL_THRESHOLD,
            )
        }

        /**
         * Work out how to draw the Moon as it actually appears from where the
         * observer is standing.
         *
         * The shape is always built lit-on-the-right and then turned, rather
         * than mirrored: with a real angle in hand there is no need for the
         * hemisphere special case, because a southern view simply comes out
         * rotated past 180 degrees.
         *
         * @param phaseAngle elongation from the Sun, degrees `[0, 360)`.
         * @param brightLimbAngleFromZenith direction of the lit side measured
         *   clockwise from straight up, from
         *   [MoonOrientation.brightLimbAngleFromZenith].
         */
        fun oriented(phaseAngle: Double, brightLimbAngleFromZenith: Double): MoonAppearance {
            val canonical = forPhase(phaseAngle, observerLatitude = 0.0)
                .copy(litOnRight = true)

            // The canonical drawing already points its lit side to the right,
            // which is 90 degrees clockwise from up, so only the remainder of
            // the angle has to be turned.
            return canonical.copy(
                rotationDegrees = normalizeDegrees(brightLimbAngleFromZenith - 90.0),
            )
        }
    }

    /** True when less than half the disc is lit, i.e. the terminator cuts inward. */
    val isCrescent: Boolean get() = illumination < 0.5

    /** Absolute openness of the terminator, `0.0` (new/full) to `1.0` (quarter). */
    val terminatorOpenness: Double get() = 1.0 - abs(terminatorCurveFactor)
}
