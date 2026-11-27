package com.example.piggybank.util

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionSortOption
import com.example.piggybank.model.filterAndSortTransactions
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val EXCEL_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

enum class ExportPeriodType(val label: String) {
    ALL("Semua Data"),
    MONTH("Bulan"),
    YEAR("Tahun")
}

data class ExcelExportConfig(
    val periodType: ExportPeriodType = ExportPeriodType.ALL,
    val selectedMonth: YearMonthSelection = currentYearMonthSelection(),
    val selectedYear: Int = currentYearMonthSelection().year,
    val includeSummary: Boolean = true,
    val includeTransactions: Boolean = true,
    val includeGoals: Boolean = true
) {
    val selectedSheetCount: Int
        get() = listOf(includeSummary, includeTransactions, includeGoals).count { it }

    val hasAnySheetSelected: Boolean
        get() = selectedSheetCount > 0
}

data class ExcelExportPreview(
    val fileName: String,
    val periodLabel: String,
    val filteredTransactions: List<Transaction>,
    val goals: List<SavingsGoal>,
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long,
    val totalAssets: Long,
    val totalGoalTarget: Long,
    val totalGoalSaved: Long,
    val completedGoalsCount: Int,
    val selectedSheetCount: Int
)

private data class CategorySummaryRow(
    val typeLabel: String,
    val category: String,
    val count: Int,
    val totalAmount: Long,
    val fraction: Double
)

fun buildExcelExportPreview(
    allTransactions: List<Transaction>,
    goals: List<SavingsGoal>,
    config: ExcelExportConfig,
    exportDateIso: String = todayIsoDate()
): ExcelExportPreview {
    val filteredTransactions = filterTransactionsForExport(allTransactions, config)
    val totalIncome = filteredTransactions
        .filter { it.type.equals("income", ignoreCase = true) }
        .sumOf { it.amount }
    val totalExpense = filteredTransactions
        .filter { it.type.equals("expense", ignoreCase = true) }
        .sumOf { it.amount }
    val netBalance = totalIncome - totalExpense
    val totalAssets = calculateTotalAssets(allTransactions, goals)
    val totalGoalTarget = goals.sumOf { it.targetAmount }
    val totalGoalSaved = goals.sumOf { it.currentAmount }
    val completedGoalsCount = goals.count { it.isCompleted }

    return ExcelExportPreview(
        fileName = buildExportFileName(config, exportDateIso),
        periodLabel = buildExportPeriodLabel(config),
        filteredTransactions = filteredTransactions,
        goals = goals,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        netBalance = netBalance,
        totalAssets = totalAssets,
        totalGoalTarget = totalGoalTarget,
        totalGoalSaved = totalGoalSaved,
        completedGoalsCount = completedGoalsCount,
        selectedSheetCount = config.selectedSheetCount
    )
}

fun filterTransactionsForExport(
    allTransactions: List<Transaction>,
    config: ExcelExportConfig
): List<Transaction> {
    val sorted = filterAndSortTransactions(
        transactions = allTransactions,
        typeFilter = null,
        searchQuery = "",
        sortOption = TransactionSortOption.NEWEST,
        monthPrefix = if (config.periodType == ExportPeriodType.MONTH) {
            config.selectedMonth.toYearMonthPrefix()
        } else {
            null
        }
    )
    return when (config.periodType) {
        ExportPeriodType.ALL -> sorted
        ExportPeriodType.MONTH -> sorted
        ExportPeriodType.YEAR -> {
            val yearPrefix = String.format(Locale.US, "%04d-", config.selectedYear)
            sorted.filter { it.date.trim().startsWith(yearPrefix) }
        }
    }
}

fun buildExportPeriodLabel(config: ExcelExportConfig): String = when (config.periodType) {
    ExportPeriodType.ALL -> "Semua Periode"
    ExportPeriodType.MONTH -> {
        val monthFull = fullIndonesianMonths[(config.selectedMonth.month - 1).coerceIn(0, 11)]
        "$monthFull ${config.selectedMonth.year}"
    }
    ExportPeriodType.YEAR -> "Tahun ${config.selectedYear}"
}

