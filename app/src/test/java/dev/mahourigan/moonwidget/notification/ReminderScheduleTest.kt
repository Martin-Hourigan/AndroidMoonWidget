package dev.mahourigan.moonwidget.notification

import dev.mahourigan.moonwidget.data.NotificationLead
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.data.MoonriseLead
import dev.mahourigan.moonwidget.data.NotifiableEvent
import dev.mahourigan.moonwidget.data.customLeadDaysFor
import dev.mahourigan.moonwidget.data.leadDaysFor
import dev.mahourigan.moonwidget.data.moonriseLeadMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduleTest {

    private val zone: ZoneId = ZoneId.of("Australia/Sydney")

    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, month, day, hour, minute).atZone(zone).toInstant()

    /** A full moon peaking at 2am on the 28th. */
    private val fullMoon = at(9, 28, 2, 0)

    @Test
    fun `a seven day reminder fires seven days before, at the chosen time`() {
        val fire = ReminderSchedule.reminderTime(
            event = fullMoon,
            leadDays = 7,
            minuteOfDay = 9 * 60,
            zone = zone,
            now = at(9, 1, 12),
        )

        assertEquals(at(9, 21, 9, 0), fire)
    }

    /**
     * The point of the time-of-day setting: a full moon peaking at 2am must not
     * produce a 2am notification.
     */
    @Test
    fun `an on-the-day reminder uses the chosen time, not the moment of the event`() {
        val fire = ReminderSchedule.reminderTime(
            event = fullMoon,
            leadDays = 0,
            minuteOfDay = 9 * 60,
            zone = zone,
            now = at(9, 1, 12),
        )

        assertEquals(at(9, 28, 9, 0), fire)
        assertTrue("should not fire at the 2am peak", fire != fullMoon)
    }

    @Test
    fun `a reminder whose moment has passed is refused`() {
        val fire = ReminderSchedule.reminderTime(
            event = fullMoon,
            leadDays = 7,
            minuteOfDay = 9 * 60,
            zone = zone,
            // Already the 25th: the seven-day mark was four days ago.
            now = at(9, 25, 12),
        )

        assertNull(fire)
    }

    /**
     * The real behaviour that depends on the above: a missed lead should attach
     * to the following event rather than being dropped or fired immediately.
     */
    @Test
    fun `a missed lead rolls on to the next event`() {
        val events = sequenceOf(fullMoon, at(10, 27, 14, 0))

        val fire = ReminderSchedule.nextReminder(
            events = events,
            leadDays = 7,
            minuteOfDay = 9 * 60,
            zone = zone,
            now = at(9, 25, 12),
        )

        assertEquals("should use the October full moon", at(10, 20, 9, 0), fire)
    }

    @Test
    fun `nextReminder returns null when nothing is far enough ahead`() {
        val fire = ReminderSchedule.nextReminder(
            events = sequenceOf(fullMoon),
            leadDays = 7,
            minuteOfDay = 9 * 60,
            zone = zone,
            now = at(9, 25, 12),
            maxEventsToTry = 1,
        )
        assertNull(fire)
    }

    @Test
    fun `reminders are always in the future`() {
        val now = at(9, 10, 8, 30)

        listOf(0, 1, 3, 7, 14).forEach { lead ->
            val fire = ReminderSchedule.nextReminder(
                events = sequenceOf(fullMoon, at(10, 27, 14, 0)),
                leadDays = lead,
                minuteOfDay = 9 * 60,
                zone = zone,
                now = now,
            )
            assertNotNull("no reminder for a $lead day lead", fire)
            assertTrue("$lead day lead fired in the past", fire!!.isAfter(now))
        }
    }

    /** Ordering sanity: more notice means an earlier reminder. */
    @Test
    fun `longer leads fire earlier`() {
        val now = at(9, 1, 12)
        val times = listOf(0, 1, 3, 7).map { lead ->
            ReminderSchedule.reminderTime(fullMoon, lead, 9 * 60, zone, now)!!
        }

        times.zipWithNext { earlier, later ->
            assertTrue("leads out of order", earlier.isAfter(later))
        }
    }

    @Test
    fun `the time of day is respected exactly`() {
        listOf(0, 6 * 60, 9 * 60 + 30, 23 * 60 + 59).forEach { minuteOfDay ->
            val fire = ReminderSchedule.reminderTime(
                event = fullMoon,
                leadDays = 3,
                minuteOfDay = minuteOfDay,
                zone = zone,
                now = at(9, 1, 12),
            )!!

            val local = fire.atZone(zone)
            assertEquals(minuteOfDay / 60, local.hour)
            assertEquals(minuteOfDay % 60, local.minute)
        }
    }

    @Test
    fun `custom lead resolves per event`() {
        val settings = Settings(
            notifyFullMoon = true,
            notifyNewMoon = true,
            fullMoonCustomLeadDays = 12,
            newMoonCustomLeadDays = 20,
        )

        assertEquals(12, settings.customLeadDaysFor(NotifiableEvent.FULL_MOON))
        assertEquals(20, settings.customLeadDaysFor(NotifiableEvent.NEW_MOON))
    }

    @Test
    fun `custom lead is clamped to a sensible range`() {
        assertEquals(
            NotificationLead.MAX_CUSTOM_DAYS,
            Settings(fullMoonCustomLeadDays = 999).customLeadDaysFor(NotifiableEvent.FULL_MOON),
        )
        assertEquals(
            NotificationLead.MIN_CUSTOM_DAYS,
            Settings(fullMoonCustomLeadDays = 0).customLeadDaysFor(NotifiableEvent.FULL_MOON),
        )
    }

    @Test
    fun `leads are deduplicated and sorted`() {
        val settings = Settings(
            notifyFullMoon = true,
            fullMoonLeadOnDay = true,
            fullMoonLeadSevenDays = true,
            fullMoonLeadCustom = true,
            // Same as the seven-day toggle, so it must not schedule twice.
            fullMoonCustomLeadDays = 7,
        )

        assertEquals(listOf(0, 7), settings.leadDaysFor(NotifiableEvent.FULL_MOON))
    }

    /** The whole point of the change: the two events are independent. */
    @Test
    fun `each event keeps its own leads`() {
        val settings = Settings(
            notifyFullMoon = true,
            notifyNewMoon = true,
            fullMoonLeadOnDay = true,
            fullMoonLeadSevenDays = true,
            newMoonLeadOnDay = false,
            newMoonLeadOneDay = true,
        )

        assertEquals(listOf(0, 7), settings.leadDaysFor(NotifiableEvent.FULL_MOON))
        assertEquals(listOf(1), settings.leadDaysFor(NotifiableEvent.NEW_MOON))
    }

    /** A switched-off event schedules nothing, however its leads are set. */
    @Test
    fun `a disabled event schedules nothing`() {
        val settings = Settings(
            notifyFullMoon = false,
            fullMoonLeadOnDay = true,
            fullMoonLeadSevenDays = true,
        )
        assertTrue(settings.leadDaysFor(NotifiableEvent.FULL_MOON).isEmpty())
    }

    @Test
    fun `an enabled event with no leads schedules nothing`() {
        val settings = Settings(
            notifyFullMoon = true,
            fullMoonLeadOnDay = false,
            fullMoonLeadOneDay = false,
            fullMoonLeadThreeDays = false,
            fullMoonLeadSevenDays = false,
            fullMoonLeadCustom = false,
        )
        assertTrue(settings.leadDaysFor(NotifiableEvent.FULL_MOON).isEmpty())
    }

    /** Default: one reminder each, on the day. */
    @Test
    fun `defaults give a single same-day reminder per event`() {
        val settings = Settings(notifyFullMoon = true, notifyNewMoon = true)

        assertEquals(listOf(0), settings.leadDaysFor(NotifiableEvent.FULL_MOON))
        assertEquals(listOf(0), settings.leadDaysFor(NotifiableEvent.NEW_MOON))
    }

    /** Moonrise leads are minutes, and never days. */
    @Test
    fun `moonrise leads are measured in minutes`() {
        val settings = Settings(
            notifyMoonrise = true,
            riseLeadAt = true,
            riseLead30 = true,
        )

        assertEquals(listOf(0, 30), settings.moonriseLeadMinutes())
        // Day-based leads must never apply to a daily event.
        assertTrue(settings.leadDaysFor(NotifiableEvent.MOONRISE).isEmpty())
    }

    @Test
    fun `moonrise schedules nothing when switched off`() {
        assertTrue(Settings(notifyMoonrise = false, riseLeadAt = true).moonriseLeadMinutes().isEmpty())
    }

    @Test
    fun `every moonrise lead maps to a distinct toggle`() {
        val keys = MoonriseLead.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    /**
     * Crossing a daylight-saving boundary should still land on the wall-clock
     * time asked for, not an hour either side of it.
     */
    @Test
    fun `daylight saving does not shift the notification time`() {
        // Sydney moves to daylight time on the first Sunday of October 2026.
        val octoberEvent = LocalDateTime.of(2026, 10, 10, 20, 0).atZone(zone).toInstant()

        val fire = ReminderSchedule.reminderTime(
            event = octoberEvent,
            leadDays = 7,
            minuteOfDay = 9 * 60,
            zone = zone,
            now = LocalDateTime.of(2026, 9, 20, 12, 0).atZone(zone).toInstant(),
        )!!

        val local = fire.atZone(zone)
        assertEquals(9, local.hour)
        assertEquals(0, local.minute)
        assertEquals(3, local.dayOfMonth)

        // And it really is a week before, in wall-clock terms.
        val gap = Duration.between(fire, octoberEvent).toDays()
        assertTrue("gap was $gap days", gap in 6..7)
    }
}
