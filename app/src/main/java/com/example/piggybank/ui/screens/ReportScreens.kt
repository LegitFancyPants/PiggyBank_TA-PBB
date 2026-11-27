package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.piggybank.ui.components.BorderlessEmptyState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.sampleTransactions
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
import com.example.piggybank.util.MonthlyStatRow
import com.example.piggybank.util.ProfilePreferences
import com.example.piggybank.util.ThousandSeparatorVisualTransformation
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.buildStatisticYearOptions
import com.example.piggybank.util.calculateMonthExpense
import com.example.piggybank.util.calculateMonthIncome
import com.example.piggybank.util.calculateMonthlyBudgetUsage
import com.example.piggybank.util.calculateMonthlyStatisticsByYear
import com.example.piggybank.util.currentYearMonthSelection
import com.example.piggybank.util.formatChartPercentage
import com.example.piggybank.util.indonesianMonths
import com.example.piggybank.util.sanitizeNominalInput
import com.example.piggybank.util.toRupiah
import com.example.piggybank.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private fun formatGroupedNumber(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
        isGroupingUsed = true
        maximumFractionDigits = 0
    }
    val formatted = formatter.format(abs(amount))
    return if (amount < 0L) "-$formatted" else formatted
}

// =========================================================================
// LAYAR 1: LAPORAN (RINGKASAN STATISTIK BULANAN & ANGGARAN BULANAN)
// =========================================================================

@Composable
fun ReportScreen(
    transactionViewModel: TransactionViewModel,
    onBackClick: () -> Unit,
    onMonthlyStatisticsClick: () -> Unit,
    onMonthlyBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences(context) }
    val txState by transactionViewModel.uiState.collectAsState()
    val currentMonth = remember { currentYearMonthSelection() }

    var monthlyBudgetLimit by remember { mutableStateOf(preferences.getMonthlyBudgetLimit()) }
    LaunchedEffect(Unit) {
        monthlyBudgetLimit = preferences.getMonthlyBudgetLimit()
    }

    val monthExpense = remember(txState.allTransactions, currentMonth) {
        calculateMonthExpense(txState.allTransactions, currentMonth)
    }
    val monthIncome = remember(txState.allTransactions, currentMonth) {
        calculateMonthIncome(txState.allTransactions, currentMonth)
    }
    val budgetUsage = remember(monthExpense, monthlyBudgetLimit) {
        calculateMonthlyBudgetUsage(
            usedExpense = monthExpense,
            budgetLimit = monthlyBudgetLimit
        )
    }

    ReportContent(
        currentMonth = currentMonth,
        monthExpense = monthExpense,
        monthIncome = monthIncome,
        totalBalance = txState.netBalance,
        budgetUsage = budgetUsage,
        onBackClick = onBackClick,
        onMonthlyStatisticsClick = onMonthlyStatisticsClick,
        onMonthlyBudgetClick = onMonthlyBudgetClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContent(
    currentMonth: YearMonthSelection,
    monthExpense: Long,
    monthIncome: Long,
    totalBalance: Long,
    budgetUsage: MonthlyBudgetUsage,
    onBackClick: () -> Unit,
    onMonthlyStatisticsClick: () -> Unit,
    onMonthlyBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthShort = indonesianMonths[(currentMonth.month - 1).coerceIn(0, 11)]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Laporan",
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
            // Card 1: Statistik Bulanan - <Bulan>
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onMonthlyStatisticsClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Statistik Bulanan - $monthShort",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Lihat Statistik Bulanan",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportMetricColumn(
                            label = "Pengeluaran",
                            value = monthExpense.toRupiah(),
                            valueColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricColumn(
                            label = "Pemasukan",
                            value = monthIncome.toRupiah(),
                            valueColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricColumn(
                            label = "Total Saldo",
                            value = totalBalance.toRupiah(),
                            valueColor = if (totalBalance < 0L) ExpenseRed else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Card 2: Anggaran Bulanan
            MonthlyBudgetSummaryCard(
                budgetUsage = budgetUsage,
                onCardClick = onMonthlyBudgetClick,
                showChevron = true,
                onEditClick = null
            )
        }
    }
}

@Composable
private fun ReportMetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MonthlyBudgetSummaryCard(
    budgetUsage: MonthlyBudgetUsage,
    onCardClick: (() -> Unit)?,
    showChevron: Boolean,
    onEditClick: (() -> Unit)?
) {
    val hasLimit = budgetUsage.budgetLimit > 0L
    val isOverBudget = budgetUsage.isOverBudget

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .let { base ->
                if (onCardClick != null) base.clickable(onClick = onCardClick) else base
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anggaran Bulanan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Lihat Detail Anggaran",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (onEditClick != null) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ubah Anggaran Bulanan",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BudgetLineRow(
                        label = "Anggaran :",
                        value = if (hasLimit) budgetUsage.budgetLimit.toRupiah() else "Belum diatur",
                        valueColor = if (hasLimit) TextPrimary else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BudgetLineRow(
                        label = "Pengeluaran :",
                        value = budgetUsage.usedExpense.toRupiah(),
                        valueColor = TextPrimary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = CardBorder
                    )
                    BudgetLineRow(
                        label = "Sisa :",
                        value = if (hasLimit) budgetUsage.signedRemainingAmount.toRupiah() else "-",
                        valueColor = when {
                            !hasLimit -> TextSecondary
                            isOverBudget -> ExpenseRed
                            else -> IncomeGreen
                        },
                        valueWeight = FontWeight.Bold
                    )
                }

                BudgetCircularGauge(
                    budgetUsage = budgetUsage,
                    size = 94.dp
                )
            }
        }
    }
}