fun buildExportFileName(
    config: ExcelExportConfig,
    exportDateIso: String = todayIsoDate()
): String {
    val compactDate = exportDateIso.filter { it.isDigit() }.take(8).ifBlank { "export" }
    val rawSuffix = when (config.periodType) {
        ExportPeriodType.ALL -> "Semua_$compactDate"
        ExportPeriodType.MONTH -> {
            val monthShort = indonesianMonths[(config.selectedMonth.month - 1).coerceIn(0, 11)]
            "${monthShort}_${config.selectedMonth.year}"
        }
        ExportPeriodType.YEAR -> "${config.selectedYear}"
    }
    val safeSuffix = rawSuffix.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    return "PiggyBank_Laporan_${safeSuffix}.xlsx"
}

/**
 * Generates a valid multi-sheet Office Open XML (.xlsx) workbook in memory using
 * standard ZipOutputStream + SpreadsheetML XML without external heavyweight dependencies.
 */
fun generateExcelWorkbookBytes(
    config: ExcelExportConfig,
    preview: ExcelExportPreview,
    displayName: String,
    monthlyBudgetLimit: Long,
    exportDateIso: String = todayIsoDate()
): ByteArray {
    require(config.hasAnySheetSelected) { "Minimal satu sheet harus dipilih untuk ekspor Excel." }

    val sheets = mutableListOf<Pair<String, String>>()
    if (config.includeSummary) {
        sheets.add(
            "Ringkasan" to buildSummarySheetXml(
                config = config,
                preview = preview,
                displayName = displayName,
                monthlyBudgetLimit = monthlyBudgetLimit,
                exportDateIso = exportDateIso
            )
        )
    }
    if (config.includeTransactions) {
        sheets.add(
            "Riwayat Transaksi" to buildTransactionsSheetXml(
                preview = preview,
                exportDateIso = exportDateIso
            )
        )
    }
    if (config.includeGoals) {
        sheets.add(
            "Target Celengan" to buildGoalsSheetXml(
                preview = preview,
                exportDateIso = exportDateIso
            )
        )
    }

    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size))
        writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
        writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets.map { it.first }))
        writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheets.size))
        writeZipEntry(zip, "xl/styles.xml", buildStylesXml())
        sheets.forEachIndexed { index, (_, sheetXml) ->
            writeZipEntry(zip, "xl/worksheets/sheet${index + 1}.xml", sheetXml)
        }
    }
    return output.toByteArray()
}

// =========================================================================
// Sheet Builders
// =========================================================================

