package dev.mahourigan.moonwidget.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Periodically redraws the widget.
 *
 * Half-hourly is a deliberate compromise: the illuminated fraction changes far
 * too slowly to notice, but rise/set times roll over to the next day and the
 * "full moon in N days" countdown needs to tick. WorkManager's minimum period
 * is 15 minutes anyway, and battery matters more than instant accuracy here.
 */
class MoonUpdateWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        MoonWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "moon_widget_refresh"
        private const val INTERVAL_MINUTES = 30L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MoonUpdateWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP so re-placing a widget doesn't reset the schedule.
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            MidnightUpdateWorker.schedule(context)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            MidnightUpdateWorker.cancel(context)
        }
    }
}

/**
 * Redraws the widget just after local midnight, then schedules itself again.
 *
 * The half-hourly worker gets there eventually, but the date rolling over is
 * the one moment when the display is visibly wrong — the "full moon in N days"
 * countdown and the day hints on rise/set times all change at once. Waiting up
 * to thirty minutes to correct that is the difference people would notice.
 *
 * A one-shot that re-arms is used rather than listening for `ACTION_DATE_CHANGED`,
 * because that broadcast is not exempt from the implicit-broadcast restrictions
 * introduced in Android 8, so a manifest-declared receiver would not reliably
 * fire.
 */
class MidnightUpdateWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        MoonWidget().updateAll(applicationContext)
        // Re-arm for the next midnight before returning.
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "moon_widget_midnight"

        /** A minute past midnight, so we are safely the other side of the boundary. */
        private const val BUFFER_SECONDS = 60L

        fun schedule(context: Context, zone: ZoneId = ZoneId.systemDefault()) {
            val now = java.time.ZonedDateTime.now(zone)
            val nextMidnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone)
            val delaySeconds = Duration.between(now, nextMidnight).seconds + BUFFER_SECONDS

            val request = OneTimeWorkRequestBuilder<MidnightUpdateWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                // REPLACE: a new schedule should win, e.g. after a timezone change.
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
