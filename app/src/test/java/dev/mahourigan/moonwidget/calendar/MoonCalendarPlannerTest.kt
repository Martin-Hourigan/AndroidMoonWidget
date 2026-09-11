package dev.mahourigan.moonwidget.calendar

import dev.mahourigan.moonwidget.astronomy.MoonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

class MoonCalendarPlannerTest {

    private val newcastle = ZoneId.of("Australia/Sydney")
    private val start = Instant.parse("2026-09-01T00:00:00Z")

    private fun plan(
        includeFull: Boolean = true,
        includeNew: Boolean = true,
        zone: ZoneId = newcastle,
        months: Long = 24,
    ) = MoonCalendarPlanner.occurrences(
        from = start,
        zone = zone,
        includeFull = includeFull,
        includeNew = includeNew,
        horizonMonths = months,
    )

    // --- What gets included ---

    @Test
    fun `wanting neither kind produces nothing`() {
        assertTrue(plan(includeFull = false, includeNew = false).isEmpty())
    }

    @Test
    fun `each kind can be asked for on its own`() {
        assertTrue(plan(includeNew = false).all { it.kind == MoonEventKind.FULL })
        assertTrue(plan(includeFull = false).all { it.kind == MoonEventKind.NEW })
    }

    @Test
    fun `both kinds come back in date order`() {
        val dates = plan().map { it.date }
        assertEquals(dates.sorted(), dates)
    }

    @Test
    fun `a two year horizon holds roughly two years of lunations`() {
        // ~12.37 lunations a year, both kinds, two years.
        val count = plan().size
        assertTrue("expected about 49, got $count", count in 45..53)
    }

    @Test
    fun `nothing lands beyond the horizon`() {
        val cutoff = start.atZone(newcastle).toLocalDate().plusMonths(6)
        assertTrue(plan(months = 6).all { !it.date.isAfter(cutoff) })
    }

    @Test
    fun `nothing lands before the starting point`() {
        val from = start.atZone(newcastle).toLocalDate()
        assertTrue(plan().all { !it.date.isBefore(from) })
    }

    // --- The invariant the reconcile depends on ---

    /**
     * [CalendarReconcile] keys entries by date, which is only safe because two
     * wanted moons can never share a day. Full and new are always about
     * fourteen days apart, so this should hold over any span.
     */
    @Test
    fun `no two moons ever fall on the same day`() {
        val dates = MoonCalendarPlanner.occurrences(
            from = start,
            zone = newcastle,
            horizonMonths = 120, // ten years
        ).map { it.date }

        val duplicates = dates.groupBy { it }.filterValues { it.size > 1 }
        assertTrue("dates must be unique, clashes: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `the same invariant holds in a far western zone`() {
        // A different zone moves the boundaries; it must not create a clash.
        val dates = MoonCalendarPlanner.occurrences(
            from = start,
            zone = ZoneId.of("Pacific/Honolulu"),
            horizonMonths = 120,
        ).map { it.date }

        assertEquals(dates.size, dates.toSet().size)
    }

    // --- Astronomy sanity ---

    @Test
    fun `successive full moons are a lunation apart`() {
        val fullMoons = plan(includeNew = false).map { it.peak }

        fullMoons.zipWithNext().forEach { (a, b) ->
            val gap = Duration.between(a, b).toMinutes() / 1440.0
            // The real interval swings either side of the mean by up to ~7 hours.
            assertTrue("gap of $gap days between $a and $b", abs(gap - MoonPhase.SYNODIC_MONTH) < 0.6)
        }
    }

    @Test
    fun `a full moon really is full, and a new moon really is new`() {
        plan().take(12).forEach { occurrence ->
            val illumination = MoonPhase.at(occurrence.peak).illumination
            when (occurrence.kind) {
                MoonEventKind.FULL ->
                    assertTrue("$occurrence should be lit", illumination > 0.99)
                MoonEventKind.NEW ->
                    assertTrue("$occurrence should be dark", illumination < 0.01)
            }
        }
    }

    @Test
    fun `full and new moons alternate`() {
        val kinds = plan().map { it.kind }
        kinds.zipWithNext().forEach { (a, b) ->
            assertTrue("two $a in a row", a != b)
        }
    }

    // --- Naming and flags ---

    @Test
    fun `only full moons carry a name slot or a supermoon flag`() {
        plan().filter { it.kind == MoonEventKind.NEW }.forEach {
            assertNull("a new moon has no traditional name", it.slot)
            assertTrue("a new moon is never a supermoon", !it.isSupermoon)
        }
    }

    @Test
    fun `every full moon has a name slot`() {
        plan(includeNew = false).forEach { assertNotNull(it.slot) }
    }

    @Test
    fun `supermoons happen, but are not the common case`() {
        val fullMoons = plan(includeNew = false)
        val supermoons = fullMoons.count { it.isSupermoon }

        assertTrue("expected at least one supermoon in two years", supermoons > 0)
        assertTrue("expected supermoons to be a minority, got $supermoons of ${fullMoons.size}",
            supermoons < fullMoons.size / 2)
    }

    // --- Zone handling ---

    @Test
    fun `the local date follows the observer's zone`() {
        // A moon peaking near midnight UTC belongs to a different day either
        // side of the date line, and the entry has to land on the day the user
        // would call it.
        val east = MoonCalendarPlanner.occurrences(
            from = start, zone = ZoneId.of("Pacific/Auckland"), horizonMonths = 24,
        )
        val west = MoonCalendarPlanner.occurrences(
            from = start, zone = ZoneId.of("America/Anchorage"), horizonMonths = 24,
        )

        val differing = east.zip(west).count { (a, b) -> a.date != b.date }
        assertTrue("zones nearly a day apart should disagree somewhere", differing > 0)
    }

    @Test
    fun `the peak instant does not depend on the zone`() {
        // Only the calendar day is local; the moment itself is absolute.
        val sydney = plan(zone = newcastle).map { it.peak }
        val london = plan(zone = ZoneId.of("Europe/London")).map { it.peak }
        assertEquals(sydney, london)
    }
}
