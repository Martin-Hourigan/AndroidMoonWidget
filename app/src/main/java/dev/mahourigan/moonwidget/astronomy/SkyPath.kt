package dev.mahourigan.moonwidget.astronomy

import java.time.Duration
import java.time.Instant
import kotlin.math.acos
import kotlin.math.tan

/** The Moon's height above the horizon at one moment. */
data class SkyPoint(
    val time: Instant,
    /** Degrees above the horizon; negative below it. */
    val altitude: Double,
)

/** One journey from horizon to horizon. */
data class SkyPass(
    val rise: Instant,
    val set: Instant,
) {
    val duration: Duration get() = Duration.between(rise, set)

    /**
     * How far through the pass [now] falls: 0 at the rise, 1 at the set.
     *
     * Clamped, so a moment outside the pass sits at whichever end it is nearest
     * rather than running off the drawing.
     */
    fun fractionAt(now: Instant): Double {
        val total = duration.toMillis()
        if (total <= 0) return 0.0
        val elapsed = Duration.between(rise, now).toMillis()
        return (elapsed.toDouble() / total).coerceIn(0.0, 1.0)
    }
}

/**
 * The Moon's path across the sky, above the horizon and below it.
 *
 * The Moon traces a circle on the celestial sphere once per lunar day, and the
 * observer's horizon cuts that circle as a chord. How far off-centre the chord
 * sits — which depends on the observer's latitude and the Moon's declination —
 * is why the time spent up and the time spent down are rarely equal. At
 * Newcastle the visible half of the journey ranges from roughly nine and a half
 * hours to over fifteen, within a single month.
 */
object SkyPath {

    /** A lunar day: the Moon drifts east, so it runs ~50 minutes longer than a solar one. */
    val LUNAR_DAY: Duration = Duration.ofMinutes(24 * 60 + 50)

    /**
     * Altitude sampled evenly between two moments.
     *
     * @param count how many points, including both ends. Two or more.
     */
    fun samples(
        from: Instant,
        to: Instant,
        location: GeoLocation,
        // Ten-minute resolution. A pass that culminates near the zenith turns
        // sharply at the top, and coarser sampling shows that as a visible
        // corner rather than a peak.
        count: Int = 150,
    ): List<SkyPoint> {
        val steps = count.coerceAtLeast(2)
        val span = Duration.between(from, to).toMillis()

        return (0 until steps).map { index ->
            val time = from.plusMillis(span * index / (steps - 1))
            SkyPoint(time, RiseSet.altitude(JulianDate.fromInstant(time), location))
        }
    }

    /**
     * A window of one lunar day centred on [around].
     *
     * Deliberately not "this rise to the next rise": near the poles there may be
     * no rise for weeks, and a fixed window still draws something truthful. It
     * is also slightly longer than a lunar day, so a full pass is always inside
     * it wherever one exists.
     */
    fun windowAround(around: Instant): Pair<Instant, Instant> {
        val half = LUNAR_DAY.dividedBy(2)
        return around.minus(half) to around.plus(half)
    }

    /**
     * The pass the Moon is on now, or the next one if it is currently down.
     *
     * Null when there is no pass to speak of — above or below the horizon for
     * days on end, which happens at high latitudes.
     */
    fun currentOrNextPass(
        from: Instant,
        location: GeoLocation,
        searchHours: Long = 30,
    ): SkyPass? {
        val next = RiseSet.nextEvents(from, location)

        if (!next.currentlyUp) {
            val rise = next.rise ?: return null
            // The set that closes the pass this rise opens, not the one before it.
            val set = RiseSet.nextEvents(rise.plusSeconds(60), location).set ?: return null
            return SkyPass(rise, set)
        }

        val rise = lastRiseAtOrBefore(from, location, searchHours) ?: return null
        val set = next.set ?: return null
        return SkyPass(rise, set)
    }

    /**
     * The most recent rise at or before [target].
     *
     * [RiseSet.nextEvents] only ever looks forward, so this walks up from far
     * enough back and keeps the last rise that has already happened.
     */
    private fun lastRiseAtOrBefore(
        target: Instant,
        location: GeoLocation,
        searchHours: Long,
    ): Instant? {
        var cursor = target.minus(Duration.ofHours(searchHours))
        var best: Instant? = null

        // A lunar day holds one rise, so a 30-hour window holds at most two.
        repeat(4) {
            val found = RiseSet.nextEvents(cursor, location, searchDays = 2).rise ?: return best
            if (found.isAfter(target)) return best
            best = found
            cursor = found.plus(Duration.ofMinutes(30))
        }
        return best
    }

    /**
     * Hour angle at which a body of this declination meets the horizon, degrees.
     *
     * `cos H₀ = −tan(latitude)·tan(declination)`. Null when the cosine falls
     * outside ±1, meaning the body never sets or never rises from there.
     *
     * This is the closed form behind the whole diagram, and the tests use it to
     * check the sampled rise and set times independently.
     */
    fun horizonHourAngle(latitude: Double, declination: Double): Double? {
        val cosH = -tan(latitude.radians) * tan(declination.radians)
        return if (cosH < -1.0 || cosH > 1.0) null else acos(cosH).degrees
    }

    /**
     * Hours above the horizon per lunar day, from the closed form.
     *
     * Returns the full lunar day when the Moon never sets, and zero when it
     * never rises.
     */
    fun hoursAboveHorizon(latitude: Double, declination: Double): Double {
        val h0 = horizonHourAngle(latitude, declination)
            ?: return if (-tan(latitude.radians) * tan(declination.radians) < -1.0) {
                LUNAR_DAY.toMinutes() / 60.0
            } else {
                0.0
            }
        // The Moon covers 360 degrees of hour angle in one lunar day.
        return (2 * h0 / 360.0) * (LUNAR_DAY.toMinutes() / 60.0)
    }
}
