package dev.mahourigan.moonwidget.widget

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.data.LocationRepository
import dev.mahourigan.moonwidget.data.Prefs
import dev.mahourigan.moonwidget.data.PresetLocations
import dev.mahourigan.moonwidget.data.ResolvedLocation
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.domain.MoonSnapshot
import dev.mahourigan.moonwidget.render.CompassRenderer
import dev.mahourigan.moonwidget.render.MoonRenderer
import dev.mahourigan.moonwidget.render.TimeFormatting
import dev.mahourigan.moonwidget.ui.MainActivity
import dev.mahourigan.moonwidget.render.MoonOrbitRenderer
import dev.mahourigan.moonwidget.render.SkyDomeRenderer
import dev.mahourigan.moonwidget.ui.Palette
import dev.mahourigan.moonwidget.ui.toCompassPalette
import dev.mahourigan.moonwidget.ui.toOrbitPalette
import dev.mahourigan.moonwidget.ui.toDomePalette
import dev.mahourigan.moonwidget.ui.toGlanceFontFamily
import dev.mahourigan.moonwidget.ui.toPalette
import dev.mahourigan.moonwidget.ui.toRenderPalette
import dev.mahourigan.moonwidget.ui.bearingText
import dev.mahourigan.moonwidget.ui.displayName
import dev.mahourigan.moonwidget.ui.formatted
import dev.mahourigan.moonwidget.ui.fullMoonCountdown
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Home-screen widget.
 *
 * Two layouts, chosen by available space: a compact square showing just the
 * Moon, its phase and illumination, and a wide layout that adds rise/set times
 * and the countdown to the next full moon.
 */
class MoonWidget : GlanceAppWidget() {

    /** Glance recomposes with the real size, so both layouts stay in one file. */
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Loaded eagerly so the very first frame has something real to draw
        // rather than flashing empty.
        val initial = loadWidgetData(context)

