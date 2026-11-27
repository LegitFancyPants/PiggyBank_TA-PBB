package com.example.piggybank.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionSortOption
import com.example.piggybank.model.groupTransactionsForDisplay
import com.example.piggybank.model.sampleTransactions
import com.example.piggybank.ui.components.BorderlessEmptyState
import com.example.piggybank.ui.components.rememberRemainingLazyListHeight
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.DarkTeal
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.HeroExpenseArrow
import com.example.piggybank.ui.theme.HeroIncomeArrow
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.MintSurfaceTint
import com.example.piggybank.ui.theme.OutlineGray
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.SoftMint
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.formatDateWithDay
import com.example.piggybank.util.fullIndonesianMonths
import com.example.piggybank.util.indonesianMonths
import com.example.piggybank.util.toRupiah
import com.example.piggybank.util.transactionCategoryIcon
import com.example.piggybank.viewmodel.TransactionFilter
import com.example.piggybank.viewmodel.TransactionUiState
import com.example.piggybank.viewmodel.TransactionViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    TransactionListContent(
        uiState = uiState,
        onFilterSelected = viewModel::setFilter,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSortOptionSelected = viewModel::setSortOption,
        onMonthSelected = viewModel::selectMonth,
        onPreviousMonth = viewModel::goToPreviousMonth,
        onNextMonth = viewModel::goToNextMonth,
        onTransactionClick = onTransactionClick,
        onRefresh = viewModel::fetchTransactions,
        modifier = modifier
    )
}

