package dev.mahourigan.moonwidget.astronomy

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Geocentric ecliptic coordinates. */
data class EclipticPosition(
    /** Ecliptic longitude, degrees `[0, 360)`. */
    val longitude: Double,
    /** Ecliptic latitude, degrees. */
    val latitude: Double,
    /** Distance from Earth's centre, km. */
    val distanceKm: Double,
)

/** Geocentric equatorial coordinates. */
data class EquatorialPosition(
    /** Right ascension, degrees `[0, 360)`. */
    val rightAscension: Double,
    /** Declination, degrees. */
    val declination: Double,
)

/**
 * Positions of the Moon and Sun.
 *
 * Ported from a Java prototype validated against Meeus' worked examples:
 * Example 47.a (Moon, 1992-04-12 0h TD) reproduces longitude, latitude and
 * distance exactly to the printed precision, and Example 25.b (Sun) matches
 * to 1e-5 degrees. See MoonPositionTest.
 */
object CelestialPositions {

    // --- Table 47.A: multiples of D, M, M', F, then the longitude and radius coefficients ---
    private val ARG_LR = arrayOf(
        intArrayOf(0, 0, 1, 0), intArrayOf(2, 0, -1, 0), intArrayOf(2, 0, 0, 0), intArrayOf(0, 0, 2, 0),
        intArrayOf(0, 1, 0, 0), intArrayOf(0, 0, 0, 2), intArrayOf(2, 0, -2, 0), intArrayOf(2, -1, -1, 0),
        intArrayOf(2, 0, 1, 0), intArrayOf(2, -1, 0, 0), intArrayOf(0, 1, -1, 0), intArrayOf(1, 0, 0, 0),
        intArrayOf(0, 1, 1, 0), intArrayOf(2, 0, 0, -2), intArrayOf(0, 0, 1, 2), intArrayOf(0, 0, 1, -2),
        intArrayOf(4, 0, -1, 0), intArrayOf(0, 0, 3, 0), intArrayOf(4, 0, -2, 0), intArrayOf(2, 1, -1, 0),
        intArrayOf(2, 1, 0, 0), intArrayOf(1, 0, -1, 0), intArrayOf(1, 1, 0, 0), intArrayOf(2, -1, 1, 0),
        intArrayOf(2, 0, 2, 0), intArrayOf(4, 0, 0, 0), intArrayOf(2, 0, -3, 0), intArrayOf(0, 1, -2, 0),
        intArrayOf(2, 0, -1, 2), intArrayOf(2, -1, -2, 0), intArrayOf(1, 0, 1, 0), intArrayOf(2, -2, 0, 0),
        intArrayOf(0, 1, 2, 0), intArrayOf(0, 2, 0, 0), intArrayOf(2, -2, -1, 0), intArrayOf(2, 0, 1, -2),
        intArrayOf(2, 0, 0, 2), intArrayOf(4, -1, -1, 0), intArrayOf(0, 0, 2, 2), intArrayOf(3, 0, -1, 0),
        intArrayOf(2, 1, 1, 0), intArrayOf(4, -1, -2, 0), intArrayOf(0, 2, -1, 0), intArrayOf(2, 2, -1, 0),
        intArrayOf(2, 1, -2, 0), intArrayOf(2, -1, 0, -2), intArrayOf(4, 0, 1, 0), intArrayOf(0, 0, 4, 0),
        intArrayOf(4, -1, 0, 0), intArrayOf(1, 0, -2, 0), intArrayOf(2, 1, 0, -2), intArrayOf(0, 0, 2, -2),
        intArrayOf(1, 1, 1, 0), intArrayOf(3, 0, -2, 0), intArrayOf(4, 0, -3, 0), intArrayOf(2, -1, 2, 0),
        intArrayOf(0, 2, 1, 0), intArrayOf(1, 1, -1, 0), intArrayOf(2, 0, 3, 0), intArrayOf(2, 0, -1, -2),
    )

    /** Longitude coefficients, units of 1e-6 degrees. */
    private val COEFF_L = longArrayOf(
        6288774, 1274027, 658314, 213618, -185116, -114332, 58793, 57066, 53322, 45758,
        -40923, -34720, -30383, 15327, -12528, 10980, 10675, 10034, 8548, -7888,
        -6766, -5163, 4987, 4036, 3994, 3861, 3665, -2689, -2602, 2390,
        -2348, 2236, -2120, -2069, 2048, -1773, -1595, 1215, -1110, -892,
        -810, 759, -713, -700, 691, 596, 549, 537, 520, -487,
        -399, -381, 351, -340, 330, 327, -323, 299, 294, 0,
    )

    /** Radius-vector coefficients, units of 1e-3 km. */
    private val COEFF_R = longArrayOf(
        -20905355, -3699111, -2955968, -569925, 48888, -3149, 246158, -152138, -170733, -204586,
        -129620, 108743, 104755, 10321, 0, 79661, -34782, -23210, -21636, 24208,
        30824, -8379, -16675, -12831, -10445, -11650, 14403, -7003, 0, 10056,
        6322, -9884, 5751, 0, -4950, 4130, 0, -3958, 0, 3258,
        2616, -1897, -2117, 2354, 0, 0, -1423, -1117, -1571, -1739,
        0, -4421, 0, 0, 0, 0, 1165, 0, 0, 8752,
    )

