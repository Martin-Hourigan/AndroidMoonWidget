package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.astronomy.MoonSlot
import dev.mahourigan.moonwidget.data.NotifiableEvent
import dev.mahourigan.moonwidget.data.NotificationLead
import dev.mahourigan.moonwidget.data.customLeadDaysFor
import dev.mahourigan.moonwidget.data.SettingKey
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.data.needsCalendarPermission
import dev.mahourigan.moonwidget.data.needsNotificationPermission
import dev.mahourigan.moonwidget.data.valueOf
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Which row each row hangs off, derived from the declarations below.
 *
 * Dependencies now run two deep — the blended-glyph choice sits under the
 * glyph, which sits under the Moon itself — and checking only the immediate
 * parent would leave a grandchild on screen with its grandparent switched off.
 */
private val PARENT_OF: Map<SettingKey, SettingKey> by lazy {
    SECTIONS.asSequence()
        .flatMap { it.rows.asSequence() }
        .mapNotNull { row -> row.dependsOn?.let { row.key to it } }
        .toMap()
}

/** True when every setting this one hangs off is switched on. */
private fun Settings.ancestorsOn(key: SettingKey): Boolean {
    var parent = PARENT_OF[key]
    while (parent != null) {
        if (!valueOf(parent)) return false
        parent = PARENT_OF[parent]
    }
    return true
}

/** One row in the settings list. */
private data class ToggleRow(
    val key: SettingKey,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    /**
     * Only shown while this parent setting is on, and drawn indented beneath it.
     * Keeps the dependent choices hidden until they are relevant.
     */
    val dependsOn: SettingKey? = null,
    /**
     * Whether to indent. Defaults to whether the row has a parent, but a row
     * whose parent lives in another section would otherwise look nested under
     * whatever happens to sit above it.
     */
    val indented: Boolean? = null,
    /** Small heading rendered above this row, to group sibling children. */
    @StringRes val groupHeading: Int? = null,
    /**
     * Rows sharing an id form one collapsible group. The group stays folded
     * until tapped, so switching on three notifications does not drop fifteen
     * rows onto the screen at once.
     */
    val groupId: String? = null,
    /**
     * Set for the monthly-name rows. The title then shows whatever the chosen
     * [MoonNameSet] calls that slot, with the month as the subtitle.
     */
    val namesSlot: MoonSlot? = null,
)

private data class ToggleSection(
    @StringRes val title: Int,
    val rows: List<ToggleRow>,
)

/**
 * Built from a declared list rather than hand-written rows, so adding a setting
 * means adding one entry here and one field in [Settings] — the compiler's
 * exhaustive `when` on [SettingKey] catches anything left unwired.
 */
