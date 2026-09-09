package com.nordisapps.fmradio.fm

import android.os.IBinder
import android.os.Parcel

class IFMPlayerProxy(
    private val remote: IBinder
) : IFMPlayer {
    companion object {
        const val DESCRIPTOR =
            "com.samsung.android.media.fmradio.internal.IFMPlayer"

        const val TRANSACTION_ON = 4
        const val TRANSACTION_IS_ON = 7
        const val TRANSACTION_OFF = 6
        const val TRANSACTION_TUNE = 3
        const val TRANSACTION_SEEK_DOWN = 9
        const val TRANSACTION_SEEK_UP = 8
        const val TRANSACTION_SCAN = 12
        const val TRANSACTION_CANCEL_SCAN = 13
        const val TRANSACTION_IS_SCANNING = 14
        const val TRANSACTION_GET_LAST_SCAN_RESULT = 33
        const val TRANSACTION_GET_CURRENT_CHANNEL = 11
        const val TRANSACTION_SET_SPEAKER = 40
        const val TRANSACTION_SET_STEREO = 34
        const val TRANSACTION_SET_SOFTMUTE = 47
        const val TRANSACTION_GET_SOFTMUTE = 48
        const val TRANSACTION_IS_HEADSET_PLUGGED = 38
        const val TRANSACTION_ENABLE_RDS = 20
        const val TRANSACTION_IS_RDS_ENABLE = 30
        const val TRANSACTION_ENABLE_DNS = 22
        const val TRANSACTION_IS_DNS_ENABLE = 24
        const val TRANSACTION_ENABLE_AF = 25
        const val TRANSACTION_DISABLE_AF = 26
        const val TRANSACTION_SET_LISTENER = 1
        const val TRANSACTION_GET_INTEGER_TUNNING_PARAMETER = 51
        const val TRANSACTION_SET_CHANNEL_SPACING = 28
        const val TRANSACTION_SET_BAND = 27
        const val TRANSACTION_DISABLE_RDS = 21
        const val TRANSACTION_SET_MONO = 35
    }

    private inline fun <T> transact(
        code: Int,
        writeArgs: Parcel.() -> Unit = {},
        readResult: Parcel.() -> T
    ): T {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()

        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeArgs()

            remote.transact(code, data, reply, 0)

            reply.readException()
            reply.readResult()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    override fun on(): Boolean = transact(TRANSACTION_ON) { readInt() != 0 }

    override fun isOn(): Boolean = transact(TRANSACTION_IS_ON) { readInt() != 0 }

    override fun off(): Boolean = transact(TRANSACTION_OFF) { readInt() != 0 }

    override fun tune(freq: Long) = transact(
        TRANSACTION_TUNE,
        writeArgs = { writeLong(freq) },
        readResult = {}
    )

    override fun seekDown(): Long = transact(TRANSACTION_SEEK_DOWN) { readLong() }

    override fun seekUp(): Long = transact(TRANSACTION_SEEK_UP) { readLong() }

    override fun scan() = transact(TRANSACTION_SCAN) {}

    override fun cancelScan(): Boolean = transact(TRANSACTION_CANCEL_SCAN) { readInt() != 0 }

    override fun isScanning(): Boolean = transact(TRANSACTION_IS_SCANNING) { readInt() != 0 }

    override fun getLastScanResult(): LongArray = transact(TRANSACTION_GET_LAST_SCAN_RESULT) {
        createLongArray() ?: LongArray(0)
    }

    override fun getCurrentChannel(): Long =
        transact(TRANSACTION_GET_CURRENT_CHANNEL) { readLong() }

    override fun setSpeakerOn(enabled: Boolean) = transact(
        TRANSACTION_SET_SPEAKER,
        writeArgs = { writeInt(if (enabled) 1 else 0) }
    ) {}

    override fun setStereo() = transact(TRANSACTION_SET_STEREO) {}

    override fun setMono() = transact(TRANSACTION_SET_MONO) {}

    override fun setSoftmute(enabled: Boolean) = transact(
        TRANSACTION_SET_SOFTMUTE,
        writeArgs = { writeInt(if (enabled) 1 else 0) }
    ) {}

    override fun getSoftMuteMode(): Boolean = transact(TRANSACTION_GET_SOFTMUTE) { readInt() != 0 }

    override fun isHeadsetPlugged(): Boolean =
        transact(TRANSACTION_IS_HEADSET_PLUGGED) { readInt() != 0 }

    override fun disableRDS() = transact(TRANSACTION_DISABLE_RDS) {}

    override fun enableRDS() = transact(TRANSACTION_ENABLE_RDS) {}

    override fun isRDSEnable(): Boolean = transact(TRANSACTION_IS_RDS_ENABLE) { readInt() != 0 }

    override fun enableDNS() = transact(TRANSACTION_ENABLE_DNS) {}

    override fun isDNSEnable(): Boolean = transact(TRANSACTION_IS_DNS_ENABLE) { readInt() != 0 }

    override fun enableAF() = transact(TRANSACTION_ENABLE_AF) {}

    override fun disableAF() = transact(TRANSACTION_DISABLE_AF) {}

    override fun setBand(band: Int) = transact(TRANSACTION_SET_BAND, writeArgs = { writeInt(band) }
    ) {}

    override fun setChannelSpacing(spacing: Int) = transact(
        TRANSACTION_SET_CHANNEL_SPACING,
        writeArgs = { writeInt(spacing) }
    ) {}

    override fun setListener(listener: IFMEventListener) = transact(
        TRANSACTION_SET_LISTENER,
        writeArgs = { writeStrongBinder(listener.asBinder()) }
    ) {}

    override fun getIntegerTunningParameter(
        key: String,
        defaultValue: Int
    ): Int = transact(
        TRANSACTION_GET_INTEGER_TUNNING_PARAMETER,
        writeArgs = {
            writeString(key)
            writeInt(defaultValue)
        }
    ) { readInt() }
}