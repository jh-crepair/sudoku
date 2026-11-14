package com.kidssudoku.puzzle.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidssudoku.puzzle.R
import com.kidssudoku.puzzle.ui.theme.*

@Composable
fun GameScreen(
    puzzleId : Int,
    onBack   : () -> Unit,
    onAbandon: () -> Unit,
    onClear  : (puzzleId: Int, stars: Int, elapsedSeconds: Int) -> Unit,
    viewModel: GameViewModel
) {
    LaunchedEffect(puzzleId) { viewModel.loadPuzzle(puzzleId) }

    val uiState                by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCell           by viewModel.selectedCell.collectAsStateWithLifecycle()
    val showNumberPicker       by viewModel.showNumberPicker.collectAsStateWithLifecycle()
    val isPaused               by viewModel.isPaused.collectAsStateWithLifecycle()
    val elapsedSeconds         by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val timeLimitSeconds       by viewModel.timeLimitSeconds.collectAsStateWithLifecycle()
    val failCount              by viewModel.failCount.collectAsStateWithLifecycle()
    val isGameOver             by viewModel.isGameOver.collectAsStateWithLifecycle()
    val gameOverReason         by viewModel.gameOverReason.collectAsStateWithLifecycle()
    val hintsRemaining         by viewModel.hintsRemaining.collectAsStateWithLifecycle()
    val isCleared              by viewModel.isCleared.collectAsStateWithLifecycle()
    val onlyAvailableNumbers   by viewModel.onlyAvailableNumbersAllowed.collectAsStateWithLifecycle()
    val conflictingSourceCells by viewModel.conflictingSourceCells.collectAsStateWithLifecycle()
    val conflictTrigger        by viewModel.conflictTrigger.collectAsStateWithLifecycle()
    val hostView = LocalView.current

    SideEffect {
        hostView.keepScreenOn =
            uiState.grid.isNotEmpty() && !isPaused && !isGameOver && !isCleared
    }
    DisposableEffect(hostView) {
        onDispose { hostView.keepScreenOn = false }
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAbandonConfirmDialog by remember { mutableStateOf(false) }

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
                puzzleInfo       = uiState.puzzleInfo,
                elapsedSeconds   = elapsedSeconds,
                timeLimitSeconds = timeLimitSeconds,
                onBack           = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Grid ─────────────────────────────────────────────────────────
            if (uiState.grid.isNotEmpty()) {
                SudokuGrid(
                    grid                   = uiState.grid,
                    gridSize               = uiState.gridSize,
                    fixedCells             = uiState.fixedCells,
                    errorCells             = uiState.errorCells,
                    hintCells              = uiState.hintCells,
                    checkCells             = uiState.checkCells,
                    questionCells           = uiState.questionCells,
                    conflictingSourceCells = conflictingSourceCells,
                    conflictTrigger        = conflictTrigger,
                    selectedCell           = selectedCell,
                    onCellClick            = { r, c -> viewModel.onCellClick(r, c) }
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

            // ── 게임판 하단: 오류 횟수/제한 회수 정보 표시 ───────────────────
            FailureCountBadge(
                failCount    = failCount,
                maxFailCount = GameViewModel.MAX_FAIL_COUNT
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── 하단 컨트롤 ────────────────────────────────────────────────
            GameBottomControls(
                hintsRemaining = hintsRemaining,
                isPaused       = isPaused,
                onClearAll     = { showClearConfirmDialog = true },
                onHint         = { viewModel.useHint() },
                onPause        = { viewModel.togglePause() },
                onAbandon      = { showAbandonConfirmDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))
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
                    availableNumbers = viewModel.getSelectableNumbers(selRow, selCol),
                    isGuided         = onlyAvailableNumbers,
                    isChecked        = selRow to selCol in uiState.checkCells,
                    hasQuestion      = selRow to selCol in uiState.questionCells,
                    onNumberSelected = { num -> viewModel.inputNumber(selRow, selCol, num) },
                    onDelete         = { viewModel.deleteNumber(selRow, selCol) },
                    onToggleCheck    = { viewModel.toggleCheck(selRow, selCol) },
                    onToggleQuestion = { viewModel.toggleQuestion(selRow, selCol) },
                    onDismiss        = { viewModel.dismissNumberPicker() }
                )
            }
        }

        // ── "모두 지우기" 확인 다이얼로그 ─────────────────────────────────────
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = {
                    Text(
                        text       = "모두 지우기",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp,
                        color      = Color(0xFF263238)
                    )
                },
                text = {
                    Text(
                        text     = "입력한 모든 숫자를 지우시겠습니까?",
                        fontSize = 16.sp,
                        color    = Color(0xFF455A64)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearConfirmDialog = false
                            viewModel.clearAllUserInput()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text       = "지우기",
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = false },
                        shape   = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text  = "취소",
                            color = Color(0xFF546E7A)
                        )
                    }
                },
                shape          = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }

        if (showAbandonConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showAbandonConfirmDialog = false },
                title = {
                    Text(
                        text = "게임을 포기할까요?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF263238)
                    )
                },
                text = {
                    Text(
                        text = "진행 시간과 입력한 숫자가 모두 초기화되고 게임 목록으로 이동합니다.",
                        fontSize = 16.sp,
                        color = Color(0xFF455A64)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAbandonConfirmDialog = false
                            viewModel.abandonCurrentPuzzle(onAbandon)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("포기", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showAbandonConfirmDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("계속하기", color = Color(0xFF546E7A))
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }

        // ── Game Over overlay ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isGameOver,
            enter   = fadeIn() + scaleIn(initialScale = 0.9f),
            exit    = fadeOut() + scaleOut()
        ) {
            val isTimeOver = gameOverReason == GameOverReason.TIME_OVER
            val reasonDesc = if (isTimeOver) {
                "제한 시간을 초과했습니다!"
            } else {
                "실패 횟수가 ${GameViewModel.MAX_FAIL_COUNT}회에 도달했습니다!"
            }
            val emoji = if (isTimeOver) "⏰" else "❌"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape     = RoundedCornerShape(28.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(emoji, fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text       = "게임 실패",
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text       = reasonDesc,
                            fontSize   = 16.sp,
                            color      = Color(0xFF546E7A),
                            textAlign  = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Stats card
                        Surface(
                            shape    = RoundedCornerShape(16.dp),
                            color    = Color(0xFFF5F7FA),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("진행 시간", fontSize = 14.sp, color = Color(0xFF78909C))
                                    val elMin = elapsedSeconds / 60
                                    val elSec = elapsedSeconds % 60
                                    val limMin = timeLimitSeconds / 60
                                    val limSec = timeLimitSeconds % 60
                                    Text(
                                        "%02d:%02d / %02d:%02d".format(elMin, elSec, limMin, limSec),
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color(0xFF263238)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("오류 횟수", fontSize = 14.sp, color = Color(0xFF78909C))
                                    Text(
                                        "$failCount / ${GameViewModel.MAX_FAIL_COUNT}",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (failCount >= GameViewModel.MAX_FAIL_COUNT) Color(0xFFD32F2F) else Color(0xFF263238)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Action buttons
                        Button(
                            onClick   = { viewModel.restartCurrentPuzzle() },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = ButtonDefaults.buttonColors(
                                containerColor = EasyColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text(
                                text       = "다시하기 ↺",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick  = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape    = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text     = "목록으로",
                                fontSize = 16.sp,
                                color    = Color(0xFF546E7A)
                            )
                        }
                    }
                }
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
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .heightIn(min = 450.dp),
                    shape  = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⏸️", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text       = "일시정지",
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text       = "준비가 되면 계속하기를 눌러주세요!",
                            fontSize   = 15.sp,
                            color      = Color(0xFF78909C),
                            textAlign  = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick   = { viewModel.togglePause() },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = ButtonDefaults.buttonColors(
                                containerColor = EasyColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text(
                                text       = "▶ 계속하기",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
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
    puzzleInfo       : String,
    elapsedSeconds   : Int,
    timeLimitSeconds : Int,
    onBack           : () -> Unit
) {
    val elapsedMinutes = elapsedSeconds / 60
    val elapsedRemainingSeconds = elapsedSeconds % 60
    val limitMinutes = timeLimitSeconds / 60
    val limitRemainingSeconds = timeLimitSeconds % 60
    val timeStr = "%02d:%02d / %02d:%02d".format(
        elapsedMinutes,
        elapsedRemainingSeconds,
        limitMinutes,
        limitRemainingSeconds
    )

    // 진행시간이 제한시간의 20% 이내만 남은 경우 (남은 시간 <= 제한시간 * 0.2)
    val remainingSeconds = (timeLimitSeconds - elapsedSeconds).coerceAtLeast(0)
    val isWarning = remainingSeconds <= (timeLimitSeconds * 0.2f)
    val timerColor = if (isWarning) Color(0xFFE53935) else Color(0xFF333333)

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter            = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "뒤로가기",
                tint               = Color(0xFF37474F),
                modifier           = Modifier.size(28.dp)
            )
        }

        Column(
            modifier              = Modifier.weight(1f),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                text       = puzzleInfo,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF78909C)
            )
            Text(
                text       = timeStr,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = timerColor
            )
        }

        // 좌측 뒤로가기 버튼과 대칭을 맞추어 타이머가 정중앙에 배치되도록 하는 더미 여백
        Spacer(modifier = Modifier.size(48.dp))
    }
}

// ── 오류 횟수 / 제한 회수 뱃지 ───────────────────────────────────────────────

@Composable
private fun FailureCountBadge(
    failCount    : Int,
    maxFailCount : Int
) {
    val bgColor = when {
        failCount >= 8 -> Color(0xFFFFEBEE)
        failCount >= 5 -> Color(0xFFFFF3E0)
        else           -> Color(0xFFECEFF1)
    }
    val borderColor = when {
        failCount >= 8 -> Color(0xFFEF5350)
        failCount >= 5 -> Color(0xFFFFB74D)
        else           -> Color(0xFFCFD8DC)
    }
    val textColor = when {
        failCount >= 8 -> Color(0xFFC62828)
        failCount >= 5 -> Color(0xFFE65100)
        else           -> Color(0xFF37474F)
    }

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = bgColor,
        border   = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.padding(top = 10.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text       = "오류 횟수: ",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF546E7A)
            )
            Text(
                text       = "$failCount / $maxFailCount",
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = textColor
            )
        }
    }
}

