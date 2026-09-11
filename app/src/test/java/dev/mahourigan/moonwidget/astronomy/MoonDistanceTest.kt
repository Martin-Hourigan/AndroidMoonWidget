package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonDistanceTest {

    @Test
    fun `distance stays within the real orbital range over a year`() {
        var julianDay = JulianDate.fromCalendar(2026, 1, 1.0)
        var minimum = Double.MAX_VALUE
        var maximum = Double.MIN_VALUE

        repeat(365) {
            val distance = MoonDistance.at(julianDay).distanceKm
            minimum = minOf(minimum, distance)
            maximum = maxOf(maximum, distance)
            julianDay += 1.0
        }

        // Real extremes are roughly 356,400 - 406,700 km. Allow a little slack
        // for the truncated series, but catch anything wildly wrong.
        assertTrue("minimum was $minimum km", minimum in 355_000.0..372_000.0)
        assertTrue("maximum was $maximum km", maximum in 398_000.0..408_000.0)
    }

    @Test
    fun `perigee apogee fraction is bounded and moves with distance`() {
        var julianDay = JulianDate.fromCalendar(2026, 3, 1.0)
        var sawLow = false
        var sawHigh = false

        repeat(60) {
            val info = MoonDistance.at(julianDay)
            assertTrue(info.perigeeApogeeFraction in 0.0..1.0)
            if (info.perigeeApogeeFraction < 0.2) sawLow = true
            if (info.perigeeApogeeFraction > 0.8) sawHigh = true
            julianDay += 1.0
        }

        assertTrue("expected to pass near perigee within two months", sawLow)
        assertTrue("expected to pass near apogee within two months", sawHigh)
    }

    /**
     * Apparent diameter should track distance inversely, and sit in the range
     * commonly quoted for the Moon: about 29.4' to 33.5'.
     */
    @Test
    fun `apparent diameter is in the expected arcminute range`() {
        var julianDay = JulianDate.fromCalendar(2026, 1, 1.0)
        repeat(200) {
            val diameter = MoonDistance.at(julianDay).apparentDiameterArcmin
            assertTrue("diameter was $diameter'", diameter in 28.5..34.5)
            julianDay += 1.5
        }
    }

    @Test
    fun `closer moons look bigger`() {
        var julianDay = JulianDate.fromCalendar(2026, 5, 1.0)
        var nearest = MoonDistance.at(julianDay)
        var furthest = nearest

        repeat(40) {
            val info = MoonDistance.at(julianDay)
            if (info.distanceKm < nearest.distanceKm) nearest = info
            if (info.distanceKm > furthest.distanceKm) furthest = info
            julianDay += 1.0
        }

        assertTrue(nearest.apparentDiameterArcmin > furthest.apparentDiameterArcmin)
    }

    /**
     * A supermoon needs both conditions. Being near perigee at some random
     * phase does not count, which is the easy thing to get wrong.
     */
    @Test
    fun `supermoon requires the moon to be both close and full`() {
        var julianDay = JulianDate.fromCalendar(2026, 1, 1.0)
        var checked = 0

        repeat(700) {
            if (MoonDistance.isSupermoon(julianDay)) {
                val phase = MoonPhase.at(julianDay)
                assertEquals(PhaseName.FULL_MOON, phase.phaseName)
                assertTrue(MoonDistance.at(julianDay).isNearPerigee)
                checked++
            }
            julianDay += 0.5
        }

        assertTrue("expected at least one supermoon in a year", checked > 0)
    }

    @Test
    fun `a moon cannot be both super and micro`() {
        var julianDay = JulianDate.fromCalendar(2026, 1, 1.0)
        repeat(400) {
            assertFalse(
                MoonDistance.isSupermoon(julianDay) && MoonDistance.isMicromoon(julianDay)
            )
            julianDay += 0.9
        }
    }
}
