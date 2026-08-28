package com.nordisapps.fmradio.fm

import android.os.Binder
import android.os.IBinder
import android.os.Parcel

abstract class IFMEventListenerStub :
    Binder(),
    IFMEventListener {
    companion object {
        const val DESCRIPTOR =
            "com.samsung.android.media.fmradio.internal.IFMEventListener"
        const val TRANSACTION_ON_RADIO_ENABLED = 1
        const val TRANSACTION_ON_CHANNEL_FOUND = 3
        const val TRANSACTION_ON_SCAN_STARTED = 4
        const val TRANSACTION_ON_SCAN_STOPPED = 5
        const val TRANSACTION_ON_SCAN_FINISHED = 6
        const val TRANSACTION_ON_TUNED = 7
        const val TRANSACTION_ON_RDS_RECEIVED = 10
        const val TRANSACTION_ON_HEADSET_DISCONNECTED = 9
    }

    init {
        attachInterface(this, DESCRIPTOR)
    }

    override fun asBinder(): IBinder {
        return this
    }

    private fun handleScanResult(data: Parcel, reply: Parcel?, callback: (LongArray) -> Unit) {
        val size = data.readInt()
        val freqs = if (size < 0) LongArray(0) else LongArray(size)
        callback(freqs)
        reply?.writeNoException()
        reply?.writeLongArray(freqs)
    }

    override fun onTransact(
        code: Int,
        data: Parcel,
        reply: Parcel?,
        flags: Int
    ): Boolean {
        if (code == INTERFACE_TRANSACTION) {
            reply?.writeString(DESCRIPTOR)
            return true
        }

        data.enforceInterface(DESCRIPTOR)

        when (code) {
            TRANSACTION_ON_RADIO_ENABLED -> {
                onRadioEnabled()
                return true
            }

            TRANSACTION_ON_CHANNEL_FOUND -> {
                val freq = data.readLong()
                onChannelFound(freq)
                return true
            }

            TRANSACTION_ON_SCAN_STARTED -> {
                onScanStarted()
                return true
            }

            TRANSACTION_ON_SCAN_STOPPED -> {
                handleScanResult(data, reply) { onScanStopped(it) }
                return true
            }

            TRANSACTION_ON_SCAN_FINISHED -> {
                handleScanResult(data, reply) { onScanFinished(it) }
                return true
            }

            TRANSACTION_ON_TUNED -> {
                val freq = data.readLong()
                onTuned(freq)
                return true
            }

            TRANSACTION_ON_RDS_RECEIVED -> {
                val freq = data.readLong()
                val channelName =
                    data.readString()
                val radioText =
                    data.readString()

                onRadioDataSystemReceived(
                    freq,
                    channelName,
                    radioText
                )
                return true
            }

            TRANSACTION_ON_HEADSET_DISCONNECTED -> {
                onHeadsetDisconnected()
                return true
            }
        }
        return super.onTransact(code, data, reply, flags)
    }
}