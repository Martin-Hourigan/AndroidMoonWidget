package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.astronomy.MoonAppearance
import dev.mahourigan.moonwidget.data.MoonTheme
import dev.mahourigan.moonwidget.data.Settings
import dev.mahourigan.moonwidget.data.ThemeColors
import dev.mahourigan.moonwidget.data.ThemeRole
import dev.mahourigan.moonwidget.data.contrastRatio
import dev.mahourigan.moonwidget.data.hexOf
import dev.mahourigan.moonwidget.data.valueOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Theme picker.
 *
 * Every row previews itself in its own colours rather than in the theme
 * currently applied — a swatch strip tells you far less than seeing the Moon
 * and a line of text as they will actually look.
 */
@Composable
fun ThemeScreen(
    settings: Settings,
    onSelectTheme: (MoonTheme) -> Unit,
    onCustomColor: (ThemeRole, Long) -> Unit,
    onResetCustom: () -> Unit,
    onBack: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<ThemeRole?>(null) }

    editing?.let { role ->
        ColorPickerDialog(
            role = role,
            initial = settings.customTheme.valueOf(role),
            onDismiss = { editing = null },
            onConfirm = {
                onCustomColor(role, it)
                editing = null
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

        ScreenTopBar(title = stringResource(R.string.settings_theme), onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(MoonTheme.entries, key = { it.name }) { theme ->
                // A custom theme previews the user's own colours, not the seed.
                val colors = if (theme.isCustom) settings.customTheme else theme.colors

                ThemeCard(
                    theme = theme,
                    colors = colors,
                    selected = theme == settings.theme,
                    onClick = { onSelectTheme(theme) },
                )
            }

            // The editor stays available whichever theme is applied, so colours
            // can be adjusted and then switched to.
            item(key = "custom_editor") {
                Spacer(Modifier.height(12.dp))
                SectionLabel(stringResource(R.string.theme_custom_colours))

                ThemeRole.entries.forEach { role ->
                    ColorRow(
                        role = role,
                        argb = settings.customTheme.valueOf(role),
                        onClick = { editing = role },
                    )
                }

                ContrastWarnings(settings.customTheme)

                Text(
                    text = stringResource(R.string.theme_reset_custom),
                    color = MoonColors.muted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onResetCustom)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** One selectable theme, drawn in its own colours. */
@Composable
private fun ThemeCard(
    theme: MoonTheme,
    colors: ThemeColors,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val preview = colors.toPalette()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(preview.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) preview.accent else preview.surface,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // A waxing gibbous shows the lit face, the dark limb and the rim
            // all at once, so one preview covers every moon colour.
            MoonShape(
                appearance = MoonAppearance.forPhase(phaseAngle = 125.0),
                diameter = 40.dp,
                litColor = preview.moon,
                shadowColor = preview.moonShadow,
            )
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(theme.displayNameRes),
                    color = preview.text,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                )
                Text(
                    text = stringResource(theme.noteRes),
                    color = preview.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (selected) Text("●", color = preview.accent, fontSize = 12.sp)
        }
    }
}

/**
 * Flag combinations that will read as a broken app rather than a bold choice.
 *
 * Advisory only — nothing is blocked. Someone may well want a moody theme they
 * can barely read, but they should know that is what they picked.
 */
@Composable
private fun ContrastWarnings(colors: ThemeColors) {
    val problems = buildList {
        // 4.5:1 is the WCAG floor for body text; 3:1 is enough for a shape you
        // only need to make out.
        if (contrastRatio(colors.text, colors.background) < 4.5) {
            add(R.string.theme_warning_text)
        }
        if (contrastRatio(colors.moon, colors.background) < 3.0 &&
            contrastRatio(colors.moon, colors.surface) < 3.0
        ) {
            add(R.string.theme_warning_moon)
        }
        if (contrastRatio(colors.accent, colors.background) < 3.0) {
            add(R.string.theme_warning_accent)
        }
    }

    problems.forEach { message ->
        Text(
            text = stringResource(message),
            color = MoonColors.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
        )
    }
}

/** One editable colour in the custom theme. */
@Composable
private fun ColorRow(role: ThemeRole, argb: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(role.displayNameRes), color = MoonColors.text, fontSize = 15.sp)
            Text(stringResource(role.noteRes), color = MoonColors.muted, fontSize = 12.sp)
        }

        Text(
            text = hexOf(argb),
            color = MoonColors.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 10.dp),
        )

        Spacer(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color(argb))
                // An outline keeps a swatch visible when it matches the page.
                .border(1.dp, MoonColors.muted, CircleShape)
        )
    }
}
