package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.data.AppFont
import dev.mahourigan.moonwidget.data.AppSection
import dev.mahourigan.moonwidget.data.MoonTheme
import dev.mahourigan.moonwidget.data.TextSize
import dev.mahourigan.moonwidget.data.ThemeRole
import dev.mahourigan.moonwidget.astronomy.MoonSign
import dev.mahourigan.moonwidget.astronomy.CompassPoint
import dev.mahourigan.moonwidget.astronomy.Constellation
import dev.mahourigan.moonwidget.astronomy.MoonNameSet
import dev.mahourigan.moonwidget.astronomy.MoonSlot
import dev.mahourigan.moonwidget.astronomy.PhaseName
import dev.mahourigan.moonwidget.astronomy.ZodiacSign
import dev.mahourigan.moonwidget.render.TimeFormatting
import android.content.Context
import androidx.annotation.StringRes
import java.time.Instant
import java.time.ZoneId

/**
 * Maps the astronomy layer's enums onto string resources.
 *
 * The astronomy package has no Android dependencies — that is what lets it run
 * in plain JVM unit tests — so the translation to user-facing text happens
 * here, at the boundary.
 */

@get:StringRes
val PhaseName.displayNameRes: Int
    get() = when (this) {
        PhaseName.NEW_MOON -> R.string.phase_new_moon
        PhaseName.WAXING_CRESCENT -> R.string.phase_waxing_crescent
        PhaseName.FIRST_QUARTER -> R.string.phase_first_quarter
        PhaseName.WAXING_GIBBOUS -> R.string.phase_waxing_gibbous
        PhaseName.FULL_MOON -> R.string.phase_full_moon
        PhaseName.WANING_GIBBOUS -> R.string.phase_waning_gibbous
        PhaseName.LAST_QUARTER -> R.string.phase_last_quarter
        PhaseName.WANING_CRESCENT -> R.string.phase_waning_crescent
    }

fun PhaseName.displayName(context: Context): String = context.getString(displayNameRes)

@get:StringRes
val ZodiacSign.displayNameRes: Int
    get() = when (this) {
        ZodiacSign.ARIES -> R.string.zodiac_aries
        ZodiacSign.TAURUS -> R.string.zodiac_taurus
        ZodiacSign.GEMINI -> R.string.zodiac_gemini
        ZodiacSign.CANCER -> R.string.zodiac_cancer
        ZodiacSign.LEO -> R.string.zodiac_leo
        ZodiacSign.VIRGO -> R.string.zodiac_virgo
        ZodiacSign.LIBRA -> R.string.zodiac_libra
        ZodiacSign.SCORPIO -> R.string.zodiac_scorpio
        ZodiacSign.SAGITTARIUS -> R.string.zodiac_sagittarius
        ZodiacSign.CAPRICORN -> R.string.zodiac_capricorn
        ZodiacSign.AQUARIUS -> R.string.zodiac_aquarius
        ZodiacSign.PISCES -> R.string.zodiac_pisces
    }

@get:StringRes
val ZodiacSign.descriptionRes: Int
    get() = when (this) {
        ZodiacSign.ARIES -> R.string.zodiac_aries_description
        ZodiacSign.TAURUS -> R.string.zodiac_taurus_description
        ZodiacSign.GEMINI -> R.string.zodiac_gemini_description
        ZodiacSign.CANCER -> R.string.zodiac_cancer_description
        ZodiacSign.LEO -> R.string.zodiac_leo_description
        ZodiacSign.VIRGO -> R.string.zodiac_virgo_description
        ZodiacSign.LIBRA -> R.string.zodiac_libra_description
        ZodiacSign.SCORPIO -> R.string.zodiac_scorpio_description
        ZodiacSign.SAGITTARIUS -> R.string.zodiac_sagittarius_description
        ZodiacSign.CAPRICORN -> R.string.zodiac_capricorn_description
        ZodiacSign.AQUARIUS -> R.string.zodiac_aquarius_description
        ZodiacSign.PISCES -> R.string.zodiac_pisces_description
    }

/**
 * The Sun's own take on each sign.
 *
 * Separate from [descriptionRes], which is written for the Moon — "a moon for
 * starting things", "the Moon's home sign" — and would read as nonsense
 * attached to the Sun.
 */
