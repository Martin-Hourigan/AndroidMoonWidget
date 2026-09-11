package dev.mahourigan.moonwidget.calendar

import dev.mahourigan.moonwidget.data.LocationRepository
import dev.mahourigan.moonwidget.data.Prefs
import dev.mahourigan.moonwidget.data.Settings
import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** What a sync attempt did, or why it could not. */
sealed interface CalendarSyncResult {
    data class Applied(val inserted: Int, val updated: Int, val deleted: Int) : CalendarSyncResult {
        val changed: Int get() = inserted + updated + deleted
    }

    /** The user has not granted calendar access, or has revoked it. */
    data object MissingPermission : CalendarSyncResult

    /** No writable calendar on the device — e.g. no account has been added. */
    data object NoCalendar : CalendarSyncResult

    data class Failed(val error: Throwable) : CalendarSyncResult
}

/**
 * Writes full and new moons into the device's calendar, and takes them out
 * again.
 *
 * Events go into the user's existing calendar rather than a private one of our
 * own, because a locally-created calendar never reaches Google's servers and so
 * would not appear on their other devices. The cost of that choice is that we
 * cannot use `ExtendedProperties` to tag our rows — the provider reserves that
 * table for sync adapters — so the entries are recognised by
 * [MOON_CALENDAR_MARKER] in the description instead. Unlike a locally stored
 * list of row ids, that marker travels with the event through a sync, so it
 * still identifies our entries on a new phone.
 *
 * Every path here is safe to run repeatedly: the work is a reconcile against
 * what is already there, not a blind insert. See [CalendarReconcile].
 */
object MoonCalendarSync {

    private const val TAG = "MoonCalendarSync"

    /** Provider batches have a size limit; well under it, and cheap to retry. */
    private const val BATCH_SIZE = 100

    fun hasPermission(context: Context): Boolean =
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            .all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

    /**
     * Bring the calendar in line with the settings.
     *
     * Switching the feature off is handled here too: with nothing wanted, the
     * reconcile removes everything of ours, including past entries.
     */
    suspend fun sync(context: Context, now: Instant = Instant.now()): CalendarSyncResult {
        if (!hasPermission(context)) return CalendarSyncResult.MissingPermission

        val prefs = Prefs(context)
        val settings = prefs.currentSettings()
        val zone = runCatching { LocationRepository(context, prefs).resolve().zone }
            .getOrDefault(ZoneId.systemDefault())

        return runCatching {
            val existing = readOurEvents(context)

            if (!settings.calendarSyncEnabled) {
                // Off means gone, history included — the user asked for the
                // entries to be removed, not archived.
                return@runCatching apply(context, calendarId = null, CalendarReconcile.removeAll(existing))
            }

            val calendarId = writableCalendarId(context)
                ?: return@runCatching CalendarSyncResult.NoCalendar

            val desired = MoonCalendarText.describeAll(
                context = context,
                occurrences = MoonCalendarPlanner.occurrences(
                    from = now,
                    zone = zone,
                    includeFull = settings.calendarFullMoons,
                    includeNew = settings.calendarNewMoons,
                    southernSeasons = southernSeasonsFor(context, prefs, settings),
                ),
                settings = settings,
                zone = zone,
            )

            val plan = CalendarReconcile.plan(
                desired = desired,
                existing = existing,
                today = now.atZone(zone).toLocalDate(),
            )

            apply(context, calendarId, plan)
        }.getOrElse {
            Log.e(TAG, "Calendar sync failed", it)
            CalendarSyncResult.Failed(it)
        }
    }

    /** Remove every entry of ours, whatever the settings say. */
    suspend fun removeAll(context: Context): CalendarSyncResult {
        if (!hasPermission(context)) return CalendarSyncResult.MissingPermission
        return runCatching {
            apply(context, calendarId = null, CalendarReconcile.removeAll(readOurEvents(context)))
        }.getOrElse {
            Log.e(TAG, "Calendar cleanup failed", it)
            CalendarSyncResult.Failed(it)
        }
    }

    /**
     * Season names follow the hemisphere the user is in, inverted by the
     * setting — the same rule the main screen uses, so a moon's name never
     * differs between the app and the calendar.
     */
    private suspend fun southernSeasonsFor(
        context: Context,
        prefs: Prefs,
        settings: Settings,
    ): Boolean {
        val latitude = runCatching { LocationRepository(context, prefs).resolve().geo.latitude }
            .getOrDefault(0.0)
        return (latitude < 0) != settings.swapSeasonNames
    }