@Composable
private fun BudgetLineRow(
    label: String,
    value: String,
    valueColor: Color,
    valueWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = valueWeight,
                color = valueColor,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BudgetCircularGauge(
    budgetUsage: MonthlyBudgetUsage,
    size: Dp
) {
    val hasLimit = budgetUsage.budgetLimit > 0L
    val isOverBudget = budgetUsage.isOverBudget
    val ringColor = when {
        !hasLimit -> CardBorder
        isOverBudget -> ExpenseRed
        else -> PrimaryTeal
    }
    val sweepAngle = when {
        !hasLimit -> 0f
        isOverBudget -> 360f
        else -> (budgetUsage.usageFraction * 360f).coerceIn(0f, 360f)
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = CardBorder,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            if (sweepAngle > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = stroke
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = if (hasLimit) formatChartPercentage(budgetUsage.usagePercentageExact) else "0%",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) ExpenseRed else TextPrimary,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    !hasLimit -> "Belum diatur"
                    isOverBudget -> "Melebihi"
                    else -> "Terpakai"
                },
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// LAYAR 2: DETAIL STATISTIK BULANAN (TABEL PER BULAN & FILTER TAHUN)
// =========================================================================

@Composable
fun MonthlyStatisticsScreen(
    transactionViewModel: TransactionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val txState by transactionViewModel.uiState.collectAsState()
    MonthlyStatisticsContent(
        allTransactions = txState.allTransactions,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatisticsContent(
    allTransactions: List<Transaction>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val yearOptions = remember(allTransactions) {
        buildStatisticYearOptions(allTransactions)
    }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }

    val yearlyGroups = remember(allTransactions, selectedYear) {
        calculateMonthlyStatisticsByYear(allTransactions, filterYear = selectedYear)
    }

    val summaryIncome = remember(yearlyGroups) {
        yearlyGroups.sumOf { group -> group.months.sumOf { it.totalIncome } }
    }
    val summaryExpense = remember(yearlyGroups) {
        yearlyGroups.sumOf { group -> group.months.sumOf { it.totalExpense } }
    }
    val summaryBalance = summaryIncome - summaryExpense

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistik Bulanan",
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
                    Box {
                        Row(
                            modifier = Modifier
                                .onSizeChanged { anchorHeightPx = it.height }
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { yearMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedYear?.toString() ?: "Semua",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Filter Tahun",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (yearMenuExpanded) {
                            YearFilterPopup(
                                yearOptions = yearOptions,
                                selectedYear = selectedYear,
                                anchorHeightPx = anchorHeightPx,
                                onSelectYear = { year ->
                                    selectedYear = year
                                    yearMenuExpanded = false
                                },
                                onDismiss = { yearMenuExpanded = false }
                            )
                        }
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
        ) {
            // Hero Total Saldo + Pengeluaran | Pemasukan
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Saldo",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = summaryBalance.toRupiah(),
                        style = TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summaryBalance < 0L) ExpenseRed else TextPrimary,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pengeluaran",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summaryExpense.toRupiah(),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(34.dp),
                            color = CardBorder
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pemasukan",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summaryIncome.toRupiah(),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = CardBorder)

            // Header Tabel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundCanvas)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bulan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.weight(0.75f)
                )
                Text(
                    text = "Pengeluaran",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Pemasukan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Saldo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.05f)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = CardBorder)

            if (yearlyGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BorderlessEmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        message = "Tidak ada catatan"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    yearlyGroups.forEach { group ->
                        item(key = "year_${group.year}") {
                            Text(
                                text = group.year.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BackgroundCanvas)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(thickness = 1.dp, color = CardBorder)
                        }
                        items(
                            items = group.months,
                            key = { row -> "${row.year}-${row.month}" }
                        ) { row ->
                            MonthlyStatTableRow(row = row)
                            HorizontalDivider(thickness = 1.dp, color = CardBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearFilterPopup(
    yearOptions: List<Int>,
    selectedYear: Int?,
    anchorHeightPx: Int,
    onSelectYear: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = remember { currentYearMonthSelection().year }
    val targetYear = selectedYear ?: currentYear
    val initialIndex = remember(yearOptions, targetYear) {
        val idx = yearOptions.indexOf(targetYear)
        if (idx >= 0) (idx - 1).coerceAtLeast(0) else 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, anchorHeightPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier.width(152.dp),
            shape = RoundedCornerShape(12.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                YearFilterRowItem(
                    label = "Semua",
                    isSelected = selectedYear == null,
                    onClick = { onSelectYear(null) }
                )
                HorizontalDivider(thickness = 1.dp, color = CardBorder)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 264.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = yearOptions,
                        key = { it }
                    ) { year ->
                        YearFilterRowItem(
                            label = year.toString(),
                            isSelected = selectedYear == year,
                            onClick = { onSelectYear(year) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearFilterRowItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryTeal else TextPrimary
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MonthlyStatTableRow(row: MonthlyStatRow) {
    val monthLabel = indonesianMonths[(row.month - 1).coerceIn(0, 11)]
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(0.75f)
            )
            Text(
                text = formatGroupedNumber(row.totalExpense),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontFeatureSettings = "tnum"
                ),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = formatGroupedNumber(row.totalIncome),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontFeatureSettings = "tnum"
                ),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = formatGroupedNumber(row.balance),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (row.balance < 0L) ExpenseRed else IncomeGreen,
                    fontFeatureSettings = "tnum"
                ),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.05f)
            )
        }
    }
}

// =========================================================================
// LAYAR 3: DETAIL ANGGARAN BULANAN (BUDGET + PEMILIH BULAN + PENGATURAN)
// =========================================================================

@Composable
fun MonthlyBudgetScreen(
    transactionViewModel: TransactionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences(context) }
    val txState by transactionViewModel.uiState.collectAsState()

    var selectedMonth by remember { mutableStateOf(currentYearMonthSelection()) }
    var monthlyBudgetLimit by remember { mutableStateOf(preferences.getMonthlyBudgetLimit()) }

    val monthExpense = remember(txState.allTransactions, selectedMonth) {
        calculateMonthExpense(txState.allTransactions, selectedMonth)
    }
    val budgetUsage = remember(monthExpense, monthlyBudgetLimit) {
        calculateMonthlyBudgetUsage(
            usedExpense = monthExpense,
            budgetLimit = monthlyBudgetLimit
        )
    }

    MonthlyBudgetContent(
        selectedMonth = selectedMonth,
        budgetUsage = budgetUsage,
        onSelectMonth = { year, month -> selectedMonth = YearMonthSelection(year, month) },
        onSaveBudgetLimit = { newLimit ->
            preferences.setMonthlyBudgetLimit(newLimit)
            monthlyBudgetLimit = preferences.getMonthlyBudgetLimit()
        },
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBudgetContent(
    selectedMonth: YearMonthSelection,
    budgetUsage: MonthlyBudgetUsage,
    onSelectMonth: (Int, Int) -> Unit,
    onSaveBudgetLimit: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Anggaran",
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
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMonthPicker = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = selectedMonth.formatLabel(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Pilih Bulan",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
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
            Surface(color = BackgroundCanvas) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showBudgetDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pengaturan Anggaran",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
            MonthlyBudgetSummaryCard(
                budgetUsage = budgetUsage,
                onCardClick = { showBudgetDialog = true },
                showChevron = false,
                onEditClick = { showBudgetDialog = true }
            )
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onSelectMonth = { year, month ->
                onSelectMonth(year, month)
                showMonthPicker = false
            }
        )
    }

    if (showBudgetDialog) {
        EditMonthlyBudgetDialog(
            initialLimit = budgetUsage.budgetLimit,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { newLimit ->
                onSaveBudgetLimit(newLimit)
                showBudgetDialog = false
            }
        )
    }
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
                text = "Pengaturan Anggaran Bulanan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tentukan batas pengeluaran bulanan untuk memantau pemakaian kas setiap bulannya.",
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReportScreenPreview() {
    PiggyBankTheme {
        ReportContent(
            currentMonth = YearMonthSelection(2026, 11),
            monthExpense = 7_227_895L,
            monthIncome = 7_142_386L,
            totalBalance = 1_034_656L,
            budgetUsage = calculateMonthlyBudgetUsage(7_227_895L, 3_000_000L),
            onBackClick = {},
            onMonthlyStatisticsClick = {},
            onMonthlyBudgetClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MonthlyStatisticsScreenPreview() {
    PiggyBankTheme {
        MonthlyStatisticsContent(
            allTransactions = sampleTransactions(),
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MonthlyBudgetScreenPreview() {
    PiggyBankTheme {
        MonthlyBudgetContent(
            selectedMonth = YearMonthSelection(2026, 11),
            budgetUsage = calculateMonthlyBudgetUsage(7_227_895L, 3_000_000L),
            onSelectMonth = { _, _ -> },
            onSaveBudgetLimit = {},
            onBackClick = {}
        )
    }
}
