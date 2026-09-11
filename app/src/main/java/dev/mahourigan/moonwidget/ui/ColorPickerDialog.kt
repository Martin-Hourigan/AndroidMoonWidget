package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.data.ThemeRole
import dev.mahourigan.moonwidget.data.hexOf
import dev.mahourigan.moonwidget.data.hsvToRgb
import dev.mahourigan.moonwidget.data.parseHex
import dev.mahourigan.moonwidget.data.rgbToHsv
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Pick one colour, by hue/saturation/brightness or by typing a hex code.
 *
 * HSB rather than RGB sliders: "make it a bit darker" is one slider here and
 * three coordinated ones in RGB. The hex field is kept for anyone who already
 * knows the value they want, and the two stay in step in both directions.
 */
@Composable
fun ColorPickerDialog(
    role: ThemeRole,
    initial: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val start = remember(initial) { rgbToHsv(initial) }
    var hue by remember(initial) { mutableFloatStateOf(start[0]) }
    var saturation by remember(initial) { mutableFloatStateOf(start[1]) }
    var value by remember(initial) { mutableFloatStateOf(start[2]) }

    // Only what the user is mid-way through typing; the sliders stay canonical.
    var typed by remember(initial) { mutableStateOf<String?>(null) }

    val current = hsvToRgb(hue, saturation, value)
    val pending = typed
    val hexText = pending ?: hexOf(current).removePrefix("#")
    val typedValid = pending == null || parseHex(pending) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoonColors.surface,
        titleContentColor = MoonColors.text,
        textContentColor = MoonColors.muted,
        title = { Text(stringResource(role.displayNameRes)) },
        text = {
            Column {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(current))
                        .border(1.dp, MoonColors.muted, RoundedCornerShape(12.dp))
                )

                Spacer(Modifier.height(12.dp))

                // Each track is painted with the colours that position would
                // actually give, so a colour can be aimed at rather than hunted
                // for. Saturation and brightness are floored for the hue strip
                // only, so it stays a visible rainbow even at 0% of either.
                ChannelSlider(
                    label = stringResource(R.string.colour_hue),
                    valueText = "${hue.roundToInt()}°",
                    value = hue,
                    range = 0f..360f,
                    thumbColor = Color(current),
                    track = (0..6).map {
                        Color(
                            hsvToRgb(
                                it * 60f,
                                saturation.coerceAtLeast(0.5f),
                                value.coerceAtLeast(0.6f),
                            )
                        )
                    },
                    onChange = { hue = it; typed = null },
                )
                ChannelSlider(
                    label = stringResource(R.string.colour_saturation),
                    valueText = "${(saturation * 100).roundToInt()}%",
                    value = saturation,
                    range = 0f..1f,
                    thumbColor = Color(current),
                    track = listOf(
                        Color(hsvToRgb(hue, 0f, value.coerceAtLeast(0.6f))),
                        Color(hsvToRgb(hue, 1f, value.coerceAtLeast(0.6f))),
                    ),
                    onChange = { saturation = it; typed = null },
                )
                ChannelSlider(
                    label = stringResource(R.string.colour_brightness),
                    valueText = "${(value * 100).roundToInt()}%",
                    value = value,
                    range = 0f..1f,
                    thumbColor = Color(current),
                    track = listOf(
                        Color(hsvToRgb(hue, saturation, 0f)),
                        Color(hsvToRgb(hue, saturation, 1f)),
                    ),
                    onChange = { value = it; typed = null },
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { entry ->
                        val cleaned = entry.removePrefix("#").take(6).uppercase()
                        typed = cleaned
                        parseHex(cleaned)?.let { parsed ->
                            val hsv = rgbToHsv(parsed)
                            // A grey has no meaningful hue, and a black no
                            // meaningful saturation; keep the slider where it
                            // was rather than snapping it to zero.
                            if (hsv[1] > 0f) hue = hsv[0]
                            if (hsv[2] > 0f) saturation = hsv[1]
                            value = hsv[2]
                            typed = null
                        }
                    },
                    isError = !typedValid,
                    singleLine = true,
                    prefix = { Text("#", color = MoonColors.muted) },
                    label = { Text(stringResource(R.string.colour_hex)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MoonColors.text,
                        unfocusedTextColor = MoonColors.text,
                        focusedContainerColor = MoonColors.background,
                        unfocusedContainerColor = MoonColors.background,
                        focusedLabelColor = MoonColors.muted,
                        unfocusedLabelColor = MoonColors.muted,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text(stringResource(R.string.dialog_save), color = MoonColors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = MoonColors.muted)
            }
        },
    )
}

/**
 * One HSB channel, with the track painted as the gradient of results.
 *
 * @param track two or more stops spanning the range, left to right.
 * @param thumbColor the colour currently chosen, so the thumb is a live sample.
 */
// The track/thumb slots are still experimental; the alternative is a plain grey
// track, which for a colour picker is barely usable.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    thumbColor: Color,
    track: List<Color>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = MoonColors.muted, fontSize = 12.sp)
            Text(valueText, color = MoonColors.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            track = {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(track))
                )
            },
            thumb = {
                // Ringed in the text colour so it stays findable against any
                // point on the gradient, including one that matches it.
                Spacer(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(thumbColor)
                        .border(3.dp, MoonColors.text, CircleShape)
                )
            },
        )
    }
}
