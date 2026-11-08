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
                // 축하 폭죽 효과음
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
        val durationSec = 1.7
        val numSamples = (sampleRate * durationSec).toInt()
        val shorts = ShortArray(numSamples)
        val random = Random(12345)

        // 3발의 폭죽 (발사 시간 -> 폭발 시간)
        val fireworkTimes = listOf(
            0.00 to 0.16,
            0.32 to 0.50,
            0.64 to 0.82
        )

        for ((launchT, burstT) in fireworkTimes) {
            val launchSample = (launchT * sampleRate).toInt()
            val burstSample = (burstT * sampleRate).toInt()
            val endSample = ((burstT + 0.75) * sampleRate).toInt().coerceAtMost(numSamples)

            // 1. 발사 휘슬 소리 (rising whistle 500Hz -> 2000Hz)
            val whistleSamples = burstSample - launchSample
            for (i in launchSample until burstSample.coerceAtMost(numSamples)) {
                val localT = (i - launchSample).toDouble() / whistleSamples
                val freq = 550.0 + 1600.0 * localT * localT
                val t = (i - launchSample).toDouble() / sampleRate
                val amp = 0.28 * sin(Math.PI * localT)
                val whistleWave = sin(2 * Math.PI * freq * t) * amp * Short.MAX_VALUE
                val current = shorts[i].toInt()
                shorts[i] = (current + whistleWave.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            // 2. 쾅! 폭발음 (저음 펀치 100Hz -> 45Hz + 화이트 노이즈 폭발)
            val boomDuration = (0.38 * sampleRate).toInt()
            val boomEnd = (burstSample + boomDuration).coerceAtMost(numSamples)
            for (i in burstSample until boomEnd) {
                val localT = (i - burstSample).toDouble() / boomDuration
                val boomEnv = exp(-localT * 7.5)
                val boomFreq = 95.0 - 50.0 * localT
                val boomTone = sin(2 * Math.PI * boomFreq * (i - burstSample) / sampleRate)
                val noise = random.nextDouble() * 2.0 - 1.0
                val boomWave = (boomTone * 0.65 + noise * 0.6) * boomEnv * Short.MAX_VALUE * 0.85
                val current = shorts[i].toInt()
                shorts[i] = (current + boomWave.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            // 3. 파직파직 퍼지는 불꽃 파열음 (Crackles / Sparks)
            for (i in burstSample until endSample) {
                val localT = (i - burstSample).toDouble() / (endSample - burstSample)
                val crackleEnv = exp(-localT * 3.2)
                if (random.nextDouble() < 0.04 * (1.0 - localT)) {
                    val popLen = (sampleRate * 0.0035).toInt()
                    val popSign = if (random.nextBoolean()) 1.0 else -1.0
                    for (k in 0 until popLen) {
                        if (i + k < numSamples) {
                            val popVal = popSign * (1.0 - k.toDouble() / popLen) * crackleEnv * Short.MAX_VALUE * 0.75
                            val current = shorts[i + k].toInt()
                            shorts[i + k] = (current + popVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                }
            }
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
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                }
                SoundEffect.BUTTON_CLICK -> {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
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