@get:StringRes
val ZodiacSign.sunDescriptionRes: Int
    get() = when (this) {
        ZodiacSign.ARIES -> R.string.sun_aries_description
        ZodiacSign.TAURUS -> R.string.sun_taurus_description
        ZodiacSign.GEMINI -> R.string.sun_gemini_description
        ZodiacSign.CANCER -> R.string.sun_cancer_description
        ZodiacSign.LEO -> R.string.sun_leo_description
        ZodiacSign.VIRGO -> R.string.sun_virgo_description
        ZodiacSign.LIBRA -> R.string.sun_libra_description
        ZodiacSign.SCORPIO -> R.string.sun_scorpio_description
        ZodiacSign.SAGITTARIUS -> R.string.sun_sagittarius_description
        ZodiacSign.CAPRICORN -> R.string.sun_capricorn_description
        ZodiacSign.AQUARIUS -> R.string.sun_aquarius_description
        ZodiacSign.PISCES -> R.string.sun_pisces_description
    }

/**
 * Constellation names.
 *
 * Mostly the same words as the signs, and deliberately reusing those strings so
 * there is one translation to keep right. The three that differ have their own:
 * the constellations are Scorpius and Capricornus, not Scorpio and Capricorn,
 * and Ophiuchus has no sign to borrow from.
 */
@get:StringRes
val Constellation.displayNameRes: Int
    get() = when (this) {
        Constellation.ARIES -> R.string.zodiac_aries
        Constellation.TAURUS -> R.string.zodiac_taurus
        Constellation.GEMINI -> R.string.zodiac_gemini
        Constellation.CANCER -> R.string.zodiac_cancer
        Constellation.LEO -> R.string.zodiac_leo
        Constellation.VIRGO -> R.string.zodiac_virgo
        Constellation.LIBRA -> R.string.zodiac_libra
        Constellation.SCORPIUS -> R.string.constellation_scorpius
        Constellation.OPHIUCHUS -> R.string.constellation_ophiuchus
        Constellation.SAGITTARIUS -> R.string.zodiac_sagittarius
        Constellation.CAPRICORNUS -> R.string.constellation_capricornus
        Constellation.AQUARIUS -> R.string.zodiac_aquarius
        Constellation.PISCES -> R.string.zodiac_pisces
    }

@get:StringRes
val Constellation.descriptionRes: Int
    get() = when (this) {
        Constellation.ARIES -> R.string.sun_aries_description
        Constellation.TAURUS -> R.string.sun_taurus_description
        Constellation.GEMINI -> R.string.sun_gemini_description
        Constellation.CANCER -> R.string.sun_cancer_description
        Constellation.LEO -> R.string.sun_leo_description
        Constellation.VIRGO -> R.string.sun_virgo_description
        Constellation.LIBRA -> R.string.sun_libra_description
        Constellation.SCORPIUS -> R.string.sun_scorpio_description
        Constellation.OPHIUCHUS -> R.string.sun_ophiuchus_description
        Constellation.SAGITTARIUS -> R.string.sun_sagittarius_description
        Constellation.CAPRICORNUS -> R.string.sun_capricorn_description
        Constellation.AQUARIUS -> R.string.sun_aquarius_description
        Constellation.PISCES -> R.string.sun_pisces_description
    }

fun Constellation.displayName(context: Context): String = context.getString(displayNameRes)

fun Constellation.description(context: Context): String = context.getString(descriptionRes)

/** "23 Aug – 22 Sep", in the device's own date order. */
fun monthDayRange(context: Context, from: java.time.MonthDay, to: java.time.MonthDay): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM")
    return context.getString(
        R.string.detail_sun_sign_range,
        formatter.format(from),
        formatter.format(to),
    )
}

fun ZodiacSign.displayName(context: Context): String = context.getString(displayNameRes)

fun ZodiacSign.description(context: Context): String = context.getString(descriptionRes)

/** e.g. "17° Sagittarius" */
fun MoonSign.formatted(context: Context): String = context.getString(
    R.string.moon_sign_position,
    wholeDegrees,
    sign.displayName(context),
)

/**
 * "Today" / "Tomorrow" / "Sat 29 Aug".
 *
 * Naming the day is more useful than counting to it — "Saturday" is something
 * you can plan around, "6 days" needs working out. The date is included so it
 * stays unambiguous beyond a week, where a bare weekday would repeat.
 */
