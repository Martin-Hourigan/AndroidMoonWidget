package dev.mahourigan.moonwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class LocationSourceTest {

    private fun roundTrip(source: LocationSource): LocationSource =
        LocationSource.deserialize(LocationSource.serialize(source))

    @Test
    fun `auto survives a round trip`() {
        assertEquals(LocationSource.Auto, roundTrip(LocationSource.Auto))
    }

    @Test
    fun `preset survives a round trip`() {
        val source = LocationSource.Preset("newcastle_au")
        assertEquals(source, roundTrip(source))
    }

    @Test
    fun `custom survives a round trip including negative coordinates`() {
        val source = LocationSource.Custom(
            label = "Shed",
            latitude = -32.9283,
            longitude = 151.7817,
            zoneId = "Australia/Sydney",
        )
        assertEquals(source, roundTrip(source))
    }

    @Test
    fun `pipe characters in a label cannot corrupt the record`() {
        val source = LocationSource.Custom("we|rd|name", 10.0, 20.0)
        val restored = roundTrip(source) as LocationSource.Custom

        assertEquals(10.0, restored.latitude, 1e-9)
        assertEquals(20.0, restored.longitude, 1e-9)
        assertTrue("pipes should be stripped", !restored.label.contains('|'))
    }

    @Test
    fun `unknown or corrupt input falls back to the default preset`() {
        val default = LocationSource.Preset(PresetLocations.DEFAULT_ID)

        assertEquals(default, LocationSource.deserialize(null))
        assertEquals(default, LocationSource.deserialize(""))
        assertEquals(default, LocationSource.deserialize("nonsense"))
        assertEquals(default, LocationSource.deserialize("custom:broken|notanumber|alsobad|"))
    }
}

class PresetLocationsTest {

    @Test
    fun `default preset exists`() {
        assertEquals(PresetLocations.DEFAULT_ID, PresetLocations.default.id)
    }

    @Test
    fun `ids are unique`() {
        val ids = PresetLocations.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every preset has valid coordinates and a real timezone`() {
        PresetLocations.all.forEach { preset ->
            assertTrue(
                "${preset.id} latitude out of range",
                preset.location.latitude in -90.0..90.0,
            )
            assertTrue(
                "${preset.id} longitude out of range",
                preset.location.longitude in -180.0..180.0,
            )
            // Throws if the zone id is not recognised.
            ZoneId.of(preset.zoneId)
        }
    }

    @Test
    fun `search matches city and region, and is case insensitive`() {
        assertTrue(PresetLocations.search("newc").any { it.id == "newcastle_au" })
        assertTrue(PresetLocations.search("NEWC").any { it.id == "newcastle_au" })
        assertTrue(PresetLocations.search("nsw").any { it.id == "sydney" })
        assertEquals(PresetLocations.all.size, PresetLocations.search("").size)
    }
}
