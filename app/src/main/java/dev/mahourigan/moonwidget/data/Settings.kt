package dev.mahourigan.moonwidget.data

import dev.mahourigan.moonwidget.astronomy.MoonNameSet

/**
 * Everything the user can switch on or off.
 *
 * Defaults are chosen so a fresh install looks like the app did before settings
 * existed: the common things on, the opinionated extras off.
 */
data class Settings(
    // --- Widget contents ---
    /** The drawn Moon itself. */
    val widgetShowMoonImage: Boolean = true,
    /** Phase name ("Waxing Gibbous") on the wide widget. */
    val widgetShowPhaseName: Boolean = true,
    /** Illuminated percentage. */
    val widgetShowIllumination: Boolean = true,
    /** Next rise and set times. */
    val widgetShowRiseSet: Boolean = true,
    /** "Full moon in 4 days". */
    val widgetShowFullMoonCountdown: Boolean = true,
    /** Zodiac sign and symbol. Off by default — not everyone wants astrology. */
    val widgetShowMoonSign: Boolean = false,
    /** The dome showing tonight's pass across the sky. */
    /** Lay the zodiac glyph over the widget Moon. */
    val widgetShowSignOnMoon: Boolean = false,
    val widgetShowSkyPath: Boolean = true,
    /** Show the illumination under the moon even on the small square widget. */
    val widgetCompactShowIllumination: Boolean = false,
    /** The compass bearing on the wide widget. */
    val widgetShowDirection: Boolean = false,
    /** Draw the pass as a ring round the Moon instead of a dome below it. */
    val widgetSkyPathAsOrbit: Boolean = false,
    /** Draw the bearing as a ring round the Moon instead of a dial below it. */
    val widgetDirectionAsOrbit: Boolean = false,

    // --- App contents ---
    /** The large drawn Moon at the top of the screen. */
    val showMoonImage: Boolean = true,
    /** Lay the zodiac glyph over the large drawn Moon. */
    val showSignOnMoon: Boolean = false,
    /** Draw that glyph in one blended tone rather than inverting it. */
    val signOnMoonBlended: Boolean = false,
    val showPhaseName: Boolean = true,
    val showIllumination: Boolean = true,
    /** The rise/set card as a whole. */
    val showRiseSet: Boolean = true,
    /** Which way to face to see it, as a compass bearing. */
    val showCompassDirection: Boolean = true,
    /** "Currently above the horizon", inside the rise/set card. */
    val showHorizonStatus: Boolean = true,
    val showNextFullMoon: Boolean = true,
    /** The place name under the cards. */
    val showLocationLabel: Boolean = true,
    /**
     * Safe to hide: the location picker is also reachable from settings, which
     * is always visible.
     */
    val showChangeLocationButton: Boolean = true,
    val showMoonSignSection: Boolean = true,
    /**
     * The Sun's sign — the familiar "star sign" for the time of year.
     *
     * The Moon's sign changes every two and a half days; the Sun's is what
     * people actually mean by their sign, and it names the month.
     */
    val showSunSignSection: Boolean = true,
    /** The twelve-sign tropical zodiac everyone knows. */
    val sunSignTropical: Boolean = true,
    /**
     * The thirteen constellations the Sun really crosses, Ophiuchus included.
     * Off by default: it contradicts the sign most people call their own.
     */
    val sunSignAstronomical: Boolean = false,
    /** Distance, apparent size, and the supermoon / micromoon note. */
    val showDistance: Boolean = true,
    /** Days since the last new moon. */
    val showAge: Boolean = true,
    /** Next new moon alongside next full moon. */
    val showNextNewMoon: Boolean = true,
    /** The chart of the Moon's height through the day, above and below the horizon. */
    val showSkyPath: Boolean = true,
    // --- Times on the full / new moon rows ---
    /**
     * Master switch for putting a time of day on the phase rows,
     * e.g. "Fri, Aug 28 · 5:47 PM". The four settings below refine it.
     */
    val showPhaseEventTime: Boolean = true,
    /** Include a time on the full moon row. */
    val phaseTimeFullMoon: Boolean = true,
    /** Include a time on the new moon row. */
    val phaseTimeNewMoon: Boolean = true,
    /** Show when the Moon rises — when it first clears the horizon. */
    val phaseTimeRise: Boolean = true,
    /**
     * Show when the Moon peaks — highest in the sky, and best placed for
     * looking at.
     *
     * Called culmination in the astronomy code, and deliberately not "zenith":
     * the Moon passes directly overhead only between about 28.6 degrees north
     * and south.
     */
    val phaseTimePeak: Boolean = false,

    // --- Special moons ---
    /** Master switch for the traditional monthly moon names. */
    val showNamedMoons: Boolean = false,
    /**
     * Override the hemisphere the season names are chosen for.
     *
     * By default the names follow your latitude: shifted six months south of
     * the equator, since the traditional names describe northern conditions
     * and a "Wolf Moon" in a Newcastle January is midsummer. Turning this on
     * inverts whatever the location implies, for anyone who would rather see
     * the names they grew up with.
     */
    val swapSeasonNames: Boolean = false,
    /** Which tradition the names come from. Not a toggle — picked on its own screen. */
    val moonNameSet: MoonNameSet = MoonNameSet.DEFAULT,
    /** Show the January moon name. */
    val namedJanuary: Boolean = true,
    /** Show the February moon name. */
    val namedFebruary: Boolean = true,
    /** Show the March moon name. */
    val namedMarch: Boolean = true,
    /** Show the April moon name. */
    val namedApril: Boolean = true,
    /** Show the May moon name. */
    val namedMay: Boolean = true,
    /** Show the June moon name. */
    val namedJune: Boolean = true,
    /** Show the July moon name. */
    val namedJuly: Boolean = true,
    /** Show the August moon name. */
    val namedAugust: Boolean = true,
    /** Show the September moon name. */
    val namedSeptember: Boolean = true,
    /** Show the October moon name. */
    val namedOctober: Boolean = true,
    /** Show the November moon name. */
    val namedNovember: Boolean = true,
    /** Show the December moon name. */
    val namedDecember: Boolean = true,
    /** Show the Harvest moon name. */
    val namedHarvest: Boolean = true,

    // --- Calendar ---
    /**
     * Write full and new moons into the device calendar.
     *
     * Off by default, and needs calendar permission, which is only requested
     * when this is switched on. Switching it off removes every entry the app
     * wrote, past ones included — see
     * [dev.mahourigan.moonwidget.calendar.MoonCalendarSync].
     */
    val calendarSyncEnabled: Boolean = false,
    /** Include full moons, with their traditional names. */
    val calendarFullMoons: Boolean = true,
    /** Include new moons. */
    val calendarNewMoons: Boolean = false,

    // --- Presentation ---
    /**
     * Draw craters and maria across the lit face, tinted with the theme's
     * Moon colour, instead of a flat fill.
     *
     * Shares [MoonAppearance.rotationDegrees] with the plain disc, so when
     * [trueMoonOrientation] is also on the texture turns with it — the same
     * craters end up pointing the way they actually would in the sky.
     */
    val moonTextureEnabled: Boolean = false,
    /**
     * Rotate the disc to the angle the Moon really presents in the sky.
     *
     * The Moon appears to roll as it crosses the sky: the same crescent that
     * rises on its side can be sitting like a bowl by the time it sets. Off,
     * the terminator is simply drawn vertical and only flipped by hemisphere.
     */
    val trueMoonOrientation: Boolean = true,
    /** Ignore the system setting and always use a 24-hour clock. */
    val force24HourTime: Boolean = false,
    /** Highlight supermoons with a marker on the widget. */
    val highlightSupermoon: Boolean = true,
    /** Typeface for the app and the widget. Picked on its own screen. */
    val appFont: AppFont = AppFont.DEFAULT,
    /** Overall text size, as a multiplier. Picked on its own screen. */
    val textSize: TextSize = TextSize.DEFAULT,
    /** The order of the movable cards on the main screen. */
    val sectionOrder: List<AppSection> = AppSection.DEFAULT_ORDER,
    /** Which colour theme. Not a toggle — picked on its own screen. */
    val theme: MoonTheme = MoonTheme.DEFAULT,
    /**
     * The hand-picked colours, kept even while a preset is selected so that
     * trying one out and coming back does not lose the work.
     */
    val customTheme: ThemeColors = ThemeColors.MIDNIGHT,

    // --- Notifications ---
    val notifyFullMoon: Boolean = false,
    val notifyNewMoon: Boolean = false,
    /** Notify when the Moon rises, for the current location. */
    val notifyMoonrise: Boolean = false,

    // --- When each notification arrives ---
    // Kept per event: a week's warning of a full moon says nothing about
    // wanting the same for a new moon.
    val fullMoonLeadOnDay: Boolean = true,
    val fullMoonLeadOneDay: Boolean = false,
    val fullMoonLeadThreeDays: Boolean = false,
    val fullMoonLeadSevenDays: Boolean = false,
    val fullMoonLeadCustom: Boolean = false,
    val fullMoonCustomLeadDays: Int = 14,

    val newMoonLeadOnDay: Boolean = true,
    val newMoonLeadOneDay: Boolean = false,
    val newMoonLeadThreeDays: Boolean = false,
    val newMoonLeadSevenDays: Boolean = false,
    val newMoonLeadCustom: Boolean = false,
    val newMoonCustomLeadDays: Int = 14,

    /** Moonrise is a daily event, so its leads are minutes rather than days. */
    val riseLeadAt: Boolean = true,
    val riseLead15: Boolean = false,
    val riseLead30: Boolean = false,
    val riseLead60: Boolean = false,
    /**
     * Local time of day for advance reminders, as minutes past midnight.
     *
     * Without this a full moon reminder would fire at the exact moment of
     * fullness, which is as likely to be 3am as not. Moonrise alerts ignore it
     * — the whole point of those is the moment itself.
     */
    val notificationMinuteOfDay: Int = 9 * 60,
) {
    /** The colours actually on screen, resolving [MoonTheme.CUSTOM] to the user's own. */
    val palette: ThemeColors
        get() = if (theme.isCustom) customTheme else theme.colors

    /** True if any notification is switched on — decides whether to schedule work. */
    val anyNotificationEnabled: Boolean
        get() = notifyFullMoon || notifyNewMoon || notifyMoonrise

    /** Full or new moon reminders, the ones lead times apply to. */
    val anyPhaseNotification: Boolean get() = notifyFullMoon || notifyNewMoon

    /** True when at least one kind of time is selected. */
    val anyPhaseTimeSelected: Boolean get() = phaseTimeRise || phaseTimePeak

    /** Whether a time should appear on the full moon row. */
    val wantsFullMoonTime: Boolean
        get() = showPhaseEventTime && phaseTimeFullMoon && anyPhaseTimeSelected

    /** Whether a time should appear on the new moon row. */
    val wantsNewMoonTime: Boolean
        get() = showPhaseEventTime && phaseTimeNewMoon && anyPhaseTimeSelected

    /** Both kinds shown at once, so each needs labelling to tell them apart. */
    val showsBothPhaseTimes: Boolean get() = phaseTimeRise && phaseTimePeak

    /** Whether the rise/set card would have any content; used to skip empty cards. */
    val riseSetCardHasContent: Boolean
        get() = showRiseSet || showHorizonStatus || showCompassDirection

    /** Whether the phase-timing card would have any content. */
    val timingCardHasContent: Boolean
        get() = showNextFullMoon || showNextNewMoon || showAge

    /**
     * Whether the widget's tappable panel appears at all.
     *
     * Either toggle brings it up; the panel then cycles through whichever of
     * its views are available, the time-to-next-crossing included. Both off
     * means gone, so switching the sky path off still removes it the way it
     * always did.
     */
    val widgetPanelEnabled: Boolean
        get() = (widgetShowSkyPath && !widgetSkyPathIsOrbit) ||
            (widgetShowDirection && !widgetDirectionIsOrbit)

    /**
     * Whether each reading is drawn round the Moon rather than in the panel.
     *
     * Conditional on the Moon being shown at all: a ring needs something to
     * orbit, so with the Moon switched off both fall back to the panel rather
     * than disappearing, which is the only behaviour that loses nothing.
     */
    val widgetSkyPathIsOrbit: Boolean
        get() = widgetShowMoonImage && widgetShowSkyPath && widgetSkyPathAsOrbit

    val widgetDirectionIsOrbit: Boolean
        get() = widgetShowMoonImage && widgetShowDirection && widgetDirectionAsOrbit

    /** Whether the wide widget's text column would have any content. */
    val widgetTextColumnHasContent: Boolean
        get() = widgetShowPhaseName || widgetShowIllumination ||
            widgetShowMoonSign || widgetShowRiseSet || widgetShowFullMoonCountdown ||
            widgetShowDirection || widgetPanelEnabled
}

