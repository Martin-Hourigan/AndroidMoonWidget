package dev.mahourigan.moonwidget.widget

import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.domain.MoonSnapshot

/**
 * What the tappable panel on the widget is currently showing.
 *
 * The panel occupies one small area — where the sky dome used to sit on its
 * own — and tapping it moves to the next view. The Moon itself keeps its own
 * tap, which opens the app, so the widget still does the obvious thing.
 */
enum class WidgetPanel {
    /** Tonight's pass as a dome, with the Moon somewhere along it. */
    SKY_PATH,

    /** How long until the Moon next rises, or sets if it is already up. */
    NEXT_EVENT,

    /** Which way to face, as a dial. */
    COMPASS,
    ;

    companion object {

        /**
         * The views worth showing right now, in cycle order.
         *
         * Driven by the settings *and* by whether there is anything to draw:
         * a dome needs a pass, and a compass needs a bearing, and at high
         * latitudes the Moon can go weeks without providing either. A view
         * with nothing behind it is left out rather than shown empty.
         */
        fun available(snapshot: MoonSnapshot, settings: Settings): List<WidgetPanel> =
            buildList {
                if (settings.widgetShowSkyPath && snapshot.pass != null) add(SKY_PATH)
                // Always offered once the panel exists at all: the time to the
                // next crossing is the thing people check a moon widget for.
                if (snapshot.next.rise != null || snapshot.next.set != null) add(NEXT_EVENT)
                if (settings.widgetShowDirection && bearingOf(snapshot) != null) add(COMPASS)
            }

        /**
         * Which view a stored [index] selects.
         *
         * Wrapped rather than clamped, so the tap always advances and never
         * sticks at the end. Null when there is nothing to show at all, which
         * is also what hides the panel.
         *
         * Taking it modulo the list is what makes a stale index harmless: turn
         * the compass off while it is on screen and the next draw simply lands
         * somewhere valid instead of on a view that no longer exists.
         */
        fun at(index: Int, available: List<WidgetPanel>): WidgetPanel? {
            if (available.isEmpty()) return null
            return available[Math.floorMod(index, available.size)]
        }

        /** The bearing the compass points at: where it is, or where it will rise. */
        fun bearingOf(snapshot: MoonSnapshot): Double? =
            if (snapshot.isUp) snapshot.position?.azimuth else snapshot.riseAzimuth
    }
}
