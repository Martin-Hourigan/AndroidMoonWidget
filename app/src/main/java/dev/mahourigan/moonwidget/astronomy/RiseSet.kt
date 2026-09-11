package dev.mahourigan.moonwidget.astronomy

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** An observer's position on Earth. */
data class GeoLocation(
    /** Degrees, positive north. */
    val latitude: Double,
    /** Degrees, positive east. */
    val longitude: Double,
)

/**
 * Moonrise and moonset for one local day.
 *
 * Either may be null: the Moon runs about 50 minutes later each day, so roughly
 * once a month a calendar day genuinely contains no rise, or no set. At high
 * latitudes the Moon can also stay up or down for days at a time. A null means
 * "did not happen today", not "failed to calculate".
 */
data class RiseSetTimes(
    val rise: Instant?,
    val set: Instant?,
    /** True when the Moon was above the horizon for the whole day. */
    val alwaysUp: Boolean = false,
    /** True when the Moon never cleared the horizon. */
    val alwaysDown: Boolean = false,
)

/**
 * The upcoming horizon crossings, looking forward from a moment in time rather
 * than being tied to a calendar day.
 *
 * @param currentlyUp whether the Moon is above the horizon right now, which
 *   decides whether "sets at" or "rises at" is the more useful thing to lead with.
 */
data class NextRiseSet(
    val rise: Instant?,
    val set: Instant?,
    val currentlyUp: Boolean,
)

object RiseSet {

    /** Earth's equatorial radius, km — used for the parallax correction. */
    private const val EARTH_RADIUS_KM = 6378.14

    /** Atmospheric refraction at the horizon, degrees. */
    private const val REFRACTION = 34.0 / 60.0

    /**
     * Fraction of the Moon's angular radius used when defining "risen".
     *
     * Convention (Meeus ch. 15) is to time rise/set against the Moon's upper
     * limb, so the disc's own semidiameter is subtracted from the parallax term.
     */
    private const val LIMB_FACTOR = 0.7275

    /** Minutes between altitude samples when hunting for horizon crossings. */
    private const val SAMPLE_MINUTES = 10

    /**
     * Altitude of the Moon's centre above the horizon, in degrees.
     */
    fun altitude(julianDay: Double, location: GeoLocation): Double {
        val position = CelestialPositions.moon(julianDay)
        val equatorial = CelestialPositions.toEquatorial(position, julianDay)

        val localSiderealTime = normalizeDegrees(
            CelestialPositions.greenwichMeanSiderealTime(julianDay) + location.longitude
        )
        val hourAngle = normalizeSignedDegrees(localSiderealTime - equatorial.rightAscension)

        val latitude = location.latitude.radians
        val declination = equatorial.declination.radians

        val sinAltitude = sin(latitude) * sin(declination) +
            cos(latitude) * cos(declination) * cos(hourAngle.radians)

        return asin(sinAltitude.coerceIn(-1.0, 1.0)).degrees
    }

    /**
     * The altitude the Moon's centre must reach for its upper limb to sit on the
     * horizon. Depends on distance, so it is recomputed rather than hardcoded.
     */
    private fun horizonAltitude(julianDay: Double): Double {
        val distance = CelestialPositions.moon(julianDay).distanceKm
        val parallax = asin(EARTH_RADIUS_KM / distance).degrees
        return LIMB_FACTOR * parallax - REFRACTION
    }

    /**
     * Moonrise and moonset during the local calendar [date] at [location].
     */
    fun forDate(date: LocalDate, location: GeoLocation, zone: ZoneId): RiseSetTimes {
        val dayStart = date.atStartOfDay(zone).toInstant()
        val jdStart = JulianDate.fromInstant(dayStart)
        val stepDays = SAMPLE_MINUTES / (24.0 * 60.0)
        val steps = (24 * 60) / SAMPLE_MINUTES

        var riseJd: Double? = null
        var setJd: Double? = null

        var previousJd = jdStart
        var previousDelta = altitude(previousJd, location) - horizonAltitude(previousJd)
        val startedAbove = previousDelta > 0
        var everAbove = startedAbove
        var everBelow = !startedAbove

        for (step in 1..steps) {
            val currentJd = jdStart + step * stepDays
            val currentDelta = altitude(currentJd, location) - horizonAltitude(currentJd)

            if (currentDelta > 0) everAbove = true else everBelow = true

            if (previousDelta < 0 && currentDelta >= 0 && riseJd == null) {
                riseJd = refineCrossing(previousJd, currentJd, location)
            } else if (previousDelta > 0 && currentDelta <= 0 && setJd == null) {
                setJd = refineCrossing(previousJd, currentJd, location)
            }

            previousJd = currentJd
            previousDelta = currentDelta
        }

        return RiseSetTimes(
            rise = riseJd?.let { JulianDate.toInstant(it) },
            set = setJd?.let { JulianDate.toInstant(it) },
            alwaysUp = !everBelow,
            alwaysDown = !everAbove,
        )
    }

