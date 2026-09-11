package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.astronomy.MoonNameSet
import dev.mahourigan.moonwidget.calendar.CalendarSyncWorker
import dev.mahourigan.moonwidget.calendar.MoonCalendarSync
import dev.mahourigan.moonwidget.data.LocationRepository
import dev.mahourigan.moonwidget.data.AppFont
import dev.mahourigan.moonwidget.data.AppSection
import dev.mahourigan.moonwidget.data.MoonTheme
import dev.mahourigan.moonwidget.data.TextSize
import dev.mahourigan.moonwidget.data.ThemeRole
import dev.mahourigan.moonwidget.data.NotifiableEvent
import dev.mahourigan.moonwidget.data.LocationSource
import dev.mahourigan.moonwidget.data.Prefs
import dev.mahourigan.moonwidget.data.PresetLocations
import dev.mahourigan.moonwidget.data.ResolvedLocation
import dev.mahourigan.moonwidget.data.SettingKey
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.data.affectsCalendar
import dev.mahourigan.moonwidget.data.affectsSnapshot
import dev.mahourigan.moonwidget.data.needsNotificationPermission
import dev.mahourigan.moonwidget.domain.MoonSnapshot
import dev.mahourigan.moonwidget.notification.MoonEventWorker
import dev.mahourigan.moonwidget.notification.MoonNotifications
import dev.mahourigan.moonwidget.widget.MoonWidget
import android.app.Application
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MoonUiState(
    val snapshot: MoonSnapshot? = null,
    val source: LocationSource = LocationSource.Preset(""),
    val savedPlaces: List<LocationSource.Custom> = emptyList(),
    val settings: Settings = Settings(),
    val isLoading: Boolean = true,
    /** Set when a snapshot could not be produced at all. */
    val failed: Boolean = false,
)

class MoonViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val locations = LocationRepository(application, prefs)

    private val _state = MutableStateFlow(MoonUiState())
    val state: StateFlow<MoonUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            prefs.savedPlaces.collect { places ->
                _state.value = _state.value.copy(savedPlaces = places)
            }
        }
        viewModelScope.launch {
            prefs.settings.collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, failed = false)

            // Same tiered fallback as the widget: a bad saved location should
            // not leave the screen permanently empty.
            val current = runCatching { prefs.currentSettings() }.getOrDefault(Settings())

            val snapshot = withContext(Dispatchers.Default) {
                runCatching {
                    MoonSnapshot.compute(locations.resolve(), settings = current)
                }
                    .recoverCatching {
                        Log.w(TAG, "Falling back to the default location", it)
                        MoonSnapshot.compute(
                            ResolvedLocation.fromPreset(PresetLocations.default, isFallback = true),
                            settings = current,
                        )
                    }
                    .onFailure { Log.e(TAG, "Could not compute a moon snapshot", it) }
                    .getOrNull()
            }

            _state.value = _state.value.copy(
                snapshot = snapshot,
                source = runCatching { prefs.currentLocationSource() }
                    .getOrDefault(LocationSource.Preset(PresetLocations.DEFAULT_ID)),
                isLoading = false,
                failed = snapshot == null,
            )
        }
    }

    /** Change location, recompute, and push the new values out to any widgets. */
    fun selectLocation(source: LocationSource) {
        viewModelScope.launch {
            prefs.setLocationSource(source)
            refresh()
            // Moon times are location-dependent, and a big enough move shifts
            // an entry onto a different day — so the calendar has to follow.
            if (prefs.currentSettings().calendarSyncEnabled) syncCalendar()
            MoonWidget().updateAll(getApplication())
        }
    }

    fun saveCustomPlace(place: LocationSource.Custom) {
        viewModelScope.launch {
            prefs.savePlace(place)
            selectLocation(place)
        }
    }

    fun deleteCustomPlace(label: String) {
        viewModelScope.launch { prefs.removePlace(label) }
    }

    fun hasLocationPermission(): Boolean = locations.hasLocationPermission()

    fun hasNotificationPermission(): Boolean =
        MoonNotifications.hasPermission(getApplication())

    /**
     * Persist a toggle, then apply its consequences: redraw the widget, and
     * bring notification scheduling in line with the new state.
     */
    fun setSetting(key: SettingKey, value: Boolean) {
        viewModelScope.launch {
            prefs.setSetting(key, value)

            if (key.affectsSnapshot) refresh()
            if (key.needsNotificationPermission) {
                runCatching { MoonEventWorker.sync(getApplication()) }
                    .onFailure { Log.e(TAG, "Could not sync notification schedule", it) }
            }
            if (key.affectsCalendar) syncCalendar()

            MoonWidget().updateAll(getApplication())
        }
    }

    /**
     * Rewrite the calendar entries, and match the repeating job to the setting.
     *
     * Runs off the main thread: it is a provider query plus a batch write, and
     * on a full first run that is around fifty rows.
     */
    private suspend fun syncCalendar() {
        runCatching {
            withContext(Dispatchers.IO) { MoonCalendarSync.sync(getApplication()) }
            CalendarSyncWorker.sync(getApplication())
        }.onFailure { Log.e(TAG, "Could not sync the calendar", it) }
    }

    fun hasCalendarPermission(): Boolean = MoonCalendarSync.hasPermission(getApplication())

    /** Called once the system calendar-permission dialog has been answered. */
    fun onCalendarPermissionResult(granted: Boolean, key: SettingKey) {
        if (granted) setSetting(key, true)
    }

    /** Not a toggle, so it has its own setter; still recomputes and redraws. */
    fun selectMoonNameSet(set: MoonNameSet) {
        viewModelScope.launch {
            prefs.setMoonNameSet(set)
            refresh()
            MoonWidget().updateAll(getApplication())
        }
    }

    /**
     * Theme changes need no recompute — the snapshot holds no colours — but the
     * widget draws its Moon to a bitmap, so it has to be told to redraw.
     */
    fun selectTheme(theme: MoonTheme) {
        viewModelScope.launch {
            prefs.setTheme(theme)
            MoonWidget().updateAll(getApplication())
        }
    }

    /** Presentation only, but the widget draws text too, so it redraws as well. */
    fun selectFont(font: AppFont) {
        viewModelScope.launch {
            prefs.setAppFont(font)
            MoonWidget().updateAll(getApplication())
        }
    }

    fun selectTextSize(size: TextSize) {
        viewModelScope.launch {
            prefs.setTextSize(size)
            MoonWidget().updateAll(getApplication())
        }
    }

    /** Main-screen layout only; the widget has its own fixed arrangement. */
    fun setSectionOrder(order: List<AppSection>) {
        viewModelScope.launch { prefs.setSectionOrder(order) }
    }

    fun setCustomColor(role: ThemeRole, argb: Long) {
        viewModelScope.launch {
            prefs.setCustomColor(role, argb)
            MoonWidget().updateAll(getApplication())
        }
    }

    fun resetCustomTheme() {
        viewModelScope.launch {
            prefs.resetCustomTheme()
            MoonWidget().updateAll(getApplication())
        }
    }

    /** Both of these change when reminders fire, so re-sync the schedule. */
    fun setCustomLeadDays(event: NotifiableEvent, days: Int) {
        viewModelScope.launch {
            prefs.setCustomLeadDays(event, days)
            runCatching { MoonEventWorker.sync(getApplication()) }
                .onFailure { Log.e(TAG, "Could not sync after lead change", it) }
        }
    }

    fun setNotificationMinuteOfDay(minute: Int) {
        viewModelScope.launch {
            prefs.setNotificationMinuteOfDay(minute)
            runCatching { MoonEventWorker.sync(getApplication()) }
                .onFailure { Log.e(TAG, "Could not sync after time change", it) }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            prefs.resetSettings()
            refresh()
            runCatching { MoonEventWorker.sync(getApplication()) }
            // Reset returns the calendar setting to off, so this clears out
            // every entry the app had written.
            syncCalendar()
            MoonWidget().updateAll(getApplication())
        }
    }

    /** Called once the system permission dialog has been answered. */
    fun onNotificationPermissionResult(granted: Boolean, key: SettingKey) {
        if (granted) setSetting(key, true)
    }

    private companion object {
        const val TAG = "MoonViewModel"
    }
}