private fun buildSummarySheetXml(
    config: ExcelExportConfig,
    preview: ExcelExportPreview,
    displayName: String,
    monthlyBudgetLimit: Long,
    exportDateIso: String
): String {
    val rows = mutableListOf<ExcelRow>()
    rows.add(ExcelRow(listOf(ExcelCell.text("LAPORAN KEUANGAN PIGGYBANK", STYLE_TITLE))))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text(
                    "Pemilik: ${displayName.trim().ifBlank { "Pengguna" }} | Periode: ${preview.periodLabel} | Diekspor: ${formatDisplayDate(exportDateIso)}",
                    STYLE_META
                )
            )
        )
    )
    rows.add(ExcelRow(emptyList()))

    // Bagian 1: Ringkasan Keuangan Utama
    rows.add(ExcelRow(listOf(ExcelCell.text("RINGKASAN KEUANGAN", STYLE_SECTION))))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Metrik", STYLE_HEADER),
                ExcelCell.text("Nilai", STYLE_HEADER),
                ExcelCell.text("Keterangan", STYLE_HEADER)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Total Aset Keseluruhan", STYLE_TEXT),
                ExcelCell.number(
                    preview.totalAssets,
                    if (preview.totalAssets < 0L) STYLE_CURRENCY_RED else STYLE_CURRENCY_GREEN
                ),
                ExcelCell.text("Saldo kas semua waktu + total saldo celengan", STYLE_TEXT)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Total Pemasukan", STYLE_TEXT),
                ExcelCell.number(preview.totalIncome, STYLE_CURRENCY_GREEN),
                ExcelCell.text(preview.periodLabel, STYLE_TEXT)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Total Pengeluaran", STYLE_TEXT),
                ExcelCell.number(preview.totalExpense, STYLE_CURRENCY_RED),
                ExcelCell.text(preview.periodLabel, STYLE_TEXT)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Saldo Bersih Periode", STYLE_TOTAL_LABEL),
                ExcelCell.number(
                    preview.netBalance,
                    if (preview.netBalance < 0L) STYLE_TOTAL_CURRENCY_RED else STYLE_TOTAL_CURRENCY_GREEN
                ),
                ExcelCell.text("Pemasukan dikurangi Pengeluaran", STYLE_TOTAL_LABEL)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Jumlah Transaksi", STYLE_TEXT),
                ExcelCell.number(preview.filteredTransactions.size.toLong(), STYLE_INTEGER),
                ExcelCell.text("Transaksi tercatat pada periode ini", STYLE_TEXT)
            )
        )
    )

    // Info Anggaran Bulanan
    val safeBudget = monthlyBudgetLimit.coerceAtLeast(0L)
    if (safeBudget > 0L) {
        val budgetLabelNote = if (config.periodType == ExportPeriodType.MONTH) {
            val usage = calculateMonthlyBudgetUsage(preview.totalExpense, safeBudget)
            val pct = String.format(Locale.US, "%.1f%%", usage.usagePercentageExact)
            if (usage.isOverBudget) {
                "Melebihi batas ($pct, kelebihan ${usage.overBudgetAmount.toRupiah()})"
            } else {
                "Terpakai $pct (sisa ${usage.remainingAmount.toRupiah()})"
            }
        } else {
            "Batas pengeluaran per bulan"
        }
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.text("Batas Anggaran Bulanan", STYLE_TEXT),
                    ExcelCell.number(safeBudget, STYLE_CURRENCY),
                    ExcelCell.text(budgetLabelNote, STYLE_TEXT)
                )
            )
        )
    } else {
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.text("Batas Anggaran Bulanan", STYLE_TEXT),
                    ExcelCell.text("Belum diatur", STYLE_TEXT_CENTER),
                    ExcelCell.text("Atur di Profil -> Laporan -> Anggaran Bulanan", STYLE_TEXT)
                )
            )
        )
    }

    // Bagian 2: Ringkasan Celengan
    rows.add(ExcelRow(emptyList()))
    rows.add(ExcelRow(listOf(ExcelCell.text("RINGKASAN TARGET CELENGAN", STYLE_SECTION))))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Metrik Celengan", STYLE_HEADER),
                ExcelCell.text("Nilai", STYLE_HEADER),
                ExcelCell.text("Keterangan", STYLE_HEADER)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Total Target Celengan", STYLE_TEXT),
                ExcelCell.number(preview.goals.size.toLong(), STYLE_INTEGER),
                ExcelCell.text("${preview.completedGoalsCount} target sudah tercapai", STYLE_TEXT)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Total Nominal Target", STYLE_TEXT),
                ExcelCell.number(preview.totalGoalTarget, STYLE_CURRENCY),
                ExcelCell.text("Akumulasi seluruh target celengan", STYLE_TEXT)
            )
        )
    )
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Total Saldo Terkumpul", STYLE_TOTAL_LABEL),
                ExcelCell.number(preview.totalGoalSaved, STYLE_TOTAL_CURRENCY_GREEN),
                ExcelCell.text("Dana terkumpul di seluruh celengan", STYLE_TOTAL_LABEL)
            )
        )
    )

    // Bagian 3: Rekap per Kategori
    val categoryBreakdown = buildCategoryBreakdown(preview.filteredTransactions)
    rows.add(ExcelRow(emptyList()))
    rows.add(ExcelRow(listOf(ExcelCell.text("REKAP PER KATEGORI (${preview.periodLabel.uppercase(Locale.ROOT)})", STYLE_SECTION))))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("Tipe", STYLE_HEADER),
                ExcelCell.text("Kategori", STYLE_HEADER),
                ExcelCell.text("Jumlah Transaksi", STYLE_HEADER),
                ExcelCell.text("Total Nominal (Rp)", STYLE_HEADER),
                ExcelCell.text("Porsi", STYLE_HEADER)
            )
        )
    )
    if (categoryBreakdown.isEmpty()) {
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.text("Tidak ada transaksi pada periode ini.", STYLE_META)
                )
            )
        )
    } else {
        categoryBreakdown.forEach { cat ->
            val isIncome = cat.typeLabel == "Pemasukan"
            rows.add(
                ExcelRow(
                    listOf(
                        ExcelCell.text(cat.typeLabel, if (isIncome) STYLE_TEXT_GREEN else STYLE_TEXT_RED),
                        ExcelCell.text(cat.category, STYLE_TEXT),
                        ExcelCell.number(cat.count.toLong(), STYLE_INTEGER),
                        ExcelCell.number(cat.totalAmount, if (isIncome) STYLE_CURRENCY_GREEN else STYLE_CURRENCY_RED),
                        ExcelCell.decimal(cat.fraction, STYLE_PERCENT)
                    )
                )
            )
        }
    }

    return renderWorksheetXml(
        columnWidths = listOf(28.0, 24.0, 42.0, 22.0, 14.0),
        rows = rows
    )
}