fun countdownText(
    context: Context,
    target: Instant,
    zone: ZoneId,
    now: Instant = Instant.now(),
): String {
    val today = now.atZone(zone).toLocalDate()
    return when (target.atZone(zone).toLocalDate()) {
        today -> context.getString(R.string.countdown_today)
        today.plusDays(1) -> context.getString(R.string.countdown_tomorrow)
        else -> TimeFormatting.dayAndDate(target, zone, context)
    }
}

/**
 * The time(s) for a phase event, or null when none are requested.
 *
 * A single time is left unlabelled — which kind it is, is a setting, and
 * repeating it on every row would be noise. With both shown they have to be
 * told apart, so each gets a short label.
 *
 * Returned separately from the date so the UI can put it on its own line;
 * "Fri, Aug 28 · rise 5:37 PM · peak 12:10 AM" is too long for one row.
 */
fun phaseTimesText(
    context: Context,
    zone: ZoneId,
    riseTime: Instant? = null,
    peakTime: Instant? = null,
    force24Hour: Boolean = false,
): String? {
    fun formatted(instant: Instant) = TimeFormatting.format(instant, zone, context, force24Hour)

    return when {
        riseTime != null && peakTime != null -> listOf(
            context.getString(R.string.time_labelled_rise, formatted(riseTime)),
            context.getString(R.string.time_labelled_peak, formatted(peakTime)),
        ).joinToString(" · ")

        riseTime != null -> formatted(riseTime)
        peakTime != null -> formatted(peakTime)
        else -> null
    }
}

/** "Full moon today" / "Full moon tomorrow" / "Full moon Sat 29 Aug" */
fun fullMoonCountdown(
    context: Context,
    target: Instant,
    zone: ZoneId,
    now: Instant = Instant.now(),
): String = when (target.atZone(zone).toLocalDate()) {
    now.atZone(zone).toLocalDate() -> context.getString(R.string.full_moon_today)
    now.atZone(zone).toLocalDate().plusDays(1) -> context.getString(R.string.full_moon_tomorrow)
    else -> context.getString(
        R.string.full_moon_on,
        TimeFormatting.dayAndDate(target, zone, context),
    )
}


/** The name a given [MoonNameSet] uses for a given [MoonSlot]. */
@StringRes
fun moonNameRes(set: MoonNameSet, slot: MoonSlot): Int = when (set) {
    MoonNameSet.ALMANAC -> when (slot) {
        MoonSlot.JANUARY -> R.string.moon_almanac_january
        MoonSlot.FEBRUARY -> R.string.moon_almanac_february
        MoonSlot.MARCH -> R.string.moon_almanac_march
        MoonSlot.APRIL -> R.string.moon_almanac_april
        MoonSlot.MAY -> R.string.moon_almanac_may
        MoonSlot.JUNE -> R.string.moon_almanac_june
        MoonSlot.JULY -> R.string.moon_almanac_july
        MoonSlot.AUGUST -> R.string.moon_almanac_august
        MoonSlot.SEPTEMBER -> R.string.moon_almanac_september
        MoonSlot.OCTOBER -> R.string.moon_almanac_october
        MoonSlot.NOVEMBER -> R.string.moon_almanac_november
        MoonSlot.DECEMBER -> R.string.moon_almanac_december
        MoonSlot.HARVEST -> R.string.moon_almanac_harvest
    }
    MoonNameSet.OLD_ENGLISH -> when (slot) {
        MoonSlot.JANUARY -> R.string.moon_old_english_january
        MoonSlot.FEBRUARY -> R.string.moon_old_english_february
        MoonSlot.MARCH -> R.string.moon_old_english_march
        MoonSlot.APRIL -> R.string.moon_old_english_april
        MoonSlot.MAY -> R.string.moon_old_english_may
        MoonSlot.JUNE -> R.string.moon_old_english_june
        MoonSlot.JULY -> R.string.moon_old_english_july
        MoonSlot.AUGUST -> R.string.moon_old_english_august
        MoonSlot.SEPTEMBER -> R.string.moon_old_english_september
        MoonSlot.OCTOBER -> R.string.moon_old_english_october
        MoonSlot.NOVEMBER -> R.string.moon_old_english_november
        MoonSlot.DECEMBER -> R.string.moon_old_english_december
        MoonSlot.HARVEST -> R.string.moon_old_english_harvest
    }
    MoonNameSet.CELTIC -> when (slot) {
        MoonSlot.JANUARY -> R.string.moon_celtic_january
        MoonSlot.FEBRUARY -> R.string.moon_celtic_february
        MoonSlot.MARCH -> R.string.moon_celtic_march
        MoonSlot.APRIL -> R.string.moon_celtic_april
        MoonSlot.MAY -> R.string.moon_celtic_may
        MoonSlot.JUNE -> R.string.moon_celtic_june
        MoonSlot.JULY -> R.string.moon_celtic_july
        MoonSlot.AUGUST -> R.string.moon_celtic_august
        MoonSlot.SEPTEMBER -> R.string.moon_celtic_september
        MoonSlot.OCTOBER -> R.string.moon_celtic_october
        MoonSlot.NOVEMBER -> R.string.moon_celtic_november
        MoonSlot.DECEMBER -> R.string.moon_celtic_december
        MoonSlot.HARVEST -> R.string.moon_celtic_harvest
    }
    MoonNameSet.ALTERNATIVE -> when (slot) {
        MoonSlot.JANUARY -> R.string.moon_alternative_january
        MoonSlot.FEBRUARY -> R.string.moon_alternative_february
        MoonSlot.MARCH -> R.string.moon_alternative_march
        MoonSlot.APRIL -> R.string.moon_alternative_april
        MoonSlot.MAY -> R.string.moon_alternative_may
        MoonSlot.JUNE -> R.string.moon_alternative_june
        MoonSlot.JULY -> R.string.moon_alternative_july
        MoonSlot.AUGUST -> R.string.moon_alternative_august
        MoonSlot.SEPTEMBER -> R.string.moon_alternative_september
        MoonSlot.OCTOBER -> R.string.moon_alternative_october
        MoonSlot.NOVEMBER -> R.string.moon_alternative_november
        MoonSlot.DECEMBER -> R.string.moon_alternative_december
        MoonSlot.HARVEST -> R.string.moon_alternative_harvest
    }
}

