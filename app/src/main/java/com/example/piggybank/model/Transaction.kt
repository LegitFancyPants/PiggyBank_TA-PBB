package com.example.piggybank.model

import com.google.gson.annotations.SerializedName

data class Transaction(
    val id: Long,
    val title: String,
    val amount: Long,
    val type: String,
    val category: String,
    val date: String,
    val note: String = "",
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class TransactionRequest(
    val title: String,
    val amount: Long,
    val type: String,
    val category: String,
    val date: String,
    val note: String = ""
)

enum class TransactionSortOption(val label: String) {
    NEWEST("Dari Terbaru"),
    OLDEST("Dari Terlama"),
    ALPHABETICAL("Sesuai Abjad (A–Z)"),
    AMOUNT_DESC("Nominal Terbesar"),
    AMOUNT_ASC("Nominal Terkecil")
}

data class DailyTransactionGroup(
    val date: String,
    val transactions: List<Transaction>,
    val totalIncome: Long,
    val totalExpense: Long
)

fun filterAndSortTransactions(
    transactions: List<Transaction>,
    typeFilter: String? = null,
    searchQuery: String = "",
    sortOption: TransactionSortOption = TransactionSortOption.NEWEST,
    monthPrefix: String? = null
): List<Transaction> {
    val keyword = searchQuery.trim()
    val filtered = transactions.filter { tx ->
        val matchesMonth = monthPrefix == null || tx.date.trim().startsWith(monthPrefix)
        val matchesType = typeFilter == null || tx.type.equals(typeFilter, ignoreCase = true)
        val matchesQuery = keyword.isEmpty() ||
            tx.title.contains(keyword, ignoreCase = true) ||
            tx.category.contains(keyword, ignoreCase = true)
        matchesMonth && matchesType && matchesQuery
    }

    return when (sortOption) {
        TransactionSortOption.NEWEST -> filtered.sortedWith(
            compareByDescending<Transaction> { it.date.trim() }
                .thenByDescending { it.id }
        )
        TransactionSortOption.OLDEST -> filtered.sortedWith(
            compareBy<Transaction> { it.date.trim() }
                .thenBy { it.id }
        )
        TransactionSortOption.ALPHABETICAL -> filtered.sortedWith(
            compareBy<Transaction> { it.title.trim().lowercase() }
                .thenByDescending { it.date.trim() }
                .thenByDescending { it.id }
        )
        TransactionSortOption.AMOUNT_DESC -> filtered.sortedWith(
            compareByDescending<Transaction> { it.amount }
                .thenByDescending { it.date.trim() }
                .thenByDescending { it.id }
        )
        TransactionSortOption.AMOUNT_ASC -> filtered.sortedWith(
            compareBy<Transaction> { it.amount }
                .thenByDescending { it.date.trim() }
                .thenByDescending { it.id }
        )
    }
}

fun groupTransactionsForDisplay(
    sortedTransactions: List<Transaction>,
    sortOption: TransactionSortOption = TransactionSortOption.NEWEST
): List<DailyTransactionGroup> {
    if (sortedTransactions.isEmpty()) return emptyList()

    val incomeByDate = mutableMapOf<String, Long>()
    val expenseByDate = mutableMapOf<String, Long>()
    sortedTransactions.forEach { tx ->
        val key = tx.date.trim()
        if (tx.type.equals("income", ignoreCase = true)) {
            incomeByDate[key] = (incomeByDate[key] ?: 0L) + tx.amount
        } else {
            expenseByDate[key] = (expenseByDate[key] ?: 0L) + tx.amount
        }
    }

    val isChronological = sortOption == TransactionSortOption.NEWEST ||
        sortOption == TransactionSortOption.OLDEST

    if (isChronological) {
        val grouped = LinkedHashMap<String, MutableList<Transaction>>()
        sortedTransactions.forEach { tx ->
            val key = tx.date.trim()
            grouped.getOrPut(key) { mutableListOf() }.add(tx)
        }
        return grouped.map { (dateKey, items) ->
            DailyTransactionGroup(
                date = dateKey,
                transactions = items,
                totalIncome = incomeByDate[dateKey] ?: 0L,
                totalExpense = expenseByDate[dateKey] ?: 0L
            )
        }
    }

    val runs = mutableListOf<DailyTransactionGroup>()
    var currentDate = sortedTransactions.first().date.trim()
    var currentItems = mutableListOf<Transaction>()

    sortedTransactions.forEach { tx ->
        val dateKey = tx.date.trim()
        if (dateKey != currentDate && currentItems.isNotEmpty()) {
            runs.add(
                DailyTransactionGroup(
                    date = currentDate,
                    transactions = currentItems,
                    totalIncome = incomeByDate[currentDate] ?: 0L,
                    totalExpense = expenseByDate[currentDate] ?: 0L
                )
            )
            currentDate = dateKey
            currentItems = mutableListOf()
        }
        currentItems.add(tx)
    }
    if (currentItems.isNotEmpty()) {
        runs.add(
            DailyTransactionGroup(
                date = currentDate,
                transactions = currentItems,
                totalIncome = incomeByDate[currentDate] ?: 0L,
                totalExpense = expenseByDate[currentDate] ?: 0L
            )
        )
    }
    return runs
}

fun sampleTransactions(): List<Transaction> = listOf(
    Transaction(
        id = 1L,
        title = "Makan Siang Warteg",
        amount = 18_000L,
        type = "expense",
        category = "Makanan",
        date = "2026-11-22",
        note = "Nasi ayam goreng + es teh"
    ),
    Transaction(
        id = 2L,
        title = "Uang Saku Bulanan",
        amount = 2_000_000L,
        type = "income",
        category = "Gaji",
        date = "2026-11-20",
        note = "Transfer bulanan dari orang tua"
    ),
    Transaction(
        id = 3L,
        title = "Isi Bensin Pertalite",
        amount = 35_000L,
        type = "expense",
        category = "Transportasi",
        date = "2026-11-19",
        note = "SPBU dekat kampus"
    ),
    Transaction(
        id = 4L,
        title = "Beli Buku Catatan Kuliah",
        amount = 42_000L,
        type = "expense",
        category = "Belanja",
        date = "2026-11-18",
        note = "Buku tulis dan alat tulis semester 5"
    )
)
