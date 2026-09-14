package dev.mahourigan.moonwidget.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

/** When the widget's snapshot was last asked for, per widget. */
internal val REFRESHED_AT = longPreferencesKey("refreshed_at")

/**
 * Redraws every widget with a freshly computed snapshot.
 *
 * [updateAll] on its own is not enough, and this is the whole reason this file
 * exists. `provideGlance` runs once per Glance session, not once per update; on
 * a session that is still alive `updateAll` only asks for a recomposition, and
 * a recomposition re-reads a settings flow that has not emitted. The widget
 * therefore redraws the identical [dev.mahourigan.moonwidget.domain.MoonSnapshot]
 * it was born with — frozen rise and set times, a frozen countdown, and a sky
 * dome marker parked wherever the Moon happened to be when the session started.
 * It looks like a working widget, because the stale values are all consistent
 * with one another; the only way to catch it is to compare against the app.
 *
 * Stamping the time into each widget's own state first gives the composition
 * something that has genuinely changed to key its snapshot off. This is the
 * same mechanism [CyclePanelAction] uses for the panel index, which is already
 * known to survive an update.
 */
suspend fun refreshWidgets(context: Context) {
    val now = System.currentTimeMillis()

    runCatching {
        GlanceAppWidgetManager(context)
            .getGlanceIds(MoonWidget::class.java)
            .forEach { id ->
                updateAppWidgetState(context, id) { prefs -> prefs[REFRESHED_AT] = now }
            }
    }
    // Redraw regardless: a failure to stamp is not a reason to skip the update,
    // and a cold session recomputes anyway.
    MoonWidget().updateAll(context)
}