// ── 하단 컨트롤 (모두 지우기 & 힌트 보기 & 일시 정지) ─────────────────────────

@Composable
private fun GameBottomControls(
    hintsRemaining : Int,
    isPaused       : Boolean,
    onClearAll     : () -> Unit,
    onHint         : () -> Unit,
    onPause        : () -> Unit,
    onAbandon      : () -> Unit,
    modifier       : Modifier = Modifier
) {
    val hintEnabled = hintsRemaining > 0 && !isPaused
    val controlsEnabled = !isPaused

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameControlButton(
            emoji = "💡",
            label = "힌트",
            enabled = hintEnabled,
            enabledColor = Color(0xFFFFF9C4),
            enabledBorderColor = Color(0xFFFFB300),
            enabledTextColor = Color(0xFFE65100),
            onClick = onHint,
            trailingContent = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            color = if (hintEnabled) Color(0xFFFF9800) else Color(0xFFBDBDBD),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$hintsRemaining",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        )

        val pauseBgColor = if (isPaused) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
        val pauseBorderColor = if (isPaused) Color(0xFF66BB6A) else Color(0xFF64B5F6)
        val pauseTextColor = if (isPaused) Color(0xFF2E7D32) else Color(0xFF1565C0)
        GameControlButton(
            emoji = if (isPaused) "▶" else "⏸",
            label = if (isPaused) "계속하기" else "일시 정지",
            enabled = true,
            enabledColor = pauseBgColor,
            enabledBorderColor = pauseBorderColor,
            enabledTextColor = pauseTextColor,
            onClick = onPause
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameControlButton(
                modifier = Modifier.weight(1f),
                emoji = "🗑️",
                label = "모두 지우기",
                enabled = controlsEnabled,
                enabledColor = Color(0xFFFFEBEE),
                enabledBorderColor = Color(0xFFFFCDD2),
                enabledTextColor = Color(0xFFC62828),
                onClick = onClearAll
            )
            GameControlButton(
                modifier = Modifier.weight(1f),
                emoji = "🚪",
                label = "포기",
                enabled = controlsEnabled,
                enabledColor = Color(0xFFFFEBEE),
                enabledBorderColor = Color(0xFFFFCDD2),
                enabledTextColor = Color(0xFFC62828),
                onClick = onAbandon
            )
        }
    }
}

