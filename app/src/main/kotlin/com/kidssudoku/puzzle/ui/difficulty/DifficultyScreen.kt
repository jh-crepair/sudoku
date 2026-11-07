package com.kidssudoku.puzzle.ui.difficulty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidssudoku.puzzle.ui.theme.*

@Composable
fun DifficultyScreen(
    gridSize: Int,
    onDifficultySelected: (difficulty: String) -> Unit,
    onBack: () -> Unit
) {
    val gridLabel = when (gridSize) { 4 -> "4단"; 6 -> "6단"; else -> "9단" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text(text = "←", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$gridLabel 난이도 선택",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            DifficultyCard(
                emoji       = "😊",
                label       = "초급",
                description = "쉽고 재미있게 시작해요!",
                color       = EasyColor,
                onClick     = { onDifficultySelected("EASY") }
            )
            Spacer(modifier = Modifier.height(18.dp))
            DifficultyCard(
                emoji       = "🤔",
                label       = "중급",
                description = "조금 더 생각해봐요!",
                color       = MediumColor,
                onClick     = { onDifficultySelected("MEDIUM") }
            )
            Spacer(modifier = Modifier.height(18.dp))
            DifficultyCard(
                emoji       = "😤",
                label       = "상급",
                description = "도전! 최고 난이도!",
                color       = HardColor,
                onClick     = { onDifficultySelected("HARD") }
            )
        }
    }
}

@Composable
private fun DifficultyCard(
    emoji: String,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(2.dp, color.copy(alpha = 0.35f)),
        shadowElevation = 3.dp,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 42.sp)
                Spacer(modifier = Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF607D8B)
                    )
                }
                Text(
                    text = "→",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}