        provideContent {
            // Then read again from inside the composition, and this is the part
            // that matters. provideGlance runs once per Glance session, not once
            // per update: updateAll on a live session only asks it to recompose.
            // Settings captured out here would be frozen for the life of that
            // session, so changing the theme would recompose the very same
            // values and look like nothing had happened — which is exactly what
            // it did. Collecting the store inside the composition means a change
            // reaches the widget whether or not anyone remembers to call
            // updateAll.
            //
            // The settings flow alone is still not enough, because it only
            // emits when a setting is written, and the snapshot needs a clock.
            // Keying the remember on the refresh stamp rebuilds the flow, and a
            // fresh collection recomputes the snapshot from the current time.
            // Without this the widget shows rise and set times from whenever
            // the session began — including set times that have already passed,
            // which a live computation can never produce.
            val refreshedAt = currentState(REFRESHED_AT) ?: 0L
            val data by remember(refreshedAt) { widgetData(context) }
                .collectAsState(initial = initial)
            val palette = data.settings.palette.toPalette()

            GlanceTheme {
                val snapshot = data.snapshot
                if (snapshot != null) {
                    WidgetBody(snapshot, data.settings, palette)
                } else {
                    UnavailableBody(palette, data.settings)
                }
            }
        }
    }

    /** Everything one drawing of the widget needs. */
    private data class WidgetData(
        val settings: Settings,
        val snapshot: MoonSnapshot?,
    )

    /** Re-reads and recomputes every time any setting changes. */
    private fun widgetData(context: Context): Flow<WidgetData> =
        Prefs(context).settings
            .map { settings -> WidgetData(settings, loadSnapshot(context, settings)) }
            // Leaves the last good drawing in place rather than blanking the
            // tile if the store ever fails to read.
            .catch { Log.e(TAG, "Could not read settings for the widget", it) }

    private suspend fun loadWidgetData(context: Context): WidgetData {
        val settings = runCatching { Prefs(context).currentSettings() }.getOrDefault(Settings())
        return WidgetData(settings, loadSnapshot(context, settings))
    }

    /**
     * Build a snapshot, degrading rather than failing.
     *
     * A widget that throws leaves a blank or crashed tile on the home screen
     * with no way for the user to understand or fix it, so every layer here has
     * a fallback:
     *
     *  1. the user's chosen location
     *  2. if resolving that fails (bad saved coordinates, an unknown timezone),
     *     the default preset — the astronomy is pure maths and will still work
     *  3. if even that fails, null, and the widget says so plainly
     */
    private suspend fun loadSnapshot(context: Context, settings: Settings): MoonSnapshot? {
        runCatching {
            val location = LocationRepository(context, Prefs(context)).resolve()
            return MoonSnapshot.compute(location, settings = settings)
        }.onFailure {
            Log.w(TAG, "Falling back to the default location", it)
        }

        return runCatching {
            MoonSnapshot.compute(
                ResolvedLocation.fromPreset(PresetLocations.default, isFallback = true),
                settings = settings,
            )
        }.onFailure {
            Log.e(TAG, "Could not compute a moon snapshot at all", it)
        }.getOrNull()
    }

    /** Last-resort tile. Better than a blank square with no explanation. */
    @Composable
    private fun UnavailableBody(palette: Palette, settings: Settings) {
        val context = androidx.glance.LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(palette.background)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.widget_unavailable),
                style = widgetText(palette.muted, 13, settings),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_unavailable_hint),
                style = widgetText(palette.muted, 11, settings),
            )
        }
    }

    @Composable
    private fun WidgetBody(snapshot: MoonSnapshot, settings: Settings, palette: Palette) {
        val size = LocalSize.current
        val wide = size.width >= 220.dp

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(palette.background)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (settings.widgetShowMoonImage) {
                MoonImage(
                    snapshot, settings, palette,
                    diameter = moonDiameter(size.height.value, wide, settings),
                    compact = !wide,
                )
            }

            if (wide && settings.widgetTextColumnHasContent) {
                if (settings.widgetShowMoonImage) Spacer(GlanceModifier.width(14.dp))
                WideDetails(snapshot, settings, palette)
            }
        }
    }

    /**
     * How big to draw the Moon, in dp.
     *
     * Measured against the tile rather than hardcoded: a widget may be resized
     * down to the 110dp declared minimum, and on the compact layout the dome
     * sits underneath and wants its share of the height. Asking for a fixed
     * size would simply clip on a small tile.
     */
    private fun moonDiameter(tileHeightDp: Float, wide: Boolean, settings: Settings): Int {
        val available = tileHeightDp - TILE_PADDING_DP * 2
        // On the wide layout the dome sits beside the Moon, not under it.
        val domeBlock =
            if (settings.widgetPanelEnabled && !wide) DOME_HEIGHT_DP + DOME_GAP_DP else 0f
        val preferred = when {
            wide -> WIDE_MOON_DP
            settings.widgetPanelEnabled -> COMPACT_WITH_DOME_MOON_DP
            else -> COMPACT_MOON_DP
        }

        return preferred
            .coerceAtMost(available - domeBlock)
            .coerceAtLeast(MIN_MOON_DP)
            .toInt()
    }

    @Composable
    private fun MoonImage(
        snapshot: MoonSnapshot,
        settings: Settings,
        palette: Palette,
        diameter: Int,
        compact: Boolean,
    ) {
        val context = androidx.glance.LocalContext.current
        val density = context.resources.displayMetrics.density
        val pixels = (diameter * density).toInt()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val symbol = if (settings.widgetShowSignOnMoon) snapshot.sign.sign.symbol else null
            val rings = MoonOrbitRenderer.Rings(
                passFraction = snapshot.pass
                    ?.takeIf { settings.widgetSkyPathIsOrbit }
                    ?.fractionAt(snapshot.calculatedAt),
                moonIsUp = snapshot.isUp,
                azimuthDegrees = WidgetPanel.bearingOf(snapshot)
                    ?.takeIf { settings.widgetDirectionIsOrbit },
            )

            Image(
                provider = ImageProvider(
                    if (rings.count > 0) {
                        MoonOrbitRenderer.render(
                            context = context,
                            sizePx = pixels,
                            density = density,
                            appearance = snapshot.appearance,
                            rings = rings,
                            moonPalette = palette.toRenderPalette(),
                            palette = palette.toOrbitPalette(),
                            symbol = symbol,
                            blendSymbol = settings.signOnMoonBlended,
                            useTexture = settings.moonTextureEnabled,
                        )
                    } else {
                        MoonRenderer.render(
                            context = context,
                            sizePx = pixels,
                            appearance = snapshot.appearance,
                            palette = palette.toRenderPalette(),
                            symbol = symbol,
                            blendSymbol = settings.signOnMoonBlended,
                            useTexture = settings.moonTextureEnabled,
                        )
                    }
                ),
                contentDescription = context.getString(
                    R.string.widget_moon_description,
                    snapshot.phase.phaseName.displayName(context),
                    snapshot.phase.illuminationPercent,
                ),
                modifier = GlanceModifier.size(diameter.dp),
            )

            if (compact && settings.widgetPanelEnabled) {
                Spacer(GlanceModifier.height(DOME_GAP_DP.toInt().dp))
                Panel(
                    snapshot, settings, palette,
                    widthDp = 68, heightDp = DOME_HEIGHT_DP.toInt(),
                )
            }

            if (compact && settings.widgetCompactShowIllumination) {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(
                        R.string.widget_illumination_short,
                        snapshot.phase.illuminationPercent,
                    ),
                    style = widgetText(palette.muted, 11, settings),
                )
            }

            if (settings.highlightSupermoon && snapshot.isSupermoon) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = context.getString(R.string.widget_supermoon_badge),
                    style = widgetText(palette.accent, 10, settings),
                )
            }
        }
    }

    @Composable
    private fun WideDetails(snapshot: MoonSnapshot, settings: Settings, palette: Palette) {
        val context = androidx.glance.LocalContext.current
        Column(verticalAlignment = Alignment.CenterVertically) {
            if (settings.widgetShowPhaseName) {
                Text(
                    text = snapshot.phase.phaseName.displayName(context),
                    style = widgetText(palette.text, 15, settings, FontWeight.Medium),
                )
                Spacer(GlanceModifier.height(2.dp))
            }

            if (settings.widgetShowIllumination) {
                Text(
                    text = context.getString(
                        R.string.widget_illumination_short,
                        snapshot.phase.illuminationPercent,
                    ),
                    style = widgetText(palette.muted, 12, settings),
                )
            }

            if (settings.widgetShowMoonSign) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "${snapshot.sign.sign.symbol}  ${snapshot.sign.formatted(context)}",
                    style = widgetText(palette.muted, 12, settings),
                )
            }

            if (settings.widgetShowRiseSet) {
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = riseSetLine(snapshot, context, settings.force24HourTime),
                    style = widgetText(palette.muted, 12, settings),
                )
            }

            if (settings.widgetShowDirection) {
                val bearing =
                    if (snapshot.isUp) snapshot.position?.azimuth else snapshot.riseAzimuth
                bearing?.let {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = context.getString(
                            if (snapshot.isUp) R.string.widget_direction
                            else R.string.widget_direction_at_rise,
                            bearingText(context, it),
                        ),
                        style = widgetText(palette.muted, 12, settings),
                    )
                }
            }

            if (settings.widgetPanelEnabled) {
                Spacer(GlanceModifier.height(6.dp))
                Panel(snapshot, settings, palette, widthDp = 76, heightDp = 44)
            }

            if (settings.widgetShowFullMoonCountdown) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = fullMoonCountdown(context, snapshot.nextFullMoon, snapshot.zone, snapshot.calculatedAt),
                    style = widgetText(palette.muted, 12, settings),
                )
            }
        }
    }

    /**
     * The tappable panel: one of several small views, cycled by tapping it.
     *
     * Its own click handler sits inside the tile's, which opens the app. In
     * RemoteViews the innermost handler wins, so tapping here advances the
     * panel and tapping anywhere else — the Moon especially — still opens the
     * app, which is the behaviour people expect from a widget.
     */
    @Composable
    private fun Panel(
        snapshot: MoonSnapshot,
        settings: Settings,
        palette: Palette,
        widthDp: Int,
        heightDp: Int,
    ) {
        val available = WidgetPanel.available(snapshot, settings)
        val index = currentState(PANEL_INDEX) ?: 0
        val panel = WidgetPanel.at(index, available) ?: return
        val context = androidx.glance.LocalContext.current

        // The dome and the dial are drawings sized to their box, so they need
        // the fixed width. The next-event line is a time, and at the size it
        // is set now it is wider than 68dp -- given a fixed width it would
        // simply clip. So that one takes the height and finds its own width.
        val shape = when (panel) {
            WidgetPanel.NEXT_EVENT -> GlanceModifier.height(heightDp.dp)
            else -> GlanceModifier.size(width = widthDp.dp, height = heightDp.dp)
        }

        Box(
            modifier = shape
                // Pointless — and a confusing no-op for the user — when there
                // is only one view to look at.
                .then(
                    if (available.size > 1) {
                        GlanceModifier.clickable(actionRunCallback<CyclePanelAction>())
                    } else {
                        GlanceModifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (panel) {
                WidgetPanel.SKY_PATH -> SkyDome(snapshot, palette, widthDp, heightDp)
                WidgetPanel.COMPASS -> CompassDial(snapshot, palette, heightDp)
                // Twice the old 12sp. It is the one panel that is a fact
                // rather than a picture, and it was the smallest thing on the
                // widget -- readable at arm's length is the whole point of
                // putting the next crossing on a home screen.
                WidgetPanel.NEXT_EVENT -> Text(
                    text = nextEventLine(snapshot, settings, context),
                    style = widgetText(palette.text, 24, settings, FontWeight.Medium),
                )
            }
        }
    }

    /**
     * Tonight's pass as a small dome, with the Moon somewhere along it.
     *
     * Drawn to a bitmap for the same reason the Moon is: RemoteViews cannot run
     * drawing code of its own.
     */
    @Composable
    private fun SkyDome(
        snapshot: MoonSnapshot,
        palette: Palette,
        widthDp: Int,
        heightDp: Int,
    ) {
        val pass = snapshot.pass ?: return
        val context = androidx.glance.LocalContext.current
        val density = context.resources.displayMetrics.density

        Image(
            provider = ImageProvider(
                SkyDomeRenderer.render(
                    widthPx = (widthDp * density).toInt(),
                    heightPx = (heightDp * density).toInt(),
                    fraction = pass.fractionAt(snapshot.calculatedAt),
                    moonIsUp = snapshot.isUp,
                    palette = palette.toDomePalette(),
                )
            ),
            contentDescription = context.getString(R.string.widget_path_description),
            modifier = GlanceModifier.size(width = widthDp.dp, height = heightDp.dp),
        )
    }

    /** The compass dial, square, so it is sized off the height it is given. */
    @Composable
    private fun CompassDial(snapshot: MoonSnapshot, palette: Palette, sizeDp: Int) {
        val bearing = WidgetPanel.bearingOf(snapshot) ?: return
        val context = androidx.glance.LocalContext.current
        val density = context.resources.displayMetrics.density
        val pixels = (sizeDp * density).toInt()

        Image(
            provider = ImageProvider(
                CompassRenderer.render(
                    widthPx = pixels,
                    heightPx = pixels,
                    azimuthDegrees = bearing,
                    moonIsUp = snapshot.isUp,
                    palette = palette.toCompassPalette(),
                )
            ),
            contentDescription = context.getString(
                if (snapshot.isUp) R.string.widget_direction
                else R.string.widget_direction_at_rise,
                bearingText(context, bearing),
            ),
            modifier = GlanceModifier.size(sizeDp.dp),
        )
    }

    /**
     * "Rises 4:40 pm", or the set when the Moon is already up.
     *
     * A clock time rather than a countdown: it is what you would tell someone
     * out loud, and it does not go stale between the half-hourly refreshes the
     * way "in 3h 12m" would.
     *
     * Leading with whichever comes first for the same reason [riseSetLine]
     * does: while the Moon is up, the next rise is most of a day away and
     * answers nothing anyone is asking.
     */
    private fun nextEventLine(
        snapshot: MoonSnapshot,
        settings: Settings,
        context: Context,
    ): String {
        val target = if (snapshot.isUp) snapshot.next.set else snapshot.next.rise

        return target
            // Follows the device's 12/24-hour setting, and the app's own
            // override of it, so this matches every other time on screen.
            ?.let { TimeFormatting.format(it, snapshot.zone, context, settings.force24HourTime) }
            ?: context.getString(R.string.horizon_none_soon)
    }

    /**
     * Leads with whichever event comes first, so a glance answers the question
     * actually being asked — "is it up, and when does that change?"
     */
    private fun riseSetLine(
        snapshot: MoonSnapshot,
        context: Context,
        force24Hour: Boolean,
    ): String {
        val rise = TimeFormatting.format(snapshot.next.rise, snapshot.zone, context, force24Hour)
        val set = TimeFormatting.format(snapshot.next.set, snapshot.zone, context, force24Hour)

        return if (snapshot.isUp) {
            context.getString(R.string.widget_set_then_rise, set, rise)
        } else {
            context.getString(R.string.widget_rise_then_set, rise, set)
        }
    }

    private companion object {
        const val TAG = "MoonWidget"

        const val TILE_PADDING_DP = 12f
        const val DOME_HEIGHT_DP = 40f
        const val DOME_GAP_DP = 5f

        // Three separate baselines: the compact layout gives up room to the
        // dome, so it cannot simply share the wide layout figure.
        const val WIDE_MOON_DP = 109f
        // The square tile has height to spare, and these have been enlarged
        // twice at the user's request.
        const val COMPACT_MOON_DP = 114f
        const val COMPACT_WITH_DOME_MOON_DP = 95f

        /** Below this the Moon stops being a picture of anything. */
        const val MIN_MOON_DP = 32f
    }
}

/**
 * A widget label in the chosen size and typeface.
 *
 * Glance runs in RemoteViews, well outside the app's composition, so the
 * density override that scales every sp in the app does not reach here. The
 * multiplication has to be explicit, which is why every widget label goes
 * through this rather than setting a size directly.
 */
private fun widgetText(
    color: androidx.compose.ui.graphics.Color,
    size: Int,
    settings: Settings,
    weight: FontWeight? = null,
): TextStyle = TextStyle(
    color = ColorProvider(color),
    fontSize = (size * settings.textSize.scale).sp(),
    fontFamily = settings.appFont.toGlanceFontFamily(),
    fontWeight = weight,
)

/** Glance text sizes want TextUnit; keep the call sites readable. */
private fun Float.sp() = androidx.compose.ui.unit.TextUnit(
    this,
    androidx.compose.ui.unit.TextUnitType.Sp,
)

