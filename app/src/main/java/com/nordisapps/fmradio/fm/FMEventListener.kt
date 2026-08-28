package com.nordisapps.fmradio.fm

import android.util.Log

open class FMEventListener : IFMEventListenerStub() {
    override fun onRadioEnabled() {
        Log.d("FMTEST", "RADIO ENABLED")
    }

    override fun onRadioDataSystemReceived(
        freq: Long,
        channelName: String?,
        radioText: String?
    ) {
        Log.d("FMTEST", "RDS freq=$freq station=$channelName text=$radioText")
    }

    override fun onScanStarted() {
        Log.d("FMTEST", "SCAN STARTED")
    }

    override fun onChannelFound(freq: Long) {
        Log.d("FMTEST", "CHANNEL FOUND: $freq")
    }

    override fun onScanFinished(freqs: LongArray) {
        Log.d("FMTEST", "SCAN FINISHED: ${freqs.toList()}")
    }

    override fun onScanStopped(freqs: LongArray) {
        Log.d("FMTEST", "SCAN STOPPED: ${freqs.toList()}")
    }

    override fun onTuned(freq: Long) {
        Log.d("FMTEST", "TUNED: $freq")
    }

    override fun onHeadsetDisconnected() {
        Log.d("FMTEST", "HEADSET DISCONNECTED")
    }
}