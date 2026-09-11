package dev.mahourigan.moonwidget.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

/** Where the panel's position is kept, per widget. */
internal val PANEL_INDEX = intPreferencesKey("panel_index")

/**
 * Advances the tappable panel to its next view.
 *
 * The index is stored per [GlanceId] rather than in the app's own settings, so
 * two widgets on the same home screen can sit on different views — and so that
 * flipping a widget to the compass for a moment is not a change to the user's
 * saved preferences.
 *
 * It only ever counts upward; [WidgetPanel.at] takes it modulo whatever views
 * are currently available, which is what keeps a stale index harmless when a
 * setting removes one of them.
 */
class CyclePanelAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PANEL_INDEX] = (prefs[PANEL_INDEX] ?: 0) + 1
        }
        MoonWidget().update(context, glanceId)
    }
}
