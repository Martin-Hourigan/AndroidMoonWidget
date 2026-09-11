package dev.mahourigan.moonwidget.data

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.astronomy.GeoLocation
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.ZoneId
import kotlin.coroutines.resume

/**
 * Turns the user's chosen [LocationSource] into concrete coordinates.
 *
 * Never throws and never returns null: if everything else fails it falls back
 * to the default preset and flags the result, so the widget always has
 * something sensible to draw.
 */
class LocationRepository(
    private val context: Context,
    private val prefs: Prefs,
) {

    suspend fun resolve(): ResolvedLocation =
        when (val source = prefs.currentLocationSource()) {
            is LocationSource.Preset ->
                ResolvedLocation.fromPreset(
                    PresetLocations.byId(source.id) ?: PresetLocations.default,
                    isFallback = PresetLocations.byId(source.id) == null,
                )

            is LocationSource.Custom -> ResolvedLocation(
                label = source.label,
                geo = GeoLocation(source.latitude, source.longitude),
                zone = source.zoneId.toZoneIdOrDefault(),
            )

            is LocationSource.Auto -> resolveAutomatic()
        }

    private suspend fun resolveAutomatic(): ResolvedLocation {
        if (hasLocationPermission()) {
            currentCoordinates()?.let { (latitude, longitude) ->
                prefs.cacheCoordinates(latitude, longitude)
                return ResolvedLocation(
                    label = context.getString(R.string.location_current),
                    geo = GeoLocation(latitude, longitude),
                    zone = ZoneId.systemDefault(),
                )
            }
        }

        // No permission, or no fix yet — reuse the last good position if we have one.
        prefs.cachedCoordinates.first()?.let { (latitude, longitude) ->
            return ResolvedLocation(
                label = context.getString(R.string.location_last_known),
                geo = GeoLocation(latitude, longitude),
                zone = ZoneId.systemDefault(),
                isFallback = true,
            )
        }

        return ResolvedLocation.fromPreset(PresetLocations.default, isFallback = true)
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission()
    private suspend fun currentCoordinates(): Pair<Double, Double>? =
        suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    continuation.resume(location?.let { it.latitude to it.longitude })
                }
                .addOnFailureListener { continuation.resume(null) }
                .addOnCanceledListener { continuation.resume(null) }
        }

    private fun String.toZoneIdOrDefault(): ZoneId = runCatching {
        if (isBlank()) ZoneId.systemDefault() else ZoneId.of(this)
    }.getOrDefault(ZoneId.systemDefault())
}
