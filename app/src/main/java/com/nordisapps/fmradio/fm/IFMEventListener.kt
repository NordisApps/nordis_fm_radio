package com.nordisapps.fmradio.fm

import android.os.IInterface

interface IFMEventListener : IInterface {
    fun onRadioEnabled()

    fun onRadioDataSystemReceived(
        freq: Long,
        channelName: String?,
        radioText: String?
    )

    fun onScanStarted()

    fun onChannelFound(freq: Long)

    fun onScanFinished(freqs: LongArray)

    fun onScanStopped(freqs: LongArray)

    fun onTuned(freq: Long)

    fun onHeadsetDisconnected()
}