package com.pro.aifitnesstracker.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object WaterSoundGenerator {

    fun getWaterDropSoundUri(context: Context): Uri {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val soundFile = File(reportsDir, "water_drop.wav")
        
        // Generate WAV file if it doesn't exist
        if (!soundFile.exists()) {
            generateWavFile(soundFile)
        }

        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                soundFile
            )
        } catch (e: Exception) {
            Uri.fromFile(soundFile)
        }
    }

    fun playSound(context: Context) {
        try {
            val uri = getWaterDropSoundUri(context)
            android.media.MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener { it.release() }
            }
        } catch (_: Exception) {}
    }

    private fun generateWavFile(file: File) {
        val sampleRate = 44100
        val duration = 0.25 // seconds
        val numSamples = (duration * sampleRate).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Water droplet upward sweep: from 550 Hz to 1700 Hz
            val freq = 550.0 + (1150.0 * (t / duration))
            // Exponential decay envelope to mimic sound fading
            val envelope = exp(-10.0 * t)
            pcmData[i] = (sin(2.0 * PI * freq * t) * envelope * 32767).toInt().toShort()
        }

        val byteBuffer = ByteBuffer.allocate(44 + numSamples * 2).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF Header
            put("RIFF".toByteArray())
            putInt(36 + numSamples * 2) // Chunk size
            put("WAVE".toByteArray())
            
            // Format Subchunk
            put("fmt ".toByteArray())
            putInt(16) // Subchunk size (PCM)
            putShort(1.toShort()) // Audio format (PCM = 1)
            putShort(1.toShort()) // Number of channels (Mono = 1)
            putInt(sampleRate) // Sample rate
            putInt(sampleRate * 2) // Byte rate
            putShort(2.toShort()) // Block align
            putShort(16.toShort()) // Bits per sample
            
            // Data Subchunk
            put("data".toByteArray())
            putInt(numSamples * 2) // Data chunk size
            
            // Short PCM to bytes
            for (value in pcmData) {
                putShort(value)
            }
        }

        FileOutputStream(file).use { out ->
            out.write(byteBuffer.array())
        }
    }
}
