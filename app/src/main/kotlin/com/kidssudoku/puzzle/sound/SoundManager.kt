package com.kidssudoku.puzzle.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

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
        soundIds[effect]?.let { id -> soundPool.play(id, 0.9f, 0.9f, 1, 0, 1f) }
    }

    fun setMuted(muted: Boolean) { isMuted = muted }
    fun isMuted(): Boolean = isMuted

    fun release() { soundPool.release() }
}
