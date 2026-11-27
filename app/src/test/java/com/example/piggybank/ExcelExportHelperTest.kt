package com.example.piggybank

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.util.ExcelExportConfig
import com.example.piggybank.util.ExportPeriodType
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.buildExcelExportPreview
import com.example.piggybank.util.buildExportFileName
import com.example.piggybank.util.escapeXmlText
import com.example.piggybank.util.generateExcelWorkbookBytes
import com.example.piggybank.util.guardExcelFormulaInjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ExcelExportHelperTest {

    private val sampleTx = listOf(
        Transaction(1L, "Gaji Nov", 3_000_000L, "income", "Gaji & Uang Saku", "2026-11-01", "Transfer"),
        Transaction(2L, "Makan & Minum <Promo> \"50%\"", 75_000L, "expense", "Makanan & Minuman", "2026-11-12", "=SUM(A1:A2)"),
        Transaction(3L, "Bensin Okt", 50_000L, "expense", "Transportasi", "2026-10-15", ""),
        Transaction(4L, "Bonus 2025", 1_000_000L, "income", "Bonus & Hadiah", "2025-12-20", "Akhir tahun")
    )

    private val sampleGoals = listOf(
        SavingsGoal(1L, "Dana Darurat", 2_000_000L, 2_000_000L, "2026-12-31", "Lunas"),
        SavingsGoal(2L, "Laptop Baru", 5_000_000L, 1_250_000L, "2026-01-01", "Prioritas")
    )

    private fun unzipEntries(bytes: ByteArray): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return entries
    }

    @Test
    fun generatesValidMultiSheetOoxmlZipWithAllThreeSheets() {
        val config = ExcelExportConfig(
            periodType = ExportPeriodType.ALL,
            includeSummary = true,
            includeTransactions = true,
            includeGoals = true
        )
        val preview = buildExcelExportPreview(sampleTx, sampleGoals, config, exportDateIso = "2026-11-27")
        val bytes = generateExcelWorkbookBytes(
            config = config,
            preview = preview,
            displayName = "Ghathfaan",
            monthlyBudgetLimit = 1_500_000L,
            exportDateIso = "2026-11-27"
        )

        val files = unzipEntries(bytes)
        assertTrue(files.containsKey("[Content_Types].xml"))
        assertTrue(files.containsKey("_rels/.rels"))
        assertTrue(files.containsKey("xl/workbook.xml"))
        assertTrue(files.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(files.containsKey("xl/styles.xml"))
        assertTrue(files.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(files.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(files.containsKey("xl/worksheets/sheet3.xml"))

        val workbookXml = files["xl/workbook.xml"]!!
        assertTrue(workbookXml.contains("""name="Ringkasan""""))
        assertTrue(workbookXml.contains("""name="Riwayat Transaksi""""))
        assertTrue(workbookXml.contains("""name="Target Celengan""""))

        val txSheetXml = files["xl/worksheets/sheet2.xml"]!!
        // XML special characters escaped and formula injection guarded
        assertTrue(txSheetXml.contains("Makan &amp; Minum &lt;Promo&gt; &quot;50%&quot;"))
        assertTrue(txSheetXml.contains("&apos;=SUM(A1:A2)"))
        assertTrue(txSheetXml.contains("<v>3000000</v>"))

        val goalSheetXml = files["xl/worksheets/sheet3.xml"]!!
        assertTrue(goalSheetXml.contains("Dana Darurat"))
        assertTrue(goalSheetXml.contains("Tercapai"))
        assertTrue(goalSheetXml.contains("Lewat Tenggat"))
    }

    @Test
    fun filtersByMonthAndYearAndBuildsExpectedFileNames() {
        val monthConfig = ExcelExportConfig(
            periodType = ExportPeriodType.MONTH,
            selectedMonth = YearMonthSelection(2026, 11)
        )
        val monthPreview = buildExcelExportPreview(sampleTx, sampleGoals, monthConfig, exportDateIso = "2026-11-27")
        assertEquals("PiggyBank_Laporan_Nov_2026.xlsx", monthPreview.fileName)
        assertEquals(2, monthPreview.filteredTransactions.size)
        assertEquals(3_000_000L, monthPreview.totalIncome)
        assertEquals(75_000L, monthPreview.totalExpense)
        assertEquals(2_925_000L, monthPreview.netBalance)

        val yearConfig = ExcelExportConfig(
            periodType = ExportPeriodType.YEAR,
            selectedYear = 2025
        )
        val yearPreview = buildExcelExportPreview(sampleTx, sampleGoals, yearConfig, exportDateIso = "2026-11-27")
        assertEquals("PiggyBank_Laporan_2025.xlsx", yearPreview.fileName)
        assertEquals(1, yearPreview.filteredTransactions.size)
        assertEquals(1_000_000L, yearPreview.totalIncome)
        assertEquals(0L, yearPreview.totalExpense)

        assertEquals(
            "PiggyBank_Laporan_Semua_20261127.xlsx",
            buildExportFileName(ExcelExportConfig(periodType = ExportPeriodType.ALL), "2026-11-27")
        )
    }

    @Test
    fun respectsSelectedSheetsSubsetAndRejectsEmptySelection() {
        val subsetConfig = ExcelExportConfig(
            includeSummary = false,
            includeTransactions = true,
            includeGoals = false
        )
        val preview = buildExcelExportPreview(sampleTx, sampleGoals, subsetConfig, exportDateIso = "2026-11-27")
        val files = unzipEntries(
            generateExcelWorkbookBytes(subsetConfig, preview, "Pengguna", 0L, "2026-11-27")
        )
        assertTrue(files.containsKey("xl/worksheets/sheet1.xml"))
        assertFalse(files.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(files["xl/workbook.xml"]!!.contains("""name="Riwayat Transaksi""""))

        val emptySelection = ExcelExportConfig(
            includeSummary = false,
            includeTransactions = false,
            includeGoals = false
        )
        assertFalse(emptySelection.hasAnySheetSelected)
    }

    @Test
    fun sanitizesXmlControlCharsAndFormulaPrefixes() {
        assertEquals("A &amp; B &lt;C&gt;", escapeXmlText("A & B <C>\u0000\u0007"))
        assertEquals("'=1+1", guardExcelFormulaInjection("=1+1"))
        assertEquals("'+cmd", guardExcelFormulaInjection("+cmd"))
        assertEquals("'-100", guardExcelFormulaInjection("-100"))
        assertEquals("-", guardExcelFormulaInjection("-"))
        assertEquals("'@SUM", guardExcelFormulaInjection("@SUM"))
        assertEquals("Kopi Susu", guardExcelFormulaInjection("Kopi Susu"))
    }
}
