package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

object SoundSynth {
    private var audioTrack: AudioTrack? = null

    init {
        try {
            val sampleRate = 44100
            val durationMs = 40
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            val frequency = 180.0 // Low satisfying tap
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / numSamples) // Fade out
                val sine = sin(2 * PI * frequency * t)
                samples[i] = (sine * envelope * Short.MAX_VALUE * 0.3).toInt().toShort() // Lower volume
            }
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                
            audioTrack?.write(samples, 0, samples.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTap() {
        try {
            audioTrack?.stop()
            audioTrack?.reloadStaticData()
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
