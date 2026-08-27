package com.nordisapps.fmradio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object AudioGainProcessor {
    private const val TAG = "AudioGainProcessor"

    fun boostVolume(context: Context, sourceUri: Uri, targetPeakRatio: Float = 0.85f): Uri {
        return try {
            val decoded = decodeToPcm(context, sourceUri)
            val currentPeak = findPeakAmplitude(decoded.pcmData)
            val targetPeak = (Short.MAX_VALUE * targetPeakRatio).toInt()
            val gainFactor = if (currentPeak > 0) {
                targetPeak.toFloat() / currentPeak.toFloat()
            } else {
                1.0f
            }
            Log.d(TAG, "currentPeak=$currentPeak, targetPeak=$targetPeak, calculated gain=$gainFactor")
            val boostedPcm = applyGain(decoded.pcmData, gainFactor)
            encodeToM4a(context, boostedPcm, decoded.sampleRate, decoded.channelCount, sourceUri)
            Log.d(TAG, "boostVolume succeeded for $sourceUri")
            sourceUri
        } catch (e: Exception) {
            Log.e(TAG, "boostVolume failed: ${e.message}", e)
            sourceUri
        }
    }

    private class DecodedAudio(
        val pcmData: ShortArray,
        val sampleRate: Int,
        val channelCount: Int
    )

    private fun decodeToPcm(context: Context, sourceUri: Uri): DecodedAudio {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, sourceUri, null)

        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        requireNotNull(format) { "Аудио-дорожка не найдена в файле" }
        extractor.selectTrack(audioTrackIndex)

        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = format.getString(MediaFormat.KEY_MIME)!!

        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val pcmChunks = mutableListOf<ShortArray>()

        val bufferInfo = MediaCodec.BufferInfo()
        var isExtractorDone = false
        var isDecoderDone = false

        while (!isDecoderDone) {
            if (!isExtractorDone) {
                val inputBufferIndex = decoder.dequeueInputBuffer(10_000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isExtractorDone = true
                    } else {
                        decoder.queueInputBuffer(
                            inputBufferIndex, 0, sampleSize,
                            extractor.sampleTime, 0
                        )
                        extractor.advance()
                    }
                }
            }

            val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputBufferIndex >= 0) {
                if (bufferInfo.size > 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
                    val chunk = ShortArray(bufferInfo.size / 2)
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().get(chunk)
                    pcmChunks.add(chunk)
                }
                decoder.releaseOutputBuffer(outputBufferIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    isDecoderDone = true
                }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        val totalSize = pcmChunks.sumOf { it.size }
        val fullPcm = ShortArray(totalSize)
        var offset = 0
        for (chunk in pcmChunks) {
            chunk.copyInto(fullPcm, offset)
            offset += chunk.size
        }

        Log.d(TAG,"Decoded ${fullPcm.size} samples, sampleRate=$sampleRate, channels=$channelCount")
        return DecodedAudio(fullPcm, sampleRate, channelCount)
    }

    private fun applyGain(samples: ShortArray, gainFactor: Float): ShortArray {
        val result = ShortArray(samples.size)
        for (i in samples.indices) {
            val boosted = samples[i] * gainFactor
            result[i] = boosted.coerceIn(
                Short.MIN_VALUE.toFloat(),
                Short.MAX_VALUE.toFloat()
            ).toInt().toShort()
        }
        return result
    }

    private fun findPeakAmplitude(samples: ShortArray): Int {
        var peak = 0
        for (sample in samples) {
            val abs = abs(sample.toInt())
            if (abs > peak) peak = abs
        }
        return peak
    }

    private fun encodeToM4a(
        context: Context,
        pcmData: ShortArray,
        sampleRate: Int,
        channelCount: Int,
        outputUri: Uri
    ) {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val pfd = context.contentResolver.openFileDescriptor(outputUri, "rw")!!
        val muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        val pcmBytes = ByteBuffer.allocate(pcmData.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply { asShortBuffer().put(pcmData) }
            .array()

        var readOffset = 0
        var isInputDone = false
        var isOutputDone = false

        while (!isOutputDone) {
            if (!isInputDone) {
                val inputBufferIndex = encoder.dequeueInputBuffer(10_000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputBufferIndex)!!
                    val remaining = pcmBytes.size - readOffset
                    val chunkSize = minOf(inputBuffer.capacity(), remaining)

                    if (chunkSize <= 0) {
                        encoder.queueInputBuffer(
                            inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isInputDone = true
                    } else {
                        inputBuffer.put(pcmBytes, readOffset, chunkSize)
                        encoder.queueInputBuffer(inputBufferIndex, 0, chunkSize, 0, 0)
                        readOffset += chunkSize
                    }
                }
            }

            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)!!
                    if (bufferInfo.size > 0 && muxerStarted) {
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isOutputDone = true
                    }
                }
            }
        }

        encoder.stop()
        encoder.release()
        muxer.stop()
        muxer.release()
        pfd.close()

        Log.d(TAG, "Encoded ${pcmData.size} samples back to $outputUri")
    }
}