private fun buildTransactionsSheetXml(
    preview: ExcelExportPreview,
    exportDateIso: String
): String {
    val rows = mutableListOf<ExcelRow>()
    rows.add(ExcelRow(listOf(ExcelCell.text("RIWAYAT TRANSAKSI KAS", STYLE_TITLE))))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text(
                    "Periode: ${preview.periodLabel} | Jumlah: ${preview.filteredTransactions.size} transaksi | Diekspor: ${formatDisplayDate(exportDateIso)}",
                    STYLE_META
                )
            )
        )
    )
    rows.add(ExcelRow(emptyList()))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("No", STYLE_HEADER),
                ExcelCell.text("Tanggal", STYLE_HEADER),
                ExcelCell.text("Judul Transaksi", STYLE_HEADER),
                ExcelCell.text("Tipe", STYLE_HEADER),
                ExcelCell.text("Kategori", STYLE_HEADER),
                ExcelCell.text("Nominal (Rp)", STYLE_HEADER),
                ExcelCell.text("Catatan", STYLE_HEADER)
            )
        )
    )

    if (preview.filteredTransactions.isEmpty()) {
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.text("Tidak ada catatan transaksi pada periode ini.", STYLE_META)
                )
            )
        )
    } else {
        preview.filteredTransactions.forEachIndexed { index, tx ->
            val isIncome = tx.type.equals("income", ignoreCase = true)
            rows.add(
                ExcelRow(
                    listOf(
                        ExcelCell.number((index + 1).toLong(), STYLE_INTEGER),
                        ExcelCell.text(tx.date.trim(), STYLE_TEXT_CENTER),
                        ExcelCell.safeUserText(tx.title, STYLE_TEXT),
                        ExcelCell.text(
                            if (isIncome) "Pemasukan" else "Pengeluaran",
                            if (isIncome) STYLE_TEXT_GREEN else STYLE_TEXT_RED
                        ),
                        ExcelCell.safeUserText(tx.category.ifBlank { "Lainnya" }, STYLE_TEXT),
                        ExcelCell.number(
                            tx.amount,
                            if (isIncome) STYLE_CURRENCY_GREEN else STYLE_CURRENCY_RED
                        ),
                        ExcelCell.safeUserText(tx.note.ifBlank { "-" }, STYLE_TEXT)
                    )
                )
            )
        }

        rows.add(ExcelRow(emptyList()))
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.text("Total Pemasukan", STYLE_TOTAL_LABEL),
                    ExcelCell.number(preview.totalIncome, STYLE_TOTAL_CURRENCY_GREEN),
                    ExcelCell.empty()
                )
            )
        )
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.text("Total Pengeluaran", STYLE_TOTAL_LABEL),
                    ExcelCell.number(preview.totalExpense, STYLE_TOTAL_CURRENCY_RED),
                    ExcelCell.empty()
                )
            )
        )
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.text("Saldo Bersih", STYLE_TOTAL_LABEL),
                    ExcelCell.number(
                        preview.netBalance,
                        if (preview.netBalance < 0L) STYLE_TOTAL_CURRENCY_RED else STYLE_TOTAL_CURRENCY_GREEN
                    ),
                    ExcelCell.empty()
                )
            )
        )
    }

    return renderWorksheetXml(
        columnWidths = listOf(6.0, 14.0, 28.0, 15.0, 22.0, 20.0, 32.0),
        rows = rows
    )
}

