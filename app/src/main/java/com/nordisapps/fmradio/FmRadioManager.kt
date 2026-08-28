package com.nordisapps.fmradio

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.nordisapps.fmradio.fm.IFMPlayerProxy
import com.nordisapps.fmradio.fm.RadioEventListener
import com.nordisapps.fmradio.system.SamsungAudioRouter
import com.nordisapps.fmradio.system.SystemServiceLocator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.getValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class FmRadioManager(context: Context) {
    private val radio by lazy { connectRadio() }
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var lastTuneTimestamp = 0L
    private val minTuneIntervalMs = 1500L
    private val tuneMutex = Mutex()
    var onStationNameReceived: ((String) -> Unit)? = null
    var onRadioTextReceived: ((String) -> Unit)? = null
    var onRdsCleared: (() -> Unit)? = null
    var onScanStarted: (() -> Unit)? = null
    var onChannelFound: ((Double) -> Unit)? = null
    var onScanFinished: (() -> Unit)? = null
    var onScanStopped: ((List<Double>) -> Unit)? = null
    var onHeadsetDisconnected: (() -> Unit)? = null
    private val listener = RadioEventListener(
        onStationNameReceived = { onStationNameReceived?.invoke(it) },
        onRadioTextReceived = { onRadioTextReceived?.invoke(it) },
        onRdsCleared = { onRdsCleared?.invoke() },
        onScanStartedCallback = { onScanStarted?.invoke() },
        onChannelFound = { onChannelFound?.invoke(it) },
        onScanFinished = { onScanFinished?.invoke() },
        onScanStopped = { onScanStopped?.invoke(it) },
        onHeadsetDisconnectedCallback = {
            Log.d("FmRadioManager", "HEADSET DISCONNECTED - stopping radio")
            stop()
            onHeadsetDisconnected?.invoke()
        }
    )

    @Suppress("SameParameterValue")
    private inline fun <T> safeCall(methodName: String, default: T, action: () -> T): T {
        return try {
            action()
        } catch (e: Exception) {
            Log.e("FmRadioManager", "$methodName EXCEPTION: ${e.message}", e)
            default
        }
    }

    fun isServiceAvailable(): Boolean {
        return try {
            radio.isOn()
            true
        } catch (e: Exception) {
            Log.e("FmRadioManager", "isServiceAvailable EXCEPTION: ${e.message}", e)
            false
        }
    }

    fun isHeadsetConnected(): Boolean {
        return safeCall("isHeadsetConnected", false) {
            radio.isHeadsetPlugged()
        }
    }

    private fun connectRadio(): IFMPlayerProxy {
        val binder = SystemServiceLocator.getService("FMPlayer")
        Log.d("FmRadioManager", "BINDER = $binder")
        return IFMPlayerProxy(binder)
    }

    fun play() {
        try {
            val headset = radio.isHeadsetPlugged()
            Log.d("FmRadioManager", "HEADSET = $headset")
            if (!headset) {
                return
            }

            val on = radio.on()
            Log.d("FmRadioManager", "RADIO ON = $on")
            radio.setListener(listener)
            radio.enableRDS()
            radio.disableAF()
            radio.setStereo()
            radio.setSoftmute(false)
            Log.d("FmRadioManager", "SoftMute = ${radio.getSoftMuteMode()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun tune(frequency: Double): Double {
        if (!radio.isOn()) {
            return (frequency * 20).roundToInt() / 20.0
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastTuneTimestamp
        if (elapsed < minTuneIntervalMs) {
            Thread.sleep(minTuneIntervalMs - elapsed)
        }
        lastTuneTimestamp = System.currentTimeMillis()

        return try {
            val freqKhz = (frequency * 1000).roundToLong()
            radio.tune(freqKhz)
            Log.d("FmRadioManager", "TUNED TO $frequency")

            val currentChannel = radio.getCurrentChannel()
            Log.d("FmRadioManager", "CURRENT CHANNEL: $currentChannel")
            val result = currentChannel / 1000.0
            (result * 20).roundToInt() / 20.0
        } catch (e: Exception) {
            e.printStackTrace()
            frequency
        }
    }

    suspend fun tuneSafe(frequency: Double): Double {
        return tuneMutex.withLock {
            tune(frequency)
        }
    }

    fun seekUp(): Double {
        if (!radio.isOn()) {
            return 87.5
        }

        return try {
            val frequency = radio.seekUp()
            Log.d("FmRadioManager", "SEEK UP = $frequency")
            frequency / 1000.0
        } catch (e: Exception) {
            e.printStackTrace()
            87.5
        }
    }

    fun seekDown(): Double {
        if (!radio.isOn()) {
            return 87.5
        }

        return try {
            val frequency = radio.seekDown()
            Log.d("FmRadioManager", "SEEK DOWN = $frequency")
            frequency / 1000.0
        } catch (e: Exception) {
            e.printStackTrace()
            87.5
        }
    }

    fun scan() {
        try {
            radio.scan()
            Log.d("FmRadioManager", "SCAN CALLED")
        } catch (e: Exception) {
            Log.e("FmRadioManager", "scan EXCEPTION: ${e.message}", e)
        }
    }

    fun getLastScanResult(): List<Double> {
        return safeCall("getLastScanResult", emptyList()) {
            radio.getLastScanResult().map { it / 1000.0 }
        }
    }

    fun setSpeakerOn(enabled: Boolean): Boolean {
        return try {
            radio.setSpeakerOn(enabled)
            SamsungAudioRouter.setRadioOutputPath(audioManager, enabled)
        } catch (e: Exception) {
            Log.e("FmRadioManager", "setSpeakerOn EXCEPTION: ${e.message}", e)
            false
        }
    }

    fun stop() {
        try {
            val off = radio.off()
            Log.d("FmRadioManager", "RADIO OFF = $off")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}