package dev.mahourigan.moonwidget.astronomy

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Which way up the Moon appears from a particular place at a particular moment.
 *
 * The Moon does not hang in the sky at a fixed angle. It rises, arcs over and
 * sets, and the whole disc appears to roll as it goes — a crescent that comes up
 * on its side can be sitting like a bowl by the time it sets. Nothing about the
 * Moon has turned; the observer has, along with the Earth.
 *
 * Two angles combine to give the answer:
 *
 *  - [brightLimbPositionAngle] (χ, Meeus ch. 48) — the direction of the lit
 *    side, measured against the celestial pole. This is what keeps the bright
 *    limb pointing at the Sun.
 *  - [parallacticAngle] (q, Meeus ch. 14) — how far the celestial frame is
 *    tilted relative to the observer's own horizon at the Moon's position.
 *
 * Their difference, [brightLimbAngleFromZenith], is the answer: the direction of
 * the lit side measured from straight up.
 *
 * The hemisphere flip the app used to special-case falls out of this for free.
 * At Newcastle's latitude a moon on the celestial equator transits with q = 180°
 * — that *is* the "upside-down southern moon", and no branch is needed.
 *
 * Coordinates here are geocentric. Topocentric parallax shifts the Moon by up to
 * about a degree, which would matter for an occultation prediction and does not
 * matter for choosing how to rotate a drawn disc.
 */
object MoonOrientation {

    /**
     * Angle between the celestial pole and the zenith, as seen from the Moon's
     * position on the sky. Meeus ch. 14.
     *
     * Zero when the Moon is on the meridian above the celestial pole, and ±180°
     * when it is on the meridian on the other side — which is the usual case for
     * a southern-hemisphere observer looking north at the Moon.
     *
     * @param hourAngle local hour angle of the Moon, degrees. Zero at culmination.
     * @param declination the Moon's declination, degrees.
     * @param latitude the observer's latitude, degrees. Negative south.
     */
    fun parallacticAngle(hourAngle: Double, declination: Double, latitude: Double): Double {
        val h = hourAngle.radians
        val dec = declination.radians
        val lat = latitude.radians

        return atan2(
            sin(h),
            tan(lat) * cos(dec) - sin(dec) * cos(h),
        ).degrees
    }

    /**
     * Position angle of the midpoint of the Moon's bright limb, measured from
     * the north celestial pole towards the east. Meeus ch. 48, formula 48.5.
     *
     * This points at the Sun, which is the whole reason the lit side is where it
     * is, and is the check the tests use.
     */
    fun brightLimbPositionAngle(
        sun: EquatorialPosition,
        moon: EquatorialPosition,
    ): Double {
        val sunDec = sun.declination.radians
        val moonDec = moon.declination.radians
        val deltaRa = (sun.rightAscension - moon.rightAscension).radians

        return atan2(
            cos(sunDec) * sin(deltaRa),
            sin(sunDec) * cos(moonDec) - cos(sunDec) * sin(moonDec) * cos(deltaRa),
        ).degrees
    }

    /**
     * Direction of the bright limb measured from straight up, clockwise as the
     * observer sees it, in `[0, 360)`.
     *
     * 0° means the lit side points at the zenith, 90° means it points to the
     * observer's right, 180° straight down at the horizon.
     */
    fun brightLimbAngleFromZenith(julianDay: Double, location: GeoLocation): Double {
        val moon = CelestialPositions.toEquatorial(
            CelestialPositions.moon(julianDay), julianDay,
        )
        val sun = sunEquatorial(julianDay)

        val localSiderealTime = normalizeDegrees(
            CelestialPositions.greenwichMeanSiderealTime(julianDay) + location.longitude
        )
        val hourAngle = normalizeSignedDegrees(localSiderealTime - moon.rightAscension)

        val chi = brightLimbPositionAngle(sun, moon)
        val q = parallacticAngle(hourAngle, moon.declination, location.latitude)

        // χ − q is the textbook result, but position angles are measured north
        // through east, and east runs anticlockwise for an observer facing the
        // sky. Screen rotation runs clockwise, so the sign is inverted here
        // rather than at every call site.
        return normalizeDegrees(q - chi)
    }

