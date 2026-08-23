package com.nordisapps.fmradio.fm

import android.util.Log

class RadioEventListener(
    private val onStationNameReceived: (String) -> Unit,
    private val onRadioTextReceived: (String) -> Unit,
    private val onRdsCleared: () -> Unit,
    private val onScanStartedCallback: () -> Unit,
    private val onChannelFound: (Double) -> Unit,
    private val onScanFinished: () -> Unit,
    private val onScanStopped: (List<Double>) -> Unit
) : FMEventListener() {
    private var lastTunedFreq: Long = -1L

    override fun onTuned(freq: Long) {
        if (freq != lastTunedFreq) {
            lastTunedFreq = freq
            onRdsCleared()
        }
    }

    override fun onRadioDataSystemReceived(
        freq: Long,
        channelName: String?,
        radioText: String?
    ) {
        if (!channelName.isNullOrBlank()) {
            onStationNameReceived(channelName)
        }
        if (!radioText.isNullOrBlank()) {
            onRadioTextReceived(radioText)
        }

        Log.d("FMTEST", "RDS = $channelName | $radioText")
    }

    override fun onScanStarted() {
        Log.d("FMTEST", "SCAN STARTED")
        onScanStartedCallback()
    }

    override fun onChannelFound(freq: Long) {
        Log.d("FMTEST", "CHANNEL FOUND: $freq")
        onChannelFound(freq / 1000.0)
    }

    override fun onScanFinished(freqs: LongArray) {
        Log.d("FMTEST", "SCAN FINISHED (event only, ignoring empty array)")
        onScanFinished()
    }

    override fun onScanStopped(freqs: LongArray) {
        Log.d("FMTEST", "SCAN STOPPED: ${freqs.toList()}")
        onScanStopped(freqs.map { it / 1000.0 })
    }
}