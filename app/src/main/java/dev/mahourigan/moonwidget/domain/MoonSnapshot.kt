package dev.mahourigan.moonwidget.domain

import dev.mahourigan.moonwidget.astronomy.JulianDate
import dev.mahourigan.moonwidget.astronomy.MoonAppearance
import dev.mahourigan.moonwidget.astronomy.Horizontal
import dev.mahourigan.moonwidget.astronomy.MoonOrientation
import dev.mahourigan.moonwidget.astronomy.MoonDistance
import dev.mahourigan.moonwidget.astronomy.MoonDistanceInfo
import dev.mahourigan.moonwidget.astronomy.MoonNames
import dev.mahourigan.moonwidget.astronomy.MoonPhase
import dev.mahourigan.moonwidget.astronomy.MoonPhaseInfo
import dev.mahourigan.moonwidget.astronomy.MoonSign
import dev.mahourigan.moonwidget.astronomy.MoonSlot
import dev.mahourigan.moonwidget.astronomy.MoonZodiac
import dev.mahourigan.moonwidget.astronomy.AstronomicalZodiac
import dev.mahourigan.moonwidget.astronomy.Constellation
import dev.mahourigan.moonwidget.astronomy.SunZodiac
import dev.mahourigan.moonwidget.astronomy.ZodiacSign
import dev.mahourigan.moonwidget.astronomy.NextRiseSet
import dev.mahourigan.moonwidget.astronomy.RiseSet
import dev.mahourigan.moonwidget.astronomy.RiseSetTimes
import dev.mahourigan.moonwidget.astronomy.SkyPass
import dev.mahourigan.moonwidget.astronomy.SkyPath
import dev.mahourigan.moonwidget.astronomy.SkyPoint
import dev.mahourigan.moonwidget.data.ResolvedLocation
import dev.mahourigan.moonwidget.data.Settings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * One computed view of the Moon: everything the widget or the detail screen
 * needs, calculated in a single place so the two can never drift apart.
 */