private fun buildGoalsSheetXml(
    preview: ExcelExportPreview,
    exportDateIso: String
): String {
    val rows = mutableListOf<ExcelRow>()
    rows.add(ExcelRow(listOf(ExcelCell.text("DAFTAR TARGET CELENGAN", STYLE_TITLE))))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text(
                    "Total Target: ${preview.goals.size} (${preview.completedGoalsCount} tercapai) | Diekspor: ${formatDisplayDate(exportDateIso)}",
                    STYLE_META
                )
            )
        )
    )
    rows.add(ExcelRow(emptyList()))
    rows.add(
        ExcelRow(
            listOf(
                ExcelCell.text("No", STYLE_HEADER),
                ExcelCell.text("Nama Target", STYLE_HEADER),
                ExcelCell.text("Nominal Target (Rp)", STYLE_HEADER),
                ExcelCell.text("Saldo Terkumpul (Rp)", STYLE_HEADER),
                ExcelCell.text("Sisa Kekurangan (Rp)", STYLE_HEADER),
                ExcelCell.text("Progress", STYLE_HEADER),
                ExcelCell.text("Status", STYLE_HEADER),
                ExcelCell.text("Tenggat Waktu", STYLE_HEADER),
                ExcelCell.text("Catatan", STYLE_HEADER)
            )
        )
    )

    if (preview.goals.isEmpty()) {
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.text("Belum ada target celengan yang tercatat.", STYLE_META)
                )
            )
        )
    } else {
        preview.goals.forEachIndexed { index, goal ->
            val isOverdue = goal.isOverdue(exportDateIso)
            val (statusLabel, statusStyle) = when {
                goal.isCompleted -> "Tercapai" to STYLE_TEXT_GREEN
                isOverdue -> "Lewat Tenggat" to STYLE_TEXT_RED
                else -> "Belum Tercapai" to STYLE_TEXT_CENTER
            }
            rows.add(
                ExcelRow(
                    listOf(
                        ExcelCell.number((index + 1).toLong(), STYLE_INTEGER),
                        ExcelCell.safeUserText(goal.name, STYLE_TEXT),
                        ExcelCell.number(goal.targetAmount, STYLE_CURRENCY),
                        ExcelCell.number(goal.currentAmount, STYLE_CURRENCY_GREEN),
                        ExcelCell.number(goal.remainingAmount, STYLE_CURRENCY),
                        ExcelCell.decimal(goal.progressFraction.toDouble(), STYLE_PERCENT),
                        ExcelCell.text(statusLabel, statusStyle),
                        ExcelCell.text(goal.deadline.ifBlank { "Tanpa tenggat" }, STYLE_TEXT_CENTER),
                        ExcelCell.safeUserText(goal.note.ifBlank { "-" }, STYLE_TEXT)
                    )
                )
            )
        }

        val totalRemaining = preview.goals.sumOf { it.remainingAmount }
        rows.add(ExcelRow(emptyList()))
        rows.add(
            ExcelRow(
                listOf(
                    ExcelCell.empty(),
                    ExcelCell.text("Total Keseluruhan", STYLE_TOTAL_LABEL),
                    ExcelCell.number(preview.totalGoalTarget, STYLE_TOTAL_CURRENCY),
                    ExcelCell.number(preview.totalGoalSaved, STYLE_TOTAL_CURRENCY_GREEN),
                    ExcelCell.number(totalRemaining, STYLE_TOTAL_CURRENCY),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty(),
                    ExcelCell.empty()
                )
            )
        )
    }

    return renderWorksheetXml(
        columnWidths = listOf(6.0, 26.0, 20.0, 20.0, 20.0, 12.0, 16.0, 16.0, 30.0),
        rows = rows
    )
}

