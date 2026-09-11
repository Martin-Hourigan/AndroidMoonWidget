package dev.mahourigan.moonwidget.astronomy

import java.time.LocalDate
import java.time.MonthDay

/**
 * Where the Sun is in the *tropical* zodiac — the familiar "star sign".
 *
 * Computed from the Sun's real ecliptic longitude rather than looked up from a
 * table of dates, so it is exact for any moment and cannot drift. The tropical
 * zodiac is tied to the equinox, so the sign is a position in the seasons, not
 * a constellation; see [AstronomicalZodiac] for where the Sun actually is
 * against the stars.
 */
object SunZodiac {

    fun at(julianDay: Double): ZodiacSign =
        ZodiacSign.forLongitude(CelestialPositions.sunLongitude(julianDay))

    /**
     * The conventional dates quoted for a sign.
     *
     * Only ever used as a label beside a sign that was itself computed, so the
     * two cannot contradict each other. The real boundaries wander by about a
     * day either way with the leap-year cycle, which is why these are given as
     * the customary dates rather than presented as exact.
     */
    fun conventionalDates(sign: ZodiacSign): Pair<MonthDay, MonthDay> = when (sign) {
        ZodiacSign.ARIES -> MonthDay.of(3, 21) to MonthDay.of(4, 19)
        ZodiacSign.TAURUS -> MonthDay.of(4, 20) to MonthDay.of(5, 20)
        ZodiacSign.GEMINI -> MonthDay.of(5, 21) to MonthDay.of(6, 20)
        ZodiacSign.CANCER -> MonthDay.of(6, 21) to MonthDay.of(7, 22)
        ZodiacSign.LEO -> MonthDay.of(7, 23) to MonthDay.of(8, 22)
        ZodiacSign.VIRGO -> MonthDay.of(8, 23) to MonthDay.of(9, 22)
        ZodiacSign.LIBRA -> MonthDay.of(9, 23) to MonthDay.of(10, 22)
        ZodiacSign.SCORPIO -> MonthDay.of(10, 23) to MonthDay.of(11, 21)
        ZodiacSign.SAGITTARIUS -> MonthDay.of(11, 22) to MonthDay.of(12, 21)
        ZodiacSign.CAPRICORN -> MonthDay.of(12, 22) to MonthDay.of(1, 19)
        ZodiacSign.AQUARIUS -> MonthDay.of(1, 20) to MonthDay.of(2, 18)
        ZodiacSign.PISCES -> MonthDay.of(2, 19) to MonthDay.of(3, 20)
    }
}

/**
 * The thirteen constellations the Sun genuinely passes in front of.
 *
 * Not the same thing as the zodiac signs, and deliberately a separate type from
 * [ZodiacSign]: the signs are twelve equal thirty-degree slices, while these are
 * the real constellations, which are wildly uneven. The Sun spends forty-four
 * days in Virgo and seven in Scorpius.
 *
 * Ophiuchus is the one most people have heard of, usually via the recurring
 * "NASA changed the zodiac" story. NASA changed nothing: it published a piece
 * about the constellations in 2016, and the Babylonians had already left
 * Ophiuchus out some 2,500 years earlier to get a tidy twelve that matched
 * their calendar.
 *
 * @param start the day the Sun enters this constellation; it stays until the
 *   next one begins.
 */
enum class Constellation(private val glyph: String, val start: MonthDay) {
    CAPRICORNUS("♑", MonthDay.of(1, 18)),
    AQUARIUS("♒", MonthDay.of(2, 15)),
    PISCES("♓", MonthDay.of(3, 11)),
    ARIES("♈", MonthDay.of(4, 18)),
    TAURUS("♉", MonthDay.of(5, 13)),
    GEMINI("♊", MonthDay.of(6, 19)),
    CANCER("♋", MonthDay.of(7, 17)),
    LEO("♌", MonthDay.of(8, 7)),
    VIRGO("♍", MonthDay.of(9, 12)),
    LIBRA("♎", MonthDay.of(10, 26)),
    SCORPIUS("♏", MonthDay.of(11, 19)),
    OPHIUCHUS("⛎", MonthDay.of(11, 26)),
    SAGITTARIUS("♐", MonthDay.of(12, 15)),
    ;

    /** Monochrome glyph, matching [ZodiacSign.symbol]. */
    val symbol: String get() = glyph + ZodiacSign.TEXT_PRESENTATION

    /** The last day the Sun is here — the day before the next one starts. */
    val end: MonthDay
        get() {
            val next = entries[(ordinal + 1) % entries.size].start
            // MonthDay has no arithmetic, so step back through a leap year,
            // where 29 February exists and cannot be skipped over.
            val asDate = LocalDate.of(LEAP_REFERENCE_YEAR, next.month, next.dayOfMonth).minusDays(1)
            return MonthDay.of(asDate.month, asDate.dayOfMonth)
        }

    companion object {
        /** A leap year, so 29 February is a real date when stepping backwards. */
        private const val LEAP_REFERENCE_YEAR = 2024

        /**
         * The constellation the Sun is in on [date].
         *
         * Boundaries are given as dates rather than derived from the Sun's
         * position, because the constellation edges are the IAU's irregular
         * polygons rather than anything computable from a longitude alone.
         * Precession moves them by roughly one day every seventy years, which
         * is far below the couple of days that published tables already
         * disagree by.
         */
        fun at(date: LocalDate): Constellation = at(MonthDay.of(date.month, date.dayOfMonth))

        fun at(monthDay: MonthDay): Constellation =
            // Sorted by start date, so the answer is the last one already begun.
            entries.lastOrNull { it.start <= monthDay }
            // Before the first start of the year is still the sign that began
            // in December and runs over the year boundary.
                ?: entries.last()
    }
}

/** Kept as an object for symmetry with [SunZodiac] at call sites. */
object AstronomicalZodiac {
    fun at(date: LocalDate): Constellation = Constellation.at(date)
}
