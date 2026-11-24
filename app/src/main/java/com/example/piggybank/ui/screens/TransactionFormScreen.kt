package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.OutlineGray
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.DateIsoVisualTransformation
import com.example.piggybank.util.ThousandSeparatorVisualTransformation
import com.example.piggybank.util.formatDateDigitsToIso
import com.example.piggybank.util.isoDateToUtcMillis
import com.example.piggybank.util.isValidCalendarDate
import com.example.piggybank.util.millisToIsoDate
import com.example.piggybank.util.sanitizeDateDigits
import com.example.piggybank.util.sanitizeNominalInput
import com.example.piggybank.util.todayIsoDate
import com.example.piggybank.viewmodel.TransactionViewModel

private val expenseCategories = listOf(
    "Makanan & Minuman",
    "Transportasi",
    "Belanja",
    "Tagihan & Utilitas",
    "Pendidikan",
    "Lainnya"
)

private val incomeCategories = listOf(
    "Gaji & Uang Saku",
    "Bonus & Hadiah",
    "Freelance",
    "Lainnya"
)

@Composable
fun TransactionFormScreen(
    transactionId: Long?,
    viewModel: TransactionViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val editId = transactionId?.takeIf { it > 0L }
    val isEditMode = editId != null

    LaunchedEffect(editId) {
        viewModel.clearActionError()
        if (editId != null) {
            viewModel.fetchTransactionDetail(editId)
        }
    }

    val existingTransaction = if (editId != null) {
        uiState.selectedTransaction?.takeIf { it.id == editId }
            ?: uiState.allTransactions.find { it.id == editId }
    } else {
        null
    }

    TransactionFormContent(
        isEditMode = isEditMode,
        existingTransaction = existingTransaction,
        isSubmitting = uiState.isSubmitting,
        actionErrorMessage = uiState.actionErrorMessage,
        onBackClick = onBackClick,
        onSubmit = { request ->
            if (editId != null) {
                viewModel.updateTransaction(editId, request) {
                    onSaveSuccess()
                }
            } else {
                viewModel.createTransaction(request) {
                    onSaveSuccess()
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormContent(
    isEditMode: Boolean,
    existingTransaction: Transaction?,
    isSubmitting: Boolean,
    actionErrorMessage: String?,
    onBackClick: () -> Unit,
    onSubmit: (TransactionRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var type by rememberSaveable { mutableStateOf("expense") }
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(expenseCategories.first()) }
    var dateDigits by rememberSaveable { mutableStateOf(sanitizeDateDigits(todayIsoDate())) }
    var note by rememberSaveable { mutableStateOf("") }

    var isFormInitialized by rememberSaveable(existingTransaction?.id) { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val thousandSeparator = remember { ThousandSeparatorVisualTransformation() }
    val dateIsoTransformation = remember { DateIsoVisualTransformation() }

    LaunchedEffect(existingTransaction, isEditMode) {
        if (isEditMode && existingTransaction != null && !isFormInitialized) {
            type = if (existingTransaction.type.equals("income", ignoreCase = true)) "income" else "expense"
            title = existingTransaction.title
            amountText = existingTransaction.amount.toString()
            category = existingTransaction.category.ifBlank {
                if (type == "income") incomeCategories.first() else expenseCategories.first()
            }
            dateDigits = sanitizeDateDigits(existingTransaction.date.ifBlank { todayIsoDate() })
            note = existingTransaction.note
            isFormInitialized = true
        }
    }

    val categoryOptions = if (type == "income") incomeCategories else expenseCategories

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CardSurface,
        unfocusedContainerColor = CardSurface,
        disabledContainerColor = CardSurface,
        focusedBorderColor = PrimaryTeal,
        unfocusedBorderColor = CardBorder,
        focusedLabelColor = PrimaryTeal,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = PrimaryTeal
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Transaksi" else "Tambah Transaksi",
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
            Surface(color = BackgroundCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val errorToDisplay = validationError ?: actionErrorMessage
                    if (errorToDisplay != null) {
                        Text(
                            text = errorToDisplay,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ExpenseRed,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            val parsedAmount = amountText.toLongOrNull() ?: 0L
                            val formattedDate = formatDateDigitsToIso(dateDigits)

                            when {
                                trimmedTitle.isEmpty() -> {
                                    validationError = "Judul transaksi tidak boleh kosong."
                                }
                                parsedAmount <= 0L -> {
                                    validationError = "Nominal transaksi harus lebih dari Rp 0."
                                }
                                dateDigits.isEmpty() -> {
                                    validationError = "Tanggal transaksi wajib diisi."
                                }
                                !isValidCalendarDate(formattedDate) -> {
                                    validationError = "Tanggal tidak valid. Pastikan sesuai kalender (YYYY-MM-DD)."
                                }
                                else -> {
                                    validationError = null
                                    onSubmit(
                                        TransactionRequest(
                                            title = trimmedTitle,
                                            amount = parsedAmount,
                                            type = type,
                                            category = category,
                                            date = formattedDate,
                                            note = note.trim()
                                        )
                                    )
                                }
                            }
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
                                text = if (isEditMode) "Perbarui Transaksi" else "Simpan Transaksi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TransactionTypeSelector(
                selectedType = type,
                onTypeSelected = { newType ->
                    if (type != newType) {
                        type = newType
                        val newOptions = if (newType == "income") incomeCategories else expenseCategories
                        if (!isEditMode || category !in newOptions) {
                            category = newOptions.first()
                        }
                    }
                }
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (validationError != null) validationError = null
                },
                label = { Text("Judul Transaksi") },
                placeholder = { Text("Contoh: Kopi Susu Gula Aren", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = sanitizeNominalInput(input)
                    if (validationError != null) validationError = null
                },
                label = { Text("Nominal (Rp)") },
                prefix = {
                    Text(
                        text = "Rp ",
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = thousandSeparator,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    containerColor = CardSurface
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    color = TextPrimary,
                                    fontWeight = if (option == category) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            val formattedDatePreview = formatDateDigitsToIso(dateDigits)
            val isDateInvalidComplete = dateDigits.length == 8 && !isValidCalendarDate(formattedDatePreview)

            OutlinedTextField(
                value = dateDigits,
                onValueChange = { input ->
                    dateDigits = sanitizeDateDigits(input)
                    if (validationError != null) validationError = null
                },
                label = { Text("Tanggal") },
                placeholder = { Text("YYYY-MM-DD", color = TextSecondary) },
                isError = isDateInvalidComplete,
                supportingText = {
                    Text(
                        text = if (isDateInvalidComplete) {
                            "Tanggal tidak valid sesuai kalender"
                        } else {
                            "Format YYYY-MM-DD (hanya angka) atau pilih kalender"
                        },
                        fontSize = 11.sp,
                        color = if (isDateInvalidComplete) ExpenseRed else TextSecondary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = dateIsoTransformation,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pilih Tanggal",
                            tint = PrimaryTeal
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (Opsional)") },
                placeholder = { Text("Tambahkan detail transaksi...", color = TextSecondary) },
                minLines = 3,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(formatDateDigitsToIso(dateDigits))
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateDigits = sanitizeDateDigits(millisToIsoDate(millis))
                            if (validationError != null) validationError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Pilih",
                        color = PrimaryTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = "Batal",
                        color = TextSecondary
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpenseSelected = selectedType == "expense"
    val isIncomeSelected = selectedType == "income"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TypeSegmentButton(
            label = "Pengeluaran",
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            selected = isExpenseSelected,
            selectedColor = ExpenseRed,
            onClick = { onTypeSelected("expense") },
            modifier = Modifier.weight(1f)
        )
        TypeSegmentButton(
            label = "Pemasukan",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            selected = isIncomeSelected,
            selectedColor = IncomeGreen,
            onClick = { onTypeSelected("income") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TypeSegmentButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) Color.White else TextSecondary

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor else CardSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedColor else OutlineGray
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionFormScreenPreview() {
    PiggyBankTheme {
        TransactionFormContent(
            isEditMode = false,
            existingTransaction = null,
            isSubmitting = false,
            actionErrorMessage = null,
            onBackClick = {},
            onSubmit = {}
        )
    }
}
