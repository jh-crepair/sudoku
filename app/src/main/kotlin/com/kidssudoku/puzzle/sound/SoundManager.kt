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
    private val clearTapPcm: ByteArray by lazy { generateClearTapPcm() }
    private val errorBuzzerPcm: ByteArray by lazy { generateErrorBuzzerPcm() }
    private val celebrationPcm: ByteArray by lazy { generateCelebrationPcm() }
    private val gameOverPcm: ByteArray by lazy { generateGameOverPcm() }

    init {
        // Load sounds from res/raw – gracefully ignore if files are missing
        loadSound(SoundEffect.CELL_SELECT,  "sound_tap")
        loadSound(SoundEffect.NUMBER_INPUT, "sound_pop")
        loadSound(SoundEffect.UNLOCK,       "sound_unlock")
        loadSound(SoundEffect.BUTTON_CLICK, "sound_tap")
        loadSound(SoundEffect.PUZZLE_CLEAR, "sound_clear")

        // Pre-initialize lazy PCM buffers in background to avoid any first-play lag
        soundExecutor.execute {
            clearTapPcm
            errorBuzzerPcm
            celebrationPcm
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
                // 밝은 축제 축하 효과음 (SoundPool 우선 재생, 미로딩/실패 시 PCM)
                val soundId = soundIds[effect]
                if (soundId != null && soundId != 0) {
                    val streamId = soundPool.play(soundId, 0.95f, 0.95f, 1, 0, 1f)
                    if (streamId != 0) return
                }
                playPcm(celebrationPcm)
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

    private fun generateCelebrationPcm(): ByteArray {
        val durationSec = 2.2
        val numSamples = (sampleRate * durationSec).toInt()
        val floatSamples = FloatArray(numSamples)

        fun addBellNote(startTime: Double, freq: Double, duration: Double, amp: Float) {
            val startIdx = (startTime * sampleRate).toInt()
            val noteSamples = (duration * sampleRate).toInt()
            for (i in 0 until noteSamples) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val attack = (t / 0.002).coerceAtMost(1.0)
                val decay = exp(-t * (5.5 / duration))
                val tone = sin(2 * Math.PI * freq * t) * 0.65 +
                           sin(2 * Math.PI * freq * 2 * t) * 0.22 +
                           sin(2 * Math.PI * freq * 3 * t) * 0.09 +
                           sin(2 * Math.PI * freq * 4 * t) * 0.04
                floatSamples[idx] += (tone * attack * decay * amp).toFloat()
            }
        }

        fun addPartyPop(startTime: Double, startFreq: Double, endFreq: Double, duration: Double, amp: Float) {
            val startIdx = (startTime * sampleRate).toInt()
            val popSamples = (duration * sampleRate).toInt()
            for (i in 0 until popSamples) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / popSamples
                val freq = startFreq + (endFreq - startFreq) * progress.pow(0.4)
                val env = exp(-progress * 9.0)
                val wave = sin(2 * Math.PI * freq * t)
                val air = (Math.random() * 2.0 - 1.0) * exp(-progress * 16.0) * 0.15
                floatSamples[idx] += ((wave * 0.85 + air) * env * amp).toFloat()
            }
        }

        fun addSparkle(startTime: Double, freq: Double, duration: Double, amp: Float) {
            val startIdx = (startTime * sampleRate).toInt()
            val sparkleSamples = (duration * sampleRate).toInt()
            for (i in 0 until sparkleSamples) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val attack = (t / 0.001).coerceAtMost(1.0)
                val decay = exp(-t * 18.0)
                val shimmer = 1.0 + 0.15 * sin(2 * Math.PI * 18 * t)
                val wave = sin(2 * Math.PI * freq * t) * shimmer
                floatSamples[idx] += (wave * attack * decay * amp).toFloat()
            }
        }

        // 1. Party Poppers (경쾌하고 부드러운 팝 소리)
        addPartyPop(0.00, 520.0, 210.0, 0.09, 0.45f)
        addPartyPop(0.40, 680.0, 260.0, 0.08, 0.40f)
        addPartyPop(0.85, 820.0, 320.0, 0.08, 0.35f)

        // 2. 팡파르 멜로디 (도-미-솔-높은도-높은미&솔 화음)
        addBellNote(0.02, 523.25, 0.50, 0.65f) // C5
        addBellNote(0.14, 659.25, 0.50, 0.65f) // E5
        addBellNote(0.26, 783.99, 0.55, 0.70f) // G5
        addBellNote(0.38, 1046.50, 0.80, 0.75f) // C6
        addBellNote(0.55, 1318.51, 1.10, 0.70f) // E6
        addBellNote(0.55, 1567.98, 1.10, 0.60f) // G6
        addBellNote(0.55, 2093.00, 1.00, 0.40f) // C7

        // 3. 반짝이는 별가루 (Magical Stardust Cascades)
        val sparkles = listOf(
            0.70 to 2093.00,
            0.78 to 2349.32,
            0.86 to 2637.02,
            0.95 to 3135.96,
            1.04 to 2637.02,
            1.12 to 2093.00,
            1.20 to 2349.32,
            1.28 to 2637.02,
            1.38 to 3135.96,
            1.50 to 3520.00,
            1.65 to 4186.01
        )
        for ((t, f) in sparkles) {
            addSparkle(t, f, 0.35, 0.35f)
        }

        var maxAmp = 0f
        for (sample in floatSamples) {
            val absVal = abs(sample)
            if (absVal > maxAmp) maxAmp = absVal
        }
        val scale = if (maxAmp > 0f) (0.88f / maxAmp) else 1f

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