fun moonName(context: Context, set: MoonNameSet, slot: MoonSlot): String =
    context.getString(moonNameRes(set, slot))

/** "January", "Nearest the September equinox" — the slot itself, not its name. */
@get:StringRes
val MoonSlot.slotLabelRes: Int
    get() = when (this) {
        MoonSlot.JANUARY -> R.string.moon_slot_january
        MoonSlot.FEBRUARY -> R.string.moon_slot_february
        MoonSlot.MARCH -> R.string.moon_slot_march
        MoonSlot.APRIL -> R.string.moon_slot_april
        MoonSlot.MAY -> R.string.moon_slot_may
        MoonSlot.JUNE -> R.string.moon_slot_june
        MoonSlot.JULY -> R.string.moon_slot_july
        MoonSlot.AUGUST -> R.string.moon_slot_august
        MoonSlot.SEPTEMBER -> R.string.moon_slot_september
        MoonSlot.OCTOBER -> R.string.moon_slot_october
        MoonSlot.NOVEMBER -> R.string.moon_slot_november
        MoonSlot.DECEMBER -> R.string.moon_slot_december
        MoonSlot.HARVEST -> R.string.moon_slot_harvest
    }

@get:StringRes
val MoonNameSet.displayNameRes: Int
    get() = when (this) {
        MoonNameSet.ALMANAC -> R.string.name_set_almanac
        MoonNameSet.OLD_ENGLISH -> R.string.name_set_old_english
        MoonNameSet.CELTIC -> R.string.name_set_celtic
        MoonNameSet.ALTERNATIVE -> R.string.name_set_alternative
    }

@get:StringRes
val MoonNameSet.noteRes: Int
    get() = when (this) {
        MoonNameSet.ALMANAC -> R.string.name_set_almanac_note
        MoonNameSet.OLD_ENGLISH -> R.string.name_set_old_english_note
        MoonNameSet.CELTIC -> R.string.name_set_celtic_note
        MoonNameSet.ALTERNATIVE -> R.string.name_set_alternative_note
    }

@get:StringRes
val MoonTheme.displayNameRes: Int
    get() = when (this) {
        MoonTheme.MIDNIGHT -> R.string.theme_midnight
        MoonTheme.ECLIPSE -> R.string.theme_eclipse
        MoonTheme.HARVEST -> R.string.theme_harvest
        MoonTheme.SEA_GLASS -> R.string.theme_sea_glass
        MoonTheme.BLOOD_MOON -> R.string.theme_blood_moon
        MoonTheme.PARCHMENT -> R.string.theme_parchment
        MoonTheme.AMBER_SKY -> R.string.theme_amber_sky
        MoonTheme.EMBER -> R.string.theme_ember
        MoonTheme.CUSTOM -> R.string.theme_custom
    }

