package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.astronomy.MoonNameSet
import dev.mahourigan.moonwidget.astronomy.MoonSlot
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Picker for which tradition the monthly moon names come from.
 *
 * A sub-screen rather than an inline control: each set needs a sample and a
 * short note on where it actually comes from, which will not fit in a row.
 * Some of these are widely misattributed, so the provenance is part of the
 * choice rather than a footnote.
 */
@Composable
fun MoonNameSetScreen(
    selected: MoonNameSet,
    onSelect: (MoonNameSet) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonColors.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        ScreenTopBar(title = stringResource(R.string.settings_name_set), onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(MoonNameSet.entries, key = { it.name }) { set ->
                val isSelected = set == selected

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MoonColors.surface
                            else MoonColors.surface.copy(alpha = 0.35f)
                        )
                        .clickable { onSelect(set) }
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(set.displayNameRes),
                            color = MoonColors.text,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        )
                        if (isSelected) Text("●", color = MoonColors.accent, fontSize = 12.sp)
                    }

                    // The chosen set lists every month; the others show three
                    // names, which is enough to tell them apart without turning
                    // the screen into four long lists.
                    Text(
                        text = if (isSelected) allNames(context, set) else sampleOf(context, set),
                        color = MoonColors.text.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    Text(
                        text = stringResource(set.noteRes),
                        color = MoonColors.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/** Every month plus the harvest moon, one per line. */
private fun allNames(context: android.content.Context, set: MoonNameSet): String =
    MoonSlot.entries.joinToString("\n") { slot ->
        context.getString(
            R.string.name_set_line,
            context.getString(slot.slotLabelRes),
            moonName(context, set, slot),
        )
    }

/** January, June and October — enough to tell the sets apart at a glance. */
private fun sampleOf(context: android.content.Context, set: MoonNameSet): String =
    listOf(MoonSlot.JANUARY, MoonSlot.JUNE, MoonSlot.OCTOBER)
        .joinToString(" · ") { moonName(context, set, it) }
