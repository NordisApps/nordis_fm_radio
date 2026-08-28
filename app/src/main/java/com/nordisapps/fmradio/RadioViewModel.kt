package com.nordisapps.fmradio

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nordisapps.fmradio.data.FavoriteStationsDataStore
import com.nordisapps.fmradio.data.StationsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val radioManager by lazy { FmRadioManager(getApplication()) }
    private val fmRadioRecorder by lazy { FmRadioRecorder() }
    private val stationsDataStore by lazy { StationsDataStore(getApplication()) }
    private val favoriteStationsDataStore by lazy { FavoriteStationsDataStore(getApplication()) }
    private var recordingTimerJob: Job? = null
    private var recordingSeconds = 0
    var uiState by mutableStateOf(RadioUiState())
        private set

    init {
        setupRadioCallbacks()
        loadPersistedStations()
    }

    private fun checkReadiness(): RadioReadiness {
        if (!radioManager.isServiceAvailable()) {
            return RadioReadiness.ServiceUnavailable
        }
        if (!radioManager.isHeadsetConnected()) {
            return RadioReadiness.HeadsetRequired
        }
        return RadioReadiness.Ready
    }

    private fun RadioReadiness.toMessage(): String? = when (this) {
        RadioReadiness.Ready -> null
        RadioReadiness.ServiceUnavailable -> "FM-радио не поддерживается на этом устройстве"
        RadioReadiness.HeadsetRequired -> "Подключите наушники для приёма FM-радио"
    }

    fun clearReadinessMessage() {
        uiState = uiState.copy(readinessMessage = null)
    }

    private fun setupRadioCallbacks() {
        radioManager.onStationNameReceived = { updateStationName(it) }
        radioManager.onRdsCleared = { clearRds() }
        radioManager.onRadioTextReceived = { updateRadioText(it) }

        radioManager.onScanStarted = {
            updateScanning(true)
            updateScannedStations(emptyList())
        }

        radioManager.onChannelFound = { freq ->
            updateScannedStations(uiState.scannedStations + freq)
        }

        radioManager.onScanFinished = {
            updateScanning(false)
            viewModelScope.launch {
                val stations = withContext(Dispatchers.IO) { radioManager.getLastScanResult() }
                if (stations.isNotEmpty()) updateScannedStations(stations)
                updateShowScannedStations(true)
            }
        }

        radioManager.onScanStopped = { stations ->
            updateScanning(false)
            updateScannedStations(stations)
        }

        radioManager.onHeadsetDisconnected = {
            viewModelScope.launch(Dispatchers.Main) {
                updatePlaying(false)
                clearRds()
                getApplication<Application>().stopService(
                    Intent(getApplication(), FmRadioForegroundService::class.java)
                )
                uiState = uiState.copy(readinessMessage = "Наушники отключены — радио остановлено")
            }
        }
    }

    private fun loadPersistedStations() {
        viewModelScope.launch {
            updateSavedStations(stationsDataStore.getSavedStations())
            setFavoriteStations(favoriteStationsDataStore.getFavoriteStations())
        }
    }

    private fun startRadioService(frequency: Double) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, FmRadioForegroundService::class.java).apply {
            putExtra(FmRadioForegroundService.EXTRA_FREQUENCY, frequency.toString())
        }
        context.startForegroundService(serviceIntent)
    }

    private fun applyTune(tuneAction: suspend () -> Double) {
        viewModelScope.launch {
            val tuned = withContext(Dispatchers.IO) { tuneAction() }
            updateCurrentFrequency(tuned.toString())
            if (uiState.isPlaying) startRadioService(tuned)
        }
    }

    private fun updateRecordingTime() {
        val hours = recordingSeconds / 3600
        val minutes = (recordingSeconds % 3600) / 60
        val seconds = recordingSeconds % 60

        val formatted = if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }

        uiState = uiState.copy(recordingTime = formatted)
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()

        recordingTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000.milliseconds)

                if (!uiState.isRecordingPaused) {
                    recordingSeconds++
                    updateRecordingTime()
                }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        recordingSeconds = 0

        uiState = uiState.copy(
            recordingTime = "00:00"
        )
    }

    fun updateStationName(stationName: String) {
        uiState = uiState.copy(
            stationName = stationName
        )
    }

    fun updateRadioText(radioText: String) {
        uiState = uiState.copy(
            radioText = radioText
        )
    }

    fun clearRds() {
        uiState = uiState.copy(
            stationName = "",
            radioText = ""
        )
    }

    fun updateCurrentFrequency(frequency: String) {
        uiState = uiState.copy(
            currentFrequency = frequency
        )
    }

    fun updatePlaying(isPlaying: Boolean) {
        uiState = uiState.copy(
            isPlaying = isPlaying
        )
    }

    fun updateSpeakerOn(isSpeakerOn: Boolean) {
        uiState = uiState.copy(
            isSpeakerOn = isSpeakerOn
        )
    }

    fun updateScanning(isScanning: Boolean) {
        uiState = uiState.copy(
            isScanning = isScanning
        )
    }

    fun updateScannedStations(stations: List<Double>) {
        uiState = uiState.copy(scannedStations = stations)
    }

    fun updateSavedStations(stations: List<Double>) {
        uiState = uiState.copy(savedStations = stations)
    }

    fun updateShowScannedStations(show: Boolean) {
        uiState = uiState.copy(showScannedStations = show)
    }

    fun toggleFavorite(frequency: Double) {
        val current = uiState.favoriteStations
        uiState = uiState.copy(
            favoriteStations = if (current.contains(frequency)) {
                current - frequency
            } else {
                current + frequency
            }
        )
    }

    fun setFavoriteStations(stations: Set<Double>) {
        uiState = uiState.copy(favoriteStations = stations)
    }

    fun updateRecording(isRecording: Boolean) {
        uiState = uiState.copy(
            isRecording = isRecording
        )

        if (isRecording) {
            recordingSeconds = 0
            updateRecordingTime()
            startRecordingTimer()
        } else {
            stopRecordingTimer()
        }
    }

    fun updateRecordingPaused(isRecordingPaused: Boolean) {
        uiState = uiState.copy(
            isRecordingPaused = isRecordingPaused
        )
    }

    fun tuneToStation(frequency: Double) {
        applyTune { radioManager.tuneSafe(frequency) }
    }

    fun seekDown() {
        applyTune { radioManager.seekDown() }
    }

    fun seekUp() {
        applyTune { radioManager.seekUp() }
    }

    fun onScaleFrequencyChange(newFrequency: Double) {
        applyTune { radioManager.tuneSafe(newFrequency) }
    }

    fun togglePower() {
        viewModelScope.launch {
            if (uiState.isPlaying) {
                withContext(Dispatchers.IO) { radioManager.stop() }
                updatePlaying(false)
                clearRds()
                getApplication<Application>().stopService(
                    Intent(getApplication(), FmRadioForegroundService::class.java)
                )
            } else {
                val readiness = withContext(Dispatchers.IO) { checkReadiness() }
                val message = readiness.toMessage()
                if (message != null) {
                    uiState = uiState.copy(readinessMessage = message)
                    return@launch
                }
                val frequency = uiState.currentFrequency.toDoubleOrNull() ?: 87.5
                val tuned = withContext(Dispatchers.IO) {
                    radioManager.play()
                    radioManager.tuneSafe(frequency)
                }
                updateCurrentFrequency(tuned.toString())
                updatePlaying(true)
                startRadioService(tuned)
            }
        }
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            val newState = !uiState.isSpeakerOn
            val result = withContext(Dispatchers.IO) { radioManager.setSpeakerOn(newState) }
            updateSpeakerOn(result)
        }
    }

    fun startScan() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { radioManager.scan() }
        }
    }

    fun confirmScannedStations() {
        val merged = (uiState.savedStations + uiState.scannedStations).distinct().sorted()
        updateSavedStations(merged)
        updateShowScannedStations(false)
        viewModelScope.launch {
            stationsDataStore.saveStations(merged)
        }
    }

    fun toggleFavoriteAndPersist(frequency: Double) {
        toggleFavorite(frequency)
        viewModelScope.launch {
            favoriteStationsDataStore.saveFavoriteStations(uiState.favoriteStations)
        }
    }

    fun onRecordClick() {
        try {
            val psName = uiState.stationName
            fmRadioRecorder.startRecording(getApplication(), psName)
            updateRecording(true)
            updateRecordingPaused(false)
        } catch (e: Exception) {
            Log.e("RadioViewModel", "startRecording failed: ${e.message}")
        }
    }

    fun onRecordPauseClick() {
        fmRadioRecorder.pauseOrResumeRecording()
        updateRecordingPaused(fmRadioRecorder.isRecordingPaused)
    }

    fun onRecordStopClick() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fmRadioRecorder.stopRecording(getApplication())
            }
        }
        updateRecording(false)
        updateRecordingPaused(false)
    }

    fun onRecordCancelClick() {
        fmRadioRecorder.cancelRecording(getApplication())
        updateRecording(false)
        updateRecordingPaused(false)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            radioManager.onStationNameReceived = null
            radioManager.onRadioTextReceived = null
            radioManager.onRdsCleared = null
            if (fmRadioRecorder.isRecording) {
                fmRadioRecorder.stopRecording(getApplication())
            }
            radioManager.stop()
            Log.d("RadioViewModel", "RADIO OFF ON CLEARED")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}