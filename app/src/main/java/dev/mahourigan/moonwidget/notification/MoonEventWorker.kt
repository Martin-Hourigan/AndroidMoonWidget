package dev.mahourigan.moonwidget.notification

import dev.mahourigan.moonwidget.astronomy.JulianDate
import dev.mahourigan.moonwidget.astronomy.MoonDistance
import dev.mahourigan.moonwidget.astronomy.MoonPhase
import dev.mahourigan.moonwidget.data.LocationRepository
import dev.mahourigan.moonwidget.data.NotificationLead
import dev.mahourigan.moonwidget.data.Prefs
import dev.mahourigan.moonwidget.data.NotifiableEvent
import dev.mahourigan.moonwidget.data.leadDaysFor
import dev.mahourigan.moonwidget.data.moonriseLeadMinutes
import dev.mahourigan.moonwidget.domain.MoonSnapshot
import dev.mahourigan.moonwidget.ui.displayName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Fires the moon-event notifications the user has asked for, then schedules
 * itself for the next one.
 *
 * Each event type is scheduled independently rather than polled, so nothing
 * wakes up until there is genuinely something to say.
 */
class MoonEventWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val event = inputData.getString(KEY_EVENT) ?: return Result.success()
        val leadDays = inputData.getInt(KEY_LEAD_DAYS, 0)
        val prefs = Prefs(applicationContext)
        val settings = prefs.currentSettings()

        runCatching {
            when (event) {
                EVENT_FULL_MOON -> if (settings.notifyFullMoon) postFullMoon(leadDays)
                EVENT_NEW_MOON -> if (settings.notifyNewMoon) {
                    MoonNotifications.newMoon(applicationContext, leadDays)
                }
                EVENT_MOONRISE -> if (settings.notifyMoonrise) postMoonrise(prefs)
            }
        }.onFailure { Log.e(TAG, "Failed to post $event", it) }

        // Re-arm regardless, so one failure does not end the series.
        runCatching { scheduleFor(applicationContext, event, leadDays) }
            .onFailure { Log.e(TAG, "Failed to reschedule $event", it) }

        return Result.success()
    }

    private fun postFullMoon(leadDays: Int) {
        // Describe the moon at the event, not right now: a reminder a week
        // ahead should not report that the moon is 43% lit.
        val eventJd = MoonPhase.nextFullMoon(JulianDate.fromInstant(Instant.now()))
        MoonNotifications.fullMoon(
            context = applicationContext,
            illuminationPercent = MoonPhase.at(eventJd).illuminationPercent,
            isSupermoon = MoonDistance.at(eventJd).isNearPerigee,
            leadDays = leadDays,
        )
    }

    private suspend fun postMoonrise(prefs: Prefs) {
        val location = LocationRepository(applicationContext, prefs).resolve()
        val snapshot = MoonSnapshot.compute(location)
        MoonNotifications.moonrise(
            context = applicationContext,
            phaseName = snapshot.phase.phaseName.displayName(applicationContext),
        )
    }

    companion object {
        private const val TAG = "MoonEventWorker"
        private const val KEY_EVENT = "event"
        private const val KEY_LEAD_DAYS = "lead_days"

        const val EVENT_FULL_MOON = "full_moon"
        const val EVENT_NEW_MOON = "new_moon"
        const val EVENT_MOONRISE = "moonrise"

        private fun workName(event: String, leadDays: Int) =
            "moon_event_${event}_${if (leadDays < 0) "m" + (-leadDays) else leadDays.toString() + "d"}"

        /** Every lead a phase reminder might have been scheduled under. */
        private fun allPossibleLeads(): List<Int> =
            // Positive values are days ahead; negatives are moonrise minutes.
            (-60..NotificationLead.MAX_CUSTOM_DAYS).toList()

        /**
         * Bring scheduled work in line with the current settings: schedule what
         * is switched on, cancel what is not.
         */
        suspend fun sync(context: Context) {
            val settings = Prefs(context).currentSettings()

            // Clear everything first: leads can be switched off as well as on,
            // and a stale reminder would otherwise keep firing forever.
            cancelAll(context)

            settings.leadDaysFor(NotifiableEvent.FULL_MOON)
                .forEach { scheduleFor(context, EVENT_FULL_MOON, it) }

            settings.leadDaysFor(NotifiableEvent.NEW_MOON)
                .forEach { scheduleFor(context, EVENT_NEW_MOON, it) }

            // Moonrise leads are minutes before the rise, not days before a date.
            settings.moonriseLeadMinutes()
                .forEach { scheduleFor(context, EVENT_MOONRISE, leadDays = -it) }
        }

        /** Queue the next reminder for [event] at [leadDays] notice. */
        suspend fun scheduleFor(context: Context, event: String, leadDays: Int) {
            val now = Instant.now()
            val prefs = Prefs(context)
            val settings = prefs.currentSettings()
            val zone = LocationRepository(context, prefs).resolve().zone

            val target: Instant? = when (event) {
                // Moonrise is anchored to the rise itself, so the lead is
                // carried as negative minutes rather than whole days.
                EVENT_MOONRISE -> nextMoonrise(context, now)
                    ?.minusSeconds(-leadDays.toLong() * 60)
                    ?.takeIf { it.isAfter(now) }
                    ?: nextMoonrise(context, now)

                EVENT_FULL_MOON, EVENT_NEW_MOON -> ReminderSchedule.nextReminder(
                    events = phaseEvents(event, now),
                    leadDays = leadDays,
                    minuteOfDay = settings.notificationMinuteOfDay,
                    zone = zone,
                    now = now,
                )

                else -> null
            }

            if (target == null) {
                Log.w(TAG, "Nothing upcoming for $event at $leadDays days notice")
                return
            }

            val delay = Duration.between(now, target).seconds.coerceAtLeast(60)

            val request = OneTimeWorkRequestBuilder<MoonEventWorker>()
                .setInitialDelay(delay, TimeUnit.SECONDS)
                .setInputData(
                    androidx.work.workDataOf(KEY_EVENT to event, KEY_LEAD_DAYS to leadDays)
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(event, leadDays),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /** Successive full or new moons, as a lazy sequence. */
        private fun phaseEvents(event: String, from: Instant): Sequence<Instant> = sequence {
            var jd = JulianDate.fromInstant(from)
            repeat(6) {
                val next = if (event == EVENT_FULL_MOON) {
                    MoonPhase.nextFullMoon(jd)
                } else {
                    MoonPhase.nextNewMoon(jd)
                }
                yield(JulianDate.toInstant(next))
                jd = next + 1
            }
        }

        private suspend fun nextMoonrise(context: Context, from: Instant): Instant? {
            val prefs = Prefs(context)
            val location = LocationRepository(context, prefs).resolve()
            return dev.mahourigan.moonwidget.astronomy.RiseSet
                .nextEvents(from, location.geo)
                .rise
        }

        fun cancelAll(context: Context) {
            val manager = WorkManager.getInstance(context)
            listOf(EVENT_FULL_MOON, EVENT_NEW_MOON, EVENT_MOONRISE).forEach { event ->
                allPossibleLeads().forEach { lead ->
                    manager.cancelUniqueWork(workName(event, lead))
                }
            }
        }
    }
}
