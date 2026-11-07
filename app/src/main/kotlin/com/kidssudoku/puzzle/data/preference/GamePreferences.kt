package com.kidssudoku.puzzle.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sudoku_preferences", Context.MODE_PRIVATE)

    private val _onlyAvailableNumbers = MutableStateFlow(
        prefs.getBoolean(KEY_ONLY_AVAILABLE_NUMBERS, true)
    )
    val onlyAvailableNumbers: StateFlow<Boolean> = _onlyAvailableNumbers.asStateFlow()

    fun setOnlyAvailableNumbers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ONLY_AVAILABLE_NUMBERS, enabled).apply()
        _onlyAvailableNumbers.value = enabled
    }

    var seederVersion: Int
        get() = prefs.getInt(KEY_SEEDER_VERSION, 0)
        set(value) = prefs.edit().putInt(KEY_SEEDER_VERSION, value).apply()

    companion object {
        private const val KEY_ONLY_AVAILABLE_NUMBERS = "only_available_numbers"
        private const val KEY_SEEDER_VERSION = "seeder_version"
    }
}
