package dev.mahourigan.moonwidget.astronomy

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.floor

/**
 * Julian Day conversions (Meeus, *Astronomical Algorithms*, ch. 7).
 *
 * Julian Day is the continuous day count astronomers use so that date arithmetic
 * doesn't have to care about months, leap years, or calendar reform.
 */
object JulianDate {

    /** JD of the J2000.0 epoch: 2000-01-01 12:00 TT. */
    const val J2000 = 2451545.0

    /** Days in a Julian century, used to scale the polynomial terms. */
    const val DAYS_PER_CENTURY = 36525.0

    /**
     * Julian Day for a Gregorian calendar date.
     *
     * @param day may carry a fractional part, e.g. 12.5 for noon on the 12th.
     */
    fun fromCalendar(year: Int, month: Int, day: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        return floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            day + b - 1524.5
    }

    fun fromInstant(instant: Instant): Double =
        instant.epochSecond / 86400.0 + 2440587.5

    fun fromUtc(dateTime: LocalDateTime): Double =
        fromInstant(dateTime.toInstant(ZoneOffset.UTC))

    /** Midnight UTC at the start of [date]. */
    fun startOfDayUtc(date: LocalDate): Double =
        fromCalendar(date.year, date.monthValue, date.dayOfMonth.toDouble())

    fun toInstant(jd: Double): Instant =
        Instant.ofEpochSecond(((jd - 2440587.5) * 86400.0).toLong())

    /** Julian centuries since J2000.0 — the time argument for the position series. */
    fun centuriesSinceJ2000(jd: Double): Double = (jd - J2000) / DAYS_PER_CENTURY
}