private fun buildCategoryBreakdown(transactions: List<Transaction>): List<CategorySummaryRow> {
    if (transactions.isEmpty()) return emptyList()

    val result = mutableListOf<CategorySummaryRow>()
    listOf("expense" to "Pengeluaran", "income" to "Pemasukan").forEach { (rawType, label) ->
        val typed = transactions.filter { it.type.equals(rawType, ignoreCase = true) }
        val grandTotal = typed.sumOf { it.amount }
        if (grandTotal <= 0L) return@forEach

        typed
            .groupBy { it.category.trim().ifBlank { "Lainnya" } }
            .entries
            .map { (cat, items) ->
                val catTotal = items.sumOf { it.amount }
                CategorySummaryRow(
                    typeLabel = label,
                    category = cat,
                    count = items.size,
                    totalAmount = catTotal,
                    fraction = catTotal.toDouble() / grandTotal.toDouble()
                )
            }
            .sortedByDescending { it.totalAmount }
            .also { result.addAll(it) }
    }
    return result
}

// =========================================================================
// Low-Level OOXML Packaging & Rendering
// =========================================================================

private const val STYLE_DEFAULT = 0
private const val STYLE_TITLE = 1
private const val STYLE_META = 2
private const val STYLE_SECTION = 3
private const val STYLE_HEADER = 4
private const val STYLE_TEXT = 5
private const val STYLE_CURRENCY = 6
private const val STYLE_CURRENCY_GREEN = 7
private const val STYLE_CURRENCY_RED = 8
private const val STYLE_TOTAL_LABEL = 9
private const val STYLE_TOTAL_CURRENCY = 10
private const val STYLE_PERCENT = 11
private const val STYLE_INTEGER = 12
private const val STYLE_TEXT_CENTER = 13
private const val STYLE_TOTAL_CURRENCY_GREEN = 14
private const val STYLE_TOTAL_CURRENCY_RED = 15
private const val STYLE_TEXT_GREEN = 16
private const val STYLE_TEXT_RED = 17

private data class ExcelRow(val cells: List<ExcelCell>)

private sealed class ExcelCell(val styleIndex: Int) {
    class TextCell(val value: String, styleIndex: Int) : ExcelCell(styleIndex)
    class LongCell(val value: Long, styleIndex: Int) : ExcelCell(styleIndex)
    class DoubleCell(val value: Double, styleIndex: Int) : ExcelCell(styleIndex)
    data object EmptyCell : ExcelCell(STYLE_DEFAULT)

