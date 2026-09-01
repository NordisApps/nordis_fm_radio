package com.nordisapps.fmradio.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.nordisapps.fmradio.RadioViewModel

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RadioHomeScreen(viewModel: RadioViewModel) {
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.readinessMessage) {
        val message = uiState.readinessMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearReadinessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) {
        RadioPlayerScreen(
            currentFrequency = uiState.currentFrequency,
            stationName = uiState.stationName,
            radioText = uiState.radioText,
            isPlaying = uiState.isPlaying,
            isScanning = uiState.isScanning,
            isSpeakerOn = uiState.isSpeakerOn,
            isRecording = uiState.isRecording,
            isRecordingPaused = uiState.isRecordingPaused,
            recordingTime = uiState.recordingTime,
            scannedStations = uiState.scannedStations,
            savedStations = uiState.savedStations,
            favoriteStations = uiState.favoriteStations,
            showScannedStations = uiState.showScannedStations,
            onSeekDownClick = viewModel::seekDown,
            onSeekUpClick = viewModel::seekUp,
            onPowerClick = viewModel::togglePower,
            onSpeakerClick = viewModel::toggleSpeaker,
            onScaleFrequencyChange = viewModel::onScaleFrequencyChange,
            onScanClick = viewModel::startScan,
            onConfirmScannedStations = viewModel::confirmScannedStations,
            onSavedStationSelected = viewModel::tuneToStation,
            onFavoriteToggle = viewModel::toggleFavoriteAndPersist,
            onRecordClick = viewModel::onRecordClick,
            onRecordPauseClick = viewModel::onRecordPauseClick,
            onRecordStopClick = viewModel::onRecordStopClick,
            onRecordCancelClick = viewModel::onRecordCancelClick
        )
    }
}