/**
 * A single switchable setting, described so the settings screen can be built
 * from a list rather than hand-written row by row.
 */
data class SettingToggle(
    val key: SettingKey,
    val isOn: Boolean,
)

enum class SettingKey {
    WIDGET_MOON_IMAGE,
    WIDGET_PHASE_NAME,
    WIDGET_ILLUMINATION,
    WIDGET_RISE_SET,
    WIDGET_FULL_MOON_COUNTDOWN,
    WIDGET_MOON_SIGN,
    WIDGET_SIGN_ON_MOON,
    WIDGET_SKY_PATH,
    WIDGET_COMPACT_ILLUMINATION,
    WIDGET_DIRECTION,
    WIDGET_SKY_PATH_ORBIT,
    WIDGET_DIRECTION_ORBIT,
    SHOW_MOON_IMAGE,
    SHOW_SIGN_ON_MOON,
    SIGN_ON_MOON_BLENDED,
    SHOW_PHASE_NAME,
    SHOW_ILLUMINATION,
    SHOW_RISE_SET,
    SHOW_COMPASS_DIRECTION,
    SHOW_HORIZON_STATUS,
    SHOW_NEXT_FULL_MOON,
    SHOW_LOCATION_LABEL,
    SHOW_CHANGE_LOCATION_BUTTON,
    SHOW_MOON_SIGN_SECTION,
    SHOW_SUN_SIGN_SECTION,
    SUN_SIGN_TROPICAL,
    SUN_SIGN_ASTRONOMICAL,
    SHOW_DISTANCE,
    SHOW_AGE,
    SHOW_NEXT_NEW_MOON,
    SHOW_SKY_PATH,
    SHOW_PHASE_EVENT_TIME,
    PHASE_TIME_FULL_MOON,
    PHASE_TIME_NEW_MOON,
    PHASE_TIME_RISE,
    PHASE_TIME_PEAK,
    SHOW_NAMED_MOONS,
    NAMED_MOON_JANUARY,
    NAMED_MOON_FEBRUARY,
    NAMED_MOON_MARCH,
    NAMED_MOON_APRIL,
    NAMED_MOON_MAY,
    NAMED_MOON_JUNE,
    NAMED_MOON_JULY,
    NAMED_MOON_AUGUST,
    NAMED_MOON_SEPTEMBER,
    NAMED_MOON_OCTOBER,
    NAMED_MOON_NOVEMBER,
    NAMED_MOON_DECEMBER,
    NAMED_MOON_HARVEST,
    CALENDAR_SYNC,
    CALENDAR_FULL_MOONS,
    CALENDAR_NEW_MOONS,
    MOON_TEXTURE,
    TRUE_ORIENTATION,
    SWAP_SEASON_NAMES,
    FORCE_24_HOUR,
    HIGHLIGHT_SUPERMOON,
    NOTIFY_FULL_MOON,
    NOTIFY_NEW_MOON,
    NOTIFY_MOONRISE,
    FULL_LEAD_ON_DAY,
    FULL_LEAD_ONE_DAY,
    FULL_LEAD_THREE_DAYS,
    FULL_LEAD_SEVEN_DAYS,
    FULL_LEAD_CUSTOM,
    NEW_LEAD_ON_DAY,
    NEW_LEAD_ONE_DAY,
    NEW_LEAD_THREE_DAYS,
    NEW_LEAD_SEVEN_DAYS,
    NEW_LEAD_CUSTOM,
    RISE_LEAD_AT,
    RISE_LEAD_15,
    RISE_LEAD_30,
    RISE_LEAD_60,
}

