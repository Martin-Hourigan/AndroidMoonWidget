package dev.mahourigan.moonwidget.data

/**
 * Which notification a lead setting belongs to.
 *
 * Leads are per-event rather than shared: wanting a week's warning of a full
 * moon says nothing about wanting one for a new moon, and moonrise is a daily
 * event where days of notice make no sense at all.
 */
enum class NotifiableEvent {
    FULL_MOON,
    NEW_MOON,

    /**
     * Happens every day at a different time, so its leads are measured in
     * minutes before the rise rather than days before a date.
     */
    MOONRISE,
    ;

    /** The toggle that switches this notification on. */
    val enableKey: SettingKey
        get() = when (this) {
            FULL_MOON -> SettingKey.NOTIFY_FULL_MOON
            NEW_MOON -> SettingKey.NOTIFY_NEW_MOON
            MOONRISE -> SettingKey.NOTIFY_MOONRISE
        }
}

/** How far ahead of a full or new moon to send a reminder. */
enum class NotificationLead(val days: Int) {
    ON_DAY(0),
    ONE_DAY(1),
    THREE_DAYS(3),
    SEVEN_DAYS(7),

    /** A user-chosen number of days; read the per-event custom value instead. */
    CUSTOM(-1),
    ;

    companion object {
        const val MIN_CUSTOM_DAYS = 1

        /** A lunation is about 29.5 days, so more notice than this would overlap. */
        const val MAX_CUSTOM_DAYS = 28
    }
}

/** How far ahead of a moonrise to send a reminder, in minutes. */
enum class MoonriseLead(val minutes: Int) {
    AT_RISE(0),
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    ;

    val key: SettingKey
        get() = when (this) {
            AT_RISE -> SettingKey.RISE_LEAD_AT
            FIFTEEN_MINUTES -> SettingKey.RISE_LEAD_15
            THIRTY_MINUTES -> SettingKey.RISE_LEAD_30
            ONE_HOUR -> SettingKey.RISE_LEAD_60
        }
}

/** The toggle for [lead] on [event]. Only valid for the phase events. */
fun leadKey(event: NotifiableEvent, lead: NotificationLead): SettingKey = when (event) {
    NotifiableEvent.FULL_MOON -> when (lead) {
        NotificationLead.ON_DAY -> SettingKey.FULL_LEAD_ON_DAY
        NotificationLead.ONE_DAY -> SettingKey.FULL_LEAD_ONE_DAY
        NotificationLead.THREE_DAYS -> SettingKey.FULL_LEAD_THREE_DAYS
        NotificationLead.SEVEN_DAYS -> SettingKey.FULL_LEAD_SEVEN_DAYS
        NotificationLead.CUSTOM -> SettingKey.FULL_LEAD_CUSTOM
    }

    NotifiableEvent.NEW_MOON -> when (lead) {
        NotificationLead.ON_DAY -> SettingKey.NEW_LEAD_ON_DAY
        NotificationLead.ONE_DAY -> SettingKey.NEW_LEAD_ONE_DAY
        NotificationLead.THREE_DAYS -> SettingKey.NEW_LEAD_THREE_DAYS
        NotificationLead.SEVEN_DAYS -> SettingKey.NEW_LEAD_SEVEN_DAYS
        NotificationLead.CUSTOM -> SettingKey.NEW_LEAD_CUSTOM
    }

    NotifiableEvent.MOONRISE ->
        error("Moonrise leads are measured in minutes; use MoonriseLead")
}

/** The user's custom day count for [event]. */
fun Settings.customLeadDaysFor(event: NotifiableEvent): Int {
    val raw = when (event) {
        NotifiableEvent.FULL_MOON -> fullMoonCustomLeadDays
        NotifiableEvent.NEW_MOON -> newMoonCustomLeadDays
        NotifiableEvent.MOONRISE -> 0
    }
    return raw.coerceIn(NotificationLead.MIN_CUSTOM_DAYS, NotificationLead.MAX_CUSTOM_DAYS)
}

/**
 * The day counts to schedule for [event], deduplicated and sorted.
 *
 * Empty when the event is switched off, or when every lead has been unticked —
 * an enabled notification with no chosen timing has nothing to schedule.
 */
fun Settings.leadDaysFor(event: NotifiableEvent): List<Int> {
    if (event == NotifiableEvent.MOONRISE) return emptyList()
    if (!valueOf(event.enableKey)) return emptyList()

    return NotificationLead.entries
        .filter { valueOf(leadKey(event, it)) }
        .map { if (it == NotificationLead.CUSTOM) customLeadDaysFor(event) else it.days }
        .distinct()
        .sorted()
}

/** Minutes-before values to schedule for moonrise. */
fun Settings.moonriseLeadMinutes(): List<Int> {
    if (!notifyMoonrise) return emptyList()

    return MoonriseLead.entries
        .filter { valueOf(it.key) }
        .map { it.minutes }
        .distinct()
        .sorted()
}
