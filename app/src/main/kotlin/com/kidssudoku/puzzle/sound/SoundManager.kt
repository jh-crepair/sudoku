package com.kidssudoku.puzzle.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

enum class SoundEffect {
    CELL_SELECT,
    NUMBER_INPUT,
    ERROR_INPUT,
    PUZZLE_CLEAR,
    UNLOCK,
    BUTTON_CLICK
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

    init {
        // Load sounds from res/raw – gracefully ignore if files are missing
        loadSound(SoundEffect.CELL_SELECT,  "sound_tap")
        loadSound(SoundEffect.NUMBER_INPUT, "sound_pop")
        loadSound(SoundEffect.ERROR_INPUT,  "sound_error")
        loadSound(SoundEffect.PUZZLE_CLEAR, "sound_clear")
        loadSound(SoundEffect.UNLOCK,       "sound_unlock")
        loadSound(SoundEffect.BUTTON_CLICK, "sound_tap")
    }

    private fun loadSound(effect: SoundEffect, resName: String) {
        runCatching {
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId != 0) soundIds[effect] = soundPool.load(context, resId, 1)
        }
    }

    fun play(effect: SoundEffect) {
        if (isMuted) return

        val soundId = soundIds[effect]
        if (soundId != null && soundId != 0) {
            val streamId = soundPool.play(soundId, 0.9f, 0.9f, 1, 0, 1f)
            if (streamId != 0) return
        }

        // Fallback to ToneGenerator for guaranteed sound output
        playFallbackTone(effect)
    }

    private fun playFallbackTone(effect: SoundEffect) {
        val tg = toneGenerator ?: return
        try {
            when (effect) {
                SoundEffect.ERROR_INPUT -> {
                    // Double beep warning sound for error
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                }
                SoundEffect.NUMBER_INPUT -> {
                    // Short pleasant tick/ack tone
                    tg.startTone(ToneGenerator.TONE_PROP_ACK, 70)
                }
                SoundEffect.PUZZLE_CLEAR -> {
                    // Celebration fanfare sequence
                    tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
                    mainHandler.postDelayed({
                        try {
                            tg.startTone(ToneGenerator.TONE_PROP_ACK, 250)
                        } catch (_: Exception) {}
                    }, 200)
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
    }
}
