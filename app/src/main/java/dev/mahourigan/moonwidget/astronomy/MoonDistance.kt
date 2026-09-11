package dev.mahourigan.moonwidget.astronomy

/**
 * How far away the Moon is, and what that means.
 *
 * The distance already falls out of the position series in [CelestialPositions]
 * — this just gives it a name and a scale.
 */
data class MoonDistanceInfo(
    val distanceKm: Double,
    /**
     * Where this sits between perigee and apogee: `0.0` at the closest the Moon
     * gets, `1.0` at the furthest.
     */
    val perigeeApogeeFraction: Double,
    val isNearPerigee: Boolean,
    val isNearApogee: Boolean,
) {
    /** Apparent diameter in arcminutes — about 29.4' at apogee, 33.5' at perigee. */
    val apparentDiameterArcmin: Double
        get() = 2 * Math.toDegrees(Math.asin(MoonDistance.MEAN_RADIUS_KM / distanceKm)) * 60

    val distanceKmRounded: Int get() = Math.round(distanceKm).toInt()
}

object MoonDistance {

    /** Mean lunar radius, km. */
    const val MEAN_RADIUS_KM = 1737.4

    /** Typical extremes of the lunar orbit, km. */
    const val TYPICAL_PERIGEE_KM = 356_500.0
    const val TYPICAL_APOGEE_KM = 406_700.0

    /**
     * A full moon is popularly called a "supermoon" when it falls within 90% of
     * the perigee distance — the widely used Nolle criterion. That works out at
     * roughly 361,900 km.
     */
    private const val SUPERMOON_LIMIT_KM =
        TYPICAL_PERIGEE_KM + 0.1 * (TYPICAL_APOGEE_KM - TYPICAL_PERIGEE_KM)

    /** Mirror of the supermoon rule at the far end, for a "micromoon". */
    private const val MICROMOON_LIMIT_KM =
        TYPICAL_APOGEE_KM - 0.1 * (TYPICAL_APOGEE_KM - TYPICAL_PERIGEE_KM)

    fun at(julianDay: Double): MoonDistanceInfo {
        val distance = CelestialPositions.moon(julianDay).distanceKm
        val span = TYPICAL_APOGEE_KM - TYPICAL_PERIGEE_KM

        return MoonDistanceInfo(
            distanceKm = distance,
            perigeeApogeeFraction = ((distance - TYPICAL_PERIGEE_KM) / span).coerceIn(0.0, 1.0),
            isNearPerigee = distance <= SUPERMOON_LIMIT_KM,
            isNearApogee = distance >= MICROMOON_LIMIT_KM,
        )
    }

    /**
     * Whether a full moon at this moment counts as a supermoon.
     *
     * Both conditions matter: the Moon has to be close *and* full. Being near
     * perigee at first quarter is not a supermoon, it is just Tuesday.
     */
    fun isSupermoon(julianDay: Double): Boolean {
        val phase = MoonPhase.at(julianDay)
        return phase.phaseName == PhaseName.FULL_MOON && at(julianDay).isNearPerigee
    }

    fun isMicromoon(julianDay: Double): Boolean {
        val phase = MoonPhase.at(julianDay)
        return phase.phaseName == PhaseName.FULL_MOON && at(julianDay).isNearApogee
    }
}
