package dev.mahourigan.moonwidget.calendar

import dev.mahourigan.moonwidget.data.Prefs
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Keeps the calendar entries current.
 *
 * Unlike the notification worker, this is a plain repeating job rather than a
 * self-rescheduling one-shot: there is no single moment it has to hit, only a
 * horizon that needs topping up as time passes, and a slow drift of moon times
 * whenever the user moves. Daily is far more often than either needs, and the
 * reconcile makes a run with nothing to do almost free.
 */
class CalendarSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val result = MoonCalendarSync.sync(applicationContext)
        Log.i(TAG, "Calendar sync: $result")

        // Retry only where retrying could plausibly help. A missing permission
        // or a device with no calendar will not fix itself before the next
        // scheduled run, and retrying would just burn battery.
        return when (result) {
            is CalendarSyncResult.Failed -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val TAG = "CalendarSyncWorker"
        private const val WORK_NAME = "moon_calendar_sync"

        /**
         * Match the schedule to the setting: run daily while it is on, stop
         * when it is off.
         */
        suspend fun sync(context: Context) {
            val enabled = Prefs(context).currentSettings().calendarSyncEnabled
            if (enabled) schedule(context) else cancel(context)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP, not UPDATE: re-arming on every settings change would
                // restart the period and could starve a device that is toggled
                // often, and the existing job already does the right thing.
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