@Composable
private fun GameControlButton(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    enabled: Boolean,
    enabledColor: Color,
    enabledBorderColor: Color,
    enabledTextColor: Color,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val disabledColor = Color(0xFFF1F1F1)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) enabledColor else disabledColor,
        border = BorderStroke(
            width = 2.dp,
            color = if (enabled) enabledBorderColor else Color(0xFFE0E0E0)
        ),
        shadowElevation = if (enabled) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 18.sp, color = enabledTextColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) enabledTextColor else Color(0xFF9E9E9E)
            )
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(6.dp))
                trailingContent()
            }
        }
    }
}

// ── Sudoku Grid ───────────────────────────────────────────────────────────────

@Composable
fun SudokuGrid(
    grid                   : List<List<Int>>,
    gridSize               : Int,
    fixedCells             : Set<Pair<Int, Int>>,
    errorCells             : Set<Pair<Int, Int>>,
    hintCells              : Set<Pair<Int, Int>>,
    checkCells             : Set<Pair<Int, Int>>,
    questionCells          : Set<Pair<Int, Int>>,
    conflictingSourceCells : Set<Pair<Int, Int>>,
    conflictTrigger        : Long,
    selectedCell           : Pair<Int, Int>?,
    onCellClick            : (Int, Int) -> Unit
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

                    // 2. 내부 셀 경계선 및 박스 경계선 그리기
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

                    // 3. 전체 외곽 테두리
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
                    val isFixed          = row to col in fixedCells
                    val isError          = row to col in errorCells
                    val isHint           = row to col in hintCells
                    val isConflictSource = row to col in conflictingSourceCells
                    val isSelected       = selectedCell == row to col
                    val sameRow          = selectedCell?.first == row
                    val sameCol          = selectedCell?.second == col
                    val sameBox          = selectedCell?.let { (sr, sc) ->
                        (row / boxRows == sr / boxRows) && (col / boxCols == sc / boxCols)
                    } == true
                    val isLinked         = !isSelected && (sameRow || sameCol || sameBox)

                    val cellBg = when {
                        isError && isSelected -> ErrorColor.copy(alpha = 0.35f)
                        isSelected            -> SelectedHighlight
                        isHint                -> HintCellBg
                        isError               -> ErrorColor.copy(alpha = 0.20f)
                        isFixed               -> FixedCellBg
                        isLinked              -> LinkedHighlight
                        else                  -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = cellDp * col, y = cellDp * row)
                            .size(cellDp)
                            .background(cellBg)
                            .then(
                                if (isError) Modifier.border(2.dp, ErrorColor, RoundedCornerShape(4.dp))
                                else Modifier
                            )
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        // ── 원인 셀 포커스 물결 효과 (Expanding Ripple Wave Animation) ──
                        if (isConflictSource) {
                            ConflictWaveEffect(trigger = conflictTrigger)
                        }

                        val value = grid[row][col]
                        if (value != 0) {
                            // 원인 셀일 경우 살짝 튀어오르는 바운스 애니메이션
                            val textScale = if (isConflictSource && conflictTrigger > 0L) {
                                val bounce = remember(conflictTrigger) { Animatable(1f) }
                                LaunchedEffect(conflictTrigger) {
                                    bounce.snapTo(1f)
                                    bounce.animateTo(1.35f, tween(150, easing = FastOutLinearInEasing))
                                    bounce.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                                bounce.value
                            } else 1f

                            Text(
                                text = "$value",
                                modifier = Modifier.scale(textScale),
                                fontSize = when (gridSize) {
                                    4    -> 34.sp
                                    6    -> 26.sp
                                    else -> 20.sp
                                },
                                fontWeight = if (isFixed || isError || isConflictSource) FontWeight.ExtraBold else FontWeight.Bold,
                                color = when {
                                    isConflictSource -> Color(0xFFD84315) // 원인 셀은 진한 주황/적색으로 강조
                                    isError          -> ErrorColor
                                    isFixed          -> FixedCellColor
                                    isHint           -> HintNumberColor
                                    else             -> UserInputColor
                                },
                                textAlign = TextAlign.Center
                            )
                        }

                        if (row to col in checkCells) {
                            CellMarker(
                                text = "✓",
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }

                        if (row to col in questionCells) {
                            CellMarker(
                                text = "?",
                                color = Color(0xFFEF6C00),
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }

                        // 오답 입력 셀: 하단 빨간 점 인디케이터
                        if (isError && value != 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .size(6.dp)
                                    .background(ErrorColor, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellMarker(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .size(14.dp)
            .background(Color.White.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 10.sp
        )
    }
}

// ── Conflict Wave (Ripple) Effect Component ──────────────────────────────────

@Composable
private fun ConflictWaveEffect(
    trigger: Long,
    modifier: Modifier = Modifier
) {
    var lastTrigger by remember { mutableStateOf(0L) }
    val animProgress = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (trigger > 0L && trigger != lastTrigger) {
            lastTrigger = trigger
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }
    }

    val progress = animProgress.value
    if (progress < 1f) {
        // 첫 번째 확장 물결 링 (Expanding Wave Ring 1)
        val wave1Scale = 1f + progress * 0.85f
        val wave1Alpha = (1f - progress).coerceIn(0f, 1f) * 0.9f

        // 두 번째 확장 물결 링 (약간 지연되어 따라오는 Expanding Wave Ring 2)
        val p2 = ((progress - 0.20f) / 0.80f).coerceIn(0f, 1f)
        val wave2Scale = 1f + p2 * 0.70f
        val wave2Alpha = if (progress > 0.20f) (1f - p2) * 0.7f else 0f

        // 셀 배경 펄스 글로우
        val bgPulse = if (progress < 0.35f) (progress / 0.35f) else (1f - progress) / 0.65f
        val bgAlpha = bgPulse * 0.35f

        // 배경 하이라이트 글로우
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFF7043).copy(alpha = bgAlpha), RoundedCornerShape(8.dp))
        )

        // Wave Ring 1 (바깥으로 퍼져나가는 첫 번째 물결 테두리)
        Box(
            modifier = modifier
                .fillMaxSize()
                .scale(wave1Scale)
                .border(
                    width = 3.dp,
                    color = Color(0xFFFF3D00).copy(alpha = wave1Alpha),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        // Wave Ring 2 (뒤따라 퍼져나가는 두 번째 물결 테두리)
        if (progress > 0.20f) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .scale(wave2Scale)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFF9800).copy(alpha = wave2Alpha),
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }
    }
}

// ── Number Picker Overlay ─────────────────────────────────────────────────────

@Composable
fun NumberPickerOverlay(
    gridSize         : Int,
    availableNumbers : Set<Int>,
    isGuided         : Boolean,
    isChecked        : Boolean,
    hasQuestion      : Boolean,
    onNumberSelected : (Int) -> Unit,
    onDelete         : () -> Unit,
    onToggleCheck    : () -> Unit,
    onToggleQuestion : () -> Unit,
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
            modifier  = Modifier
                .padding(horizontal = 24.dp)
                .clickable { /* consume */ },
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
                    text       = if (isGuided) "입력 가능한 숫자를 선택하세요" else "숫자를 선택하세요",
                    fontSize   = 16.sp,
                    color      = if (isGuided) Color(0xFF388E3C) else Color(0xFF78909C),
                    fontWeight = FontWeight.SemiBold
                )

                val perRow = when (gridSize) {
                    4    -> 4 // 1행에 4개 (1 2 3 4)
                    6    -> 3 // 2행에 3개씩 (1 2 3 / 4 5 6)
                    else -> 3 // 3행에 3개씩 키패드 형태 (1 2 3 / 4 5 6 / 7 8 9)로 배치하여 찌그러짐 방지
                }
                val chunks = (1..gridSize).chunked(perRow)
                val buttonSize = if (gridSize == 9) 56.dp else 60.dp

                chunks.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { num ->
                            val isAvailable = num in availableNumbers
                            NumberButton(
                                number     = num,
                                available  = isAvailable,
                                isGuided   = isGuided,
                                buttonSize = buttonSize,
                                onClick    = { if (isAvailable) onNumberSelected(num) }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarkerActionButton(
                        text = "✓",
                        description = "체크 표시 전환",
                        active = isChecked,
                        activeColor = Color(0xFF2E7D32),
                        buttonSize = buttonSize,
                        onClick = onToggleCheck
                    )
                    MarkerActionButton(
                        text = "🗑",
                        description = "숫자 지우기",
                        active = false,
                        activeColor = Color(0xFFC62828),
                        backgroundColor = Color(0xFFFFEBEE),
                        buttonSize = buttonSize,
                        fontSize = if (gridSize == 9) 24.sp else 26.sp,
                        onClick = onDelete
                    )
                    MarkerActionButton(
                        text = "?",
                        description = "물음표 표시 전환",
                        active = hasQuestion,
                        activeColor = Color(0xFFEF6C00),
                        buttonSize = buttonSize,
                        onClick = onToggleQuestion
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkerActionButton(
    text: String,
    description: String,
    active: Boolean,
    activeColor: Color,
    buttonSize: androidx.compose.ui.unit.Dp,
    backgroundColor: Color = activeColor.copy(alpha = 0.12f),
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonColor = if (enabled) activeColor else Color(0xFF9E9E9E)
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(
                when {
                    !enabled -> Color(0xFFF1F1F1)
                    active -> activeColor.copy(alpha = 0.18f)
                    else -> backgroundColor
                }
            )
            .border(
                width = if (active && enabled) 2.dp else 1.dp,
                color = if (enabled) {
                    if (active) activeColor else activeColor.copy(alpha = 0.35f)
                } else {
                    Color(0xFFE0E0E0)
                },
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = buttonColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NumberButton(
    number     : Int,
    available  : Boolean,
    isGuided   : Boolean,
    buttonSize : androidx.compose.ui.unit.Dp = 60.dp,
    onClick    : () -> Unit
) {
    val bgColor = when {
        !isGuided -> Color(0xFFE3F2FD) // 자유 입력 모드: 산뜻한 파란색 활성화
        available -> Color(0xFF4CAF50).copy(alpha = 0.12f) // 가이드 모드 활성화: 초록색
        else      -> Color(0xFFF5F5F5) // 가이드 모드 비활성화: 연회색
    }

    val borderColor = when {
        !isGuided -> Color(0xFF64B5F6)
        available -> EasyColor
        else      -> Color(0xFFE0E0E0)
    }

    val textColor = when {
        !isGuided -> Color(0xFF1565C0)
        available -> Color(0xFF2E7D32)
        else      -> Color(0xFFBDBDBD)
    }

    Box(
        modifier = Modifier
            .size(buttonSize)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (available) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(enabled = available, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "$number",
            fontSize   = if (buttonSize < 60.dp) 24.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color      = textColor
        )
    }
}
