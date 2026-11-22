package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.sampleSavingsGoals
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.DarkTeal
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.ExpenseTint
import com.example.piggybank.ui.theme.MintSurfaceTint
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.SoftMint
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.toRupiah
import com.example.piggybank.viewmodel.GoalUiState
import com.example.piggybank.viewmodel.GoalViewModel

@Composable
fun GoalListScreen(
    viewModel: GoalViewModel,
    onGoalClick: (Long) -> Unit,
    onAddGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    GoalListContent(
        uiState = uiState,
        onGoalClick = onGoalClick,
        onAddGoalClick = onAddGoalClick,
        onRefresh = viewModel::fetchGoals,
        modifier = modifier
    )
}

@Composable
fun GoalListContent(
    uiState: GoalUiState,
    onGoalClick: (Long) -> Unit,
    onAddGoalClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Target Celengan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wujudkan target finansialmu bertahap",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            item {
                GoalSummaryBanner(
                    totalActiveGoals = uiState.totalActiveGoals,
                    totalSavedAllGoals = uiState.totalSavedAllGoals
                )
            }

            if (uiState.isUsingMockData && uiState.errorMessage != null) {
                item {
                    GoalMockDataBanner(
                        message = uiState.errorMessage,
                        onRefresh = onRefresh
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                }
            } else if (uiState.goals.isEmpty()) {
                item {
                    EmptyGoalsCard()
                }
            } else {
                items(
                    items = uiState.goals,
                    key = { it.id }
                ) { goal ->
                    SavingsGoalItemCard(
                        goal = goal,
                        onClick = { onGoalClick(goal.id) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddGoalClick,
            shape = RoundedCornerShape(16.dp),
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Target"
                )
            },
            text = {
                Text(
                    text = "Target Baru",
                    fontWeight = FontWeight.SemiBold
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun GoalSummaryBanner(
    totalActiveGoals: Int,
    totalSavedAllGoals: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SoftMint
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Ringkasan Celengan",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$totalActiveGoals Target Aktif",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total terkumpul ${totalSavedAllGoals.toRupiah()}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryTeal,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }
    }
}

@Composable
private fun GoalMockDataBanner(
    message: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MintSurfaceTint),
        border = BorderStroke(1.dp, SoftMint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = DarkTeal,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Coba Lagi",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Coba Lagi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )
            }
        }
    }
}

@Composable
private fun SavingsGoalItemCard(
    goal: SavingsGoal,
    onClick: () -> Unit
) {
    val isOverdue = goal.isOverdue()
    val cardBorderColor = if (isOverdue) ExpenseRed.copy(alpha = 0.45f) else CardBorder
    val deadlineColor = if (isOverdue) ExpenseRed else TextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = goal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isOverdue) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseTint
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Melewati tenggat",
                                tint = ExpenseRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Lewat tenggat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = SoftMint
                ) {
                    Text(
                        text = "${goal.progressPercentage}%",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = PrimaryTeal,
                trackColor = CardBorder,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goal.currentAmount.toRupiah()} / ${goal.targetAmount.toRupiah()}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Tenggat",
                        tint = deadlineColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (goal.deadline.isNotBlank()) "Target: ${goal.deadline}" else "Tanpa tenggat",
                        fontSize = 12.sp,
                        fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal,
                        color = deadlineColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MintSurfaceTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Belum ada target celengan. Tekan + Target Baru untuk memulai!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GoalListScreenPreview() {
    val samples = sampleSavingsGoals()
    PiggyBankTheme {
        GoalListContent(
            uiState = GoalUiState(
                goals = samples,
                totalActiveGoals = samples.size,
                totalSavedAllGoals = samples.sumOf { it.currentAmount },
                isLoading = false
            ),
            onGoalClick = {},
            onAddGoalClick = {},
            onRefresh = {}
        )
    }
}
