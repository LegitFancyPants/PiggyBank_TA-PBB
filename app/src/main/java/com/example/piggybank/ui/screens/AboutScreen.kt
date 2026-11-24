package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.MonthlyBudgetUsage
import com.example.piggybank.util.ProfilePreferences
import com.example.piggybank.util.ThousandSeparatorVisualTransformation
import com.example.piggybank.util.calculateMonthlyBudgetUsage
import com.example.piggybank.util.calculateTotalAssets
import com.example.piggybank.util.sanitizeNominalInput
import com.example.piggybank.util.toRupiah
import com.example.piggybank.viewmodel.GoalViewModel
import com.example.piggybank.viewmodel.TransactionViewModel

// =========================================================================
// BIODATA PRAKTIKAN
// =========================================================================
private const val DEVELOPER_NAME = "Nugroho Ghathfaan Rajendra"
private const val DEVELOPER_NIM = "21120124130048"
private const val DEVELOPER_GROUP = "Kelompok 09 - Praktikum PPB 2026"

@Composable
fun ProfileScreen(
    transactionViewModel: TransactionViewModel,
    goalViewModel: GoalViewModel,
    onAboutDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences(context) }

    val txState by transactionViewModel.uiState.collectAsState()
    val goalState by goalViewModel.uiState.collectAsState()

    var displayName by remember { mutableStateOf(preferences.getDisplayName()) }
    var monthlyBudgetLimit by remember { mutableStateOf(preferences.getMonthlyBudgetLimit()) }

    val totalAssets = remember(txState.allTransactions, goalState.goals) {
        calculateTotalAssets(txState.allTransactions, goalState.goals)
    }
    val budgetUsage = remember(txState.totalExpense, monthlyBudgetLimit) {
        calculateMonthlyBudgetUsage(
            usedExpense = txState.totalExpense,
            budgetLimit = monthlyBudgetLimit
        )
    }

    ProfileContent(
        displayName = displayName,
        totalAssets = totalAssets,
        budgetUsage = budgetUsage,
        onSaveDisplayName = { newName ->
            preferences.setDisplayName(newName)
            displayName = preferences.getDisplayName()
        },
        onSaveMonthlyBudget = { newLimit ->
            preferences.setMonthlyBudgetLimit(newLimit)
            monthlyBudgetLimit = preferences.getMonthlyBudgetLimit()
        },
        onAboutDetailClick = onAboutDetailClick,
        modifier = modifier
    )
}

