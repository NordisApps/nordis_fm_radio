package com.nordisapps.fmradio

import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FmRadioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingUri: Uri? = null
    private var currentRecordingFile: File? = null
    private var isPaused: Boolean = false
    val isRecordingPaused: Boolean
        get() = isPaused

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun startRecording(context: Context, psName: String) {
        if (isRecording) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setParameters("fmradio_recoding=on")

        val fileName = buildFileName(context, psName)
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startRecordingModern(context, recorder, fileName)
            } else {
                startRecordingLegacy(recorder, fileName)
            }
        } catch (e: Exception) {
            Log.e("FmRadioRecorder", "startRecording failed: ${e.message}")
            cleanupAfterFailure(context, audioManager)
            throw e
        }
    }

    private fun startRecordingModern(context: Context, recorder: MediaRecorder, fileName: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$fileName.m4a")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/NordisFMRadio")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Не удалось создать файл записи")
        currentRecordingUri = uri

        val pfd = resolver.openFileDescriptor(uri, "w")
            ?: throw IllegalStateException("Не удалось открыть файл записи")

        pfd.use { pfd ->
            mediaRecorder = recorder
            recorder.apply {
                @Suppress("WrongConstant")
                setAudioSource(11) // AUDIO_DEVICE_IN_FM_TUNER
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(pfd.fileDescriptor)
                prepare()
                start()
            }
        }
    }

    private fun startRecordingLegacy(recorder: MediaRecorder, fileName: String) {
        val musicDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MUSIC
        )
        val targetDir = File(musicDir, "NordisFMRadio").apply { mkdirs() }
        val outputFile = File(targetDir, "$fileName.m4a")
        currentRecordingFile = outputFile

        mediaRecorder = recorder
        recorder.apply {
            @Suppress("WrongConstant")
            setAudioSource(11) // AUDIO_DEVICE_IN_FM_TUNER
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    fun pauseOrResumeRecording() {
        val recorder = mediaRecorder ?: return
        if (isPaused) {
            recorder.resume()
            isPaused = false
        } else {
            recorder.pause()
            isPaused = true
        }
    }

    private fun cleanupAfterFailure(context: Context, audioManager: AudioManager) {
        mediaRecorder?.release()
        mediaRecorder = null
        currentRecordingUri?.let { context.contentResolver.delete(it, null, null) }
        currentRecordingUri = null
        currentRecordingFile?.delete()
        currentRecordingFile = null
        audioManager.setParameters("fmradio_recoding=off")
    }

    fun stopRecording(context: Context) {
        if (!isRecording) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("FmRadioRecorder", "stopRecording failed: ${e.message}")
        }
        mediaRecorder = null

        audioManager.setParameters("fmradio_recoding=off")

        currentRecordingUri?.let { uri ->
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
            AudioGainProcessor.boostVolume(context, uri)
        }
        currentRecordingUri = null
        currentRecordingFile = null
        isPaused = false
    }

    fun cancelRecording(context: Context) {
        if (!isRecording) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("FmRadioRecorder", "cancelRecording failed: ${e.message}")
        }
        mediaRecorder = null

        audioManager.setParameters("fmradio_recoding=off")

        currentRecordingUri?.let { uri ->
            context.contentResolver.delete(uri, null, null)
        }
        currentRecordingUri = null

        currentRecordingFile?.delete()
        currentRecordingFile = null

        isPaused = false
    }

    private fun buildFileName(context: Context, psName: String): String {
        val formatter = SimpleDateFormat("dd-MM-yy_HH-mm", Locale.getDefault())
        val dateSuffix = formatter.format(Date())

        val baseName = if (psName.isNotBlank()) {
            psName.trim()
        } else {
            "RadioRecording"
        }

        var candidate = "${baseName}_$dateSuffix"
        var counter = 1
        while (fileExists(context, "$candidate.m4a")) {
            candidate = "${baseName}${counter}_$dateSuffix"
            counter++
        }
        return candidate
    }

    private fun fileExists(context: Context, displayName: String): Boolean {
        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val cursor = resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, arrayOf(displayName), null
        )
        return cursor?.use { it.count > 0 } ?: false
    }
}