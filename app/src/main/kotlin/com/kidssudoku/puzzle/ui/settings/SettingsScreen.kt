package com.kidssudoku.puzzle.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidssudoku.puzzle.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidssudoku.puzzle.ui.theme.CreamBackground
import com.kidssudoku.puzzle.ui.theme.EasyColor
import com.kidssudoku.puzzle.ui.theme.HardColor
import com.kidssudoku.puzzle.ui.theme.MediumColor

@Composable
fun SettingsScreen(
    onBack               : () -> Unit,
    onPrivacyPolicyClick : () -> Unit,
    viewModel            : SettingsViewModel
) {
    val onlyAvailableNumbers by viewModel.onlyAvailableNumbers.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()

    var showDialog        by remember { mutableStateOf(false) }
    var pendingGridSize   by remember { mutableStateOf(0) }
    var pendingDifficulty by remember { mutableStateOf<String?>(null) } // null이면 단 전체 초기화

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
                    text       = stringResource(R.string.settings),
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Gameplay guide option (Toggle Switch) ─────────────────────────
            Text(
                text       = stringResource(R.string.game_settings),
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
                            text       = stringResource(R.string.only_available_numbers),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = if (onlyAvailableNumbers) {
                                stringResource(R.string.guided_numbers_on)
                            } else {
                                stringResource(R.string.guided_numbers_off)
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        .clickable { viewModel.setSoundEnabled(!soundEnabled) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.sound_effects),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(if (soundEnabled) R.string.sound_effects_on else R.string.sound_effects_off),
                            fontSize = 13.sp,
                            color = if (soundEnabled) EasyColor else Color(0xFF78909C)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) },
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

            // ── Grouped Reset Section ──────────────────────────────────────────
            Text(
                text       = stringResource(R.string.reset_progress),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF37474F)
            )
            Text(
                text     = stringResource(R.string.reset_progress_description),
                fontSize = 13.sp,
                color    = Color(0xFF78909C)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 4단 그룹
            GridResetGroupCard(
                gridSize = 4,
                emoji    = "🌱",
                gridLabel = stringResource(R.string.grid_4_label),
                color    = EasyColor,
                onResetAll = {
                    pendingGridSize   = 4
                    pendingDifficulty = null
                    showDialog        = true
                },
                onResetDifficulty = { diff ->
                    pendingGridSize   = 4
                    pendingDifficulty = diff
                    showDialog        = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6단 그룹
            GridResetGroupCard(
                gridSize = 6,
                emoji    = "🌿",
                gridLabel = stringResource(R.string.grid_6_label),
                color    = MediumColor,
                onResetAll = {
                    pendingGridSize   = 6
                    pendingDifficulty = null
                    showDialog        = true
                },
                onResetDifficulty = { diff ->
                    pendingGridSize   = 6
                    pendingDifficulty = diff
                    showDialog        = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 9단 그룹
            GridResetGroupCard(
                gridSize = 9,
                emoji    = "🌳",
                gridLabel = stringResource(R.string.grid_9_label),
                color    = HardColor,
                onResetAll = {
                    pendingGridSize   = 9
                    pendingDifficulty = null
                    showDialog        = true
                },
                onResetDifficulty = { diff ->
                    pendingGridSize   = 9
                    pendingDifficulty = diff
                    showDialog        = true
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFFECEFF1))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text     = stringResource(R.string.app_version),
                fontSize = 13.sp,
                color    = Color(0xFFB0BEC5)
            )
            TextButton(onClick = onPrivacyPolicyClick) {
                Text(stringResource(R.string.privacy_policy))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Confirm Dialog ─────────────────────────────────────────────────────
    if (showDialog) {
        val isAllReset = pendingDifficulty == null
        val difficultyLabel = when (pendingDifficulty) {
            "EASY" -> stringResource(R.string.difficulty_easy)
            "MEDIUM" -> stringResource(R.string.difficulty_medium)
            "HARD" -> stringResource(R.string.difficulty_hard)
            else -> ""
        }

        val titleText = if (isAllReset) {
            stringResource(R.string.reset_grid_title, pendingGridSize)
        } else {
            stringResource(R.string.reset_difficulty_title, pendingGridSize, difficultyLabel)
        }
        val messageText = if (isAllReset) {
            stringResource(R.string.reset_grid_message, pendingGridSize)
        } else {
            stringResource(R.string.reset_difficulty_message, pendingGridSize, difficultyLabel)
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text       = titleText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text     = messageText,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isAllReset) {
                            viewModel.resetGridSizeProgress(pendingGridSize)
                        } else {
                            viewModel.resetProgress(pendingGridSize, pendingDifficulty!!)
                        }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset), color = HardColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun GridResetGroupCard(
    gridSize          : Int,
    emoji             : String,
    gridLabel         : String,
    color             : Color,
    onResetAll        : () -> Unit,
    onResetDifficulty : (String) -> Unit
) {
    Surface(
        shape          = RoundedCornerShape(16.dp),
        color          = Color.White,
        border         = BorderStroke(1.5.dp, color.copy(alpha = 0.3f)),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 상단: [단 라벨] 및 [단 초기화] 버튼
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text       = gridLabel,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF263238)
                    )
                }

                OutlinedButton(
                    onClick        = onResetAll,
                    shape          = RoundedCornerShape(10.dp),
                    colors         = ButtonDefaults.outlinedButtonColors(contentColor = HardColor),
                    border         = BorderStroke(1.dp, HardColor.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text       = stringResource(R.string.reset_grid_button, gridSize),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 하단: [초급 초기화] [중급 초기화] [상급 초기화]
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val diffs = listOf(
                    Triple("EASY", R.string.difficulty_easy, EasyColor),
                    Triple("MEDIUM", R.string.difficulty_medium, MediumColor),
                    Triple("HARD", R.string.difficulty_hard, HardColor)
                )
                diffs.forEach { (diff, labelRes, diffColor) ->
                    FilledTonalButton(
                        onClick        = { onResetDifficulty(diff) },
                        modifier       = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape          = RoundedCornerShape(10.dp),
                        colors         = ButtonDefaults.filledTonalButtonColors(
                            containerColor = diffColor.copy(alpha = 0.12f),
                            contentColor   = Color(0xFF37474F)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text       = stringResource(R.string.reset_difficulty_button, stringResource(labelRes)),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