    companion object {
        fun text(value: String, styleIndex: Int = STYLE_DEFAULT): ExcelCell =
            TextCell(value, styleIndex)

        fun safeUserText(value: String, styleIndex: Int = STYLE_TEXT): ExcelCell =
            TextCell(guardExcelFormulaInjection(value), styleIndex)

        fun number(value: Long, styleIndex: Int = STYLE_INTEGER): ExcelCell =
            LongCell(value, styleIndex)

        fun decimal(value: Double, styleIndex: Int = STYLE_PERCENT): ExcelCell =
            DoubleCell(value, styleIndex)

        fun empty(): ExcelCell = EmptyCell
    }
}

internal fun guardExcelFormulaInjection(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return when (trimmed.first()) {
        '=', '+', '@', '\t', '\r' -> "'$trimmed"
        '-' -> if (trimmed.length > 1) "'$trimmed" else trimmed
        else -> trimmed
    }
}

internal fun escapeXmlText(raw: String): String {
    val sb = StringBuilder(raw.length + 16)
    for (ch in raw) {
        when (ch) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&apos;")
            '\t', '\n', '\r' -> sb.append(ch)
            else -> {
                if (ch.code >= 0x20) {
                    sb.append(ch)
                }
            }
        }
    }
    return sb.toString()
}

private fun colIndexToLetter(index: Int): String {
    var current = index
    val sb = StringBuilder()
    while (current >= 0) {
        sb.append(('A'.code + (current % 26)).toChar())
        current = (current / 26) - 1
    }
    return sb.reverse().toString()
}

private fun renderWorksheetXml(
    columnWidths: List<Double>,
    rows: List<ExcelRow>
): String {
    val sb = StringBuilder(4096)
    sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
    if (columnWidths.isNotEmpty()) {
        sb.append("<cols>")
        columnWidths.forEachIndexed { idx, width ->
            val col = idx + 1
            val formattedWidth = String.format(Locale.US, "%.1f", width)
            sb.append("""<col min="$col" max="$col" width="$formattedWidth" customWidth="1"/>""")
        }
        sb.append("</cols>")
    }
    sb.append("<sheetData>")
    rows.forEachIndexed { rowIdx, row ->
        val rowNum = rowIdx + 1
        val hasHeaderCell = row.cells.any { it.styleIndex == STYLE_HEADER }
        val rowHeightAttr = if (hasHeaderCell) """ ht="24" customHeight="1"""" else ""
        sb.append("""<row r="$rowNum"$rowHeightAttr>""")
        row.cells.forEachIndexed { colIdx, cell ->
            if (cell is ExcelCell.EmptyCell) return@forEachIndexed
            val ref = "${colIndexToLetter(colIdx)}$rowNum"
            when (cell) {
                is ExcelCell.TextCell -> {
                    val escaped = escapeXmlText(cell.value)
                    sb.append("""<c r="$ref" t="inlineStr" s="${cell.styleIndex}"><is><t xml:space="preserve">$escaped</t></is></c>""")
                }
                is ExcelCell.LongCell -> {
                    sb.append("""<c r="$ref" s="${cell.styleIndex}"><v>${cell.value}</v></c>""")
                }
                is ExcelCell.DoubleCell -> {
                    val safeDouble = if (cell.value.isFinite()) cell.value else 0.0
                    val formatted = String.format(Locale.US, "%.6f", safeDouble)
                    sb.append("""<c r="$ref" s="${cell.styleIndex}"><v>$formatted</v></c>""")
                }
                ExcelCell.EmptyCell -> Unit
            }
        }
        sb.append("</row>")
    }
    sb.append("</sheetData>")
    sb.append("</worksheet>")
    return sb.toString()
}

private fun buildContentTypesXml(sheetCount: Int): String = buildString {
    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
    append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
    append("""<Default Extension="xml" ContentType="application/xml"/>""")
    append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
    append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
    for (i in 1..sheetCount) {
        append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
    }
    append("</Types>")
}

private fun buildRootRelsXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        """</Relationships>"""

