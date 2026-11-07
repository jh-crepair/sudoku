package com.kidssudoku.puzzle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Colors ─────────────────────────────────────────────────────────────
val CreamBackground   = Color(0xFFFFFDE7)
val FixedCellBg       = Color(0xFFCFD8DC)
val InputCellBg       = Color(0xFFFFFFFF)
val SelectedHighlight = Color(0xFFFFD54F)
val LinkedHighlight   = Color(0xFFFFF9C4)
val UserInputColor    = Color(0xFF1565C0)
val FixedCellColor    = Color(0xFF212121)
val ErrorColor        = Color(0xFFEF5350)
val HintCellBg        = Color(0xFFE8F5E9)
val HintNumberColor   = Color(0xFF2E7D32)

val EasyColor   = Color(0xFF66BB6A)
val MediumColor = Color(0xFFFFA726)
val HardColor   = Color(0xFFEF5350)
val StarColor   = Color(0xFFFFD700)

private val LightColorScheme = lightColorScheme(
    primary           = Color(0xFF4CAF50),
    secondary         = Color(0xFFFFA726),
    background        = CreamBackground,
    surface           = Color(0xFFFFFFFF),
    onPrimary         = Color.White,
    onSecondary       = Color.White,
    onBackground      = Color(0xFF212121),
    onSurface         = Color(0xFF212121),
    error             = ErrorColor,
    onError           = Color.White
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

// Keep old Theme alias for compatibility
@Composable
fun Theme(content: @Composable () -> Unit) = SudokuTheme(content)
