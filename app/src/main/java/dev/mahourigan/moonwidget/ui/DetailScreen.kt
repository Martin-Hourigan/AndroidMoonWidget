package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.astronomy.SunZodiac
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.data.namedMoonKey
import dev.mahourigan.moonwidget.data.valueOf
import dev.mahourigan.moonwidget.domain.MoonSnapshot
import dev.mahourigan.moonwidget.render.TimeFormatting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mahourigan.moonwidget.data.AppSection
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun DetailScreen(
    state: MoonUiState,
    onChangeLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    rearranging: Boolean = false,
    onReorder: (List<AppSection>) -> Unit = {},
    onDoneRearranging: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonColors.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val snapshot = state.snapshot
        when {
            rearranging -> RearrangeContent(
                order = state.settings.sectionOrder,
                onReorder = onReorder,
                onDone = onDoneRearranging,
            )
            snapshot != null ->
                DetailContent(snapshot, state.settings, onChangeLocation, onOpenSettings)
            state.failed -> FailureContent(onRetry)
            else -> CircularProgressIndicator(color = MoonColors.text)
        }
    }
}

/** Shown only if even the default-location fallback could not be computed. */
@Composable
private fun FailureContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.detail_unavailable),
            color = MoonColors.text,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MoonColors.surface,
                contentColor = MoonColors.text,
            ),
        ) {
            Text(stringResource(R.string.detail_retry))
        }
    }
}

