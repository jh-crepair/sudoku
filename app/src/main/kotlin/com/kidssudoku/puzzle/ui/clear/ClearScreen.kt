package com.kidssudoku.puzzle.ui.clear

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidssudoku.puzzle.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ClearScreen(
    stars          : Int,
    elapsedSeconds : Int,
    onNextPuzzle   : () -> Unit,
    onBackToList   : () -> Unit
) {
    val minutes  = elapsedSeconds / 60
    val seconds  = elapsedSeconds % 60
    val timeStr  = "%02d:%02d".format(minutes, seconds)

    // Bounce animation for the emoji
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val scale by animateFloatAsState(
        targetValue    = if (visible) 1f else 0.3f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label          = "emoji_scale"
    )

    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            // Celebration emoji
            Text(
                text     = "🎉",
                fontSize = 90.sp,
                modifier = Modifier.scale(scale)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text       = "완성했어요!",
                fontSize   = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Star rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    val starVisible = remember { mutableStateOf(false) }
                    LaunchedEffect(visible) {
                        if (visible) {
                            delay((i * 200).toLong())
                            starVisible.value = true
                        }
                    }
                    val starScale by animateFloatAsState(
                        targetValue   = if (starVisible.value) 1f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label         = "star_$i"
                    )
                    Text(
                        text       = if (i <= stars) "★" else "☆",
                        fontSize   = 56.sp,
                        color      = if (i <= stars) StarColor else Color(0xFFCFD8DC),
                        modifier   = Modifier.scale(starScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Time card
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 36.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text     = "⏱  걸린 시간",
                        fontSize = 16.sp,
                        color    = Color(0xFF78909C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text       = timeStr,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick  = onNextPuzzle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("다음 문제  →", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick  = onBackToList,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("목록으로", fontSize = 20.sp)
            }
        }
    }
}
