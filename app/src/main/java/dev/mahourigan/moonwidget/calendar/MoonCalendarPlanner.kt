package dev.mahourigan.moonwidget.calendar

import dev.mahourigan.moonwidget.astronomy.JulianDate
import dev.mahourigan.moonwidget.astronomy.MoonDistance
import dev.mahourigan.moonwidget.astronomy.MoonNames
import dev.mahourigan.moonwidget.astronomy.MoonPhase
import dev.mahourigan.moonwidget.astronomy.MoonSlot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The two moments worth putting in a calendar. */
enum class MoonEventKind { FULL, NEW }

/**
 * One full or new moon, as the calendar needs to know it.
 *
 * Carries no display text: this layer stays free of Android so it can be
 * unit-tested on a plain JVM, exactly like the astronomy package it sits on.
 * See [MoonCalendarText] for the translation to titles and descriptions.
 */
data class MoonOccurrence(
    val kind: MoonEventKind,
    /** The exact moment of the phase. */
    val peak: Instant,
    /** The local calendar day the entry lands on. */
    val date: LocalDate,
    /** Traditional name slot; full moons only. */
    val slot: MoonSlot?,
    val isSupermoon: Boolean,
)

/**
 * Works out which moons belong in the calendar, and when.
 *
 * Deliberately produces a *complete* picture of the wanted state over the whole
 * horizon rather than "the next one" — the calendar is then reconciled against
 * it (see [CalendarReconcile]), so a run that is late, repeated, or interrupted
 * converges on the same answer instead of piling up duplicates.
 */
object MoonCalendarPlanner {

    /**
     * How far ahead to fill in. Two years is around fifty entries — enough that
     * a user who opens their calendar a year out still sees them, and small
     * enough to write in one batch.
     */
    const val DEFAULT_HORIZON_MONTHS = 24L

    /** Stops a runaway loop if a phase search ever failed to advance. */
    private const val MAX_OCCURRENCES = 400

    /**
     * Every wanted moon between [from] and [horizonMonths] beyond it, in date
     * order.
     *
     * @param zone the observer's zone — a full moon at 00:30 UTC belongs to a
     *   different calendar day in Newcastle than in London, and the entry must
     *   land on the day the user would call it.
     * @param southernSeasons shifts the traditional names six months, matching
     *   the same option on the main screen.
     */
    fun occurrences(
        from: Instant,
        zone: ZoneId,
        includeFull: Boolean = true,
        includeNew: Boolean = true,
        southernSeasons: Boolean = false,
        horizonMonths: Long = DEFAULT_HORIZON_MONTHS,
    ): List<MoonOccurrence> {
        if (!includeFull && !includeNew) return emptyList()

        val cutoff = from.atZone(zone).toLocalDate().plusMonths(horizonMonths)
        val result = mutableListOf<MoonOccurrence>()

        if (includeFull) {
            collect(from, zone, cutoff, MoonEventKind.FULL, southernSeasons, result)
        }
        if (includeNew) {
            collect(from, zone, cutoff, MoonEventKind.NEW, southernSeasons, result)
        }

        return result.sortedBy { it.date }
    }

    private fun collect(
        from: Instant,
        zone: ZoneId,
        cutoff: LocalDate,
        kind: MoonEventKind,
        southernSeasons: Boolean,
        into: MutableList<MoonOccurrence>,
    ) {
        var cursor = JulianDate.fromInstant(from)

        repeat(MAX_OCCURRENCES) {
            val julianDay = when (kind) {
                MoonEventKind.FULL -> MoonPhase.nextFullMoon(cursor)
                MoonEventKind.NEW -> MoonPhase.nextNewMoon(cursor)
            }
            val peak = JulianDate.toInstant(julianDay)
            val date = peak.atZone(zone).toLocalDate()

            if (date.isAfter(cutoff)) return

            into.add(
                MoonOccurrence(
                    kind = kind,
                    peak = peak,
                    date = date,
                    // Only full moons carry the traditional names.
                    slot = if (kind == MoonEventKind.FULL) {
                        MoonNames.forFullMoon(julianDay, zone, southernSeasons)
                    } else {
                        null
                    },
                    // A new moon is never a "supermoon" in the usual sense —
                    // the term describes a full moon near perigee.
                    isSupermoon = kind == MoonEventKind.FULL &&
                        MoonDistance.isSupermoon(julianDay),
                )
            )

            // Step past this one so the next search cannot return it again.
            cursor = julianDay + 1
        }
    }
}
