package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.sampleTransactions
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.ExpenseTint
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.formatDisplayDate
import com.example.piggybank.util.toRupiah
import com.example.piggybank.util.transactionCategoryIcon
import com.example.piggybank.viewmodel.TransactionViewModel

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val resolvedId = viewModel.resolveTransactionId(transactionId)

    LaunchedEffect(transactionId) {
        viewModel.clearActionError()
        viewModel.fetchTransactionDetail(transactionId)
    }

    val transaction = uiState.selectedTransaction?.takeIf { it.id == resolvedId }
        ?: uiState.allTransactions.find { it.id == resolvedId }

    TransactionDetailContent(
        transaction = transaction,
        isDetailLoading = uiState.isDetailLoading,
        isSubmitting = uiState.isSubmitting,
        actionErrorMessage = uiState.actionErrorMessage,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
        onConfirmDelete = { id ->
            viewModel.deleteTransaction(id) {
                onDeleteSuccess()
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailContent(
    transaction: Transaction?,
    isDetailLoading: Boolean,
    isSubmitting: Boolean,
    actionErrorMessage: String?,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onConfirmDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detail Transaksi",
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
        },
        bottomBar = {
            if (transaction != null) {
                Surface(color = BackgroundCanvas) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ExpenseRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ExpenseRed
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hapus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onEditClick(transaction.id) },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
            if (isDetailLoading && transaction == null) {
                CircularProgressIndicator(
                    color = PrimaryTeal,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (transaction == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Transaksi tidak ditemukan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Data transaksi mungkin sudah dihapus atau belum tersedia.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val isIncome = transaction.type.equals("income", ignoreCase = true)
                val accentColor = if (isIncome) IncomeGreen else ExpenseRed
                val formattedAmount = transaction.amount.toRupiah(prefix = if (isIncome) "+" else "-")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (actionErrorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ExpenseTint,
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = actionErrorMessage,
                                fontSize = 13.sp,
                                color = ExpenseRed,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = transactionCategoryIcon(transaction.category),
                                    contentDescription = transaction.category,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = formattedAmount,
                                style = TextStyle(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    fontFeatureSettings = "tnum"
                                ),
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 20.dp),
                                color = CardBorder
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DetailMetadataRow(
                                    label = "Judul Transaksi",
                                    value = transaction.title,
                                    valueWeight = FontWeight.Bold
                                )
                                DetailMetadataRow(
                                    label = "Kategori",
                                    value = transaction.category,
                                    valueWeight = FontWeight.SemiBold
                                )
                                DetailMetadataRow(
                                    label = "Tanggal",
                                    value = formatDisplayDate(transaction.date),
                                    valueWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 20.dp),
                                color = CardBorder
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Catatan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = BackgroundCanvas,
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text(
                                        text = transaction.note.ifBlank { "Tidak ada catatan tambahan." },
                                        fontSize = 14.sp,
                                        color = Color(0xFF334155),
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDeleteDialog && transaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Hapus Transaksi?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Transaksi \"${transaction.title}\" akan dihapus secara permanen. Lanjutkan?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onConfirmDelete(transaction.id)
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

@Composable
private fun DetailMetadataRow(
    label: String,
    value: String,
    valueWeight: FontWeight
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = valueWeight,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionDetailScreenPreview() {
    PiggyBankTheme {
        TransactionDetailContent(
            transaction = sampleTransactions().first(),
            isDetailLoading = false,
            isSubmitting = false,
            actionErrorMessage = null,
            onBackClick = {},
            onEditClick = {},
            onConfirmDelete = {}
        )
    }
}