private val SECTIONS = listOf(
    ToggleSection(
        R.string.settings_section_widget,
        listOf(
            ToggleRow(
                SettingKey.WIDGET_MOON_IMAGE,
                R.string.setting_widget_moon_image,
                R.string.setting_widget_moon_image_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_SIGN_ON_MOON,
                R.string.setting_widget_sign_on_moon,
                R.string.setting_widget_sign_on_moon_sub,
                dependsOn = SettingKey.WIDGET_MOON_IMAGE,
            ),
            ToggleRow(
                SettingKey.WIDGET_PHASE_NAME,
                R.string.setting_widget_phase_name,
                R.string.setting_widget_phase_name_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_ILLUMINATION,
                R.string.setting_widget_illumination,
                R.string.setting_widget_illumination_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_RISE_SET,
                R.string.setting_widget_rise_set,
                R.string.setting_widget_rise_set_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_FULL_MOON_COUNTDOWN,
                R.string.setting_widget_countdown,
                R.string.setting_widget_countdown_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_MOON_SIGN,
                R.string.setting_widget_moon_sign,
                R.string.setting_widget_moon_sign_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_SKY_PATH,
                R.string.setting_widget_sky_path,
                R.string.setting_widget_sky_path_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_DIRECTION,
                R.string.setting_widget_direction,
                R.string.setting_widget_direction_sub,
            ),
            ToggleRow(
                SettingKey.WIDGET_COMPACT_ILLUMINATION,
                R.string.setting_widget_compact_illumination,
                R.string.setting_widget_compact_illumination_sub,
            ),
        ),
    ),
    ToggleSection(
        R.string.settings_section_app,
        listOf(
            ToggleRow(
                SettingKey.SHOW_MOON_IMAGE,
                R.string.setting_show_moon_image,
                R.string.setting_show_moon_image_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_SIGN_ON_MOON,
                R.string.setting_show_sign_on_moon,
                R.string.setting_show_sign_on_moon_sub,
                dependsOn = SettingKey.SHOW_MOON_IMAGE,
            ),
            ToggleRow(
                SettingKey.SIGN_ON_MOON_BLENDED,
                R.string.setting_sign_blended,
                R.string.setting_sign_blended_sub,
                dependsOn = SettingKey.SHOW_SIGN_ON_MOON,
            ),
            ToggleRow(
                SettingKey.SHOW_PHASE_NAME,
                R.string.setting_show_phase_name,
                R.string.setting_show_phase_name_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_ILLUMINATION,
                R.string.setting_show_illumination,
                R.string.setting_show_illumination_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_RISE_SET,
                R.string.setting_show_rise_set,
                R.string.setting_show_rise_set_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_COMPASS_DIRECTION,
                R.string.setting_show_direction,
                R.string.setting_show_direction_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_HORIZON_STATUS,
                R.string.setting_show_horizon_status,
                R.string.setting_show_horizon_status_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_NEXT_FULL_MOON,
                R.string.setting_show_next_full_moon,
                R.string.setting_show_next_full_moon_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_MOON_SIGN_SECTION,
                R.string.setting_show_moon_sign,
                R.string.setting_show_moon_sign_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_SUN_SIGN_SECTION,
                R.string.setting_show_sun_sign,
                R.string.setting_show_sun_sign_sub,
            ),
            ToggleRow(
                SettingKey.SUN_SIGN_TROPICAL,
                R.string.setting_sun_sign_tropical,
                R.string.setting_sun_sign_tropical_sub,
                dependsOn = SettingKey.SHOW_SUN_SIGN_SECTION,
            ),
            ToggleRow(
                SettingKey.SUN_SIGN_ASTRONOMICAL,
                R.string.setting_sun_sign_astronomical,
                R.string.setting_sun_sign_astronomical_sub,
                dependsOn = SettingKey.SHOW_SUN_SIGN_SECTION,
            ),
            ToggleRow(
                SettingKey.SHOW_DISTANCE,
                R.string.setting_show_distance,
                R.string.setting_show_distance_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_AGE,
                R.string.setting_show_age,
                R.string.setting_show_age_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_NEXT_NEW_MOON,
                R.string.setting_show_next_new_moon,
                R.string.setting_show_next_new_moon_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_SKY_PATH,
                R.string.setting_show_sky_path,
                R.string.setting_show_sky_path_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_PHASE_EVENT_TIME,
                R.string.setting_show_phase_event_time,
                R.string.setting_show_phase_event_time_sub,
            ),
            ToggleRow(
                SettingKey.PHASE_TIME_FULL_MOON,
                R.string.setting_phase_time_full_moon,
                R.string.setting_phase_time_full_moon_sub,
                dependsOn = SettingKey.SHOW_PHASE_EVENT_TIME,
                groupHeading = R.string.settings_group_which_moons,
            ),
            ToggleRow(
                SettingKey.PHASE_TIME_NEW_MOON,
                R.string.setting_phase_time_new_moon,
                R.string.setting_phase_time_new_moon_sub,
                dependsOn = SettingKey.SHOW_PHASE_EVENT_TIME,
            ),
            ToggleRow(
                SettingKey.PHASE_TIME_RISE,
                R.string.setting_phase_time_rise,
                R.string.setting_phase_time_rise_sub,
                dependsOn = SettingKey.SHOW_PHASE_EVENT_TIME,
                groupHeading = R.string.settings_group_which_time,
            ),
            ToggleRow(
                SettingKey.PHASE_TIME_PEAK,
                R.string.setting_phase_time_peak,
                R.string.setting_phase_time_peak_sub,
                dependsOn = SettingKey.SHOW_PHASE_EVENT_TIME,
            ),
            ToggleRow(
                SettingKey.SHOW_LOCATION_LABEL,
                R.string.setting_show_location_label,
                R.string.setting_show_location_label_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_CHANGE_LOCATION_BUTTON,
                R.string.setting_show_change_location,
                R.string.setting_show_change_location_sub,
            ),
        ),
    ),
    ToggleSection(
        R.string.settings_section_special_moons,
        listOf(
            ToggleRow(
                SettingKey.HIGHLIGHT_SUPERMOON,
                R.string.setting_highlight_supermoon,
                R.string.setting_highlight_supermoon_sub,
            ),
            ToggleRow(
                SettingKey.SHOW_NAMED_MOONS,
                R.string.setting_show_named_moons,
                R.string.setting_show_named_moons_sub,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_JANUARY,
                R.string.moon_slot_january,
                R.string.moon_slot_january,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                groupHeading = R.string.settings_group_which_names,
                namesSlot = MoonSlot.JANUARY,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_FEBRUARY,
                R.string.moon_slot_february,
                R.string.moon_slot_february,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.FEBRUARY,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_MARCH,
                R.string.moon_slot_march,
                R.string.moon_slot_march,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.MARCH,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_APRIL,
                R.string.moon_slot_april,
                R.string.moon_slot_april,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.APRIL,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_MAY,
                R.string.moon_slot_may,
                R.string.moon_slot_may,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.MAY,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_JUNE,
                R.string.moon_slot_june,
                R.string.moon_slot_june,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.JUNE,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_JULY,
                R.string.moon_slot_july,
                R.string.moon_slot_july,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.JULY,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_AUGUST,
                R.string.moon_slot_august,
                R.string.moon_slot_august,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.AUGUST,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_SEPTEMBER,
                R.string.moon_slot_september,
                R.string.moon_slot_september,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.SEPTEMBER,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_OCTOBER,
                R.string.moon_slot_october,
                R.string.moon_slot_october,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.OCTOBER,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_NOVEMBER,
                R.string.moon_slot_november,
                R.string.moon_slot_november,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.NOVEMBER,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_DECEMBER,
                R.string.moon_slot_december,
                R.string.moon_slot_december,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.DECEMBER,
            ),
            ToggleRow(
                SettingKey.NAMED_MOON_HARVEST,
                R.string.moon_slot_harvest,
                R.string.moon_slot_harvest,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                namesSlot = MoonSlot.HARVEST,
            ),
        ),
    ),
    ToggleSection(
        R.string.settings_section_calendar,
        listOf(
            ToggleRow(
                SettingKey.CALENDAR_SYNC,
                R.string.setting_calendar_sync,
                R.string.setting_calendar_sync_sub,
            ),
            ToggleRow(
                SettingKey.CALENDAR_FULL_MOONS,
                R.string.setting_calendar_full_moons,
                R.string.setting_calendar_full_moons_sub,
                dependsOn = SettingKey.CALENDAR_SYNC,
            ),
            ToggleRow(
                SettingKey.CALENDAR_NEW_MOONS,
                R.string.setting_calendar_new_moons,
                R.string.setting_calendar_new_moons_sub,
                dependsOn = SettingKey.CALENDAR_SYNC,
            ),
        ),
    ),
    ToggleSection(
        R.string.settings_section_presentation,
        listOf(
            ToggleRow(
                SettingKey.MOON_TEXTURE,
                R.string.setting_moon_texture,
                R.string.setting_moon_texture_sub,
            ),
            ToggleRow(
                SettingKey.TRUE_ORIENTATION,
                R.string.setting_true_orientation,
                R.string.setting_true_orientation_sub,
            ),
            ToggleRow(
                SettingKey.SWAP_SEASON_NAMES,
                R.string.setting_swap_season_names,
                R.string.setting_swap_season_names_sub,
                dependsOn = SettingKey.SHOW_NAMED_MOONS,
                // Its parent is over in Special Moons, so indenting it here
                // would read as nested under whichever row precedes it.
                indented = false,
            ),
            ToggleRow(
                SettingKey.FORCE_24_HOUR,
                R.string.setting_force_24h,
                R.string.setting_force_24h_sub,
            ),
        ),
    ),
    ToggleSection(
        R.string.settings_section_notifications,
        listOf(
            ToggleRow(
                SettingKey.NOTIFY_FULL_MOON,
                R.string.setting_notify_full_moon,
                R.string.setting_notify_full_moon_sub,
            ),
            ToggleRow(
                SettingKey.FULL_LEAD_ON_DAY,
                R.string.setting_lead_on_day,
                R.string.setting_lead_on_day_sub,
                dependsOn = SettingKey.NOTIFY_FULL_MOON,
                groupHeading = R.string.settings_group_when,
                groupId = "lead_full",
            ),
            ToggleRow(
                SettingKey.FULL_LEAD_ONE_DAY,
                R.string.setting_lead_one_day,
                R.string.setting_lead_one_day_sub,
                dependsOn = SettingKey.NOTIFY_FULL_MOON,
                groupId = "lead_full",
            ),
            ToggleRow(
                SettingKey.FULL_LEAD_THREE_DAYS,
                R.string.setting_lead_three_days,
                R.string.setting_lead_three_days_sub,
                dependsOn = SettingKey.NOTIFY_FULL_MOON,
                groupId = "lead_full",
            ),
            ToggleRow(
                SettingKey.FULL_LEAD_SEVEN_DAYS,
                R.string.setting_lead_seven_days,
                R.string.setting_lead_seven_days_sub,
                dependsOn = SettingKey.NOTIFY_FULL_MOON,
                groupId = "lead_full",
            ),
            ToggleRow(
                SettingKey.FULL_LEAD_CUSTOM,
                R.string.setting_lead_custom,
                R.string.setting_lead_custom,
                dependsOn = SettingKey.NOTIFY_FULL_MOON,
                groupId = "lead_full",
            ),
            ToggleRow(
                SettingKey.NOTIFY_NEW_MOON,
                R.string.setting_notify_new_moon,
                R.string.setting_notify_new_moon_sub,
            ),
            ToggleRow(
                SettingKey.NEW_LEAD_ON_DAY,
                R.string.setting_lead_on_day,
                R.string.setting_lead_on_day_sub,
                dependsOn = SettingKey.NOTIFY_NEW_MOON,
                groupHeading = R.string.settings_group_when,
                groupId = "lead_new",
            ),
            ToggleRow(
                SettingKey.NEW_LEAD_ONE_DAY,
                R.string.setting_lead_one_day,
                R.string.setting_lead_one_day_sub,
                dependsOn = SettingKey.NOTIFY_NEW_MOON,
                groupId = "lead_new",
            ),
            ToggleRow(
                SettingKey.NEW_LEAD_THREE_DAYS,
                R.string.setting_lead_three_days,
                R.string.setting_lead_three_days_sub,
                dependsOn = SettingKey.NOTIFY_NEW_MOON,
                groupId = "lead_new",
            ),
            ToggleRow(
                SettingKey.NEW_LEAD_SEVEN_DAYS,
                R.string.setting_lead_seven_days,
                R.string.setting_lead_seven_days_sub,
                dependsOn = SettingKey.NOTIFY_NEW_MOON,
                groupId = "lead_new",
            ),
            ToggleRow(
                SettingKey.NEW_LEAD_CUSTOM,
                R.string.setting_lead_custom,
                R.string.setting_lead_custom,
                dependsOn = SettingKey.NOTIFY_NEW_MOON,
                groupId = "lead_new",
            ),
            ToggleRow(
                SettingKey.NOTIFY_MOONRISE,
                R.string.setting_notify_moonrise,
                R.string.setting_notify_moonrise_sub,
            ),
            ToggleRow(
                SettingKey.RISE_LEAD_AT,
                R.string.setting_rise_at,
                R.string.setting_rise_at_sub,
                dependsOn = SettingKey.NOTIFY_MOONRISE,
                groupHeading = R.string.settings_group_when,
                groupId = "lead_rise",
            ),
            ToggleRow(
                SettingKey.RISE_LEAD_15,
                R.string.setting_rise_15,
                R.string.setting_rise_15_sub,
                dependsOn = SettingKey.NOTIFY_MOONRISE,
                groupId = "lead_rise",
            ),
            ToggleRow(
                SettingKey.RISE_LEAD_30,
                R.string.setting_rise_30,
                R.string.setting_rise_30_sub,
                dependsOn = SettingKey.NOTIFY_MOONRISE,
                groupId = "lead_rise",
            ),
            ToggleRow(
                SettingKey.RISE_LEAD_60,
                R.string.setting_rise_60,
                R.string.setting_rise_60_sub,
                dependsOn = SettingKey.NOTIFY_MOONRISE,
                groupId = "lead_rise",
            ),
        ),
    ),
)

