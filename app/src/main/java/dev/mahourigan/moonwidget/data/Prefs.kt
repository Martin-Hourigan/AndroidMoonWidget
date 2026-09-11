package dev.mahourigan.moonwidget.data

import dev.mahourigan.moonwidget.astronomy.MoonNameSet
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moon_prefs")

/**
 * Persisted settings.
 *
 * The last known good coordinates are cached so a widget refresh never has to
 * wait on a location fix — a slightly stale position is far better than a blank
 * widget, and the Moon barely notices a few kilometres.
 */
class Prefs(private val context: Context) {

    private object Keys {
        val LOCATION_SOURCE = stringPreferencesKey("location_source")
        val CACHED_LATITUDE = doublePreferencesKey("cached_latitude")
        val CACHED_LONGITUDE = doublePreferencesKey("cached_longitude")
        val SAVED_PLACES = stringPreferencesKey("saved_places")
        val MOON_NAME_SET = stringPreferencesKey("moon_name_set")
        val FULL_CUSTOM_LEAD_DAYS = intPreferencesKey("full_custom_lead_days")
        val NEW_CUSTOM_LEAD_DAYS = intPreferencesKey("new_custom_lead_days")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute_of_day")
        val THEME = stringPreferencesKey("theme")
        val APP_FONT = stringPreferencesKey("app_font")
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val SECTION_ORDER = stringPreferencesKey("section_order")

        /** One boolean key per toggle, named from the enum so they stay in step. */
        fun setting(key: SettingKey) = booleanPreferencesKey("setting_${key.name.lowercase()}")

        /** Custom theme colours, one ARGB key per role. */
        fun customColor(role: ThemeRole) = longPreferencesKey("theme_${role.name.lowercase()}")
    }