    // --- Table 47.B: latitude terms ---
    private val ARG_B = arrayOf(
        intArrayOf(0, 0, 0, 1), intArrayOf(0, 0, 1, 1), intArrayOf(0, 0, 1, -1), intArrayOf(2, 0, 0, -1),
        intArrayOf(2, 0, -1, 1), intArrayOf(2, 0, -1, -1), intArrayOf(2, 0, 0, 1), intArrayOf(0, 0, 2, 1),
        intArrayOf(2, 0, 1, -1), intArrayOf(0, 0, 2, -1), intArrayOf(2, -1, 0, -1), intArrayOf(2, 0, -2, -1),
        intArrayOf(2, 0, 1, 1), intArrayOf(2, 1, 0, -1), intArrayOf(2, -1, -1, 1), intArrayOf(2, -1, 0, 1),
        intArrayOf(2, -1, -1, -1), intArrayOf(0, 1, -1, -1), intArrayOf(4, 0, -1, -1), intArrayOf(0, 1, 0, 1),
        intArrayOf(0, 0, 0, 3), intArrayOf(0, 1, -1, 1), intArrayOf(1, 0, 0, 1), intArrayOf(0, 1, 1, 1),
        intArrayOf(0, 1, 1, -1), intArrayOf(0, 1, 0, -1), intArrayOf(1, 0, 0, -1), intArrayOf(0, 0, 3, 1),
        intArrayOf(4, 0, 0, -1), intArrayOf(4, 0, -1, 1), intArrayOf(0, 0, 1, -3), intArrayOf(4, 0, -2, 1),
        intArrayOf(2, 0, 0, -3), intArrayOf(2, 0, 2, -1), intArrayOf(2, -1, 1, -1), intArrayOf(2, 0, -2, 1),
        intArrayOf(0, 0, 3, -1), intArrayOf(2, 0, 2, 1), intArrayOf(2, 0, -3, -1), intArrayOf(2, 1, -1, 1),
        intArrayOf(2, 1, 0, 1), intArrayOf(4, 0, 0, 1), intArrayOf(2, -1, 1, 1), intArrayOf(2, -2, 0, -1),
        intArrayOf(0, 0, 1, 3), intArrayOf(2, 1, 1, -1), intArrayOf(1, 1, 0, -1), intArrayOf(1, 1, 0, 1),
        intArrayOf(0, 1, -2, -1), intArrayOf(2, 1, -1, -1), intArrayOf(1, 0, 1, 1), intArrayOf(2, -1, -2, -1),
        intArrayOf(0, 1, 2, 1), intArrayOf(4, 0, -2, -1), intArrayOf(4, -1, -1, -1), intArrayOf(1, 0, 1, -1),
        intArrayOf(4, 0, 1, -1), intArrayOf(1, 0, -1, -1), intArrayOf(4, -1, 0, -1), intArrayOf(2, -2, 0, 1),
    )

    /** Latitude coefficients, units of 1e-6 degrees. */
    private val COEFF_B = longArrayOf(
        5128122, 280602, 277693, 173237, 55413, 46271, 32573, 17198, 9266, 8822,
        8216, 4324, 4200, -3359, 2463, 2211, 2065, -1870, 1828, -1794,
        -1749, -1565, -1491, -1475, -1410, -1344, -1335, 1107, 1021, 833,
        777, 671, 607, 596, 491, -451, 439, 422, 421, -366,
        -351, 331, 315, 302, -283, -229, 223, 223, -220, -220,
        -185, 181, -177, 176, 166, -164, 132, -119, 115, 107,
    )

