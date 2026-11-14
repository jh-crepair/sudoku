package com.kidssudoku.puzzle.ui.home

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
fun HomeScreen(
    onGridSizeSelected: (gridSize: Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(text = "⚙️", fontSize = 28.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(text = "🍒", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "체리의 스도쿠",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFC62845)
            )
            Text(
                text = "체리와 함께하는 숫자 퍼즐",
                fontSize = 18.sp,
                color = Color(0xFF607D8B)
            )

            Spacer(modifier = Modifier.height(42.dp))

            // Grid size cards - enlarged box size for perfect text containment
            GridSizeCard(
                emoji    = "🌱",
                label    = "4단",
                subtitle = "1~4 숫자 · 간단하게",
                color    = EasyColor,
                onClick  = { onGridSizeSelected(4) }
            )
            Spacer(modifier = Modifier.height(18.dp))
            GridSizeCard(
                emoji    = "🌿",
                label    = "6단",
                subtitle = "1~6 숫자 · 차근차근",
                color    = MediumColor,
                onClick  = { onGridSizeSelected(6) }
            )
            Spacer(modifier = Modifier.height(18.dp))
            GridSizeCard(
                emoji    = "🌳",
                label    = "9단",
                subtitle = "1~9 숫자 · 클래식 스도쿠",
                color    = HardColor,
                onClick  = { onGridSizeSelected(9) }
            )
        }
    }
}

@Composable
private fun GridSizeCard(
    emoji: String,
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
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
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 40.sp)
                Spacer(modifier = Modifier.width(16.dp))
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF607D8B)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "→",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}
