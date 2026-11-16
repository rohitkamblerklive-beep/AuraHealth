package com.pro.aifitnesstracker.data.sleep

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

enum class SleepSoundType(
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    RAIN("Rain Sounds", "Gentle raindrops on leaves", "🌧️"),
    OCEAN("Ocean Waves", "Rhythmic soothing tides", "🌊"),
    FOREST("Forest Night", "Night crickets & gentle breeze", "🌲"),
    WHITE_NOISE("White Noise", "Deep background sound masking", "📻"),
    AMBIENT_TONE("Ambient Piano / Meditation", "432 Hz warm harmonic chords", "🎹"),
    STREAM("Nature Stream", "Peaceful mountain brook", "💧")
}

/**
 * High-Fidelity Procedural Relaxation Sound Generator using Android AudioTrack.
 * Generates continuous, non-repeating, crackle-free audio with 0 external dependencies.
 */
class SleepAudioManager {

    private var audioTrack: AudioTrack? = null
    private var synthesisThread: Thread? = null
    @Volatile private var isRunning = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSound = MutableStateFlow<SleepSoundType?>(null)
    val currentSound: StateFlow<SleepSoundType?> = _currentSound.asStateFlow()

    private val _volume = MutableStateFlow(0.75f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(0)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    companion object {
        private const val SAMPLE_RATE = 22050
        @Volatile private var instance: SleepAudioManager? = null

        fun getInstance(): SleepAudioManager {
            return instance ?: synchronized(this) {
                instance ?: SleepAudioManager().also { instance = it }
            }
        }
    }

    fun play(sound: SleepSoundType) {
        if (_currentSound.value == sound && _isPlaying.value) return
        stopInternal(keepTimer = true)

        _currentSound.value = sound
        _isPlaying.value = true
        isRunning = true

        startSynthesisThread(sound)
    }

    fun pause() {
        _isPlaying.value = false
        isRunning = false
        synthesisThread?.interrupt()
        try {
            audioTrack?.pause()
        } catch (_: Exception) {}
    }

    fun resume() {
        val sound = _currentSound.value ?: return
        if (_isPlaying.value) return
        _isPlaying.value = true
        isRunning = true
        startSynthesisThread(sound)
    }

    fun stop() {
        stopTimer()
        stopInternal(keepTimer = false)
        _currentSound.value = null
        _isPlaying.value = false
    }

    private fun stopInternal(keepTimer: Boolean) {
        isRunning = false
        synthesisThread?.interrupt()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        synthesisThread = null
        if (!keepTimer) {
            stopTimer()
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (_: Exception) {}
    }

    fun setSleepTimer(minutes: Int) {
        stopTimer()
        if (minutes <= 0) {
            _timerRemainingSeconds.value = 0
            return
        }
        val totalSecs = minutes * 60
        _timerRemainingSeconds.value = totalSecs

        timerRunnable = object : Runnable {
            override fun run() {
                val current = _timerRemainingSeconds.value
                if (current <= 1) {
                    _timerRemainingSeconds.value = 0
                    stop()
                } else {
                    _timerRemainingSeconds.value = current - 1
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 1000)
    }

    private fun stopTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
        _timerRemainingSeconds.value = 0
    }

    private fun startSynthesisThread(sound: SleepSoundType) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(_volume.value)
            audioTrack?.play()
        } catch (e: Exception) {
            _isPlaying.value = false
            return
        }

        synthesisThread = Thread {
            val shortBuffer = ShortArray(1024)
            val random = Random()
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var filterState = 0.0
            var sampleIndex = 0L

            while (isRunning && !Thread.currentThread().isInterrupted) {
                for (i in shortBuffer.indices) {
                    val t = sampleIndex.toDouble() / SAMPLE_RATE.toDouble()
                    val sampleVal: Double = when (sound) {
                        SleepSoundType.RAIN -> {
                            // Pink noise with micro-droplet spikes
                            val white = random.nextDouble() * 2.0 - 1.0
                            filterState = 0.95 * filterState + 0.05 * white
                            var drop = 0.0
                            if (random.nextInt(120) == 0) {
                                drop = (random.nextDouble() - 0.5) * 0.4
                            }
                            (filterState * 0.8 + drop).coerceIn(-1.0, 1.0)
                        }
                        SleepSoundType.OCEAN -> {
                            // Pink noise modulated by low frequency wave swell
                            val white = random.nextDouble() * 2.0 - 1.0
                            filterState = 0.98 * filterState + 0.02 * white
                            val swell = (sin(2.0 * PI * t * 0.08) * 0.45 + 0.55)
                            (filterState * swell * 1.5).coerceIn(-1.0, 1.0)
                        }
                        SleepSoundType.FOREST -> {
                            // Night crickets (4.2 kHz burst pulse) + gentle low wind
                            val white = random.nextDouble() * 2.0 - 1.0
                            filterState = 0.99 * filterState + 0.01 * white
                            phase1 += 2.0 * PI * 4200.0 / SAMPLE_RATE
                            val chirpLfo = sin(2.0 * PI * t * 14.0)
                            val cricket = if (chirpLfo > 0.3) sin(phase1) * 0.25 else 0.0
                            (filterState * 0.4 + cricket).coerceIn(-1.0, 1.0)
                        }
                        SleepSoundType.WHITE_NOISE -> {
                            // Gentle broadband noise with mild high attenuation
                            val white = random.nextDouble() * 2.0 - 1.0
                            filterState = 0.7 * filterState + 0.3 * white
                            filterState * 0.65
                        }
                        SleepSoundType.AMBIENT_TONE -> {
                            // Warm 432 Hz + 540 Hz (major third) + 648 Hz (fifth) chord with gentle LFO swell
                            phase1 += 2.0 * PI * 432.0 / SAMPLE_RATE
                            phase2 += 2.0 * PI * 540.0 / SAMPLE_RATE
                            phase3 += 2.0 * PI * 648.0 / SAMPLE_RATE
                            val breathLfo = sin(2.0 * PI * t * 0.12) * 0.25 + 0.75
                            val s1 = sin(phase1) * 0.35
                            val s2 = sin(phase2) * 0.25
                            val s3 = sin(phase3) * 0.20
                            (s1 + s2 + s3) * breathLfo
                        }
                        SleepSoundType.STREAM -> {
                            // Babbling brook: modulated resonance in 800-1400 Hz range
                            val white = random.nextDouble() * 2.0 - 1.0
                            val streamLfo = (sin(2.0 * PI * t * 1.5) * 0.3 + 0.7)
                            filterState = 0.92 * filterState + 0.08 * white
                            (filterState * streamLfo * 1.2).coerceIn(-1.0, 1.0)
                        }
                    }

                    // Convert to 16-bit PCM integer
                    shortBuffer[i] = (sampleVal * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                    sampleIndex++
                }

                try {
                    audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                } catch (e: Exception) {
                    break
                }
            }
        }.apply {
            name = "SleepAudioSynthesisThread"
            priority = Thread.MIN_PRIORITY
            start()
        }
    }
}