private fun buildWorkbookXml(sheetNames: List<String>): String = buildString {
    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
    append("<sheets>")
    sheetNames.forEachIndexed { idx, name ->
        val safeName = escapeXmlText(name.take(31))
        val id = idx + 1
        append("""<sheet name="$safeName" sheetId="$id" r:id="rId$id"/>""")
    }
    append("</sheets>")
    append("</workbook>")
}

private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
    for (i in 1..sheetCount) {
        append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
    }
    append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
    append("</Relationships>")
}

private fun buildStylesXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<numFmts count="3">""" +
        """<numFmt numFmtId="164" formatCode="&quot;Rp &quot;#,##0;-&quot;Rp &quot;#,##0;&quot;Rp &quot;0"/>""" +
        """<numFmt numFmtId="165" formatCode="0.0%"/>""" +
        """<numFmt numFmtId="166" formatCode="#,##0"/>""" +
        """</numFmts>""" +
        """<fonts count="7">""" +
        """<font><sz val="11"/><color rgb="FF0F172A"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="14"/><color rgb="FF0D7377"/><name val="Calibri"/></font>""" +
        """<font><i/><sz val="10"/><color rgb="FF64748B"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FF0F172A"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FF16A34A"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FFDC2626"/><name val="Calibri"/></font>""" +
        """</fonts>""" +
        """<fills count="5">""" +
        """<fill><patternFill patternType="none"/></fill>""" +
        """<fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FF0D7377"/><bgColor indexed="64"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFE6F4F1"/><bgColor indexed="64"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/><bgColor indexed="64"/></patternFill></fill>""" +
        """</fills>""" +
        """<borders count="3">""" +
        """<border><left/><right/><top/><bottom/><diagonal/></border>""" +
        """<border>""" +
        """<left style="thin"><color rgb="FFCBD5E1"/></left>""" +
        """<right style="thin"><color rgb="FFCBD5E1"/></right>""" +
        """<top style="thin"><color rgb="FFCBD5E1"/></top>""" +
        """<bottom style="thin"><color rgb="FFCBD5E1"/></bottom>""" +
        """<diagonal/>""" +
        """</border>""" +
        """<border>""" +
        """<left style="thin"><color rgb="FFCBD5E1"/></left>""" +
        """<right style="thin"><color rgb="FFCBD5E1"/></right>""" +
        """<top style="thin"><color rgb="FF0D7377"/></top>""" +
        """<bottom style="medium"><color rgb="FF0D7377"/></bottom>""" +
        """<diagonal/>""" +
        """</border>""" +
        """</borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="18">""" +
        // 0: Default
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        // 1: Title
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        // 2: Meta
        """<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        // 3: Section
        """<xf numFmtId="0" fontId="4" fillId="4" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
        // 4: Header
        """<xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>""" +
        // 5: Table Text
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>""" +
        // 6: Currency
        """<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 7: Currency Green
        """<xf numFmtId="164" fontId="5" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 8: Currency Red
        """<xf numFmtId="164" fontId="6" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 9: Total Label
        """<xf numFmtId="0" fontId="4" fillId="3" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>""" +
        // 10: Total Currency
        """<xf numFmtId="164" fontId="4" fillId="3" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 11: Percent
        """<xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 12: Integer
        """<xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
        // 13: Text Center
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
        // 14: Total Currency Green
        """<xf numFmtId="164" fontId="5" fillId="3" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 15: Total Currency Red
        """<xf numFmtId="164" fontId="6" fillId="3" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        // 16: Text Green Bold
        """<xf numFmtId="0" fontId="5" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
        // 17: Text Red Bold
        """<xf numFmtId="0" fontId="6" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
        """</cellXfs>""" +
        """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
        """</styleSheet>"""

private fun writeZipEntry(zip: ZipOutputStream, entryName: String, xmlContent: String) {
    zip.putNextEntry(ZipEntry(entryName))
    zip.write(xmlContent.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
}
