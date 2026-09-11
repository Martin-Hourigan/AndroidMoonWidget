package dev.mahourigan.moonwidget.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Getting this wrong is how a user ends up with fifty duplicate full moons, or
 * with their own events deleted — so it is tested far harder than its size
 * suggests.
 */
class CalendarReconcileTest {

    private val today = LocalDate.of(2026, 9, 1)

    private fun desired(day: Int, title: String = "Full Moon", body: String = "body") =
        DesiredEvent(LocalDate.of(2026, 9, day), title, body)

    private fun existing(id: Long, day: Int, title: String = "Full Moon", body: String = "body") =
        ExistingEvent(id, LocalDate.of(2026, 9, day), title, body)

    // --- The easy cases ---

    @Test
    fun `nothing wanted and nothing there is nothing to do`() {
        val plan = CalendarReconcile.plan(emptyList(), emptyList(), today)
        assertTrue(plan.isEmpty)
    }

    @Test
    fun `an empty calendar gets everything inserted`() {
        val wanted = listOf(desired(5), desired(20))
        val plan = CalendarReconcile.plan(wanted, emptyList(), today)

        assertEquals(wanted, plan.insert)
        assertTrue(plan.update.isEmpty())
        assertTrue(plan.delete.isEmpty())
    }

    /** The common case, and the one that has to stay free: nothing changed. */
    @Test
    fun `a calendar already in step needs no work at all`() {
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5), desired(20)),
            existing = listOf(existing(1, 5), existing(2, 20)),
            today = today,
        )
        assertTrue("expected no changes, got $plan", plan.isEmpty)
    }

    // --- Change detection ---

    @Test
    fun `a changed title is an update, not a delete and reinsert`() {
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5, title = "Full Moon — Harvest Moon")),
            existing = listOf(existing(7, 5, title = "Full Moon")),
            today = today,
        )

        assertEquals(listOf(7L to desired(5, title = "Full Moon — Harvest Moon")), plan.update)
        assertTrue(plan.insert.isEmpty())
        assertTrue(plan.delete.isEmpty())
    }

    @Test
    fun `a changed description is an update too`() {
        // This is what a location change looks like: same day, new time in the body.
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5, body = "Full moon at 6:12 PM")),
            existing = listOf(existing(7, 5, body = "Full moon at 5:37 PM")),
            today = today,
        )
        assertEquals(1, plan.update.size)
        assertEquals(7L, plan.update.single().first)
    }

    @Test
    fun `an entry no longer wanted is deleted`() {
        // Switching new moons off leaves its entries behind to be removed.
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5)),
            existing = listOf(existing(1, 5), existing(2, 20)),
            today = today,
        )
        assertEquals(listOf(2L), plan.delete)
        assertTrue(plan.insert.isEmpty())
        assertTrue(plan.update.isEmpty())
    }

    // --- Self-healing ---

    @Test
    fun `duplicates on one day are collapsed to the lowest id`() {
        // A run that died halfway, or two devices racing.
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5)),
            existing = listOf(existing(9, 5), existing(3, 5), existing(11, 5)),
            today = today,
        )

        assertEquals("should keep exactly one", listOf(9L, 11L), plan.delete)
        assertTrue(plan.insert.isEmpty())
    }

    @Test
    fun `duplicates are collapsed even when the survivor also needs updating`() {
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5, title = "New title")),
            existing = listOf(existing(4, 5), existing(2, 5)),
            today = today,
        )

        assertEquals(listOf(4L), plan.delete)
        assertEquals(listOf(2L), plan.update.map { it.first })
    }

    @Test
    fun `duplicates on a day that is no longer wanted all go`() {
        val plan = CalendarReconcile.plan(
            desired = emptyList(),
            existing = listOf(existing(1, 5), existing(2, 5)),
            today = today,
        )
        assertEquals(listOf(1L, 2L), plan.delete)
    }

    // --- The past is left alone ---

    @Test
    fun `past entries are never touched`() {
        // A record of moons that already happened. Re-deriving them every sync
        // only to delete them would quietly erase the user's history.
        val past = ExistingEvent(1, today.minusDays(30), "Full Moon", "body")
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(20)),
            existing = listOf(past),
            today = today,
        )

        assertTrue("past must not be deleted", plan.delete.isEmpty())
        assertTrue("past must not be updated", plan.update.isEmpty())
        assertEquals(listOf(desired(20)), plan.insert)
    }

    @Test
    fun `a past entry whose text has since changed is still left alone`() {
        val past = ExistingEvent(1, today.minusDays(30), "Stale Title", "stale")
        val plan = CalendarReconcile.plan(
            desired = listOf(DesiredEvent(today.minusDays(30), "Fresh Title", "fresh")),
            existing = listOf(past),
            today = today,
        )
        assertTrue(plan.isEmpty)
    }

    @Test
    fun `moons already past are not inserted`() {
        val plan = CalendarReconcile.plan(
            desired = listOf(DesiredEvent(today.minusDays(1), "Full Moon", "body")),
            existing = emptyList(),
            today = today,
        )
        assertTrue(plan.isEmpty)
    }

    @Test
    fun `today itself counts as upcoming`() {
        // Tonight's full moon is exactly the one worth showing.
        val plan = CalendarReconcile.plan(
            desired = listOf(DesiredEvent(today, "Full Moon", "body")),
            existing = emptyList(),
            today = today,
        )
        assertEquals(1, plan.insert.size)
    }

    // --- Removal ---

    @Test
    fun `removing everything includes the past`() {
        // Switching the feature off means gone, not archived — the user asked
        // for the entries to be removed.
        val plan = CalendarReconcile.removeAll(
            listOf(
                ExistingEvent(1, today.minusDays(60), "Full Moon", "body"),
                ExistingEvent(2, today.plusDays(9), "New Moon", "body"),
            )
        )

        assertEquals(listOf(1L, 2L), plan.delete)
        assertTrue(plan.insert.isEmpty())
        assertTrue(plan.update.isEmpty())
    }

    @Test
    fun `removing from an empty calendar is a no-op`() {
        assertTrue(CalendarReconcile.removeAll(emptyList()).isEmpty)
    }

    // --- The property that matters most ---

    /**
     * Applying a plan and reconciling again must find nothing left to do.
     * Without this, every sync would churn the calendar and the entries would
     * flicker in and out of the user's other devices.
     */
    @Test
    fun `reconciling is idempotent`() {
        val wanted = listOf(desired(5), desired(20, title = "New Moon"))
        val messy = listOf(
            existing(1, 5, title = "Stale"),        // needs updating
            existing(2, 5),                          // duplicate
            existing(3, 12),                         // not wanted
            ExistingEvent(4, today.minusDays(5), "Old", "old"),  // past, untouchable
        )

        val first = CalendarReconcile.plan(wanted, messy, today)
        val settled = simulate(messy, first)

        val second = CalendarReconcile.plan(wanted, settled, today)
        assertTrue("second run should have nothing to do, got $second", second.isEmpty)
    }

    @Test
    fun `a plan never touches the same id twice`() {
        // Deleting and updating one row in the same batch would be a race.
        val plan = CalendarReconcile.plan(
            desired = listOf(desired(5), desired(6)),
            existing = listOf(existing(1, 5), existing(2, 5), existing(3, 9)),
            today = today,
        )

        val touched = plan.delete + plan.update.map { it.first }
        assertEquals("ids must be distinct", touched.size, touched.toSet().size)
    }

    /** Stands in for the content provider, so a plan can be replayed. */
    private fun simulate(before: List<ExistingEvent>, plan: CalendarPlan): List<ExistingEvent> {
        val updates = plan.update.toMap()
        var nextId = (before.maxOfOrNull { it.id } ?: 0L) + 1

        val kept = before
            .filterNot { it.id in plan.delete }
            .map { row ->
                updates[row.id]?.let {
                    row.copy(date = it.date, title = it.title, description = it.description)
                } ?: row
            }

        val added = plan.insert.map {
            ExistingEvent(nextId++, it.date, it.title, it.description)
        }

        return kept + added
    }
}
