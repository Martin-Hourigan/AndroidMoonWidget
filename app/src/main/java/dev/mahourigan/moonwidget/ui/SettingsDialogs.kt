package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Whole-number entry, clamped to a range. */
@Composable
fun NumberDialog(
    title: String,
    initial: Int,
    min: Int,
    max: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoonColors.surface,
        title = { Text(title, color = MoonColors.text, fontSize = 17.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(2) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = dialogFieldColors(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$min–$max",
                    color = MoonColors.muted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { parsed?.let(onConfirm) }) {
                Text(
                    text = stringResource(R.string.dialog_save),
                    color = if (valid) MoonColors.accent else MoonColors.muted,
                )
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
 * Hour and minute entry.
 *
 * Two number fields rather than the platform picker: the platform dialog brings
 * its own theming that fights the rest of this screen, and this only needs to
 * set one time.
 */
@Composable
fun TimeOfDayDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hour by remember { mutableStateOf((initialMinuteOfDay / 60).toString()) }
    var minute by remember {
        mutableStateOf(String.format(Locale.US, "%02d", initialMinuteOfDay % 60))
    }

    val parsedHour = hour.toIntOrNull()
    val parsedMinute = minute.toIntOrNull()
    val valid = parsedHour in 0..23 && parsedMinute in 0..59

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoonColors.surface,
        title = {
            Text(
                text = stringResource(R.string.notification_time_title),
                color = MoonColors.text,
                fontSize = 17.sp,
            )
        },
        text = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(88.dp),
                    colors = dialogFieldColors(),
                )
                Text(
                    text = ":",
                    color = MoonColors.text,
                    fontSize = 20.sp,
                    modifier = Modifier.width(24.dp),
                )
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(88.dp),
                    colors = dialogFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (parsedHour != null && parsedMinute != null) {
                        onConfirm(parsedHour * 60 + parsedMinute)
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.dialog_save),
                    color = if (valid) MoonColors.accent else MoonColors.muted,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = MoonColors.muted)
            }
        },
    )
}

/** 24-hour rendering; the summary row only needs to be unambiguous, not local. */
fun formatMinuteOfDay(minuteOfDay: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

@Composable
private fun dialogFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MoonColors.text,
    unfocusedTextColor = MoonColors.text,
    focusedContainerColor = MoonColors.background,
    unfocusedContainerColor = MoonColors.background,
    cursorColor = MoonColors.text,
)

private operator fun IntRange.contains(value: Int?): Boolean = value != null && value in this
