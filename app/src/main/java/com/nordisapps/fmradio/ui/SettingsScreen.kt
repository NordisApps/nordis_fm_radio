package com.nordisapps.fmradio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    tuningStep: TuningStep,
    onTuningStepChange: (TuningStep) -> Unit,
    frequencyBand: FrequencyBand,
    onFrequencyBandChange: (FrequencyBand) -> Unit,
    isRdsEnabled: Boolean,
    onRdsToggle: (Boolean) -> Unit,
    isMonoMode: Boolean,
    onMonoModeToggle: (Boolean) -> Unit,
    isSoftMuteEnabled: Boolean,
    onSoftMuteToggle: (Boolean) -> Unit
) {
    LazyColumn {
        item { SettingsSectionHeader("Тюнинг") }

        item {
            SettingsDropdownRow(
                title = "Шаг настройки",
                selectedLabel = tuningStep.label,
                options = TuningStep.entries,
                optionLabel = { it.label },
                onOptionSelected = onTuningStepChange
            )
        }

        item {
            SettingsDropdownRow(
                title = "Диапазон частот",
                selectedLabel = frequencyBand.label,
                options = FrequencyBand.entries,
                optionLabel = { it.label },
                onOptionSelected = onFrequencyBandChange
            )
        }

        if (frequencyBand == FrequencyBand.JAPAN_EXTENDED) {
            item {
                Text(
                    text = "На некоторых устройствах этот режим может быть ограничен до 76–90 МГц из-за особенностей прошивки FM-чипа",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
        }

        item { SettingsSectionHeader("Аудио") }

        item {
            SettingsSwitchRow(
                title = "RDS",
                subtitle = "Название станции и радиотекст",
                checked = isRdsEnabled,
                onCheckedChange = onRdsToggle
            )
        }

        item {
            SettingsSwitchRow(
                title = "Моно-режим",
                checked = isMonoMode,
                onCheckedChange = onMonoModeToggle
            )
        }

        item {
            SettingsSwitchRow(
                title = "Плавное приглушение",
                subtitle = "Снижает шум при слабом сигнале",
                checked = isSoftMuteEnabled,
                onCheckedChange = onSoftMuteToggle
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(subtitle) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun <T> SettingsDropdownRow(
    title: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row {
                TextButton(onClick = { isExpanded = true }) {
                    Text(selectedLabel)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(optionLabel(option)) },
                            onClick = {
                                onOptionSelected(option)
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

enum class TuningStep(val label: String, val stepMhz: Float) {
    STEP_50_KHZ("50 кГц", 0.05f),
    STEP_100_KHZ("100 кГц", 0.1f),
    STEP_200_KHZ("200 кГц", 0.2f)
}

enum class FrequencyBand(val label: String, val minMhz: Float, val maxMhz: Float) {
    STANDARD("87.5-108 МГц", 87.5f, 108.0f),
    JAPAN("76-90 МГц", 76.0f, 90.0f),
    JAPAN_EXTENDED("76-108 МГц", 76.0f, 108.0f)
}