    /**
     * Current settings, with any unset toggle falling back to its default.
     */
    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        var result = Settings(
            // Not a toggle, so it is stored separately from the boolean keys.
            moonNameSet = MoonNameSet.fromNameOrDefault(prefs[Keys.MOON_NAME_SET]),
            fullMoonCustomLeadDays =
                prefs[Keys.FULL_CUSTOM_LEAD_DAYS] ?: Settings().fullMoonCustomLeadDays,
            newMoonCustomLeadDays =
                prefs[Keys.NEW_CUSTOM_LEAD_DAYS] ?: Settings().newMoonCustomLeadDays,
            notificationMinuteOfDay =
                prefs[Keys.NOTIFICATION_MINUTE] ?: Settings().notificationMinuteOfDay,
            theme = MoonTheme.fromNameOrDefault(prefs[Keys.THEME]),
            appFont = AppFont.fromNameOrDefault(prefs[Keys.APP_FONT]),
            textSize = TextSize.fromNameOrDefault(prefs[Keys.TEXT_SIZE]),
            sectionOrder = AppSection.deserialize(prefs[Keys.SECTION_ORDER]),
            // Any role never edited keeps its seed value.
            customTheme = ThemeRole.entries.fold(ThemeColors.MIDNIGHT) { colors, role ->
                prefs[Keys.customColor(role)]?.let { colors.with(role, it) } ?: colors
            },
        )
        SettingKey.entries.forEach { key ->
            prefs[Keys.setting(key)]?.let { stored -> result = result.with(key, stored) }
        }
        result
    }

    suspend fun currentSettings(): Settings = settings.first()

    suspend fun setSetting(key: SettingKey, value: Boolean) {
        context.dataStore.edit { it[Keys.setting(key)] = value }
    }

    suspend fun setMoonNameSet(set: MoonNameSet) {
        context.dataStore.edit { it[Keys.MOON_NAME_SET] = set.name }
    }

    suspend fun setCustomLeadDays(event: NotifiableEvent, days: Int) {
        val clamped = days.coerceIn(
            NotificationLead.MIN_CUSTOM_DAYS,
            NotificationLead.MAX_CUSTOM_DAYS,
        )
        context.dataStore.edit {
            when (event) {
                NotifiableEvent.FULL_MOON -> it[Keys.FULL_CUSTOM_LEAD_DAYS] = clamped
                NotifiableEvent.NEW_MOON -> it[Keys.NEW_CUSTOM_LEAD_DAYS] = clamped
                NotifiableEvent.MOONRISE -> Unit
            }
        }
    }

    suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { it[Keys.APP_FONT] = font.name }
    }

    suspend fun setTextSize(size: TextSize) {
        context.dataStore.edit { it[Keys.TEXT_SIZE] = size.name }
    }

    suspend fun setSectionOrder(order: List<AppSection>) {
        context.dataStore.edit { it[Keys.SECTION_ORDER] = AppSection.serialize(order) }
    }

    suspend fun setTheme(theme: MoonTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    /**
     * Set one custom colour and switch to the custom theme.
     *
     * Editing a colour is only ever done with the intent of using it, so
     * selecting CUSTOM here saves a second step that would otherwise be easy
     * to forget and look like the picker was broken.
     */
    suspend fun setCustomColor(role: ThemeRole, argb: Long) {
        context.dataStore.edit {
            it[Keys.customColor(role)] = argb
            it[Keys.THEME] = MoonTheme.CUSTOM.name
        }
    }

    /** Reset the custom colours to the seed, leaving the selected theme alone. */
    suspend fun resetCustomTheme() {
        context.dataStore.edit { prefs ->
            ThemeRole.entries.forEach { prefs.remove(Keys.customColor(it)) }
        }
    }

    suspend fun setNotificationMinuteOfDay(minute: Int) {
        context.dataStore.edit { it[Keys.NOTIFICATION_MINUTE] = minute.coerceIn(0, 24 * 60 - 1) }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { prefs ->
            SettingKey.entries.forEach { prefs.remove(Keys.setting(it)) }
            prefs.remove(Keys.MOON_NAME_SET)
            prefs.remove(Keys.FULL_CUSTOM_LEAD_DAYS)
            prefs.remove(Keys.NEW_CUSTOM_LEAD_DAYS)
            prefs.remove(Keys.NOTIFICATION_MINUTE)
            prefs.remove(Keys.THEME)
            prefs.remove(Keys.APP_FONT)
            prefs.remove(Keys.TEXT_SIZE)
            prefs.remove(Keys.SECTION_ORDER)
            ThemeRole.entries.forEach { prefs.remove(Keys.customColor(it)) }
        }
    }

    val locationSource: Flow<LocationSource> = context.dataStore.data.map { prefs ->
        LocationSource.deserialize(prefs[Keys.LOCATION_SOURCE])
    }

    suspend fun currentLocationSource(): LocationSource = locationSource.first()

    suspend fun setLocationSource(source: LocationSource) {
        context.dataStore.edit { it[Keys.LOCATION_SOURCE] = LocationSource.serialize(source) }
    }

    val cachedCoordinates: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        val latitude = prefs[Keys.CACHED_LATITUDE]
        val longitude = prefs[Keys.CACHED_LONGITUDE]
        if (latitude != null && longitude != null) latitude to longitude else null
    }

    suspend fun cacheCoordinates(latitude: Double, longitude: Double) {
        context.dataStore.edit {
            it[Keys.CACHED_LATITUDE] = latitude
            it[Keys.CACHED_LONGITUDE] = longitude
        }
    }

    /** User-saved custom places, newest first. */
    val savedPlaces: Flow<List<LocationSource.Custom>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SAVED_PLACES]
            .orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }
            .mapNotNull { LocationSource.deserialize(it) as? LocationSource.Custom }
    }

    suspend fun savePlace(place: LocationSource.Custom) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.SAVED_PLACES].orEmpty()
                .split('\n')
                .filter { it.isNotBlank() }
                .filterNot { it.contains("custom:${place.label}|") }
            prefs[Keys.SAVED_PLACES] =
                (listOf(LocationSource.serialize(place)) + existing).joinToString("\n")
        }
    }

    suspend fun removePlace(label: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_PLACES] = prefs[Keys.SAVED_PLACES].orEmpty()
                .split('\n')
                .filter { it.isNotBlank() && !it.contains("custom:$label|") }
                .joinToString("\n")
        }
    }
}
