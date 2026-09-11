package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * These mirror a Java prototype that was run against Meeus' printed worked
 * examples before the Kotlin was written. The textbook values below are the
 * reference; if a change breaks one of these, the change is wrong.
 */
class JulianDateTest {

    @Test
    fun `J2000 epoch`() {
        assertEquals(2451545.0, JulianDate.fromCalendar(2000, 1, 1.5), 1e-9)
    }

    @Test
    fun `Meeus example 7a - Sputnik launch`() {
        assertEquals(2436116.31, JulianDate.fromCalendar(1957, 10, 4.81), 1e-6)
    }

    @Test
    fun `instant and calendar agree`() {
        val date = LocalDate.of(2026, 8, 22)
        val viaCalendar = JulianDate.fromCalendar(2026, 8, 22.0)
        val viaInstant = JulianDate.fromInstant(date.atStartOfDay(ZoneId.of("UTC")).toInstant())
        assertEquals(viaCalendar, viaInstant, 1e-9)
    }
}

class CelestialPositionsTest {

    /** Meeus, Example 47.a — Moon at 1992 April 12, 0h TD. */
    @Test
    fun `moon position matches Meeus example 47a`() {
        val jde = JulianDate.fromCalendar(1992, 4, 12.0)
        assertEquals(2448724.5, jde, 1e-9)

        val position = CelestialPositions.moon(jde)
        assertEquals(133.162655, position.longitude, 1e-4)
        assertEquals(-3.229126, position.latitude, 1e-4)
        assertEquals(368409.7, position.distanceKm, 1.0)
    }

    /** Meeus, Example 25.b — Sun at 1992 October 13, 0h TD. */
    @Test
    fun `sun longitude matches Meeus example 25b`() {
        val jde = JulianDate.fromCalendar(1992, 10, 13.0)
        assertEquals(199.90895, CelestialPositions.sunLongitude(jde), 0.01)
    }

    @Test
    fun `obliquity is near 23,4 degrees in the modern era`() {
        val obliquity = CelestialPositions.obliquity(JulianDate.J2000)
        assertTrue("got $obliquity", obliquity in 23.4..23.5)
    }
}

class MoonPhaseTest {

    /**
     * Known lunation instants. Elongation should be ~0 at new, ~180 at full,
     * ~90 at first quarter — this exercises the Moon and Sun code together,
     * so it catches errors that either one alone would hide.
     */
    @Test
    fun `elongation matches known lunations of January 2024`() {
        val newMoon = MoonPhase.at(JulianDate.fromCalendar(2024, 1, 11 + 11.95 / 24.0))
        assertTrue(
            "new moon elongation was ${newMoon.phaseAngle}",
            newMoon.phaseAngle < 0.5 || newMoon.phaseAngle > 359.5,
        )
        assertTrue("new moon illumination was ${newMoon.illumination}", newMoon.illumination < 0.001)

        val fullMoon = MoonPhase.at(JulianDate.fromCalendar(2024, 1, 25 + 17.9 / 24.0))
        assertEquals(180.0, fullMoon.phaseAngle, 0.5)
        assertTrue("full moon illumination was ${fullMoon.illumination}", fullMoon.illumination > 0.999)

        val firstQuarter = MoonPhase.at(JulianDate.fromCalendar(2024, 1, 18 + 3.88 / 24.0))
        assertEquals(90.0, firstQuarter.phaseAngle, 0.5)
        assertEquals(0.5, firstQuarter.illumination, 0.01)
    }

    @Test
    fun `phase names land on the right quarters`() {
        assertEquals(PhaseName.NEW_MOON, MoonPhase.at(JulianDate.fromCalendar(2024, 1, 11 + 11.95 / 24.0)).phaseName)
        assertEquals(PhaseName.FULL_MOON, MoonPhase.at(JulianDate.fromCalendar(2024, 1, 25 + 17.9 / 24.0)).phaseName)
        assertEquals(PhaseName.FIRST_QUARTER, MoonPhase.at(JulianDate.fromCalendar(2024, 1, 18 + 3.88 / 24.0)).phaseName)
    }