@Composable
fun ProfileContent(
    displayName: String,
    totalAssets: Long,
    budgetUsage: MonthlyBudgetUsage,
    onSaveDisplayName: (String) -> Unit,
    onSaveMonthlyBudget: (Long) -> Unit,
    onAboutDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val avatarInitial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Profil Personal (Avatar Inisial + Nama Editable + Total Aset)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showNameDialog = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ubah Nama",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Total Aset: ${totalAssets.toRupiah()}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Kartu Sorotan: Anggaran Bulanan (dinamis menyesuaikan pengeluaran di Kas)
        MonthlyBudgetHighlightCard(
            budgetUsage = budgetUsage,
            onClick = { showBudgetDialog = true }
        )

        // Kartu Menu Profil
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileMenuRow(
                    icon = Icons.Default.Info,
                    title = "Tentang Aplikasi",
                    onClick = onAboutDetailClick
                )
            }
        }
    }

    if (showNameDialog) {
        EditDisplayNameDialog(
            initialName = displayName,
            onDismiss = { showNameDialog = false },
            onConfirm = { newName ->
                onSaveDisplayName(newName)
                showNameDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        EditMonthlyBudgetDialog(
            initialLimit = budgetUsage.budgetLimit,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { newLimit ->
                onSaveMonthlyBudget(newLimit)
                showBudgetDialog = false
            }
        )
    }
}

@Composable
private fun MonthlyBudgetHighlightCard(
    budgetUsage: MonthlyBudgetUsage,
    onClick: () -> Unit
) {
    val hasLimit = budgetUsage.budgetLimit > 0L
    val isOverBudget = budgetUsage.isOverBudget
    val accentColor = if (isOverBudget) ExpenseRed else PrimaryTeal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverBudget) ExpenseRed.copy(alpha = 0.45f) else CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anggaran Bulanan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (!hasLimit) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (budgetUsage.usedExpense > 0L) {
                                "Pengeluaran tercatat ${budgetUsage.usedExpense.toRupiah()} • Ketuk untuk atur batas"
                            } else {
                                "Atur batas pengeluaran bulan ini"
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (hasLimit) {
                    Text(
                        text = if (isOverBudget) {
                            "${budgetUsage.usagePercentage}% • Melebihi batas"
                        } else {
                            "${budgetUsage.usagePercentage}%"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFeatureSettings = "tnum"
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (hasLimit) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { budgetUsage.usageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = accentColor,
                    trackColor = CardBorder,
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${budgetUsage.usedExpense.toRupiah()} / ${budgetUsage.budgetLimit.toRupiah()}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isOverBudget) {
                            "Sisa ${budgetUsage.remainingAmount.toRupiah()}"
                        } else {
                            "Lebih ${budgetUsage.overBudgetAmount.toRupiah()}"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isOverBudget) IncomeGreen else ExpenseRed,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PrimaryTeal,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EditDisplayNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Ubah Nama Profil",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it.take(40) },
                label = { Text("Nama Panggilan") },
                placeholder = { Text("Masukkan nama...", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = PrimaryTeal,
                    cursorColor = PrimaryTeal
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputName) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Simpan", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EditMonthlyBudgetDialog(
    initialLimit: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var budgetInput by remember(initialLimit) {
        mutableStateOf(if (initialLimit > 0L) initialLimit.toString() else "")
    }
    val thousandSeparator = remember { ThousandSeparatorVisualTransformation() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Atur Anggaran Bulanan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tentukan batas pengeluaran bulanan untuk memantau pemakaian kas bulan ini.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { input ->
                        budgetInput = sanitizeNominalInput(input)
                    },
                    label = { Text("Batas Anggaran (Rp)") },
                    prefix = {
                        Text(
                            text = "Rp ",
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    },
                    supportingText = {
                        Text(
                            text = "Kosongkan atau isi 0 untuk menonaktifkan batas anggaran",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = thousandSeparator,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = PrimaryTeal,
                        cursorColor = PrimaryTeal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(budgetInput.toLongOrNull() ?: 0L) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Simpan", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tentang Aplikasi",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundCanvas
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Logo PiggyBank",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "PiggyBank",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Versi 1.0.0",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aplikasi tracking arus kas harian dan manajemen target tabungan pribadi berbasis Android Native (Jetpack Compose) dan RESTful API.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = CardBorder
                    )

                    Text(
                        text = "Dikembangkan Oleh",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AboutMetadataRow(
                            label = "Nama",
                            value = DEVELOPER_NAME
                        )
                        AboutMetadataRow(
                            label = "NIM",
                            value = DEVELOPER_NIM
                        )
                        AboutMetadataRow(
                            label = "Kelompok",
                            value = DEVELOPER_GROUP
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "Tech Stack",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Kotlin, Jetpack Compose, Express.js, Supabase",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutMetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 16.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    PiggyBankTheme {
        ProfileContent(
            displayName = "Nugroho Ghathfaan",
            totalAssets = 3_450_000L,
            budgetUsage = calculateMonthlyBudgetUsage(
                usedExpense = 620_000L,
                budgetLimit = 1_500_000L
            ),
            onSaveDisplayName = {},
            onSaveMonthlyBudget = {},
            onAboutDetailClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AboutDetailScreenPreview() {
    PiggyBankTheme {
        AboutDetailScreen(onBackClick = {})
    }
}
