package com.nordisapps.fmradio.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nordisapps.fmradio.ui.FrequencyBand
import com.nordisapps.fmradio.ui.TuningStep
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "fm_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        private val TUNING_STEP_KEY = stringPreferencesKey("tuning_step")
        private val FREQUENCY_BAND_KEY = stringPreferencesKey("frequency_band")
        private val RDS_ENABLED_KEY = booleanPreferencesKey("rds_enabled")
        private val MONO_MODE_KEY = booleanPreferencesKey("mono_mode")
        private val SOFT_MUTE_ENABLED_KEY = booleanPreferencesKey("soft_mute_enabled")
        private val LAST_FREQUENCY_KEY = doublePreferencesKey("last_frequency")
    }

    suspend fun getSettings(): FmSettings {
        val prefs = context.settingsDataStore.data.first()
        val frequencyBand = prefs[FREQUENCY_BAND_KEY]?.let { name ->
            FrequencyBand.entries.find { it.name == name }
        } ?: FrequencyBand.STANDARD

        val lastFrequency = prefs[LAST_FREQUENCY_KEY]
        val validLastFrequency = lastFrequency?.takeIf {
            it in frequencyBand.minMhz.toDouble()..frequencyBand.maxMhz.toDouble()
        }

        return FmSettings(
            tuningStep = prefs[TUNING_STEP_KEY]?.let { name ->
                TuningStep.entries.find { it.name == name }
            } ?: TuningStep.STEP_100_KHZ,
            frequencyBand = frequencyBand,
            isRdsEnabled = prefs[RDS_ENABLED_KEY] ?: true,
            isMonoMode = prefs[MONO_MODE_KEY] ?: false,
            isSoftMuteEnabled = prefs[SOFT_MUTE_ENABLED_KEY] ?: true,
            lastFrequency = validLastFrequency ?: frequencyBand.minMhz.toDouble()
        )
    }

    suspend fun saveTuningStep(step: TuningStep) {
        context.settingsDataStore.edit { prefs -> prefs[TUNING_STEP_KEY] = step.name }
    }

    suspend fun saveFrequencyBand(band: FrequencyBand) {
        context.settingsDataStore.edit { prefs -> prefs[FREQUENCY_BAND_KEY] = band.name }
    }

    suspend fun saveRdsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[RDS_ENABLED_KEY] = enabled }
    }

    suspend fun saveMonoMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[MONO_MODE_KEY] = enabled }
    }

    suspend fun saveSoftMuteEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[SOFT_MUTE_ENABLED_KEY] = enabled }
    }

    suspend fun saveLastFrequency(frequency: Double) {
        context.settingsDataStore.edit { prefs -> prefs[LAST_FREQUENCY_KEY] = frequency }
    }
}

data class FmSettings(
    val tuningStep: TuningStep,
    val frequencyBand: FrequencyBand,
    val isRdsEnabled: Boolean,
    val isMonoMode: Boolean,
    val isSoftMuteEnabled: Boolean,
    val lastFrequency: Double
)