/** Read one setting out of [Settings] by key. */
fun Settings.valueOf(key: SettingKey): Boolean = when (key) {
    SettingKey.WIDGET_MOON_IMAGE -> widgetShowMoonImage
    SettingKey.WIDGET_PHASE_NAME -> widgetShowPhaseName
    SettingKey.WIDGET_ILLUMINATION -> widgetShowIllumination
    SettingKey.WIDGET_RISE_SET -> widgetShowRiseSet
    SettingKey.WIDGET_FULL_MOON_COUNTDOWN -> widgetShowFullMoonCountdown
    SettingKey.WIDGET_MOON_SIGN -> widgetShowMoonSign
    SettingKey.WIDGET_SIGN_ON_MOON -> widgetShowSignOnMoon
    SettingKey.WIDGET_SKY_PATH -> widgetShowSkyPath
    SettingKey.WIDGET_COMPACT_ILLUMINATION -> widgetCompactShowIllumination
    SettingKey.WIDGET_DIRECTION -> widgetShowDirection
    SettingKey.WIDGET_SKY_PATH_ORBIT -> widgetSkyPathAsOrbit
    SettingKey.WIDGET_DIRECTION_ORBIT -> widgetDirectionAsOrbit
    SettingKey.SHOW_MOON_IMAGE -> showMoonImage
    SettingKey.SHOW_SIGN_ON_MOON -> showSignOnMoon
    SettingKey.SIGN_ON_MOON_BLENDED -> signOnMoonBlended
    SettingKey.SHOW_PHASE_NAME -> showPhaseName
    SettingKey.SHOW_ILLUMINATION -> showIllumination
    SettingKey.SHOW_RISE_SET -> showRiseSet
    SettingKey.SHOW_COMPASS_DIRECTION -> showCompassDirection
    SettingKey.SHOW_HORIZON_STATUS -> showHorizonStatus
    SettingKey.SHOW_NEXT_FULL_MOON -> showNextFullMoon
    SettingKey.SHOW_LOCATION_LABEL -> showLocationLabel
    SettingKey.SHOW_CHANGE_LOCATION_BUTTON -> showChangeLocationButton
    SettingKey.SHOW_MOON_SIGN_SECTION -> showMoonSignSection
    SettingKey.SHOW_SUN_SIGN_SECTION -> showSunSignSection
    SettingKey.SUN_SIGN_TROPICAL -> sunSignTropical
    SettingKey.SUN_SIGN_ASTRONOMICAL -> sunSignAstronomical
    SettingKey.SHOW_DISTANCE -> showDistance
    SettingKey.SHOW_AGE -> showAge
    SettingKey.SHOW_NEXT_NEW_MOON -> showNextNewMoon
    SettingKey.SHOW_SKY_PATH -> showSkyPath
    SettingKey.SHOW_PHASE_EVENT_TIME -> showPhaseEventTime
    SettingKey.PHASE_TIME_FULL_MOON -> phaseTimeFullMoon
    SettingKey.PHASE_TIME_NEW_MOON -> phaseTimeNewMoon
    SettingKey.PHASE_TIME_RISE -> phaseTimeRise
    SettingKey.PHASE_TIME_PEAK -> phaseTimePeak
    SettingKey.SHOW_NAMED_MOONS -> showNamedMoons
    SettingKey.NAMED_MOON_JANUARY -> namedJanuary
    SettingKey.NAMED_MOON_FEBRUARY -> namedFebruary
    SettingKey.NAMED_MOON_MARCH -> namedMarch
    SettingKey.NAMED_MOON_APRIL -> namedApril
    SettingKey.NAMED_MOON_MAY -> namedMay
    SettingKey.NAMED_MOON_JUNE -> namedJune
    SettingKey.NAMED_MOON_JULY -> namedJuly
    SettingKey.NAMED_MOON_AUGUST -> namedAugust
    SettingKey.NAMED_MOON_SEPTEMBER -> namedSeptember
    SettingKey.NAMED_MOON_OCTOBER -> namedOctober
    SettingKey.NAMED_MOON_NOVEMBER -> namedNovember
    SettingKey.NAMED_MOON_DECEMBER -> namedDecember
    SettingKey.NAMED_MOON_HARVEST -> namedHarvest
    SettingKey.CALENDAR_SYNC -> calendarSyncEnabled
    SettingKey.CALENDAR_FULL_MOONS -> calendarFullMoons
    SettingKey.CALENDAR_NEW_MOONS -> calendarNewMoons
    SettingKey.MOON_TEXTURE -> moonTextureEnabled
    SettingKey.TRUE_ORIENTATION -> trueMoonOrientation
    SettingKey.SWAP_SEASON_NAMES -> swapSeasonNames
    SettingKey.FORCE_24_HOUR -> force24HourTime
    SettingKey.HIGHLIGHT_SUPERMOON -> highlightSupermoon
    SettingKey.NOTIFY_FULL_MOON -> notifyFullMoon
    SettingKey.NOTIFY_NEW_MOON -> notifyNewMoon
    SettingKey.NOTIFY_MOONRISE -> notifyMoonrise
    SettingKey.FULL_LEAD_ON_DAY -> fullMoonLeadOnDay
    SettingKey.FULL_LEAD_ONE_DAY -> fullMoonLeadOneDay
    SettingKey.FULL_LEAD_THREE_DAYS -> fullMoonLeadThreeDays
    SettingKey.FULL_LEAD_SEVEN_DAYS -> fullMoonLeadSevenDays
    SettingKey.FULL_LEAD_CUSTOM -> fullMoonLeadCustom
    SettingKey.NEW_LEAD_ON_DAY -> newMoonLeadOnDay
    SettingKey.NEW_LEAD_ONE_DAY -> newMoonLeadOneDay
    SettingKey.NEW_LEAD_THREE_DAYS -> newMoonLeadThreeDays
    SettingKey.NEW_LEAD_SEVEN_DAYS -> newMoonLeadSevenDays
    SettingKey.NEW_LEAD_CUSTOM -> newMoonLeadCustom
    SettingKey.RISE_LEAD_AT -> riseLeadAt
    SettingKey.RISE_LEAD_15 -> riseLead15
    SettingKey.RISE_LEAD_30 -> riseLead30
    SettingKey.RISE_LEAD_60 -> riseLead60
}

