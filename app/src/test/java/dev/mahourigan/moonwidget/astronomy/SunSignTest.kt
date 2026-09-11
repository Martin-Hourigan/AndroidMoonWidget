package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset

class SunSignTest {

    private fun signOn(year: Int, month: Int, day: Int): ZodiacSign =
        SunZodiac.at(
            JulianDate.fromInstant(
                LocalDate.of(year, month, day).atTime(12, 0).toInstant(ZoneOffset.UTC)
            )
        )

    // --- Tropical: computed, so it should match the customary dates ---

    @Test
    fun `the equinox opens Aries`() {
        // 0 degrees Aries *is* the March equinox, by definition — this is the
        // one anchor the whole tropical zodiac hangs off.
        assertEquals(ZodiacSign.ARIES, signOn(2026, 3, 21))
        assertEquals(ZodiacSign.PISCES, signOn(2026, 3, 19))
    }

    @Test
    fun `the solstices open Cancer and Capricorn`() {
        assertEquals(ZodiacSign.CANCER, signOn(2026, 6, 22))
        assertEquals(ZodiacSign.CAPRICORN, signOn(2026, 12, 23))
    }

    @Test
    fun `the September equinox opens Libra`() {
        assertEquals(ZodiacSign.LIBRA, signOn(2026, 9, 24))
        assertEquals(ZodiacSign.VIRGO, signOn(2026, 9, 21))
    }

    @Test
    fun `every computed sign agrees with its own quoted dates`() {
        // Walk a whole year and check the sign never contradicts the range the
        // app would print beside it. Boundary days are skipped: the real
        // crossing moves by up to a day with the leap cycle, which is exactly
        // why the quoted dates are called conventional.
        var date = LocalDate.of(2026, 1, 1)
        var checked = 0

        while (date.year == 2026) {
            val sign = signOn(date.year, date.monthValue, date.dayOfMonth)
            val (from, to) = SunZodiac.conventionalDates(sign)
            val today = MonthDay.of(date.month, date.dayOfMonth)

            val nearBoundary = daysBetween(from, today) <= 1 || daysBetween(to, today) <= 1
            if (!nearBoundary) {
                assertTrue(
                    "$today computed as $sign, whose range is $from..$to",
                    withinRange(today, from, to),
                )
                checked++
            }
            date = date.plusDays(1)
        }

        assertTrue("expected most of the year checked, got $checked", checked > 300)
    }

    @Test
    fun `the twelve sign ranges tile the year without gap or overlap`() {
        ZodiacSign.entries.forEach { sign ->
            val (_, to) = SunZodiac.conventionalDates(sign)
            val next = ZodiacSign.entries[(sign.ordinal + 1) % ZodiacSign.entries.size]
            val (nextFrom, _) = SunZodiac.conventionalDates(next)

            assertEquals(
                "$sign should end the day before $next begins",
                1,
                daysBetween(to, nextFrom),
            )
        }
    }

    // --- Astronomical: the thirteen real constellations ---

    @Test
    fun `Ophiuchus is where the Sun really is in early December`() {
        assertEquals(Constellation.OPHIUCHUS, Constellation.at(MonthDay.of(12, 1)))
        assertEquals(Constellation.OPHIUCHUS, Constellation.at(MonthDay.of(11, 26)))
        assertEquals(Constellation.OPHIUCHUS, Constellation.at(MonthDay.of(12, 14)))
    }

    @Test
    fun `Scorpius gets barely a week`() {
        assertEquals(Constellation.SCORPIUS, Constellation.at(MonthDay.of(11, 19)))
        assertEquals(Constellation.SCORPIUS, Constellation.at(MonthDay.of(11, 25)))
        // Handed straight over to Ophiuchus.
        assertEquals(Constellation.OPHIUCHUS, Constellation.at(MonthDay.of(11, 26)))
    }

