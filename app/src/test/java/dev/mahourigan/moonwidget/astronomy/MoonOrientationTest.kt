package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * The rotation applied to the drawn Moon.
 *
 * The load-bearing check is [the bright limb points at the Sun][
 * `bright limb always points at the Sun`]: the χ − q formulation and a bearing
 * computed purely in horizon coordinates share no algebra, so their agreement is
 * evidence rather than a restatement of the same expression.
 */
class MoonOrientationTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val london = GeoLocation(latitude = 51.5074, longitude = -0.1278)
    private val quito = GeoLocation(latitude = -0.1807, longitude = -78.4678)

    /** Smallest signed difference between two bearings, in degrees. */
    private fun angleBetween(a: Double, b: Double): Double =
        abs(normalizeSignedDegrees(a - b))

    // --- The independent cross-check ---

    @Test
    fun `bright limb always points at the Sun`() {
        var checked = 0
        var worst = 0.0

        for (location in listOf(newcastle, london, quito)) {
            // Six-hourly for a full lunation, so every phase is sampled at every
            // stage of its journey across the sky.
            var time = Instant.parse("2026-01-01T00:00:00Z")
            val end = time.plus(30, ChronoUnit.DAYS)

            while (time.isBefore(end)) {
                val jd = JulianDate.fromInstant(time)

                val moon = CelestialPositions.toEquatorial(CelestialPositions.moon(jd), jd)
                val sun = MoonOrientation.sunEquatorial(jd)

                val viaPositionAngles = MoonOrientation.brightLimbAngleFromZenith(jd, location)
                val viaHorizon = MoonOrientation.bearingFromZenith(
                    from = MoonOrientation.horizontal(moon, jd, location),
                    to = MoonOrientation.horizontal(sun, jd, location),
                )

                val error = angleBetween(viaPositionAngles, viaHorizon)
                worst = maxOf(worst, error)
                checked++

                assertTrue(
                    "At $time from $location the limb angle was $viaPositionAngles " +
                        "but the Sun lies at $viaHorizon",
                    error < 0.01,
                )

                time = time.plus(6, ChronoUnit.HOURS)
            }
        }

        assertTrue("Expected a full lunation at three sites", checked > 300)
        assertTrue("Worst disagreement was $worst degrees", worst < 0.01)
    }

    // --- Parallactic angle, on its own ---

    /**
     * On the meridian the tilt is always exactly 0 or 180, and which one depends
     * on whether the object culminates on the equator side of the zenith or
     * beyond it. Both cases are worth pinning down: between them they are the
     * whole reason the Moon looks different from different latitudes.
     */
    @Test
    fun `parallactic angle is zero when culminating on the equator side of the zenith`() {
        // Declination below the observer's latitude: the object tops out due
        // south, and going up from it along the meridian reaches the zenith and
        // then the pole, both in the same direction. No tilt.
        val q = MoonOrientation.parallacticAngle(
            hourAngle = 0.0,
            declination = 20.0,
            latitude = 51.5,
        )
        assertEquals(0.0, q, 1e-9)
    }

    @Test
    fun `parallactic angle is 180 degrees when culminating beyond the zenith`() {
        // Declination above the observer's latitude puts the object north of the
        // zenith, so the pole now lies below it while the zenith is above.
        val q = MoonOrientation.parallacticAngle(
            hourAngle = 0.0,
            declination = 70.0,
            latitude = 51.5,
        )
        assertEquals(180.0, abs(q), 1e-9)
    }

    @Test
    fun `parallactic angle is 180 degrees when culminating on the far side of the zenith`() {
        // A southern observer looking north at the Moon has the south celestial
        // pole behind them, so the sky is turned right over. This is the
        // hemisphere flip, arriving without a hemisphere branch.
        val q = MoonOrientation.parallacticAngle(
            hourAngle = 0.0,
            declination = 0.0,
            latitude = -32.9283,
        )
        assertEquals(180.0, abs(q), 1e-9)
    }

    @Test
    fun `parallactic angle changes sign either side of the meridian`() {
        val before = MoonOrientation.parallacticAngle(-45.0, 10.0, 51.5)
        val after = MoonOrientation.parallacticAngle(45.0, 10.0, 51.5)

        assertTrue("Expected opposite tilts, got $before and $after", before * after < 0)
        assertEquals("and symmetric ones", abs(before), abs(after), 1e-9)
    }

    /**
     * The claim made to the user when this was specified: the disc rolls through
     * roughly 114° between the horizons at Newcastle's latitude. If that is
     * wrong the whole feature is not worth having.
     */
    @Test
    fun `the disc rolls by more than a right angle between the horizons`() {
        // An equatorial object rises and sets at hour angles of -90 and +90 for
        // any observer, which makes the endpoints exact rather than searched for.
        val atRise = MoonOrientation.parallacticAngle(-90.0, 0.0, newcastle.latitude)
        val atTransit = MoonOrientation.parallacticAngle(0.0, 0.0, newcastle.latitude)
        val atSet = MoonOrientation.parallacticAngle(90.0, 0.0, newcastle.latitude)

        assertEquals(180.0, abs(atTransit), 1e-9)

        val swing = angleBetween(atRise, atTransit) + angleBetween(atTransit, atSet)
        assertEquals("total roll from horizon to horizon", 114.0, swing, 2.0)
    }

    @Test
    fun `northern and southern observers see the Moon close to upside down`() {
        val jd = JulianDate.fromInstant(Instant.parse("2026-03-21T12:00:00Z"))

        // Mirrored latitudes on the same meridian, so only the hemisphere differs.
        val north = GeoLocation(latitude = 40.0, longitude = 0.0)
        val south = GeoLocation(latitude = -40.0, longitude = 0.0)

        val difference = angleBetween(
            MoonOrientation.brightLimbAngleFromZenith(jd, north),
            MoonOrientation.brightLimbAngleFromZenith(jd, south),
        )
        // Not a clean 180: χ is the same for both observers and only q differs,
        // and q only reaches its full opposition on the meridian. Away from it
        // the two views are strongly but not exactly inverted.
        assertTrue("Expected roughly opposite, got $difference degrees apart", difference > 100.0)
    }

    // --- Horizon coordinates, used by the cross-check ---

    @Test
    fun `an object on the meridian sits due south for a northern observer`() {
        val jd = JulianDate.fromInstant(Instant.parse("2026-06-01T00:00:00Z"))
        val lst = normalizeDegrees(
            CelestialPositions.greenwichMeanSiderealTime(jd) + london.longitude
        )
        // Right ascension equal to the local sidereal time puts the hour angle at
        // zero, and a declination below the observer's latitude puts it south.
        val onMeridian = EquatorialPosition(rightAscension = lst, declination = 0.0)

        val where = MoonOrientation.horizontal(onMeridian, jd, london)
        assertEquals(180.0, where.azimuth, 1e-6)
        assertEquals(90.0 - london.latitude, where.altitude, 1e-6)
    }

    @Test
    fun `bearing is zero straight up and ninety to the right`() {
        val moon = Horizontal(altitude = 30.0, azimuth = 90.0)

        assertEquals(
            0.0,
            MoonOrientation.bearingFromZenith(moon, Horizontal(altitude = 50.0, azimuth = 90.0)),
            1e-9,
        )
        assertEquals(
            180.0,
            MoonOrientation.bearingFromZenith(moon, Horizontal(altitude = 10.0, azimuth = 90.0)),
            1e-9,
        )
        // Same altitude, slightly further clockwise round the compass, so
        // directly right. Kept to a small step because over a longer arc the
        // great circle bends towards the zenith and the bearing genuinely is
        // less than 90 — at 30 degrees of azimuth it comes out near 82.
        assertEquals(
            90.0,
            MoonOrientation.bearingFromZenith(moon, Horizontal(altitude = 30.0, azimuth = 90.5)),
            0.2,
        )
        assertTrue(
            "over a long arc the bearing should bend towards the zenith",
            MoonOrientation.bearingFromZenith(moon, Horizontal(altitude = 30.0, azimuth = 120.0)) < 88.0,
        )
    }

    // --- Continuity ---

    /**
     * London rather than Newcastle: the Moon's declination never exceeds about
     * 28.7°, so from 51.5°N it cannot pass near the zenith. Close to the zenith
     * the tilt genuinely does swing very fast, and that real behaviour would
     * mask an actual discontinuity in a test like this one.
     */
    @Test
    fun `the angle never jumps between adjacent minutes`() {
        var time = Instant.parse("2026-02-01T00:00:00Z")
        val end = time.plus(3, ChronoUnit.DAYS)
        var previous: Double? = null
        var worst = 0.0

        while (time.isBefore(end)) {
            val angle = MoonOrientation.brightLimbAngleFromZenith(
                JulianDate.fromInstant(time), london,
            )
            previous?.let {
                // Wrapping through 360 is not a jump, so compare the short way round.
                val step = angleBetween(angle, it)
                worst = maxOf(worst, step)
                assertTrue("Jumped $step degrees in a minute at $time", step < 1.0)
            }
            previous = angle
            time = time.plus(1, ChronoUnit.MINUTES)
        }

        assertTrue("Expected some movement, worst step was $worst", worst > 0.0)
    }
}