@Composable
fun TransactionListContent(
    uiState: TransactionUiState,
    onFilterSelected: (TransactionFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelected: (TransactionSortOption) -> Unit,
    onMonthSelected: (Int, Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMonthPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        TransactionTopBar(
            selectedMonth = uiState.selectedMonth,
            onMonthClick = { showMonthPicker = true }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = uiState.selectedMonth,
                transitionSpec = {
                    when {
                        uiState.monthChangeDirection < 0 -> {
                            (slideInVertically(tween(260)) { it / 4 } + fadeIn(tween(240)))
                                .togetherWith(slideOutVertically(tween(220)) { -it / 4 } + fadeOut(tween(180)))
                        }
                        uiState.monthChangeDirection > 0 -> {
                            (slideInVertically(tween(260)) { -it / 4 } + fadeIn(tween(240)))
                                .togetherWith(slideOutVertically(tween(220)) { it / 4 } + fadeOut(tween(180)))
                        }
                        else -> {
                            fadeIn(tween(220)).togetherWith(fadeOut(tween(180)))
                        }
                    }
                },
                label = "MonthTransition",
                modifier = Modifier.fillMaxSize()
            ) { activeMonth ->
                MonthTransactionsPage(
                    uiState = uiState,
                    activeMonth = activeMonth,
                    onFilterSelected = onFilterSelected,
                    onSearchQueryChange = onSearchQueryChange,
                    onSortOptionSelected = onSortOptionSelected,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onTransactionClick = onTransactionClick,
                    onRefresh = onRefresh
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = uiState.selectedMonth,
            onDismiss = { showMonthPicker = false },
            onSelectMonth = { year, month ->
                onMonthSelected(year, month)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun MonthTransactionsPage(
    uiState: TransactionUiState,
    activeMonth: YearMonthSelection,
    onFilterSelected: (TransactionFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelected: (TransactionSortOption) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    val currentOnPreviousMonth by rememberUpdatedState(onPreviousMonth)
    val currentOnNextMonth by rememberUpdatedState(onNextMonth)

    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 72.dp.toPx() } }
    val maxPullPx = remember(density) { with(density) { 112.dp.toPx() } }

    var rawOverscrollPx by remember(activeMonth) { mutableFloatStateOf(0f) }
    var hadScrolledInGesture by remember(activeMonth) { mutableStateOf(false) }

    val animatedOverscrollPx by animateFloatAsState(
        targetValue = rawOverscrollPx,
        animationSpec = spring(stiffness = 600f),
        label = "MonthOverscroll"
    )

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            hadScrolledInGesture = false
        }
    }

    val nestedScrollConnection = remember(activeMonth, listState, thresholdPx, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (rawOverscrollPx > 0f && available.y < 0f) {
                        val updated = (rawOverscrollPx + available.y * 0.55f).coerceAtLeast(0f)
                        rawOverscrollPx = updated
                        return Offset(0f, available.y)
                    }
                    if (rawOverscrollPx < 0f && available.y > 0f) {
                        val updated = (rawOverscrollPx + available.y * 0.55f).coerceAtMost(0f)
                        rawOverscrollPx = updated
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                if (consumed.y != 0f) {
                    hadScrolledInGesture = true
                    return Offset.Zero
                }
                if (!hadScrolledInGesture) {
                    if (!listState.canScrollBackward && available.y > 0f) {
                        rawOverscrollPx = (rawOverscrollPx + available.y * 0.45f)
                            .coerceIn(0f, maxPullPx)
                        return Offset(0f, available.y)
                    }
                    if (!listState.canScrollForward && available.y < 0f) {
                        rawOverscrollPx = (rawOverscrollPx + available.y * 0.45f)
                            .coerceIn(-maxPullPx, 0f)
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                val triggeredNext = rawOverscrollPx >= thresholdPx
                val triggeredPrev = rawOverscrollPx <= -thresholdPx
                rawOverscrollPx = 0f
                hadScrolledInGesture = false
                if (triggeredNext) {
                    currentOnNextMonth()
                    return available
                }
                if (triggeredPrev) {
                    currentOnPreviousMonth()
                    return available
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                rawOverscrollPx = 0f
                hadScrolledInGesture = false
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    val emptyAnchorIndex = 3
    val emptyAreaHeight = rememberRemainingLazyListHeight(
        listState = listState,
        anchorItemIndex = emptyAnchorIndex
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .offset { IntOffset(0, animatedOverscrollPx.roundToInt()) },
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = if (uiState.groupedTransactions.isEmpty()) 0.dp else 28.dp
            )
        ) {
            item {
                HeroBalanceSummaryCard(
                    netBalance = uiState.netBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    monthShortLabel = indonesianMonths[(activeMonth.month - 1).coerceIn(0, 11)],
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            item {
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            item {
                TransactionSearchAndSortBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    selectedSortOption = uiState.selectedSortOption,
                    onSortOptionSelected = onSortOptionSelected,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
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
            } else if (uiState.groupedTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = emptyAreaHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyTransactionsState(
                            searchQuery = uiState.searchQuery,
                            onClearSearch = { onSearchQueryChange("") }
                        )
                    }
                }
            } else {
                uiState.groupedTransactions.forEachIndexed { groupIndex, group ->
                    item(key = "header_${groupIndex}_${group.date}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (groupIndex == 0) 4.dp else 12.dp)
                        ) {
                            DailyTransactionHeader(
                                date = group.date,
                                totalIncome = group.totalIncome,
                                totalExpense = group.totalExpense,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(thickness = 1.dp, color = CardBorder)
                        }
                    }
                    itemsIndexed(
                        items = group.transactions,
                        key = { index, transaction -> "tx_${groupIndex}_${index}_${transaction.id}" }
                    ) { _, transaction ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TransactionItemCard(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                            HorizontalDivider(thickness = 1.dp, color = CardBorder)
                        }
                    }
                }
            }
        }

        if (abs(animatedOverscrollPx) > 12f) {
            val isPullingNext = animatedOverscrollPx > 0f
            val readyToSwitch = abs(rawOverscrollPx) >= thresholdPx
            val targetLabel = if (isPullingNext) {
                activeMonth.nextMonth().formatLabel()
            } else {
                activeMonth.previousMonth().formatLabel()
            }
            val hintText = if (readyToSwitch) {
                "Lepas untuk ke $targetLabel"
            } else if (isPullingNext) {
                "Tarik ke bawah untuk $targetLabel"
            } else {
                "Tarik ke atas untuk $targetLabel"
            }

            Surface(
                shape = CircleShape,
                color = if (readyToSwitch) PrimaryTeal else CardSurface,
                border = BorderStroke(1.dp, if (readyToSwitch) PrimaryTeal else CardBorder),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(if (isPullingNext) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPullingNext) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (readyToSwitch) Color.White else PrimaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = hintText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (readyToSwitch) Color.White else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionTopBar(
    selectedMonth: YearMonthSelection,
    onMonthClick: () -> Unit,
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
            Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = "Logo PiggyBank",
                tint = PrimaryTeal,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PiggyBank",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onMonthClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = selectedMonth.formatLabel(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Pilih Bulan",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun MonthYearPickerDialog(
    selectedMonth: YearMonthSelection,
    onDismiss: () -> Unit,
    onSelectMonth: (Int, Int) -> Unit
) {
    var draftYear by remember(selectedMonth) { mutableIntStateOf(selectedMonth.year) }
    var draftMonth by remember(selectedMonth) { mutableIntStateOf(selectedMonth.month) }
    var isSelectingYear by remember { mutableStateOf(false) }

    val yearRange = remember { (1900..2100).toList() }
    val yearGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = (draftYear - 1900 - 3).coerceIn(0, yearRange.lastIndex)
    )

    LaunchedEffect(isSelectingYear) {
        if (isSelectingYear) {
            val targetIndex = (draftYear - 1900 - 3).coerceIn(0, yearRange.lastIndex)
            yearGridState.scrollToItem(targetIndex)
        }
    }

    val fullMonthTitle = "${fullIndonesianMonths[(draftMonth - 1).coerceIn(0, 11)]} $draftYear"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = fullMonthTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isSelectingYear = !isSelectingYear }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = draftYear.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelectingYear) PrimaryTeal else TextSecondary
                        )
                        Icon(
                            imageVector = if (isSelectingYear) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = "Pilih Tahun",
                            tint = if (isSelectingYear) PrimaryTeal else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (draftYear > 1900) draftYear-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Tahun Sebelumnya",
                                tint = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { if (draftYear < 2100) draftYear++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Tahun Berikutnya",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSelectingYear) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = yearGridState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(164.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(yearRange, key = { it }) { yearOption ->
                            val isSelected = yearOption == draftYear
                            Surface(
                                onClick = {
                                    draftYear = yearOption
                                    isSelectingYear = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = CircleShape,
                                color = if (isSelected) PrimaryTeal else Color.Transparent
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = yearOption.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(164.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        for (rowIndex in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (colIndex in 0 until 4) {
                                    val monthNumber = rowIndex * 4 + colIndex + 1
                                    val isSelected = monthNumber == draftMonth
                                    Surface(
                                        onClick = { draftMonth = monthNumber },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        shape = CircleShape,
                                        color = if (isSelected) PrimaryTeal else Color.Transparent
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = indonesianMonths[monthNumber - 1],
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                    TextButton(
                        onClick = { onSelectMonth(draftYear, draftMonth) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Konfirmasi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBalanceSummaryCard(
    netBalance: Long,
    totalIncome: Long,
    totalExpense: Long,
    monthShortLabel: String,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricSubCard(
                    label = "Pemasukan ($monthShortLabel)",
                    amount = totalIncome.toRupiah(),
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    iconTint = HeroIncomeArrow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                SummaryMetricSubCard(
                    label = "Pengeluaran ($monthShortLabel)",
                    amount = totalExpense.toRupiah(),
                    icon = Icons.AutoMirrored.Filled.CallMade,
                    iconTint = HeroExpenseArrow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                letterSpacing = (-0.2).sp,
                fontWeight = FontWeight.Medium,
                color = SoftMint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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
            Surface(
                onClick = { onFilterSelected(filter) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = CircleShape,
                color = if (isSelected) PrimaryTeal else CardSurface,
                border = if (isSelected) null else BorderStroke(1.dp, OutlineGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionSearchAndSortBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSortOption: TransactionSortOption,
    onSortOptionSelected: (TransactionSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Cari transaksi...",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryTeal
            ),
            modifier = Modifier.weight(1f)
        )

        Box {
            Surface(
                onClick = { sortMenuExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = CardSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (sortMenuExpanded) PrimaryTeal else CardBorder
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Urutkan transaksi",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                containerColor = CardSurface
            ) {
                TransactionSortOption.entries.forEach { option ->
                    val isSelected = option == selectedSortOption
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) PrimaryTeal else TextPrimary
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            onSortOptionSelected(option)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTransactionHeader(
    date: String,
    totalIncome: Long,
    totalExpense: Long,
    modifier: Modifier = Modifier
) {
    val summaryText = when {
        totalIncome > 0L && totalExpense > 0L ->
            "Masuk: ${totalIncome.toRupiah()}  •  Keluar: ${totalExpense.toRupiah()}"
        totalIncome > 0L ->
            "Pemasukan: ${totalIncome.toRupiah()}"
        totalExpense > 0L ->
            "Pengeluaran: ${totalExpense.toRupiah()}"
        else -> ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateWithDay(date),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (summaryText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = summaryText,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    val iconBackground = if (isIncome) IncomeGreen else ExpenseRed
    val iconTint = Color.White
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val formattedAmount = transaction.amount.toRupiah(prefix = if (isIncome) "+" else "-")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transactionCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
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
                    text = transaction.category,
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
private fun EmptyTransactionsState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmedQuery = searchQuery.trim()
    val isSearching = trimmedQuery.isNotEmpty()

    BorderlessEmptyState(
        icon = if (isSearching) Icons.Default.Search else Icons.AutoMirrored.Filled.ReceiptLong,
        message = if (isSearching) {
            "Transaksi \"$trimmedQuery\" tidak ditemukan"
        } else {
            "Tidak ada catatan"
        },
        actionLabel = if (isSearching) "Hapus Pencarian" else null,
        onActionClick = if (isSearching) onClearSearch else null,
        messageColor = if (isSearching) TextPrimary else TextSecondary,
        modifier = modifier
    )
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
                groupedTransactions = groupTransactionsForDisplay(samples),
                selectedFilter = TransactionFilter.ALL,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = totalIncome - totalExpense,
                isLoading = false
            ),
            onFilterSelected = {},
            onSearchQueryChange = {},
            onSortOptionSelected = {},
            onMonthSelected = { _, _ -> },
            onPreviousMonth = {},
            onNextMonth = {},
            onTransactionClick = {},
            onRefresh = {}
        )
    }
}
