package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.R
import dev.mahourigan.moonwidget.data.LocationSource
import dev.mahourigan.moonwidget.data.PresetLocations
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Location picker: automatic, a bundled city, or your own coordinates.
 */
@Composable
fun LocationScreen(
    state: MoonUiState,
    hasPermission: Boolean,
    onSelect: (LocationSource) -> Unit,
    onSaveCustom: (LocationSource.Custom) -> Unit,
    onDelete: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showCustomForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonColors.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        ScreenTopBar(title = stringResource(R.string.location_title), onBack = onBack)

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item {
                SectionLabel(stringResource(R.string.location_section_automatic))
                OptionRow(
                    title = stringResource(R.string.location_use_my_location),
                    subtitle = if (hasPermission) {
                        stringResource(R.string.location_auto_available)
                    } else {
                        stringResource(R.string.location_auto_needs_permission)
                    },
                    selected = state.source is LocationSource.Auto,
                    onClick = {
                        if (hasPermission) onSelect(LocationSource.Auto) else onRequestPermission()
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            if (state.savedPlaces.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.location_section_saved)) }
                items(state.savedPlaces, key = { it.label }) { place ->
                    OptionRow(
                        title = place.label,
                        subtitle = formatCoordinates(place.latitude, place.longitude),
                        selected = (state.source as? LocationSource.Custom)?.label == place.label,
                        onClick = { onSelect(place) },
                        trailing = {
                            TextButton(onClick = { onDelete(place.label) }) {
                                Text(stringResource(R.string.location_remove), color = MoonColors.muted, fontSize = 12.sp)
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            item {
                SectionLabel(stringResource(R.string.location_section_add))
                if (showCustomForm) {
                    CustomLocationForm(
                        onCancel = { showCustomForm = false },
                        onSave = {
                            showCustomForm = false
                            onSaveCustom(it)
                        },
                    )
                } else {
                    TextButton(onClick = { showCustomForm = true }) {
                        Text(stringResource(R.string.location_enter_coordinates), color = MoonColors.text)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionLabel(stringResource(R.string.location_section_cities))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.location_search), color = MoonColors.muted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
            }

            items(PresetLocations.search(query), key = { it.id }) { preset ->
                OptionRow(
                    title = preset.name,
                    subtitle = preset.region,
                    selected = (state.source as? LocationSource.Preset)?.id == preset.id,
                    onClick = { onSelect(LocationSource.Preset(preset.id)) },
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun CustomLocationForm(
    onCancel: () -> Unit,
    onSave: (LocationSource.Custom) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    val parsedLatitude = latitude.toDoubleOrNull()
    val parsedLongitude = longitude.toDoubleOrNull()
    val valid = label.isNotBlank() &&
        parsedLatitude != null && parsedLatitude in -90.0..90.0 &&
        parsedLongitude != null && parsedLongitude in -180.0..180.0

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(R.string.location_name), color = MoonColors.muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = { Text(stringResource(R.string.location_latitude), color = MoonColors.muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = fieldColors(),
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = { Text(stringResource(R.string.location_longitude), color = MoonColors.muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = fieldColors(),
            )
        }
        Text(
            text = stringResource(R.string.location_coordinate_hint),
            color = MoonColors.muted.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.location_cancel), color = MoonColors.muted)
            }
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        LocationSource.Custom(
                            label = label.trim(),
                            latitude = parsedLatitude!!,
                            longitude = parsedLongitude!!,
                        )
                    )
                },
            ) {
                Text(stringResource(R.string.location_save), color = if (valid) MoonColors.text else MoonColors.muted)
            }
        }
    }
}

/** Shared by the location and settings screens. */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MoonColors.muted.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MoonColors.surface else MoonColors.surface.copy(alpha = 0.35f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MoonColors.text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            )
            Text(subtitle, color = MoonColors.muted, fontSize = 12.sp)
        }
        if (selected) {
            Text("●", color = MoonColors.accent, fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MoonColors.text,
    unfocusedTextColor = MoonColors.text,
    focusedContainerColor = MoonColors.surface.copy(alpha = 0.35f),
    unfocusedContainerColor = MoonColors.surface.copy(alpha = 0.35f),
    cursorColor = MoonColors.text,
)

@Composable
private fun formatCoordinates(latitude: Double, longitude: Double): String = stringResource(
    R.string.location_coordinates,
    kotlin.math.abs(latitude),
    stringResource(if (latitude >= 0) R.string.location_north else R.string.location_south),
    kotlin.math.abs(longitude),
    stringResource(if (longitude >= 0) R.string.location_east else R.string.location_west),
)
