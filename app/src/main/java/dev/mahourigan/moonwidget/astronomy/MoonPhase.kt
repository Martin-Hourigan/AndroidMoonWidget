package dev.mahourigan.moonwidget.astronomy

import java.time.Instant
import kotlin.math.cos

/**
 * The eight named phases.
 *
 * Deliberately carries no display text: this package stays free of Android
 * dependencies so it can be unit-tested on a plain JVM, and user-facing names
 * belong in string resources. See `ui/DisplayStrings.kt` for the mapping.
 */
enum class PhaseName {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT,
}

/**
 * Everything the UI needs about the Moon's current appearance.
 *
 * @param phaseAngle elongation from the Sun in degrees `[0, 360)`. 0 is new,
 *   180 is full. Values below 180 are waxing.
 * @param illumination lit fraction of the disc, `0.0`–`1.0`.
 * @param ageDays days elapsed since the last new moon.
 */
data class MoonPhaseInfo(
    val phaseAngle: Double,
    val illumination: Double,
    val ageDays: Double,
    val phaseName: PhaseName,
    val isWaxing: Boolean,
) {
    val illuminationPercent: Int get() = Math.round(illumination * 100).toInt()
}

object MoonPhase {

    /** Mean length of a lunation, in days. */
    const val SYNODIC_MONTH = 29.530588853

    /**
     * Half-width of the window, in degrees of elongation, within which a phase
     * is called by its exact name rather than the crescent/gibbous in between.
     *
     * 0 to 360 split across 8 names gives 45 degrees each, so the four "exact"
     * phases claim 22.5 degrees either side of their centre.
     */
    private const val PHASE_WINDOW = 22.5

    fun at(julianDay: Double): MoonPhaseInfo {
        val moonLongitude = CelestialPositions.moon(julianDay).longitude
        val sunLongitude = CelestialPositions.sunLongitude(julianDay)
        val phaseAngle = normalizeDegrees(moonLongitude - sunLongitude)

        return MoonPhaseInfo(
            phaseAngle = phaseAngle,
            illumination = (1 - cos(phaseAngle.radians)) / 2,
            ageDays = phaseAngle / 360.0 * SYNODIC_MONTH,
            phaseName = nameFor(phaseAngle),
            isWaxing = phaseAngle < 180.0,
        )
    }

    fun at(instant: Instant): MoonPhaseInfo = at(JulianDate.fromInstant(instant))

    private fun nameFor(phaseAngle: Double): PhaseName = when {
        phaseAngle < PHASE_WINDOW -> PhaseName.NEW_MOON
        phaseAngle < 90 - PHASE_WINDOW -> PhaseName.WAXING_CRESCENT
        phaseAngle < 90 + PHASE_WINDOW -> PhaseName.FIRST_QUARTER
        phaseAngle < 180 - PHASE_WINDOW -> PhaseName.WAXING_GIBBOUS
        phaseAngle < 180 + PHASE_WINDOW -> PhaseName.FULL_MOON
        phaseAngle < 270 - PHASE_WINDOW -> PhaseName.WANING_GIBBOUS
        phaseAngle < 270 + PHASE_WINDOW -> PhaseName.LAST_QUARTER
        phaseAngle < 360 - PHASE_WINDOW -> PhaseName.WANING_CRESCENT
        else -> PhaseName.NEW_MOON
    }

    /**
     * Instant of the next time the Moon reaches [targetAngle] degrees of elongation.
     * Use 0.0 for the next new moon, 180.0 for the next full moon.
     *
     * Brackets the crossing by stepping forward a few hours at a time, then
     * bisects. Cheap, and accurate to well under a minute.
     */
    fun nextPhaseAngle(fromJulianDay: Double, targetAngle: Double): Double {
        val stepDays = 0.125 // 3 hours
        var low = fromJulianDay
        var difference = signedDifference(low, targetAngle)

        // Walk forward until the signed difference flips from negative to positive.
        var high = low
        var guard = 0
        while (guard++ < (SYNODIC_MONTH / stepDays).toInt() + 10) {
            high = low + stepDays
            val next = signedDifference(high, targetAngle)
            if (difference < 0 && next >= 0) break
            low = high
            difference = next
        }

        repeat(60) {
            val mid = (low + high) / 2
            if (signedDifference(mid, targetAngle) < 0) low = mid else high = mid
        }
        return (low + high) / 2
    }

    fun nextNewMoon(fromJulianDay: Double): Double = nextPhaseAngle(fromJulianDay, 0.0)

    fun nextFullMoon(fromJulianDay: Double): Double = nextPhaseAngle(fromJulianDay, 180.0)

    /**
     * How far the current elongation sits before (negative) or after (positive)
     * [targetAngle], wrapped to `(-180, 180]` so the crossing is a clean sign change.
     */
    private fun signedDifference(julianDay: Double, targetAngle: Double): Double {
        val moonLongitude = CelestialPositions.moon(julianDay).longitude
        val sunLongitude = CelestialPositions.sunLongitude(julianDay)
        return normalizeSignedDegrees(moonLongitude - sunLongitude - targetAngle)
    }
}
