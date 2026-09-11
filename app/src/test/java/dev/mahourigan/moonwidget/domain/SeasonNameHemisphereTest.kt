package dev.mahourigan.moonwidget.domain

import dev.mahourigan.moonwidget.astronomy.GeoLocation
import dev.mahourigan.moonwidget.data.ResolvedLocation
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.astronomy.MoonSlot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The season names are chosen from the observer's hemisphere, with a setting
 * that only *inverts* that choice. Easy to get backwards, so it is pinned here.
 */
class SeasonNameHemisphereTest {

    private val newcastle = ResolvedLocation(
        label = "Newcastle",
        geo = GeoLocation(-32.9283, 151.7817),
        zone = ZoneId.of("Australia/Sydney"),
    )

    private val london = ResolvedLocation(
        label = "London",
        geo = GeoLocation(51.5074, -0.1278),
        zone = ZoneId.of("Europe/London"),
    )

    /** Late December, so the next full moon lands in the January slot. */
    private val beforeAJanuaryFullMoon =
        LocalDateTime.of(2025, 12, 26, 12, 0).atZone(ZoneId.of("UTC")).toInstant()

    private fun nameAt(location: ResolvedLocation, swap: Boolean): MoonSlot? =
        MoonSnapshot.compute(
            location = location,
            at = beforeAJanuaryFullMoon,
            settings = Settings(showNamedMoons = true, swapSeasonNames = swap),
        ).nextFullMoonSlot

    @Test
    fun `northern location gets the traditional northern name`() {
        assertEquals(MoonSlot.JANUARY, nameAt(london, swap = false))
    }

    /**
     * A January full moon is midsummer in Newcastle, so the default is the
     * shifted name rather than the northern Wolf Moon.
     */
    @Test
    fun `southern location gets the shifted name by default`() {
        assertEquals(MoonSlot.JULY, nameAt(newcastle, swap = false))
    }

    @Test
    fun `the override inverts whichever hemisphere you are in`() {
        assertEquals(MoonSlot.JULY, nameAt(london, swap = true))
        assertEquals(MoonSlot.JANUARY, nameAt(newcastle, swap = true))
    }

    /** Names are only computed when the feature is switched on. */
    @Test
    fun `no name when named moons are off`() {
        val snapshot = MoonSnapshot.compute(
            location = newcastle,
            at = beforeAJanuaryFullMoon,
            settings = Settings(showNamedMoons = false),
        )
        assertEquals(null, snapshot.nextFullMoonSlot)
    }

    /**
     * Swapping is a pure inversion: the two hemispheres should always disagree
     * for the same instant, and each should agree with the other once swapped.
     */
    @Test
    fun `hemispheres disagree and swapping reconciles them`() {
        assertEquals(nameAt(london, swap = false), nameAt(newcastle, swap = true))
        assertEquals(nameAt(newcastle, swap = false), nameAt(london, swap = true))
    }
}
