package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.data.AppFont
import dev.mahourigan.moonwidget.data.TextSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Typeface and text size, together on one screen.
 *
 * Both are the same kind of decision — how the words look — and splitting them
 * across two sub-screens would mean two trips to compare a size against a face.
 * Every option renders itself, so the choice is made by looking rather than by
 * reading a label and guessing.
 */
@Composable
fun TypographyScreen(
    font: AppFont,
    size: TextSize,
    onSelectFont: (AppFont) -> Unit,
    onSelectSize: (TextSize) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonColors.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        ScreenTopBar(title = stringResource(R.string.settings_typography), onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionLabel(stringResource(R.string.typography_typeface)) }

            items(AppFont.entries, key = { "font_${it.name}" }) { option ->
                ChoiceRow(
                    selected = option == font,
                    onClick = { onSelectFont(option) },
                ) {
                    Column {
                        Text(
                            text = stringResource(option.displayNameRes),
                            color = MoonColors.text,
                            fontSize = 16.sp,
                            fontWeight = if (option == font) FontWeight.Medium else FontWeight.Normal,
                            // Each name is set in the face it names.
                            fontFamily = option.toFontFamily(),
                        )
                        Text(
                            text = stringResource(R.string.typography_sample),
                            color = MoonColors.muted,
                            fontSize = 13.sp,
                            fontFamily = option.toFontFamily(),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel(stringResource(R.string.typography_size))
            }

            items(TextSize.entries, key = { "size_${it.name}" }) { option ->
                ChoiceRow(
                    selected = option == size,
                    onClick = { onSelectSize(option) },
                ) {
                    // Each row previews itself by undoing the size currently in
                    // force and applying its own, so the four are comparable
                    // side by side rather than all drawn at the active size.
                    val outer = LocalDensity.current
                    val preview = Density(
                        density = outer.density,
                        fontScale = outer.fontScale / size.scale * option.scale,
                    )
                    CompositionLocalProvider(LocalDensity provides preview) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(option.displayNameRes),
                                color = MoonColors.text,
                                fontSize = 16.sp,
                                fontWeight =
                                    if (option == size) FontWeight.Medium else FontWeight.Normal,
                            )
                            Text(
                                text = "${(option.scale * 100).toInt()}%",
                                color = MoonColors.muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MoonColors.surface else MoonColors.surface.copy(alpha = 0.35f)
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MoonColors.accent else MoonColors.surface.copy(alpha = 0f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) { content() }
        if (selected) Text("●", color = MoonColors.accent, fontSize = 12.sp)
    }
}