    /** Where to face to see the Moon, and how high it is, at [julianDay]. */
    fun moonPosition(julianDay: Double, location: GeoLocation): Horizontal =
        horizontal(
            CelestialPositions.toEquatorial(CelestialPositions.moon(julianDay), julianDay),
            julianDay,
            location,
        )

    /**
     * The Sun in equatorial coordinates.
     *
     * Its ecliptic latitude is zero by definition — the ecliptic is the Sun's
     * own path — so only the longitude has to be computed.
     */
    fun sunEquatorial(julianDay: Double): EquatorialPosition =
        CelestialPositions.toEquatorial(
            EclipticPosition(
                longitude = CelestialPositions.sunLongitude(julianDay),
                latitude = 0.0,
                distanceKm = 0.0,
            ),
            julianDay,
        )

    /**
     * Where a body sits in the observer's own sky: how high, and which way to
     * face.
     *
     * Also used to check [brightLimbAngleFromZenith] from a completely
     * different direction — see [bearingFromZenith].
     */
    fun horizontal(position: EquatorialPosition, julianDay: Double, location: GeoLocation): Horizontal {
        val localSiderealTime = normalizeDegrees(
            CelestialPositions.greenwichMeanSiderealTime(julianDay) + location.longitude
        )
        val h = normalizeSignedDegrees(localSiderealTime - position.rightAscension).radians
        val dec = position.declination.radians
        val lat = location.latitude.radians

        val altitude = kotlin.math.asin(
            (sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(h)).coerceIn(-1.0, 1.0)
        ).degrees

        // Measured from north through east, the way a compass bearing runs.
        val azimuth = normalizeDegrees(
            atan2(
                sin(h),
                cos(h) * sin(lat) - tan(dec) * cos(lat),
            ).degrees + 180.0
        )

        return Horizontal(altitude = altitude, azimuth = azimuth)
    }

    /**
     * Bearing from [from] to [to] across the sky, measured from straight up and
     * running clockwise as the observer sees it.
     *
     * This is the independent route to the same answer: the bright limb points
     * at the Sun, so the bearing from the Moon to the Sun must equal
     * [brightLimbAngleFromZenith]. Computed entirely in horizon coordinates, it
     * shares no algebra with the χ − q formulation, so agreement between the two
     * is real evidence rather than a restatement.
     */
    fun bearingFromZenith(from: Horizontal, to: Horizontal): Double {
        val a1 = from.altitude.radians
        val a2 = to.altitude.radians
        // Azimuth increases clockwise, which is also the direction screen angles
        // run, so no handedness correction is needed here.
        val deltaAz = (to.azimuth - from.azimuth).radians

        return normalizeDegrees(
            atan2(
                cos(a2) * sin(deltaAz),
                cos(a1) * sin(a2) - sin(a1) * cos(a2) * cos(deltaAz),
            ).degrees
        )
    }
}

/** A position in the observer's sky. */
data class Horizontal(
    /** Degrees above the horizon; negative below. */
    val altitude: Double,
    /** Degrees clockwise from north. */
    val azimuth: Double,
)

/**
 * The sixteen points of the compass.
 *
 * Sixteen rather than eight because the Moon's rising point wanders through
 * some fifty degrees over a month, and eight points would call most of that
 * "east". No display text here — see `ui/DisplayStrings.kt`.
 */
enum class CompassPoint {
    NORTH,
    NORTH_NORTH_EAST,
    NORTH_EAST,
    EAST_NORTH_EAST,
    EAST,
    EAST_SOUTH_EAST,
    SOUTH_EAST,
    SOUTH_SOUTH_EAST,
    SOUTH,
    SOUTH_SOUTH_WEST,
    SOUTH_WEST,
    WEST_SOUTH_WEST,
    WEST,
    WEST_NORTH_WEST,
    NORTH_WEST,
    NORTH_NORTH_WEST,
    ;

    companion object {
        /** Each point covers 22.5 degrees, centred on its own bearing. */
        private const val SECTOR = 360.0 / 16

        fun forAzimuth(azimuth: Double): CompassPoint {
            val normalized = normalizeDegrees(azimuth)
            // Offset by half a sector so a bearing rounds to the nearest point
            // rather than always down to the one before it.
            val index = ((normalized + SECTOR / 2) / SECTOR).toInt() % entries.size
            return entries[index]
        }
    }
}
