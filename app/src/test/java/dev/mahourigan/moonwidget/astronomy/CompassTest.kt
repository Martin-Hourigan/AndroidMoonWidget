package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Which way to face to see the Moon. */
class CompassTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val london = GeoLocation(latitude = 51.5074, longitude = -0.1278)

    // --- Naming a bearing ---

    @Test
    fun `the cardinal bearings name themselves`() {
        assertEquals(CompassPoint.NORTH, CompassPoint.forAzimuth(0.0))
        assertEquals(CompassPoint.EAST, CompassPoint.forAzimuth(90.0))
        assertEquals(CompassPoint.SOUTH, CompassPoint.forAzimuth(180.0))
        assertEquals(CompassPoint.WEST, CompassPoint.forAzimuth(270.0))
        assertEquals(CompassPoint.NORTH, CompassPoint.forAzimuth(360.0))
    }

    @Test
    fun `a bearing rounds to its nearest point, not the one before it`() {
        // 80 degrees is closer to east than to east-north-east, and naive
        // division by the sector width would call it ENE.
        assertEquals(CompassPoint.EAST, CompassPoint.forAzimuth(80.0))
        assertEquals(CompassPoint.EAST_NORTH_EAST, CompassPoint.forAzimuth(70.0))
        assertEquals(CompassPoint.NORTH, CompassPoint.forAzimuth(350.0))
        assertEquals(CompassPoint.NORTH_NORTH_WEST, CompassPoint.forAzimuth(340.0))
    }

    @Test
    fun `every sector midpoint lands on its own point`() {
        CompassPoint.entries.forEachIndexed { index, point ->
            val midpoint = index * 22.5
            assertEquals("at $midpoint degrees", point, CompassPoint.forAzimuth(midpoint))
        }
    }

    @Test
    fun `bearings outside a single turn still resolve`() {
        assertEquals(CompassPoint.EAST, CompassPoint.forAzimuth(450.0))
        assertEquals(CompassPoint.WEST, CompassPoint.forAzimuth(-90.0))
    }

    // --- Where the Moon actually is ---

    @Test
    fun `the Moon rises in the east and sets in the west`() {
        // The one thing everybody already knows, and a real check on the
        // azimuth convention: get north and south the wrong way round and this
        // comes out reversed.
        var day = Instant.parse("2026-01-01T00:00:00Z")
        val end = day.plus(28, ChronoUnit.DAYS)
        var checked = 0

        while (day.isBefore(end)) {
            val pass = SkyPath.currentOrNextPass(day, newcastle)
            if (pass != null) {
                val rise = MoonOrientation.moonPosition(
                    JulianDate.fromInstant(pass.rise), newcastle,
                ).azimuth
                val set = MoonOrientation.moonPosition(
                    JulianDate.fromInstant(pass.set), newcastle,
                ).azimuth

                assertTrue("rising at $rise is not in the eastern half", rise in 0.0..180.0)
                assertTrue("setting at $set is not in the western half", set in 180.0..360.0)
                checked++
            }
            day = day.plus(3, ChronoUnit.DAYS)
        }
        assertTrue("expected passes across the month", checked > 5)
    }

    /**
     * Rising and setting are mirror images about the north-south line, so their
     * bearings must be equally far from due east and due west.
     */
    @Test
    fun `rise and set bearings are symmetric about the meridian`() {
        val pass = SkyPath.currentOrNextPass(
            Instant.parse("2026-08-24T00:00:00Z"), newcastle,
        )!!

        val rise = MoonOrientation.moonPosition(JulianDate.fromInstant(pass.rise), newcastle).azimuth
        val set = MoonOrientation.moonPosition(JulianDate.fromInstant(pass.set), newcastle).azimuth

        // Mirroring about north-south maps a bearing to 360 minus itself.
        assertEquals("set should mirror rise", 360.0 - rise, set, 4.0)
    }

    @Test
    fun `at culmination the Moon is due north or due south`() {
        // Newcastle is south of the tropics, so the Moon tops out to the north;
        // London is north of them, so it tops out to the south. Nothing else is
        // possible from those latitudes.
        val transitSouth = RiseSet.nextTransit(Instant.parse("2026-08-24T00:00:00Z"), newcastle)!!
        val fromNewcastle = MoonOrientation.moonPosition(
            JulianDate.fromInstant(transitSouth), newcastle,
        ).azimuth
        assertEquals("Newcastle should look north", 0.0, normalizeSignedDegrees(fromNewcastle), 1.0)

        val transitNorth = RiseSet.nextTransit(Instant.parse("2026-08-24T00:00:00Z"), london)!!
        val fromLondon = MoonOrientation.moonPosition(
            JulianDate.fromInstant(transitNorth), london,
        ).azimuth
        assertEquals("London should look south", 180.0, fromLondon, 1.0)
    }

    @Test
    fun `altitude is zero at the horizon crossings`() {
        val pass = SkyPath.currentOrNextPass(
            Instant.parse("2026-08-24T00:00:00Z"), newcastle,
        )!!

        // Not exactly zero: rise and set are timed against the upper limb with
        // refraction and parallax, so the centre sits a little below.
        listOf(pass.rise, pass.set).forEach { moment ->
            val altitude = MoonOrientation.moonPosition(
                JulianDate.fromInstant(moment), newcastle,
            ).altitude
            assertEquals("altitude at $moment", 0.0, altitude, 1.0)
        }
    }

    @Test
    fun `the bearing sweeps steadily and never jumps`() {
        var time = Instant.parse("2026-08-24T12:00:00Z")
        val end = time.plus(12, ChronoUnit.HOURS)
        var previous: Double? = null

        while (time.isBefore(end)) {
            val azimuth = MoonOrientation.moonPosition(
                JulianDate.fromInstant(time), london,
            ).azimuth
            previous?.let {
                val step = kotlin.math.abs(normalizeSignedDegrees(azimuth - it))
                assertTrue("jumped $step degrees in a minute at $time", step < 2.0)
            }
            previous = azimuth
            time = time.plus(1, ChronoUnit.MINUTES)
        }
    }
}
