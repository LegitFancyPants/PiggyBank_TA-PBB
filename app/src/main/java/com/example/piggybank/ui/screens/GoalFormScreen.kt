package com.example.piggybank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.ExpenseRed
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
import com.example.piggybank.viewmodel.GoalViewModel

@Composable
fun GoalFormScreen(
    goalId: Long?,
    viewModel: GoalViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val resolvedId = goalId?.let { viewModel.resolveGoalId(it) }
    val editId = resolvedId?.takeIf { it != 0L }
    val isEditMode = editId != null

    LaunchedEffect(editId) {
        viewModel.clearError()
        if (editId != null) {
            viewModel.fetchGoalDetail(editId)
        }
    }

    val existingGoal = if (editId != null) {
        uiState.selectedGoal?.takeIf { it.id == editId }
            ?: uiState.goals.find { it.id == editId }
    } else {
        null
    }

    GoalFormContent(
        isEditMode = isEditMode,
        existingGoal = existingGoal,
        isSubmitting = uiState.isSubmitting,
        actionErrorMessage = uiState.errorMessage,
        onBackClick = onBackClick,
        onSubmit = { request ->
            if (editId != null) {
                viewModel.updateGoal(editId, request) {
                    onSaveSuccess()
                }
            } else {
                viewModel.createGoal(request) {
                    onSaveSuccess()
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormContent(
    isEditMode: Boolean,
    existingGoal: SavingsGoal?,
    isSubmitting: Boolean,
    actionErrorMessage: String?,
    onBackClick: () -> Unit,
    onSubmit: (GoalRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var targetAmountText by rememberSaveable { mutableStateOf("") }
    var currentAmountText by rememberSaveable { mutableStateOf("0") }
    var deadlineDigits by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    var isFormInitialized by rememberSaveable(existingGoal?.id) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val thousandSeparator = remember { ThousandSeparatorVisualTransformation() }
    val dateIsoTransformation = remember { DateIsoVisualTransformation() }

    LaunchedEffect(existingGoal, isEditMode) {
        if (isEditMode && existingGoal != null && !isFormInitialized) {
            name = existingGoal.name
            targetAmountText = existingGoal.targetAmount.toString()
            currentAmountText = existingGoal.currentAmount.toString()
            deadlineDigits = sanitizeDateDigits(existingGoal.deadline)
            note = existingGoal.note
            isFormInitialized = true
        }
    }

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
                        text = if (isEditMode) "Edit Target Tabungan" else "Target Tabungan Baru",
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
                    val errorToShow = validationError ?: actionErrorMessage
                    if (errorToShow != null) {
                        Text(
                            text = errorToShow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ExpenseRed,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val parsedTarget = targetAmountText.toLongOrNull() ?: 0L
                            val parsedCurrent = currentAmountText.toLongOrNull() ?: 0L
                            val formattedDeadline = formatDateDigitsToIso(deadlineDigits)

                            when {
                                trimmedName.isEmpty() -> {
                                    validationError = "Nama target tabungan tidak boleh kosong."
                                }
                                parsedTarget <= 0L -> {
                                    validationError = "Nominal target harus lebih dari Rp 0."
                                }
                                parsedCurrent > parsedTarget -> {
                                    validationError = "Saldo terkumpul tidak boleh melebihi nominal target."
                                }
                                deadlineDigits.isNotEmpty() && !isValidCalendarDate(formattedDeadline) -> {
                                    validationError = "Tanggal target tidak valid. Pastikan sesuai kalender (YYYY-MM-DD)."
                                }
                                else -> {
                                    validationError = null
                                    onSubmit(
                                        GoalRequest(
                                            name = trimmedName,
                                            targetAmount = parsedTarget,
                                            currentAmount = parsedCurrent.coerceAtLeast(0L),
                                            deadline = if (deadlineDigits.isEmpty()) "" else formattedDeadline,
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
                                text = if (isEditMode) "Perbarui Target" else "Simpan Target",
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
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (validationError != null) validationError = null
                },
                label = { Text("Nama Target Tabungan") },
                placeholder = { Text("Contoh: Dana Darurat Mahasiswa", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = targetAmountText,
                onValueChange = { input ->
                    targetAmountText = sanitizeNominalInput(input)
                    if (validationError != null) validationError = null
                },
                label = { Text("Nominal Target (Rp)") },
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

            OutlinedTextField(
                value = currentAmountText,
                onValueChange = { input ->
                    currentAmountText = sanitizeNominalInput(input)
                    if (validationError != null) validationError = null
                },
                label = { Text("Saldo Awal Terkumpul (Rp)") },
                prefix = {
                    Text(
                        text = "Rp ",
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                },
                supportingText = {
                    Text(
                        text = "Isi 0 jika mulai menabung dari awal",
                        fontSize = 11.sp,
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

            val formattedDeadlinePreview = formatDateDigitsToIso(deadlineDigits)
            val isDeadlineInvalidComplete = deadlineDigits.length == 8 && !isValidCalendarDate(formattedDeadlinePreview)

            OutlinedTextField(
                value = deadlineDigits,
                onValueChange = { input ->
                    deadlineDigits = sanitizeDateDigits(input)
                    if (validationError != null) validationError = null
                },
                label = { Text("Tenggat Waktu / Deadline (Opsional)") },
                placeholder = { Text("YYYY-MM-DD", color = TextSecondary) },
                isError = isDeadlineInvalidComplete,
                supportingText = {
                    Text(
                        text = if (isDeadlineInvalidComplete) {
                            "Tanggal target tidak valid sesuai kalender"
                        } else {
                            "Opsional, format YYYY-MM-DD (hanya angka)"
                        },
                        fontSize = 11.sp,
                        color = if (isDeadlineInvalidComplete) ExpenseRed else TextSecondary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = dateIsoTransformation,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pilih Tenggat Waktu",
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
                label = { Text("Catatan / Motivasi (Opsional)") },
                placeholder = { Text("Tuliskan motivasi atau rencana menabung...", color = TextSecondary) },
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
            initialSelectedDateMillis = isoDateToUtcMillis(formatDateDigitsToIso(deadlineDigits))
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            deadlineDigits = sanitizeDateDigits(millisToIsoDate(millis))
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GoalFormScreenPreview() {
    PiggyBankTheme {
        GoalFormContent(
            isEditMode = false,
            existingGoal = null,
            isSubmitting = false,
            actionErrorMessage = null,
            onBackClick = {},
            onSubmit = {}
        )
    }
}
