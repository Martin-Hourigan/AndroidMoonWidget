package dev.mahourigan.moonwidget.domain

import dev.mahourigan.moonwidget.astronomy.GeoLocation
import dev.mahourigan.moonwidget.data.ResolvedLocation
import dev.mahourigan.moonwidget.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** How the two orientation settings combine. */
class OrientationSettingsTest {

    private val at = Instant.parse("2026-08-23T07:00:00Z")

    private fun locationAt(latitude: Double) = ResolvedLocation(
        label = "Test",
        geo = GeoLocation(latitude = latitude, longitude = 151.7817),
        zone = ZoneId.of("Australia/Sydney"),
    )

    private fun appearanceWith(settings: Settings, latitude: Double = -32.9283) =
        MoonSnapshot.compute(locationAt(latitude), at, settings).appearance

    @Test
    fun `true rotation turns the disc`() {
        val appearance = appearanceWith(Settings(trueMoonOrientation = true))
        assertNotEquals(0.0, appearance.rotationDegrees, 0.001)
    }

    @Test
    fun `upright mode leaves the disc unturned`() {
        val appearance = appearanceWith(Settings(trueMoonOrientation = false))
        assertEquals(0.0, appearance.rotationDegrees, 0.0)
    }

    @Test
    fun `upright mode picks the lit side from the hemisphere`() {
        val settings = Settings(trueMoonOrientation = false)
        assertNotEquals(
            appearanceWith(settings, latitude = 51.5).litOnRight,
            appearanceWith(settings, latitude = -32.9).litOnRight,
        )
    }

    /**
     * The behaviour as described: the disc is at the rise angle when it rises
     * and the set angle when it sets, having turned a long way in between.
     */
    @Test
    fun `the angle at moonrise differs from the angle at moonset`() {
        val settings = Settings(trueMoonOrientation = true)
        val snapshot = MoonSnapshot.compute(locationAt(-32.9283), at, settings)

        val rise = snapshot.riseSet.rise
        val set = snapshot.riseSet.set
        // Both crossings fall inside this local day, so neither should be null.
        assertTrue("expected a moonrise on this date", rise != null)
        assertTrue("expected a moonset on this date", set != null)

        val atRise = MoonSnapshot.compute(locationAt(-32.9283), rise!!, settings)
            .appearance.rotationDegrees
        val atSet = MoonSnapshot.compute(locationAt(-32.9283), set!!, settings)
            .appearance.rotationDegrees

        val turned = kotlin.math.abs(
            dev.mahourigan.moonwidget.astronomy.normalizeSignedDegrees(atSet - atRise)
        )
        assertTrue("Expected a big turn between the horizons, got $turned", turned > 30.0)
    }
}
