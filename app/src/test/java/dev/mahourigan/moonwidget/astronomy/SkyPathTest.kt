package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * The path across the sky.
 *
 * The load-bearing check is [`sampled pass length matches the closed form`]:
 * the drawn path comes from sampling altitudes, while `cos H₀ = −tan φ·tan δ`
 * is pure trigonometry that never touches the sampler. Agreement between them
 * means the picture is telling the truth.
 */
class SkyPathTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val london = GeoLocation(latitude = 51.5074, longitude = -0.1278)
    private val quito = GeoLocation(latitude = -0.1807, longitude = -78.4678)
    private val tromso = GeoLocation(latitude = 69.6492, longitude = 18.9553)

    private val someMoment: Instant = Instant.parse("2026-08-23T07:00:00Z")

    private fun declinationAt(time: Instant): Double {
        val jd = JulianDate.fromInstant(time)
        return CelestialPositions.toEquatorial(CelestialPositions.moon(jd), jd).declination
    }

    // --- The closed form, on its own ---

    @Test
    fun `on the equator everything is up for half the time`() {
        // tan(0) is zero, so the chord runs through the centre whatever the
        // declination, and the circle is halved.
        listOf(-25.0, 0.0, 25.0).forEach { declination ->
            val h0 = SkyPath.horizonHourAngle(latitude = 0.0, declination = declination)
            assertEquals("declination $declination", 90.0, h0!!, 1e-9)
        }
    }

    @Test
    fun `a body on the celestial equator is up for half the time anywhere`() {
        listOf(-60.0, -32.9, 0.0, 51.5).forEach { latitude ->
            val h0 = SkyPath.horizonHourAngle(latitude = latitude, declination = 0.0)
            assertEquals("latitude $latitude", 90.0, h0!!, 1e-9)
        }
    }

    @Test
    fun `the split reverses between hemispheres`() {
        // Declination north of the equator favours northern observers and
        // short-changes southern ones, and the two swap exactly.
        val north = SkyPath.hoursAboveHorizon(latitude = 40.0, declination = 20.0)
        val south = SkyPath.hoursAboveHorizon(latitude = -40.0, declination = 20.0)
        val lunarDay = SkyPath.LUNAR_DAY.toMinutes() / 60.0

        assertTrue("north should get the long half, got $north", north > 13.0)
        assertTrue("south should get the short half, got $south", south < 11.5)
        assertEquals("the two halves must account for the whole day", lunarDay, north + south, 1e-9)
    }

    /** The range quoted when this was designed, checked rather than asserted from memory. */
    @Test
    fun `at Newcastle the visible half swings by hours across a month`() {
        val shortest = SkyPath.hoursAboveHorizon(newcastle.latitude, declination = 28.5)
        val longest = SkyPath.hoursAboveHorizon(newcastle.latitude, declination = -28.5)

        assertEquals("shortest pass", 9.6, shortest, 0.3)
        assertEquals("longest pass", 15.2, longest, 0.3)
    }

    @Test
    fun `a circumpolar body has no horizon crossing`() {
        // From Tromso a declination of 25 degrees north never sets.
        assertNull(SkyPath.horizonHourAngle(latitude = tromso.latitude, declination = 25.0))
        assertEquals(
            SkyPath.LUNAR_DAY.toMinutes() / 60.0,
            SkyPath.hoursAboveHorizon(tromso.latitude, 25.0),
            1e-9,
        )
        // And a declination of 25 degrees south never comes up at all.
        assertNull(SkyPath.horizonHourAngle(latitude = tromso.latitude, declination = -25.0))
        assertEquals(0.0, SkyPath.hoursAboveHorizon(tromso.latitude, -25.0), 1e-9)
    }

    // --- The cross-check ---

    @Test
    fun `sampled pass length matches the closed form`() {
        var checked = 0

        for (location in listOf(newcastle, london, quito)) {
            // Every few days for a month, so the declination sweeps its range.
            var day = Instant.parse("2026-01-01T12:00:00Z")
            val end = day.plus(28, ChronoUnit.DAYS)

            while (day.isBefore(end)) {
                val pass = SkyPath.currentOrNextPass(day, location)
                if (pass != null) {
                    val sampled = pass.duration.toMinutes() / 60.0

                    // Declination at mid-pass: it drifts a little during the
                    // pass, and the midpoint is the fair single value to use.
                    val middle = pass.rise.plus(Duration.between(pass.rise, pass.set).dividedBy(2))
                    val predicted = SkyPath.hoursAboveHorizon(
                        location.latitude, declinationAt(middle),
                    )

                    // Not exact, and should not be: the sampler times the upper
                    // limb with refraction and parallax, while the closed form
                    // uses the centre at zero altitude. That is worth minutes,
                    // not hours — an error in the geometry would be hours.
                    assertTrue(
                        "At $day from $location the pass ran $sampled h " +
                            "but the closed form predicts $predicted h",
                        abs(sampled - predicted) < 0.35,
                    )
                    checked++
                }
                day = day.plus(3, ChronoUnit.DAYS)
            }
        }

        assertTrue("expected passes at three sites over a month", checked > 20)
    }

    @Test
    fun `peak altitude matches the latitude and declination`() {
        val pass = SkyPath.currentOrNextPass(someMoment, newcastle)
        assertNotNull(pass)

        val points = SkyPath.samples(pass!!.rise, pass.set, newcastle, count = 200)
        val peak = points.maxOf { it.altitude }

        // A body culminates at 90 minus the gap between latitude and declination.
        val middle = pass.rise.plus(Duration.between(pass.rise, pass.set).dividedBy(2))
        val expected = 90.0 - abs(newcastle.latitude - declinationAt(middle))

        assertEquals("peak altitude", expected, peak, 0.6)
    }

    // --- Sampling ---

    @Test
    fun `samples run in order and start and end where asked`() {
        val (from, to) = SkyPath.windowAround(someMoment)
        val points = SkyPath.samples(from, to, newcastle, count = 50)

        assertEquals(50, points.size)
        assertEquals(from, points.first().time)
        assertEquals(to, points.last().time)
        points.zipWithNext().forEach { (a, b) ->
            assertTrue("times must increase", b.time.isAfter(a.time))
        }
        points.forEach {
            assertTrue("altitude ${it.altitude} out of range", it.altitude in -90.0..90.0)
        }
    }

    @Test
    fun `a full window contains both a rise and a set`() {
        val (from, to) = SkyPath.windowAround(someMoment)
        val points = SkyPath.samples(from, to, newcastle, count = 300)

        val crossings = points.zipWithNext().count { (a, b) -> (a.altitude < 0) != (b.altitude < 0) }
        assertTrue("expected the horizon crossed twice, saw $crossings", crossings >= 2)
    }

    @Test
    fun `the window is one lunar day centred on the moment`() {
        val (from, to) = SkyPath.windowAround(someMoment)
        assertEquals(SkyPath.LUNAR_DAY.toMinutes(), Duration.between(from, to).toMinutes())
        assertEquals(
            Duration.between(from, someMoment).toMinutes(),
            Duration.between(someMoment, to).toMinutes(),
        )
    }

    // --- Passes ---

    @Test
    fun `the pass brackets the moment when the Moon is up`() {
        val next = RiseSet.nextEvents(someMoment, newcastle)
        val pass = SkyPath.currentOrNextPass(someMoment, newcastle)
        assertNotNull(pass)

        if (next.currentlyUp) {
            assertTrue("rise should already have happened", !pass!!.rise.isAfter(someMoment))
            assertTrue("set should still be ahead", pass.set.isAfter(someMoment))
        } else {
            assertTrue("the whole pass should be ahead", pass!!.rise.isAfter(someMoment))
        }
        assertTrue("a pass must run forwards", pass.set.isAfter(pass.rise))
    }

    @Test
    fun `the fraction runs from zero at the rise to one at the set`() {
        val pass = SkyPass(
            rise = Instant.parse("2026-08-23T03:00:00Z"),
            set = Instant.parse("2026-08-23T15:00:00Z"),
        )
        assertEquals(0.0, pass.fractionAt(pass.rise), 1e-9)
        assertEquals(1.0, pass.fractionAt(pass.set), 1e-9)
        assertEquals(0.5, pass.fractionAt(Instant.parse("2026-08-23T09:00:00Z")), 1e-9)
    }

    @Test
    fun `the fraction clamps outside the pass`() {
        val pass = SkyPass(
            rise = Instant.parse("2026-08-23T03:00:00Z"),
            set = Instant.parse("2026-08-23T15:00:00Z"),
        )
        assertEquals(0.0, pass.fractionAt(Instant.parse("2026-08-22T00:00:00Z")), 1e-9)
        assertEquals(1.0, pass.fractionAt(Instant.parse("2026-08-24T00:00:00Z")), 1e-9)
    }

    @Test
    fun `the Moon is genuinely above the horizon during its own pass`() {
        val pass = SkyPath.currentOrNextPass(someMoment, newcastle)
        assertNotNull(pass)

        // Sampled strictly inside, so the horizon endpoints themselves are not
        // caught by rounding.
        val inside = SkyPath.samples(pass!!.rise, pass.set, newcastle, count = 20).drop(1).dropLast(1)
        inside.forEach {
            assertTrue("altitude ${it.altitude} at ${it.time} should be above the horizon", it.altitude > 0)
        }
    }
}
