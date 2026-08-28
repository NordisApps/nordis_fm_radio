package com.nordisapps.fmradio

data class RadioUiState(
    val readinessMessage: String? = null,
    val stationName: String = "",
    val radioText: String = "",
    val currentFrequency: String = "",
    val isPlaying: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isScanning: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingTime: String = "00:00",
    val scannedStations: List<Double> = emptyList(),
    val savedStations: List<Double> = emptyList(),
    val showScannedStations: Boolean = false,
    val favoriteStations: Set<Double> = emptySet()
)