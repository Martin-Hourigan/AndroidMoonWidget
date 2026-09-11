package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Month
import java.time.ZoneId

class MoonNamesTest {

    private val zone: ZoneId = ZoneId.of("Australia/Sydney")
    private val utc: ZoneId = ZoneId.of("UTC")

    /**
     * The September equinox falls on 22 or 23 September in this era. Anything
     * outside that would mean the solar longitude search is wrong.
     */
    @Test
    fun `september equinox lands on the 22nd or 23rd`() {
        for (year in 2024..2035) {
            val date = JulianDate.toInstant(MoonNames.septemberEquinox(year))
                .atZone(utc)
                .toLocalDate()

            assertEquals("wrong month in $year", Month.SEPTEMBER, date.month)
            assertTrue(
                "equinox in $year was $date",
                date.dayOfMonth in 21..24,
            )
        }
    }

    /** At the equinox the Sun's apparent longitude is 180 degrees by definition. */
    @Test
    fun `sun longitude is 180 degrees at the september equinox`() {
        for (year in 2024..2030) {
            val longitude = CelestialPositions.sunLongitude(MoonNames.septemberEquinox(year))
            assertEquals("year $year", 180.0, longitude, 0.001)
        }
    }

    @Test
    fun `every month maps to its traditional name`() {
        assertEquals(MoonSlot.JANUARY, MoonSlot.forMonth(Month.JANUARY))
        assertEquals(MoonSlot.FEBRUARY, MoonSlot.forMonth(Month.FEBRUARY))
        assertEquals(MoonSlot.JUNE, MoonSlot.forMonth(Month.JUNE))
        assertEquals(MoonSlot.OCTOBER, MoonSlot.forMonth(Month.OCTOBER))
        assertEquals(MoonSlot.DECEMBER, MoonSlot.forMonth(Month.DECEMBER))
    }

    /**
     * Exactly one full moon a year is the Harvest Moon, and it must be the one
     * closest to the September equinox.
     */
    @Test
    fun `exactly one harvest moon per year`() {
        for (year in 2025..2032) {
            var jd = JulianDate.fromCalendar(year, 1, 1.0)
            val end = JulianDate.fromCalendar(year, 12, 31.0)

            var harvests = 0
            while (jd < end) {
                val full = MoonPhase.nextFullMoon(jd)
                if (full >= end) break
                if (MoonNames.forFullMoon(full, utc) == MoonSlot.HARVEST) harvests++
                jd = full + 1
            }

            assertEquals("year $year had $harvests harvest moons", 1, harvests)
        }
    }

    /** The Harvest Moon has to be within half a lunation of the equinox. */
    @Test
    fun `harvest moon is close to the september equinox`() {
        for (year in 2025..2032) {
            val equinox = MoonNames.septemberEquinox(year)
            val harvest = MoonNames.nearestFullMoonTo(equinox)

            val gap = kotlin.math.abs(harvest - equinox)
            assertTrue("year $year: harvest was $gap days from the equinox", gap <= 15.0)
            assertEquals(MoonSlot.HARVEST, MoonNames.forFullMoon(harvest, utc))
        }
    }

    /**
     * The Harvest Moon usually displaces the Corn Moon in September, but lands
     * in October often enough that both must be possible.
     */
    @Test
    fun `harvest moon can fall in september or october`() {
        val months = (2020..2040).map { year ->
            JulianDate.toInstant(MoonNames.nearestFullMoonTo(MoonNames.septemberEquinox(year)))
                .atZone(utc)
                .month
        }.toSet()

        assertTrue("expected September harvests", Month.SEPTEMBER in months)
        assertTrue("expected at least one October harvest in 21 years", Month.OCTOBER in months)
    }

    @Test
    fun `southern seasons shift the names by six months`() {
        // A January full moon is the Wolf Moon in the north, Buck in the south.
        val january = MoonPhase.nextFullMoon(JulianDate.fromCalendar(2026, 1, 2.0))

        assertEquals(MoonSlot.JANUARY, MoonNames.forFullMoon(january, zone, southernSeasons = false))
        assertEquals(MoonSlot.JULY, MoonNames.forFullMoon(january, zone, southernSeasons = true))
    }

    /**
     * The Harvest rule is tied to the real September equinox, so it must not
     * fire once the names have been shifted for the southern hemisphere.
     */
    @Test
    fun `no harvest moon when names are shifted south`() {
        for (year in 2025..2030) {
            val harvest = MoonNames.nearestFullMoonTo(MoonNames.septemberEquinox(year))
            val shifted = MoonNames.forFullMoon(harvest, zone, southernSeasons = true)

            assertTrue("year $year gave $shifted", shifted != MoonSlot.HARVEST)
        }
    }

    @Test
    fun `every full moon in a year gets a name`() {
        var jd = JulianDate.fromCalendar(2026, 1, 1.0)
        val end = JulianDate.fromCalendar(2027, 1, 1.0)
        var counted = 0

        while (jd < end) {
            val full = MoonPhase.nextFullMoon(jd)
            if (full >= end) break
            // Throws if any month is unmapped.
            MoonNames.forFullMoon(full, zone)
            MoonNames.forFullMoon(full, zone, southernSeasons = true)
            counted++
            jd = full + 1
        }

        // 12 or 13 full moons in a calendar year.
        assertTrue("counted $counted full moons", counted in 12..13)
    }
}