    /**
     * The next rise and the next set at or after [from], looking ahead up to
     * [searchDays].
     *
     * This is what a widget should show. "Today's" rise is misleading late in
     * the evening, when it happened seventeen hours ago; what someone glancing
     * at their home screen wants is the next time the Moon comes up.
     *
     * Both fields are still nullable: near the poles the Moon can stay above or
     * below the horizon for weeks, and inventing a time would be worse than
     * admitting there isn't one.
     */
    fun nextEvents(
        from: Instant,
        location: GeoLocation,
        searchDays: Int = 3,
    ): NextRiseSet {
        val startJd = JulianDate.fromInstant(from)
        val stepDays = SAMPLE_MINUTES / (24.0 * 60.0)
        val steps = searchDays * (24 * 60) / SAMPLE_MINUTES

        var riseJd: Double? = null
        var setJd: Double? = null

        var previousJd = startJd
        var previousDelta = altitude(previousJd, location) - horizonAltitude(previousJd)
        val startsAboveHorizon = previousDelta > 0

        for (step in 1..steps) {
            val currentJd = startJd + step * stepDays
            val currentDelta = altitude(currentJd, location) - horizonAltitude(currentJd)

            if (previousDelta < 0 && currentDelta >= 0 && riseJd == null) {
                riseJd = refineCrossing(previousJd, currentJd, location)
            } else if (previousDelta > 0 && currentDelta <= 0 && setJd == null) {
                setJd = refineCrossing(previousJd, currentJd, location)
            }

            if (riseJd != null && setJd != null) break

            previousJd = currentJd
            previousDelta = currentDelta
        }

        return NextRiseSet(
            rise = riseJd?.let { JulianDate.toInstant(it) },
            set = setJd?.let { JulianDate.toInstant(it) },
            currentlyUp = startsAboveHorizon,
        )
    }

    /**
     * Local hour angle of the Moon, degrees in `(-180, 180]`.
     *
     * Zero at culmination (due north/south, highest in the sky), negative
     * before, positive after.
     */
    fun hourAngle(julianDay: Double, location: GeoLocation): Double {
        val equatorial = CelestialPositions.toEquatorial(
            CelestialPositions.moon(julianDay), julianDay,
        )
        val localSiderealTime = normalizeDegrees(
            CelestialPositions.greenwichMeanSiderealTime(julianDay) + location.longitude
        )
        return normalizeSignedDegrees(localSiderealTime - equatorial.rightAscension)
    }

    /**
     * The next culmination — the moment the Moon is highest in the sky.
     *
     * Note this is culmination, not the zenith: the Moon passes through the
     * actual zenith only between about 28.6 degrees north and south.
     *
     * Found by root-finding on the hour angle rather than hunting for a peak in
     * altitude, which is both more precise and cheaper. The hour angle rises
     * steadily and wraps from +180 back to -180, so an *ascending* zero
     * crossing is unambiguously the culmination and never the wrap.
     */
    fun nextTransit(from: Instant, location: GeoLocation, searchDays: Int = 2): Instant? {
        val startJd = JulianDate.fromInstant(from)
        val stepDays = SAMPLE_MINUTES / (24.0 * 60.0)
        val steps = searchDays * (24 * 60) / SAMPLE_MINUTES

        var previousJd = startJd
        var previousAngle = hourAngle(previousJd, location)

        for (step in 1..steps) {
            val currentJd = startJd + step * stepDays
            val currentAngle = hourAngle(currentJd, location)

            if (previousAngle < 0 && currentAngle >= 0) {
                return JulianDate.toInstant(refineTransit(previousJd, currentJd, location))
            }

            previousJd = currentJd
            previousAngle = currentAngle
        }
        return null
    }

    /**
     * The rise closest in time to [target], looking either side of it.
     *
     * Used for "when does the full moon come up?": fullness can fall at 3am, in
     * which case the relevant rise is the previous evening's, not the one on
     * the same calendar date.
     */
    fun nearestRise(target: Instant, location: GeoLocation, windowHours: Long = 18): Instant? =
        nearest(target, windowHours) { from -> nextEvents(from, location, searchDays = 2).rise }

    /** As [nearestRise], but for culmination. */
    fun nearestTransit(target: Instant, location: GeoLocation, windowHours: Long = 18): Instant? =
        nearest(target, windowHours) { from -> nextTransit(from, location) }

    /**
     * Walk forward from [windowHours] before [target], collecting occurrences,
     * and keep whichever lands closest to [target].
     */
    private inline fun nearest(
        target: Instant,
        windowHours: Long,
        findNext: (Instant) -> Instant?,
    ): Instant? {
        var cursor = target.minus(Duration.ofHours(windowHours))
        val limit = target.plus(Duration.ofHours(windowHours))

        var best: Instant? = null
        var bestGap = Long.MAX_VALUE

        // A lunar day is ~24h50m, so a 36-hour window holds at most two events.
        repeat(4) {
            val found = findNext(cursor) ?: return best
            if (found.isAfter(limit)) return best

            val gap = Duration.between(found, target).abs().toMillis()
            if (gap < bestGap) {
                bestGap = gap
                best = found
            }
            // Step past this one to look for the next.
            cursor = found.plus(Duration.ofMinutes(30))
        }
        return best
    }

    /** Bisect a bracketed hour-angle zero crossing. */
    private fun refineTransit(lowJd: Double, highJd: Double, location: GeoLocation): Double {
        var low = lowJd
        var high = highJd
        repeat(30) {
            val mid = (low + high) / 2
            if (hourAngle(mid, location) < 0) low = mid else high = mid
        }
        return (low + high) / 2
    }

    /**
     * Bisect a bracketed horizon crossing down to about a second.
     */
    private fun refineCrossing(lowJd: Double, highJd: Double, location: GeoLocation): Double {
        var low = lowJd
        var high = highJd
        repeat(30) {
            val mid = (low + high) / 2
            val delta = altitude(mid, location) - horizonAltitude(mid)
            // Keep the bracket straddling the crossing, whichever way it runs.
            val lowDelta = altitude(low, location) - horizonAltitude(low)
            if ((lowDelta < 0) == (delta < 0)) low = mid else high = mid
        }
        return (low + high) / 2
    }
}
