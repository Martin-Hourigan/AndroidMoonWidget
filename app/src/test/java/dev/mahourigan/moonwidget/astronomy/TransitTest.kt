package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

class TransitTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val zone = ZoneId.of("Australia/Sydney")

    private fun at(month: Int, day: Int, hour: Int) =
        LocalDateTime.of(2026, month, day, hour, 0).atZone(zone).toInstant()

    /**
     * Culmination is where the hour angle crosses zero, so the hour angle at the
     * reported moment should be essentially zero.
     */
    @Test
    fun `hour angle is zero at the reported culmination`() {
        for (day in 1..10) {
            val transit = RiseSet.nextTransit(at(9, day, 0), newcastle)
            assertNotNull("no culmination found on day $day", transit)

            val angle = RiseSet.hourAngle(JulianDate.fromInstant(transit!!), newcastle)
            assertTrue("hour angle was $angle degrees on day $day", abs(angle) < 0.05)
        }
    }

    /**
     * The Moon is at its highest when it culminates, so altitude either side
     * must be lower. This is the check that would catch a sign error putting us
     * at anti-transit — the lowest point — instead.
     */
    @Test
    fun `altitude peaks at culmination`() {
        for (day in 1..10) {
            val transit = requireNotNull(RiseSet.nextTransit(at(9, day, 0), newcastle))
            val jd = JulianDate.fromInstant(transit)

            val atPeak = RiseSet.altitude(jd, newcastle)
            val before = RiseSet.altitude(jd - 1.0 / 24, newcastle) // one hour earlier
            val after = RiseSet.altitude(jd + 1.0 / 24, newcastle)

            assertTrue("day $day: $before >= $atPeak an hour before", atPeak > before)
            assertTrue("day $day: $after >= $atPeak an hour after", atPeak > after)
        }
    }

    /** Successive culminations are one lunar day apart, about 24h50m. */
    @Test
    fun `culminations repeat roughly every 24 hours 50 minutes`() {
        var cursor = at(9, 1, 0)
        var comparisons = 0
        var previous = requireNotNull(RiseSet.nextTransit(cursor, newcastle))

        repeat(8) {
            cursor = previous.plus(Duration.ofHours(1))
            val next = requireNotNull(RiseSet.nextTransit(cursor, newcastle))
            val gap = Duration.between(previous, next).toMinutes()

            assertTrue("gap was $gap minutes", gap in 1470..1530) // 24h30m .. 25h30m
            previous = next
            comparisons++
        }
        assertTrue(comparisons >= 8)
    }

    /**
     * Culmination should fall between a rise and the following set, near the
     * midpoint. This ties the transit code back to the already-validated
     * rise/set code.
     */
    @Test
    fun `culmination falls near the midpoint of rise and set`() {
        var checked = 0

        for (day in 1..12) {
            val from = at(9, day, 0)
            val events = RiseSet.nextEvents(from, newcastle)
            val rise = events.rise ?: continue
            val set = RiseSet.nextEvents(rise.plus(Duration.ofMinutes(30)), newcastle).set ?: continue
            if (!set.isAfter(rise)) continue

            val transit = requireNotNull(RiseSet.nextTransit(rise, newcastle))
            assertTrue("culmination $transit not between $rise and $set", transit in rise..set)

            val midpoint = rise.plus(Duration.between(rise, set).dividedBy(2))
            val offBy = Duration.between(midpoint, transit).abs().toMinutes()
            // Not exactly the midpoint — declination shifts through the night —
            // but it should be close.
            assertTrue("culmination was $offBy min from the midpoint", offBy < 40)
            checked++
        }

        assertTrue("expected several days to check, got $checked", checked >= 8)
    }

    @Test
    fun `nearest rise picks the closest one either side of the target`() {
        val fullMoon = JulianDate.toInstant(
            MoonPhase.nextFullMoon(JulianDate.fromInstant(at(9, 1, 0)))
        )
        val rise = requireNotNull(RiseSet.nearestRise(fullMoon, newcastle))

        val gap = Duration.between(rise, fullMoon).abs().toHours()
        assertTrue("nearest rise was $gap hours from fullness", gap <= 18)
    }

    @Test
    fun `nearest culmination picks the closest one either side of the target`() {
        val fullMoon = JulianDate.toInstant(
            MoonPhase.nextFullMoon(JulianDate.fromInstant(at(9, 1, 0)))
        )
        val transit = requireNotNull(RiseSet.nearestTransit(fullMoon, newcastle))

        val gap = Duration.between(transit, fullMoon).abs().toHours()
        assertTrue("nearest culmination was $gap hours from fullness", gap <= 18)
    }

    /**
     * A full moon is opposite the Sun, so it culminates around local midnight.
     * Independent physical check on the whole chain.
     */
    @Test
    fun `full moon culminates near local midnight`() {
        val fullMoon = JulianDate.toInstant(
            MoonPhase.nextFullMoon(JulianDate.fromInstant(at(9, 1, 0)))
        )
        val transit = requireNotNull(RiseSet.nearestTransit(fullMoon, newcastle))

        val hour = transit.atZone(zone).hour
        val hoursFromMidnight = minOf(hour, 24 - hour)
        assertTrue("full moon culminated at $hour:00 local", hoursFromMidnight <= 2)
    }
}
