package com.kidssudoku.puzzle.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import java.util.Random
import java.util.concurrent.Executors
import kotlin.math.*

enum class SoundEffect {
    CELL_SELECT,
    NUMBER_INPUT,
    ERROR_INPUT,
    PUZZLE_CLEAR,
    UNLOCK,
    BUTTON_CLICK,
    GAME_OVER
}

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<SoundEffect, Int>()
    @Volatile
    private var isMuted = false

    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    } catch (_: Exception) {
        null
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val soundExecutor = Executors.newCachedThreadPool()
    private val sampleRate = 22050

    // Pre-synthesized PCM sounds
    private val clearTapPcm: ByteArray by lazy { generateClearTapPcm() }
    private val errorBuzzerPcm: ByteArray by lazy { generateErrorBuzzerPcm() }
    private val fireworksPcm: ByteArray by lazy { generateFireworksPcm() }
    private val gameOverPcm: ByteArray by lazy { generateGameOverPcm() }

    init {
        // Load sounds from res/raw – gracefully ignore if files are missing
        loadSound(SoundEffect.CELL_SELECT,  "sound_tap")
        loadSound(SoundEffect.NUMBER_INPUT, "sound_pop")
        loadSound(SoundEffect.UNLOCK,       "sound_unlock")
        loadSound(SoundEffect.BUTTON_CLICK, "sound_tap")

        // Pre-initialize lazy PCM buffers in background to avoid any first-play lag
        soundExecutor.execute {
            clearTapPcm
            errorBuzzerPcm
            fireworksPcm
            gameOverPcm
        }
    }

    private fun loadSound(effect: SoundEffect, resName: String) {
        runCatching {
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId != 0) soundIds[effect] = soundPool.load(context, resId, 1)
        }
    }

    fun play(effect: SoundEffect) {
        if (isMuted) return

        when (effect) {
            SoundEffect.ERROR_INPUT -> {
                // 경고스러운 버저 효과음
                playPcm(errorBuzzerPcm)
                return
            }

            SoundEffect.PUZZLE_CLEAR -> {
                playPcm(fireworksPcm)
                return
            }
            SoundEffect.GAME_OVER -> {
                // 게임 실패 효과음
                playPcm(gameOverPcm)
                return
            }
            else -> {
                val soundId = soundIds[effect]
                if (soundId != null && soundId != 0) {
                    val streamId = soundPool.play(soundId, 0.9f, 0.9f, 1, 0, 1f)
                    if (streamId != 0) return
                }
                // Fallback to ToneGenerator for other generic sounds
                playFallbackTone(effect)
            }
        }
    }

    private fun playPcm(pcmData: ByteArray) {
        if (isMuted) return
        soundExecutor.execute {
            var track: AudioTrack? = null
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(pcmData.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcmData, 0, pcmData.size)
                track.play()

                val durationMs = (pcmData.size * 1000L) / (sampleRate * 2)
                Thread.sleep(durationMs + 60)
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun generateClearTapPcm(): ByteArray {
        val durationSec = 0.16
        val numSamples = (sampleRate * durationSec).toInt()
        val shorts = ShortArray(numSamples)
        val f0 = 1567.98 // G6 (맑고 청아한 크리스탈 차임)
        val f1 = 3135.96 // 2nd harmonic
        val f2 = 4703.94 // 3rd harmonic
        val fSub = 783.99 // G5

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attackSamples = sampleRate * 0.0015
            val attack = if (i < attackSamples) (i / attackSamples) else 1.0

            val envMain = exp(-t * 22.0)
            val envHigh = exp(-t * 45.0)
            val envSub = exp(-t * 28.0)

            val waveMain = sin(2 * Math.PI * f0 * t) * 0.65
            val waveSub = sin(2 * Math.PI * fSub * t) * 0.18
            val waveH1 = sin(2 * Math.PI * f1 * t) * 0.22
            val waveH2 = sin(2 * Math.PI * f2 * t) * 0.08

            val mixed = (waveMain * envMain + waveSub * envSub + waveH1 * envHigh + waveH2 * envHigh) * attack
            val sampleVal = (mixed * Short.MAX_VALUE * 0.85).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            shorts[i] = sampleVal.toShort()
        }
        return shortsToBytes(shorts)
    }

    private fun generateErrorBuzzerPcm(): ByteArray {
        val durationSec = 0.28
        val numSamples = (sampleRate * durationSec).toInt()
        val shorts = ShortArray(numSamples)
        val freq = 145.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // 묵직하고 거친 경고 버저음 합성 (기본파 + 홀수 고조파 + 사각파)
            val sine1 = sin(2 * Math.PI * freq * t)
            val sine2 = sin(2 * Math.PI * (freq * 2) * t) * 0.4
            val sine3 = sin(2 * Math.PI * (freq * 3) * t) * 0.3
            val square = (if ((freq * t % 1.0) > 0.5) 0.35 else -0.35)
            val rawWave = sine1 * 0.5 + sine2 + sine3 + square

            val progress = i.toDouble() / numSamples
            // 날카로운 어택 후 약간의 서스테인, 그리고 빠른 감쇠
            val env = when {
                progress < 0.05 -> progress / 0.05
                else -> (1.0 - progress).pow(1.8)
            }
            val sampleVal = (rawWave * env * Short.MAX_VALUE * 0.85).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            shorts[i] = sampleVal.toShort()
        }
        return shortsToBytes(shorts)
    }

    private fun generateFireworksPcm(): ByteArray {
        val durationSec = 2.8
        val numSamples = (sampleRate * durationSec).toInt()
        val floatSamples = FloatArray(numSamples)
        val rng = Random(2026)

        fun addLaunch(startTime: Double, duration: Double, startFreq: Double, endFreq: Double, amp: Float) {
            val startIdx = (startTime * sampleRate).toInt()
            val nSamples = (duration * sampleRate).toInt()
            for (i in 0 until nSamples) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val prog = i.toDouble() / nSamples
                val freq = startFreq + (endFreq - startFreq) * prog.pow(2.2)
                val env = sin(prog * Math.PI * 0.5).pow(1.5)
                val whistle = sin(2 * Math.PI * freq * t)
                val air = (rng.nextDouble() * 2.0 - 1.0) * 0.35
                floatSamples[idx] += ((whistle * 0.65 + air * 0.35) * env * amp).toFloat()
            }
        }

        fun addFireworkExplosion(burstTime: Double, bassFreq: Double, amp: Float) {
            val burstIdx = (burstTime * sampleRate).toInt()

            // 1. Bass boom
            val boomDuration = 0.85
            val boomSamples = (boomDuration * sampleRate).toInt()
            for (i in 0 until boomSamples) {
                val idx = burstIdx + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val prog = i.toDouble() / boomSamples
                val env = exp(-prog * 5.0)
                val f = bassFreq * exp(-prog * 2.0)
                val core = sin(2 * Math.PI * f * t)
                val harm2 = sin(2 * Math.PI * (f * 2.1) * t) * 0.35
                val blastNoise = (rng.nextDouble() * 2.0 - 1.0) * exp(-prog * 18.0) * 0.85
                floatSamples[idx] += ((core + harm2 + blastNoise) * env * amp).toFloat()
            }

            // 2. Sparkles & Crackles
            val showerDuration = 1.35
            val showerSamples = (showerDuration * sampleRate).toInt()
            for (i in 0 until showerSamples) {
                val idx = burstIdx + i
                if (idx >= numSamples) break
                val prog = i.toDouble() / showerSamples
                val shimmerEnv = sin(prog * Math.PI) * exp(-prog * 2.2)
                val noiseSizzle = (rng.nextDouble() * 2.0 - 1.0) * 0.15 * shimmerEnv * amp
                floatSamples[idx] += noiseSizzle.toFloat()
            }

            val numSparks = (140 * showerDuration).toInt()
            for (s in 0 until numSparks) {
                val sparkT = 0.05 + rng.nextDouble().pow(1.4) * (showerDuration - 0.1)
                val sparkIdx = burstIdx + (sparkT * sampleRate).toInt()
                if (sparkIdx >= numSamples) continue
                val decayFactor = exp(-sparkT * 2.2)
                val sAmp = (rng.nextDouble() * 0.6 + 0.4) * decayFactor * amp * 0.75
                val sLen = (sampleRate * (0.002 + rng.nextDouble() * 0.003)).toInt()
                val sFreq = 2800.0 + rng.nextDouble() * 3000.0
                for (k in 0 until sLen) {
                    val tIdx = sparkIdx + k
                    if (tIdx >= numSamples) break
                    val tk = k.toDouble() / sampleRate
                    val kEnv = (1.0 - k.toDouble() / sLen).pow(1.5)
                    val value = (sin(2 * Math.PI * sFreq * tk) * 0.6 + (rng.nextDouble() * 2.0 - 1.0) * 0.4) * kEnv * sAmp
                    floatSamples[tIdx] += value.toFloat()
                }
            }
        }

        fun addCrowdCheer(startTime: Double, duration: Double, startFreq: Double, endFreq: Double, amp: Float) {
            val startIdx = (startTime * sampleRate).toInt()
            val nSamples = (duration * sampleRate).toInt()
            var phase = 0.0
            for (i in 0 until nSamples) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / nSamples
                val vibrato = sin(2 * Math.PI * 5.2 * t) * 7.0
                val frequency = startFreq + (endFreq - startFreq) * progress + vibrato
                phase += 2 * Math.PI * frequency / sampleRate

                val voiced = sin(phase) * 0.45 +
                    sin(phase * 2.0) * 0.38 +
                    sin(phase * 3.0) * 0.26 +
                    sin(phase * 4.0) * 0.16
                val envelope = minOf(
                    (progress / 0.08).coerceIn(0.0, 1.0),
                    ((1.0 - progress) / 0.16).coerceIn(0.0, 1.0)
                )
                val breath = (rng.nextDouble() * 2.0 - 1.0) * 0.10
                floatSamples[idx] += ((voiced + breath) * envelope * amp).toFloat()
            }
        }

        addLaunch(0.00, 0.35, 450.0, 1900.0, 0.42f)
        addFireworkExplosion(0.35, 80.0, 0.85f)

        addLaunch(0.38, 0.36, 520.0, 2200.0, 0.45f)
        addFireworkExplosion(0.74, 92.0, 0.90f)

        addLaunch(0.78, 0.42, 380.0, 2500.0, 0.55f)
        addFireworkExplosion(1.20, 68.0, 1.00f)
        addFireworkExplosion(1.36, 85.0, 0.70f)

        addCrowdCheer(0.95, 1.65, 230.0, 360.0, 0.28f)
        addCrowdCheer(1.08, 1.50, 290.0, 420.0, 0.24f)
        addCrowdCheer(1.28, 1.35, 190.0, 310.0, 0.22f)
        addCrowdCheer(1.48, 1.25, 340.0, 460.0, 0.20f)
        addCrowdCheer(1.72, 1.05, 260.0, 390.0, 0.18f)

        var maxAmp = 0f
        for (sample in floatSamples) {
            val absVal = abs(sample)
            if (absVal > maxAmp) maxAmp = absVal
        }
        val scale = if (maxAmp > 0f) (0.94f / maxAmp) else 1f

        val shorts = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            shorts[i] = (floatSamples[i] * scale * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return shortsToBytes(shorts)
    }

    private fun generateGameOverPcm(): ByteArray {
        val durationSec = 0.8
        val numSamples = (sampleRate * durationSec).toInt()
        val shorts = ShortArray(numSamples)
        // 3 descending sad tones: 310Hz -> 260Hz -> 195Hz
        val notes = listOf(310.0, 260.0, 195.0)
        val noteSamples = numSamples / notes.size

        for ((idx, freq) in notes.withIndex()) {
            val start = idx * noteSamples
            val end = (start + noteSamples).coerceAtMost(numSamples)
            for (i in start until end) {
                val t = (i - start).toDouble() / sampleRate
                val localProg = (i - start).toDouble() / noteSamples
                val env = (1.0 - localProg).pow(1.5)
                val wave = sin(2 * Math.PI * freq * t) * 0.7 + sin(2 * Math.PI * (freq * 0.5) * t) * 0.3
                val sampleVal = (wave * env * Short.MAX_VALUE * 0.75).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                shorts[i] = sampleVal.toShort()
            }
        }
        return shortsToBytes(shorts)
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            val s = shorts[i].toInt()
            bytes[i * 2] = (s and 0x00FF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0x00FF).toByte()
        }
        return bytes
    }

    private fun playFallbackTone(effect: SoundEffect) {
        val tg = toneGenerator ?: return
        try {
            when (effect) {
                SoundEffect.NUMBER_INPUT -> {
                    tg.startTone(ToneGenerator.TONE_PROP_ACK, 70)
                }
                SoundEffect.CELL_SELECT -> {
                    playPcm(clearTapPcm)
                }
                SoundEffect.BUTTON_CLICK -> {
                    playPcm(clearTapPcm)
                }
                SoundEffect.UNLOCK -> {
                    tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
                }
                else -> {}
            }
        } catch (_: Exception) {
            // Ignore tone generator errors gracefully
        }
    }

    fun setMuted(muted: Boolean) { isMuted = muted }
    fun isMuted(): Boolean = isMuted

    fun release() {
        soundPool.release()
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        try {
            soundExecutor.shutdownNow()
        } catch (_: Exception) {}
    }
}