    @Test
    fun `the year-end constellation wraps into January`() {
        // Sagittarius starts 15 December and runs past New Year, so a date
        // before the first start of the year still has to resolve.
        assertEquals(Constellation.SAGITTARIUS, Constellation.at(MonthDay.of(12, 20)))
        assertEquals(Constellation.SAGITTARIUS, Constellation.at(MonthDay.of(1, 1)))
        assertEquals(Constellation.SAGITTARIUS, Constellation.at(MonthDay.of(1, 17)))
        assertEquals(Constellation.CAPRICORNUS, Constellation.at(MonthDay.of(1, 18)))
    }

    @Test
    fun `every day of the year lands in exactly one constellation`() {
        var date = LocalDate.of(2024, 1, 1) // a leap year, so 29 February is included
        var days = 0

        while (date.year == 2024) {
            // Would throw or return null if a day fell through the boundaries.
            Constellation.at(date)
            days++
            date = date.plusDays(1)
        }
        assertEquals(366, days)
    }

    @Test
    fun `each constellation ends the day before the next begins`() {
        Constellation.entries.forEach { constellation ->
            val next = Constellation.entries[(constellation.ordinal + 1) % Constellation.entries.size]
            assertEquals(
                "$constellation should end the day before $next starts",
                1,
                daysBetween(constellation.end, next.start),
            )
        }
    }

    @Test
    fun `the starts are in calendar order, which the lookup relies on`() {
        val starts = Constellation.entries.map { it.start }
        assertEquals(starts.sorted(), starts)
    }

    @Test
    fun `Virgo is the longest stretch and Scorpius the shortest`() {
        val spans = Constellation.entries.associateWith { spanDays(it) }
        assertEquals(Constellation.VIRGO, spans.maxBy { it.value }.key)
        assertEquals(Constellation.SCORPIUS, spans.minBy { it.value }.key)
        // 44 and 7 days respectively, per the published tables.
        assertEquals(44, spans.getValue(Constellation.VIRGO))
        assertEquals(7, spans.getValue(Constellation.SCORPIUS))
    }

    @Test
    fun `the spans add up to a whole year`() {
        assertEquals(365, Constellation.entries.sumOf { spanDays(it) })
    }

    // --- The point of showing both ---

    @Test
    fun `the two systems disagree for most of the year`() {
        // Precession has moved the sky about 24 degrees since the tropical
        // dates were fixed, so they should almost never agree.
        var date = LocalDate.of(2026, 1, 1)
        var same = 0
        var total = 0

        while (date.year == 2026) {
            val tropical = signOn(date.year, date.monthValue, date.dayOfMonth).name
            val astronomical = Constellation.at(date).name
                // Scorpio/Scorpius and Capricorn/Capricornus are the same word.
                .replace("SCORPIUS", "SCORPIO")
                .replace("CAPRICORNUS", "CAPRICORN")

            if (tropical == astronomical) same++
            total++
            date = date.plusDays(1)
        }

        assertTrue("expected disagreement on most days, matched $same of $total", same < total / 3)
    }

    @Test
    fun `today's two answers are both sensible signs`() {
        // A guard against an off-by-one that returns something absurd.
        val date = LocalDate.of(2026, 9, 2)
        assertEquals(ZodiacSign.VIRGO, signOn(2026, 9, 2))
        assertEquals(Constellation.LEO, Constellation.at(date))
        assertNotEquals(ZodiacSign.VIRGO.name, Constellation.at(date).name)
    }

    // --- helpers ---

    /** Days from [a] to [b], wrapping across the year end. */
    private fun daysBetween(a: MonthDay, b: MonthDay): Int {
        val year = 2023 // non-leap, so the tiling adds to 365
        val from = LocalDate.of(year, a.month, a.dayOfMonth)
        var to = LocalDate.of(year, b.month, b.dayOfMonth)
        if (to.isBefore(from)) to = to.plusYears(1)
        return (to.toEpochDay() - from.toEpochDay()).toInt()
    }

    private fun spanDays(constellation: Constellation): Int {
        val next = Constellation.entries[(constellation.ordinal + 1) % Constellation.entries.size]
        return daysBetween(constellation.start, next.start)
    }

    private fun withinRange(day: MonthDay, from: MonthDay, to: MonthDay): Boolean =
        if (from <= to) day in from..to else day >= from || day <= to
}