data class MoonSnapshot(
    val calculatedAt: Instant,
    val location: ResolvedLocation,
    val phase: MoonPhaseInfo,
    val appearance: MoonAppearance,
    val sign: MoonSign,
    /**
     * Where the Sun is, in both zodiacs.
     *
     * Always computed rather than gated behind its settings, unlike the
     * genuinely expensive fields here: it is one solar longitude and a date
     * comparison. Leaving it unconditional keeps it out of
     * [dev.mahourigan.moonwidget.data.affectsSnapshot], so toggling its row cannot
     * leave a stale value on screen.
     */
    val sunSign: ZodiacSign,
    val sunConstellation: Constellation,
    val distance: MoonDistanceInfo,
    /** True when this is a full moon near perigee. */
    val isSupermoon: Boolean,
    /** True when this is a full moon near apogee. */
    val isMicromoon: Boolean,
    /** Rise and set within the current local calendar day. */
    val riseSet: RiseSetTimes,
    /** The next crossings from now — what the widget actually shows. */
    val next: NextRiseSet,
    val nextNewMoon: Instant,
    val nextFullMoon: Instant,
    /**
     * When to look on the night of the next full / new moon.
     *
     * Each is null when not requested in settings, or when there is genuinely
     * no such event nearby — which happens at high latitudes.
     */
    val nextFullMoonRise: Instant?,
    val nextFullMoonPeak: Instant?,
    val nextNewMoonRise: Instant?,
    val nextNewMoonPeak: Instant?,
    /**
     * Traditional name for the next full moon, or null when names are switched
     * off or that particular name is unticked.
     */
    val nextFullMoonSlot: MoonSlot?,
    /**
     * Where to face to see the Moon, and how high it is. Null when nothing on
     * screen asks for it.
     */
    val position: Horizontal?,
    /** Bearing of the next moonrise, for when it is not up yet. */
    val riseAzimuth: Double?,
    /**
     * The pass the Moon is on, or the next one if it is down. Null when not
     * asked for, or when there is no pass — at high latitudes it can stay up or
     * down for weeks.
     */
    val pass: SkyPass?,
    /**
     * Height above the horizon sampled across one lunar day centred on now.
     * Empty unless the chart is switched on.
     */
    val skyPath: List<SkyPoint>,
) {
    val zone: ZoneId get() = location.zone

    /** True when the Moon is above the horizon at [calculatedAt]. */
    val isUp: Boolean get() = next.currentlyUp

    fun riseTime(): LocalTime? = riseSet.rise?.atZone(zone)?.toLocalTime()

    fun setTime(): LocalTime? = riseSet.set?.atZone(zone)?.toLocalTime()

    /** Whole days until the next full moon, rounded to nearest. */
    val daysToFullMoon: Long
        get() = Duration.between(calculatedAt, nextFullMoon).toDaysRounded()

    val daysToNewMoon: Long
        get() = Duration.between(calculatedAt, nextNewMoon).toDaysRounded()

    private fun Duration.toDaysRounded(): Long = (toMinutes() / 1440.0).roundToLong()

    companion object {
        /**
         * How to draw the disc, under whichever orientation rule is switched on.
         *
         * Either way the hemisphere comes from the observer's latitude and is
         * never guessed at, so there is nothing here for the user to override.
         */
        private fun appearanceFor(
            phase: MoonPhaseInfo,
            julianDay: Double,
            location: ResolvedLocation,
            settings: Settings,
        ): MoonAppearance {
            if (settings.trueMoonOrientation) {
                return MoonAppearance.oriented(
                    phaseAngle = phase.phaseAngle,
                    brightLimbAngleFromZenith =
                        MoonOrientation.brightLimbAngleFromZenith(julianDay, location.geo),
                )
            }

            // Upright: the terminator is vertical and only the lit side varies,
            // which the latitude alone decides.
            return MoonAppearance.forPhase(
                phaseAngle = phase.phaseAngle,
                observerLatitude = location.geo.latitude,
            )
        }

        fun compute(
            location: ResolvedLocation,
            at: Instant = Instant.now(),
            settings: Settings = Settings(),
        ): MoonSnapshot {
            val julianDay = JulianDate.fromInstant(at)
            val phase = MoonPhase.at(julianDay)

            // Season names follow the hemisphere you are in; the setting
            // inverts whatever the latitude implies.
            val useSouthernSeasonNames =
                (location.geo.latitude < 0) != settings.swapSeasonNames

            val nextNew = JulianDate.toInstant(MoonPhase.nextNewMoon(julianDay))
            val nextFull = JulianDate.toInstant(MoonPhase.nextFullMoon(julianDay))

            // Only computed when it will actually be shown — each lookup scans a
            // 36-hour window, and the widget rebuilds this on every refresh.
            fun riseFor(target: Instant, wanted: Boolean): Instant? =
                if (wanted && settings.phaseTimeRise) {
                    RiseSet.nearestRise(target, location.geo)
                } else {
                    null
                }

            fun peakFor(target: Instant, wanted: Boolean): Instant? =
                if (wanted && settings.phaseTimePeak) {
                    RiseSet.nearestTransit(target, location.geo)
                } else {
                    null
                }
            val wantsDirection = settings.showCompassDirection || settings.widgetShowDirection
            val position = if (wantsDirection) {
                MoonOrientation.moonPosition(julianDay, location.geo)
            } else {
                null
            }

            // Both are real work, so neither is done unless something shows it.
            val pass = if (settings.showSkyPath || settings.widgetShowSkyPath) {
                SkyPath.currentOrNextPass(at, location.geo)
            } else {
                null
            }
            val skyPath = if (settings.showSkyPath) {
                val (from, to) = SkyPath.windowAround(at)
                SkyPath.samples(from, to, location.geo)
            } else {
                emptyList()
            }

            // Not LocalDate.ofInstant: that overload is API 34+, and minSdk is 26.
            val localDate = at.atZone(location.zone).toLocalDate()

            return MoonSnapshot(
                calculatedAt = at,
                location = location,
                phase = phase,
                appearance = appearanceFor(phase, julianDay, location, settings),
                sign = MoonZodiac.at(julianDay),
                sunSign = SunZodiac.at(julianDay),
                sunConstellation = AstronomicalZodiac.at(localDate),
                distance = MoonDistance.at(julianDay),
                isSupermoon = MoonDistance.isSupermoon(julianDay),
                isMicromoon = MoonDistance.isMicromoon(julianDay),
                riseSet = RiseSet.forDate(localDate, location.geo, location.zone),
                next = RiseSet.nextEvents(at, location.geo),
                nextNewMoon = nextNew,
                nextFullMoon = nextFull,
                nextFullMoonRise = riseFor(nextFull, settings.wantsFullMoonTime),
                nextFullMoonPeak = peakFor(nextFull, settings.wantsFullMoonTime),
                nextFullMoonSlot = if (settings.showNamedMoons) {
                    MoonNames.forFullMoon(
                        fullMoonJulianDay = MoonPhase.nextFullMoon(julianDay),
                        zone = location.zone,
                        southernSeasons = useSouthernSeasonNames,
                    )
                } else {
                    null
                },
                nextNewMoonRise = riseFor(nextNew, settings.wantsNewMoonTime),
                nextNewMoonPeak = peakFor(nextNew, settings.wantsNewMoonTime),
                position = position,
                // Only worth computing while the Moon is down, which is exactly
                // when the current bearing is no use for finding it.
                riseAzimuth = RiseSet.nextEvents(at, location.geo).rise
                    ?.takeIf { wantsDirection }
                    ?.let {
                        MoonOrientation.moonPosition(JulianDate.fromInstant(it), location.geo).azimuth
                    },
                pass = pass,
                skyPath = skyPath,
            )
        }
    }
}
