package dev.mahourigan.moonwidget.astronomy

import java.time.Month
import java.time.ZoneId
import kotlin.math.abs

/**
 * Which monthly slot a full moon belongs to.
 *
 * The actual names depend on which [MoonNameSet] the user has chosen, so the
 * slot is deliberately set-agnostic: it says "the September moon" or "the
 * Harvest moon", not what that is called.
 */
enum class MoonSlot(val month: Month) {
    JANUARY(Month.JANUARY),
    FEBRUARY(Month.FEBRUARY),
    MARCH(Month.MARCH),
    APRIL(Month.APRIL),
    MAY(Month.MAY),
    JUNE(Month.JUNE),
    JULY(Month.JULY),
    AUGUST(Month.AUGUST),
    SEPTEMBER(Month.SEPTEMBER),
    OCTOBER(Month.OCTOBER),
    NOVEMBER(Month.NOVEMBER),
    DECEMBER(Month.DECEMBER),

    /**
     * Not tied to a month: the full moon falling nearest the September equinox.
     * Usually displaces September's name, but lands in October about a third of
     * the time and displaces that one instead.
     */
    HARVEST(Month.SEPTEMBER),
    ;

    companion object {
        private val BY_MONTH = entries.filter { it != HARVEST }.associateBy { it.month }

        fun forMonth(month: Month): MoonSlot = BY_MONTH.getValue(month)
    }
}

object MoonNames {

    /**
     * Half the window either side of the equinox in which a full moon can be
     * the nearest one. A lunation is ~29.5 days, so nothing further than about
     * 15 days away can win.
     */
    private const val EQUINOX_SEARCH_DAYS = 20

    /**
     * The slot the full moon at [fullMoonJulianDay] belongs to.
     *
     * @param southernSeasons shift the names six months so they line up with
     *   local seasons. The traditional names describe northern conditions —
     *   "Wolf Moon" in a Newcastle January is a midsummer heatwave — so users
     *   south of the equator may prefer them moved.
     */
    fun forFullMoon(
        fullMoonJulianDay: Double,
        zone: ZoneId,
        southernSeasons: Boolean = false,
    ): MoonSlot {
        val date = JulianDate.toInstant(fullMoonJulianDay).atZone(zone).toLocalDate()

        val effectiveMonth = if (southernSeasons) {
            date.month.plus(6)
        } else {
            date.month
        }

        // The Harvest Moon rule is tied to the real September equinox, so it
        // only applies when the names have not been shifted.
        if (!southernSeasons && isNearestFullMoonToSeptemberEquinox(fullMoonJulianDay, zone)) {
            return MoonSlot.HARVEST
        }

        return MoonSlot.forMonth(effectiveMonth)
    }

    /**
     * Whether this full moon is the one closest to the September equinox, which
     * is what makes it the Harvest Moon.
     */
    fun isNearestFullMoonToSeptemberEquinox(fullMoonJulianDay: Double, zone: ZoneId): Boolean {
        val year = JulianDate.toInstant(fullMoonJulianDay).atZone(zone).year
        val equinox = septemberEquinox(year)

        // Only full moons within a lunation of the equinox can qualify.
        if (abs(fullMoonJulianDay - equinox) > EQUINOX_SEARCH_DAYS) return false

        val nearest = nearestFullMoonTo(equinox)
        // Same moment to within a few hours means it is the same full moon.
        return abs(nearest - fullMoonJulianDay) < 0.25
    }

    /**
     * Julian day of the September equinox, found where the Sun's apparent
     * longitude reaches 180 degrees.
     */
    fun septemberEquinox(year: Int): Double {
        var low = JulianDate.fromCalendar(year, 9, 1.0)
        var high = JulianDate.fromCalendar(year, 10, 1.0)

        // The Sun's longitude climbs steadily through this window, so a plain
        // bisection on (longitude - 180) is safe.
        repeat(60) {
            val mid = (low + high) / 2
            if (normalizeSignedDegrees(CelestialPositions.sunLongitude(mid) - 180.0) < 0) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2
    }

    /** The full moon closest in time to [target], looking both ways. */
    fun nearestFullMoonTo(target: Double): Double {
        // Step back beyond a lunation, then walk forward and keep the closest.
        var cursor = target - MoonPhase.SYNODIC_MONTH - 1
        var best = MoonPhase.nextFullMoon(cursor)

        repeat(3) {
            val next = MoonPhase.nextFullMoon(cursor)
            if (abs(next - target) < abs(best - target)) best = next
            cursor = next + 1
        }
        return best
    }
}
