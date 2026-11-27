package com.example.piggybank.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.MintSurfaceTint
import com.example.piggybank.ui.theme.OutlineGray
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.EXCEL_MIME_TYPE
import com.example.piggybank.util.ExcelExportConfig
import com.example.piggybank.util.ExportPeriodType
import com.example.piggybank.util.ProfilePreferences
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.buildExcelExportPreview
import com.example.piggybank.util.buildStatisticYearOptions
import com.example.piggybank.util.currentYearMonthSelection
import com.example.piggybank.util.fullIndonesianMonths
import com.example.piggybank.util.generateExcelWorkbookBytes
import com.example.piggybank.util.writeExcelBytesToCacheForShare
import com.example.piggybank.util.writeExcelBytesToUri
import com.example.piggybank.viewmodel.GoalViewModel
import com.example.piggybank.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ExportActionState {
    IDLE,
    SAVING,
    SHARING
}

@Composable
fun ExportExcelScreen(
    transactionViewModel: TransactionViewModel,
    goalViewModel: GoalViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences(context) }
    val txState by transactionViewModel.uiState.collectAsState()
    val goalState by goalViewModel.uiState.collectAsState()

    val displayName = remember { preferences.getDisplayName() }
    val monthlyBudgetLimit = remember { preferences.getMonthlyBudgetLimit() }
    val yearOptions = remember(txState.allTransactions) {
        buildStatisticYearOptions(txState.allTransactions)
    }

    var config by remember {
        mutableStateOf(
            ExcelExportConfig(
                periodType = ExportPeriodType.ALL,
                selectedMonth = txState.selectedMonth,
                selectedYear = txState.selectedMonth.year
            )
        )
    }
    var actionState by remember { mutableStateOf(ExportActionState.IDLE) }

    val preview = remember(txState.allTransactions, goalState.goals, config) {
        buildExcelExportPreview(
            allTransactions = txState.allTransactions,
            goals = goalState.goals,
            config = config
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(EXCEL_MIME_TYPE)
    ) { uri ->
        if (uri == null) {
            actionState = ExportActionState.IDLE
            return@rememberLauncherForActivityResult
        }
        val activeConfig = config
        val activePreview = preview
        coroutineScope.launch {
            actionState = ExportActionState.SAVING
            val result = runCatching {
                val bytes = withContext(Dispatchers.Default) {
                    generateExcelWorkbookBytes(
                        config = activeConfig,
                        preview = activePreview,
                        displayName = displayName,
                        monthlyBudgetLimit = monthlyBudgetLimit
                    )
                }
                writeExcelBytesToUri(context, uri, bytes).getOrThrow()
            }
            actionState = ExportActionState.IDLE
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("File ${activePreview.fileName} berhasil disimpan.")
            } else {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.message ?: "Gagal menyimpan file Excel."
                )
            }
        }
    }

    ExportExcelContent(
        config = config,
        yearOptions = yearOptions,
        actionState = actionState,
        snackbarHostState = snackbarHostState,
        onConfigChange = { config = it },
        onSaveClick = {
            if (!config.hasAnySheetSelected || actionState != ExportActionState.IDLE) return@ExportExcelContent
            try {
                saveDocumentLauncher.launch(preview.fileName)
            } catch (_: ActivityNotFoundException) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Pemilih dokumen tidak tersedia di perangkat ini.")
                }
            }
        },
        onShareClick = {
            if (!config.hasAnySheetSelected || actionState != ExportActionState.IDLE) return@ExportExcelContent
            val activeConfig = config
            val activePreview = preview
            coroutineScope.launch {
                actionState = ExportActionState.SHARING
                val shareUriResult = runCatching {
                    val bytes = withContext(Dispatchers.Default) {
                        generateExcelWorkbookBytes(
                            config = activeConfig,
                            preview = activePreview,
                            displayName = displayName,
                            monthlyBudgetLimit = monthlyBudgetLimit
                        )
                    }
                    writeExcelBytesToCacheForShare(context, activePreview.fileName, bytes).getOrThrow()
                }
                actionState = ExportActionState.IDLE

                shareUriResult.onSuccess { uri ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = EXCEL_MIME_TYPE
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan PiggyBank (${activePreview.periodLabel})")
                        clipData = ClipData.newRawUri(activePreview.fileName, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Bagikan Laporan Excel")
                        )
                    } catch (_: ActivityNotFoundException) {
                        snackbarHostState.showSnackbar("Tidak ada aplikasi yang dapat membuka atau membagikan file.")
                    }
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(
                        err.message ?: "Gagal membuat file Excel untuk dibagikan."
                    )
                }
            }
        },
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportExcelContent(
    config: ExcelExportConfig,
    yearOptions: List<Int>,
    actionState: ExportActionState,
    snackbarHostState: SnackbarHostState,
    onConfigChange: (ExcelExportConfig) -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    val isBusy = actionState != ExportActionState.IDLE
    val canExport = config.hasAnySheetSelected && !isBusy

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Excel",
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
            Surface(
                color = BackgroundCanvas,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSaveClick,
                        enabled = canExport,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (actionState == ExportActionState.SAVING) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simpan ke Perangkat",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onShareClick,
                        enabled = canExport,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (canExport) PrimaryTeal else CardBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (actionState == ExportActionState.SHARING) {
                            CircularProgressIndicator(
                                color = PrimaryTeal,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bagikan File (.xlsx)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Kartu 1: Filter Periode Transaksi
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Periode Transaksi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportPeriodType.entries.forEach { period ->
                            val selected = config.periodType == period
                            Surface(
                                onClick = {
                                    onConfigChange(config.copy(periodType = period))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = CircleShape,
                                color = if (selected) PrimaryTeal else BackgroundCanvas,
                                border = if (selected) null else BorderStroke(1.dp, OutlineGray)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = period.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (selected) Color.White else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (config.periodType == ExportPeriodType.MONTH) {
                        val fullMonthName =
                            fullIndonesianMonths[(config.selectedMonth.month - 1).coerceIn(0, 11)]
                        PeriodSelectorBox(
                            label = "Bulan Terpilih",
                            value = "$fullMonthName ${config.selectedMonth.year}",
                            onClick = { showMonthPicker = true }
                        )
                    } else if (config.periodType == ExportPeriodType.YEAR) {
                        PeriodSelectorBox(
                            label = "Tahun Terpilih",
                            value = "Tahun ${config.selectedYear}",
                            onClick = { showYearPicker = true }
                        )
                    }
                }
            }

            // Kartu 2: Pilih Sheet yang Disertakan
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Konten Sheet Excel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pilih data yang ingin dimasukkan ke dalam file .xlsx",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ExportSheetCheckboxRow(
                        title = "Ringkasan Keuangan",
                        subtitle = "Total aset, pemasukan, pengeluaran, anggaran, & rekap kategori",
                        checked = config.includeSummary,
                        onCheckedChange = { onConfigChange(config.copy(includeSummary = it)) }
                    )
                    HorizontalDivider(color = CardBorder)
                    ExportSheetCheckboxRow(
                        title = "Riwayat Transaksi",
                        subtitle = "Daftar transaksi kas lengkap beserta tanggal, kategori, & catatan",
                        checked = config.includeTransactions,
                        onCheckedChange = { onConfigChange(config.copy(includeTransactions = it)) }
                    )
                    HorizontalDivider(color = CardBorder)
                    ExportSheetCheckboxRow(
                        title = "Target Celengan",
                        subtitle = "Seluruh target tabungan, saldo terkumpul, progress, & tenggat",
                        checked = config.includeGoals,
                        onCheckedChange = { onConfigChange(config.copy(includeGoals = it)) }
                    )

                    if (!config.hasAnySheetSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih minimal 1 sheet untuk dapat mengekspor file Excel.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = config.selectedMonth,
            onDismiss = { showMonthPicker = false },
            onSelectMonth = { year, month ->
                onConfigChange(
                    config.copy(selectedMonth = YearMonthSelection(year = year, month = month))
                )
                showMonthPicker = false
            }
        )
    }

    if (showYearPicker) {
        ExportYearPickerDialog(
            yearOptions = yearOptions,
            selectedYear = config.selectedYear,
            onDismiss = { showYearPicker = false },
            onSelectYear = { year ->
                onConfigChange(config.copy(selectedYear = year))
                showYearPicker = false
            }
        )
    }
}

@Composable
private fun PeriodSelectorBox(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = BackgroundCanvas,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Ubah",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ExportSheetCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryTeal,
                uncheckedColor = OutlineGray,
                checkmarkColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ExportYearPickerDialog(
    yearOptions: List<Int>,
    selectedYear: Int,
    onDismiss: () -> Unit,
    onSelectYear: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(yearOptions, selectedYear) {
        val targetIdx = yearOptions.indexOf(selectedYear)
        if (targetIdx >= 0) {
            listState.scrollToItem((targetIdx - 2).coerceAtLeast(0))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pilih Tahun",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(yearOptions, key = { it }) { year ->
                        val isSelected = year == selectedYear
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MintSurfaceTint else Color.Transparent)
                                .clickable { onSelectYear(year) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = year.toString(),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Batal", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExportExcelScreenPreview() {
    val config = ExcelExportConfig(
        periodType = ExportPeriodType.MONTH,
        selectedMonth = currentYearMonthSelection()
    )
    PiggyBankTheme {
        ExportExcelContent(
            config = config,
            yearOptions = listOf(2026, 2025, 2024),
            actionState = ExportActionState.IDLE,
            snackbarHostState = remember { SnackbarHostState() },
            onConfigChange = {},
            onSaveClick = {},
            onShareClick = {},
            onBackClick = {}
        )
    }
}
