package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.sampleTransactions
import com.example.piggybank.ui.components.BorderlessEmptyState
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.ChartCategorySlice
import com.example.piggybank.util.ChartLegendEntry
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.buildChartMonthTimeline
import com.example.piggybank.util.buildDonutLegendEntries
import com.example.piggybank.util.calculateChartCategorySlices
import com.example.piggybank.util.chartCategoryColorArgb
import com.example.piggybank.util.currentYearMonthSelection
import com.example.piggybank.util.formatChartPercentage
import com.example.piggybank.util.formatCompactRupiah
import com.example.piggybank.util.formatMonthTabLabel
import com.example.piggybank.util.toRupiah
import com.example.piggybank.util.transactionCategoryIcon
import com.example.piggybank.viewmodel.TransactionViewModel

private fun chartCategoryColor(category: String): Color =
    Color(chartCategoryColorArgb(category))

@Composable
fun ChartsScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedType by remember { mutableStateOf("expense") }

    ChartsContent(
        allTransactions = uiState.allTransactions,
        selectedMonth = uiState.selectedMonth,
        selectedType = selectedType,
        isLoading = uiState.isLoading,
        onTypeSelected = { selectedType = it },
        onMonthSelected = viewModel::selectMonth,
        modifier = modifier
    )
}

@Composable
fun ChartsContent(
    allTransactions: List<Transaction>,
    selectedMonth: YearMonthSelection,
    selectedType: String,
    isLoading: Boolean,
    onTypeSelected: (String) -> Unit,
    onMonthSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { currentYearMonthSelection() }
    var showMonthPicker by remember { mutableStateOf(false) }

    val monthTimeline = remember(allTransactions, currentMonth, selectedMonth) {
        buildChartMonthTimeline(
            transactions = allTransactions,
            currentMonth = currentMonth,
            selectedMonth = selectedMonth
        )
    }

    val categorySlices = remember(allTransactions, selectedType, selectedMonth) {
        calculateChartCategorySlices(
            transactions = allTransactions,
            type = selectedType,
            month = selectedMonth
        )
    }

    val legendEntries = remember(categorySlices) {
        buildDonutLegendEntries(categorySlices, maxEntries = 5)
    }

    val totalAmount = remember(categorySlices) {
        categorySlices.sumOf { it.totalAmount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        ChartsTopBar(
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            onCalendarClick = { showMonthPicker = true }
        )

        ChartMonthTabStrip(
            months = monthTimeline,
            selectedMonth = selectedMonth,
            currentMonth = currentMonth,
            onMonthSelected = { month -> onMonthSelected(month.year, month.month) }
        )

        HorizontalDivider(thickness = 1.dp, color = CardBorder)

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryTeal)
            }
        } else if (categorySlices.isEmpty()) {
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
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item(key = "donut_summary") {
                    DonutSummarySection(
                        totalAmount = totalAmount,
                        legendEntries = legendEntries
                    )
                    HorizontalDivider(thickness = 1.dp, color = CardBorder)
                }

                itemsIndexed(
                    items = categorySlices,
                    key = { index, slice -> "slice_${index}_${slice.category}" }
                ) { _, slice ->
                    CategoryBreakdownRow(
                        slice = slice,
                        color = chartCategoryColor(slice.category)
                    )
                    HorizontalDivider(thickness = 1.dp, color = CardBorder)
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onSelectMonth = { year, month ->
                onMonthSelected(year, month)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun ChartsTopBar(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    onCalendarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(228.dp)
                .height(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, PrimaryTeal)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val isExpense = selectedType == "expense"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isExpense) PrimaryTeal else Color.Transparent)
                        .clickable { onTypeSelected("expense") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pengeluaran",
                        fontSize = 13.sp,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (isExpense) Color.White else TextPrimary,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (!isExpense) PrimaryTeal else Color.Transparent)
                        .clickable { onTypeSelected("income") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pemasukan",
                        fontSize = 13.sp,
                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isExpense) Color.White else TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        IconButton(
            onClick = onCalendarClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Pilih Bulan",
                tint = PrimaryTeal,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ChartMonthTabStrip(
    months: List<YearMonthSelection>,
    selectedMonth: YearMonthSelection,
    currentMonth: YearMonthSelection,
    onMonthSelected: (YearMonthSelection) -> Unit
) {
    val rowState = rememberLazyListState()
    val selectedIndex = months.indexOf(selectedMonth)

    LaunchedEffect(selectedIndex, months.size) {
        if (selectedIndex >= 0) {
            rowState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = rowState,
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundCanvas),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(months, key = { _, item -> item.toYearMonthPrefix() }) { _, month ->
            val isSelected = month == selectedMonth
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .clickable { onMonthSelected(month) }
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatMonthTabLabel(month, currentMonth),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryTeal else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun DonutSummarySection(
    totalAmount: Long,
    legendEntries: List<ChartLegendEntry>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier.size(144.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(11.dp)
                ) {
                    val strokeWidthPx = 22.dp.toPx()
                    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                    val gapDegrees = if (legendEntries.size > 1) 2.2f else 0f
                    var startAngle = -90f

                    legendEntries.forEach { entry ->
                        val rawSweep = entry.fraction * 360f
                        val drawSweep = (rawSweep - gapDegrees).coerceAtLeast(1.2f)
                        drawArc(
                            color = Color(entry.colorArgb),
                            startAngle = startAngle,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            style = stroke
                        )
                        startAngle += rawSweep
                    }
                }

                Text(
                    text = formatCompactRupiah(totalAmount),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFeatureSettings = "tnum"
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                legendEntries.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val entryColor = Color(entry.colorArgb)
                        Canvas(modifier = Modifier.size(11.dp)) {
                            drawCircle(
                                color = entryColor,
                                style = Stroke(width = 3.2.dp.toPx())
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatChartPercentage(entry.percentage),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    slice: ChartCategorySlice,
    color: Color
) {
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transactionCategoryIcon(slice.category),
                    contentDescription = slice.category,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slice.category,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.totalAmount.toRupiah(),
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val labelReserve = 62.dp
                    val availableBarWidth = (maxWidth - labelReserve).coerceAtLeast(12.dp)
                    val filledBarWidth = (availableBarWidth * slice.fraction).coerceAtLeast(8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(filledBarWidth)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatChartPercentage(slice.percentage),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChartsScreenPreview() {
    PiggyBankTheme {
        ChartsContent(
            allTransactions = sampleTransactions(),
            selectedMonth = YearMonthSelection(2026, 11),
            selectedType = "expense",
            isLoading = false,
            onTypeSelected = {},
            onMonthSelected = { _, _ -> }
        )
    }
}
