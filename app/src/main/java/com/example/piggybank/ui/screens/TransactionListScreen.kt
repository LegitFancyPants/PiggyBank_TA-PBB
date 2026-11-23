package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.sampleTransactions
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.DarkTeal
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.ExpenseTint
import com.example.piggybank.ui.theme.HeroExpenseArrow
import com.example.piggybank.ui.theme.HeroIncomeArrow
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.IncomeTint
import com.example.piggybank.ui.theme.MintSurfaceTint
import com.example.piggybank.ui.theme.OutlineGray
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.SoftMint
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.toRupiah
import com.example.piggybank.util.transactionCategoryIcon
import com.example.piggybank.viewmodel.TransactionFilter
import com.example.piggybank.viewmodel.TransactionUiState
import com.example.piggybank.viewmodel.TransactionViewModel

@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onTransactionClick: (Long) -> Unit,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    TransactionListContent(
        uiState = uiState,
        onFilterSelected = viewModel::setFilter,
        onTransactionClick = onTransactionClick,
        onAddTransactionClick = onAddTransactionClick,
        onRefresh = viewModel::fetchTransactions,
        modifier = modifier
    )
}

@Composable
fun TransactionListContent(
    uiState: TransactionUiState,
    onFilterSelected: (TransactionFilter) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransactionClick: () -> Unit,
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
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TransactionTopBar()
            }

            item {
                HeroBalanceSummaryCard(
                    netBalance = uiState.netBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (uiState.isUsingMockData) {
                item {
                    MockDataBanner(
                        message = uiState.errorMessage
                            ?: "Mode lokal aktif. Tekan Coba Lagi untuk sinkronisasi server.",
                        onRefresh = onRefresh,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Transaksi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.filteredTransactions.size} transaksi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
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
            } else if (uiState.filteredTransactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(
                    items = uiState.filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddTransactionClick,
            shape = RoundedCornerShape(16.dp),
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Transaksi"
            )
        }
    }
}

@Composable
private fun TransactionTopBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundCanvas)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftMint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Logo PiggyBank",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "PiggyBank",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(CardSurface)
                .border(width = 1.dp, color = CardBorder, shape = CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nov 2026",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun HeroBalanceSummaryCard(
    netBalance: Long,
    totalIncome: Long,
    totalExpense: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryTeal, DarkTeal)
                )
            )
            .padding(20.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopEnd)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Saldo Bersih Saat Ini",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SoftMint
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = netBalance.toRupiah(),
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricSubCard(
                    label = "Pemasukan",
                    amount = totalIncome.toRupiah(),
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    iconTint = HeroIncomeArrow,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricSubCard(
                    label = "Pengeluaran",
                    amount = totalExpense.toRupiah(),
                    icon = Icons.AutoMirrored.Filled.CallMade,
                    iconTint = HeroExpenseArrow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricSubCard(
    label: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SoftMint
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = amount,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MockDataBanner(
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
private fun FilterChipsRow(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CardSurface,
                    labelColor = TextSecondary,
                    selectedContainerColor = PrimaryTeal,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = OutlineGray,
                    borderWidth = 1.dp,
                    selectedBorderColor = Color.Transparent,
                    selectedBorderWidth = 0.dp
                )
            )
        }
    }
}

@Composable
private fun TransactionItemCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type.equals("income", ignoreCase = true)
    val iconBackground = if (isIncome) IncomeTint else ExpenseTint
    val iconTint = if (isIncome) IncomeGreen else ExpenseRed
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val formattedAmount = transaction.amount.toRupiah(prefix = if (isIncome) "+" else "-")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transactionCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • ${transaction.date}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedAmount,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyTransactionsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Belum ada catatan transaksi",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tekan tombol + di kanan bawah untuk mencatat pemasukan atau pengeluaran baru.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionListScreenPreview() {
    val samples = sampleTransactions()
    val totalIncome = samples.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = samples.filter { it.type == "expense" }.sumOf { it.amount }
    PiggyBankTheme {
        TransactionListContent(
            uiState = TransactionUiState(
                allTransactions = samples,
                filteredTransactions = samples,
                selectedFilter = TransactionFilter.ALL,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = totalIncome - totalExpense,
                isLoading = false
            ),
            onFilterSelected = {},
            onTransactionClick = {},
            onAddTransactionClick = {},
            onRefresh = {}
        )
    }
}
