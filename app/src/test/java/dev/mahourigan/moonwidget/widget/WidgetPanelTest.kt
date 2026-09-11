package dev.mahourigan.moonwidget.widget

import dev.mahourigan.moonwidget.data.PresetLocations
import dev.mahourigan.moonwidget.data.ResolvedLocation
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.domain.MoonSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WidgetPanelTest {

    private val location = ResolvedLocation.fromPreset(PresetLocations.default)
    private val now = Instant.parse("2026-09-02T09:00:00Z")

    private fun snapshot(settings: Settings) =
        MoonSnapshot.compute(location, at = now, settings = settings)

    private val bothOn = Settings(widgetShowSkyPath = true, widgetShowDirection = true)

    // --- Which views are offered ---

    @Test
    fun `all three appear when both toggles are on`() {
        val available = WidgetPanel.available(snapshot(bothOn), bothOn)
        assertEquals(
            listOf(WidgetPanel.SKY_PATH, WidgetPanel.NEXT_EVENT, WidgetPanel.COMPASS),
            available,
        )
    }

    @Test
    fun `switching the compass off drops it from the cycle`() {
        val settings = bothOn.copy(widgetShowDirection = false)
        val available = WidgetPanel.available(snapshot(settings), settings)

        assertTrue(WidgetPanel.COMPASS !in available)
        assertTrue(WidgetPanel.SKY_PATH in available)
    }

    @Test
    fun `switching the sky path off drops the dome but keeps the rest`() {
        val settings = bothOn.copy(widgetShowSkyPath = false)
        val available = WidgetPanel.available(snapshot(settings), settings)

        assertTrue(WidgetPanel.SKY_PATH !in available)
        assertEquals(listOf(WidgetPanel.NEXT_EVENT, WidgetPanel.COMPASS), available)
    }

    @Test
    fun `the panel is hidden entirely when both toggles are off`() {
        val settings = Settings(widgetShowSkyPath = false, widgetShowDirection = false)
        // The widget checks this before ever building the list.
        assertTrue(!settings.widgetPanelEnabled)
    }

    @Test
    fun `either toggle alone is enough to show the panel`() {
        assertTrue(Settings(widgetShowSkyPath = true, widgetShowDirection = false).widgetPanelEnabled)
        assertTrue(Settings(widgetShowSkyPath = false, widgetShowDirection = true).widgetPanelEnabled)
    }

    // --- Cycling ---

    @Test
    fun `tapping walks through every view and comes back round`() {
        val available = WidgetPanel.available(snapshot(bothOn), bothOn)
        val seen = (0..available.size).map { WidgetPanel.at(it, available) }

        assertEquals(available, seen.take(available.size))
        assertEquals("should wrap to the start", seen.first(), seen.last())
    }

    @Test
    fun `the index only ever counts up, so it grows past the list`() {
        val available = listOf(WidgetPanel.SKY_PATH, WidgetPanel.NEXT_EVENT)

        // The stored index is never reset — the modulo is what keeps it sane.
        assertEquals(WidgetPanel.SKY_PATH, WidgetPanel.at(0, available))
        assertEquals(WidgetPanel.NEXT_EVENT, WidgetPanel.at(1, available))
        assertEquals(WidgetPanel.SKY_PATH, WidgetPanel.at(2, available))
        assertEquals(WidgetPanel.NEXT_EVENT, WidgetPanel.at(9999, available))
    }

    /**
     * The case that would otherwise crash: sitting on the compass, then turning
     * it off. The stored index now points past the end of a shorter list.
     */
    @Test
    fun `a stale index left by a removed view lands somewhere valid`() {
        val before = listOf(WidgetPanel.SKY_PATH, WidgetPanel.NEXT_EVENT, WidgetPanel.COMPASS)
        val after = listOf(WidgetPanel.SKY_PATH, WidgetPanel.NEXT_EVENT)

        val onCompass = 2
        assertEquals(WidgetPanel.COMPASS, WidgetPanel.at(onCompass, before))
        assertEquals(WidgetPanel.SKY_PATH, WidgetPanel.at(onCompass, after))
    }

    @Test
    fun `a negative index would still resolve`() {
        // Nothing writes one, but floorMod means a corrupted store cannot
        // bring the widget down.
        val available = listOf(WidgetPanel.SKY_PATH, WidgetPanel.NEXT_EVENT)
        assertEquals(WidgetPanel.NEXT_EVENT, WidgetPanel.at(-1, available))
        assertEquals(WidgetPanel.SKY_PATH, WidgetPanel.at(-2, available))
    }

    @Test
    fun `nothing available means nothing drawn`() {
        assertNull(WidgetPanel.at(0, emptyList()))
        assertNull(WidgetPanel.at(7, emptyList()))
    }

    @Test
    fun `a single view is stable however often it is tapped`() {
        val available = listOf(WidgetPanel.NEXT_EVENT)
        (0..5).forEach { assertEquals(WidgetPanel.NEXT_EVENT, WidgetPanel.at(it, available)) }
    }

    // --- The bearing the compass uses ---

    @Test
    fun `the bearing follows whether the Moon is up`() {
        val snapshot = snapshot(bothOn)
        val bearing = WidgetPanel.bearingOf(snapshot)

        // Whichever it is, it has to be a real compass bearing.
        assertTrue("got $bearing", bearing != null && bearing in 0.0..360.0)
        assertEquals(
            if (snapshot.isUp) snapshot.position?.azimuth else snapshot.riseAzimuth,
            bearing,
        )
    }

    @Test
    fun `no bearing is computed when nothing on the widget asks for one`() {
        // The snapshot skips the work entirely, so the compass must not be
        // offered — it would have nothing to point at.
        val settings = Settings(widgetShowSkyPath = true, widgetShowDirection = false)
            .copy(showCompassDirection = false)
        val snapshot = snapshot(settings)

        assertNull(WidgetPanel.bearingOf(snapshot))
        assertTrue(WidgetPanel.COMPASS !in WidgetPanel.available(snapshot, settings))
    }
}
