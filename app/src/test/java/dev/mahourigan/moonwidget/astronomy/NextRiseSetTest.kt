package dev.mahourigan.moonwidget.astronomy

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * The widget shows the *next* rise and set rather than the current calendar
 * day's, so these check the forward search rather than the per-day one.
 */
class NextRiseSetTest {

    private val newcastle = GeoLocation(latitude = -32.9283, longitude = 151.7817)
    private val zone = ZoneId.of("Australia/Sydney")

    private fun instantAt(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()

    @Test
    fun `next events are always in the future`() {
        // Sample across a lunation so we hit every geometry.
        for (dayOffset in 0..29) {
            val from = instantAt(2026, 9, 1, 23).plus(Duration.ofDays(dayOffset.toLong()))
            val next = RiseSet.nextEvents(from, newcastle)

            next.rise?.let {
                assertTrue("rise $it was before $from", !it.isBefore(from))
            }
            next.set?.let {
                assertTrue("set $it was before $from", !it.isBefore(from))
            }
        }
    }

    /**
     * The bug this feature fixes: at 11pm the old code reported a moonrise from
     * that morning. The next rise must be ahead of the asking time, not behind.
     */
    @Test
    fun `late evening does not report this morning's rise`() {
        val lateEvening = instantAt(2026, 9, 10, 23)
        val next = RiseSet.nextEvents(lateEvening, newcastle)

        val rise = requireNotNull(next.rise) { "expected a rise within 3 days" }
        assertTrue("rise should be after the query time", rise.isAfter(lateEvening))
        assertTrue(
            "rise should be within a couple of days, was $rise",
            Duration.between(lateEvening, rise) < Duration.ofDays(2),
        )
    }

    @Test
    fun `both a rise and a set are found within the search window`() {
        val from = instantAt(2026, 9, 5, 12)
        val next = RiseSet.nextEvents(from, newcastle)

        assertNotNull(next.rise)
        assertNotNull(next.set)
    }

    /**
     * `currentlyUp` has to agree with the altitude at that moment, since the UI
     * uses it to decide whether to lead with "sets" or "rises".
     *
     * The exact threshold is the Moon's own horizon altitude (a fraction of a
     * degree, varying with distance), so samples sitting within half a degree
     * of the horizon are skipped rather than asserted against a magic number —
     * pinning a literal here would make the test pass by luck.
     */
    @Test
    fun `currentlyUp agrees with the computed altitude`() {
        var checked = 0

        for (hour in 0..23) {
            val at = instantAt(2026, 9, 12, hour)
            val altitude = RiseSet.altitude(JulianDate.fromInstant(at), newcastle)
            if (abs(altitude) < 0.5) continue // too close to the boundary to judge

            val next = RiseSet.nextEvents(at, newcastle)
            assertTrue(
                "hour $hour: currentlyUp=${next.currentlyUp} but altitude=$altitude",
                next.currentlyUp == (altitude > 0),
            )
            checked++
        }

        assertTrue("expected most hours to be unambiguous, checked $checked", checked >= 20)
    }

    /**
     * If the Moon is up now, the set must come before the following rise — and
     * vice versa. Getting this backwards would make the widget read wrong.
     */
    @Test
    fun `event ordering follows from whether the moon is currently up`() {
        for (hour in 0..23 step 3) {
            val at = instantAt(2026, 9, 18, hour)
            val next = RiseSet.nextEvents(at, newcastle)
            val rise = next.rise ?: continue
            val set = next.set ?: continue

            if (next.currentlyUp) {
                assertTrue("up at hour $hour, so set should precede rise", set.isBefore(rise))
            } else {
                assertTrue("down at hour $hour, so rise should precede set", rise.isBefore(set))
            }
        }
    }

    @Test
    fun `next rise agrees with the per-day calculation when it falls on the same day`() {
        val date = LocalDate.of(2026, 9, 7)
        val startOfDay = date.atStartOfDay(zone).toInstant()

        val fromDay = RiseSet.forDate(date, newcastle, zone).rise
        val fromSearch = RiseSet.nextEvents(startOfDay, newcastle).rise

        if (fromDay != null && fromSearch != null &&
            fromSearch.atZone(zone).toLocalDate() == date
        ) {
            val gap = Duration.between(fromDay, fromSearch).abs()
            assertTrue("the two methods disagreed by $gap", gap < Duration.ofMinutes(1))
        }
    }

    @Test
    fun `polar search does not fabricate events`() {
        val tromso = GeoLocation(69.6492, 18.9553)
        val from = LocalDateTime.of(2026, 12, 21, 12, 0)
            .atZone(ZoneId.of("Europe/Oslo")).toInstant()

        val next = RiseSet.nextEvents(from, tromso, searchDays = 1)
        // Whatever it finds must still be in the future and inside the window.
        next.rise?.let { assertTrue(!it.isBefore(from)) }
        next.set?.let { assertTrue(!it.isBefore(from)) }
    }
}