    /**
     * Geocentric ecliptic position of the Moon.
     *
     * Accuracy is roughly 10" in longitude and 4" in latitude — far finer than
     * anything a phase display or rise/set time needs.
     */
    fun moon(julianDay: Double): EclipticPosition {
        val t = JulianDate.centuriesSinceJ2000(julianDay)

        // Mean longitude, elongation, anomalies and argument of latitude.
        val meanLongitude = normalizeDegrees(
            218.3164477 + 481267.88123421 * t - 0.0015786 * t.pow(2) +
                t.pow(3) / 538841.0 - t.pow(4) / 65194000.0
        )
        val elongation = normalizeDegrees(
            297.8501921 + 445267.1114034 * t - 0.0018819 * t.pow(2) +
                t.pow(3) / 545868.0 - t.pow(4) / 113065000.0
        )
        val sunAnomaly = normalizeDegrees(
            357.5291092 + 35999.0502909 * t - 0.0001536 * t.pow(2) + t.pow(3) / 24490000.0
        )
        val moonAnomaly = normalizeDegrees(
            134.9633964 + 477198.8675055 * t + 0.0087414 * t.pow(2) +
                t.pow(3) / 69699.0 - t.pow(4) / 14712000.0
        )
        val argOfLatitude = normalizeDegrees(
            93.2720950 + 483202.0175233 * t - 0.0036539 * t.pow(2) -
                t.pow(3) / 3526000.0 + t.pow(4) / 863310000.0
        )

        // Venus / Jupiter / flattening corrections.
        val a1 = normalizeDegrees(119.75 + 131.849 * t)
        val a2 = normalizeDegrees(53.09 + 479264.290 * t)
        val a3 = normalizeDegrees(313.45 + 481266.484 * t)

        // Eccentricity correction to terms involving the Sun's anomaly.
        val eccentricity = 1 - 0.002516 * t - 0.0000074 * t.pow(2)

        var sumL = 0.0
        var sumR = 0.0
        for (i in ARG_LR.indices) {
            val terms = ARG_LR[i]
            val argument = (
                terms[0] * elongation + terms[1] * sunAnomaly +
                    terms[2] * moonAnomaly + terms[3] * argOfLatitude
                ).radians
            val eccFactor = eccentricity.pow(abs(terms[1]))
            sumL += COEFF_L[i] * eccFactor * sin(argument)
            sumR += COEFF_R[i] * eccFactor * cos(argument)
        }

        var sumB = 0.0
        for (i in ARG_B.indices) {
            val terms = ARG_B[i]
            val argument = (
                terms[0] * elongation + terms[1] * sunAnomaly +
                    terms[2] * moonAnomaly + terms[3] * argOfLatitude
                ).radians
            sumB += COEFF_B[i] * eccentricity.pow(abs(terms[1])) * sin(argument)
        }

        sumL += 3958 * sin(a1.radians) +
            1962 * sin((meanLongitude - argOfLatitude).radians) +
            318 * sin(a2.radians)

        sumB += -2235 * sin(meanLongitude.radians) +
            382 * sin(a3.radians) +
            175 * sin((a1 - argOfLatitude).radians) +
            175 * sin((a1 + argOfLatitude).radians) +
            127 * sin((meanLongitude - moonAnomaly).radians) -
            115 * sin((meanLongitude + moonAnomaly).radians)

        return EclipticPosition(
            longitude = normalizeDegrees(meanLongitude + sumL / 1_000_000.0),
            latitude = sumB / 1_000_000.0,
            distanceKm = 385_000.56 + sumR / 1_000.0,
        )
    }

    /** Apparent ecliptic longitude of the Sun, degrees (Meeus ch. 25, low precision). */
    fun sunLongitude(julianDay: Double): Double {
        val t = JulianDate.centuriesSinceJ2000(julianDay)
        val meanLongitude = normalizeDegrees(280.46646 + 36000.76983 * t + 0.0003032 * t.pow(2))
        val meanAnomaly = normalizeDegrees(357.52911 + 35999.05029 * t - 0.0001537 * t.pow(2))

        // Equation of the centre.
        val centre = (1.914602 - 0.004817 * t - 0.000014 * t.pow(2)) * sin(meanAnomaly.radians) +
            (0.019993 - 0.000101 * t) * sin((2 * meanAnomaly).radians) +
            0.000289 * sin((3 * meanAnomaly).radians)

        // Correct for nutation and aberration to get the *apparent* longitude.
        val omega = 125.04 - 1934.136 * t
        return normalizeDegrees(meanLongitude + centre - 0.00569 - 0.00478 * sin(omega.radians))
    }

    /** Mean obliquity of the ecliptic, degrees (Meeus eq. 22.2). */
    fun obliquity(julianDay: Double): Double {
        val t = JulianDate.centuriesSinceJ2000(julianDay)
        return 23.439291 - 0.0130042 * t - 1.64e-7 * t.pow(2) + 5.04e-7 * t.pow(3)
    }

    /** Convert ecliptic coordinates to equatorial ones (Meeus eq. 13.3, 13.4). */
    fun toEquatorial(position: EclipticPosition, julianDay: Double): EquatorialPosition {
        val eps = obliquity(julianDay).radians
        val lon = position.longitude.radians
        val lat = position.latitude.radians

        val rightAscension = atan2(
            sin(lon) * cos(eps) - kotlin.math.tan(lat) * sin(eps),
            cos(lon),
        )
        val declination = asin(sin(lat) * cos(eps) + cos(lat) * sin(eps) * sin(lon))

        return EquatorialPosition(
            rightAscension = normalizeDegrees(rightAscension.degrees),
            declination = declination.degrees,
        )
    }

    /** Greenwich mean sidereal time, degrees (Meeus eq. 12.4). */
    fun greenwichMeanSiderealTime(julianDay: Double): Double {
        val t = JulianDate.centuriesSinceJ2000(julianDay)
        return normalizeDegrees(
            280.46061837 + 360.98564736629 * (julianDay - JulianDate.J2000) +
                0.000387933 * t.pow(2) - t.pow(3) / 38_710_000.0
        )
    }
}