/** Return a copy with one setting changed. */
fun Settings.with(key: SettingKey, value: Boolean): Settings = when (key) {
    SettingKey.WIDGET_MOON_IMAGE -> copy(widgetShowMoonImage = value)
    SettingKey.WIDGET_PHASE_NAME -> copy(widgetShowPhaseName = value)
    SettingKey.WIDGET_ILLUMINATION -> copy(widgetShowIllumination = value)
    SettingKey.WIDGET_RISE_SET -> copy(widgetShowRiseSet = value)
    SettingKey.WIDGET_FULL_MOON_COUNTDOWN -> copy(widgetShowFullMoonCountdown = value)
    SettingKey.WIDGET_MOON_SIGN -> copy(widgetShowMoonSign = value)
    SettingKey.WIDGET_SIGN_ON_MOON -> copy(widgetShowSignOnMoon = value)
    SettingKey.WIDGET_SKY_PATH -> copy(widgetShowSkyPath = value)
    SettingKey.WIDGET_COMPACT_ILLUMINATION -> copy(widgetCompactShowIllumination = value)
    SettingKey.WIDGET_DIRECTION -> copy(widgetShowDirection = value)
    SettingKey.WIDGET_SKY_PATH_ORBIT -> copy(widgetSkyPathAsOrbit = value)
    SettingKey.WIDGET_DIRECTION_ORBIT -> copy(widgetDirectionAsOrbit = value)
    SettingKey.SHOW_MOON_IMAGE -> copy(showMoonImage = value)
    SettingKey.SHOW_SIGN_ON_MOON -> copy(showSignOnMoon = value)
    SettingKey.SIGN_ON_MOON_BLENDED -> copy(signOnMoonBlended = value)
    SettingKey.SHOW_PHASE_NAME -> copy(showPhaseName = value)
    SettingKey.SHOW_ILLUMINATION -> copy(showIllumination = value)
    SettingKey.SHOW_RISE_SET -> copy(showRiseSet = value)
    SettingKey.SHOW_COMPASS_DIRECTION -> copy(showCompassDirection = value)
    SettingKey.SHOW_HORIZON_STATUS -> copy(showHorizonStatus = value)
    SettingKey.SHOW_NEXT_FULL_MOON -> copy(showNextFullMoon = value)
    SettingKey.SHOW_LOCATION_LABEL -> copy(showLocationLabel = value)
    SettingKey.SHOW_CHANGE_LOCATION_BUTTON -> copy(showChangeLocationButton = value)
    SettingKey.SHOW_MOON_SIGN_SECTION -> copy(showMoonSignSection = value)
    SettingKey.SHOW_SUN_SIGN_SECTION -> copy(showSunSignSection = value)
    SettingKey.SUN_SIGN_TROPICAL -> copy(sunSignTropical = value)
    SettingKey.SUN_SIGN_ASTRONOMICAL -> copy(sunSignAstronomical = value)
    SettingKey.SHOW_DISTANCE -> copy(showDistance = value)
    SettingKey.SHOW_AGE -> copy(showAge = value)
    SettingKey.SHOW_NEXT_NEW_MOON -> copy(showNextNewMoon = value)
    SettingKey.SHOW_SKY_PATH -> copy(showSkyPath = value)
    SettingKey.SHOW_PHASE_EVENT_TIME -> copy(showPhaseEventTime = value)
    SettingKey.PHASE_TIME_FULL_MOON -> copy(phaseTimeFullMoon = value)
    SettingKey.PHASE_TIME_NEW_MOON -> copy(phaseTimeNewMoon = value)
    SettingKey.PHASE_TIME_RISE -> copy(phaseTimeRise = value)
    SettingKey.PHASE_TIME_PEAK -> copy(phaseTimePeak = value)
    SettingKey.SHOW_NAMED_MOONS -> copy(showNamedMoons = value)
    SettingKey.NAMED_MOON_JANUARY -> copy(namedJanuary = value)
    SettingKey.NAMED_MOON_FEBRUARY -> copy(namedFebruary = value)
    SettingKey.NAMED_MOON_MARCH -> copy(namedMarch = value)
    SettingKey.NAMED_MOON_APRIL -> copy(namedApril = value)
    SettingKey.NAMED_MOON_MAY -> copy(namedMay = value)
    SettingKey.NAMED_MOON_JUNE -> copy(namedJune = value)
    SettingKey.NAMED_MOON_JULY -> copy(namedJuly = value)
    SettingKey.NAMED_MOON_AUGUST -> copy(namedAugust = value)
    SettingKey.NAMED_MOON_SEPTEMBER -> copy(namedSeptember = value)
    SettingKey.NAMED_MOON_OCTOBER -> copy(namedOctober = value)
    SettingKey.NAMED_MOON_NOVEMBER -> copy(namedNovember = value)
    SettingKey.NAMED_MOON_DECEMBER -> copy(namedDecember = value)
    SettingKey.NAMED_MOON_HARVEST -> copy(namedHarvest = value)
    SettingKey.CALENDAR_SYNC -> copy(calendarSyncEnabled = value)
    SettingKey.CALENDAR_FULL_MOONS -> copy(calendarFullMoons = value)
    SettingKey.CALENDAR_NEW_MOONS -> copy(calendarNewMoons = value)
    SettingKey.MOON_TEXTURE -> copy(moonTextureEnabled = value)
    SettingKey.TRUE_ORIENTATION -> copy(trueMoonOrientation = value)
    SettingKey.SWAP_SEASON_NAMES -> copy(swapSeasonNames = value)
    SettingKey.FORCE_24_HOUR -> copy(force24HourTime = value)
    SettingKey.HIGHLIGHT_SUPERMOON -> copy(highlightSupermoon = value)
    SettingKey.NOTIFY_FULL_MOON -> copy(notifyFullMoon = value)
    SettingKey.NOTIFY_NEW_MOON -> copy(notifyNewMoon = value)
    SettingKey.NOTIFY_MOONRISE -> copy(notifyMoonrise = value)
    SettingKey.FULL_LEAD_ON_DAY -> copy(fullMoonLeadOnDay = value)
    SettingKey.FULL_LEAD_ONE_DAY -> copy(fullMoonLeadOneDay = value)
    SettingKey.FULL_LEAD_THREE_DAYS -> copy(fullMoonLeadThreeDays = value)
    SettingKey.FULL_LEAD_SEVEN_DAYS -> copy(fullMoonLeadSevenDays = value)
    SettingKey.FULL_LEAD_CUSTOM -> copy(fullMoonLeadCustom = value)
    SettingKey.NEW_LEAD_ON_DAY -> copy(newMoonLeadOnDay = value)
    SettingKey.NEW_LEAD_ONE_DAY -> copy(newMoonLeadOneDay = value)
    SettingKey.NEW_LEAD_THREE_DAYS -> copy(newMoonLeadThreeDays = value)
    SettingKey.NEW_LEAD_SEVEN_DAYS -> copy(newMoonLeadSevenDays = value)
    SettingKey.NEW_LEAD_CUSTOM -> copy(newMoonLeadCustom = value)
    SettingKey.RISE_LEAD_AT -> copy(riseLeadAt = value)
    SettingKey.RISE_LEAD_15 -> copy(riseLead15 = value)
    SettingKey.RISE_LEAD_30 -> copy(riseLead30 = value)
    SettingKey.RISE_LEAD_60 -> copy(riseLead60 = value)
}

