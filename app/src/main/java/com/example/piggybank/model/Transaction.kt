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
