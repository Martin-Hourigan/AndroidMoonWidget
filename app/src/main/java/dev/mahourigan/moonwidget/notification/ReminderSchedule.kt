package dev.mahourigan.moonwidget.notification

import java.time.Instant
import java.time.ZoneId

/**
 * Works out when a reminder should actually fire.
 *
 * Deliberately free of Android and WorkManager so the arithmetic can be tested
 * directly — off-by-one days and "fires in the past" bugs are the easy mistakes
 * here, and neither is visible from the UI until a reminder fails to arrive.
 */
object ReminderSchedule {

    /**
     * The moment to notify for an event at [event], [leadDays] ahead of it, at
     * [minuteOfDay] local time.
     *
     * Returns null when that moment has already passed — the caller should then
     * try the following occurrence rather than firing immediately.
     *
     * The time of day applies to same-day reminders too: a full moon peaks at
     * whatever hour it peaks, often the middle of the night, and a notification
     * at 3am is not a feature.
     */
    fun reminderTime(
        event: Instant,
        leadDays: Int,
        minuteOfDay: Int,
        zone: ZoneId,
        now: Instant,
    ): Instant? {
        val eventDate = event.atZone(zone).toLocalDate()
        val fireDate = eventDate.minusDays(leadDays.toLong())

        val fireAt = fireDate
            .atStartOfDay(zone)
            .plusMinutes(minuteOfDay.toLong())
            .toInstant()

        return fireAt.takeIf { it.isAfter(now) }
    }

    /**
     * The first reminder still in the future, walking through [events] in order.
     *
     * A seven-day reminder for a full moon three days away has already been
     * missed, so it should attach to the *next* full moon instead.
     */
    fun nextReminder(
        events: Sequence<Instant>,
        leadDays: Int,
        minuteOfDay: Int,
        zone: ZoneId,
        now: Instant,
        maxEventsToTry: Int = 4,
    ): Instant? = events
        .take(maxEventsToTry)
        .firstNotNullOfOrNull { event ->
            reminderTime(event, leadDays, minuteOfDay, zone, now)
        }
}
