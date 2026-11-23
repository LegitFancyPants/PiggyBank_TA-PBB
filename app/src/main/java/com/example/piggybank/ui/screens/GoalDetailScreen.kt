package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.toRupiah
import com.example.piggybank.viewmodel.GoalViewModel

@Composable
fun GoalDetailScreen(
    goalId: Long,
    viewModel: GoalViewModel,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(goalId) {
        viewModel.fetchGoalDetail(goalId)
    }

    val goal = uiState.selectedGoal?.takeIf { it.id == goalId }
        ?: uiState.goals.find { it.id == goalId }

    GoalDetailContent(
        goal = goal,
        isDetailLoading = uiState.isDetailLoading,
        isSubmitting = uiState.isSubmitting,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
        onAddDeposit = { targetGoal, depositAmount, onDone ->
            viewModel.addDeposit(targetGoal, depositAmount, onDone)
        },
        onConfirmDelete = { id ->
            viewModel.deleteGoal(id) {
                onDeleteSuccess()
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailContent(
    goal: SavingsGoal?,
    isDetailLoading: Boolean,
    isSubmitting: Boolean,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onAddDeposit: (SavingsGoal, Long, () -> Unit) -> Unit,
    onConfirmDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDepositDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var depositInput by remember { mutableStateOf("") }
    var depositError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detail Target",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (goal != null) {
                        IconButton(
                            onClick = { onEditClick(goal.id) },
                            enabled = !isSubmitting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Target",
                                tint = Color(0xFF475569)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSubmitting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Target",
                                tint = ExpenseRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundCanvas
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
            if (goal != null) {
                Surface(color = BackgroundCanvas) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                depositInput = ""
                                depositError = null
                                showDepositDialog = true
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "Tambah Setoran",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isDetailLoading && goal == null) {
                CircularProgressIndicator(
                    color = PrimaryTeal,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (goal == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Target tabungan tidak ditemukan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Data celengan mungkin sudah dihapus atau belum tersedia.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val isOverdue = goal.isOverdue()
                val heroBorderColor = if (isOverdue) ExpenseRed.copy(alpha = 0.45f) else CardBorder
                val deadlineTint = if (isOverdue) ExpenseRed else TextSecondary

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, heroBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = goal.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Tenggat",
                                        tint = deadlineTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (goal.deadline.isNotBlank()) {
                                            "Tenggat: ${goal.deadline}"
                                        } else {
                                            "Tanpa batas tenggat"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal,
                                        color = deadlineTint
                                    )
                                }

                                if (isOverdue) {
                                    Surface(
                                        shape = CircleShape,
                                        color = ExpenseTint
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WarningAmber,
                                                contentDescription = "Melewati tenggat",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(12.dp)
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
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${goal.progressPercentage}%",
                                    style = TextStyle(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryTeal,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                                Text(
                                    text = "tercapai",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { goal.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(CircleShape),
                                color = PrimaryTeal,
                                trackColor = CardBorder,
                                drawStopIndicator = {}
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MintSurfaceTint
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Sudah Terkumpul",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryTeal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = goal.currentAmount.toRupiah(),
                                            style = TextStyle(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkTeal,
                                                fontFeatureSettings = "tnum"
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = BackgroundCanvas
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Sisa Kekurangan",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = goal.remainingAmount.toRupiah(),
                                            style = TextStyle(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontFeatureSettings = "tnum"
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Total Target: ${goal.targetAmount.toRupiah()}",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontFeatureSettings = "tnum"
                                )
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = CardBorder
                            )

                            Text(
                                text = "Catatan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = goal.note.ifBlank { "Tidak ada catatan motivasi." },
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDepositDialog && goal != null) {
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Tambah Setoran Tabungan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Masukkan nominal yang ingin ditambahkan ke celengan \"${goal.name}\":",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = depositInput,
                        onValueChange = { input ->
                            depositInput = input.filter { it.isDigit() }
                            if (depositError != null) depositError = null
                        },
                        label = { Text("Nominal Setoran (Rp)") },
                        prefix = {
                            Text(
                                text = "Rp ",
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = PrimaryTeal,
                            cursorColor = PrimaryTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (depositError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = depositError ?: "",
                            fontSize = 12.sp,
                            color = ExpenseRed
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = depositInput.toLongOrNull() ?: 0L
                        if (amount <= 0L) {
                            depositError = "Nominal setoran harus lebih dari Rp 0."
                        } else {
                            depositError = null
                            onAddDeposit(goal, amount) {
                                showDepositDialog = false
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Simpan Setoran",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text(
                        text = "Batal",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    if (showDeleteDialog && goal != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Hapus Target Tabungan?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Target tabungan \"${goal.name}\" akan dihapus secara permanen. Lanjutkan?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onConfirmDelete(goal.id)
                    }
                ) {
                    Text(
                        text = "Hapus",
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = "Batal",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GoalDetailScreenPreview() {
    PiggyBankTheme {
        GoalDetailContent(
            goal = sampleSavingsGoals().first(),
            isDetailLoading = false,
            isSubmitting = false,
            onBackClick = {},
            onEditClick = {},
            onAddDeposit = { _, _, _ -> },
            onConfirmDelete = {}
        )
    }
}
