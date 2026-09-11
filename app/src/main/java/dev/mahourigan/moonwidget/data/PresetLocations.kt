package dev.mahourigan.moonwidget.data

import dev.mahourigan.moonwidget.astronomy.GeoLocation

/**
 * A named place the user can pick without granting location permission.
 *
 * @param id stable key stored in preferences — never change these once shipped.
 * @param zoneId IANA timezone, used to decide which local day rise/set applies to.
 */
data class PresetLocation(
    val id: String,
    val name: String,
    val region: String,
    val location: GeoLocation,
    val zoneId: String,
) {
    val displayName: String get() = "$name, $region"
}

object PresetLocations {

    /** Used when nothing has been chosen and no fix is available. */
    const val DEFAULT_ID = "newcastle_au"

    val all: List<PresetLocation> = listOf(
        // Australia
        PresetLocation("newcastle_au", "Newcastle", "NSW", GeoLocation(-32.9283, 151.7817), "Australia/Sydney"),
        PresetLocation("sydney", "Sydney", "NSW", GeoLocation(-33.8688, 151.2093), "Australia/Sydney"),
        PresetLocation("melbourne", "Melbourne", "VIC", GeoLocation(-37.8136, 144.9631), "Australia/Melbourne"),
        PresetLocation("brisbane", "Brisbane", "QLD", GeoLocation(-27.4698, 153.0251), "Australia/Brisbane"),
        PresetLocation("perth", "Perth", "WA", GeoLocation(-31.9523, 115.8613), "Australia/Perth"),
        PresetLocation("adelaide", "Adelaide", "SA", GeoLocation(-34.9285, 138.6007), "Australia/Adelaide"),
        PresetLocation("hobart", "Hobart", "TAS", GeoLocation(-42.8821, 147.3272), "Australia/Hobart"),
        PresetLocation("canberra", "Canberra", "ACT", GeoLocation(-35.2809, 149.1300), "Australia/Sydney"),
        PresetLocation("darwin", "Darwin", "NT", GeoLocation(-12.4634, 130.8456), "Australia/Darwin"),

        // Nearby
        PresetLocation("auckland", "Auckland", "New Zealand", GeoLocation(-36.8485, 174.7633), "Pacific/Auckland"),
        PresetLocation("wellington", "Wellington", "New Zealand", GeoLocation(-41.2865, 174.7762), "Pacific/Auckland"),
        PresetLocation("singapore", "Singapore", "Singapore", GeoLocation(1.3521, 103.8198), "Asia/Singapore"),
        PresetLocation("tokyo", "Tokyo", "Japan", GeoLocation(35.6762, 139.6503), "Asia/Tokyo"),

        // Rest of world
        PresetLocation("london", "London", "United Kingdom", GeoLocation(51.5074, -0.1278), "Europe/London"),
        PresetLocation("dublin", "Dublin", "Ireland", GeoLocation(53.3498, -6.2603), "Europe/Dublin"),
        PresetLocation("paris", "Paris", "France", GeoLocation(48.8566, 2.3522), "Europe/Paris"),
        PresetLocation("berlin", "Berlin", "Germany", GeoLocation(52.5200, 13.4050), "Europe/Berlin"),
        PresetLocation("new_york", "New York", "USA", GeoLocation(40.7128, -74.0060), "America/New_York"),
        PresetLocation("los_angeles", "Los Angeles", "USA", GeoLocation(34.0522, -118.2437), "America/Los_Angeles"),
        PresetLocation("toronto", "Toronto", "Canada", GeoLocation(43.6532, -79.3832), "America/Toronto"),
        PresetLocation("cape_town", "Cape Town", "South Africa", GeoLocation(-33.9249, 18.4241), "Africa/Johannesburg"),
        PresetLocation("mumbai", "Mumbai", "India", GeoLocation(19.0760, 72.8777), "Asia/Kolkata"),
        PresetLocation("dubai", "Dubai", "UAE", GeoLocation(25.2048, 55.2708), "Asia/Dubai"),
        PresetLocation("reykjavik", "Reykjavik", "Iceland", GeoLocation(64.1466, -21.9426), "Atlantic/Reykjavik"),
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): PresetLocation? = byId[id]

    val default: PresetLocation get() = byId.getValue(DEFAULT_ID)

    /** Case-insensitive match on city or region, for the picker's search field. */
    fun search(query: String): List<PresetLocation> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        return all.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.region.contains(trimmed, ignoreCase = true)
        }
    }
}
