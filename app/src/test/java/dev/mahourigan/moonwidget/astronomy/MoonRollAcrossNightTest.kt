package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * The roll across a single night, at the resolution someone would actually
 * notice it.
 *
 * This is the behaviour the feature exists for, so it is asserted rather than
 * left to a screenshot: over one pass of the sky the disc must turn by a large,
 * steady amount, and never sit still.
 */
class MoonRollAcrossNightTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val sydneyZone: ZoneId = ZoneId.of("Australia/Sydney")

    private fun limbAngleAt(local: ZonedDateTime): Double =
        MoonOrientation.brightLimbAngleFromZenith(
            JulianDate.fromInstant(local.toInstant()), newcastle,
        )

    @Test
    fun `the disc turns steadily through the night of 2026-08-23`() {
        // The night captured on the emulator: the Moon is up from early
        // afternoon until it sets at 3:47 the next morning, transiting around
        // 20:40.
        val start = ZonedDateTime.of(2026, 8, 23, 17, 0, 0, 0, sydneyZone)

        val samples = (0..10).map { start.plusHours(it.toLong()) }
        val angles = samples.map { limbAngleAt(it) }

        samples.zip(angles).forEach { (time, angle) ->
            println("${time.toLocalTime()}  limb ${"%6.1f".format(angle)}°")
        }

        // Total roll, following the short arc between adjacent samples so a wrap
        // through 360 is not mistaken for a full revolution.
        val roll = angles.zipWithNext().sumOf { (a, b) -> abs(normalizeSignedDegrees(b - a)) }
        println("total roll over ${samples.size - 1} hours: ${"%.1f".format(roll)}°")

        assertTrue("Expected a large roll across the night, got $roll", roll > 60.0)

        // Never stuck: every hour should move the disc at least a little.
        angles.zipWithNext().forEachIndexed { index, (a, b) ->
            val step = abs(normalizeSignedDegrees(b - a))
            assertTrue(
                "Hour $index moved only $step degrees, which would look frozen",
                step > 0.5,
            )
        }
    }

    @Test
    fun `the roll is fastest near culmination`() {
        // Close to the meridian the tilt swings quickest, which is why the disc
        // looks almost static in the hours before it sets.
        val nearTransit = ZonedDateTime.of(2026, 8, 23, 20, 30, 0, 0, sydneyZone)
        val nearSet = ZonedDateTime.of(2026, 8, 24, 2, 30, 0, 0, sydneyZone)

        fun hourlyChange(from: ZonedDateTime): Double =
            abs(normalizeSignedDegrees(limbAngleAt(from.plusHours(1)) - limbAngleAt(from)))

        val atTransit = hourlyChange(nearTransit)
        val atSet = hourlyChange(nearSet)
        println("roll near culmination: ${"%.1f".format(atTransit)}°/h, near setting: ${"%.1f".format(atSet)}°/h")

        assertTrue(
            "Expected faster turning near culmination ($atTransit) than near setting ($atSet)",
            atTransit > atSet,
        )
    }

    /**
     * Compared by hour angle rather than by clock time, because the two
     * hemispheres do not share a night: the same instant that sits mid-evening
     * in London is close to dawn in Newcastle, with the Moon somewhere else
     * entirely in the sky.
     *
     * Hour angle is the object's own progress across its arc, so ±45° means the
     * same stage of the journey to both observers.
     */
    @Test
    fun `mirrored latitudes roll in opposite directions`() {
        fun turnAcrossMeridian(latitude: Double): Double {
            val before = MoonOrientation.parallacticAngle(-45.0, 0.0, latitude)
            val after = MoonOrientation.parallacticAngle(45.0, 0.0, latitude)
            return normalizeSignedDegrees(after - before)
        }

        val north = turnAcrossMeridian(40.0)
        val south = turnAcrossMeridian(-40.0)
        println("turn across the meridian — 40°N ${"%.1f".format(north)}°, 40°S ${"%.1f".format(south)}°")

        assertTrue("Expected opposite directions, got $north and $south", north * south < 0)
        assertTrue("and equal magnitudes", abs(abs(north) - abs(south)) < 1e-9)
    }
}