/**
 * Settings that change what [dev.mahourigan.moonwidget.domain.MoonSnapshot] computes,
 * rather than merely what is shown.
 *
 * Toggling one of these has to trigger a recompute — the values live inside the
 * snapshot, so re-rendering with the same snapshot would silently change
 * nothing. Everything else only affects visibility and needs no recalculation.
 */
val SettingKey.affectsSnapshot: Boolean
    get() = this in setOf(
        SettingKey.SHOW_COMPASS_DIRECTION,
        SettingKey.WIDGET_DIRECTION,
        SettingKey.SHOW_SKY_PATH,
        SettingKey.WIDGET_SKY_PATH,
        SettingKey.TRUE_ORIENTATION,
        SettingKey.SHOW_NAMED_MOONS,
        SettingKey.SWAP_SEASON_NAMES,
        SettingKey.SHOW_PHASE_EVENT_TIME,
        SettingKey.PHASE_TIME_FULL_MOON,
        SettingKey.PHASE_TIME_NEW_MOON,
        SettingKey.PHASE_TIME_RISE,
        SettingKey.PHASE_TIME_PEAK,
    )

/** The toggle governing a particular monthly slot. */
fun namedMoonKey(slot: dev.mahourigan.moonwidget.astronomy.MoonSlot): SettingKey = when (slot) {
    dev.mahourigan.moonwidget.astronomy.MoonSlot.JANUARY -> SettingKey.NAMED_MOON_JANUARY
    dev.mahourigan.moonwidget.astronomy.MoonSlot.FEBRUARY -> SettingKey.NAMED_MOON_FEBRUARY
    dev.mahourigan.moonwidget.astronomy.MoonSlot.MARCH -> SettingKey.NAMED_MOON_MARCH
    dev.mahourigan.moonwidget.astronomy.MoonSlot.APRIL -> SettingKey.NAMED_MOON_APRIL
    dev.mahourigan.moonwidget.astronomy.MoonSlot.MAY -> SettingKey.NAMED_MOON_MAY
    dev.mahourigan.moonwidget.astronomy.MoonSlot.JUNE -> SettingKey.NAMED_MOON_JUNE
    dev.mahourigan.moonwidget.astronomy.MoonSlot.JULY -> SettingKey.NAMED_MOON_JULY
    dev.mahourigan.moonwidget.astronomy.MoonSlot.AUGUST -> SettingKey.NAMED_MOON_AUGUST
    dev.mahourigan.moonwidget.astronomy.MoonSlot.SEPTEMBER -> SettingKey.NAMED_MOON_SEPTEMBER
    dev.mahourigan.moonwidget.astronomy.MoonSlot.OCTOBER -> SettingKey.NAMED_MOON_OCTOBER
    dev.mahourigan.moonwidget.astronomy.MoonSlot.NOVEMBER -> SettingKey.NAMED_MOON_NOVEMBER
    dev.mahourigan.moonwidget.astronomy.MoonSlot.DECEMBER -> SettingKey.NAMED_MOON_DECEMBER
    dev.mahourigan.moonwidget.astronomy.MoonSlot.HARVEST -> SettingKey.NAMED_MOON_HARVEST
}

