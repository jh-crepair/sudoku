package com.kidssudoku.puzzle.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidssudoku.puzzle.ui.theme.CreamBackground
import com.kidssudoku.puzzle.ui.theme.EasyColor
import com.kidssudoku.puzzle.ui.theme.HardColor
import com.kidssudoku.puzzle.ui.theme.MediumColor

@Composable
fun SettingsScreen(
    onBack    : () -> Unit,
    viewModel : SettingsViewModel
) {
    val onlyAvailableNumbers by viewModel.onlyAvailableNumbers.collectAsStateWithLifecycle()

    var showDialog        by remember { mutableStateOf(false) }
    var pendingGridSize   by remember { mutableStateOf(0) }
    var pendingDifficulty by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text(text = "←", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text       = "설정",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Gameplay guide option (Toggle Switch) ─────────────────────────
            Text(
                text       = "🎮  게임 가이드 설정",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF37474F)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.setOnlyAvailableNumbers(!onlyAvailableNumbers) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "입력 가능한 숫자만 선택",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = if (onlyAvailableNumbers) {
                                "빈칸 터치 시 규칙상 입력 가능한 숫자만 가이드합니다."
                            } else {
                                "비활성화 상태: 모든 숫자를 자유롭게 입력할 수 있습니다."
                            },
                            fontSize = 13.sp,
                            color    = if (onlyAvailableNumbers) EasyColor else Color(0xFF78909C)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = onlyAvailableNumbers,
                        onCheckedChange = { viewModel.setOnlyAvailableNumbers(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EasyColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCFD8DC),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Reset section ──────────────────────────────────────────────
            Text(
                text       = "🔄  진행 초기화",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF37474F)
            )
            Text(
                text     = "선택한 단계의 모든 진행 상황을 처음으로 되돌립니다.",
                fontSize = 13.sp,
                color    = Color(0xFF78909C)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val combos = listOf(
                Triple(4, "EASY",   "4단  초급"),
                Triple(4, "MEDIUM", "4단  중급"),
                Triple(4, "HARD",   "4단  고급"),
                Triple(6, "EASY",   "6단  초급"),
                Triple(6, "MEDIUM", "6단  중급"),
                Triple(6, "HARD",   "6단  고급"),
                Triple(9, "EASY",   "9단  초급"),
                Triple(9, "MEDIUM", "9단  중급"),
                Triple(9, "HARD",   "9단  고급")
            )

            combos.forEach { (gs, diff, label) ->
                ResetRow(
                    label   = label,
                    color   = when (diff) {
                        "EASY" -> EasyColor; "MEDIUM" -> MediumColor; else -> HardColor
                    },
                    onClick = {
                        pendingGridSize   = gs
                        pendingDifficulty = diff
                        showDialog        = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFFECEFF1))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text     = "어린이 스도쿠  v1.0.0",
                fontSize = 13.sp,
                color    = Color(0xFFB0BEC5)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Confirm Dialog ─────────────────────────────────────────────────────
    if (showDialog) {
        val diffKr = when (pendingDifficulty) {
            "EASY" -> "초급"; "MEDIUM" -> "중급"; else -> "고급"
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text       = "진행 초기화",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text     = "${pendingGridSize}단 $diffKr 의 모든 진행 상황을\n" +
                               "초기화할까요? 되돌릴 수 없습니다.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProgress(pendingGridSize, pendingDifficulty)
                        showDialog = false
                    }
                ) {
                    Text("초기화", color = HardColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun ResetRow(
    label   : String,
    color   : Color,
    onClick : () -> Unit
) {
    Surface(
        shape     = RoundedCornerShape(12.dp),
        color     = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF37474F)
            )
            OutlinedButton(
                onClick = onClick,
                shape   = RoundedCornerShape(10.dp),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = HardColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("초기화", fontSize = 14.sp)
            }
        }
    }
}
