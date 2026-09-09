package com.nordisapps.fmradio.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.nordisapps.fmradio.RadioViewModel

@Composable
fun RadioHomeScreen(viewModel: RadioViewModel, snackbarHostState: SnackbarHostState) {
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.readinessMessage) {
        val message = uiState.readinessMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearReadinessMessage()
        }
    }

    RadioPlayerScreen(
        currentFrequency = uiState.currentFrequency,
        stationName = uiState.stationName,
        radioText = uiState.radioText,
        isPlaying = uiState.isPlaying,
        isRecording = uiState.isRecording,
        isRecordingPaused = uiState.isRecordingPaused,
        recordingTime = uiState.recordingTime,
        scannedStations = uiState.scannedStations,
        savedStations = uiState.savedStations,
        favoriteStations = uiState.favoriteStations,
        showScannedStations = uiState.showScannedStations,
        tuningStep = uiState.tuningStep,
        frequencyBand = uiState.frequencyBand,
        onSeekDownClick = viewModel::seekDown,
        onSeekUpClick = viewModel::seekUp,
        onScaleFrequencyChange = viewModel::onScaleFrequencyChange,
        onConfirmScannedStations = viewModel::confirmScannedStations,
        onSavedStationSelected = viewModel::tuneToStation,
        onFavoriteToggle = viewModel::toggleFavoriteAndPersist
    )
}