/** Notification toggles need the runtime permission on Android 13+. */
val SettingKey.needsNotificationPermission: Boolean
    get() = this in setOf(
        SettingKey.NOTIFY_FULL_MOON,
        SettingKey.NOTIFY_NEW_MOON,
        SettingKey.NOTIFY_MOONRISE,
    )

/**
 * Writing to the calendar needs its own runtime permission, asked for only
 * when the feature is switched on.
 *
 * Only the master switch is listed: the children are unreachable until it is
 * on, by which point permission has already been granted.
 */
val SettingKey.needsCalendarPermission: Boolean
    get() = this == SettingKey.CALENDAR_SYNC

/**
 * Settings that change which entries belong in the calendar, so toggling one
 * has to re-run the reconcile.
 *
 * Wider than [needsCalendarPermission] on purpose: the moon *names* and the
 * supermoon marker appear in the event titles, so changing those has to rewrite
 * them, or the calendar quietly disagrees with the app.
 */
val SettingKey.affectsCalendar: Boolean
    get() = this in setOf(
        SettingKey.CALENDAR_SYNC,
        SettingKey.CALENDAR_FULL_MOONS,
        SettingKey.CALENDAR_NEW_MOONS,
        SettingKey.SHOW_NAMED_MOONS,
        SettingKey.SWAP_SEASON_NAMES,
        SettingKey.HIGHLIGHT_SUPERMOON,
        SettingKey.FORCE_24_HOUR,
    ) || name.startsWith("NAMED_MOON_")