@Composable
private fun DetailContent(
    snapshot: MoonSnapshot,
    settings: Settings,
    onChangeLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        if (settings.showMoonImage) {
            MoonShape(
                appearance = snapshot.appearance,
                diameter = 180.dp,
                useTexture = settings.moonTextureEnabled,
                symbol = if (settings.showSignOnMoon) snapshot.sign.sign.symbol else null,
                blendSymbol = settings.signOnMoonBlended,
            )
            Spacer(Modifier.height(20.dp))
        }

        if (settings.showPhaseName) {
            Text(
                text = snapshot.phase.phaseName.displayName(context),
                color = MoonColors.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (settings.showIllumination) {
            Text(
                text = stringResource(
                    R.string.detail_illuminated,
                    snapshot.phase.illuminationPercent,
                ),
                color = MoonColors.muted,
                fontSize = 15.sp,
            )
        }

        if (settings.highlightSupermoon && (snapshot.isSupermoon || snapshot.isMicromoon)) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (snapshot.isSupermoon) R.string.detail_supermoon
                    else R.string.detail_micromoon
                ),
                color = MoonColors.text,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Cards are skipped entirely when empty — an empty rounded rectangle
        // looks like a rendering bug rather than a choice. The order is the
        // user's; see AppSection.
        settings.sectionOrder.forEach { section ->
            SectionCard(section, snapshot, settings, context)
        }

        Spacer(Modifier.height(8.dp))

        if (settings.showLocationLabel) {
            Text(
                text = if (snapshot.location.isFallback) {
                    stringResource(R.string.detail_location_default, snapshot.location.label)
                } else {
                    snapshot.location.label
                },
                color = MoonColors.muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (settings.showChangeLocationButton) {
            Button(
                onClick = onChangeLocation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoonColors.surface,
                    contentColor = MoonColors.text,
                ),
            ) {
                Text(stringResource(R.string.detail_change_location))
            }
        }

        // Always visible: it is the only guaranteed way to reach settings.
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.settings_open), color = MoonColors.muted)
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Shown when there is genuinely no crossing in the search window. */
@Composable
private fun horizonNote(snapshot: MoonSnapshot): String = stringResource(
    when {
        snapshot.riseSet.alwaysUp -> R.string.horizon_up_all_day
        snapshot.riseSet.alwaysDown -> R.string.horizon_below
        else -> R.string.horizon_none_soon
    }
)


/**
 * One movable card, chosen by [section].
 *
 * Each keeps its own emptiness check: a card with everything switched off is
 * skipped rather than drawn as a bare rounded rectangle, which reads as a bug.
 * Every card that does draw leaves the same gap behind it, so reordering cannot
 * change the spacing.
 */
@Composable
private fun SectionCard(
    section: AppSection,
    snapshot: MoonSnapshot,
    settings: Settings,
    context: android.content.Context,
) {
    when (section) {
        AppSection.RISE_SET -> {
            if (settings.riseSetCardHasContent) {
                Card {
                    if (settings.showRiseSet) {
                        // Upcoming crossings, not "today's" — a rise that happened
                        // this morning is not what someone opening the app wants.
                        InfoRow(
                            label = stringResource(
                                if (snapshot.isUp) R.string.detail_sets else R.string.detail_rises
                            ),
                            value = TimeFormatting.formatWithDayHint(
                                instant = if (snapshot.isUp) snapshot.next.set else snapshot.next.rise,
                                zone = snapshot.zone,
                                context = context,
                                now = snapshot.calculatedAt,
                                force24Hour = settings.force24HourTime,
                                placeholder = horizonNote(snapshot),
                            ),
                        )
                        InfoRow(
                            label = stringResource(
                                if (snapshot.isUp) R.string.detail_rises_again
                                else R.string.detail_then_sets
                            ),
                            value = TimeFormatting.formatWithDayHint(
                                instant = if (snapshot.isUp) snapshot.next.rise else snapshot.next.set,
                                zone = snapshot.zone,
                                context = context,
                                now = snapshot.calculatedAt,
                                force24Hour = settings.force24HourTime,
                                placeholder = horizonNote(snapshot),
                            ),
                        )
                    }
                    // Where to point yourself. While the Moon is down its current
                // bearing is no help for finding it, so the next rise is shown
                // instead — that is the direction that will matter.
                if (settings.showCompassDirection) {
                    val bearing = if (snapshot.isUp) {
                        snapshot.position?.azimuth
                    } else {
                        snapshot.riseAzimuth
                    }
                    bearing?.let {
                        InfoRow(
                            label = stringResource(
                                if (snapshot.isUp) R.string.detail_direction
                                else R.string.detail_direction_at_rise
                            ),
                            value = bearingText(context, it),
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CompassRose(
                                azimuthDegrees = it,
                                // Dimmed while down: the dot is showing where the
                                // Moon will rise, not where it is right now.
                                dotColor = if (snapshot.isUp) MoonColors.accent else MoonColors.muted,
                            )
                        }
                    }
                }
                if (settings.showHorizonStatus) {
                        Text(
                            text = stringResource(
                                if (snapshot.isUp) R.string.detail_currently_up
                                else R.string.detail_currently_down
                            ),
                            color = MoonColors.muted.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = if (settings.showRiseSet) 6.dp else 0.dp),
                        )
                    }
                }
            }
        }

        AppSection.SKY_PATH -> {
            if (settings.showSkyPath && snapshot.skyPath.size >= 2) {
                Card {
                    SkyPathChart(
                        points = snapshot.skyPath,
                        pass = snapshot.pass,
                        now = snapshot.calculatedAt,
                        zone = snapshot.zone,
                        force24Hour = settings.force24HourTime,
                    )
                }
            }
        }

        AppSection.TIMING -> {
            if (settings.timingCardHasContent) {
                Card {
                    if (settings.showNextFullMoon) {
                        InfoRow(
                            stringResource(R.string.detail_next_full_moon),
                            countdownText(
                                context = context,
                                target = snapshot.nextFullMoon,
                                zone = snapshot.zone,
                                now = snapshot.calculatedAt,
                            ),
                            detail = phaseTimesText(
                                context = context,
                                zone = snapshot.zone,
                                riseTime = snapshot.nextFullMoonRise,
                                peakTime = snapshot.nextFullMoonPeak,
                                force24Hour = settings.force24HourTime,
                            ),
                        )
                    }
                    // Each name has its own toggle, checked here rather than in the
                    // snapshot so unticking one does not force a recompute.
                    snapshot.nextFullMoonSlot
                        ?.takeIf { settings.valueOf(namedMoonKey(it)) }
                        ?.let { slot ->
                            InfoRow(
                                stringResource(R.string.detail_known_as),
                                moonName(context, settings.moonNameSet, slot),
                            )
                        }
                    if (settings.showNextNewMoon) {
                        InfoRow(
                            stringResource(R.string.detail_next_new_moon),
                            countdownText(
                                context = context,
                                target = snapshot.nextNewMoon,
                                zone = snapshot.zone,
                                now = snapshot.calculatedAt,
                            ),
                            detail = phaseTimesText(
                                context = context,
                                zone = snapshot.zone,
                                riseTime = snapshot.nextNewMoonRise,
                                peakTime = snapshot.nextNewMoonPeak,
                                force24Hour = settings.force24HourTime,
                            ),
                        )
                    }
                    if (settings.showAge) {
                        InfoRow(
                            stringResource(R.string.detail_age),
                            stringResource(
                                R.string.detail_age_value,
                                "%.1f".format(snapshot.phase.ageDays),
                            ),
                        )
                    }
                }
            }
        }

        AppSection.DISTANCE -> {
            if (settings.showDistance) {
                Card {
                    InfoRow(
                        stringResource(R.string.detail_distance),
                        stringResource(
                            R.string.detail_distance_value,
                            "%,d".format(snapshot.distance.distanceKmRounded),
                        ),
                    )
                    InfoRow(
                        stringResource(R.string.detail_apparent_size),
                        stringResource(
                            R.string.detail_apparent_size_value,
                            "%.1f".format(snapshot.distance.apparentDiameterArcmin),
                        ),
                    )
                    if (snapshot.distance.isNearPerigee || snapshot.distance.isNearApogee) {
                        Text(
                            text = stringResource(
                                if (snapshot.distance.isNearPerigee) R.string.detail_near_perigee
                                else R.string.detail_near_apogee
                            ),
                            color = MoonColors.muted.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        AppSection.SUN_SIGN -> {
            // Both systems can be shown at once, precisely because they
            // disagree — that disagreement is the interesting part.
            if (settings.showSunSignSection &&
                (settings.sunSignTropical || settings.sunSignAstronomical)
            ) {
                // With one system on screen "Traditional" alone says nothing
                // about what it describes; the pair of labels is only needed
                // once there are two rows to tell apart.
                val bothShown = settings.sunSignTropical && settings.sunSignAstronomical

                Card {
                    if (settings.sunSignTropical) {
                        val sign = snapshot.sunSign
                        val (from, to) = SunZodiac.conventionalDates(sign)
                        InfoRow(
                            label = stringResource(
                                if (bothShown) R.string.detail_sun_sign_traditional
                                else R.string.detail_sun_sign
                            ),
                            value = "${sign.symbol}  ${sign.displayName(context)}",
                            detail = monthDayRange(context, from, to),
                        )
                        Text(
                            text = context.getString(sign.sunDescriptionRes),
                            color = MoonColors.muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    if (settings.sunSignAstronomical) {
                        val constellation = snapshot.sunConstellation
                        InfoRow(
                            label = stringResource(
                                if (bothShown) R.string.detail_sun_sign_astronomical
                                else R.string.detail_sun_sign
                            ),
                            value = "${constellation.symbol}  ${constellation.displayName(context)}",
                            detail = monthDayRange(context, constellation.start, constellation.end),
                        )
                        Text(
                            text = constellation.description(context),
                            color = MoonColors.muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    // Only worth explaining when both are on screen disagreeing
                    // with each other.
                    if (settings.sunSignTropical && settings.sunSignAstronomical) {
                        Text(
                            text = stringResource(R.string.detail_sun_sign_caveat),
                            color = MoonColors.muted.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        AppSection.MOON_SIGN -> {
            if (settings.showMoonSignSection) {

                Card {
                    InfoRow(
                        label = stringResource(R.string.detail_moon_sign),
                        value = "${snapshot.sign.sign.symbol}  ${snapshot.sign.formatted(context)}",
                    )
                    Text(
                        text = snapshot.sign.sign.description(context),
                        color = MoonColors.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.detail_zodiac_caveat),
                        color = MoonColors.muted.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Rearranging mode, shown in place of the main screen.
 *
 * Compact rows rather than the real cards: five full-size cards do not fit on
 * one screen, and a list that scrolls while you drag is far harder to aim. The
 * order here is the order there.
 */
@Composable
private fun RearrangeContent(
    order: List<AppSection>,
    onReorder: (List<AppSection>) -> Unit,
    onDone: () -> Unit,
) {
    // Dragging mutates a local copy and only reports the result on release, so
    // a half-finished drag never gets written to storage.
    var working by remember(order) { mutableStateOf(order) }
    // Tracked by section rather than by index: the index of the row under the
    // finger changes every time a swap happens, and a gesture keyed on it gets
    // torn down mid-drag.
    var draggingSection by remember { mutableStateOf<AppSection?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val handleLabel = stringResource(R.string.cd_drag_handle)
    val rowHeight = 64.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.rearrange_title),
                color = MoonColors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.rearrange_done),
                color = MoonColors.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Text(
            text = stringResource(R.string.rearrange_hint),
            color = MoonColors.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        working.forEach { section ->
            // Children of a Column are identified by position, so reordering
            // the list would otherwise dispose the row under the finger and
            // recreate it somewhere else — taking the in-flight gesture with
            // it. Keying by section makes Compose move the existing node.
            key(section) {
            val isDragging = section == draggingSection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(vertical = 4.dp)
                    // The dragged row follows the finger; the rest stay put and
                    // simply swap places underneath it.
                    .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                    .zIndex(if (isDragging) 1f else 0f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isDragging) MoonColors.surface
                        else MoonColors.surface.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2261",
                    color = MoonColors.accent,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .semantics { contentDescription = handleLabel }
                        .padding(end = 14.dp)
                        // Keyed on the section alone. Keying on the list as
                        // well would restart this detector on every swap, which
                        // cancels the drag after a single step and strands the
                        // row wherever it happened to be.
                        .pointerInput(section) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingSection = section
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggingSection = null
                                    dragOffset = 0f
                                    onReorder(working)
                                },
                                onDragCancel = {
                                    draggingSection = null
                                    dragOffset = 0f
                                },
                            ) { change, drag ->
                                change.consume()
                                dragOffset += drag.y

                                // Swap once the finger has travelled a whole
                                // row, then carry the remainder so the next
                                // swap is measured from the new position.
                                val current = working.indexOf(section)
                                val steps = (dragOffset / rowHeightPx).roundToInt()
                                if (current >= 0 && steps != 0) {
                                    val target = (current + steps).coerceIn(working.indices)
                                    if (target != current) {
                                        working = working.toMutableList().apply {
                                            add(target, removeAt(current))
                                        }
                                        dragOffset -= (target - current) * rowHeightPx
                                    }
                                }
                            }
                        },
                )
                Text(
                    text = stringResource(section.displayNameRes),
                    color = MoonColors.text,
                    fontSize = 16.sp,
                )
            }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MoonColors.surface.copy(alpha = 0.55f))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun InfoRow(label: String, value: String, detail: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // The label keeps its natural width; the value takes the rest and
            // aligns right, so a long value wraps instead of shunting the label.
            Text(label, color = MoonColors.muted, fontSize = 14.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                color = MoonColors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        // Secondary line, e.g. "rise 5:37 PM · peak 12:10 AM", which is too
        // long to sit beside the label.
        detail?.let {
            Text(
                text = it,
                color = MoonColors.muted,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}
