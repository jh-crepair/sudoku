package com.kidssudoku.puzzle.ui.puzzlelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kidssudoku.puzzle.data.model.Progress
import com.kidssudoku.puzzle.data.model.Puzzle
import com.kidssudoku.puzzle.data.model.PuzzleStatus
import com.kidssudoku.puzzle.ui.theme.*

@Composable
fun PuzzleListScreen(
    gridSize: Int,
    difficulty: String,
    onPuzzleSelected: (puzzleId: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: PuzzleListViewModel
) {
    LaunchedEffect(gridSize, difficulty) {
        viewModel.load(gridSize, difficulty)
    }

    val puzzles     by viewModel.puzzles.collectAsStateWithLifecycle()
    val progressMap by viewModel.progressMap.collectAsStateWithLifecycle()

    val diffColor = when (difficulty) {
        "EASY" -> EasyColor; "MEDIUM" -> MediumColor; else -> HardColor
    }
    val diffLabel = when (difficulty) {
        "EASY" -> "초급"; "MEDIUM" -> "중급"; else -> "고급"
    }
    val gridLabel = when (gridSize) { 4 -> "4단"; 6 -> "6단"; else -> "9단" }
    val cleared   = progressMap.values.count { it.status == PuzzleStatus.CLEARED.name }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text(text = "←", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "$gridLabel  $diffLabel",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                    Text(
                        text = "$cleared / 20 완료",
                        fontSize = 13.sp,
                        color = Color(0xFF78909C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (puzzles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = diffColor)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(puzzles, key = { it.id }) { puzzle ->
                        val progress = progressMap[puzzle.id]
                        val status   = progress?.status ?: PuzzleStatus.LOCKED.name
                        PuzzleCell(
                            orderIndex = puzzle.orderIndex,
                            status     = status,
                            stars      = progress?.starRating ?: 0,
                            onClick    = {
                                if (status != PuzzleStatus.LOCKED.name) {
                                    onPuzzleSelected(puzzle.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleCell(
    orderIndex: Int,
    status: String,
    stars: Int,
    onClick: () -> Unit
) {
    val isLocked  = status == PuzzleStatus.LOCKED.name
    val isCleared = status == PuzzleStatus.CLEARED.name

    val bgColor = when {
        isCleared -> Color(0xFFE8F5E9)
        isLocked  -> Color(0xFFEEEEEE)
        else      -> Color.White
    }
    val textColor = when {
        isCleared -> EasyColor
        isLocked  -> Color(0xFFBDBDBD)
        else      -> Color(0xFF333333)
    }

    Card(
        modifier  = Modifier
            .aspectRatio(1f)
            .alpha(if (isLocked) 0.55f else 1f)
            .clickable(enabled = !isLocked, onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLocked) 0.dp else 4.dp
        )
    ) {
        Box(
            modifier          = Modifier.fillMaxSize(),
            contentAlignment  = Alignment.Center
        ) {
            if (isLocked) {
                Text(
                    text     = "🔒",
                    fontSize = 22.sp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "$orderIndex",
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color      = textColor
                    )
                    if (isCleared && stars > 0) {
                        Row {
                            repeat(stars) {
                                Text(text = "★", fontSize = 11.sp, color = StarColor)
                            }
                            repeat(3 - stars) {
                                Text(text = "☆", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