    /**
     * Every event carrying our marker, across all calendars.
     *
     * Not scoped to the calendar we write to on purpose: if the user switches
     * accounts, entries written into the old one must still be findable, or
     * turning the feature off would strand them.
     */
    private fun readOurEvents(context: Context): List<ExistingEvent> {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events.DESCRIPTION} LIKE ? AND ${CalendarContract.Events.DELETED} = 0",
            arrayOf("%$MOON_CALENDAR_MARKER%"),
            null,
        ) ?: return emptyList()

        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val start = it.getLong(3)
                    add(
                        ExistingEvent(
                            id = it.getLong(0),
                            title = it.getString(1).orEmpty(),
                            description = it.getString(2).orEmpty(),
                            date = dateOfAllDay(start),
                        )
                    )
                }
            }
        }
    }

    /**
     * An all-day event is stored as midnight **UTC** on its date, by contract,
     * regardless of where the user is — so it must be read back in UTC too.
     * Using the local zone here would shift entries by a day for anyone west of
     * Greenwich, which is exactly the sort of bug that only shows up for other
     * people.
     */
    private fun dateOfAllDay(dtStartMillis: Long): LocalDate =
        Instant.ofEpochMilli(dtStartMillis).atZone(ZoneOffset.UTC).toLocalDate()

    private fun startMillisOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /**
     * A calendar we are allowed to add events to, preferring the user's primary
     * one so entries land where they would expect.
     */
    private fun writableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE,
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            // Contributor is the lowest level that may add events.
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null,
        ) ?: return null

        data class Candidate(val id: Long, val primary: Boolean, val google: Boolean, val visible: Boolean)

        val candidates = cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        Candidate(
                            id = it.getLong(0),
                            primary = it.getInt(1) == 1,
                            google = it.getString(2) == "com.google",
                            visible = it.getInt(3) == 1,
                        )
                    )
                }
            }
        }

        // Primary first, then a visible Google calendar, then anything writable
        // — so a device with no Google account still gets somewhere to write.
        return candidates
            .sortedWith(
                compareByDescending<Candidate> { it.primary }
                    .thenByDescending { it.google }
                    .thenByDescending { it.visible }
            )
            .firstOrNull()
            ?.id
    }

    /**
     * Carry out a plan.
     *
     * Batched, and chunked below the provider's operation limit. Deletes are
     * applied before inserts so a day being replaced never briefly holds two
     * entries.
     */
    private fun apply(context: Context, calendarId: Long?, plan: CalendarPlan): CalendarSyncResult {
        if (plan.isEmpty) return CalendarSyncResult.Applied(0, 0, 0)

        val operations = mutableListOf<ContentProviderOperation>()

        plan.delete.forEach { id ->
            operations += ContentProviderOperation
                .newDelete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
                .build()
        }

        plan.update.forEach { (id, event) ->
            operations += ContentProviderOperation
                .newUpdate(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
                .withValue(CalendarContract.Events.TITLE, event.title)
                .withValue(CalendarContract.Events.DESCRIPTION, event.description)
                .build()
        }

        if (calendarId != null) {
            plan.insert.forEach { event ->
                operations += ContentProviderOperation
                    .newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValues(valuesFor(calendarId, event))
                    .build()
            }
        }

        operations.chunked(BATCH_SIZE).forEach { batch ->
            context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(batch))
        }

        return CalendarSyncResult.Applied(
            inserted = if (calendarId != null) plan.insert.size else 0,
            updated = plan.update.size,
            deleted = plan.delete.size,
        )
    }

    private fun valuesFor(calendarId: Long, event: DesiredEvent) = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, event.title)
        put(CalendarContract.Events.DESCRIPTION, event.description)
        put(CalendarContract.Events.DTSTART, startMillisOf(event.date))
        // Exclusive end: an all-day event runs to midnight the following day.
        put(CalendarContract.Events.DTEND, startMillisOf(event.date.plusDays(1)))
        put(CalendarContract.Events.ALL_DAY, 1)
        // Required to be UTC for an all-day event, and unrelated to where the
        // user is — see dateOfAllDay.
        put(CalendarContract.Events.EVENT_TIMEZONE, ZoneOffset.UTC.id)
        // A silent entry: this is a marker on the day, not an appointment.
        put(CalendarContract.Events.HAS_ALARM, 0)
    }
}