/**
 * Every key the settings screen actually shows a row for.
 *
 * Exposed so a unit test can assert it covers all of [SettingKey] — otherwise a
 * new setting can be added, stored and read, yet be unreachable by the user.
 */
internal val settingsScreenKeys: List<SettingKey>
    get() = SECTIONS.flatMap { section -> section.rows.map { it.key } }

/**
 * The bits of settings-screen state that have to outlive the screen itself.
 *
 * Created by the navigation host so a return from a sub-screen lands exactly
 * where it left off, scroll position included.
 */
@Stable
class SettingsExpansion(
    val sections: MutableState<Set<Int>>,
    val groups: MutableState<Set<String>>,
    val listState: LazyListState,
)

@Composable
fun rememberSettingsExpansion(): SettingsExpansion {
    val sections = rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val groups = rememberSaveable { mutableStateOf(emptySet<String>()) }
    val listState = rememberLazyListState()
    return remember(sections, groups, listState) {
        SettingsExpansion(sections, groups, listState)
    }
}

@Composable
fun SettingsScreen(
    settings: Settings,
    hasNotificationPermission: Boolean,
    hasCalendarPermission: Boolean,
    onToggle: (SettingKey, Boolean) -> Unit,
    /** Called with the toggle that wants permission, so it can be enabled if granted. */
    onRequestNotificationPermission: (SettingKey) -> Unit,
    /** Same, for the toggle that writes to the calendar. */
    onRequestCalendarPermission: (SettingKey) -> Unit,
    onOpenLocation: () -> Unit,
    onOpenNameSet: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenTypography: () -> Unit,
    onRearrange: () -> Unit,
    onCustomLeadDays: (NotifiableEvent, Int) -> Unit,
    onNotificationTime: (Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    /**
     * Which sections are open, held by the caller.
     *
     * Hoisted deliberately: this screen is destroyed when you step into the
     * theme or name-set picker, so state remembered in here would be gone by
     * the time you came back. Everything would be folded up and scrolled to the
     * top, which reads as having been dumped back at the start rather than
     * returned to where you were.
     */
    expansion: SettingsExpansion,
) {
    var expandedSections by expansion.sections
    var expandedGroups by expansion.groups
    var customLeadFor by rememberSaveable { mutableStateOf<NotifiableEvent?>(null) }
    var showCustomLeadDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }

    if (showCustomLeadDialog) {
        val target = customLeadFor ?: NotifiableEvent.FULL_MOON
        NumberDialog(
            title = stringResource(R.string.custom_lead_title),
            initial = settings.customLeadDaysFor(target),
            min = NotificationLead.MIN_CUSTOM_DAYS,
            max = NotificationLead.MAX_CUSTOM_DAYS,
            onDismiss = { showCustomLeadDialog = false },
            onConfirm = {
                onCustomLeadDays(target, it)
                showCustomLeadDialog = false
            },
        )
    }

    if (showTimeDialog) {
        TimeOfDayDialog(
            initialMinuteOfDay = settings.notificationMinuteOfDay,
            onDismiss = { showTimeDialog = false },
            onConfirm = {
                onNotificationTime(it)
                showTimeDialog = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonColors.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        ScreenTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        LazyColumn(state = expansion.listState, modifier = Modifier.fillMaxSize()) {

            // A second route to the location picker, so hiding the button on
            // the main screen never leaves it unreachable.
            item(key = "location_entry") {
                SectionLabel(stringResource(R.string.location_title))
                EntryRow(
                    title = stringResource(R.string.settings_change_location),
                    subtitle = stringResource(R.string.settings_change_location_sub),
                    onClick = onOpenLocation,
                )
                Spacer(Modifier.height(16.dp))
            }

            SECTIONS.forEach { section ->
                val isExpanded = section.title in expandedSections

                item(key = "header_${section.title}") {
                    SectionHeader(
                        title = stringResource(section.title),
                        expanded = isExpanded,
                        onClick = {
                            expandedSections = if (isExpanded) {
                                expandedSections - section.title
                            } else {
                                expandedSections + section.title
                            }
                        },
                    )
                }

                // Children collapse away with their parent rather than sitting
                // there greyed out — nothing to read until it is relevant.
                val visibleRows = if (!isExpanded) {
                    emptyList()
                } else {
                    section.rows.filter { row ->
                        val parentOn = settings.ancestorsOn(row.key)
                        // A folded group keeps only its heading row, which acts
                        // as the thing you tap to unfold it.
                        val groupOpen = row.groupId == null ||
                            row.groupId in expandedGroups ||
                            row.groupHeading != null
                        parentOn && groupOpen
                    }
                }

                items(visibleRows, key = { it.key.name }) { row ->
                    val needsNotification =
                        row.key.needsNotificationPermission && !hasNotificationPermission
                    val needsCalendar =
                        row.key.needsCalendarPermission && !hasCalendarPermission
                    val requiresPermission = needsNotification || needsCalendar

                    val groupId = row.groupId
                    val groupOpen = groupId == null || groupId in expandedGroups

                    row.groupHeading?.let { heading ->
                        if (groupId == null) {
                            SubGroupLabel(stringResource(heading))
                        } else {
                            SubGroupHeader(
                                title = stringResource(heading),
                                expanded = groupOpen,
                                onClick = {
                                    expandedGroups = if (groupOpen) {
                                        expandedGroups - groupId
                                    } else {
                                        expandedGroups + groupId
                                    }
                                },
                            )
                        }
                    }

                    val context = LocalContext.current
                    // The heading row is still emitted while folded so it can be
                    // tapped, so its own checkbox has to be suppressed.
                    if (groupOpen) CheckboxRow(
                        title = row.namesSlot
                            ?.let { moonName(context, settings.moonNameSet, it) }
                            ?: stringResource(row.title),
                        subtitle = when {
                            needsCalendar ->
                                stringResource(R.string.setting_needs_calendar_permission)
                            needsNotification ->
                                stringResource(R.string.setting_needs_notification_permission)
                            row.namesSlot != null -> stringResource(row.namesSlot.slotLabelRes)
                            // The only subtitle carrying a placeholder.
                            row.key == SettingKey.FULL_LEAD_CUSTOM -> pluralStringResource(
                                R.plurals.lead_days_before,
                                settings.customLeadDaysFor(NotifiableEvent.FULL_MOON),
                                settings.customLeadDaysFor(NotifiableEvent.FULL_MOON),
                            )
                            row.key == SettingKey.NEW_LEAD_CUSTOM -> pluralStringResource(
                                R.plurals.lead_days_before,
                                settings.customLeadDaysFor(NotifiableEvent.NEW_MOON),
                                settings.customLeadDaysFor(NotifiableEvent.NEW_MOON),
                            )
                            else -> stringResource(row.subtitle)
                        },
                        checked = settings.valueOf(row.key),
                        indented = row.indented ?: (row.dependsOn != null),
                        onCheckedChange = { wanted ->
                            // Asking for permission only at the moment it is
                            // actually needed, not on first launch.
                            when {
                                wanted && needsCalendar -> onRequestCalendarPermission(row.key)
                                wanted && needsNotification ->
                                    onRequestNotificationPermission(row.key)
                                else -> onToggle(row.key, wanted)
                            }
                        },
                    )

                    // Not a toggle, so it cannot be a ToggleRow. Rendered in the
                    // same item as its master so it sits directly beneath it.
                    // Custom day count, per event.
                    val customEvent = when (row.key) {
                        SettingKey.FULL_LEAD_CUSTOM -> NotifiableEvent.FULL_MOON
                        SettingKey.NEW_LEAD_CUSTOM -> NotifiableEvent.NEW_MOON
                        else -> null
                    }
                    if (groupOpen && customEvent != null && settings.valueOf(row.key)) {
                        EntryRow(
                            title = stringResource(R.string.settings_custom_lead),
                            subtitle = pluralStringResource(
                                R.plurals.lead_days_before,
                                settings.customLeadDaysFor(customEvent),
                                settings.customLeadDaysFor(customEvent),
                            ),
                            indented = true,
                            onClick = {
                                customLeadFor = customEvent
                                showCustomLeadDialog = true
                            },
                        )
                    }


                    if (row.key == SettingKey.SHOW_NAMED_MOONS && settings.showNamedMoons) {
                        EntryRow(
                            title = stringResource(R.string.settings_name_set),
                            subtitle = stringResource(settings.moonNameSet.displayNameRes),
                            indented = true,
                            onClick = onOpenNameSet,
                        )
                    }
                }

                if (isExpanded && section.title == R.string.settings_section_presentation) {
                    item(key = "theme_entry") {
                        EntryRow(
                            title = stringResource(R.string.settings_theme),
                            subtitle = stringResource(settings.theme.displayNameRes),
                            onClick = onOpenTheme,
                        )
                        EntryRow(
                            title = stringResource(R.string.settings_typography),
                            subtitle = stringResource(
                                R.string.typography_summary,
                                stringResource(settings.appFont.displayNameRes),
                                stringResource(settings.textSize.displayNameRes),
                            ),
                            onClick = onOpenTypography,
                        )
                    }
                }

                if (isExpanded && section.title == R.string.settings_section_app) {
                    item(key = "rearrange_entry") {
                        EntryRow(
                            title = stringResource(R.string.settings_rearrange),
                            subtitle = stringResource(R.string.settings_rearrange_sub),
                            onClick = onRearrange,
                        )
                    }
                }

                // One clock covers every advance reminder, so it belongs to the
                // section rather than to any single event.
                if (isExpanded &&
                    section.title == R.string.settings_section_notifications &&
                    settings.anyPhaseNotification
                ) {
                    item(key = "notification_time") {
                        EntryRow(
                            title = stringResource(R.string.settings_notification_time),
                            subtitle = formatMinuteOfDay(settings.notificationMinuteOfDay),
                            onClick = { showTimeDialog = true },
                        )
                    }
                }

                item(key = "space_${section.title}") {
                    Spacer(Modifier.height(if (isExpanded) 16.dp else 2.dp))
                }
            }

            item {
                // Below every setting, above the reset: checking for a new
                // build is housekeeping, not configuration.
                UpdateRow()
                Spacer(Modifier.height(8.dp))
            }

            item {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.settings_reset), color = MoonColors.muted)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Top bar for a settings page: a back arrow, then the title.
 *
 * The arrow does exactly what the system back gesture does, so the two can't
 * disagree about where "up" is.
 */
@Composable
internal fun ScreenTopBar(title: String, onBack: () -> Unit) {
    val backLabel = stringResource(R.string.cd_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\u2039",
            color = MoonColors.text,
            fontSize = 30.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClickLabel = backLabel, onClick = onBack)
                .semantics { contentDescription = backLabel }
                // Generous padding: the glyph alone is well under a 48dp target.
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            color = MoonColors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Tappable section heading. */
@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            color = if (expanded) MoonColors.text else MoonColors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        // Rotating a glyph avoids pulling in an icon dependency for one arrow.
        Text(
            text = if (expanded) "⌄" else "›",
            color = MoonColors.muted,
            fontSize = 16.sp,
        )
    }
}

/** A row that opens another screen rather than toggling anything. */
@Composable
private fun EntryRow(
    title: String,
    subtitle: String,
    indented: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) 20.dp else 0.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MoonColors.text, fontSize = if (indented) 14.sp else 15.sp)
            Text(subtitle, color = MoonColors.muted, fontSize = 12.sp)
        }
        Text("›", color = MoonColors.muted, fontSize = 18.sp)
    }
}

/** Tappable heading that folds a set of sibling child rows away. */
@Composable
private fun SubGroupHeader(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, top = 10.dp, bottom = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MoonColors.muted.copy(alpha = if (expanded) 0.9f else 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (expanded) "⌄" else "›",
            color = MoonColors.muted,
            fontSize = 14.sp,
        )
    }
}

/** Heading for a set of sibling child rows, e.g. "Which moons". */
@Composable
private fun SubGroupLabel(text: String) {
    Text(
        text = text,
        color = MoonColors.muted.copy(alpha = 0.6f),
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 28.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun CheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    indented: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) 20.dp else 0.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            // The whole row is the target, not just the small box.
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MoonColors.text,
                fontSize = if (indented) 14.sp else 15.sp,
            )
            Text(subtitle, color = MoonColors.muted, fontSize = 12.sp)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MoonColors.accent,
                uncheckedColor = MoonColors.muted,
                checkmarkColor = MoonColors.background,
            ),
        )
    }
}
