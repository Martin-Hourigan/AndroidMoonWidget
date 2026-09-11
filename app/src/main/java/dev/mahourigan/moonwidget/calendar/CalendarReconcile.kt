package dev.mahourigan.moonwidget.calendar

import java.time.LocalDate

/**
 * The tag that marks an event as ours.
 *
 * Deliberately **not** a string resource. This text is how the app recognises
 * its own entries later, so it has to survive the user changing device
 * language — a localised marker would orphan every event already written the
 * moment the locale changed. It is user-visible in the event body, which is a
 * small price for cleanup that cannot lose track of itself.
 *
 * The obvious alternative, `CalendarContract.ExtendedProperties`, is closed to
 * ordinary apps: the provider only accepts writes there from sync adapters.
 */
const val MOON_CALENDAR_MARKER = "Added by Moon Widget"

/** An entry the calendar should contain. */
data class DesiredEvent(
    val date: LocalDate,
    val title: String,
    val description: String,
)

/** An entry the calendar already contains, recognised by [MOON_CALENDAR_MARKER]. */
data class ExistingEvent(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val description: String,
)

/**
 * What to do to bring the calendar in line. Empty everywhere means nothing to
 * do, which is the common case once the horizon is filled.
 */
data class CalendarPlan(
    val insert: List<DesiredEvent> = emptyList(),
    val update: List<Pair<Long, DesiredEvent>> = emptyList(),
    val delete: List<Long> = emptyList(),
) {
    val isEmpty: Boolean get() = insert.isEmpty() && update.isEmpty() && delete.isEmpty()

    val size: Int get() = insert.size + update.size + delete.size
}

/**
 * Works out the difference between the moons that should be in the calendar and
 * the ones already there.
 *
 * Identity is the **local date**. A full moon and a new moon are always about
 * fourteen days apart, so no two wanted entries ever share a day — which makes
 * the date a safe key and means no hidden identifier has to be smuggled into
 * the event text.
 *
 * The whole point of reconciling rather than blindly inserting is that this is
 * idempotent: running it twice changes nothing the second time, and a run that
 * died halfway is repaired by the next one rather than leaving duplicates.
 */
object CalendarReconcile {

    /**
     * Bring the calendar in line with [desired].
     *
     * Entries before [today] are left completely alone — they are a record of
     * moons that have already happened, and re-deriving them every sync only to
     * delete them would quietly erase the user's history. Only [removeAll]
     * touches the past, and only when the user explicitly switches the feature
     * off.
     */
    fun plan(
        desired: List<DesiredEvent>,
        existing: List<ExistingEvent>,
        today: LocalDate,
    ): CalendarPlan {
        val wanted = desired
            .filterNot { it.date.isBefore(today) }
            .associateBy { it.date }

        val insert = mutableListOf<DesiredEvent>()
        val update = mutableListOf<Pair<Long, DesiredEvent>>()
        val delete = mutableListOf<Long>()

        // Group by date so a day that somehow ended up with two of our entries
        // — an interrupted run, a sync that duplicated a row — is healed rather
        // than left to accumulate.
        val byDate = existing
            .filterNot { it.date.isBefore(today) }
            .groupBy { it.date }

        byDate.forEach { (date, sameDay) ->
            // Keep the lowest id: arbitrary, but stable across runs, so two
            // devices reconciling the same calendar agree on the survivor.
            val keeper = sameDay.minBy { it.id }
            delete += sameDay.filter { it.id != keeper.id }.map { it.id }

            val want = wanted[date]
            if (want == null) {
                // Ours, still upcoming, but no longer wanted: the kind was
                // switched off, or the horizon moved.
                delete += keeper.id
            } else if (keeper.title != want.title || keeper.description != want.description) {
                // Same day, different text — a renamed name set, a shifted
                // time after a location change.
                update += keeper.id to want
            }
        }

        wanted.forEach { (date, want) ->
            if (date !in byDate) insert += want
        }

        return CalendarPlan(
            insert = insert.sortedBy { it.date },
            update = update.sortedBy { it.second.date },
            delete = delete.sorted(),
        )
    }

    /**
     * Remove every entry of ours, past included.
     *
     * This is the "user switched the setting off" path, where leaving history
     * behind would be the surprising outcome — they asked for the entries to go.
     */
    fun removeAll(existing: List<ExistingEvent>): CalendarPlan =
        CalendarPlan(delete = existing.map { it.id }.sorted())
}
