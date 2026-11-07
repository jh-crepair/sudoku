package com.kidssudoku.puzzle.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidssudoku.puzzle.ui.theme.*

@Composable
fun GameScreen(
    puzzleId : Int,
    onBack   : () -> Unit,
    onClear  : (puzzleId: Int, stars: Int, elapsedSeconds: Int) -> Unit,
    viewModel: GameViewModel
) {
    LaunchedEffect(puzzleId) { viewModel.loadPuzzle(puzzleId) }

    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCell    by viewModel.selectedCell.collectAsStateWithLifecycle()
    val showNumberPicker by viewModel.showNumberPicker.collectAsStateWithLifecycle()
    val isPaused        by viewModel.isPaused.collectAsStateWithLifecycle()
    val elapsedSeconds  by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val hintsRemaining  by viewModel.hintsRemaining.collectAsStateWithLifecycle()
    val isCleared       by viewModel.isCleared.collectAsStateWithLifecycle()

    LaunchedEffect(isCleared) {
        if (isCleared) {
            val stars   = viewModel.calculateStars()
            val elapsed = elapsedSeconds
            onClear(puzzleId, stars, elapsed)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── HUD ──────────────────────────────────────────────────────────
            GameHud(
                puzzleInfo     = uiState.puzzleInfo,
                elapsedSeconds = elapsedSeconds,
                hintsRemaining = hintsRemaining,
                isPaused       = isPaused,
                onBack         = onBack,
                onHint         = { viewModel.useHint() },
                onPause        = { viewModel.togglePause() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Grid ─────────────────────────────────────────────────────────
            if (uiState.grid.isNotEmpty()) {
                SudokuGrid(
                    grid         = uiState.grid,
                    gridSize     = uiState.gridSize,
                    fixedCells   = uiState.fixedCells,
                    errorCells   = uiState.errorCells,
                    hintCells    = uiState.hintCells,
                    selectedCell = selectedCell,
                    onCellClick  = { r, c -> viewModel.onCellClick(r, c) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // ── Number picker popup ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = showNumberPicker && selectedCell != null,
            enter   = fadeIn() + scaleIn(),
            exit    = fadeOut() + scaleOut()
        ) {
            selectedCell?.let { (selRow, selCol) ->
                NumberPickerOverlay(
                    gridSize         = uiState.gridSize,
                    availableNumbers = viewModel.getAvailableNumbers(selRow, selCol),
                    onNumberSelected = { num -> viewModel.inputNumber(selRow, selCol, num) },
                    onDelete         = { viewModel.deleteNumber(selRow, selCol) },
                    onDismiss        = { viewModel.dismissNumberPicker() }
                )
            }
        }

        // ── Pause overlay ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isPaused,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape  = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏸", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text       = "일시정지",
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { viewModel.togglePause() },
                            modifier  = Modifier.fillMaxWidth().height(56.dp),
                            shape     = RoundedCornerShape(14.dp)
                        ) {
                            Text("계속하기", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── HUD ──────────────────────────────────────────────────────────────────────

@Composable
private fun GameHud(
    puzzleInfo     : String,
    elapsedSeconds : Int,
    hintsRemaining : Int,
    isPaused       : Boolean,
    onBack         : () -> Unit,
    onHint         : () -> Unit,
    onPause        : () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeStr = "%02d:%02d".format(minutes, seconds)

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Text(text = "←", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
        }

        Column(
            modifier              = Modifier.weight(1f),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                text      = puzzleInfo,
                fontSize  = 13.sp,
                color     = Color(0xFF78909C)
            )
            Text(
                text       = timeStr,
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF333333)
            )
        }

        // Hint button
        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick  = onHint,
                enabled  = hintsRemaining > 0 && !isPaused
            ) {
                Text(
                    text     = "💡",
                    fontSize = 26.sp,
                    color    = if (hintsRemaining > 0) Color.Unspecified else Color(0xFFBDBDBD)
                )
            }
            if (hintsRemaining > 0) {
                Box(
                    modifier          = Modifier
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(18.dp)
                        .background(EasyColor, CircleShape),
                    contentAlignment  = Alignment.Center
                ) {
                    Text(
                        text      = "$hintsRemaining",
                        fontSize  = 10.sp,
                        color     = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pause button
        IconButton(onClick = onPause) {
            Text(
                text     = if (isPaused) "▶" else "⏸",
                fontSize = 22.sp,
                color    = Color(0xFF37474F)
            )
        }
    }
}

// ── Sudoku Grid ───────────────────────────────────────────────────────────────

@Composable
fun SudokuGrid(
    grid         : List<List<Int>>,
    gridSize     : Int,
    fixedCells   : Set<Pair<Int, Int>>,
    errorCells   : Set<Pair<Int, Int>>,
    hintCells    : Set<Pair<Int, Int>>,
    selectedCell : Pair<Int, Int>?,
    onCellClick  : (Int, Int) -> Unit
) {
    val (boxRows, boxCols) = when (gridSize) {
        4    -> 2 to 2
        6    -> 2 to 3
        else -> 3 to 3
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val totalW = maxWidth
        val cellDp = totalW / gridSize

        val outerBorderColor = Color(0xFF263238)
        val boxBorderColor   = Color(0xFF37474F)
        val innerBorderColor = Color(0xFFCFD8DC)

        Box(
            modifier = Modifier
                .size(totalW)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .drawWithContent {
                    // 1. 셀 내용(배경색, 텍스트) 먼저 그리기
                    drawContent()

                    val cellWidth = size.width / gridSize
                    val cellHeight = size.height / gridSize

                    // 2. 내부 셀 경계선 및 2x2(또는 박스) 외부 경계선 그리기
                    // 수직선
                    for (col in 1 until gridSize) {
                        val isBoxBorder = (col % boxCols == 0)
                        val strokeW = if (isBoxBorder) 3.dp.toPx() else 1.dp.toPx()
                        val color = if (isBoxBorder) boxBorderColor else innerBorderColor
                        val x = cellWidth * col
                        drawLine(
                            color = color,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeW
                        )
                    }

                    // 수평선
                    for (row in 1 until gridSize) {
                        val isBoxBorder = (row % boxRows == 0)
                        val strokeW = if (isBoxBorder) 3.dp.toPx() else 1.dp.toPx()
                        val color = if (isBoxBorder) boxBorderColor else innerBorderColor
                        val y = cellHeight * row
                        drawLine(
                            color = color,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW
                        )
                    }

                    // 3. 전체 외곽 테두리 (가장 바깥 경계)
                    val outerW = 3.5.dp.toPx()
                    val halfOuter = outerW / 2f
                    drawRoundRect(
                        color = outerBorderColor,
                        topLeft = Offset(halfOuter, halfOuter),
                        size = Size(size.width - outerW, size.height - outerW),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = outerW)
                    )
                }
        ) {
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val isFixed    = row to col in fixedCells
                    val isError    = row to col in errorCells
                    val isHint     = row to col in hintCells
                    val isSelected = selectedCell == row to col
                    val sameRow    = selectedCell?.first == row
                    val sameCol    = selectedCell?.second == col
                    val sameBox    = selectedCell?.let { (sr, sc) ->
                        (row / boxRows == sr / boxRows) && (col / boxCols == sc / boxCols)
                    } == true
                    val isLinked   = !isSelected && (sameRow || sameCol || sameBox)

                    val cellBg = when {
                        isSelected -> SelectedHighlight
                        isHint     -> HintCellBg
                        isError    -> ErrorColor.copy(alpha = 0.18f)
                        isFixed    -> FixedCellBg
                        isLinked   -> LinkedHighlight
                        else       -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = cellDp * col, y = cellDp * row)
                            .size(cellDp)
                            .background(cellBg)
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        val value = grid[row][col]
                        if (value != 0) {
                            Text(
                                text = "$value",
                                fontSize = when (gridSize) {
                                    4    -> 34.sp
                                    6    -> 26.sp
                                    else -> 20.sp
                                },
                                fontWeight = if (isFixed) FontWeight.ExtraBold else FontWeight.Bold,
                                color = when {
                                    isError -> ErrorColor
                                    isFixed -> FixedCellColor
                                    isHint  -> HintNumberColor
                                    else    -> UserInputColor
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Number Picker Overlay ─────────────────────────────────────────────────────

@Composable
fun NumberPickerOverlay(
    gridSize         : Int,
    availableNumbers : Set<Int>,
    onNumberSelected : (Int) -> Unit,
    onDelete         : () -> Unit,
    onDismiss        : () -> Unit
) {
    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss),
        contentAlignment  = Alignment.Center
    ) {
        Card(
            modifier  = Modifier.clickable { /* consume */ },
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(
                modifier              = Modifier.padding(20.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text       = "숫자를 선택하세요",
                    fontSize   = 16.sp,
                    color      = Color(0xFF78909C),
                    fontWeight = FontWeight.Medium
                )

                val perRow = if (gridSize <= 4) 4 else if (gridSize == 6) 3 else 5
                val chunks = (1..gridSize).chunked(perRow)
                chunks.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { num ->
                            val available = num in availableNumbers
                            NumberButton(
                                number    = num,
                                available = available,
                                onClick   = { if (available) onNumberSelected(num) }
                            )
                        }
                    }
                }

                // Delete button
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑", fontSize = 26.sp)
                }
            }
        }
    }
}

@Composable
private fun NumberButton(
    number    : Int,
    available : Boolean,
    onClick   : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(
                if (available) Color(0xFF4CAF50).copy(alpha = 0.12f)
                else           Color(0xFFF5F5F5)
            )
            .border(
                width  = if (available) 2.dp else 1.dp,
                color  = if (available) EasyColor else Color(0xFFE0E0E0),
                shape  = CircleShape
            )
            .clickable(enabled = available, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "$number",
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = if (available) Color(0xFF2E7D32) else Color(0xFFBDBDBD)
        )
    }
}