@get:StringRes
val MoonTheme.noteRes: Int
    get() = when (this) {
        MoonTheme.MIDNIGHT -> R.string.theme_midnight_note
        MoonTheme.ECLIPSE -> R.string.theme_eclipse_note
        MoonTheme.HARVEST -> R.string.theme_harvest_note
        MoonTheme.SEA_GLASS -> R.string.theme_sea_glass_note
        MoonTheme.BLOOD_MOON -> R.string.theme_blood_moon_note
        MoonTheme.PARCHMENT -> R.string.theme_parchment_note
        MoonTheme.AMBER_SKY -> R.string.theme_amber_sky_note
        MoonTheme.EMBER -> R.string.theme_ember_note
        MoonTheme.CUSTOM -> R.string.theme_custom_note
    }

@get:StringRes
val ThemeRole.displayNameRes: Int
    get() = when (this) {
        ThemeRole.BACKGROUND -> R.string.role_background
        ThemeRole.SURFACE -> R.string.role_surface
        ThemeRole.TEXT -> R.string.role_text
        ThemeRole.MOON -> R.string.role_moon
        ThemeRole.ACCENT -> R.string.role_accent
    }

@get:StringRes
val ThemeRole.noteRes: Int
    get() = when (this) {
        ThemeRole.BACKGROUND -> R.string.role_background_note
        ThemeRole.SURFACE -> R.string.role_surface_note
        ThemeRole.TEXT -> R.string.role_text_note
        ThemeRole.MOON -> R.string.role_moon_note
        ThemeRole.ACCENT -> R.string.role_accent_note
    }

@get:StringRes
val AppFont.displayNameRes: Int
    get() = when (this) {
        AppFont.SYSTEM -> R.string.font_system
        AppFont.SERIF -> R.string.font_serif
        AppFont.MONOSPACE -> R.string.font_monospace
        AppFont.CONDENSED -> R.string.font_condensed
    }

@get:StringRes
val TextSize.displayNameRes: Int
    get() = when (this) {
        TextSize.SMALL -> R.string.text_size_small
        TextSize.DEFAULT -> R.string.text_size_default
        TextSize.LARGE -> R.string.text_size_large
        TextSize.EXTRA_LARGE -> R.string.text_size_extra_large
    }

@get:StringRes
val AppSection.displayNameRes: Int
    get() = when (this) {
        AppSection.RISE_SET -> R.string.section_rise_set
        AppSection.SKY_PATH -> R.string.section_sky_path
        AppSection.TIMING -> R.string.section_timing
        AppSection.DISTANCE -> R.string.section_distance
        AppSection.MOON_SIGN -> R.string.section_moon_sign
        AppSection.SUN_SIGN -> R.string.section_sun_sign
    }

@get:StringRes
val CompassPoint.displayNameRes: Int
    get() = when (this) {
        CompassPoint.NORTH -> R.string.compass_n
        CompassPoint.NORTH_NORTH_EAST -> R.string.compass_nne
        CompassPoint.NORTH_EAST -> R.string.compass_ne
        CompassPoint.EAST_NORTH_EAST -> R.string.compass_ene
        CompassPoint.EAST -> R.string.compass_e
        CompassPoint.EAST_SOUTH_EAST -> R.string.compass_ese
        CompassPoint.SOUTH_EAST -> R.string.compass_se
        CompassPoint.SOUTH_SOUTH_EAST -> R.string.compass_sse
        CompassPoint.SOUTH -> R.string.compass_s
        CompassPoint.SOUTH_SOUTH_WEST -> R.string.compass_ssw
        CompassPoint.SOUTH_WEST -> R.string.compass_sw
        CompassPoint.WEST_SOUTH_WEST -> R.string.compass_wsw
        CompassPoint.WEST -> R.string.compass_w
        CompassPoint.WEST_NORTH_WEST -> R.string.compass_wnw
        CompassPoint.NORTH_WEST -> R.string.compass_nw
        CompassPoint.NORTH_NORTH_WEST -> R.string.compass_nnw
    }

/** A bearing as people say it: "ENE 58°". */
fun bearingText(context: android.content.Context, azimuth: Double): String = context.getString(
    R.string.compass_bearing,
    context.getString(CompassPoint.forAzimuth(azimuth).displayNameRes),
    Math.round(azimuth).toInt() % 360,
)