    @Test
    fun `waxing flag flips at full moon`() {
        assertTrue(MoonPhase.at(JulianDate.fromCalendar(2024, 1, 18 + 3.88 / 24.0)).isWaxing)
        assertTrue(!MoonPhase.at(JulianDate.fromCalendar(2024, 2, 2.0)).isWaxing)
    }

    @Test
    fun `next full moon is found within one lunation and is actually full`() {
        val start = JulianDate.fromCalendar(2026, 8, 1.0)
        val full = MoonPhase.nextFullMoon(start)

        assertTrue("next full moon should be after the start", full > start)
        assertTrue("should be within one lunation", full - start <= MoonPhase.SYNODIC_MONTH + 1)
        assertEquals(180.0, MoonPhase.at(full).phaseAngle, 0.01)
    }

    @Test
    fun `next new moon is found and is actually new`() {
        val start = JulianDate.fromCalendar(2026, 8, 1.0)
        val new = MoonPhase.nextNewMoon(start)

        assertTrue(new > start)
        assertTrue(new - start <= MoonPhase.SYNODIC_MONTH + 1)
        val angle = MoonPhase.at(new).phaseAngle
        assertTrue("elongation was $angle", angle < 0.01 || angle > 359.99)
    }

    @Test
    fun `illumination is symmetric either side of full`() {
        val full = MoonPhase.nextFullMoon(JulianDate.fromCalendar(2026, 8, 1.0))
        val before = MoonPhase.at(full - 3).illumination
        val after = MoonPhase.at(full + 3).illumination
        assertEquals(before, after, 0.05)
    }
}

class ZodiacTest {

    @Test
    fun `sign boundaries`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.forLongitude(0.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.forLongitude(29.99))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.forLongitude(30.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.forLongitude(359.99))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.forLongitude(360.0))
    }

    @Test
    fun `negative and oversized longitudes wrap`() {
        assertEquals(ZodiacSign.PISCES, ZodiacSign.forLongitude(-1.0))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.forLongitude(390.0))
    }

    @Test
    fun `degrees into sign stay in range`() {
        var jd = JulianDate.fromCalendar(2026, 1, 1.0)
        repeat(60) {
            val moonSign = MoonZodiac.at(jd)
            assertTrue(moonSign.degreesIntoSign >= 0.0)
            assertTrue(moonSign.degreesIntoSign < 30.0)
            jd += 0.5
        }
    }

    @Test
    fun `moon works through all twelve signs within a month`() {
        var jd = JulianDate.fromCalendar(2026, 1, 1.0)
        val seen = mutableSetOf<ZodiacSign>()
        repeat(120) {
            seen += MoonZodiac.at(jd).sign
            jd += 0.25
        }
        assertEquals("moon should visit every sign in a lunar month", 12, seen.size)
    }
}

class RiseSetTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val newcastleZone = ZoneId.of("Australia/Sydney")

    @Test
    fun `altitude at the reported rise equals the horizon altitude`() {
        val times = RiseSet.forDate(LocalDate.of(2026, 8, 22), newcastle, newcastleZone)
        val rise = requireNotNull(times.rise)

        val jd = JulianDate.fromInstant(rise)
        val altitude = RiseSet.altitude(jd, newcastle)
        // The Moon's centre sits a touch above the geometric horizon at rise.
        assertEquals(0.09, altitude, 0.05)
    }

    @Test
    fun `moonrise slips later by roughly fifty minutes a day`() {
        var previous: Int? = null
        var comparisons = 0

        for (offset in 0..7) {
            val date = LocalDate.of(2026, 8, 20).plusDays(offset.toLong())
            val rise = RiseSet.forDate(date, newcastle, newcastleZone).rise ?: continue
            val minutes = rise.atZone(newcastleZone).toLocalTime().toSecondOfDay() / 60

            previous?.let {
                var slip = minutes - it
                if (slip < -600) slip += 1440 // crossed midnight
                assertTrue("daily slip was $slip minutes", slip in 20..90)
                comparisons++
            }
            previous = minutes
        }
        assertTrue("expected several days to compare", comparisons >= 5)
    }

    /**
     * A full moon is opposite the Sun, so it rises as the Sun sets. This is the
     * strongest single check available without external ephemeris data: it only
     * passes if position, coordinate conversion and sidereal time all agree.
     */
    @Test
    fun `full moon rises close to sunset`() {
        val fullMoonJd = MoonPhase.nextFullMoon(JulianDate.fromCalendar(2026, 8, 1.0))
        val date = JulianDate.toInstant(fullMoonJd).atZone(newcastleZone).toLocalDate()

        val moonrise = requireNotNull(RiseSet.forDate(date, newcastle, newcastleZone).rise)
        val sunset = requireNotNull(approximateSunset(date))

        val gap = abs(Duration.between(moonrise, sunset).toMinutes())
        assertTrue("moonrise was $gap minutes from sunset", gap < 75)
    }

    @Test
    fun `a day with no moonset is reported as null rather than a wrong time`() {
        // London, 2026-08-22: the Moon rises but does not set before midnight.
        val london = GeoLocation(51.5074, -0.1278)
        val times = RiseSet.forDate(LocalDate.of(2026, 8, 22), london, ZoneId.of("Europe/London"))

        assertNotNull("expected a moonrise", times.rise)
        assertNull("expected no moonset on this date", times.set)
    }

    @Test
    fun `polar midwinter is handled without inventing times`() {
        val tromso = GeoLocation(69.6492, 18.9553)
        val zone = ZoneId.of("Europe/Oslo")
        val times = RiseSet.forDate(LocalDate.of(2026, 12, 21), tromso, zone)

        // Whatever the outcome, it must be self-consistent: no rise/set implies
        // the Moon stayed on one side of the horizon all day.
        if (times.rise == null && times.set == null) {
            assertTrue("must flag which side of the horizon", times.alwaysUp || times.alwaysDown)
        }
    }

    /** Rough sunset, good to a minute or two — only used for the full-moon check. */
    private fun approximateSunset(date: LocalDate): java.time.Instant? {
        val start = date.atStartOfDay(newcastleZone).toInstant()
        val jdStart = JulianDate.fromInstant(start)
        val step = 10.0 / (24.0 * 60.0)

        var previousJd = jdStart
        var previousAltitude = sunAltitude(previousJd)
        for (i in 1..144) {
            val jd = jdStart + i * step
            val altitude = sunAltitude(jd)
            if (previousAltitude > 0 && altitude <= 0) {
                var low = previousJd
                var high = jd
                repeat(30) {
                    val mid = (low + high) / 2
                    if (sunAltitude(mid) > 0) low = mid else high = mid
                }
                return JulianDate.toInstant((low + high) / 2)
            }
            previousJd = jd
            previousAltitude = altitude
        }
        return null
    }

    private fun sunAltitude(jd: Double): Double {
        val longitude = CelestialPositions.sunLongitude(jd)
        val equatorial = CelestialPositions.toEquatorial(
            EclipticPosition(longitude, 0.0, 149_600_000.0), jd,
        )
        val lst = normalizeDegrees(CelestialPositions.greenwichMeanSiderealTime(jd) + newcastle.longitude)
        val hourAngle = normalizeSignedDegrees(lst - equatorial.rightAscension)

        val lat = newcastle.latitude.radians
        val dec = equatorial.declination.radians
        val sinAltitude = kotlin.math.sin(lat) * kotlin.math.sin(dec) +
            kotlin.math.cos(lat) * kotlin.math.cos(dec) * kotlin.math.cos(hourAngle.radians)
        return kotlin.math.asin(sinAltitude.coerceIn(-1.0, 1.0)).degrees + 0.833
    }
}
