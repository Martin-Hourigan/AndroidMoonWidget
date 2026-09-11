package dev.mahourigan.moonwidget.data

import dev.mahourigan.moonwidget.astronomy.GeoLocation
import java.time.ZoneId

/** How the app decides where the observer is. */
sealed interface LocationSource {

    /** Follow the device's own (coarse) location. */
    data object Auto : LocationSource

    /** One of the bundled cities. */
    data class Preset(val id: String) : LocationSource

    /** Coordinates the user entered and named. */
    data class Custom(
        val label: String,
        val latitude: Double,
        val longitude: Double,
        /** IANA zone id; falls back to the device zone when blank. */
        val zoneId: String = "",
    ) : LocationSource

    companion object {
        /** Compact form for DataStore. */
        fun serialize(source: LocationSource): String = when (source) {
            is Auto -> "auto"
            is Preset -> "preset:${source.id}"
            is Custom -> "custom:${source.label.replace('|', ' ')}|" +
                "${source.latitude}|${source.longitude}|${source.zoneId}"
        }

        fun deserialize(raw: String?): LocationSource {
            if (raw.isNullOrBlank()) return Preset(PresetLocations.DEFAULT_ID)
            return when {
                raw == "auto" -> Auto

                raw.startsWith("preset:") -> Preset(raw.removePrefix("preset:"))

                raw.startsWith("custom:") -> {
                    val parts = raw.removePrefix("custom:").split('|')
                    val latitude = parts.getOrNull(1)?.toDoubleOrNull()
                    val longitude = parts.getOrNull(2)?.toDoubleOrNull()
                    if (latitude == null || longitude == null) {
                        Preset(PresetLocations.DEFAULT_ID)
                    } else {
                        Custom(
                            label = parts.getOrNull(0).orEmpty().ifBlank { "Custom" },
                            latitude = latitude,
                            longitude = longitude,
                            zoneId = parts.getOrNull(3).orEmpty(),
                        )
                    }
                }

                else -> Preset(PresetLocations.DEFAULT_ID)
            }
        }
    }
}

/**
 * A location the app has actually settled on, ready to calculate with.
 *
 * @param isFallback true when this is not what the user asked for — permission
 *   was denied, or no fix has arrived yet. The UI should say so rather than
 *   silently showing times for the wrong place.
 */
data class ResolvedLocation(
    val label: String,
    val geo: GeoLocation,
    val zone: ZoneId,
    val isFallback: Boolean = false,
) {
    companion object {
        fun fromPreset(preset: PresetLocation, isFallback: Boolean = false) = ResolvedLocation(
            label = preset.displayName,
            geo = preset.location,
            zone = ZoneId.of(preset.zoneId),
            isFallback = isFallback,
        )
    }
}
